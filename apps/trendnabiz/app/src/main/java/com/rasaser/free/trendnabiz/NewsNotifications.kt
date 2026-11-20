package com.rasaser.free.trendnabiz

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object NewsNotifications {
    const val CRITICAL = "trendnabiz_critical_v1"
    const val GENERAL = "trendnabiz_general_v1"
    private const val TTL = 180000L
    private val recentlySent = LinkedHashMap<String,Long>() // transient identifiers only, no content
    const val EXTRA_ACTION = "news_action"
    const val EXTRA_ID = "news_id"
    const val EXTRA_HEADLINE = "news_headline"
    const val EXTRA_SUMMARY = "news_summary"
    const val EXTRA_SOURCE = "news_source"
    const val EXTRA_URL = "news_url"
    const val EXTRA_TIME = "news_time"
    const val EXTRA_SEVERITY = "news_severity"
    fun readStory(intent: Intent?): NewsItem? {
        if(intent == null) return null
        val id=intent.getStringExtra(EXTRA_ID) ?: return null
        val title=intent.getStringExtra(EXTRA_HEADLINE) ?: return null
        val url=intent.getStringExtra(EXTRA_URL) ?: return null
        if(!url.startsWith("https://")) return null
        return NewsItem(
            id=id,title=title,
            description=intent.getStringExtra(EXTRA_SUMMARY) ?: "",
            source=intent.getStringExtra(EXTRA_SOURCE) ?: "Source",
            sourceUrl=url,publishedAt=intent.getLongExtra(EXTRA_TIME,System.currentTimeMillis()),
            severity=intent.getStringExtra(EXTRA_SEVERITY) ?: "normal"
        )
    }

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CRITICAL, "TRENDNABIZ · Kritik haberler", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Önemli ve kritik olaylar için sesli bildirim"
                enableVibration(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(GENERAL, "TRENDNABIZ · Dünya haberleri", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Yeni haber ve olayların bildirimleri"
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
        )
    }

    fun allowed(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun pending(context: Context, story: NewsItem, action: String, salt: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_ACTION, action)
            putExtra(EXTRA_ID, story.id)
            putExtra(EXTRA_HEADLINE, story.title)
            putExtra(EXTRA_SUMMARY, story.description)
            putExtra(EXTRA_SOURCE, story.source)
            putExtra(EXTRA_URL, story.sourceUrl)
            putExtra(EXTRA_TIME, story.publishedAt)
            putExtra(EXTRA_SEVERITY, story.severity)
        }
        return PendingIntent.getActivity(
            context,
            (story.id + ":" + action + ":" + salt).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun publish(context: Context, item: NewsItem, test: Boolean = false) {
        if (!allowed(context)) return
        createChannels(context)
        if (!test && UserPrefs.criticalOnly(context) && !item.critical) return
        val now=System.currentTimeMillis()
        val timeout = if(test) TTL else (item.publishedAt+TTL-now).coerceIn(0L, TTL)
        if(timeout<=0L) return
        synchronized(recentlySent) {
            recentlySent.entries.removeAll { it.value<=now }
            if(!test && recentlySent.containsKey(item.id)) return
            if(!test) recentlySent[item.id]=now+timeout
        }
        val channel = if (item.critical || test) CRITICAL else GENERAL
        val lang = UserPrefs.language(context)
        val sourceText = "${UiText.get(lang, "source")}: ${item.source}"
        val description = NewsLocalization.description(item, lang)
        val logo = BitmapFactory.decodeResource(context.resources, R.drawable.brand_mark)
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(logo)
            .setContentTitle(item.title)
            .setContentText(description)
            .setStyle(NotificationCompat.BigTextStyle().bigText("${description}\n$sourceText"))
            .setSubText(sourceText)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setColor(ContextCompat.getColor(context, R.color.tn_blue))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(if (item.critical || test) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setTimeoutAfter(timeout)
            .setContentIntent(pending(context, item, "open", 0))
            .addAction(R.drawable.ic_translate, UiText.get(lang, "translate"), pending(context, item, "translate", 1))
            .addAction(R.drawable.ic_speak, UiText.get(lang, "listen"), pending(context, item, "listen", 2))
            .addAction(R.drawable.ic_share, UiText.get(lang, "share"), pending(context, item, "share", 3))
        try {
            NotificationManagerCompat.from(context).notify(if (test) "test" else item.id, 201, builder.build())
        } catch (_: SecurityException) {
            // Device permission or policy may block delivery.
        }
    }

    fun test(context: Context) {
        val lang = UserPrefs.language(context)
        publish(context, NewsItem(
            id = "trend_test",
            title = UiText.get(lang, "testStory"),
            description = UiText.get(lang, "testDetail"),
            source = "TRENDNABIZ • TEST",
            sourceUrl = "https://trendnabiz-engine.okiminsozu.workers.dev/healthz",
            publishedAt = System.currentTimeMillis(),
            magnitude = 6.1
        ), test = true)
    }
}
