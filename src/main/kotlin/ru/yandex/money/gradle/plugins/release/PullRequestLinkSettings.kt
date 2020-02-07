package ru.yandex.money.gradle.plugins.release

/**
 * Настройки добавления ссылки на pull request в CHANGELOG.md
 */
data class PullRequestLinkSettings(
    /**
     *  Добавлять ссылку на bitbucket pull request в CHANGELOG.md при релизе
     */
    var pullRequestLinkInChangelogEnabled: Boolean,
    /**
     *  Логин пользователя bitbucket
     */
    var bitbucketUser: String? = null,
    /**
     *  Пароль пользователя bitbucket
     */
    var bitbucketPassword: String? = null
)
