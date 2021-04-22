package ru.yoomoney.gradle.plugins.release.bitbucket

import java.net.URI
import javax.annotation.Nonnull

/**
 * Настройки подключения к bitbucket
 *
 * @author Oleg Kandaurov
 * @since 28.01.2019
 */
class BitbucketConnectionSettings private constructor(
    @param:Nonnull val uri: URI?,
    @param:Nonnull val apiToken: String?
) {
    /**
     * Базовый урл
     */
    /**
     * Имя пользователя
     */
    /**
     * Пароль пользователя
     */

    override fun toString(): String {
        return "BitbucketConnectionSettings{" +
                "uri=" + uri +
                ", apiToken='***'" +
                '}'
    }

    /**
     * Билдер для [BitbucketConnectionSettings]
     */
    class Builder {
        private var uri: URI? = null
        private var apiToken: String? = null
        fun withUri(@Nonnull uri: URI?): Builder {
            this.uri = uri
            return this
        }

        fun withApiToken(@Nonnull apiToken: String?): Builder {
            this.apiToken = apiToken
            return this
        }

        fun build(): BitbucketConnectionSettings {
            return BitbucketConnectionSettings(uri, apiToken)
        }
    }

    companion object {
        /**
         * Создает новый объект билдера для [BitbucketConnectionSettings]
         *
         * @return new Builder()
         */
        fun builder(): Builder {
            return Builder()
        }
    }
}
