package com.rasaser.free.icerikureticisi

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * First independent LIVE publication connector: Telegram Bot API channels.
 * Other seven-platform entries remain local bookmarks until their approved
 * provider OAuth connectors are actually installed and verified.
 */
@Composable
fun IndependentConnectionsPanel(creatorId: Long) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var channels by remember(creatorId) {
        mutableStateOf(runCatching { TelegramSecureStore.channels(context, creatorId) }
            .getOrElse { emptyList() })
    }
    var showAdd by remember(creatorId) { mutableStateOf(false) }
    var deleteChannel by remember(creatorId) { mutableStateOf<TelegramChannel?>(null) }
    var selected by remember(creatorId) { mutableStateOf<Set<String>>(emptySet()) }
    var caption by remember(creatorId) { mutableStateOf("") }
    var photo by remember(creatorId) { mutableStateOf<Uri?>(null) }
    var sending by remember(creatorId) { mutableStateOf(false) }
    var response by remember(creatorId) { mutableStateOf<List<String>>(emptyList()) }
    var expanded by remember(creatorId) { mutableStateOf(false) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        photo = it
    }

    fun refresh() {
        channels = TelegramSecureStore.channels(context, creatorId)
        selected = selected.intersect(channels.map { it.id }.toSet())
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔗 " + tr("Bağımsız hesap bağlantıları"), fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) tr("Gizle") else tr("Aç"))
                }
            }
            Text(
                tr("Resmî bağlantılar ayrı yönetilir. Telegram kanallarına gerçek yayın onayı alınabilir."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (expanded) {
                Text("Telegram Bot API • " + tr("Bağlı kanal") + ": " + channels.size,
                    fontWeight = FontWeight.SemiBold)
                OutlinedButton(onClick = { showAdd = true }) {
                    Text("＋ " + tr("Telegram kanalı bağla"))
                }
                if (channels.isEmpty()) {
                    Text(tr("Henüz doğrulanmış Telegram kanalı yok."),
                        style = MaterialTheme.typography.bodySmall)
                }
                channels.forEach { channel ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selected.contains(channel.id),
                            onCheckedChange = { checked ->
                                selected = if (checked) selected + channel.id else selected - channel.id
                            }
                        )
                        Column(Modifier.weight(1f)) {
                            Text(channel.channelTitle, fontWeight = FontWeight.SemiBold,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("@" + channel.botUsername + " • " + tr("Yayın yetkisi doğrulandı"),
                                style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(onClick = { deleteChannel = channel }) {
                            Text(tr("Kaldır"))
                        }
                    }
                }
                if (channels.isNotEmpty()) {
                    Text(
                        tr("Gönderilecek Telegram kanallarını seçin. Yayın, gönder tuşuna basınca başlar."),
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = caption,
                        onValueChange = { caption = it.take(4096) },
                        label = { Text(tr("Gönderi metni")) },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { photoPicker.launch("image/*") }, enabled = !sending) {
                            Text(if (photo == null) tr("Fotoğraf seç") else tr("Fotoğraf seçildi"))
                        }
                        if (photo != null) TextButton(onClick = { photo = null }, enabled = !sending) {
                            Text(tr("Temizle"))
                        }
                    }
                    val hasContent = caption.isNotBlank() || photo != null
                    Button(
                        onClick = {
                            sending = true
                            response = emptyList()
                            val targets = channels.filter { it.id in selected }
                            val message = caption
                            val image = photo
                            scope.launch {
                                val results = withContext(Dispatchers.IO) {
                                    targets.map { channel ->
                                        try {
                                            val stored = TelegramSecureStore.credential(
                                                context, channel.id, creatorId
                                            ) ?: error("Şifreli kanal kaydı bulunamadı.")
                                            val messageId = if (image != null) {
                                                TelegramBotApi.publishPhoto(
                                                    context, stored.token, stored.channel.channelId,
                                                    image, message
                                                )
                                            } else {
                                                TelegramBotApi.publishText(
                                                    stored.token, stored.channel.channelId, message
                                                )
                                            }
                                            "${channel.channelTitle}: " +
                                                "✓ Telegram tarafından yayınlandı (#$messageId)"
                                        } catch (e: Exception) {
                                            "${channel.channelTitle}: " +
                                                "✕ " + TelegramBotApi.safeMessage(e)
                                        }
                                    }
                                }
                                response = results
                                sending = false
                            }
                        },
                        enabled = !sending && selected.isNotEmpty() && hasContent &&
                            (photo == null || caption.length <= 1024),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (sending) CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        ) else Text(tr("Seçili kanallarda yayınla") + " (" + selected.size + ")")
                    }
                    response.forEach {
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text(
                    tr("Bluesky, Instagram, Facebook, YouTube, TikTok ve Pinterest: resmî OAuth izinleri hazırlanıyor; bu platformlarda otomatik yayın henüz etkin değil."),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    if (showAdd) TelegramAddChannelDialog(
        onDismiss = { showAdd = false },
        onVerified = { result, token ->
            TelegramSecureStore.add(
                context, creatorId, result.channelId, result.channelTitle,
                result.botUsername, System.currentTimeMillis(), token
            )
            refresh()
            selected = channels.map { it.id }.toSet()
            showAdd = false
        }
    )
    deleteChannel?.let { channel ->
        AlertDialog(
            onDismissRequest = { deleteChannel = null },
            title = { Text(tr("Telegram bağlantısını kaldır")) },
            text = { Text(channel.channelTitle + " • " +
                tr("Kanalın bot anahtarı bu cihazdan silinecek.")) },
            confirmButton = {
                TextButton(onClick = {
                    TelegramSecureStore.remove(context, creatorId, channel.id)
                    refresh()
                    deleteChannel = null
                }) { Text(tr("Kaldır")) }
            },
            dismissButton = {
                TextButton(onClick = { deleteChannel = null }) { Text(tr("Vazgeç")) }
            }
        )
    }
}

@Composable
private fun TelegramAddChannelDialog(
    onDismiss: () -> Unit,
    onVerified: (TelegramVerification, String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var token by remember { mutableStateOf("") }
    var chatId by remember { mutableStateOf("") }
    var checking by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!checking) onDismiss() },
        title = { Text(tr("Telegram kanalı bağla")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    tr("BotFather'dan bot oluşturun, botu Telegram kanalınıza yayın yetkili yönetici olarak ekleyin."),
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it.trim().take(256); message = "" },
                    label = { Text(tr("BotFather bot tokenı")) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = chatId,
                    onValueChange = { chatId = it.trim().take(100); message = "" },
                    label = { Text(tr("Kanal: @kanaladi veya -100...")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    tr("Bot tokenı yalnız telefonun Android Keystore şifreli kasasında saklanır. RaSaSeR sunucusuna gönderilmez."),
                    style = MaterialTheme.typography.bodySmall
                )
                if (message.isNotBlank()) Text(
                    message, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    checking = true
                    scope.launch {
                        try {
                            val result = TelegramBotApi.verify(token, chatId)
                            onVerified(result, token)
                            token = ""
                        } catch (e: Exception) {
                            // Never reflect the secret token in user-visible errors.
                            message = TelegramBotApi.safeMessage(e)
                        } finally {
                            checking = false
                        }
                    }
                },
                enabled = !checking && TelegramBotApi.validBotToken(token) &&
                    TelegramBotApi.validChannelId(chatId)
            ) {
                if (checking) CircularProgressIndicator(
                    modifier = Modifier.size(18.dp), strokeWidth = 2.dp
                ) else Text(tr("Doğrula ve kaydet"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !checking) {
                Text(tr("Vazgeç"))
            }
        }
    )
}
