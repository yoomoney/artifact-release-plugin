package ru.yandex.money.gradle.plugins.release

/**
 * Объект расширение DSL gradle для конфигурации плагина
 */
open class ReleaseExtension {
    /**
     * Перечень задач которые нужно выполнить в момент релиза
     */
    var releaseTasks: MutableList<String> = mutableListOf("build")

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
     *  Требовать наличия файла CHANGELOG.md в корне проекта
     */
    var changelogRequired: Boolean = true
}