package ru.yoomoney.gradle.plugins.release.git

/**
 * Настройки для работы с git-репозиторием
 *
 * @author horyukova
 * @since 19.12.2020
 */
data class GitSettings(
    val username: String,
    val email: String,
    val sshKeyPath: String?,
    val passphraseSshKey: String?
)