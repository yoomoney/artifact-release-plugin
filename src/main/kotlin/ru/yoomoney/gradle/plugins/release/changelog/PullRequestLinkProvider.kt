package ru.yoomoney.gradle.plugins.release.changelog

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.kohsuke.github.GHIssueState
import ru.yoomoney.gradle.plugins.release.PullRequestInfoProvider
import ru.yoomoney.gradle.plugins.release.PullRequestLinkSettings
import ru.yoomoney.gradle.plugins.release.bitbucket.BitbucketClient
import ru.yoomoney.gradle.plugins.release.bitbucket.BitbucketConnectionSettings
import ru.yoomoney.gradle.plugins.release.bitbucket.PullRequestState
import ru.yoomoney.gradle.plugins.release.git.GitManager
import ru.yoomoney.gradle.plugins.release.github.GitHubClient
import java.net.URI

/**
 * Предоставляет ссылку на pull request релиза
 *
 * @author Alexander Esipov
 * @since 13.02.2020
 */
class PullRequestLinkProvider(private val gitManager: GitManager,
                              private val gitHubClient: GitHubClient,
                              private val settings: PullRequestLinkSettings) {

    constructor(gitManager: GitManager, pullRequestLinkSettings: PullRequestLinkSettings) :
            this(gitManager, GitHubClient(pullRequestLinkSettings), pullRequestLinkSettings)

    companion object {
        private val log: Logger = Logging.getLogger(PullRequestInfoProvider::class.java)
    }

    /**
     * Возвращает ссылку на pull request изменения артефакта с последнего релиза
     */
    fun getReleasePullRequestLink(): String? {
        when (settings.pullRequestInfoProvider) {
            PullRequestInfoProvider.GIT_HUB -> return getReleasePullRequestLinkFromGitHub()
            PullRequestInfoProvider.BITBUCKET -> return getReleasePullRequestLinkFromBitbucket()
            else -> return null
        }
    }

    /**
     * Возвращает ссылку на pull request изменения артефакта с последнего релиза
     */
    fun getReleasePullRequestLinkFromBitbucket(): String? {
        return try {
            val artifactLocation = parseArtifactLocation(gitManager.getRemoteOriginUrl())

            val bitbucketClient = BitbucketClient(BitbucketConnectionSettings.builder()
                    .withUri(URI.create(artifactLocation.host))
                    .withUser(settings.bitbucketUser
                            ?: throw IllegalArgumentException("bitbucketUser is absent"))
                    .withPassword(settings.bitbucketPassword
                            ?: throw IllegalArgumentException("bitbucketPassword is absent"))
                    .build())

            return bitbucketClient
                    .getLatestPullRequestLink(artifactLocation.project, artifactLocation.repository, PullRequestState.MERGED)
                    .filter {
                        val bitbucketPullRequestCommitMessages: List<String> = bitbucketClient
                                .getPullRequestCommits(artifactLocation.project, artifactLocation.repository, it.pullRequestId!!)
                                .map { it.id!! }
                        isPullRequestValid(bitbucketPullRequestCommitMessages)
                    }
                    .map { it.link }
                    .orElse(null)
        } catch (e: Exception) {
            log.warn("can't getPullRequestLink", e)
            null
        }
    }

    /**
     * Возвращает ссылку на pull request изменения артефакта с последнего релиза
     */
    fun getReleasePullRequestLinkFromGitHub(): String? {
        return try {
            val location = parseArtifactLocation(gitManager.getRemoteOriginUrl())

            val latestPullRequest = gitHubClient
                    .getLatestPullRequest(location.project, location.repository, GHIssueState.CLOSED)
                    ?: return null

            val githubPullRequestCommitMessages = latestPullRequest
                    .listCommits().toList()
                    .map { it.sha }

            if (!isPullRequestValid(githubPullRequestCommitMessages)) {
                return null;
            }

            return latestPullRequest.url.toString()

        } catch (e: Exception) {
            log.warn("can't getPullRequestLink", e)
            null
        }
    }

    private fun parseArtifactLocation(path: String): ArtifactLocation {
        if (path.startsWith("http")) {
            return parseHttpArtifactLocation(URI(path))
        }

        return parseSshArtifactLocation(path)
    }

    private fun parseHttpArtifactLocation(url: URI): ArtifactLocation {
        val pathFragments = url.path.split("/")
        val scheme = if (url.scheme.startsWith("http")) url.scheme + "://" else ""
        return ArtifactLocation(
                host = "$scheme${url.host}:${url.port}",
                project = pathFragments[1],
                repository = pathFragments[2].removeSuffix(".git")
        )
    }

    private fun parseSshArtifactLocation(path: String): ArtifactLocation {
        val pathFragments = path.split("@", ":", "/")

        return ArtifactLocation(
                host = pathFragments[1],
                project = pathFragments[2],
                repository = pathFragments[3].removeSuffix(".git")
        )
    }

    /**
     * Проверяет, что коммиты пулриквеста соответсвуют коммитам в git с момента последнего релиза (тэга)
     */
    private fun isPullRequestValid(commitsId: List<String>): Boolean {
        val gitCommitMessagesFromLastTag = gitManager
                .getCommitsFromLastTagToHead()
                .map { it.toObjectId().name() }

        return if (gitCommitMessagesFromLastTag.containsAll(commitsId)) {
            true
        } else {
            log.warn("unknown pull request commits: messages={}",
                    commitsId.filter { !gitCommitMessagesFromLastTag.contains(it) })
            false
        }
    }

    private data class ArtifactLocation(val host: String, val project: String, val repository: String)
}
