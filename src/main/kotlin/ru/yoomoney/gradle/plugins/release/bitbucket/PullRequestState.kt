package ru.yoomoney.gradle.plugins.release.bitbucket

/**
 * Статусы PR-ов
 *
 * @author horyukova
 * @since 18.12.2020
 */
enum class PullRequestState(val code: String) {
    /**
     * Открытый PR
     */
    OPEN("OPEN"),
    /**
     * PR задеклайнен (отменен)
     */
    DECLINED("DECLINED"),
    /**
     * PR смержен
     */
    MERGED("MERGED");
}
