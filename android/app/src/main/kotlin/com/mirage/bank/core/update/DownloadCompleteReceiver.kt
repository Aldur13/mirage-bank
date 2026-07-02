package com.mirage.bank.core.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Fires when any DownloadManager download completes. We only act if the id
 * matches the update APK we enqueued; other app downloads aren't expected
 * but this guards against false triggers regardless.
 */
class DownloadCompleteReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return

        val completedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
        val downloader = UpdateDownloader(context.applicationContext)
        val pendingId = downloader.pendingDownloadId()
        if (completedId == -1L || completedId != pendingId) return

        val status = downloader.queryStatus(completedId)
        if (status == DownloadManager.STATUS_SUCCESSFUL) {
            downloader.clearPendingDownload()
            val installIntent = ApkInstaller.installIntent(context.applicationContext, downloader.apkFile())
            context.applicationContext.startActivity(installIntent)
        }
        // On failure we deliberately do nothing here -- the Settings "check
        // for updates" flow lets the user retry, and UpdateRepository's
        // polling fallback covers the case where this broadcast is missed.
    }
}
