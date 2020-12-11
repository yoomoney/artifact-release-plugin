package ru.yoomoney.gradle.plugins.release

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.transport.URIish
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.spockframework.util.Assert
import java.io.File

abstract class AbstractReleaseTest {

    @get:Rule
    val projectDir = TemporaryFolder()

    lateinit var buildFile: File

    lateinit var git: Git
    lateinit var gitOrigin: Git
    lateinit var gradleProperties: File
    @get:Rule
    var originRepoFolder = TemporaryFolder()

    @Before
    fun setup() {
        buildFile = projectDir.newFile("build.gradle")

        buildFile.writeText("""
            plugins {
                id 'java'
                id 'ru.yoomoney.gradle.plugins.artifact-release-plugin'
            }

        """.trimIndent())

        git = Git.init().setDirectory(File(projectDir.root.absolutePath))
                .setBare(false)
                .call()
        projectDir.newFile(".gitignore")
                .writeBytes(this::class.java.getResourceAsStream("/project/gitignore")
                        .readBytes())

        gradleProperties = projectDir.newFile("gradle.properties")
        gradleProperties.writeText("version=1.0.1-SNAPSHOT")
        git.add().addFilepattern("gradle.properties")
                .addFilepattern(".gitignore")
                .addFilepattern("build.gradle")
                .call()
        git.commit().setMessage("build.gradle commit").call()
        git.tag().setName("1.0.0").call()
        gitOrigin = Git.init().setDirectory(originRepoFolder.root)
                .setBare(true)
                .call()
        val remoteSetUrl = git.remoteSetUrl()
        remoteSetUrl.setRemoteUri(URIish("file://${originRepoFolder.root.absolutePath}/"))
        remoteSetUrl.setRemoteName("origin")
        remoteSetUrl.call()
        git.push()
                .setPushAll()
                .setPushTags()
                .call()
        println("Work directory: ${projectDir.root.absolutePath}")
        println("Origin git repo directory: ${originRepoFolder.root.absolutePath}")
    }

    fun addChangeLog(fileContent: String) {
        val changelog = projectDir.newFile("CHANGELOG.md")
        if (changelog.exists()) {
            changelog.delete()
            changelog.createNewFile()
        }
        changelog.writeText(fileContent)

        git.add().addFilepattern(changelog.name).call()
        git.commit().setMessage("${changelog.name} commit").setAll(true).call()
    }

    fun getChangelogContent(): String {
        return File(projectDir.root, "CHANGELOG.md").readText()
    }

    fun checkTagExists(git: Git, expectedTag: String) {
        val tag = git.repository.resolve(expectedTag)
        if (tag == null) {
            Assert.fail("Tag $expectedTag not found in repo")
        }
        RevWalk(git.repository).parseAny(tag)
    }

    fun getCommitMessages(git: Git): List<String> {
        val revWalk = RevWalk(git.repository)
        val commitId = git.repository.resolve("HEAD")
        revWalk.markStart(revWalk.parseCommit(commitId))
        val result = mutableListOf<String>()
        for (commit in revWalk) {
            if (commit is RevCommit) {
                result.add(commit.shortMessage)
            }
        }
        revWalk.close()
        return result
    }

    fun runTasksSuccessfully(vararg tasks: String): BuildResult {
        return GradleRunner.create()
                .withProjectDir(projectDir.root)
                .withArguments(tasks.toList())
                .withPluginClasspath()
                .forwardOutput()
                .withDebug(true)
                .build()
    }

    fun runTasksFail(vararg tasks: String): BuildResult {
        return GradleRunner.create()
                .withProjectDir(projectDir.root)
                .withArguments(tasks.toList())
                .withPluginClasspath()
                .forwardOutput()
                .withDebug(true)
                .buildAndFail()
    }
}