package ru.yandex.money.gradle.plugins.release

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import ru.yandex.money.gradle.plugins.release.git.GitManager
import ru.yandex.money.gradle.plugins.release.version.ReleaseInfoStorage
import ru.yandex.money.tools.git.GitSettings

/**
 * Задача на проверку возможности релиза
 * 1) проверка прав доступа к git - пушим пустой коммит, который должен быть успешен
 * 2) проверка отсутствия релизного тега для версии будущего релиза
 *
 * @author horyukova
 * @since 28.03.2019
 */
open class CheckReleaseTask : DefaultTask() {
    @get:Input
    lateinit var gitSettings: GitSettings

    @TaskAction
    fun checkRelease() {
        val releaseVersion = ReleaseInfoStorage(project.buildDir).loadVersion()
                ?: throw GradleException("Next release version is absent")

        GitManager(project.rootDir, gitSettings).use {
            if (it.isTagExists(releaseVersion)) {
                throw GradleException("Tag $releaseVersion already exist")
            }

            if (!it.checkPush()) {
                throw GradleException("Push unsuccessful, check your repository permission settings")
            }
        }
    }
}