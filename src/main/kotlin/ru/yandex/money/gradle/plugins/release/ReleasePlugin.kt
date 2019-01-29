package ru.yandex.money.gradle.plugins.release

import org.gradle.api.Plugin
import org.gradle.api.Project
import ru.yandex.money.gradle.plugins.release.changelog.CheckChangeLogTask

/**
 * Плагин для релиза артефактов
 */
class ReleasePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val releaseExtension = project.extensions.create("releaseSettings", ReleaseExtension::class.java)

        project.tasks.create("checkChangelog", CheckChangeLogTask::class.java) {
            it.group = "verification"
            it.description = "Check that changelog has next version description"
        }


        project.tasks.create("preReleaseRotateVersion", PreReleaseRotateVersionTask::class.java) {
            it.group = "release"
            it.description = "Update version in gradle.property, rotate CHANGELOG.md"
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
            val releaseTasks = releaseExtension.releaseTasks
            if (releaseTasks.size > 0) {
                // строим зависимость для порядка выполнения в фазе Release
                // userTask1 -> userTask2 -> userTask..N -> postRelease
                for (i in 1 until releaseTasks.size) {
                    it.tasks.getByName(releaseTasks[i]).dependsOn(releaseTasks[i - 1])
                }
                it.tasks.getByName("release").dependsOn(releaseTasks.last())
            }
            // строим зависимость для порядка выполнения в фазе preRelease
            // preReleaseRotateVersion-> userTask1 -> userTask2 -> userTask..N -> preRelease
            val preReleaseTasks = releaseExtension.preReleaseTasks
            if (preReleaseTasks.size > 0) {
                it.tasks.getByName(preReleaseTasks[0]).dependsOn("preReleaseRotateVersion")
                for (i in 1 until preReleaseTasks.size) {
                    it.tasks.getByName(preReleaseTasks[i]).dependsOn(preReleaseTasks[i - 1])
                }
                it.tasks.getByName("preRelease").dependsOn(preReleaseTasks.last())
            } else {
                it.tasks.getByName("preRelease").dependsOn("preReleaseRotateVersion")
            }

            val postReleaseTask = it.tasks.getByName("release") as PostReleaseTask
            postReleaseTask.pathToGitPrivateSshKey = releaseExtension.pathToGitPrivateSshKey
        }

    }
}