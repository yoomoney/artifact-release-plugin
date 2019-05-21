package ru.yandex.money.gradle.plugins.release

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import ru.yandex.money.gradle.plugins.release.changelog.ChangelogManager
import ru.yandex.money.gradle.plugins.release.git.Credentials
import ru.yandex.money.gradle.plugins.release.git.GitReleaseManager
import ru.yandex.money.gradle.plugins.release.version.GradlePropertyVersionManager

/**
 * Поднимает patch версию у gradle.properties, добавляет -SNAPSHOT, добавляет маркеры в CHANGELOG.md, делает git push
 */
open class PostReleaseTask : DefaultTask() {

    @get:Input
    @get:Optional
    var pathToGitPrivateSshKey: String? = null

    @get:Input
    @get:Optional
    var sshKeyPassphrase: String? = null

    @TaskAction
    fun doAction() {
        val gradlePropertyVersionManager = GradlePropertyVersionManager(project.file(GradlePropertyVersionManager.DEFAULT_FILE_NAME))
        gradlePropertyVersionManager.incrementPatchVersion()
        val nextVersion = gradlePropertyVersionManager.appendSnapshotToVersion()
        val changelogFile = project.file(ChangelogManager.DEFAULT_FILE_NAME)
        if (changelogFile.exists()) {
            ChangelogManager(changelogFile).appendNextVersionDescriptionMarkers()
        }
        GitReleaseManager(project.rootDir).use {
            it.newVersionCommit(nextVersion)
            it.push(Credentials(pathToGitPrivateSshKey, sshKeyPassphrase))
        }
    }
}