package ru.yandex.money.gradle.plugins.release.git

import com.jcraft.jsch.JSch
import com.jcraft.jsch.KeyPair
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.junit.ssh.SshTestGitServer
import org.eclipse.jgit.transport.URIish
import org.hamcrest.Matchers.greaterThan
import org.hamcrest.Matchers.startsWith
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import ru.yandex.money.gradle.plugins.release.AbstractReleaseTest
import java.io.File
import java.io.FileOutputStream

/**
 * Тесты GitReleaseManager
 *
 * @author horyukova
 * @since 05.04.2019
 */

private const val TEST_USER_NAME = "git"

class GitReleaseManagerTest : AbstractReleaseTest() {

    lateinit var gitReleaseManager: GitReleaseManager
    lateinit var sshGitServer: SshTestGitServer
    lateinit var gitSshOrigin: Git
    lateinit var userKey: File

    @get:Rule
    var sshOriginRepoFolder = TemporaryFolder()

    @get:Rule
    var sshKeysFolder = TemporaryFolder()

    @Before
    fun before() {
        gitReleaseManager = GitReleaseManager(File(projectDir.root.absolutePath))

        gitSshOrigin = Git.init().setDirectory(sshOriginRepoFolder.root)
                .setBare(true)
                .call()

        userKey = sshKeysFolder.newFile("userKey")
        val publicUserKey = createKeyPair(userKey)
        val hostKey = sshKeysFolder.newFile("hostKey")
        createKeyPair(hostKey)

        sshGitServer = SshTestGitServer(TEST_USER_NAME,
                publicUserKey.toPath(),
                gitSshOrigin.repository,
                hostKey.readBytes())

        val gitServerPort = sshGitServer.start()
        assertThat("Ssh Git Server is up and running", gitServerPort, greaterThan(0))

        val remoteSetSsh = git.remoteSetUrl()
        remoteSetSsh.setUri(URIish("ssh://$TEST_USER_NAME@localhost:$gitServerPort/anyrepo"))
        remoteSetSsh.setName("origin")
        remoteSetSsh.call()
    }

    @After
    fun tearDown() {
        sshGitServer.stop()
    }

    @Test
    fun `should correctly check tag exists `() {
        git.tag().setName("2.0.0").call()
        git.branchRename().setNewName("3.0.0").call()

        assertTrue(gitReleaseManager.isTagExists("2.0.0"))
        assertFalse(gitReleaseManager.isTagExists("3.0.0"))
    }

    @Test
    fun `should push to repo with ssh `() {

        assertTrue(gitReleaseManager.checkPush(Credentials(userKey.path, null)))

        assertThat("commit is available in local repo",
                getCommitMessages(git).first(), startsWith("[Gradle Release Plugin] Check push"))
        assertThat("commit is available in remote repo",
                getCommitMessages(gitSshOrigin).first(), startsWith("[Gradle Release Plugin] Check push"))
    }

    @Test
    fun `should have indication if no ssh key defined for ssh repo `() {

        assertFalse(gitReleaseManager.checkPush(Credentials(null, null)))
    }

    private fun createKeyPair(privateKeyFile: File): File {
        val jsch = JSch()
        val pair = KeyPair.genKeyPair(jsch, KeyPair.RSA, 2048)
        FileOutputStream(privateKeyFile).use { pair.writePrivateKey(it) }
        val publicKeyFile = File(privateKeyFile.parentFile,
                privateKeyFile.name + ".pub")
        FileOutputStream(publicKeyFile).use { pair.writePublicKey(it, "git") }
        return publicKeyFile
    }
}