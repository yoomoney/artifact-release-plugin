package ru.yoomoney.gradle.plugins.release.changelog

import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import ru.yoomoney.gradle.plugins.release.version.ReleaseType
import ru.yoomoney.gradle.plugins.release.version.SemanticVersionEditor
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

    companion object {
        private val log: Logger = Logging.getLogger(ChangelogManager::class.java)
        /**
         * Тип следующего релиза
         */
        const val NEXT_VERSION_TYPE_MARKER = "### NEXT_VERSION_TYPE=MAJOR|MINOR|PATCH"
        /**
         * Маркер начала описания следующего релиза
         */
        const val DESCRIPTION_BEGIN_MARKER = "### NEXT_VERSION_DESCRIPTION_BEGIN"
        /**
         * Маркер окончания описания следующего релиза
         */
        const val DESCRIPTION_END_MARKER = "### NEXT_VERSION_DESCRIPTION_END"
        /**
         * Имя файла с изменениями
         */
        const val DEFAULT_FILE_NAME: String = "CHANGELOG.md"
        /**
         * Маркер секции **breaking changes** при мажорном обновлении
         */
        const val BREAKING_CHANGES_MARKER = "breaking changes"
        private val previousVersionRegexp = Regex("^## \\[(\\d+\\.\\d+\\.\\d+)\\]\\(.*\\)\\s+\\(\\d+-\\d+-\\d+\\)$")
        private val nextVersionTypeRegexp = Regex("^### NEXT_VERSION_TYPE=(MAJOR|MINOR|PATCH)$")
    }

    /**
     * Является ли обновление мажорным
     */
    fun isMajorVersion(): Boolean {
        return getNextVersionType() == ReleaseType.MAJOR
    }

    /**
     * Присутствует ли в CHANGELOG.MD секция **breaking changes**
     */
    fun hasBreakingChangesMarker(): Boolean {
        val description = getNexVersionDescription()
        return description.isNotEmpty() && description.contains(BREAKING_CHANGES_MARKER)
    }

    /**
     * Есть ли в changelog описание следующей версии
     */
    fun hasNextVersionInfo(): Boolean {
        return getNexVersionDescription().isNotEmpty() && getNextVersionType() != null
    }

    private fun getNextVersionType(): ReleaseType? {
        for (line in changeLog.readLines()) {
            val matchResult = nextVersionTypeRegexp.matchEntire(line)
            if (matchResult != null) {
                return ReleaseType.valueOf(matchResult.groupValues[1])
            }
        }
        return null
    }

    private fun getLastVersion(): String? {
        for (line in changeLog.readLines()) {
            val matchEntire = previousVersionRegexp.matchEntire(line)
            if (matchEntire != null) {
                return matchEntire.groupValues[1]
            }
        }
        return null
    }

    /**
     * Определяем будущую версию артифакта исходя из последней версии в changelog и NEXT_VERSION_TYPE
     */
    fun getNextVersion(): String? {
        val nextVersionType = getNextVersionType() ?: return null
        val lastVersion = getLastVersion()
        return SemanticVersionEditor(lastVersion).increment(nextVersionType)
    }

    /**
     * Убирает маркеры, добавляет описание версии и текущую дату
     * @param githubPullRequestLink ссылка на ПР в github, которая будет вставлена при ротации changelog-а
     * @return новая версия
     */
    fun updateToNextVersion(githubPullRequestLink: String?): ChangelogReleaseInfo {
        val nextVersionDescription = getNexVersionDescription()
        val nextVersionType = getNextVersionType()
        if (nextVersionDescription.isEmpty()) {
            throw GradleException("Changelog doesn't have new version description, skip update to next version")
        }
        if (nextVersionType == null) {
            throw GradleException("Changelog doesn't have new version type, skip update to next version")
        }

        val lastVersion = getLastVersion()
        val nextVersion = SemanticVersionEditor(lastVersion).increment(nextVersionType)
        log.lifecycle("Changelog release version info :lastVersion={}, nextVersion={}, type={} description=\n{}",
                lastVersion, nextVersion, nextVersionType, nextVersionDescription)

        val fullChangeLog = changeLog.readLines().stream()
                .filter { !nextVersionTypeRegexp.matches(it.trim()) }
                .collect(Collectors.joining("\n"))
        val indexOfBeginMarker = fullChangeLog.indexOf(DESCRIPTION_BEGIN_MARKER)
        val indexOfEndMarker = fullChangeLog.indexOf(DESCRIPTION_END_MARKER)
        val header = fullChangeLog.substring(0, indexOfBeginMarker)
        val footer = fullChangeLog.substring(indexOfEndMarker + DESCRIPTION_END_MARKER.length)

        val writer = PrintWriter(changeLog)
        if (header.isNotEmpty()) {
            writer.println(header)
        }
        writer.println("## [$nextVersion](${githubPullRequestLink ?: ""}) (${getCurrentDate()})")
        writer.println()
        writer.println(nextVersionDescription)
        writer.print(footer)
        writer.close()
        return ChangelogReleaseInfo(nextVersion, nextVersionDescription)
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

    /**
     * Описание релиза
     * @param releaseVersion версия текущего релиза
     * @param releaseDescriptionMd описание релиза в формате Markdown
     */
    data class ChangelogReleaseInfo(val releaseVersion: String, val releaseDescriptionMd: String)
}
