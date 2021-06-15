package ru.yoomoney.gradle.plugins.release.git

import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.revwalk.RevCommit
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import java.io.Closeable
import java.io.File

/**
 * Умеет работать с гитом в рамках релизного цикла
 * @param projectDirectory директория с git
 */
class GitManager(private val projectDirectory: File, gitSettings: GitSettings) : Closeable {

    companion object {
        private val log: Logger = Logging.getLogger(GitManager::class.java)
        private const val NEW_VERSION_RELEASE_PREFIX = "[Gradle Release Plugin] - new version commit"
        private const val PRE_TAG_COMMIT_PREFIX = "[Gradle Release Plugin] - pre tag commit"
        private val defaultAllowedFilesToCommit = listOf("CHANGELOG.md", "gradle.properties")
    }

    private val git: GitRepo = GitRepoFactory(gitSettings).createFromExistingDirectory(projectDirectory)

    private fun commit(message: String, allowedFilesForCommitRegexStr: List<String>) {
        log.lifecycle("Commit: $message")

        val newFiles = git.status().call().untracked
        val allowedFilesForCommitRegex = allowedFilesForCommitRegexStr.map { it.toRegex() }

        val gitAddCmd = git.add()

        if (newFiles.isNotEmpty()) {
            newFiles.forEach { fileName ->
                allowedFilesForCommitRegex.forEach {
                    if (it.matches(fileName)) {
                        log.lifecycle("Add file for commit: file={}", fileName)
                        gitAddCmd.addFilepattern(fileName)
                    }
                }
            }
        }

        defaultAllowedFilesToCommit.forEach {
            log.lifecycle("Add file for commit: file={}", it)
            gitAddCmd.addFilepattern(it)
        }

        gitAddCmd.call()

        git.commit()
                .setAll(true)
                .setMessage(message)
                .call()
    }

    private fun addTag(tag: String) {
        log.lifecycle("Tag : $tag")
        git.tag()
                .setName(tag)
                .call()
    }

    /**
     * Проверяет есть ли такой tag
     */
    fun isTagExists(version: String): Boolean {
        val tag = git.repository.refDatabase.getRefsByPrefix(Constants.R_TAGS).filter {
            it.name == Constants.R_TAGS + version
        }
        return !tag.isEmpty()
    }

    /**
     * Отправляет на удалённый сервер все коммиты и теги
     * @param credentials Права для доступа к репозиторию
     */
    fun push() {
        git.push { pushCommand ->
            pushCommand
                    .setPushTags()
                    .add(git.repository.fullBranch)
                    .setRemote("origin")
        }
    }

    /**
     * Делает пуш пустого коммита для проверки доступности этой операции
     */
    fun checkPush(): Boolean {
        git.commit()
                .setMessage("[Gradle Release Plugin] Check push")
                .setAllowEmpty(true)
                .call()
        val resultMessage = git.push { pushCommand ->
            pushCommand
                    .add(git.repository.fullBranch)
                    .setRemote("origin")
        }
        return resultMessage == null
    }

    /**
     * Все незакоммиченные изменения
     */
    fun getUncommittedChanges(): Set<String> {
        val status = git.status().call()
        return status.uncommittedChanges
    }

    /**
     * Делает коммит и добавляет тег в фазе preRelease
     * @param version версия для тега и коммита
     * @param allowedFilesForCommitRegexStr кастомный список regex паттернов для файлов, которые попадут в коммит
     */
    fun preTagCommit(version: String, allowedFilesForCommitRegexStr: List<String>) {
        commit("$PRE_TAG_COMMIT_PREFIX: '$version'.", allowedFilesForCommitRegexStr)
        addTag(version)
    }

    /**
     * Делает коммит в фазе release
     * @param nextVersion версия следующего релиза
     * @param allowedFilesForCommitRegexStr кастомный список regex паттернов для файлов, которые попадут в коммит
     */
    fun newVersionCommit(nextVersion: String, allowedFilesForCommitRegexStr: List<String>) {
        commit("$NEW_VERSION_RELEASE_PREFIX: '$nextVersion'.", allowedFilesForCommitRegexStr)
    }

    /**
     * Возвращает remoteOriginUrl
     */
    fun getRemoteOriginUrl(): String = git.remoteOriginUrl

    /**
     * Возвращает коммиты с последнего тэга до head
     */
    fun getCommitsFromLastTagToHead(): Iterable<RevCommit> {
        val tags = git.listTags()
        return if (tags.isEmpty()) {
            git.log().add(git.headCommit).call()
        } else {
            val lastTaggedCommit = git.repository.parseCommit(tags.last().objectId)
            git.log().addRange(lastTaggedCommit, git.headCommit).call()
        }
    }

    override fun close() {
        git.close()
    }
}
