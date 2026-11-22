package com.rasaser.free.icerikureticisi

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ClipData
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.content.ClipboardManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Checkbox
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val panelBg = Color(0xFFFFFDF8)
private val actionBg = Color(0xFFE8E3DA)
private val textInk = Color(0xFF262521)
private val mutedText = Color(0xFF736E66)
private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR"))

/** No stored passwords or APIs: share via the official app, then user confirms publish there. */
object SocialIntents {
    fun openAccount(context: Context, account: SocialAccount): Boolean {
        val pkg = account.packageName
        val url = account.profileUrl.takeIf { it.startsWith("https://", true) }
        if (pkg.isNotBlank()) {
            if (url != null) {
                val deepLink = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                    setPackage(pkg)
                }
                // Explicit component bypasses Android's app/clone chooser.
                try {
                    val match = context.packageManager.queryIntentActivities(deepLink, 0)
                        .firstOrNull { it.activityInfo.packageName == pkg && it.activityInfo.exported }
                    if (match != null) {
                        deepLink.component = ComponentName(pkg, match.activityInfo.name)
                        context.startActivity(deepLink)
                        return true
                    }
                } catch (_: Exception) { /* Launch official app home instead. */ }
            }
            try {
                val launch = context.packageManager.getLaunchIntentForPackage(pkg)
                if (launch != null) {
                    context.startActivity(launch)
                    return true
                }
                if (Build.VERSION.SDK_INT >= 33) {
                    context.packageManager.getLaunchIntentSenderForPackage(pkg)
                        .sendIntent(context, 0, null, null, null)
                    return true
                }
            } catch (_: Exception) { /* App may not be installed. */ }
        }
        // For unknown / missing apps, open a valid HTTPS address in the browser.
        if (url == null) return false
        return try {
            val browser = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addCategory(Intent.CATEGORY_BROWSABLE)
            context.startActivity(browser)
            true
        } catch (_: Exception) { false }
    }

    fun handoff(
        context: Context, account: SocialAccount, media: Uri,
        mime: String, description: String
    ): Boolean {
        if (media.scheme != "content" || !(mime.startsWith("image/") || mime.startsWith("video/")))
            return false
        val base = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, media)
            if (description.isNotBlank()) putExtra(Intent.EXTRA_TEXT, description)
            clipData = ClipData.newUri(context.contentResolver, "Paylaşılacak medya", media)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val pkg = account.packageName
        return try {
            if (pkg.isNotBlank()) {
                val share = Intent(base).setPackage(pkg)
                val matching = context.packageManager.queryIntentActivities(share, 0)
                    .firstOrNull {
                        it.activityInfo.packageName == pkg && it.activityInfo.exported
                    } ?: return false
                share.component = ComponentName(matching.activityInfo.packageName,
                    matching.activityInfo.name)
                if (description.isNotBlank()) {
                    // Some social apps ignore EXTRA_TEXT when receiving a media URI.
                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                        .setPrimaryClip(ClipData.newPlainText("Paylaşım açıklaması", description))
                }
                context.startActivity(share)
            } else {
                context.startActivity(Intent.createChooser(base, "Paylaşım uygulamasını seçin"))
            }
            true
        } catch (_: Exception) { false }
    }
}

