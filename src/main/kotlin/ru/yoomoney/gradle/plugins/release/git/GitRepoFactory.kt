package ru.yoomoney.gradle.plugins.release.git

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File
import javax.annotation.Nonnull

/**
 * Класс для создания GitRepo
 *
 * @author horyukova
 * @since 24.06.2019
 */
class GitRepoFactory(private val settings: GitSettings) {
    /**
     * Получить объект для работы с git-репозиторием из уже существующей директории
     *
     * @param directory директория с git-репозиторием
     * @return объект для работы с git-репозиторием
     */
    fun createFromExistingDirectory(directory: File): GitRepo {
        return GitRepo(Git(FileRepositoryBuilder()
                    .readEnvironment()
                    .findGitDir(directory)
                    .build()), settings)
    }
}