package ru.yoomoney.gradle.plugins.release.version

import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import ru.yoomoney.gradle.plugins.release.changelog.ChangelogManager
import ru.yoomoney.gradle.plugins.release.git.GitRepoFactory
import ru.yoomoney.gradle.plugins.release.git.GitSettings
import java.io.File
import java.util.function.Predicate

/**
 * Предоставляет информацию о версии артифакта который сейчас собирается
 */
class ArtifactVersionProvider(
    private val project: Project,
    private val isCurrentBranchForRelease: Predicate<String> =
                Predicate { branchName: String -> branchName == "master" || branchName.startsWith("release/") }
) {

    companion object {
        val log: Logger = Logging.getLogger(ArtifactVersionProvider::class.java)
    }

    /**
     * Версия артифакта который сейчас собирается
     */
    fun getArtifactVersion(): String {

        val versionFromStorage = ReleaseInfoStorage(project.buildDir).loadVersion()
        // в этом случае происходит релиз и мы собираем релизный артефакт
        if (versionFromStorage != null) {
            log.lifecycle("Artifact version from release info storage: version={}", versionFromStorage)
            return versionFromStorage
        }

        // в этом случае происходит сборка feature ветки или сборка из мастера вне релиза
        val versionFromChangeLog = getVersionFromChangeLog()
        if (versionFromChangeLog != null) {
            log.lifecycle("Artifact version from changelog: version={}", versionFromChangeLog)
            return versionFromChangeLog
        }
        // во всех остальных случаях считаем что вирсию устанавливают через gradle.properties возможно с постфиксом -SNAPSHOT
        val gradleProjectVersion = getGradleProjectVersion()
        log.lifecycle("Artifact version from gradle project: version={}", gradleProjectVersion)
        return gradleProjectVersion
    }

    private fun getGradleProjectVersion(): String {
        return appendBranchNameAndSnapshot(project.version.toString().replace("-SNAPSHOT", ""))!!
    }

    private fun appendBranchNameAndSnapshot(currentVersion: String?): String? {
        if (currentVersion == null) {
            return null
        }
        val dummyGitSettings = GitSettings(
                username = "user",
                email = "user@yoomoney.ru",
                sshKeyPath = null,
                passphraseSshKey = null)
        val git = GitRepoFactory(dummyGitSettings).createFromExistingDirectory(project.rootDir)
        val currentBranchName = git.currentBranchName
        if (isCurrentBranchForRelease.test(currentBranchName)) {
            return currentVersion
        }
        return currentVersion + "-" + currentBranchName.replace(Regex("[^a-zA-Z0-9\\-\\.]+"), "-") + "-SNAPSHOT"
    }

    private fun getVersionFromChangeLog(): String? {
        val changelogFile = File(project.rootDir, ChangelogManager.DEFAULT_FILE_NAME)
        if (changelogFile.exists()) {
            // тут версия может отсутствовать например при сборке в мастере когда уже произошла ротация
            // и не заполнен NextVersionType в changelog.md
            return appendBranchNameAndSnapshot(ChangelogManager(changelogFile).getNextVersion())
        }
        // Репозиторий без changelog.md
        return null
    }
}
