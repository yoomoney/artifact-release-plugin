package ru.yandex.money.gradle.plugins.release

import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.Matchers
import org.hamcrest.Matchers.greaterThan
import org.junit.Assert
import org.junit.Assert.assertThat
import org.junit.Test

class ReleasePluginTest : AbstractReleaseTest() {

    @Test
    fun `should fail checkChangelog on absent changelog if required`() {
        val result = runTasksFail("checkChangelog")
        assertThat(result.output, containsString("Создайте в корне проекта файл CHANGELOG.md\n"))
    }

    @Test
    fun `should success checkChangelog on absent changelog if not required`() {
        buildFile.appendText("""
        releaseSettings {
            changelogRequired = false
        }
        """)
        runTasksSuccessfully("checkChangelog")
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
        }
       
        """.trimIndent())
        addChangeLog(this::class.java.getResource("/changelogs/1Version_filledMarkers.md").readText())


        val preReleaseResult = runTasksSuccessfully("preRelease")

        assertThat(preReleaseResult.output, containsString("nextVersion=1.1.0"))
        assertThat(gradleProperties.readText(), containsString("version=1.1.0\n"))
        assertThat(getChangelogContent(), allOf(containsString("## [1.1.0]()")))
        assertThat(preReleaseResult.output, containsString("preReleaseTask1 executed 1.1.0"))
        assertThat(preReleaseResult.output.indexOf("Task :preRelease\n"), greaterThan(preReleaseResult.output.indexOf("Task :preReleaseTask1\n")))
        assertThat(preReleaseResult.output.indexOf("Task :preReleaseTask1\n"), greaterThan(preReleaseResult.output.indexOf("Task :preReleaseRotateVersion\n")))

        val releaseResult = runTasksSuccessfully("release")

        assertThat(releaseResult.output.indexOf("publishArtifacts2 executed"), greaterThan(releaseResult.output.indexOf("publishArtifacts1 executed")))
        assertThat(releaseResult.output.indexOf("Task :release"), greaterThan(releaseResult.output.indexOf("Task :publishArtifacts2")))
        assertThat(gradleProperties.readText(), containsString("version=1.1.1-SNAPSHOT"))
        assertThat(getChangelogContent(), allOf(containsString("NEXT_VERSION_TYPE=MAJOR|MINOR|PATCH"), containsString("some desc")))
        assertThat(getCommitMessages(git)[0], Matchers.startsWith("[Gradle Release Plugin] - new version commit: '1.1.1-SNAPSHOT'"))
        assertThat(getCommitMessages(gitOrigin)[0], Matchers.startsWith("[Gradle Release Plugin] - new version commit: '1.1.1-SNAPSHOT'"))
    }


    @Test
    fun `should preRelease changeLog on absent changelog if required`() {
        val preReleaseResult = runTasksFail("preRelease")
        assertThat(preReleaseResult.output, containsString("Создайте в корне проекта файл CHANGELOG.md\n"))
    }


    @Test
    fun `should release without changeLog`() {
        buildFile.appendText("""
        releaseSettings {
            changelogRequired = false
        }
        """)

        val preReleaseResult = runTasksSuccessfully("preRelease")

        assertThat(preReleaseResult.output, containsString("releaseVersion=1.0.1"))
        assertThat(gradleProperties.readText(), containsString("version=1.0.1\n"))


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
    fun `should fail checkChangelog on unfilled changelog`() {
        addChangeLog(this::class.java.getResource("/changelogs/1Version_markers.md").readText())
        val result = runTasksFail("checkChangelog")
        assertThat(result.output, containsString("Execution failed for task ':checkChangelog'"))
    }
}