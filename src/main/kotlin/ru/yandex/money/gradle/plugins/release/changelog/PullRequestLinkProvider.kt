package ru.yandex.money.gradle.plugins.release.changelog

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import ru.yandex.money.gradle.plugins.release.PullRequestLinkSettings
import ru.yandex.money.gradle.plugins.release.git.GitManager
import ru.yandex.money.tools.bitbucket.client.BitbucketClient
import ru.yandex.money.tools.bitbucket.client.BitbucketConnectionSettings
import java.net.URI

/**
 * Предоставляет ссылку на pull request релиза
 *
 * @author Alexander Esipov (asesipov@yamoney.ru)
 * @since 13.02.2020
 */
class PullRequestLinkProvider(private val gitManager: GitManager, private val pullRequestLinkSettings: PullRequestLinkSettings) {

    companion object {
        private val log: Logger = Logging.getLogger(PullRequestLinkProvider::class.java)
    }

    /**
     * Возвращает ссылку на pull request изменения артефакта с последнего релиза
     */
    fun getReleasePullRequestLink(): String? {
        return try {
            val artifactLocation = parseBitbucketArtifactLocation(URI(gitManager.getRemoteOriginUrl()))

            val bitbucketClient = BitbucketClient(BitbucketConnectionSettings.builder()
                    .withUri(URI.create(artifactLocation.host))
                    .withUser(pullRequestLinkSettings.bitbucketUser
                            ?: throw IllegalArgumentException("bitbucketUser is absent"))
                    .withPassword(pullRequestLinkSettings.bitbucketPassword
                            ?: throw IllegalArgumentException("bitbucketPassword is absent"))
                    .build())

            return bitbucketClient
                    .getLatestPullRequestLink(artifactLocation.project, artifactLocation.repository, "MERGED")
                    .filter { isPullRequestValid(bitbucketClient, artifactLocation, it.pullRequestId) }
                    .map { it.link }
                    .orElse(null)
        } catch (e: Exception) {
            log.warn("can't getPullRequestLink", e)
            null
        }
    }

    private fun parseBitbucketArtifactLocation(url: URI): BitbucketArtifactLocation {
        val pathFragments = url.path.split("/")
        val scheme = if (url.scheme.startsWith("http")) url.scheme + "://" else ""
        return BitbucketArtifactLocation(
                host = "$scheme${url.host}:${url.port}",
                project = pathFragments[1],
                repository = pathFragments[2].removeSuffix(".git")
        )
    }

    /**
     * Проверяет, что коммиты пулриквеста соответсвуют коммитам в git с момента последнего релиза (тэга)
     */
    private fun isPullRequestValid(bitbucketClient: BitbucketClient, artifactLocation: BitbucketArtifactLocation, pullRequestId: Long): Boolean {
        val bitbucketPullRequestCommitMessages: List<Commit> = bitbucketClient
                .getPullRequestCommits(artifactLocation.project, artifactLocation.repository, pullRequestId)
                .map { Commit(it.id, it.message) }

        val gitCommitMessagesFromLastTag: List<Commit> = gitManager
                .getCommitsFromLastTagToHead()
                .map { Commit(it.toObjectId().name(), it.shortMessage) }

        return if (gitCommitMessagesFromLastTag.containsAll(bitbucketPullRequestCommitMessages)) {
            true
        } else {
            log.warn("unknown pull request commits: messages={}",
                    bitbucketPullRequestCommitMessages.filter { !gitCommitMessagesFromLastTag.contains(it) })
            false
        }
    }

    private data class BitbucketArtifactLocation(val host: String, val project: String, val repository: String)

    private data class Commit(val id: String, val message: String) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Commit
            if (id != other.id) return false
            return true
        }

        override fun hashCode(): Int {
            return id.hashCode()
        }
    }
}
