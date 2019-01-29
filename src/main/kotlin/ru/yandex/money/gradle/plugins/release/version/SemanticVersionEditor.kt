package ru.yandex.money.gradle.plugins.release.version

/**
 * Редактор версий согласно [https://semver.org]
 * @param currentVersion текущая версия в формате \d+\.\d+\.\d+
 */
class SemanticVersionEditor(currentVersion: String?) {

    companion object {
        private val VERSION_PATTERN = Regex("\\d+\\.\\d+\\.\\d+")
    }

    private var patch: Int
    private var minor: Int
    private var major: Int

    init {
        val version = if (currentVersion == null) {
            "0.0.0"
        } else {
            if (!currentVersion.matches(VERSION_PATTERN)) {
                throw IllegalArgumentException("Version should be like '\\d+\\.\\d+\\.\\d+', but it is $currentVersion")
            } else {
                currentVersion
            }
        }

        val versions = version.split(".")
        major = versions[0].toInt()
        minor = versions[1].toInt()
        patch = versions[2].toInt()
    }

    /**
     * Увеличивает версию
     * @param release тип релиза
     */
    fun increment(release: ReleaseType): String {
        when (release) {
            ReleaseType.MAJOR -> {
                major++
                minor = 0
                patch = 0
            }
            ReleaseType.MINOR -> {
                minor++
                patch = 0
            }
            ReleaseType.PATCH -> patch++
            else -> throw  java.lang.IllegalArgumentException("Unknown $release")
        }

        return asString()
    }

    fun asString(): String {
        return "$major.$minor.$patch"
    }

    override fun toString(): String {
        return "SemanticVersionEditor(patch=$patch, minor=$minor, major=$major)"
    }


}

/**
 * Тип релиза
 */
enum class ReleaseType {
    MAJOR,
    MINOR,
    PATCH;
}