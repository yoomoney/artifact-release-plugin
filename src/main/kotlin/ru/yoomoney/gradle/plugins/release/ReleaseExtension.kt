package ru.yoomoney.gradle.plugins.release

/**
 * Объект расширение DSL gradle для конфигурации плагина
 */
open class ReleaseExtension {
    /**
     * Перечень задач которые нужно выполнить в момент релиза
     */
    var releaseTasks: MutableList<String> = mutableListOf()

    /**
     * Перечень задач которые нужно выполнить до релиза,
     * версию и changelog будущего артефакта можно получить через `ReleaseInfoStorage(project.buildDir)`
     */

    var preReleaseTasks: MutableList<String> = mutableListOf()
    /**
     *  Путь до приватного ssh ключа для дотсупа в git, если задан будет использоваться
     */
    var pathToGitPrivateSshKey: String? = null

    /**
     *  Passphrase для приватного ssh ключа. По умолчанию не задана
     */
    var passphraseToGitPrivateSshKey: String? = null

    /**
     *  Имя юзера, от которого будет производиться коммит в git
     */
    var gitUsername: String? = null

    /**
     *  Email юзера, от которого будет производиться коммит в git
     */
    var gitEmail: String? = null

    /**
     *  Требовать наличия файла CHANGELOG.md в корне проекта
     */
    var changelogRequired: Boolean = true

    /**
     *  Добавлять ссылку на pull request в CHANGELOG.md при релизе
     */
    var addPullRequestLinkToChangelog: Boolean = true

    /**
     *  Сервис предоставляющий информацию о пулл-реквестах. Может быть Bitbucket или GitHub.
     *  По умолчанию Bitbucket.
     */
    var pullRequestInfoProvider: String = PullRequestInfoProvider.BITBUCKET.value

    /**
     *  Токен пользователя github. Обязательный, если pullRequestInfoProvider = GitHub
     *  Генерируется в настройках gh "Developer settings -> Personal access tokens."
     *  Токен должен иметь доступ к разделу "repo"
     */
    var githubAccessToken: String? = null

    /**
     *  Логин пользователя bitbucket. Обязательный, если pullRequestInfoProvider = Bitbucket
     */
    var bitbucketUser: String? = null

    /**
     *  Пароль пользователя bitbucket. Обязательный, если pullRequestInfoProvider = Bitbucket
     */
    var bitbucketPassword: String? = null
}
