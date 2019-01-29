package ru.yandex.money.gradle.plugins.release.version

import java.io.File


/**
 * Хранит версию в файловой системе
 * @param buildDir директория где происходит сборка проекта [org.gradle.api.Project.getBuildDir]
 */
class ReleaseVersionStorage(private val buildDir: File) {

    companion object {
        private const val RELEASE_VERSION_PATH = "release/release-version.txt"
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
     * Загружает версию из файловой системы
     *
     * @return версия из файловой системы
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