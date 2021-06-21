package ru.yoomoney.gradle.plugins.release

import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.greaterThan
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleasePluginTest : AbstractReleaseTest() {

    @Test
    fun `should fail checkChangelog on absent changelog if required`() {
        buildFile.appendText("""
        releaseSettings {
            gitUsername = 'user'
            gitEmail = 'user@mail.ru'
        }
        """)
        val result = runTasksFail("checkChangelog")
        assertThat(result.output, containsString("Создайте в корне проекта файл CHANGELOG.md" + System.lineSeparator()))
    }

    @Test
    fun `should success checkChangelog on absent changelog if not required`() {
        buildFile.appendText("""
        releaseSettings {
            changelogRequired = false
            gitUsername = 'user'
            gitEmail = 'user@mail.ru'
        }
        """)
        runTasksSuccessfully("checkChangelog")
    }

    @Test
    fun `should commit new file on preRelease`() {
        buildFile.appendText("""
        task preReleaseTask1 {
            doLast {
                println 'preReleaseTask1 executed'
                new File("${'$'}projectDir/new-file.yaml").text = "new file text"
            }
        }
        releaseSettings {
            preReleaseTasks=['preReleaseTask1']
            changelogRequired = false
            gitUsername = 'user'
            gitEmail = 'user@mail.ru'
            allowedFilesForCommitRegex=['.+yaml']
        }

        """.trimIndent())
        addChangeLog(this::class.java.getResource("/changelogs/1Version_filledMarkers.md").readText())

        val preReleaseResult = runTasksSuccessfully("preRelease")

        assertThat(preReleaseResult.output, containsString("nextVersion=1.1.0"))
        assertThat(gradleProperties.readText(), containsString("version=1.1.0" + System.lineSeparator()))
        assertThat(getChangelogContent(), allOf(containsString("## [1.1.0]()")))
        assertThat(preReleaseResult.output, containsString("preReleaseTask1 executed"))
        assertThat(preReleaseResult.output.indexOf("Task :preRelease" + System.lineSeparator()),
                greaterThan(preReleaseResult.output.indexOf("Task :preReleaseTask1" + System.lineSeparator())))
        assertThat(preReleaseResult.output.indexOf("Task :preReleaseTask1" + System.lineSeparator()),
                greaterThan(preReleaseResult.output.indexOf("Task :preReleaseRotateVersion" + System.lineSeparator())))

        assertThat(git.status().call().untracked, Matchers.empty())
    }

    @Test
    fun `should release with changeLog and correct sequence`() {
        buildFile.appendText("""
        task publishArtifacts1 {                    
            doLast {
                println 'publishArtifacts1 executed'
            }
        }
        
        task publishArtifacts2 {                    
            doLast {
                println 'publishArtifacts2 executed'
            }
        }

        task preReleaseTask1 {
            doLast {
                println 'preReleaseTask1 executed ' + new File(project.buildDir,'release/release-version.txt').text
            }
        }
        
        releaseSettings {
            releaseTasks=['build','publishArtifacts1','publishArtifacts2']
            preReleaseTasks=['preReleaseTask1']
            changelogRequired = false
            gitUsername = 'user'
            gitEmail = 'user@mail.ru'
            addPullRequestLinkToChangelog = false
            pullRequestInfoProvider = "GitHub"
            githubAccessToken = 'token'
        }
       
        """.trimIndent())
        addChangeLog(this::class.java.getResource("/changelogs/1Version_filledMarkers.md").readText())

        val notInIndexFile = projectDir.newFile("test_file.txt")

        val preReleaseResult = runTasksSuccessfully("preRelease")

        assertThat(preReleaseResult.output, containsString("nextVersion=1.1.0"))
        assertThat(gradleProperties.readText(), containsString("version=1.1.0" + System.lineSeparator()))
        assertThat(getChangelogContent(), allOf(containsString("## [1.1.0]()")))
        assertThat(preReleaseResult.output, containsString("preReleaseTask1 executed 1.1.0"))
        assertThat(preReleaseResult.output.indexOf("Task :preRelease" + System.lineSeparator()),
                greaterThan(preReleaseResult.output.indexOf("Task :preReleaseTask1" + System.lineSeparator())))
        assertThat(preReleaseResult.output.indexOf("Task :preReleaseTask1" + System.lineSeparator()),
                greaterThan(preReleaseResult.output.indexOf("Task :preReleaseRotateVersion" + System.lineSeparator())))

        val releaseResult = runTasksSuccessfully("release")

        assertThat(releaseResult.output.indexOf("publishArtifacts2 executed"),
                greaterThan(releaseResult.output.indexOf("publishArtifacts1 executed")))
        assertThat(releaseResult.output.indexOf("Task :release"),
                greaterThan(releaseResult.output.indexOf("Task :publishArtifacts2")))
        assertThat(gradleProperties.readText(), containsString("version=1.1.1-SNAPSHOT"))
        assertThat(getChangelogContent(), allOf(containsString("NEXT_VERSION_TYPE=MAJOR|MINOR|PATCH"), containsString("some desc")))
        assertThat(getCommitMessages(git)[0], Matchers.startsWith("[Gradle Release Plugin] - new version commit: '1.1.1-SNAPSHOT'"))
        assertThat(getCommitMessages(gitOrigin)[0], Matchers.startsWith("[Gradle Release Plugin] - new version commit: '1.1.1-SNAPSHOT'"))

        val status = git.status().call()
        assertTrue(status.untracked.contains(notInIndexFile.name))
    }

    @Test
    fun `should preRelease changeLog on absent changelog if required`() {
        buildFile.appendText("""
        releaseSettings {
            gitUsername = 'user'
            gitEmail = 'user@mail.ru'
        }
        """)
        val preReleaseResult = runTasksFail("preRelease")
        assertThat(preReleaseResult.output, containsString("Создайте в корне проекта файл CHANGELOG.md" + System.lineSeparator()))
    }

    @Test
    fun `should release without changeLog`() {
        buildFile.appendText("""
        releaseSettings {
            changelogRequired = false
            gitUsername = 'user'
            gitEmail = 'user@mail.ru'
        }
        """)

        git.add().addFilepattern("build.gradle").call()
        git.commit().setMessage("build.gradle commit").call()

        val preReleaseResult = runTasksSuccessfully("preRelease")

        assertThat(preReleaseResult.output, containsString("releaseVersion=1.0.1"))
        assertThat(gradleProperties.readText(), containsString("version=1.0.1" + System.lineSeparator()))

        runTasksSuccessfully("release")

        assertThat(gradleProperties.readText(), containsString("version=1.0.2-SNAPSHOT"))

        assertThat(getCommitMessages(git)[0], Matchers.startsWith("[Gradle Release Plugin] - new version commit: '1.0.2-SNAPSHOT'"))
        assertThat(getCommitMessages(gitOrigin)[0], Matchers.startsWith("[Gradle Release Plugin] - new version commit: '1.0.2-SNAPSHOT'"))

        assertThat(getCommitMessages(git)[1], Matchers.startsWith("[Gradle Release Plugin] - pre tag commit: '1.0.1'"))
        assertThat(getCommitMessages(gitOrigin)[1], Matchers.startsWith("[Gradle Release Plugin] - pre tag commit: '1.0.1'"))

        checkTagExists(git, "1.0.1")
        checkTagExists(gitOrigin, "1.0.1")
    }

    @Test
    fun `should fail build on release without preRelease`() {
        buildFile.appendText("""
        releaseSettings {
            changelogRequired = false
            gitUsername = 'user'
            gitEmail = 'user@mail.ru'
        }
        """)

        val runTasksFail = runTasksFail("release")
        assertThat(runTasksFail.output, containsString("Перед запуском release, должена быть запущена задача preRelease"))
    }

    @Test
    fun `should run releaseTasks task successful`() {
        buildFile.appendText("""
        task publishArtifacts1 {
            doLast {
                println 'publishArtifacts1 executed'
            }
        }
        releaseSettings {
            changelogRequired = false
            releaseTasks=['publishArtifacts1']
            gitUsername = 'user'
            gitEmail = 'user@mail.ru'
            addPullRequestLinkToChangelog = true
            pullRequestInfoProvider = 'Bitbucket'
            bitbucketApiToken = 'token'
        }
        """)

        val runTasksFail = runTasksSuccessfully("publishArtifacts1")
        assertThat(runTasksFail.output, containsString("publishArtifacts1 executed"))
    }

    @Test
    fun `should fail build when there are uncommited changes`() {
        buildFile.appendText("""
        releaseSettings {
            changelogRequired = false
            gitUsername = 'user'
            gitEmail = 'user@mail.ru'
        }
        """)

        val runTasksFail = runTasksFail("preRelease")
        assertThat(runTasksFail.output, containsString("There are uncommitted changes"))
    }

    @Test
    fun `should fail build when tag already exist`() {
        buildFile.appendText("""
        releaseSettings {
            changelogRequired = false
            gitUsername = 'user'
            gitEmail = 'user@mail.ru'
        }
        """)
        git.add().addFilepattern("build.gradle").call()
        git.commit().setMessage("build.gradle commit").call()
        git.tag()
                .setName("1.0.1")
                .call()
        val runTasksFail = runTasksFail("preRelease")
        assertThat(runTasksFail.output, containsString("Tag 1.0.1 already exist"))
    }

    @Test
    fun `should fail checkChangelog on unfilled changelog`() {
        buildFile.appendText("""
        releaseSettings {
            gitUsername = 'user'
            gitEmail = 'user@mail.ru'
        }
        """)
        addChangeLog(this::class.java.getResource("/changelogs/1Version_markers.md").readText())
        val result = runTasksFail("checkChangelog")
        assertThat(result.output, containsString("Execution failed for task ':checkChangelog'"))
    }

    @Test
    fun `should successful run checkRelease`() {
        buildFile.appendText("""
        releaseSettings {
            changelogRequired = false
            gitUsername = 'user'
            gitEmail = 'user@mail.ru'
        }
        """)
        git.add().addFilepattern("build.gradle").call()
        git.commit().setMessage("build.gradle commit").call()

        runTasksSuccessfully("checkRelease")
    }
}