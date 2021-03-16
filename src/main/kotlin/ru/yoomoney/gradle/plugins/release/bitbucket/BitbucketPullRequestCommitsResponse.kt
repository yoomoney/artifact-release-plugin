package ru.yoomoney.gradle.plugins.release.bitbucket

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.Objects
import javax.annotation.Nonnull

/**
 * Ответ запроса получения коммитов pull request
 *
 * @author Alexander Esipov (asesipov@yamoney.ru)
 * @since 11.02.2020
 */
internal class BitbucketPullRequestCommitsResponse @JsonCreator private constructor(@Nonnull @JsonProperty("values") pullRequestsCommits: List<BitbucketPullRequestCommit>) {
    @get:Nonnull
    @Nonnull
    val pullRequestsCommits: List<BitbucketPullRequestCommit>

    init {
        this.pullRequestsCommits = Objects.requireNonNull(pullRequestsCommits)
    }
}