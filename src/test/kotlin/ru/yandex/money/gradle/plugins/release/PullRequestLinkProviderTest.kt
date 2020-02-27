package ru.yandex.money.gradle.plugins.release

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.junit.WireMockRule
import org.apache.http.HttpStatus
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.URIish
import org.hamcrest.Matchers
import org.junit.Assert.assertNull
import org.junit.Assert.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import ru.yandex.money.gradle.plugins.release.changelog.PullRequestLinkProvider
import ru.yandex.money.gradle.plugins.release.git.GitManager
import ru.yandex.money.tools.git.GitSettings
import java.io.File

class PullRequestLinkProviderTest {

    @get:Rule
    val projectDir = TemporaryFolder()
    @get:Rule
    val wireMockRule = WireMockRule()

    val gitSettings = GitSettings.builder()
            .withEmail("email")
            .withUsername("user")
            .build()

    val pullRequestLinkSettings = PullRequestLinkSettings(true, "user", "password")

    lateinit var git: Git

    @Test
    fun `should fail get pull request link cause can't call bitbucket`() {
        // given
        git = Git.init().setDirectory(File(projectDir.root.absolutePath)).setBare(false).call()
        git.remoteSetUrl()
                .setRemoteUri(URIish("http://localhost:${wireMockRule.port() + 1}/BACKEND/kassa.git"))
                .setRemoteName("origin")
                .call()

        val pullRequestLinkProvider = PullRequestLinkProvider(GitManager(File(projectDir.root.absolutePath), gitSettings), pullRequestLinkSettings)

        // when
        val pullRequestLink = pullRequestLinkProvider.getReleasePullRequestLink()

        // then
        assertNull(pullRequestLink)
    }

    @Test
    fun `should fail get pull request link cause bitbucket commits differs from git commits`() {
        // given
        git = Git.init().setDirectory(File(projectDir.root.absolutePath)).setBare(false).call()
        git.remoteSetUrl()
                .setRemoteUri(URIish("http://localhost:${wireMockRule.port()}/BACKEND/kassa.git"))
                .setRemoteName("origin")
                .call()

        git.add().addFilepattern("1.txt").call()
        git.commit().setMessage("1.txt commit").call()

        git.add().addFilepattern("3.txt").call()
        git.commit().setMessage("3.txt commit").call()

        WireMock.stubFor(WireMock.get("/rest/api/1.0/projects/BACKEND/repos/kassa/pull-requests?state=MERGED&order=NEWEST")
                .withBasicAuth("user", "password")
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.SC_OK)
                        .withBody(readTextFromClassPath("/prlink/get-latest-merged-pull-request-link.json"))))

        WireMock.stubFor(WireMock.get("/rest/api/1.0/projects/BACKEND/repos/kassa/pull-requests/1/commits")
                .withBasicAuth("user", "password")
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.SC_OK)
                        .withBody(readTextFromClassPath("/prlink/get-pull-request-commits.json"))))

        val pullRequestLinkProvider = PullRequestLinkProvider(GitManager(File(projectDir.root.absolutePath), gitSettings), pullRequestLinkSettings)

        // when
        val pullRequestLink = pullRequestLinkProvider.getReleasePullRequestLink()

        // then
        assertNull(pullRequestLink)
    }

    @Test
    fun `should successfully get pull request link`() {
        // given
        git = Git.init().setDirectory(File(projectDir.root.absolutePath)).setBare(false).call()
        git.remoteSetUrl()
            .setRemoteUri(URIish("http://localhost:${wireMockRule.port()}/BACKEND/kassa.git"))
            .setRemoteName("origin")
            .call()

        git.add().addFilepattern("1.txt").call()
        git.commit().setMessage("1.txt commit").call()
        git.tag().setName("1.0.0").call()

        git.add().addFilepattern("2.txt").call()
        val secondCommitId = git.commit().setMessage("2.txt commit").call().toObjectId().name()

        git.add().addFilepattern("3.txt").call()
        git.commit().setMessage("3.txt commit").call()

        val expectedPullRequestLink = "https://bitbucket.yamoney.ru/projects/BACKEND/repos/kassa/pull-requests/777"

        WireMock.stubFor(WireMock.get("/rest/api/1.0/projects/BACKEND/repos/kassa/pull-requests?state=MERGED&order=NEWEST")
                .withBasicAuth("user", "password")
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.SC_OK)
                        .withBody(readTextFromClassPath("/prlink/get-latest-merged-pull-request-link.json")
                                .replace("PULL_REQUEST_LINK_PLACEHOLDER", expectedPullRequestLink))))

        WireMock.stubFor(WireMock.get("/rest/api/1.0/projects/BACKEND/repos/kassa/pull-requests/1/commits")
                .withBasicAuth("user", "password")
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.SC_OK)
                        .withBody(readTextFromClassPath("/prlink/get-pull-request-commits.json")
                                .replace("COMMIT_ID_PLACEHOLDER", secondCommitId))))

        val pullRequestLinkProvider = PullRequestLinkProvider(GitManager(File(projectDir.root.absolutePath), gitSettings), pullRequestLinkSettings)

        // when
        val pullRequestLink = pullRequestLinkProvider.getReleasePullRequestLink()

        // then
        assertThat(pullRequestLink, Matchers.equalTo(expectedPullRequestLink))
    }

    private fun readTextFromClassPath(path: String): String {
        return this::class.java.getResource(path).readText()
    }
}