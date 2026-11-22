package com.rasaser.free.icerikureticisi

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * A reminder is NOT an API publication. Actual uploads require the user's action
 * in the official social-network app or an approved per-platform OAuth API.
 */
object ShareReminders {
    const val CHANNEL = "rasaser_share_reminders"
    private const val ID = "schedule_id"

    fun canNotify(context: Context): Boolean =
        (Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun enqueue(context: Context, scheduleId: Long, scheduledAt: Long) {
        val data = Data.Builder().putLong(ID, scheduleId).build()
        val request = OneTimeWorkRequestBuilder<ShareReminderWorker>()
            .setInputData(data)
            .setInitialDelay((scheduledAt - System.currentTimeMillis()).coerceAtLeast(0L),
                TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "share-reminder-$scheduleId", ExistingWorkPolicy.REPLACE, request
        )
    }

    fun cancel(context: Context, scheduleId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork("share-reminder-$scheduleId")
    }
}

class ShareReminderWorker(
    appContext: Context, parameters: WorkerParameters
) : Worker(appContext, parameters) {
    override fun doWork(): Result {
        val id = inputData.getLong("schedule_id", -1L)
        if (id < 0L) return Result.success()
        val database = CreatorDatabase(applicationContext)
        try {
            val item = database.scheduledShare(id) ?: return Result.success()
            database.markScheduleReady(id)
            if (!ShareReminders.canNotify(applicationContext)) return Result.success()
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
            if (Build.VERSION.SDK_INT >= 26) {
                manager.createNotificationChannel(
                    NotificationChannel(
                        ShareReminders.CHANNEL,
                        "Planlanan paylaşımlar",
                        NotificationManager.IMPORTANCE_DEFAULT
                    ).apply {
                        description = "Hazırladığınız fotoğraf ve videoların paylaşım zamanı"
                    }
                )
            }
            val open = Intent(applicationContext, MainActivity::class.java)
                .putExtra("creator_id", item.creatorId)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val pending = PendingIntent.getActivity(
                applicationContext,
                id.toInt(),
                open,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val notification = NotificationCompat.Builder(
                applicationContext, ShareReminders.CHANNEL
            )
                .setSmallIcon(android.R.drawable.ic_menu_send)
                .setContentTitle("Planlanan paylaşımınız hazır")
                .setContentText("Seçtiğiniz hesaplarda yayınlamak için dokunun.")
                .setContentIntent(pending)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
            NotificationManagerCompat.from(applicationContext).notify(id.toInt(), notification)
        } catch (_: SecurityException) {
            // Device denied notification access; draft remains ready in local database.
        } finally {
            database.close()
        }
        return Result.success()
    }
}
