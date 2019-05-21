package ru.yandex.money.gradle.plugins.release.git

/**
 * Права для доступа к репозиторию
 */
data class Credentials(val pathToPrivateSshKey: String?, val passphrase: String?)