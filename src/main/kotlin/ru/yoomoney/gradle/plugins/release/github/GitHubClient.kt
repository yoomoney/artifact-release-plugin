package ru.yoomoney.gradle.plugins.release.github

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.kohsuke.github.GHIssueState
import org.kohsuke.github.GHPullRequest
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder
import org.kohsuke.github.GHPullRequestQueryBuilder
import org.kohsuke.github.GHDirection
import ru.yoomoney.gradle.plugins.release.PreReleaseRotateVersionTask
import ru.yoomoney.gradle.plugins.release.PullRequestLinkSettings

/**
 * Клиент для работы с GitHub
 *
 * @author horyukova
 * @since 18.12.2020
 */
class GitHubClient(private val github: GitHub) {

    constructor(pullRequestLinkSettings: PullRequestLinkSettings) :
            this(GitHubBuilder()
                    .withOAuthToken(pullRequestLinkSettings.githubAccessToken)
                    .build())

    companion object {
        private val log: Logger = Logging.getLogger(PreReleaseRotateVersionTask::class.java)
    }

    fun getLatestPullRequest(project: String, repository: String, state: GHIssueState?): GHPullRequest? {
        try {
            return github.getRepository("$project/$repository").queryPullRequests()
                    .state(state)
                    .sort(GHPullRequestQueryBuilder.Sort.UPDATED)
                    .direction(GHDirection.DESC)
                    .list()
                    .withPageSize(1)
                    .toList()
                    .firstOrNull()
        } catch (ex: Exception) {
            log.lifecycle("Can't call github")
            return null
        }
    }
}
