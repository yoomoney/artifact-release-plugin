package ru.yoomoney.gradle.plugins.release.changelog

import org.gradle.api.GradleException
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

class ChangelogManagerTest {

    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var changelog: File

    private fun readTextFromClassPath(path: String): String {
        return this::class.java.getResource(path).readText().replace("\r", "")
    }

    @Before
    fun init() {
        changelog = folder.newFile("changelogs.md")
    }

    @Test
    fun `should append marker on empty file`() {
        changelog.writeText("")
        println("init changelog:\n" + changelog.readText())
        val manager = ChangelogManager(changelog)
        manager.appendNextVersionDescriptionMarkers()
        println("after edit:\n" + changelog.readText())
        Assert.assertEquals(readTextFromClassPath("/changelogs/emptyDescription_emptyReleaseType.md"), changelog.readText())
    }

    @Test
    fun `should append marker on file with previous version`() {
        changelog.writeText(readTextFromClassPath("/changelogs/1Version.md"))
        println("init changelog:\n" + changelog.readText())
        val manager = ChangelogManager(changelog)
        manager.appendNextVersionDescriptionMarkers()
        println("after edit:\n" + changelog.readText())
        Assert.assertEquals(readTextFromClassPath("/changelogs/1Version_markers.md"), changelog.readText())
    }

    @Test
    fun `should hasNextVersionInfo return true`() {
        changelog.writeText(readTextFromClassPath("/changelogs/filledMarkerDescription_filledMarkerMinor.md"))
        println(changelog.readText())
        val manager = ChangelogManager(changelog)
        Assert.assertTrue("В changelogs должно быть описание следующей версии", manager.hasNextVersionInfo())
    }

    @Test
    fun `should hasNextVersionInfo return false on empty description`() {
        changelog.writeText(readTextFromClassPath("/changelogs/emptyDescription_filledReleaseType.md"))
        println(changelog.readText())
        val manager = ChangelogManager(changelog)
        Assert.assertFalse(manager.hasNextVersionInfo())
    }

    @Test(expected = GradleException::class)
    fun `should updateToNextVersion fail on empty description`() {
        changelog.writeText(readTextFromClassPath("/changelogs/emptyDescription_filledReleaseType.md"))
        println(changelog.readText())
        val manager = ChangelogManager(changelog)
        Assert.assertNull(manager.updateToNextVersion(null))
    }

    @Test
    fun `should hasNextVersionInfo return false on unknown type`() {
        changelog.writeText(readTextFromClassPath("/changelogs/filledDescription_emptyReleaseType.md"))
        println(changelog.readText())
        val manager = ChangelogManager(changelog)
        Assert.assertFalse(manager.hasNextVersionInfo())
    }

    @Test(expected = GradleException::class)
    fun `should updateToNextVersion fail on unknown type`() {
        changelog.writeText(readTextFromClassPath("/changelogs/filledDescription_emptyReleaseType.md"))
        println(changelog.readText())
        val manager = ChangelogManager(changelog)
        Assert.assertNull(manager.updateToNextVersion(null))
    }

    @Test
    fun `should update minor to next version without previous release`() {
        changelog.writeText(readTextFromClassPath("/changelogs/filledMarkerDescription_filledMarkerMinor.md"))
        println("init changelog:\n" + changelog.readText())
        val manager = ChangelogManager(changelog)
        val changelogReleaseInfo = manager.updateToNextVersion(null)
        Assert.assertEquals("0.1.0", changelogReleaseInfo.releaseVersion)
        Assert.assertEquals("some description", changelogReleaseInfo.releaseDescriptionMd)

        println("after edit:\n" + changelog.readText())
        val expected = readTextFromClassPath("/changelogs/1Version_template.md")
                .replace("DATE", SimpleDateFormat("dd-MM-YYYY").format(Date()))
        Assert.assertEquals(expected, changelog.readText().replace("\r", ""))
    }

    @Test
    fun `should update patch to next version with previous release`() {
        changelog.writeText(readTextFromClassPath("/changelogs/1Version_markers.md"))
        writeReleaseType(changelog, "PATCH")
        writeNextVersionDescription(changelog, "some patch description")
        println("init changelog:\n" + changelog.readText())
        val manager = ChangelogManager(changelog)
        val changelogReleaseInfo = manager.updateToNextVersion(null)
        Assert.assertEquals("1.0.1", changelogReleaseInfo.releaseVersion)
        Assert.assertEquals("some patch description", changelogReleaseInfo.releaseDescriptionMd)

        println("after edit:\n" + changelog.readText())
        val expected = readTextFromClassPath("/changelogs/2Version_patch_template.md")
                .replace("DATE", SimpleDateFormat("dd-MM-YYYY").format(Date()))

        Assert.assertEquals(expected, changelog.readText().replace("\r", ""))
    }

