package ru.yoomoney.gradle.plugins.release.changelog

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 * Проверяет наличие описания новой версии в changelog
 */
open class CheckChangeLogTask : DefaultTask() {

    @get:Input
    var changelogRequired: Boolean = true

    companion object {
        val log: Logger = Logging.getLogger(CheckChangeLogTask::class.java)
    }

    @TaskAction
    fun doCheck() {
        val file = project.file(ChangelogManager.DEFAULT_FILE_NAME)
        if (!file.exists()) {
            log.lifecycle("${ChangelogManager.DEFAULT_FILE_NAME} is absent")
            if (changelogRequired) {
                throw GradleException("Создайте в корне проекта файл ${ChangelogManager.DEFAULT_FILE_NAME}")
            }
            return
        }
        val changelogManager = ChangelogManager(file)

        checkNextVersionInfo(changelogManager)
        checkBreakingChangesSection(changelogManager)
    }

    private fun checkNextVersionInfo(changelogManager: ChangelogManager) {
        if (!changelogManager.hasNextVersionInfo()) {
            throw GradleException("В ${ChangelogManager.DEFAULT_FILE_NAME} отсутствует описание следующей версии, " +
                    "добавьте описание между " +
                    "${ChangelogManager.DESCRIPTION_BEGIN_MARKER} и ${ChangelogManager.DESCRIPTION_END_MARKER}, " +
                    "выберите тип релиза в ${ChangelogManager.NEXT_VERSION_TYPE_MARKER}")
        }
    }

    private fun checkBreakingChangesSection(changelogManager: ChangelogManager) {
        if (!changelogManager.hasBreakingChangesWhenMajor()) {
            throw GradleException("При мажорном обновлении, в ${ChangelogManager.DEFAULT_FILE_NAME}, между секциями " +
                    "${ChangelogManager.DESCRIPTION_BEGIN_MARKER} и ${ChangelogManager.DESCRIPTION_END_MARKER} " +
                    "необходимо добавить описание в формате: \"**breaking changes** Проделанные изменения\". " +
                    "Также настоятельно рекомендуется указать в описании, какие изменения требуется сделать, " +
                    "чтобы перейти на новую версию. Пример оформления см.: " +
                    "https://github.com/yoomoney-gradle-plugins/artifact-release-plugin/blob/master/README.md")
        }
    }
}
