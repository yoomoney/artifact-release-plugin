package ru.yoomoney.gradle.plugins.release

/**
 * Настройки добавления ссылки на pull request в CHANGELOG.md
 */
data class PullRequestLinkSettings(
    /**
     *  Добавлять ссылку на pull request в CHANGELOG.md при релизе
     */
    var pullRequestLinkInChangelogEnabled: Boolean,

    /**
     *  GitServiceType
     */
    var pullRequestInfoProvider: PullRequestInfoProvider = PullRequestInfoProvider.BITBUCKET,

    /**
     *  Токен пользователя github
     */
    var githubAccessToken: String? = null,

    /**
     *  Логин пользователя bitbucket
     */
     var bitbucketUser: String? = null,
    /**
     *  Пароль пользователя bitbucket
     */
     var bitbucketPassword: String? = null
)

/**
 * Провайдер информации о pull-request
 */
enum class PullRequestInfoProvider(val value: String) {
    GIT_HUB("GitHub"), BITBUCKET("Bitbucket");

    companion object {
        private val map = values().associateBy(PullRequestInfoProvider::value)
        fun fromName(value: String) = map[value]
    }
}