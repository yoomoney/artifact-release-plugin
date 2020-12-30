package ru.yoomoney.gradle.plugins.release.bitbucket

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*
import javax.annotation.Nonnull

/**
 * Информация о пулреквесте, полученная из bitbucket
 */
class BitbucketPullRequest @JsonCreator private constructor(
        @Nonnull @JsonProperty("id") pullRequestId: Long,
        @Nonnull @JsonProperty("version") version: Long,
        @Nonnull @JsonProperty("state") state: String,
        @Nonnull @JsonProperty("title") title: String,
        @Nonnull @JsonProperty("updatedDate") updatedDate: Long,
        @Nonnull @JsonProperty("links") link: Link) {
    @get:Nonnull
    @Nonnull
    val pullRequestId: Long
    @get:Nonnull
    @Nonnull
    val version: Long
    @get:Nonnull
    @Nonnull
    val state: String
    @get:Nonnull
    @Nonnull
    val title: String
    @get:Nonnull
    @Nonnull
    val updatedDate: Long
    @Nonnull
    private val link: Link

    @Nonnull
    fun getLink(): String {
        return link.self[0].href
    }

    override fun toString(): String {
        return ("BitbucketPullRequest{"
                + "pullRequestId=" + pullRequestId
                + ", version=" + version
                + ", state=" + state
                + ", title=" + title
                + ", updatedDate=" + updatedDate
                + ", link=" + link
                + "}")
    }

    /**
     * Объект с ссылками
     */
    class Link @JsonCreator constructor(@Nonnull @JsonProperty("self") self: List<Href>?) {
        @get:JsonProperty("self")
        @get:Nonnull
        @Nonnull
        val self: List<Href>

        override fun toString(): String {
            return "Links{" +
                    "self=" + self +
                    '}'
        }

        init {
            this.self = self!!
        }
    }

    /**
     * Объект с ссылкой
     */
    class Href @JsonCreator constructor(@Nonnull @JsonProperty("href") href: String) {
        @get:JsonProperty("href")
        @get:Nonnull
        @Nonnull
        val href: String

        override fun toString(): String {
            return "Href{" +
                    "href='" + href + '\'' +
                    '}'
        }

        init {
            this.href = Objects.requireNonNull(href, "href")
        }
    }

    init {
        this.pullRequestId = Objects.requireNonNull(pullRequestId, "pullRequestId")
        this.version = Objects.requireNonNull(version, "version")
        this.state = Objects.requireNonNull(state, "state")
        this.title = Objects.requireNonNull(title, "title")
        this.updatedDate = Objects.requireNonNull(updatedDate, "updatedDate")
        this.link = Objects.requireNonNull(link, "link")
    }
}