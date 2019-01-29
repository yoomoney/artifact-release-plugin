package ru.yandex.money.gradle.plugins.release

import org.gradle.api.DefaultTask
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.TaskAction
import ru.yandex.money.gradle.plugins.release.changelog.ChangelogManager
import ru.yandex.money.gradle.plugins.release.version.GradlePropertyVersionManager
import ru.yandex.money.gradle.plugins.release.version.ReleaseVersionStorage
import java.io.File
import java.util.Objects

/**
 * Обновляет changelog.md если он есть, убирает snapshot у version в gradle.property
 */
open class PreReleaseRotateVersionTask : DefaultTask() {

    companion object {
        private val log: Logger = Logging.getLogger(PreReleaseRotateVersionTask::class.java)
    }

    private fun preReleaseByChangelog(changelog: File,
                                      gradlePropertyVersionManager: GradlePropertyVersionManager): String {
        val changelogManager = ChangelogManager(changelog)
        val nextVersion = Objects.requireNonNull(changelogManager.updateToNextVersion(), "Changelog has't next version")!!
        gradlePropertyVersionManager.updateVersion(nextVersion)
        return nextVersion
    }

    @TaskAction
    fun rotateVersion() {
        log.lifecycle("Start pre release: currentVersion = {}", project.version)
        val projectVersionManager = GradlePropertyVersionManager(project.file(GradlePropertyVersionManager.DEFAULT_FILE_NAME))
        val changelogFile = project.file(ChangelogManager.DEFAULT_FILE_NAME)
        val releaseVersion = if (changelogFile.exists()) {
            preReleaseByChangelog(changelogFile, projectVersionManager)
        } else {
            log.lifecycle("Changelog rotate skip, ${changelogFile.name} not found")
            projectVersionManager.removeSnapshotFromVersion()
        }
        log.lifecycle("Update ${projectVersionManager.gradleProperty.name}: releaseVersion={}", releaseVersion)
        ReleaseVersionStorage(project.buildDir).storeVersion(releaseVersion)
    }

}