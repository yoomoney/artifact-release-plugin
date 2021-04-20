package ru.yoomoney.gradle.plugins.release.bitbucket

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.Objects
import javax.annotation.Nonnull

/**
 * Ответ битбакета, содержащий инфу о ПР
 * Содержит не все поля ответа битбакета, а только те что сейчас потребовались.
 *
 * @author lokshin (lokshin@yamoney.ru)
 * @since 01.02.2019
 */
class BitbucketPullRequestInfoResponse @JsonCreator private constructor(
    @Nonnull @JsonProperty("values") pullRequests: List<BitbucketPullRequest?>
) {
    @Nonnull
    private val pullRequests: List<BitbucketPullRequest?>

    @Nonnull
    fun getPullRequests(): List<BitbucketPullRequest?> {
        return pullRequests
    }

    override fun toString(): String {
        return "BitbucketPullRequestInfoResponse{" +
                "pullRequests=" + pullRequests +
                '}'
    }

    init {
        this.pullRequests = Objects.requireNonNull<List<BitbucketPullRequest?>>(pullRequests)
    }
}
