package ru.yandex.money.gradle.plugins.release.version

import org.eclipse.jgit.api.Git
import org.gradle.api.Project
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito
import java.io.File

class ArtifactVersionProviderTest {

    lateinit var versionProvider: ArtifactVersionProvider

    val project = Mockito.mock(Project::class.java)
    @get:Rule
    val projectDir = TemporaryFolder()
    @get:Rule
    val buildDir = TemporaryFolder()
    lateinit var git: Git

    @Before
    fun beforeTest() {
        Mockito.`when`(project.projectDir).thenReturn(projectDir.root)
        Mockito.`when`(project.rootDir).thenReturn(projectDir.root)
        Mockito.`when`(project.buildDir).thenReturn(buildDir.root)
        Mockito.`when`(project.version).thenReturn("1.0.0")
        git = Git.init().setDirectory(File(projectDir.root.absolutePath))
                .setBare(false)
                .call()
        git.commit().setMessage("init commit").call()
        versionProvider = ArtifactVersionProvider(project)
    }

    @After
    fun afterTest() {
        Mockito.reset(project)
    }

    private fun checkoutBranch(name: String) {
        git.checkout()
                .setName(name)
                .setCreateBranch(true)
                .call()
    }

    @Test
    fun `should get version from version storage`() {
        val version = "8.8.8"
        checkoutBranch("feature/some_name123")
        ReleaseInfoStorage(buildDir.root).storeVersion(version)
        Assert.assertEquals(version, versionProvider.getArtifactVersion())
    }

    @Test
    fun `should get version from project version with -SNAPSHOT`() {
        Mockito.`when`(project.version).thenReturn("1.2.3-SNAPSHOT")
        checkoutBranch("feature/some_name123")
        Assert.assertEquals("1.2.3-feature-some-name123-SNAPSHOT", versionProvider.getArtifactVersion())
    }

    @Test
    fun `should get version from project version on master`() {
        Mockito.`when`(project.version).thenReturn("1.2.3-SNAPSHOT")
        Assert.assertEquals("1.2.3", versionProvider.getArtifactVersion())
    }

    @Test
    fun `should get version from project version on feature`() {
        checkoutBranch("feature/some_name123")
        Mockito.`when`(project.version).thenReturn("1.2.3-SNAPSHOT")
        Assert.assertEquals("1.2.3-feature-some-name123-SNAPSHOT", versionProvider.getArtifactVersion())
    }

    @Test
    fun `should get version from changelog on feature`() {
        checkoutBranch("feature/some_name123")
        File(projectDir.root, "CHANGELOG.md").writeText(
                ArtifactVersionProviderTest::class.java.getResource("/changelogs/1Version_filledMarkers.md")
                        .readText())
        Assert.assertEquals("1.1.0-feature-some-name123-SNAPSHOT", versionProvider.getArtifactVersion())
    }

    @Test
    fun `should get version from changelog on master`() {

        File(projectDir.root, "CHANGELOG.md").writeText(
                ArtifactVersionProviderTest::class.java.getResource("/changelogs/1Version_filledMarkers.md")
                        .readText())
        Assert.assertEquals("1.1.0", versionProvider.getArtifactVersion())
    }
}