package com.example.doineedto.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.doineedto.R
import com.example.doineedto.data.AppPreferences
import com.example.doineedto.data.AppUpdateManager
import com.example.doineedto.data.UpdateCheckResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UpdateCheckWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val preferences = AppPreferences(applicationContext)
        if (!preferences.isBackgroundUpdateCheckEnabled()) return@withContext Result.success()

        val updateManager = AppUpdateManager(applicationContext)

        try {
            when (val result = updateManager.checkForUpdate()) {
                is UpdateCheckResult.Available -> {
                    val update = result.update
                    if (preferences.getLastNotifiedUpdateVersion() != update.versionName) {
                        val apkFile = updateManager.downloadUpdateApk(update)
                        notifyUpdateReady(updateManager, apkFile, update.versionName)
                        preferences.setLastNotifiedUpdateVersion(update.versionName)
                    }
                    Result.success()
                }

                UpdateCheckResult.UpToDate -> Result.success()
            }
        } catch (error: Exception) {
            Result.retry()
        }
    }

    private fun notifyUpdateReady(
        updateManager: AppUpdateManager,
        apkFile: java.io.File,
        versionName: String,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        ensureNotificationChannel()

        val contentIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            updateManager.installIntent(apkFile),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(applicationContext, UPDATE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(applicationContext.getString(R.string.update_notification_title))
            .setContentText(applicationContext.getString(R.string.update_notification_body, versionName))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(UPDATE_NOTIFICATION_ID, notification)
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = applicationContext.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            UPDATE_CHANNEL_ID,
            applicationContext.getString(R.string.update_notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val UPDATE_CHANNEL_ID = "update_available"
        const val UPDATE_NOTIFICATION_ID = 1001
    }
}
