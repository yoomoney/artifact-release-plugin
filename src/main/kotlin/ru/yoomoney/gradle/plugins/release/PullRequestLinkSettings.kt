package ru.yoomoney.gradle.plugins.release

/**
 * Настройки добавления ссылки на pull request в CHANGELOG.md
 */
data class PullRequestLinkSettings(
    /**
     *  Добавлять ссылку на github pull request в CHANGELOG.md при релизе
     */
    var pullRequestLinkInChangelogEnabled: Boolean,
    /**
     *  Токен пользователя github
     */
    var githubAccessToken: String? = null
)
