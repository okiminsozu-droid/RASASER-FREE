package com.rasaser.free.icerikureticisi

import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val cream = Color(0xFFF7F1E7)
private val cardWhite = Color(0xFFFFFDF8)
private val darkWhite = Color(0xFFE8E3DA)
private val ink = Color(0xFF262521)
private val slate = Color(0xFF736E66)
private val alertRed = Color(0xFFAE4C44)
private val shape = RoundedCornerShape(20.dp)

@Composable
fun CreatorHubApp() {
    val context = LocalContext.current
    val database = remember { CreatorDatabase(context.applicationContext) }
    var creators by remember { mutableStateOf(database.creators()) }
    var creatorId by rememberSaveable { mutableStateOf(-1L) }
    var accounts by remember { mutableStateOf(emptyList<SocialAccount>()) }
    var addCreator by remember { mutableStateOf(false) }
    var addAccountFor by remember { mutableStateOf<String?>(null) }
    var deleteCreator by remember { mutableStateOf<Creator?>(null) }
    var deleteAccount by remember { mutableStateOf<SocialAccount?>(null) }
    var filterPlatform by remember { mutableStateOf<String?>(null) }
    var noticePackage by remember { mutableStateOf<String?>(null) }
    var showAccessDisclosure by remember { mutableStateOf(false) }
    var showLicenses by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf("") }
    var accessGranted by remember { mutableStateOf(notificationPermissionGranted(context)) }

    fun reload() {
        creators = database.creators()
        accounts = if (creatorId >= 0) database.accounts(creatorId) else emptyList()
        VisibleNotices.configure(context)
    }

    DisposableEffect(context) {
        val activity = context as? ComponentActivity
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                accessGranted = notificationPermissionGranted(context)
                VisibleNotices.configure(context)
            }
        }
        activity?.lifecycle?.addObserver(observer)
        VisibleNotices.configure(context)
        onDispose { activity?.lifecycle?.removeObserver(observer) }
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = ink, onPrimary = cardWhite, background = cream,
            onBackground = ink, surface = cardWhite, onSurface = ink
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().background(cream).padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(24.dp))
            if (creatorId < 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("RaSaSeR", color = slate, style = MaterialTheme.typography.labelLarge)
                        Text("İçerik Üreticisi", fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.headlineMedium)
                    }
                    Button(onClick = { addCreator = true }, contentPadding = PaddingValues(12.dp)) {
                        Text("＋", style = MaterialTheme.typography.headlineSmall)
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text("İçerik yönetim alanlarım", color = slate)
                Spacer(Modifier.height(12.dp))
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 30.dp)
                ) {
                    if (creators.isEmpty()) item {
                        LightCard {
                            Column(Modifier.padding(22.dp)) {
                                Text("İlk içerik sayfanı oluştur",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(8.dp))
                                Text("＋ düğmesine bas, sayfana isim ver ve sosyal medya hesaplarını ekle.")
                                Spacer(Modifier.height(12.dp))
                                MainButton("＋ İçerik ekle") { addCreator = true }
                            }
                        }
                    }
                    itemsIndexed(creators, key = { _, c -> c.id }) { index, creator ->
                        LightCard {
                            Row(Modifier.fillMaxWidth().clickable {
                                creatorId = creator.id
                                filterPlatform = null
                                reload()
                            }.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(48.dp).background(darkWhite, RoundedCornerShape(15.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("${index + 1}", fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleLarge)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(creator.name, fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text("İçerik sayfasını aç", color = slate,
                                        style = MaterialTheme.typography.bodySmall)
                                }
                                Text("›", style = MaterialTheme.typography.headlineMedium)
                            }
                        }
                    }
                    item {
                        TextButton(onClick = { showLicenses = true }) {
                            Text("Gizlilik ve lisans bilgileri", color = slate)
                        }
                    }
                }
            } else {
                val current = creators.firstOrNull { it.id == creatorId }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { creatorId = -1; filterPlatform = null; reload() }) {
                        Text("‹ Geri")
                    }
                    Column(Modifier.weight(1f)) {
                        Text("İçerik Üreticisi", color = slate,
                            style = MaterialTheme.typography.labelMedium)
                        Text(current?.name.orEmpty(), fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    TextButton(onClick = { deleteCreator = current }) { Text("Sil", color = alertRed) }
                }
                Spacer(Modifier.height(14.dp))
                Text("Sosyal medya platformları", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(15.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(SocialCatalog.presets, key = { it.name }) { platform ->
                        val count = VisibleNotices.active.count { it.packageName == platform.packageName }
                        PlatformTile(platform, count, filterPlatform == platform.name,
                            onSelect = {
                                filterPlatform = if (filterPlatform == platform.name) null else platform.name
                            },
                            onNotices = { noticePackage = platform.packageName })
                    }
                }
                Spacer(Modifier.height(10.dp))
                MainButton("＋ Sosyal medya hesabı ekle") { addAccountFor = "Instagram" }
                Spacer(Modifier.height(10.dp))
                LightCard {
                    Column(Modifier.padding(14.dp)) {
                        Text(if (accessGranted) "Bildirim erişimi açık" else "Bildirim erişimini etkinleştir",
                            fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "Bildirimler telefondaki uygulamalardan alınır. Hesap bazında kesin ayrım yapılamaz.",
                            color = slate, style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { showAccessDisclosure = true }) {
                            Text(if (accessGranted) "Bildirim ayarları" else "Bildirim izni ver")
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (filterPlatform == null) "Bağlantılarım" else filterPlatform.orEmpty(),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f))
                    if (filterPlatform != null) TextButton(onClick = { filterPlatform = null }) {
                        Text("Tümünü göster")
                    }
                }
                val shownAccounts = accounts.filter {
                    filterPlatform == null || it.platform == filterPlatform
                }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 28.dp)
                ) {
                    if (shownAccounts.isEmpty()) item {
                        Text("Bu bölümde henüz sosyal medya hesabı yok.",
                            color = slate, modifier = Modifier.padding(vertical = 18.dp))
                    }
                    items(shownAccounts, key = { it.id }) { account ->
                        val unread = VisibleNotices.active.count {
                            it.packageName == account.packageName && account.packageName.isNotBlank()
                        }
                        LightCard {
                            Row(Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                SocialAppIcon(account.packageName, account.platform, Modifier.size(44.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(account.label, fontWeight = FontWeight.Bold,
                                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text(account.platform, color = slate,
                                        style = MaterialTheme.typography.bodySmall)
                                    if (unread > 0) Text(
                                        "$unread etkin bildirim",
                                        color = alertRed,
                                        modifier = Modifier.clickable {
                                            noticePackage = account.packageName
                                        },
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    TextButton(onClick = {
                                        if (!openLinkedAccount(context, account)) {
                                            feedback = "Uygulama veya bağlantı açılamadı."
                                        }
                                    }) { Text("Aç ›") }
                                    TextButton(onClick = { deleteAccount = account }) {
                                        Text("Kaldır", color = alertRed)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (addCreator) NewCreatorDialog(onDismiss = { addCreator = false }) { name ->
            try {
                val id = database.addCreator(name)
                reload()
                creatorId = id
                reload()
                addCreator = false
                null
            } catch (e: Exception) { e.message ?: "İçerik eklenemedi." }
        }

        addAccountFor?.let { initial ->
            NewAccountDialog(initial, onDismiss = { addAccountFor = null }) {
                    platform, label, url, packageName ->
                try {
                    database.addAccount(creatorId, platform, label, url, packageName)
                    reload()
                    filterPlatform = null
                    addAccountFor = null
                    null
                } catch (e: Exception) { e.message ?: "Hesap eklenemedi." }
            }
        }

        deleteCreator?.let { creator ->
            ConfirmDelete("İçerik sayfasını sil", "${creator.name} ve tüm hesap bağlantıları silinsin mi?",
                onCancel = { deleteCreator = null },
                onDelete = {
                    database.deleteCreator(creator.id)
                    creatorId = -1
                    deleteCreator = null
                    reload()
                })
        }
        deleteAccount?.let { account ->
            ConfirmDelete("Hesabı kaldır", "${account.label} bağlantısı kaldırılsın mı?",
                onCancel = { deleteAccount = null },
                onDelete = {
                    database.deleteAccount(account.id)
                    deleteAccount = null
                    reload()
                })
        }
        noticePackage?.let { pkg ->
            val matches = VisibleNotices.active.filter { it.packageName == pkg }
            AlertDialog(
                onDismissRequest = { noticePackage = null },
                title = { Text("Etkin bildirimler (${matches.size})") },
                text = {
                    Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                        Text("Bu bildirimler uygulama düzeyindedir; hangi sosyal hesaba ait olduğu kesin değildir.",
                            color = slate, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        if (matches.isEmpty()) Text("Şu anda etkin bildirim bulunmuyor.")
                        matches.forEach { notice ->
                            HorizontalDivider()
                            Text(notice.title.ifBlank { "Bildirim" }, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp))
                            Text(notice.message, style = MaterialTheme.typography.bodySmall)
                            Text(SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                                .format(Date(notice.postedAt)), color = slate,
                                style = MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { noticePackage = null }) { Text("Kapat") }
                }
            )
        }
        if (showAccessDisclosure) AlertDialog(
            onDismissRequest = { showAccessDisclosure = false },
            title = { Text("Bildirim erişimi hakkında") },
            text = {
                Text(
                    "Android bu izinle diğer uygulamaların bildirimlerini de görmeye teknik olarak izin verir. " +
                        "İçerik Üreticisi yalnız eklediğiniz uygulamaların etkin bildirimlerini bellekte gösterir. " +
                        "Bildirim metinleri kaydedilmez veya sunucuya gönderilmez. " +
                        "İzni telefon ayarlarından istediğiniz zaman kapatabilirsiniz."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showAccessDisclosure = false
                    try {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    } catch (_: ActivityNotFoundException) {
                        feedback = "Bu cihazda bildirim erişim ayarı açılamadı."
                    }
                }) { Text("Android ayarlarına git") }
            },
            dismissButton = {
                TextButton(onClick = { showAccessDisclosure = false }) { Text("Vazgeç") }
            }
        )
        if (showLicenses) AlertDialog(
            onDismissRequest = { showLicenses = false },
            title = { Text("Gizlilik ve lisanslar") },
            text = {
                Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                    Text("Şifre, erişim anahtarı ve giriş oturumu saklanmaz. " +
                        "İçerik adları, hesap etiketleri ve profil bağlantıları yalnız cihazdaki SQLite veritabanında tutulur. " +
                        "Bulut eşitlemesi ve internet izni yoktur. Uygulama yedeği devre dışıdır.")
                    Spacer(Modifier.height(12.dp))
                    Text(remember {
                        context.assets.open("legal/licenses.txt").bufferedReader().use { it.readText() }
                    })
                }
            },
            confirmButton = { TextButton(onClick = { showLicenses = false }) { Text("Kapat") } }
        )
        if (feedback.isNotBlank()) AlertDialog(
            onDismissRequest = { feedback = "" },
            title = { Text("Bilgi") },
            text = { Text(feedback) },
            confirmButton = { TextButton(onClick = { feedback = "" }) { Text("Tamam") } }
        )
    }
}

@Composable
private fun LightCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardWhite),
        shape = shape,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) { content() }
}

@Composable
private fun MainButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick, modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = darkWhite, contentColor = ink),
        shape = RoundedCornerShape(15.dp)
    ) { Text(text, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(6.dp)) }
}

@Composable
private fun PlatformTile(
    platform: Platform, count: Int, selected: Boolean,
    onSelect: () -> Unit, onNotices: () -> Unit
) {
    Column(
        modifier = Modifier.width(82.dp).clip(RoundedCornerShape(16.dp))
            .background(if (selected) darkWhite else cardWhite)
            .clickable(onClick = onSelect).padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            SocialAppIcon(platform.packageName, platform.name, Modifier.size(45.dp))
            if (count > 0) Box(
                Modifier.align(Alignment.TopEnd).offset(x = 10.dp, y = (-7).dp)
                    .clip(CircleShape).background(alertRed).clickable(onClick = onNotices)
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(count.toString(), color = Color.White,
                    style = MaterialTheme.typography.labelSmall)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(platform.name, style = MaterialTheme.typography.labelSmall,
            maxLines = 1)
    }
}

@Composable
private fun SocialAppIcon(packageName: String, platform: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val image = remember(packageName) {
        if (packageName.isBlank()) null else try {
            val icon = context.packageManager.getApplicationIcon(packageName)
            val bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
            icon.setBounds(0, 0, 96, 96)
            icon.draw(Canvas(bitmap))
            bitmap.asImageBitmap()
        } catch (_: Exception) { null }
    }
    if (image != null) Image(
        bitmap = image, contentDescription = platform,
        modifier = modifier.clip(RoundedCornerShape(12.dp))
    ) else Box(
        modifier = modifier.background(
            when (platform) {
                "Instagram" -> Color(0xFFB73D83)
                "Facebook" -> Color(0xFF2465B3)
                "YouTube" -> Color(0xFFE5362D)
                "TikTok" -> Color(0xFF1C1C20)
                else -> Color(0xFF605D57)
            }, RoundedCornerShape(12.dp)
        ), contentAlignment = Alignment.Center
    ) {
        Text(when (platform) {
            "Instagram" -> "◎"
            "Facebook" -> "f"
            "YouTube" -> "▶"
            "TikTok" -> "♪"
            else -> platform.firstOrNull()?.uppercase() ?: "+"
        }, color = Color.White, fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun NewCreatorDialog(onDismiss: () -> Unit, onCreate: (String) -> String?) {
    var name by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yeni içerik üreticisi") },
        text = {
            Column {
                Text("İçerik ismini yazın. İstediğiniz kadar ayrı sayfa oluşturabilirsiniz.")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(name, onValueChange = { name = it.take(80) },
                    label = { Text("İçerik adı") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                if (error.isNotBlank()) Text(error, color = alertRed)
            }
        },
        confirmButton = {
            TextButton(onClick = { error = onCreate(name).orEmpty() }) { Text("Oluştur") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } }
    )
}

@Composable
private fun NewAccountDialog(
    initial: String, onDismiss: () -> Unit,
    onAdd: (String, String, String, String) -> String?
) {
    var selected by remember { mutableStateOf(initial) }
    var customPlatform by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var customPackage by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val isCustom = selected == "Diğer"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sosyal medya hesabı ekle") },
        text = {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState())) {
                Text("Platform", fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(SocialCatalog.presets.map { it.name } + "Diğer") { option ->
                        FilterChip(selected == option, onClick = { selected = option },
                            label = { Text(option) })
                    }
                }
                if (isCustom) {
                    OutlinedTextField(customPlatform, { customPlatform = it.take(50) },
                        label = { Text("Sosyal medya adı") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(customPackage, { customPackage = it.trim().take(150) },
                        label = { Text("Android paket adı (isteğe bağlı)") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
                OutlinedTextField(label, { label = it.take(80) },
                    label = { Text("Hesap / profil ismi") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(url, { url = it.take(2048) },
                    label = { Text("Profil bağlantısı (https://...)") },
                    modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text("Giriş bu uygulamada yapılmaz. Aç düğmesi, varsa telefondaki resmi uygulamayı açar. Şifre istemeyiz.",
                    color = slate, style = MaterialTheme.typography.bodySmall)
                if (error.isNotBlank()) Text(error, color = alertRed)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val platform = if (isCustom) customPlatform.trim() else selected
                val packageName = if (isCustom) customPackage.trim() else
                    SocialCatalog.find(platform)?.packageName.orEmpty()
                error = onAdd(platform, label, url, packageName).orEmpty()
            }) { Text("Hesabı ekle") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } }
    )
}

@Composable
private fun ConfirmDelete(title: String, body: String, onCancel: () -> Unit, onDelete: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { TextButton(onClick = onDelete) { Text("Sil", color = alertRed) } },
        dismissButton = { TextButton(onClick = onCancel) { Text("Vazgeç") } }
    )
}

private fun notificationPermissionGranted(context: Context): Boolean {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val component = ComponentName(context, SocialNotificationListener::class.java)
    return if (Build.VERSION.SDK_INT >= 27) {
        manager.isNotificationListenerAccessGranted(component)
    } else {
        Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            ?.contains(component.flattenToString()) == true
    }
}

/** Open the installed official app using its profile deep link; fall back to browser. */
private fun openLinkedAccount(context: Context, account: SocialAccount): Boolean {
    if (account.profileUrl.isNotBlank()) {
        val uri = Uri.parse(account.profileUrl)
        if (!uri.scheme.equals("https", true)) return false
        if (account.packageName.isNotBlank()) {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                    setPackage(account.packageName)
                })
                return true
            } catch (_: Exception) { /* Use a trusted HTTPS browser fallback. */ }
        }
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
            })
            return true
        } catch (_: Exception) { return false }
    }
    if (account.packageName.isBlank()) return false
    try {
        context.packageManager.getLaunchIntentForPackage(account.packageName)?.let {
            context.startActivity(it)
            return true
        }
        if (Build.VERSION.SDK_INT >= 33) {
            context.packageManager.getLaunchIntentSenderForPackage(account.packageName)
                .sendIntent(context, 0, null, null, null)
            return true
        }
    } catch (_: Exception) { /* App is missing or opening is blocked. */ }
    return false
}
