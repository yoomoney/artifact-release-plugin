package ru.yandex.money.gradle.plugins.release.version

import java.io.File

/**
 * Умеет работать с gradle.properties
 */
class GradlePropertyVersionManager(val gradleProperty: File) {

    companion object {
        /**
         * Имя файл gradle.properties
         */
        const val DEFAULT_FILE_NAME: String = "gradle.properties"
        private const val SNAPSHOT: String = "-SNAPSHOT"
        private val VERSION_REGEX: Regex = Regex("^\\s*version\\s*=\\s*(.+)$")
    }

    /**
     * @return текущая версия в [gradleProperty]
     */
    fun getCurrentVersion(): String {
        val versionLine = gradleProperty.readLines().first { it.matches(VERSION_REGEX) }
        return VERSION_REGEX.matchEntire(versionLine)!!.groupValues[1].trim()
    }

    /**
     * Удаляет постфикс -SNAPSHOT в версии из файла [gradleProperty]
     *
     * @return новая версия
     */
    fun removeSnapshotFromVersion(): String {
        val currentVersion = getCurrentVersion()
        if (currentVersion.endsWith(SNAPSHOT)) {
            val newVersion = currentVersion.replace(SNAPSHOT, "")
            updateVersion(newVersion)
            return newVersion
        }
        return currentVersion
    }

    /**
     * Добавляет постфикс -SNAPSHOT к версии из файла [gradleProperty]
     * @return новая версия
     */
    fun appendSnapshotToVersion(): String {
        val currentVersion = getCurrentVersion()

        if (!currentVersion.endsWith(SNAPSHOT)) {
            val nextVersion = currentVersion + SNAPSHOT
            updateVersion(nextVersion)
            return nextVersion
        }
        return currentVersion
    }

    /**
     * Устанавливает новую версию
     * @param newVersion новая версия
     */
    fun updateVersion(newVersion: String) {
        val lines = gradleProperty.readLines()
        gradleProperty.printWriter().use { out ->
            lines.forEach {
                if (it.matches(VERSION_REGEX)) {
                    out.println("version=$newVersion")
                } else {
                    out.println(it)
                }
            }
        }
    }

    /**
     * Поднимает patch версию с сохранением постфикса -SNAPSHOT
     * @param newVersion новая версия
     */
    fun incrementPatchVersion(): String {
        val currentVersion = getCurrentVersion()
        val versionWithoutSnapshot = if (currentVersion.endsWith(SNAPSHOT)) {
            currentVersion.replace(SNAPSHOT, "")
        } else {
            currentVersion
        }
        val version = SemanticVersionEditor(versionWithoutSnapshot)
        var nextVersion = version.increment(ReleaseType.PATCH)

        if (currentVersion.endsWith(SNAPSHOT)) {
            nextVersion += SNAPSHOT
        }
        updateVersion(nextVersion)
        return nextVersion
    }
}
