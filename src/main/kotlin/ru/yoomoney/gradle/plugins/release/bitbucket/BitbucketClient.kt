package ru.yoomoney.gradle.plugins.release.bitbucket

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.mashape.unirest.http.HttpResponse
import com.mashape.unirest.http.Unirest
import com.mashape.unirest.http.exceptions.UnirestException
import org.apache.http.HttpHeaders
import org.apache.http.HttpStatus
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.Optional
import javax.annotation.Nonnull

/**
 * Http-клиент к bitbucket
 *
 *
 * https://developer.atlassian.com/static/rest/bitbucket-server/latest/bitbucket-rest.html
 *
 * @author Oleg Kandaurov
 * @since 28.01.2019
 */
class BitbucketClient(private val settings: BitbucketConnectionSettings) {
    private val objectMapper: ObjectMapper

    /**
     * Возвращает ссылку на последний вмердженный ПР указанного репозитория
     *
     * @param project проект
     * @param repository репозиторий
     * @param state состояние. Возможные значения: OPEN, DECLINED or MERGED.
     */
    @Nonnull
    fun getLatestPullRequestLink(
        @Nonnull project: String?,
        @Nonnull repository: String?,
        @Nonnull state: PullRequestState
    ): Optional<BitbucketPullRequestLink> {
        log.info("getLatestMergedPullRequestLink: project={}, repository={}", project, repository)
        val response: HttpResponse<String>
        response = try {
            val urlPattern = "%s%s/rest/api/1.0/projects/%s/repos/%s/pull-requests?state=%s&order=NEWEST"
            val host = settings.uri!!.toASCIIString()
            val schema = if (host.startsWith("http")) "" else "https://" // если host содержит схему
            Unirest
                    .get(String.format(urlPattern, schema, host, project, repository, state.code))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer ${settings.apiToken}")
                    .asString()
        } catch (e: UnirestException) {
            throw RuntimeException("can't getLatestMergedPullRequestLink", e)
        }
        log.debug("getLatestMergedPullRequestLink: response={}", response)
        if (response.getStatus() !== HttpStatus.SC_OK || response.getBody() == null) {
            throw RuntimeException("can't getLatestMergedPullRequestLink: " +
                    "code=${response.getStatus()}, body=${response.getBody()}")
        }
        val pullRequestInfoResponse: BitbucketPullRequestInfoResponse
        try {
            pullRequestInfoResponse = objectMapper
                    .readValue(response.getBody(), BitbucketPullRequestInfoResponse::class.java)
        } catch (e: IOException) {
            log.info("can't parse response: response={}", response.getBody(), e)
            return Optional.empty()
        }
        if (pullRequestInfoResponse.getPullRequests().isEmpty()) {
            log.info("List of pull-request is empty: response={}", response.getBody())
            return Optional.empty()
        }
        val bitbucketPullRequest: BitbucketPullRequest? = pullRequestInfoResponse.getPullRequests().get(0)
        log.info("getLatestMergedPullRequestLink: pullRequest={}", bitbucketPullRequest)
        return Optional.of(BitbucketPullRequestLink.Companion.builder()
                .withPullRequestId(bitbucketPullRequest!!.pullRequestId)
                .withLink(bitbucketPullRequest!!.getLink())
                .build())
    }

    /**
     * Возвращает список коммитов пул риквеста
     */
    @Nonnull
    fun getPullRequestCommits(
        @Nonnull project: String?,
        @Nonnull repository: String?,
        pullRequestId: Long
    ): List<BitbucketPullRequestCommit> {
        log.info("getPullRequestCommits: project={}, repository={}", project, repository)
        val response: HttpResponse<String>
        response = try {
            val urlPattern = "%s%s/rest/api/1.0/projects/%s/repos/%s/pull-requests/%s/commits"
            val host = settings.uri!!.toASCIIString()
            val schema = if (host.startsWith("http")) "" else "https://" // если host содержит схему
            Unirest
                    .get(String.format(urlPattern, schema, host, project, repository, pullRequestId))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer ${settings.apiToken}")
                    .asString()
        } catch (e: UnirestException) {
            throw RuntimeException("can't getLatestMergedPullRequestLink", e)
        }
        log.debug("getPullRequestCommits: response={}", response)
        if (response.getStatus() !== HttpStatus.SC_OK || response.getBody() == null) {
            throw RuntimeException("can't getPullRequestCommits: " +
                    "code=${response.getStatus()}, body=${response.getBody()}")
        }
        val pullRequestCommitsResponse: BitbucketPullRequestCommitsResponse
        try {
            pullRequestCommitsResponse = objectMapper
                    .readValue(response.getBody(), BitbucketPullRequestCommitsResponse::class.java)
        } catch (e: IOException) {
            throw RuntimeException(java.lang.String.format("can't getPullRequestCommits: code=%s, body=%s",
                    response.getStatus(), response.getBody()), e)
        }
        return pullRequestCommitsResponse.pullRequestsCommits
    }

    companion object {
        private val log = LoggerFactory.getLogger(BitbucketClient::class.java)
        private const val ANY_REF = "ANY_REF_MATCHER_ID"
        /**
         * Ключ hook-a: 'Bitbucket Server Webhook to Jenkins'
         */
        const val BITBUCKET_TO_JENKINS_HOOK_KEY = "com.nerdwin15.stash-stash-webhook-jenkins:jenkinsPostReceiveHook"
    }

    init {
        objectMapper = ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }
}
