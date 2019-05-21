package ru.yandex.money.gradle.plugins.release

import org.gradle.api.Plugin
import org.gradle.api.Project
import ru.yandex.money.gradle.plugins.release.changelog.CheckChangeLogTask

/**
 * Плагин для релиза артефактов
 */
class ReleasePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.extensions.create("releaseSettings", ReleaseExtension::class.java)

        project.tasks.create("checkChangelog", CheckChangeLogTask::class.java) {
            it.group = "verification"
            it.description = "Check that changelog has next version description"
        }

        project.tasks.create("preReleaseRotateVersion", PreReleaseRotateVersionTask::class.java) {
            it.group = "release"
            it.description = "Update version in gradle.property, rotate CHANGELOG.md"
        }

        project.tasks.create("checkRelease", CheckReleaseTask::class.java) {
            it.group = "release"
            it.description = "Fail build if push unsuccessful or tag already exist"
        }

        project.tasks.create("preReleaseCheckExecuted", CheckPreReleaseExecutedTask::class.java) {
            it.group = "release"
            it.description = "Fail build if no preRelease was executed"
        }

        project.tasks.create("preRelease", PreReleaseCommitTask::class.java) {
            it.group = "release"
            it.description = "Commit all changes, add tag with version"
        }

        project.tasks.create("release", PostReleaseTask::class.java) {
            it.group = "release"
            it.description = "Execute user release tasks, append markers to changelog, up gradle.property patch version, append snapshot to version, git push"
        }

        project.afterEvaluate {

            configReleaseTaskOrder(it)
            configPreReleaseTaskOrder(it)

            val releaseExtension = project.extensions.getByType(ReleaseExtension::class.java)

            val checkReleaseTask = it.tasks.getByName("checkRelease") as CheckReleaseTask
            checkReleaseTask.pathToGitPrivateSshKey = releaseExtension.pathToGitPrivateSshKey
            checkReleaseTask.sshKeyPassphrase = releaseExtension.sshKeyPassphrase

            val postReleaseTask = it.tasks.getByName("release") as PostReleaseTask
            postReleaseTask.pathToGitPrivateSshKey = releaseExtension.pathToGitPrivateSshKey
            postReleaseTask.sshKeyPassphrase = releaseExtension.sshKeyPassphrase

            val checkChangeLogTask = it.tasks.getByName("checkChangelog") as CheckChangeLogTask
            checkChangeLogTask.changelogRequired = releaseExtension.changelogRequired
        }
    }

    /**
     * Строим зависимость для порядка выполнения в фазе Release
     * build -> public -> userTask..N -> preReleaseCheckExecuted-> release
     */
    private fun configReleaseTaskOrder(project: Project) {
        val releaseTasks = project.extensions.getByType(ReleaseExtension::class.java).releaseTasks
        if (releaseTasks.size > 0) {
            for (i in 1 until releaseTasks.size) {
                project.tasks.getByName(releaseTasks[i]).dependsOn(releaseTasks[i - 1])
            }
            project.tasks.getByName("preReleaseCheckExecuted").dependsOn(releaseTasks.last())
        }
        project.tasks.getByName("release").dependsOn("preReleaseCheckExecuted")
    }

    /**
     * Строим зависимость для порядка выполнения в фазе preRelease
     * checkChangelog -> preReleaseRotateVersion-> userTask1 -> userTask2 -> userTask..N -> preRelease
     */
    private fun configPreReleaseTaskOrder(project: Project) {
        val preReleaseTasks = project.extensions.getByType(ReleaseExtension::class.java).preReleaseTasks
        if (preReleaseTasks.size > 0) {
            project.tasks.getByName(preReleaseTasks[0]).dependsOn("preReleaseRotateVersion")
            for (i in 1 until preReleaseTasks.size) {
                project.tasks.getByName(preReleaseTasks[i]).dependsOn(preReleaseTasks[i - 1])
            }
            project.tasks.getByName("preRelease").dependsOn(preReleaseTasks.last())
        } else {
            project.tasks.getByName("preRelease").dependsOn("checkRelease")
        }
        project.tasks.getByName("preReleaseRotateVersion").dependsOn("checkChangelog")

        project.tasks.getByName("checkRelease").dependsOn("preReleaseRotateVersion")
    }
}