package com.rasaser.free.trendnabiz

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * Explicitly opt-in foreground monitoring for debug/test APKs.
 * FGS specialUse may require Google Play declaration/review for production.
 * No claim of unlimited background or 99% delivery. Network can still pause in Doze.
 */
class LiveMonitoringService : Service() {
    private var live: LiveNewsClient? = null

    override fun onCreate() {
        super.onCreate()
        val manager=getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(NotificationChannel(
            CHANNEL, "TRENDNABIZ • Canlı izleme", NotificationManager.IMPORTANCE_LOW
        ).apply { description="Kullanıcının açtığı canlı haber izleme bağlantısı" })
    }
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
        if(intent?.action==ACTION_STOP) {
            stopWatching()
            return START_NOT_STICKY
        }
        if(intent?.action!=ACTION_START || !BuildConfig.DEBUG) {
            stopWatching()
            return START_NOT_STICKY
        }
        val open=PendingIntent.getActivity(this,333,Intent(this,MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stop=PendingIntent.getService(this,334,Intent(this,LiveMonitoringService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification:Notification=NotificationCompat.Builder(this,CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("TRENDNABIZ • Canlı izleme açık")
            .setContentText("Yeni olaylar dinleniyor • Durdurmak için dokunun")
            .setContentIntent(open)
            .addAction(R.drawable.ic_notification,"İzlemeyi durdur",stop)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
        try {
            if(Build.VERSION.SDK_INT>=34) {
                startForeground(NOTIFICATION_ID,notification,android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else startForeground(NOTIFICATION_ID,notification)
        } catch (_:Exception) {
            UserPrefs.setMonitoring(this,false)
            stopSelf()
            return START_NOT_STICKY
        }
        UserPrefs.setMonitoring(this,true)
        if(live==null) {
            live=LiveNewsClient(
                onEvent={story->NewsNotifications.publish(this,story)},
                onConnected={},
                onState={}
            )
            live?.start()
        }
        return START_NOT_STICKY
    }
    private fun stopWatching() {
        live?.stop()
        live=null
        UserPrefs.setMonitoring(this,false)
        if(Build.VERSION.SDK_INT>=24) stopForeground(STOP_FOREGROUND_REMOVE)
        else stopForeground(true)
        stopSelf()
    }
    override fun onDestroy() {
        live?.stop()
        live=null
        UserPrefs.setMonitoring(this,false)
        super.onDestroy()
    }
    override fun onBind(intent:Intent?):IBinder?=null

    companion object {
        const val ACTION_START="com.rasaser.free.trendnabiz.START_LIVE"
        const val ACTION_STOP="com.rasaser.free.trendnabiz.STOP_LIVE"
        const val CHANNEL="trendnabiz_live_monitor_v1"
        const val NOTIFICATION_ID=570001
    }
}
