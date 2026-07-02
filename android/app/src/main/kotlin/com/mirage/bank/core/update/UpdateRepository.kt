package com.mirage.bank.core.update

import android.app.DownloadManager
import android.content.Context
import com.mirage.bank.BuildConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val AUTO_CHECK_THROTTLE_MILLIS = 6L * 60 * 60 * 1000 // 6 hours

sealed class UpdateUiState {
    object Idle : UpdateUiState()
    object Checking : UpdateUiState()
    data class Available(val update: AvailableUpdate) : UpdateUiState()
    object Downloading : UpdateUiState()
    object ReadyToInstall : UpdateUiState()
    data class Error(val message: String) : UpdateUiState()
}

/** Orchestrates UpdateChecker + UpdateDownloader and exposes a single state stream for Settings/Home to observe. */
class UpdateRepository(
    private val context: Context,
    private val checker: UpdateChecker,
    private val downloader: UpdateDownloader,
) {
    private val _state = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val state: StateFlow<UpdateUiState> = _state

    suspend fun checkOnLaunchIfDue() {
        if (!downloader.shouldAutoCheck(AUTO_CHECK_THROTTLE_MILLIS)) return
        checkNow()
    }

    suspend fun checkNow() {
        _state.value = UpdateUiState.Checking
        downloader.markCheckedNow()
        when (val result = checker.check(BuildConfig.VERSION_NAME)) {
            is UpdateCheckResult.UpdateAvailable -> _state.value = UpdateUiState.Available(result.update)
            is UpdateCheckResult.UpToDate -> _state.value = UpdateUiState.Idle
            is UpdateCheckResult.Failed -> _state.value = UpdateUiState.Error(result.reason)
        }
    }

    fun startDownload(update: AvailableUpdate) {
        downloader.enqueue(update)
        _state.value = UpdateUiState.Downloading
    }

    /** Polling fallback used by the UI while Downloading, in case the completion broadcast never fires. */
    suspend fun pollUntilDownloaded() {
        val id = downloader.pendingDownloadId()
        if (id == -1L) return
        while (_state.value is UpdateUiState.Downloading) {
            when (downloader.queryStatus(id)) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    _state.value = UpdateUiState.ReadyToInstall
                    return
                }
                DownloadManager.STATUS_FAILED -> {
                    downloader.clearPendingDownload()
                    _state.value = UpdateUiState.Error("Download failed, please retry")
                    return
                }
            }
            delay(2000)
        }
    }

    fun promptInstall() {
        val intent = ApkInstaller.installIntent(context, downloader.apkFile())
        context.startActivity(intent)
    }

    fun canInstallUnknownApps(): Boolean = ApkInstaller.canInstallUnknownApps(context)

    fun unknownSourcesSettingsIntent() = ApkInstaller.unknownSourcesSettingsIntent(context)

    fun dismiss() {
        _state.value = UpdateUiState.Idle
    }
}
