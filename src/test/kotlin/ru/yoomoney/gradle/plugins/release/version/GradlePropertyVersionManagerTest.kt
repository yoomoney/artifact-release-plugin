package ru.yoomoney.gradle.plugins.release.version

import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class GradlePropertyVersionManagerTest {

    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var gradleProperty: File

    @Before
    fun init() {
        gradleProperty = folder.newFile("gradle.property")
    }

    @Test
    fun `should return current version with SNAPSHOT`() {
        gradleProperty.writeText("some.property=123\nversion=1.0.0-SNAPSHOT\nsome.other=321\n")
        val manager = GradlePropertyVersionManager(gradleProperty)
        Assert.assertEquals(manager.getCurrentVersion(), "1.0.0-SNAPSHOT")
    }

    @Test
    fun `should return current version without SNAPSHOT`() {
        gradleProperty.writeText("some.property=123\nversion=1.0.0\nsome.other=321\n")
        val manager = GradlePropertyVersionManager(gradleProperty)
        Assert.assertEquals(manager.getCurrentVersion(), "1.0.0")
    }

    @Test
    fun `should update to next patch version with SNAPSHOT`() {
        gradleProperty.writeText("some.property=123\nversion=1.0.0-SNAPSHOT\nsome.other=321\n")
        val manager = GradlePropertyVersionManager(gradleProperty)
        manager.incrementPatchVersion()
        Assert.assertEquals("some.property=123\nversion=1.0.1-SNAPSHOT\nsome.other=321\n", gradleProperty.readText()
                .replace("\r", ""))
    }

    @Test
    fun `should append snapshot to version only ones`() {
        gradleProperty.writeText("some.property=123\nversion=1.0.0\nsome.other=321\n")
        val manager = GradlePropertyVersionManager(gradleProperty)
        manager.appendSnapshotToVersion()
        manager.appendSnapshotToVersion()
        Assert.assertEquals("some.property=123\nversion=1.0.0-SNAPSHOT\nsome.other=321\n", gradleProperty.readText()
                .replace("\r", ""))
    }

    @Test
    fun `should update to next patch version without SNAPSHOT`() {
        gradleProperty.writeText("some.property=123\nversion=1.0.0\nsome.other=321\n")
        val manager = GradlePropertyVersionManager(gradleProperty)
        manager.incrementPatchVersion()
        Assert.assertEquals("some.property=123\nversion=1.0.1\nsome.other=321\n", gradleProperty.readText()
                .replace("\r", ""))
    }
}