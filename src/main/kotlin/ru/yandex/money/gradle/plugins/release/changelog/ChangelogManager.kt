package ru.yandex.money.gradle.plugins.release.changelog

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import java.io.File
import java.io.PrintWriter
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.Date
import java.util.stream.Collectors


/**
 * Умеет работать с changelog.md
 */
class ChangelogManager(private val changeLog: File) {

    private enum class ReleaseType(private val index: Int) {
        MAJOR(0),
        MINOR(1),
        PATCH(2);

        fun upVersion(currentVersionArg: String?): String {
            val currentVersion = currentVersionArg ?: "0.0.0"
            val versionParts = currentVersion.split(".").toMutableList()
            versionParts[index] = (versionParts[index].toInt() + 1).toString()
            for (i in index + 1..2) {
                versionParts[i] = "0"
            }
            return versionParts.joinToString(".")
        }
    }

    companion object {
        private val log: Logger = Logging.getLogger(ChangelogManager::class.java)
        /**
         * Тип следующего релиза
         */
        const val NEXT_VERSION_TYPE_MARKER = "%% NEXT_VERSION_TYPE=MAJOR|MINOR|PATCH"
        /**
         * Маркер начала описания следующего релиза
         */
        const val DESCRIPTION_BEGIN_MARKER = "%% NEXT_VERSION_DESCRIPTION_BEGIN"
        /**
         * Маркер окончания описания следующего релиза
         */
        const val DESCRIPTION_END_MARKER = "%% NEXT_VERSION_DESCRIPTION_END"
        /**
         * Имя файла с изменениями
         */
        const val DEFAULT_FILE_NAME: String = "CHANGELOG.md"
        private val PREVIOUS_VERSION_REGEXP: Regex = Regex("^## \\[(\\d+\\.\\d+\\.\\d+)\\]\\(\\)\\s+\\(\\d+-\\d+-\\d+\\)$")
        private val NEXT_VERSION_TYPE_REGEXP: Regex = Regex("^%% NEXT_VERSION_TYPE=(MAJOR|MINOR|PATCH)$")
    }


    /**
     * Есть ли в changelog описание следующей версии
     */
    fun hasNextVersionInfo(): Boolean {
        return !getNexVersionDescription().isEmpty() && getNextVersionType() != null
    }

    private fun getNextVersionType(): ReleaseType? {
        for (line in changeLog.readLines()) {
            val matchResult = NEXT_VERSION_TYPE_REGEXP.matchEntire(line)
            if (matchResult != null) {
                return ReleaseType.valueOf(matchResult.groupValues[1])
            }
        }
        return null
    }


    private fun getLastVersion(): String? {
        for (line in changeLog.readLines()) {
            val matchEntire = PREVIOUS_VERSION_REGEXP.matchEntire(line)
            if (matchEntire != null) {
                return matchEntire.groupValues[1]

            }
        }
        return null
    }

    /**
     * Убирает маркеры, добавляет описание версии и текущую дату
     * @return новая версия
     */
    fun updateToNextVersion(): String? {
        val nextVersionDescription = getNexVersionDescription()
        val nextVersionType = getNextVersionType()
        if (nextVersionDescription.isEmpty()) {
            log.lifecycle("Changelog doesn't have new version description, skip update to next version")
            return null
        }
        if (nextVersionType == null) {
            log.lifecycle("Changelog doesn't have not new version type, skip update to next version")
            return null
        }
        val lastVersion = getLastVersion()
        val nextVersion = nextVersionType.upVersion(lastVersion)
        log.lifecycle("Changelog next version info :lastVersion={}, nextVersion={}, type={} description=\n{}",
                lastVersion, nextVersion, nextVersionType, nextVersionDescription)

        val fullChangeLog = changeLog.readLines().stream()
                .filter { !NEXT_VERSION_TYPE_REGEXP.matches(it.trim()) }
                .collect(Collectors.joining("\n"))
        val indexOfBeginMarker = fullChangeLog.indexOf(DESCRIPTION_BEGIN_MARKER)
        val indexOfEndMarker = fullChangeLog.indexOf(DESCRIPTION_END_MARKER)
        val header = fullChangeLog.substring(0, indexOfBeginMarker)
        val footer = fullChangeLog.substring(indexOfEndMarker + DESCRIPTION_END_MARKER.length)

        val writer = PrintWriter(changeLog)
        if (!header.isEmpty()) {
            writer.println(header)
        }
        writer.println("## [$nextVersion]() (${getCurrentDate()})")
        writer.println()
        writer.println(nextVersionDescription)
        writer.print(footer)
        writer.close()
        return nextVersion
    }

    private fun getCurrentDate(): String {
        return SimpleDateFormat("dd-MM-YYYY").format(Date())
    }

    /**
     * Добавляет маркеры для описания следующей версии
     */
    fun appendNextVersionDescriptionMarkers() {
        val newChangeLogContent = NEXT_VERSION_TYPE_MARKER + "\n" +
                DESCRIPTION_BEGIN_MARKER + "\n" +
                DESCRIPTION_END_MARKER + "\n" +
                readContent()
        Files.write(changeLog.toPath(), newChangeLogContent.toByteArray())
    }

    private fun readContent() = changeLog.readLines().joinToString("\n")


    private fun getNexVersionDescription(): String {
        val result = StringBuilder()
        var afterBegin = false

        for (line in changeLog.readLines()) {
            val trimmedLine = line.trim()
            if (!afterBegin && trimmedLine == DESCRIPTION_BEGIN_MARKER) {
                afterBegin = true
                continue
            }
            if (afterBegin && trimmedLine == DESCRIPTION_END_MARKER) {
                afterBegin = false
                break
            }
            if (afterBegin) {
                result.append(trimmedLine).append('\n')
            }
        }
        if (afterBegin) {
            // забыт DESCRIPTION_END_MARKER
            return ""
        }
        return result.toString().trim()
    }
}



