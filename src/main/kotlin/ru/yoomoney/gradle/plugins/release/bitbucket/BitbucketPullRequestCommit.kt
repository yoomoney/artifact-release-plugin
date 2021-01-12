package ru.yoomoney.gradle.plugins.release.bitbucket

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import javax.annotation.Nonnull

/**
 * Коттит пул риквеста
 *
 * @author Alexander Esipov (asesipov@yamoney.ru)
 * @since 06.02.2020
 */
class BitbucketPullRequestCommit @JsonCreator private constructor(@field:Nonnull @get:Nonnull
                                                                  @get:JsonProperty("id")
                                                                  @param:Nonnull @param:JsonProperty("id") val id: String?,
                                                                  @field:Nonnull @get:Nonnull
                                                                  @get:JsonProperty("message")
                                                                  @param:Nonnull @param:JsonProperty("message") val message: String?) {

    override fun toString(): String {
        return "BitbucketPullRequestCommit{" +
                "id='" + id + '\'' +
                ", message='" + message + '\'' +
                '}'
    }

    /**
     * Билдер для [BitbucketPullRequestCommit]
     */
    class Builder {
        private var id: String? = null
        private var message: String? = null
        fun withId(@Nonnull id: String?): Builder {
            this.id = id
            return this
        }

        fun withMessage(@Nonnull message: String?): Builder {
            this.message = message
            return this
        }

        /**
         * Собрать объект
         */
        @Nonnull
        fun build(): BitbucketPullRequestCommit {
            return BitbucketPullRequestCommit(id, message)
        }
    }

    companion object {
        /**
         * Создает новый объект билдера для [BitbucketPullRequestCommit]
         */
        @Nonnull
        fun builder(): Builder {
            return Builder()
        }
    }

}