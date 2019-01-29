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
     * версия будущего артефакта на момент выполнения задач будет лежать в
     * File(project.buildDir,'release/release-version.txt').readText()
     */
    var preReleaseTasks: MutableList<String> = mutableListOf()
    /**
     *  Путь до приватного ssh ключа для дотсупа в git, если задан будет использоваться
     */
    var pathToGitPrivateSshKey: String? = null
}