package com.rasaser.free.icerikureticisi

import android.app.Notification
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class SocialNotice(
    val key: String,
    val packageName: String,
    val title: String,
    val message: String,
    val postedAt: Long
)

/**
 * Only the currently ACTIVE notifications from linked Android apps are kept in RAM.
 * Their text is never written to SQLite, a file, logs or any server.
 * Android grants notification access globally; the user must opt in via Settings.
 */
object VisibleNotices {
    var active by mutableStateOf<List<SocialNotice>>(emptyList())
        private set
    private var allowed: Set<String> = emptySet()

    fun configure(context: Context) {
        allowed = CreatorDatabase(context).trackedPackages()
        active = active.filter { it.packageName in allowed }
        SocialNotificationListener.resyncIfConnected()
    }

    fun replace(all: List<SocialNotice>) {
        active = all.filter { it.packageName in allowed }.sortedByDescending { it.postedAt }
    }

    fun posted(notice: SocialNotice) {
        if (notice.packageName !in allowed) return
        active = (active.filterNot { it.key == notice.key } + notice)
            .sortedByDescending { it.postedAt }
    }

    fun removed(key: String) {
        active = active.filterNot { it.key == key }
    }
}

class SocialNotificationListener : NotificationListenerService() {
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onListenerConnected() {
        super.onListenerConnected()
        connected = this
        mainHandler.post {
            // The watched-app list is taken from saved user choices.
            VisibleNotices.configure(applicationContext)
        }
    }

    override fun onListenerDisconnected() {
        if (connected === this) connected = null
        mainHandler.post { VisibleNotices.replace(emptyList()) }
        super.onListenerDisconnected()
    }

    override fun onDestroy() {
        if (connected === this) connected = null
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notice = sbn?.toSocialNotice() ?: return
        mainHandler.post { VisibleNotices.posted(notice) }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        val key = sbn?.key ?: return
        mainHandler.post { VisibleNotices.removed(key) }
    }

    private fun StatusBarNotification.toSocialNotice(): SocialNotice? {
        if ((notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return null
        val title = notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val body = notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        return SocialNotice(key, packageName, title, body, postTime)
    }

    private fun resync() {
        val snapshot = try {
            activeNotifications?.mapNotNull { it.toSocialNotice() }.orEmpty()
        } catch (_: SecurityException) {
            emptyList()
        }
        mainHandler.post { VisibleNotices.replace(snapshot) }
    }

    companion object {
        @Volatile private var connected: SocialNotificationListener? = null
        fun resyncIfConnected() { connected?.resync() }
    }
}
