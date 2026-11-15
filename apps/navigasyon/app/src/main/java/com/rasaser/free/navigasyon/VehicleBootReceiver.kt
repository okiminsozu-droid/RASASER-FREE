package com.rasaser.free.navigasyon

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

/**
 * Android 10+ generally prohibits silently raising the UI on BOOT_COMPLETED.
 * For supported head units, the device's own launcher can open the app.
 * This optional receiver offers a user-visible tap-to-resume notification.
 */
class VehicleBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs = context.getSharedPreferences("offline_first_settings", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("auto_drive", false)) return
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = "rasaser_drive"
        nm.createNotificationChannel(NotificationChannel(channel, "RaSaSeR Drive", NotificationManager.IMPORTANCE_DEFAULT))
        val lang = NavLanguage.fromTag(prefs.getString("lang", "tr") ?: "tr")
        val open = Intent(context, MainActivity::class.java)
            .setAction("com.rasaser.free.navigasyon.OPEN_DRIVE")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(context, 23, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val note = Notification.Builder(context, channel)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle(L10n.get(lang, TextKey.TITLE))
            .setContentText(L10n.get(lang, TextKey.NOTIFICATION_BOOT))
            .setContentIntent(pending).setAutoCancel(true).build()
        nm.notify(19001, note)
    }
}
