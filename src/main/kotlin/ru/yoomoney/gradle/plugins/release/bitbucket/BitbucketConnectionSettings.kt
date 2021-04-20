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
    @param:Nonnull val user: String?,
    @param:Nonnull val password: String?
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
                ", user='" + user + '\'' +
                ", password='***'" +
                '}'
    }

    /**
     * Билдер для [BitbucketConnectionSettings]
     */
    class Builder {
        private var uri: URI? = null
        private var user: String? = null
        private var password: String? = null
        fun withUri(@Nonnull uri: URI?): Builder {
            this.uri = uri
            return this
        }

        fun withUser(@Nonnull user: String?): Builder {
            this.user = user
            return this
        }

        fun withPassword(@Nonnull password: String?): Builder {
            this.password = password
            return this
        }

        fun build(): BitbucketConnectionSettings {
            return BitbucketConnectionSettings(uri, user, password)
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
