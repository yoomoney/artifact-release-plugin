package ru.yandex.money.gradle.plugins.release.git

import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.PushCommand
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.JschConfigSessionFactory
import org.eclipse.jgit.transport.OpenSshConfig
import org.eclipse.jgit.transport.SshTransport
import org.eclipse.jgit.util.FS
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import java.io.Closeable
import java.io.File

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

    private fun configureTransport(command: PushCommand, credentials: Credentials) {
        if (credentials.pathToPrivateSshKey != null) {
            log.lifecycle("Set private ssh key: path={}", credentials.pathToPrivateSshKey)
            val sshSessionFactory = object : JschConfigSessionFactory() {
                override fun getJSch(hc: OpenSshConfig.Host?, fs: FS?): JSch {
                    val jsch = super.getJSch(hc, fs)
                    jsch.removeAllIdentity()
                    jsch.addIdentity(credentials.pathToPrivateSshKey)
                    return jsch
                }

                override fun configure(hc: OpenSshConfig.Host?, session: Session?) {
                    session!!.setConfig("StrictHostKeyChecking", "no")
                }
            }
            command.setTransportConfigCallback {
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
        configureTransport(pushCommand, credentials)
        log.lifecycle("Push: refs=${pushCommand.refSpecs}")
        pushCommand.call()

    }

    /**
     * Делает коммит и добавляет тег в фазе preRelease
     * @param version версия для тега и коммита
     */
    fun preTagCommit(version: String) {
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