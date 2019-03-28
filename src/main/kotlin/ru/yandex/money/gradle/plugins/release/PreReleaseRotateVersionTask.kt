package ru.yandex.money.gradle.plugins.release

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.TaskAction
import ru.yandex.money.gradle.plugins.release.changelog.ChangelogManager
import ru.yandex.money.gradle.plugins.release.git.GitReleaseManager
import ru.yandex.money.gradle.plugins.release.version.GradlePropertyVersionManager
import ru.yandex.money.gradle.plugins.release.version.ReleaseInfoStorage
import java.io.File

/**
 * Обновляет changelog.md если он есть, убирает snapshot у version в gradle.property
 */
open class PreReleaseRotateVersionTask : DefaultTask() {

    companion object {
        private val log: Logger = Logging.getLogger(PreReleaseRotateVersionTask::class.java)
    }

    private fun preReleaseByChangelog(changelog: File,
                                      gradlePropertyVersionManager: GradlePropertyVersionManager,
                                      releaseInfoStorage: ReleaseInfoStorage): String {
        val changelogManager = ChangelogManager(changelog)
        val nextVersion = changelogManager.updateToNextVersion()
        gradlePropertyVersionManager.updateVersion(nextVersion.releaseVersion)
        releaseInfoStorage.storeChangelog(nextVersion.releaseDescriptionMd)
        return nextVersion.releaseVersion
    }

    @TaskAction
    fun rotateVersion() {
        if(GitReleaseManager(project.rootDir).hasUncommitedChanges()) {
            throw GradleException("There are uncommited changes")
        }

        log.lifecycle("Start pre release: currentVersion = {}", project.version)
        val projectVersionManager = GradlePropertyVersionManager(project.file(GradlePropertyVersionManager.DEFAULT_FILE_NAME))
        val changelogFile = project.file(ChangelogManager.DEFAULT_FILE_NAME)
        val releaseInfoStorage = ReleaseInfoStorage(project.buildDir)
        val releaseVersion = if (changelogFile.exists()) {
            preReleaseByChangelog(changelogFile, projectVersionManager, releaseInfoStorage)
        } else {
            log.lifecycle("Changelog rotate skip, ${changelogFile.name} not found")
            projectVersionManager.removeSnapshotFromVersion()
        }
        log.lifecycle("Update ${projectVersionManager.gradleProperty.name}: releaseVersion={}", releaseVersion)
        releaseInfoStorage.storeVersion(releaseVersion)
    }

}