@Composable
fun ShareComposer(
    creatorId: Long,
    accounts: List<SocialAccount>,
    selectedIds: Set<Long>,
    onSelectedIds: (Set<Long>) -> Unit,
    database: CreatorDatabase,
    onMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var mediaUri by rememberSaveable(creatorId) { mutableStateOf("") }
    var mediaMime by rememberSaveable(creatorId) { mutableStateOf("") }
    var caption by rememberSaveable(creatorId) { mutableStateOf("") }
    var sendQueue by remember(creatorId) { mutableStateOf<List<Long>>(emptyList()) }
    var nextPosition by remember(creatorId) { mutableIntStateOf(0) }
    var refreshSchedules by remember(creatorId) { mutableIntStateOf(0) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val type = context.contentResolver.getType(uri).orEmpty()
            if (type.startsWith("image/") || type.startsWith("video/")) {
                // A persistable read grant keeps a scheduled draft usable after app restart.
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    mediaUri = uri.toString()
                    mediaMime = type
                } catch (_: SecurityException) {
                    onMessage("Bu sağlayıcı kalıcı dosya erişimi vermedi. Başka bir fotoğraf/video seçin.")
                }
            } else onMessage("Yalnızca fotoğraf veya video seçilebilir.")
        }
    }
    val permission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        onMessage(if (granted) "Hatırlatma izni açıldı. Zaman ayarla tuşuna yeniden dokunun."
            else "Bildirim izni verilmedi; zamanlama hatırlatması kurulamadı.")
    }
    val available = accounts.filter { selectedIds.contains(it.id) }
    val schedules = remember(creatorId, refreshSchedules) { database.scheduledFor(creatorId) }

    fun sendTo(account: SocialAccount) {
        val result = SocialIntents.handoff(
            context, account, Uri.parse(mediaUri), mediaMime, caption
        )
        onMessage(
            if (result) "Paylaşım ekranı açıldı: ${account.platform} / ${account.label}. " +
                "Yayını ilgili uygulamada onaylayın; hesap otomatik değiştirilemez."
            else "${account.platform} uygulaması bu medya paylaşımını kabul etmedi veya kurulu değil."
        )
    }

    fun plan() {
        if (mediaUri.isBlank() || available.isEmpty()) {
            onMessage("Önce fotoğraf/video ve en az bir hesap seçin.")
            return
        }
        if (!ShareReminders.canNotify(context)) {
            if (Build.VERSION.SDK_INT >= 33) {
                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else onMessage("Planlanan paylaşım için Android bildirimlerini açın.")
            return
        }
        val localNow = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, day ->
                TimePickerDialog(context, { _, hour, minute ->
                    val planned = Calendar.getInstance().apply {
                        set(year, month, day, hour, minute, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    if (planned <= System.currentTimeMillis() + 30_000L) {
                        onMessage("Gelecek bir tarih ve saat seçin.")
                    } else try {
                        val id = database.addScheduledShare(
                            creatorId, mediaUri, mediaMime, caption,
                            available.map { it.id }, planned
                        )
                        ShareReminders.enqueue(context, id, planned)
                        refreshSchedules++
                        onMessage("Hatırlatma planlandı: ${dateFormat.format(Date(planned))}. " +
                            "Otomatik yayın değil; bildirimden açıp gönderimi onaylayın.")
                    } catch (e: Exception) {
                        onMessage(e.message ?: "Zamanlama kaydedilemedi.")
                    }
                }, localNow.get(Calendar.HOUR_OF_DAY), localNow.get(Calendar.MINUTE), true).show()
            },
            localNow.get(Calendar.YEAR), localNow.get(Calendar.MONTH),
            localNow.get(Calendar.DAY_OF_MONTH)
        ).apply { datePicker.minDate = System.currentTimeMillis() - 1000L }.show()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(tr("Paylaşım oluştur"), style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { picker.launch(arrayOf("image/*")) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp)
                ) { Text(tr("▧ Resim yükle")) }
                Button(
                    onClick = { picker.launch(arrayOf("video/*")) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp)
                ) { Text(tr("▶ Video yükle")) }
            }
            if (mediaUri.isNotBlank()) {
                Text(
                    if (mediaMime.startsWith("video/")) "✓ Video seçildi" else "✓ Resim seçildi",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it.take(2500) },
                label = { Text(tr("Açıklama ekle")) },
                placeholder = { Text(tr("Paylaşım metni ve etiketler...")) },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Checkbox(
                    checked = accounts.isNotEmpty() && accounts.all { it.id in selectedIds },
                    enabled = accounts.isNotEmpty(),
                    onCheckedChange = { all ->
                        onSelectedIds(if (all) accounts.map { it.id }.toSet() else emptySet())
                    }
                )
                Text(tr("Hepsini seç"), color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        if (mediaUri.isBlank() || available.isEmpty()) {
                            onMessage("Gönderim için medya ve hesap seçin.")
                        } else {
                            sendQueue = available.map { it.id }
                            nextPosition = 1
                            sendTo(available.first())
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) { Text(tr("➤ Gönder"), style = MaterialTheme.typography.labelMedium) }
                OutlinedButton(
                    onClick = { plan() },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) { Text(tr("◷ Zaman"), style = MaterialTheme.typography.labelMedium) }
            }
            Text("${available.size} hesap seçildi",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (nextPosition < sendQueue.size && mediaUri.isNotBlank()) {
                val next = accounts.firstOrNull { it.id == sendQueue[nextPosition] }
                if (next != null) {
                    TextButton(onClick = {
                        nextPosition += 1
                        sendTo(next)
                    }) {
                        Text("➜ Sıradaki hesaba aktar (${nextPosition + 1}/${sendQueue.size})")
                    }
                }
            }
            Text(
                "Seçilen hesaplarda paylaşım ekranı açılır. Gönderi sizin onayınızla yayınlanır; " +
                    "hesap değişimi ve açıklamanın eklenmesi platforma bağlıdır.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall
            )
        }
    }

    if (schedules.isNotEmpty()) {
        Spacer(Modifier.height(10.dp))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text(tr("Zamanlanmış taslaklar"), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                schedules.take(12).forEach { draft ->
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(dateFormat.format(Date(draft.scheduledAt)),
                                style = MaterialTheme.typography.bodySmall)
                            Text(if (draft.ready) "Hatırlatma zamanı geldi" else "Hatırlatma bekleniyor",
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = {
                            mediaUri = draft.mediaUri
                            mediaMime = draft.mediaMime
                            caption = draft.caption
                            onSelectedIds(draft.targetIds.toSet())
                            sendQueue = emptyList()
                            nextPosition = 0
                            onMessage("Taslak hazırlandı. Göndermek için hesapları kontrol edin.")
                        }) { Text(tr("Hazırla")) }
                        TextButton(onClick = {
                            ShareReminders.cancel(context, draft.id)
                            database.deleteScheduledShare(draft.id)
                            refreshSchedules++
                        }) { Text(tr("Sil"), color = Color(0xFFAE4C44)) }
                    }
                }
            }
        }
    }
}
