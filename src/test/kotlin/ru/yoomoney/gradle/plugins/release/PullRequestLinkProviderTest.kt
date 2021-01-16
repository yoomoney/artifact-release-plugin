package ru.yoomoney.gradle.plugins.release

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
import org.kohsuke.github.GitHubBuilder
import ru.yoomoney.gradle.plugins.release.changelog.PullRequestLinkProvider
import ru.yoomoney.gradle.plugins.release.git.GitManager
import ru.yoomoney.gradle.plugins.release.git.GitSettings
import ru.yoomoney.gradle.plugins.release.github.GitHubClient
import java.io.File


class PullRequestLinkProviderTest {

    @get:Rule
    val projectDir = TemporaryFolder()
    @get:Rule
    val wireMockRule = WireMockRule()

    val gitSettings = GitSettings("email", "user", null, null)

    val githubSettings = PullRequestLinkSettings(true, PullRequestInfoProvider.GIT_HUB,
            "token")

    val bitbucketSettings = PullRequestLinkSettings(true, PullRequestInfoProvider.BITBUCKET,
            null, "user", "password")

    lateinit var git: Git

    @Test
    fun `should fail get pull request link cause can't call github`() {
        // given
        git = Git.init().setDirectory(File(projectDir.root.absolutePath)).setBare(false).call()
        git.remoteSetUrl()
                .setRemoteUri(URIish("http://localhost:${wireMockRule.port() + 1}/yoomoney-tech/db-queue.git"))
                .setRemoteName("origin")
                .call()

        WireMock.stubFor(WireMock.get("/user")
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.SC_OK)
                        .withBody(readTextFromClassPath("/prlink/user.json"))))


        WireMock.stubFor(WireMock.get("/repos/yoomoney-tech/db-queue")
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.SC_BAD_REQUEST)))

        val build = GitHubBuilder()
                .withOAuthToken(githubSettings.githubAccessToken)
                .withEndpoint("http://localhost:${wireMockRule.port()}")
                .build()

        val pullRequestLinkProvider = PullRequestLinkProvider(GitManager(File(projectDir.root.absolutePath), gitSettings),
                GitHubClient(build), githubSettings)

        // when
        val pullRequestLink = pullRequestLinkProvider.getReleasePullRequestLink()

        // then
        assertNull(pullRequestLink)
    }

    @Test
    fun `should fail get pull request link cause github commits differs from git commits`() {
        // given
        git = Git.init().setDirectory(File(projectDir.root.absolutePath)).setBare(false).call()
        git.remoteSetUrl()
                .setRemoteUri(URIish("http://localhost:${wireMockRule.port()}/yoomoney-tech/db-queue.git"))
                .setRemoteName("origin")
                .call()

        git.add().addFilepattern("1.txt").call()
        git.commit().setMessage("1.txt commit").call()

        git.add().addFilepattern("3.txt").call()
        git.commit().setMessage("Unknown commit").call()

        addGitHubStubs("bad_sha")

        val build = GitHubBuilder()
                .withOAuthToken(githubSettings.githubAccessToken)
                .withEndpoint("http://localhost:${wireMockRule.port()}")
                .build()

        val pullRequestLinkProvider = PullRequestLinkProvider(GitManager(File(projectDir.root.absolutePath), gitSettings),
                GitHubClient(build), githubSettings)

        // when
        val pullRequestLink = pullRequestLinkProvider.getReleasePullRequestLink()

        // then
        assertNull(pullRequestLink)
    }

    @Test
    fun `should successfully get pull request link github`() {
        // given
        git = Git.init().setDirectory(File(projectDir.root.absolutePath)).setBare(false).call()
        git.remoteSetUrl()
            .setRemoteUri(URIish("http://localhost:${wireMockRule.port()}/yoomoney-tech/db-queue.git"))
            .setRemoteName("origin")
            .call()

        git.add().addFilepattern("1.txt").call()
        git.commit().setMessage("1.txt commit").call()
        git.tag().setName("1.0.0").call()

        git.add().addFilepattern("3.txt").call()
        val commit = git.commit().setMessage("JavaDoc translation for public packages.").call().toObjectId().name()

        val expectedPullRequestLink = "https://api.github.com/repos/yoomoney-tech/db-queue/pulls/5"

        addGitHubStubs(commit)

        val build = GitHubBuilder()
                .withOAuthToken(githubSettings.githubAccessToken)
                .withEndpoint("http://localhost:${wireMockRule.port()}")
                .build()

        val pullRequestLinkProvider = PullRequestLinkProvider(GitManager(File(projectDir.root.absolutePath), gitSettings),
                GitHubClient(build), githubSettings)

        // when
        val pullRequestLink = pullRequestLinkProvider.getReleasePullRequestLink()

        // then
        assertThat(pullRequestLink, Matchers.equalTo(expectedPullRequestLink))
    }

    @Test
    fun `should successfully get pull request link github with ssh`() {
        // given
        git = Git.init().setDirectory(File(projectDir.root.absolutePath)).setBare(false).call()
        git.remoteSetUrl()
                .setRemoteUri(URIish("git@localhost/yoomoney-tech/db-queue.git"))
                .setRemoteName("origin")
                .call()

        git.add().addFilepattern("1.txt").call()
        git.commit().setMessage("1.txt commit").call()
        git.tag().setName("1.0.0").call()

        git.add().addFilepattern("3.txt").call()
        val commit = git.commit().setMessage("JavaDoc translation for public packages.").call().toObjectId().name()

        val expectedPullRequestLink = "https://api.github.com/repos/yoomoney-tech/db-queue/pulls/5"

        addGitHubStubs(commit)

        val build = GitHubBuilder()
                .withOAuthToken(githubSettings.githubAccessToken)
                .withEndpoint("http://localhost:${wireMockRule.port()}")
                .build()

        val pullRequestLinkProvider = PullRequestLinkProvider(GitManager(File(projectDir.root.absolutePath), gitSettings),
                GitHubClient(build), githubSettings)

        // when
        val pullRequestLink = pullRequestLinkProvider.getReleasePullRequestLink()

        // then
        assertThat(pullRequestLink, Matchers.equalTo(expectedPullRequestLink))
    }

    @Test
    fun `should fail get pull request link cause can't call bitbucket`() {
        // given
        git = Git.init().setDirectory(File(projectDir.root.absolutePath)).setBare(false).call()
        git.remoteSetUrl()
                .setRemoteUri(URIish("http://localhost:${wireMockRule.port() + 1}/BACKEND/kassa.git"))
                .setRemoteName("origin")
                .call()

        val pullRequestLinkProvider = PullRequestLinkProvider(GitManager(File(projectDir.root.absolutePath), gitSettings),
                bitbucketSettings)

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
                        .withBody(readTextFromClassPath("/prlink/get-commit-bitbucket.json"))))

        val pullRequestLinkProvider = PullRequestLinkProvider(GitManager(File(projectDir.root.absolutePath), gitSettings),
                bitbucketSettings)

        // when
        val pullRequestLink = pullRequestLinkProvider.getReleasePullRequestLink()

        // then
        assertNull(pullRequestLink)
    }

    @Test
    fun `should successfully get pull request link bitbucket`() {
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
                        .withBody(readTextFromClassPath("/prlink/get-commit-bitbucket.json")
                                .replace("COMMIT_ID_PLACEHOLDER", secondCommitId))))

        val pullRequestLinkProvider = PullRequestLinkProvider(GitManager(File(projectDir.root.absolutePath), gitSettings), bitbucketSettings)

        // when
        val pullRequestLink = pullRequestLinkProvider.getReleasePullRequestLink()

        // then
        assertThat(pullRequestLink, Matchers.equalTo(expectedPullRequestLink))
    }

    private fun readTextFromClassPath(path: String): String {
        return this::class.java.getResource(path).readText()
    }

    private fun addGitHubStubs(commit_sha: String) {
        WireMock.stubFor(WireMock.get("/repos/yoomoney-tech/db-queue")
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.SC_OK)
                        .withBody(readTextFromClassPath("/prlink/get-repos.json"))))

        WireMock.stubFor(WireMock.get("/repos/yoomoney-tech/db-queue/pulls?state=closed&sort=updated&direction=desc&per_page=1")
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.SC_OK)
                        .withBody(readTextFromClassPath("/prlink/get-pull-request.json"))))

        WireMock.stubFor(WireMock.get("/repos/yoomoney-tech/db-queue/pulls/5/commits")
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.SC_OK)
                        .withBody(readTextFromClassPath("/prlink/get-commits.json")
                                .replace("commit_sha", commit_sha))))

        WireMock.stubFor(WireMock.get("/user")
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.SC_OK)
                        .withBody(readTextFromClassPath("/prlink/user.json"))))

    }
}