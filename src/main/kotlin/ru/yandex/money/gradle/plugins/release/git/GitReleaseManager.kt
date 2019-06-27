package ru.yandex.money.gradle.plugins.release.git

import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.PushCommand
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.JschConfigSessionFactory
import org.eclipse.jgit.transport.OpenSshConfig
import org.eclipse.jgit.transport.SshTransport
import org.eclipse.jgit.util.FS
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.File
import java.io.IOException

/**
 * Умеет работать с гитом в рамках релизного цикла
 * @param projectDirectory директория с git
 */
class GitReleaseManager(private val projectDirectory: File) : Closeable {

    companion object {
        private val log: Logger = Logging.getLogger(GitReleaseManager::class.java)
        private const val NEW_VERSION_RELEASE_PREFIX = "[Gradle Release Plugin] - new version commit"
        private const val PRE_TAG_COMMIT_PREFIX = "[Gradle Release Plugin] - pre tag commit"
    }

    private val git: Git = Git(FileRepositoryBuilder()
            .setGitDir(File(projectDirectory, ".git"))
            .readEnvironment()
            .findGitDir()
            .build())

    private fun commit(message: String) {
        log.lifecycle("Commit: $message")
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

    private fun PushCommand.configureTransport(credentials: Credentials) {
        credentials.pathToPrivateSshKey?.let {
            log.lifecycle("Set private ssh key: path={}", credentials.pathToPrivateSshKey)
            val sshSessionFactory = object : JschConfigSessionFactory() {
                override fun getJSch(hc: OpenSshConfig.Host?, fs: FS?): JSch {
                    return super.getJSch(hc, fs).apply {
                        removeAllIdentity()
                        addIdentity(credentials.pathToPrivateSshKey, credentials.passphrase)
                    }
                }

                override fun configure(hc: OpenSshConfig.Host?, session: Session?) {
                    session!!.setConfig("StrictHostKeyChecking", "no")
                }
            }
            this.setTransportConfigCallback {
                val sshTransport = it as SshTransport
                sshTransport.sshSessionFactory = sshSessionFactory
            }
        }
    }

    /**
     * Отправляет на удалённый сервер все коммиты и теги
     * @param credentials Права для доступа к репозиторию
     */
    fun push(credentials: Credentials) {
        val pushCommand = git.push()
                .setPushTags()
                .add(git.repository.fullBranch)
                .setRemote("origin")
        pushCommand.configureTransport(credentials)
        log.lifecycle("Push: refs=${pushCommand.refSpecs}")
        pushCommand.call()
    }

    /**
     * Делает пуш пустого коммита для проверки доступности этой операции
     */
    fun checkPush(credentials: Credentials): Boolean {
        git.commit()
                .setMessage("[Gradle Release Plugin] Check push")
                .setAllowEmpty(true)
                .call()
        try {
            ByteArrayOutputStream().use { outputStream ->
                val pushCommand = git.push()
                        .add(git.repository.fullBranch)
                        .setRemote("origin")
                pushCommand.configureTransport(credentials)
                pushCommand.setOutputStream(outputStream)
                pushCommand.call()
                log.lifecycle("git pushed: refs={}", pushCommand.refSpecs)
                val out = outputStream.toString("UTF-8")
                if (out.isBlank() || out.contains("Create pull request")) {
                    return true
                } else {
                    log.error("git push failed: message={}", out)
                }
            }
        } catch (exc: GitAPIException) {
            log.error("git push failed", exc)
        } catch (exc: IOException) {
            log.error("git push failed", exc)
        }
        return false
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
     */
    fun preTagCommit(version: String) {
        val newFiles = git.status().call().untracked

        if (!newFiles.isEmpty()) {
            val add = git.add()
            log.lifecycle("Add new files for preTagCommit: files={}", newFiles)
            newFiles.forEach { add.addFilepattern(it) }
            add.call()
        }
        commit("$PRE_TAG_COMMIT_PREFIX: '$version'.")
        addTag(version)
    }

    /**
     * Делает коммит в фазе release
     * @param nextVersion версия следующего релиза
     */
    fun newVersionCommit(nextVersion: String) {
        commit("$NEW_VERSION_RELEASE_PREFIX: '$nextVersion'.")
    }

    override fun close() {
        git.close()
    }
}