package ru.yandex.money.gradle.plugins.release.version

import java.io.File

/**
 * Хранит информацию о релизе в файловой системе
 * @param buildDir директория где происходит сборка проекта [org.gradle.api.Project.getBuildDir]
 */
class ReleaseInfoStorage(private val buildDir: File) {

    companion object {
        private const val RELEASE_VERSION_PATH = "release/release-version.txt"
        private const val RELEASE_CHANGE_LOG_PATH = "release/release-changelog.md"
    }

    /**
     * Сохраняет версию в файловую систему
     *
     * @param version версия для сохранения
     */
    fun storeVersion(version: String) {
        val releaseVersionFile = File(buildDir, RELEASE_VERSION_PATH)

        releaseVersionFile.parentFile.mkdirs()
        releaseVersionFile.writeText(version)
    }

    /**
     * Сохраняет Changelog описывающий текущий релиз в файловую систему
     *
     * @return Changelog в формате Markdown
     */
    fun storeChangelog(changelog: String) {
        val changelogFile = File(buildDir, RELEASE_CHANGE_LOG_PATH)

        changelogFile.parentFile.mkdirs()
        changelogFile.writeText(changelog)
    }

    /**
     * Загружает Changelog описывающий текущий релиз из файловой системы
     *
     * @return Changelog описывающий текущий релиз
     */
    fun loadChangelog(): String? {
        val changelogFile = File(buildDir, RELEASE_CHANGE_LOG_PATH)
        return if (changelogFile.exists()) {
            changelogFile.readText()
        } else {
            null
        }
    }

    /**
     * Загружает версию из файловой системы
     *
     * @return версия текущего релиза
     */
    fun loadVersion(): String? {
        val releaseVersionFile = File(buildDir, RELEASE_VERSION_PATH)
        return if (releaseVersionFile.exists()) {
            releaseVersionFile.readText()
        } else {
            null
        }
    }
}
