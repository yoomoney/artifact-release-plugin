package ru.yandex.money.gradle.plugins.release

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import ru.yandex.money.gradle.plugins.release.git.GitReleaseManager
import ru.yandex.money.gradle.plugins.release.version.ReleaseInfoStorage

/**
 * Коммитит в гит изменения и добавляет тэг с новой версией
 */
open class PreReleaseCommitTask : DefaultTask() {

    @TaskAction
    fun commitChanges() {
        val releaseVersion = ReleaseInfoStorage(project.buildDir).loadVersion() ?: throw GradleException("Next release version is absent")
        GitReleaseManager(project.rootDir).use {
            it.preTagCommit(releaseVersion)
        }
    }
}