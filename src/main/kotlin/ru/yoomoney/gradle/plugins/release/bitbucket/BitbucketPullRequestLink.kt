package ru.yoomoney.gradle.plugins.release.bitbucket

import java.util.*
import javax.annotation.Nonnull

/**
 * Ссылка на пул риквест в bitbucket
 *
 * @author Alexander Esipov (asesipov@yamoney.ru)
 * @since 06.02.2020
 */
class BitbucketPullRequestLink private constructor(@Nonnull pullRequestId: Long?,
                                                   @Nonnull link: String?) {
    @get:Nonnull
    @Nonnull
    val pullRequestId: Long?
    @get:Nonnull
    @Nonnull
    val link: String?

    override fun toString(): String {
        return "PullRequestLink{" +
                "pullRequestId=" + pullRequestId +
                ", link='" + link + '\'' +
                '}'
    }

    /**
     * Билдер для [BitbucketPullRequestLink]
     */
    class Builder {
        private var pullRequestId: Long? = null
        private var link: String? = null
        fun withPullRequestId(@Nonnull pullRequestId: Long?): Builder {
            this.pullRequestId = pullRequestId
            return this
        }

        fun withLink(@Nonnull link: String?): Builder {
            this.link = link
            return this
        }

        /**
         * Собрать объект
         */
        @Nonnull
        fun build(): BitbucketPullRequestLink {
            return BitbucketPullRequestLink(pullRequestId, link)
        }
    }

    companion object {
        /**
         * Создает новый объект билдера для [BitbucketPullRequestLink]
         */
        @Nonnull
        fun builder(): Builder {
            return Builder()
        }
    }

    init {
        this.pullRequestId = Objects.requireNonNull(pullRequestId, "pullRequestId")
        this.link = Objects.requireNonNull(link, "link")
    }
}