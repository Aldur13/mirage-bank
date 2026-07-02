package com.mirage.bank.core.update

import android.app.DownloadManager
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Environment
import androidx.core.content.getSystemService
import java.io.File

private const val PREFS_NAME = "mirage_update_prefs"
private const val KEY_DOWNLOAD_ID = "pending_download_id"
private const val KEY_LAST_CHECK_EPOCH_MS = "last_check_epoch_ms"
const val UPDATE_APK_FILENAME = "mirage-bank-update.apk"

/** Thin wrapper around Android's DownloadManager -- the standard, no-root way to fetch a large file in the background. */
class UpdateDownloader(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun enqueue(update: AvailableUpdate): Long {
        val downloadManager = context.getSystemService<DownloadManager>()
            ?: error("DownloadManager unavailable")

        // Clear out any stale partial download from a previous attempt.
        apkFile().delete()

        val request = DownloadManager.Request(Uri.parse(update.apkDownloadUrl))
            .setTitle("Mirage Bank update ${update.versionName}")
            .setDescription("Downloading update...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, UPDATE_APK_FILENAME)
            .setAllowedOverMetered(true)

        val id = downloadManager.enqueue(request)
        prefs.edit().putLong(KEY_DOWNLOAD_ID, id).apply()
        return id
    }

    fun pendingDownloadId(): Long = prefs.getLong(KEY_DOWNLOAD_ID, -1L)

    fun clearPendingDownload() {
        prefs.edit().remove(KEY_DOWNLOAD_ID).apply()
    }

    /** Polling fallback for when the completion broadcast is missed (e.g. notification permission denied). */
    fun queryStatus(downloadId: Long): Int? {
        val downloadManager = context.getSystemService<DownloadManager>() ?: return null
        val query = DownloadManager.Query().setFilterById(downloadId)
        downloadManager.query(query)?.use { cursor ->
            if (!cursor.moveToFirst()) return null
            val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            if (statusIdx < 0) return null
            return cursor.getInt(statusIdx)
        }
        return null
    }

    fun apkFile(): File =
        File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), UPDATE_APK_FILENAME)

    fun shouldAutoCheck(throttleMillis: Long): Boolean {
        val last = prefs.getLong(KEY_LAST_CHECK_EPOCH_MS, 0L)
        return System.currentTimeMillis() - last >= throttleMillis
    }

    fun markCheckedNow() {
        prefs.edit().putLong(KEY_LAST_CHECK_EPOCH_MS, System.currentTimeMillis()).apply()
    }
}
