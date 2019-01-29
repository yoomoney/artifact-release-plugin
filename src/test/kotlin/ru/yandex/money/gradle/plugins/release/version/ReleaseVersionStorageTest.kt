package ru.yandex.money.gradle.plugins.release.version

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ReleaseVersionStorageTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun `should store and load version`() {
        val releaseVersionStorage = ReleaseVersionStorage(folder.root)
        releaseVersionStorage.storeVersion("1.0.0")
        assertEquals("1.0.0", releaseVersionStorage.loadVersion())
    }
}

