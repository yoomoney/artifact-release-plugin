package ru.yandex.money.gradle.plugins.release.git

import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ru.yandex.money.gradle.plugins.release.AbstractReleaseTest
import java.io.File

/**
 * Тесты GitReleaseManager
 *
 * @author horyukova
 * @since 05.04.2019
 */
class GitReleaseManagerTest : AbstractReleaseTest() {
    lateinit var gitReleaseManager: GitReleaseManager

    @Before
    fun before() {
        gitReleaseManager = GitReleaseManager(File(projectDir.root.absolutePath))
    }

    @Test
    fun `should correctly check tag exists `() {
        git.tag().setName("2.0.0").call()
        git.branchRename().setNewName("3.0.0").call()

        assertTrue(gitReleaseManager.isTagExists("2.0.0"))
        assertFalse(gitReleaseManager.isTagExists("3.0.0"))
    }
}