package com.mirage.bank.core.update

import com.mirage.bank.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AvailableUpdate(
    val tagName: String,
    val versionName: String,
    val releaseNotes: String,
    val apkDownloadUrl: String,
)

sealed class UpdateCheckResult {
    data class UpdateAvailable(val update: AvailableUpdate) : UpdateCheckResult()
    object UpToDate : UpdateCheckResult()
    data class Failed(val reason: String) : UpdateCheckResult()
}

class UpdateChecker(private val gitHubApi: GitHubApi) {

    suspend fun check(currentVersionName: String): UpdateCheckResult = withContext(Dispatchers.IO) {
        val (owner, repo) = BuildConfig.GITHUB_REPO.split("/", limit = 2)
        try {
            val releases = gitHubApi.listReleases(owner, repo)
            val androidRelease = releases
                .asSequence()
                .filter { !it.draft && !it.prerelease }
                .filter { it.tagName.startsWith("android-v") }
                .mapNotNull { release -> SemVer.parse(release.tagName)?.let { it to release } }
                .maxByOrNull { it.first }
                ?: return@withContext UpdateCheckResult.Failed("No Android release found")

            val (remoteVersion, release) = androidRelease
            val localVersion = SemVer.parse(currentVersionName)
                ?: return@withContext UpdateCheckResult.Failed("Could not parse local version")

            if (remoteVersion <= localVersion) {
                return@withContext UpdateCheckResult.UpToDate
            }

            val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") }
                ?: return@withContext UpdateCheckResult.Failed("Release has no APK asset")

            UpdateCheckResult.UpdateAvailable(
                AvailableUpdate(
                    tagName = release.tagName,
                    versionName = "${remoteVersion.major}.${remoteVersion.minor}.${remoteVersion.patch}",
                    releaseNotes = release.body.orEmpty(),
                    apkDownloadUrl = apkAsset.browserDownloadUrl,
                )
            )
        } catch (e: Exception) {
            UpdateCheckResult.Failed(e.message ?: "Update check failed")
        }
    }
}
