package ru.yandex.money.gradle.plugins.release.version

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ReleaseInfoStorageTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun `should store and load version and description`() {
        val releaseVersionStorage = ReleaseInfoStorage(folder.root)

        releaseVersionStorage.storeVersion("1.0.0")
        releaseVersionStorage.storeChangelog("* first line\n* second line")

        assertEquals("1.0.0", releaseVersionStorage.loadVersion())
        assertEquals("* first line\n* second line", releaseVersionStorage.loadChangelog())
    }
}
