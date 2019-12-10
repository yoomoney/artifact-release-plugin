package ru.yandex.money.gradle.plugins.release

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import ru.yandex.money.gradle.plugins.release.version.ReleaseInfoStorage

/**
 * Проверяет что был запущен preRelease
 */
open class CheckPreReleaseExecutedTask : DefaultTask() {

    @TaskAction
    fun checkPreRelease() {
        val releaseInfoStorage = ReleaseInfoStorage(project.buildDir)
        if (releaseInfoStorage.loadVersion() == null) {
            throw GradleException("Перед запуском release, должена быть запущена задача preRelease")
        }
    }
}
