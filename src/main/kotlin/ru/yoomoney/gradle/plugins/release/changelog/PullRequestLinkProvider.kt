package ru.yoomoney.gradle.plugins.release.changelog

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.kohsuke.github.GHIssueState
import org.kohsuke.github.GHPullRequest
import ru.yoomoney.gradle.plugins.release.PullRequestLinkSettings
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
                              private val gitHubClient: GitHubClient) {

    constructor(gitManager: GitManager, pullRequestLinkSettings: PullRequestLinkSettings) :
            this(gitManager, GitHubClient(pullRequestLinkSettings))

    companion object {
        private val log: Logger = Logging.getLogger(PullRequestLinkProvider::class.java)
    }

    /**
     * Возвращает ссылку на pull request изменения артефакта с последнего релиза
     */
    fun getReleasePullRequestLink(): String? {
        return try {
            val location = parseGitHubLocation(URI(gitManager.getRemoteOriginUrl()))

            val latestPullRequest = gitHubClient
                    .getLatestPullRequest(location.project, location.repository, GHIssueState.CLOSED)
                    ?: return null

            if (!isPullRequestValid(latestPullRequest)) {
                return null;
            }

            return latestPullRequest.url.toString()

        } catch (e: Exception) {
            log.warn("can't getPullRequestLink", e)
            null
        }
    }

    private fun parseGitHubLocation(url: URI): GithubArtifactLocation {
        val pathFragments = url.path.split("/")
        val scheme = if (url.scheme.startsWith("http")) url.scheme + "://" else ""
        return GithubArtifactLocation(
                host = "$scheme${url.host}:${url.port}",
                project = pathFragments[1],
                repository = pathFragments[2].removeSuffix(".git")
        )
    }

    /**
     * Проверяет, что коммиты пулриквеста соответсвуют коммитам в git с момента последнего релиза (тэга)
     */
    private fun isPullRequestValid(pullRequest: GHPullRequest): Boolean {
        val githubPullRequestCommitMessages = pullRequest
                .listCommits().toList()
                .map { it.sha }

        val gitCommitMessagesFromLastTag = gitManager
                .getCommitsFromLastTagToHead()
                .map { it.toObjectId().name() }

        return if (gitCommitMessagesFromLastTag.containsAll(githubPullRequestCommitMessages)) {
            true
        } else {
            log.warn("unknown pull request commits: messages={}",
                    githubPullRequestCommitMessages.filter { !gitCommitMessagesFromLastTag.contains(it) })
            false
        }
    }

    private data class GithubArtifactLocation(val host: String, val project: String, val repository: String)
}
