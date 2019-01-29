package ru.yandex.money.gradle.plugins.release

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.TaskAction
import ru.yandex.money.gradle.plugins.release.git.GitReleaseManager
import ru.yandex.money.gradle.plugins.release.version.ReleaseVersionStorage


/**
 * Коммитит в гит изменения и добавляет тэг с новой версией
 */
open class PreReleaseCommitTask : DefaultTask() {

    companion object {
        private val log: Logger = Logging.getLogger(PreReleaseRotateVersionTask::class.java)
    }

    @TaskAction
    fun commitChanges() {
        val releaseVersion = ReleaseVersionStorage(project.buildDir).loadVersion() ?: throw GradleException("Next release version is absent")
        GitReleaseManager(project.rootDir).use {
            it.preTagCommit(releaseVersion)
        }
    }

}