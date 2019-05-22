package ru.yandex.money.gradle.plugins.release.version

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class SemanticVersionEditorTest(
    private val versionBeforeRelease: String?,
    private val versionAfterRelease: String,
    private val releaseType: ReleaseType
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "before release {0} after release expected {1} release-type {2}")
        fun testData(): Array<Array<out Any?>> {
            return arrayOf(
                    arrayOf(null, "1.0.0", ReleaseType.MAJOR),
                    arrayOf(null, "0.1.0", ReleaseType.MINOR),
                    arrayOf(null, "0.0.1", ReleaseType.PATCH),

                    arrayOf("1.1.1", "2.0.0", ReleaseType.MAJOR),
                    arrayOf("1.1.1", "1.2.0", ReleaseType.MINOR),
                    arrayOf("1.1.1", "1.1.2", ReleaseType.PATCH)
            )
        }
    }

    @Test
    fun `should increment version`() {
        Assert.assertEquals(versionAfterRelease, SemanticVersionEditor(versionBeforeRelease).increment(releaseType))
    }
}