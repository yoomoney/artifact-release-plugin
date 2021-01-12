package ru.yoomoney.gradle.plugins.release

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import ru.yoomoney.gradle.plugins.release.git.GitManager
import ru.yoomoney.gradle.plugins.release.git.GitSettings
import ru.yoomoney.gradle.plugins.release.version.ReleaseInfoStorage

/**
 * Коммитит в гит изменения и добавляет тэг с новой версией
 */
open class PreReleaseCommitTask : DefaultTask() {
    @get:Input
    lateinit var gitSettings: GitSettings

    @TaskAction
    fun commitChanges() {
        val releaseVersion = ReleaseInfoStorage(project.buildDir).loadVersion()
                ?: throw GradleException("Next release version is absent")
        GitManager(project.rootDir, gitSettings).use {
            it.preTagCommit(releaseVersion)
        }
    }
}
