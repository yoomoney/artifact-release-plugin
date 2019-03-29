package ru.yandex.money.gradle.plugins.release

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import ru.yandex.money.gradle.plugins.release.git.Credentials
import ru.yandex.money.gradle.plugins.release.git.GitReleaseManager
import ru.yandex.money.gradle.plugins.release.version.ReleaseInfoStorage

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
    @get:Optional
    var pathToGitPrivateSshKey: String? = null

    @TaskAction
    fun checkRelease() {
        val releaseVersion = ReleaseInfoStorage(project.buildDir).loadVersion()
                ?: throw GradleException("Next release version is absent")

        GitReleaseManager(project.rootDir).use {
            if (it.isTagExists(releaseVersion)) {
                throw GradleException("Tag $releaseVersion already exist")
            }

            if (!it.checkPush(Credentials(pathToGitPrivateSshKey))) {
                throw GradleException("Push unsuccessful, check your repository permission settings")
            }
        }
    }

}