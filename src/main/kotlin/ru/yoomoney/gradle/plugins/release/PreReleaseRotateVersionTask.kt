package ru.yoomoney.gradle.plugins.release

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import ru.yoomoney.gradle.plugins.release.changelog.PullRequestLinkProvider
import ru.yoomoney.gradle.plugins.release.changelog.ChangelogManager
import ru.yoomoney.gradle.plugins.release.git.GitManager
import ru.yoomoney.gradle.plugins.release.git.GitSettings
import ru.yoomoney.gradle.plugins.release.version.GradlePropertyVersionManager
import ru.yoomoney.gradle.plugins.release.version.ReleaseInfoStorage
import java.io.File

/**
 * Обновляет changelog.md если он есть, убирает snapshot у version в gradle.property
 */
open class PreReleaseRotateVersionTask : DefaultTask() {

    companion object {
        private val log: Logger = Logging.getLogger(PreReleaseRotateVersionTask::class.java)
    }

    @get:Input
    lateinit var gitSettings: GitSettings
    @get:Input
    lateinit var pullRequestLinkSettings: PullRequestLinkSettings

    private fun preReleaseByChangelog(
        changelog: File,
        gradlePropertyVersionManager: GradlePropertyVersionManager,
        releaseInfoStorage: ReleaseInfoStorage,
        gitManager: GitManager
    ): String {
        val changelogManager = ChangelogManager(changelog)

        val githubPullRequestLink: String? =
                if (pullRequestLinkSettings.pullRequestLinkInChangelogEnabled)
                    PullRequestLinkProvider(gitManager, pullRequestLinkSettings).getReleasePullRequestLink()
                else null
        val nextVersion = changelogManager.updateToNextVersion(githubPullRequestLink)

        gradlePropertyVersionManager.updateVersion(nextVersion.releaseVersion)
        releaseInfoStorage.storeChangelog(nextVersion.releaseDescriptionMd)
        return nextVersion.releaseVersion
    }

    @TaskAction
    fun rotateVersion() {
        val gitManager = GitManager(project.rootDir, gitSettings)

        val uncommittedChanges = gitManager.getUncommittedChanges()
        if (!uncommittedChanges.isEmpty()) {
            throw GradleException("There are uncommitted changes \n" + uncommittedChanges.joinToString("\n"))
        }

        log.lifecycle("Start pre release: currentVersion = {}", project.version)
        val projectVersionManager =
                GradlePropertyVersionManager(project.file(GradlePropertyVersionManager.DEFAULT_FILE_NAME))
        val changelogFile = project.file(ChangelogManager.DEFAULT_FILE_NAME)
        val releaseInfoStorage = ReleaseInfoStorage(project.buildDir)
        val releaseVersion = if (changelogFile.exists()) {
            preReleaseByChangelog(changelogFile, projectVersionManager, releaseInfoStorage, gitManager)
        } else {
            log.lifecycle("Changelog rotate skip, ${changelogFile.name} not found")
            projectVersionManager.removeSnapshotFromVersion()
        }
        log.lifecycle("Update ${projectVersionManager.gradleProperty.name}: releaseVersion={}", releaseVersion)
        releaseInfoStorage.storeVersion(releaseVersion)
    }
}