    @Test
    fun `should update major to next version with previous release`() {
        changelog.writeText(readTextFromClassPath("/changelogs/1Version_markers.md"))
        writeReleaseType(changelog, "MAJOR")
        writeNextVersionDescription(changelog, "some major description")
        println("init changelog:\n" + changelog.readText())
        val manager = ChangelogManager(changelog)
        val changelogReleaseInfo = manager.updateToNextVersion(null)
        Assert.assertEquals("2.0.0", changelogReleaseInfo.releaseVersion)
        Assert.assertEquals("some major description", changelogReleaseInfo.releaseDescriptionMd)
        println("after edit:\n" + changelog.readText())
        val expected = readTextFromClassPath("/changelogs/2Version_major_template.md")
                .replace("DATE", SimpleDateFormat("dd-MM-YYYY").format(Date()))

        Assert.assertEquals(expected, changelog.readText().replace("\r", ""))
    }

    @Test
    fun `should updateToNextVersion several times`() {
        changelog.writeText(readTextFromClassPath("/changelogs/1Version.md"))
        println("init changelog:\n" + changelog.readText())
        val manager = ChangelogManager(changelog)

        manager.appendNextVersionDescriptionMarkers()
        writeNextVersionDescription(changelog, "some new description major")
        writeReleaseType(changelog, "MAJOR")
        Assert.assertEquals("2.0.0", manager.updateToNextVersion(null).releaseVersion)

        manager.appendNextVersionDescriptionMarkers()
        writeNextVersionDescription(changelog, "some new description minor")
        writeReleaseType(changelog, "MINOR")
        Assert.assertEquals("2.1.0", manager.updateToNextVersion(null).releaseVersion)

        manager.appendNextVersionDescriptionMarkers()
        writeNextVersionDescription(changelog, "some new description patch")
        writeReleaseType(changelog, "PATCH")
        Assert.assertEquals("2.1.1", manager.updateToNextVersion("https://localhost/pr/link/1").releaseVersion)

        manager.appendNextVersionDescriptionMarkers()
        writeReleaseType(changelog, "MAJOR")
        writeNextVersionDescription(changelog, "some new description major 2")
        Assert.assertEquals("3.0.0", manager.updateToNextVersion("https://localhost/pr/link/2").releaseVersion)

        println("after edit:\n" + changelog.readText())
        val expected = readTextFromClassPath("/changelogs/5NextVersion_template.md")
                .replace("DATE", SimpleDateFormat("dd-MM-YYYY").format(Date()))

        Assert.assertEquals(expected, changelog.readText().replace("\r", ""))
    }

    @Test
    fun `should hasBreakingChanges when major changes`() {
        // breaking changes section is present
        changelog.writeText(readTextFromClassPath("/changelogs/major_breakingChanges_filled.md"))
        var manager = ChangelogManager(changelog)
        Assert.assertTrue("Не найдена секция **breaking changes** при мажорном обновлении",
                manager.hasBreakingChangesMarker())

        // breaking changes section is absent
        changelog.writeText(readTextFromClassPath("/changelogs/major_breakingChanges_notFilled.md"))
        manager = ChangelogManager(changelog)
        Assert.assertFalse("Не найдена секция **breaking changes** при мажорном обновлении",
                manager.hasBreakingChangesMarker())
    }

    private fun writeReleaseType(file: File, type: String) {
        val currentLines = file.readLines()
        val newLines = mutableListOf<String>()
        newLines.add("### NEXT_VERSION_TYPE=$type")
        newLines.addAll(currentLines.subList(1, currentLines.size))
        file.writeText(newLines.joinToString("\n"))
    }

    private fun writeNextVersionDescription(file: File, newDesc: String) {
        val lines = file.readLines()
        val strings = mutableListOf<String>()
        strings.add(lines.get(0))
        strings.add(lines.get(1))
        strings.add(newDesc)
        strings.addAll(lines.subList(2, lines.size))
        file.delete()
        file.writeText(strings.joinToString("\n"))
    }
}
