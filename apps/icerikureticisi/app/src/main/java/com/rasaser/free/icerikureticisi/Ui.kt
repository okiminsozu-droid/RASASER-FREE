package com.rasaser.free.icerikureticisi

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

import androidx.compose.runtime.Composable
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

private val cream = Color(0xFFF7F1E7)
private val cardWhite = Color(0xFFFFFDF8)
private val darkWhite = Color(0xFFE8E3DA)
private val ink = Color(0xFF262521)
private val slate = Color(0xFF736E66)
private val alertRed = Color(0xFFAE4C44)
private val shape = RoundedCornerShape(20.dp)

@Composable
fun CreatorHubApp(initialCreatorId: Long = -1L) {
    val context = LocalContext.current
    val database = remember { CreatorDatabase(context.applicationContext) }
    var creators by remember { mutableStateOf(database.creators()) }
    var creatorId by rememberSaveable(initialCreatorId) { mutableStateOf(initialCreatorId) }
    var selectedAccountIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var accounts by remember { mutableStateOf(emptyList<SocialAccount>()) }
    var addCreator by remember { mutableStateOf(false) }
    var addAccountFor by remember { mutableStateOf<String?>(null) }
    var deleteCreator by remember { mutableStateOf<Creator?>(null) }
    var deleteAccount by remember { mutableStateOf<SocialAccount?>(null) }
    var filterPlatform by remember { mutableStateOf<String?>(null) }
    var showLicenses by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf("") }
    var language by remember { mutableStateOf(AppLanguage.get(context)) }
    var darkMode by remember { mutableStateOf(CreatorAppearance.dark(context)) }
    var showPremium by remember { mutableStateOf(false) }
    var accessRevision by remember { mutableStateOf(0) }
    val trialStatus = remember(accessRevision) { TrialAccess.status(context) }
    val ownerPremium = remember(accessRevision) { OwnerPremium.active(context) }
    DisposableEffect(context) {
        val lifecycle = (context as? FragmentActivity)?.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) accessRevision++
        }
        lifecycle?.addObserver(observer)
        onDispose { lifecycle?.removeObserver(observer) }
    }
    if (!ownerPremium && trialStatus.expired) {
        TrialExpiredScreen(onActivated = { accessRevision++ })
        return
    }
    var showLockSettings by remember { mutableStateOf(false) }
    var nameTranslationsFor by remember { mutableStateOf<Creator?>(null) }
    var profileImageFor by remember { mutableStateOf(-1L) }
    fun reload() {
        creators = database.creators()
        accounts = if (creatorId >= 0) database.accounts(creatorId) else emptyList()
    }

    val profileImagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null && profileImageFor >= 0L) {
            var savedFile = ""
            try {
                savedFile = LocalLogos.importImage(context, uri)
                database.setCreatorLogo(profileImageFor, savedFile)
                reload()
            } catch (e: Exception) {
                if (savedFile.isNotBlank()) LocalLogos.delete(context, savedFile)
                feedback = e.message ?: "Profil görseli kaydedilemedi."
            }
        }
    }
    val prefs = remember { context.getSharedPreferences("share_reminders", Context.MODE_PRIVATE) }
    val notificationRequester = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        feedback = if (granted) "Paylaşım hatırlatmaları etkinleştirildi."
            else "Bildirim izni verilmedi. Zamanlama öncesinde yeniden isteyebilirsiniz."
    }
    var firstLaunchPrompt by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT >= 33 &&
                !prefs.getBoolean("reminder_permission_explained", false) &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED
        )
    }

    LaunchedEffect(initialCreatorId) {
        if (initialCreatorId >= 0L && creators.any { it.id == initialCreatorId }) {
            creatorId = initialCreatorId
            reload()
            selectedAccountIds = accounts.map { it.id }.toSet()
        }
    }

    CompositionLocalProvider(LocalCreatorLanguage provides language) {
    MaterialTheme(
        colorScheme = if (darkMode) darkColorScheme(
            primary = Color(0xFFF0CE8E),
            onPrimary = Color(0xFF1E1E21),
            background = Color(0xFF17181B),
            onBackground = Color(0xFFF8F2E9),
            surface = Color(0xFF292A30),
            onSurface = Color(0xFFF8F2E9),
            surfaceVariant = Color(0xFF36373D),
            onSurfaceVariant = Color(0xFFC9C6C1)
        ) else lightColorScheme(
            primary = ink, onPrimary = cardWhite, background = cream,
            onBackground = ink, surface = cardWhite, onSurface = ink,
            surfaceVariant = darkWhite, onSurfaceVariant = slate
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            CreatorTopControls(
                language = language,
                isDark = darkMode,
                onLanguage = { choice ->
                    language = choice
                    AppLanguage.save(context, choice)
                },
                onDark = { enabled ->
                    darkMode = enabled
                    CreatorAppearance.setDark(context, enabled)
                },
                onPremium = { showPremium = true },
                onLock = { showLockSettings = true },
                premiumActive = ownerPremium
            )
            Spacer(Modifier.height(6.dp))
            if (creatorId < 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("RaSaSeR", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
                        Text(tr("İçerik Üreticisi"), fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.headlineMedium)
                    }
                    Button(onClick = { addCreator = true }, contentPadding = PaddingValues(12.dp)) {
                        Text("＋", style = MaterialTheme.typography.headlineSmall)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    if (ownerPremium) tr("Premium etkin")
                    else tr("Ücretsiz deneme") + " • " + trialStatus.remainingDays + " " + tr("gün kaldı"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
                Spacer(Modifier.height(14.dp))
                Text(tr("İçerik yönetim alanlarım"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                MainButton(tr("＋ İçerik ekle")) { addCreator = true }
                Spacer(Modifier.height(12.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 14.dp)
                ) {
                    if (creators.isEmpty()) item {
                        LightCard {
                            Column(Modifier.padding(22.dp)) {
                                Text(tr("İlk içerik sayfanı oluştur"),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(8.dp))
                                Text(tr("＋ düğmesine bas, sayfana isim ver ve sosyal medya hesaplarını ekle."))
                                Spacer(Modifier.height(12.dp))
                                MainButton(tr("＋ İçerik ekle")) { addCreator = true }
                            }
                        }
                    }
                    itemsIndexed(creators, key = { _, c -> c.id }) { index, creator ->
                        LightCard {
                            Row(Modifier.fillMaxWidth().clickable {
                                creatorId = creator.id
                                filterPlatform = null
                                reload()
                                selectedAccountIds = accounts.map { it.id }.toSet()
                            }.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                                CreatorAvatar(context, creator.logoFile, "${index + 1}", size = 76)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(creator.displayName(language), fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text(tr("İçerik sayfasını aç"), color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodySmall)
                                }
                                Text("›", style = MaterialTheme.typography.headlineMedium)
                            }
                        }
                    }
                    item {
                        TextButton(onClick = { showLicenses = true }) {
                            Text(tr("Gizlilik ve lisans bilgileri"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                val current = creators.firstOrNull { it.id == creatorId }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = {
                        creatorId = -1L
                        selectedAccountIds = emptySet()
                        filterPlatform = null
                        reload()
                    }) { Text(tr("‹ Geri")) }
                    Column(Modifier.weight(1f)) {
                        Text(tr("İçerik Üreticisi"), color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium)
                        Text(current?.displayName(language).orEmpty(), fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge, maxLines = 2,
                            overflow = TextOverflow.Ellipsis)
                    }
                    TextButton(onClick = { deleteCreator = current }) {
                        Text(tr("Sil"), color = alertRed)
                    }
                }
                if (current != null) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CreatorAvatar(context, current.logoFile, current.name, size = 64)
                        TextButton(onClick = {
                            profileImageFor = current.id
                            profileImagePicker.launch("image/*")
                        }) { Text(tr("Profil görselini değiştir")) }
                        TextButton(onClick = { nameTranslationsFor = current }) {
                            Text("🌐")
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                val shownAccounts = accounts.filter {
                    filterPlatform == null || it.platform == filterPlatform
                }
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 28.dp)
                ) {
                    item {
                        Text(tr("Sosyal medya platformları"), fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(SocialCatalog.presets, key = { it.name }) { platform ->
                                PlatformTile(platform, filterPlatform == platform.name) {
                                    filterPlatform = if (filterPlatform == platform.name) null
                                        else platform.name
                                }
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        MainButton(tr("＋ Sosyal medya hesabı ekle")) {
                            addAccountFor = "Instagram"
                        }
                        Spacer(Modifier.height(10.dp))
                        IndependentConnectionsPanel(creatorId = creatorId)
                        Spacer(Modifier.height(10.dp))
                        ShareComposer(
                            creatorId = creatorId,
                            accounts = accounts,
                            selectedIds = selectedAccountIds,
                            onSelectedIds = { selectedAccountIds = it },
                            database = database,
                            onMessage = { feedback = it }
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (filterPlatform == null) tr("Bağlantılarım") + " • " + accounts.size
                                 else filterPlatform.orEmpty(),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f))
                            if (filterPlatform != null) TextButton(onClick = { filterPlatform = null }) {
                                Text(tr("Tümünü göster"))
                            }
                        }
                        if (shownAccounts.isEmpty()) {
                            Text(tr("Bu bölümde henüz sosyal medya hesabı yok."),
                                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 10.dp))
                        }
                    }
                    items(shownAccounts, key = { it.id }) { account ->
                        LightCard {
                            Row(Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = account.id in selectedAccountIds,
                                    onCheckedChange = { checked ->
                                        selectedAccountIds = if (checked)
                                            selectedAccountIds + account.id
                                        else selectedAccountIds - account.id
                                    }
                                )
                                SocialAppIcon(
                                    account.packageName, account.platform,
                                    Modifier.size(44.dp).clickable {
                                        val opened = SocialIntents.openAccount(context, account)
                                        if (!opened) feedback = "Kaydedilen uygulama açılamadı. Site seçeneğini deneyin."
                                    }, account.logoFile
                                )
                                Spacer(Modifier.width(9.dp))
                                Column(Modifier.weight(1f).clickable {
                                    val opened = SocialIntents.openAccount(context, account)
                                    if (!opened) feedback = "Kaydedilen uygulama açılamadı. Site seçeneğini deneyin."
                                }) {
                                    Text(account.label, fontWeight = FontWeight.Bold,
                                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text(account.platform, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodySmall)
                                    if (OfficialSocialLinks.isPlatformShortcut(account.platform, account.profileUrl)) {
                                        Text(
                                            tr("Uygulama kısayolu kayıtlı • API bağlantısı yok"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else if (OfficialSocialLinks.isOfficialProfile(account.platform, account.profileUrl)) {
                                        Text(
                                            tr("Resmî profili aç") + " • " +
                                                tr(AuthorizedChannelRules.verifiedLabel(account.authState)),
                                            modifier = Modifier.clickable {
                                                if (!OfficialSocialLinks.launchProfile(context, account)) {
                                                    feedback = "Profil bağlantısı açılamadı."
                                                }
                                            }.padding(top = 5.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    } else {
                                        Text(tr(AuthorizedChannelRules.verifiedLabel(account.authState)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    TextButton(onClick = {
                                        if (!OfficialSocialLinks.launchProfile(context, account))
                                            feedback = "Kayıtlı profil adresi açılamadı."
                                    }) { Text(tr("Site ›")) }
                                    TextButton(onClick = {
                                        if (!SocialIntents.openAccount(context, account)) {
                                            feedback = "Telefondaki uygulama açılamadı. Siteyi kullanabilirsiniz."
                                        }
                                    }) { Text(tr("Uygulama ›")) }
                                    TextButton(onClick = { deleteAccount = account }) {
                                        Text(tr("Kaldır"), color = alertRed)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            CreatorFooter(context)
        }

        if (addCreator) NewCreatorDialog(onDismiss = { addCreator = false }) { name, imageUri ->
            var importedPath = ""
            try {
                if (imageUri != null) importedPath = LocalLogos.importImage(context, imageUri)
                val id = database.addCreator(name, importedPath)
                reload()
                creatorId = id
                reload()
                addCreator = false
                null
            } catch (e: Exception) {
                if (importedPath.isNotBlank()) LocalLogos.delete(context, importedPath)
                e.message ?: "İçerik eklenemedi."
            }
        }

        addAccountFor?.let { initial ->
            NewAccountDialog(
                initial = initial,
                existingAccounts = accounts,
                onDismiss = { addAccountFor = null },
                onMessage = { feedback = it }
            ) {
                    platform, label, url, packageName, logoUri ->
                var logoPath = ""
                try {
                    logoPath = logoUri?.let { LocalLogos.importImage(context, it) }.orEmpty()
                    val addedId = database.addAccount(creatorId, platform, label, url, packageName, logoPath)
                    reload()
                    selectedAccountIds = selectedAccountIds + addedId
                    filterPlatform = null
                    addAccountFor = null
                    null
                } catch (e: Exception) {
                    if (logoPath.isNotBlank()) LocalLogos.delete(context, logoPath)
                    e.message ?: "Hesap eklenemedi."
                }
            }
        }

        deleteCreator?.let { creator ->
            ConfirmDelete("İçerik sayfasını sil", "${creator.name} ve tüm hesap bağlantıları silinsin mi?",
                onCancel = { deleteCreator = null },
                onDelete = {
                    TelegramSecureStore.removeForCreator(context, creator.id)
                    BlueskySecureStore.removeForCreator(context, creator.id)
                    database.deleteCreator(creator.id)
                    creatorId = -1
                    selectedAccountIds = emptySet()
                    deleteCreator = null
                    reload()
                })
        }
        deleteAccount?.let { account ->
            ConfirmDelete("Hesabı kaldır", "${account.label} bağlantısı kaldırılsın mı?",
                onCancel = { deleteAccount = null },
                onDelete = {
                    database.deleteAccount(account.id)
                    selectedAccountIds = selectedAccountIds - account.id
                    deleteAccount = null
                    reload()
                })
        }
        if (firstLaunchPrompt) AlertDialog(
            onDismissRequest = {
                prefs.edit().putBoolean("reminder_permission_explained", true).apply()
                firstLaunchPrompt = false
            },
            title = { Text(tr("Zamanlanmış paylaşım hatırlatmaları")) },
            text = {
                Text("Planladığınız fotoğraf ve videoların yayın zamanı geldiğinde " +
                    "telefonunuzda hatırlatma göstermek için bildirim izni gerekir. " +
                    "Bu izin Instagram, Facebook veya başka uygulamaların bildirimlerini okumaz.")
            },
            confirmButton = {
                TextButton(onClick = {
                    prefs.edit().putBoolean("reminder_permission_explained", true).apply()
                    firstLaunchPrompt = false
                    notificationRequester.launch(Manifest.permission.POST_NOTIFICATIONS)
                }) { Text(tr("İzin iste")) }
            },
            dismissButton = {
                TextButton(onClick = {
                    prefs.edit().putBoolean("reminder_permission_explained", true).apply()
                    firstLaunchPrompt = false
                }) { Text(tr("Şimdi değil")) }
            }
        )
        if (showPremium) OwnerPremiumDialog(context, onDismiss = {
            showPremium = false
            accessRevision++
        })
        if (showLockSettings) {
            val activity = context as? FragmentActivity
            if (activity != null) AppLockSettings(activity, onDismiss = { showLockSettings = false })
        }
        nameTranslationsFor?.let { creator ->
            CreatorNameTranslationsDialog(
                creator = creator,
                onDismiss = { nameTranslationsFor = null },
                onSave = { names ->
                    database.setCreatorTranslations(creator.id, names)
                    reload()
                    nameTranslationsFor = null
                    feedback = Translations.text(language, "Değişiklikler kaydedildi.")
                }
            )
        }
        if (showLicenses) AlertDialog(
            onDismissRequest = { showLicenses = false },
            title = { Text(tr("Gizlilik ve lisanslar")) },
            text = {
                Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                    Text("Bu sürümün veritabanı ve bağlantı motorları doğrudan telefonda çalışır. " +
                        "İçerik profilleri, görseller, bağlantı adları, hesaplar ve gönderi taslakları " +
                        "yalnız telefonda tutulur; RaSaSeR sunucusuna veya web sitesine gönderilmez. " +
                        "Telegram kanal bot anahtarları ve Bluesky oturum tokenları Android Keystore " +
                        "ile AES-GCM şifrelenerek cihazda saklanır. Bluesky ana hesap şifresi istenmez; " +
                        "özel uygulama parolası doğrudan Bluesky sunucusuyla oturum açmak için kullanılır " +
                        "ve bağlantı kurulunca cihazda bile saklanmaz. " +
                        "Premium kodu imza kontrolü de cihaz içinde yapılır; internete çıkmaz. " +
                        "Kullanıcı açıkça gönder dediğinde seçilen içerik, yalnız ilgili sosyal " +
                        "medya platformuna gönderilir. İnternet yoksa bu gönderim gerçekleşmez. " +
                        "Instagram, Facebook, TikTok, Pinterest ve YouTube için yetki olmadığında " +
                        "manuel paylaşım sunulur; otomatik yayın başarılı gösterilmez. " +
                        "Uygulama yedeği ve bulut eşitlemesi kapalıdır; genel sosyal bildirimler okunmaz.")
                    Spacer(Modifier.height(12.dp))
                    Text(remember {
                        context.assets.open("legal/licenses.txt").bufferedReader().use { it.readText() }
                    })
                }
            },
            confirmButton = { TextButton(onClick = { showLicenses = false }) { Text(tr("Kapat")) } }
        )
        if (feedback.isNotBlank()) AlertDialog(
            onDismissRequest = { feedback = "" },
            title = { Text(tr("Bilgi")) },
            text = { Text(feedback) },
            confirmButton = { TextButton(onClick = { feedback = "" }) { Text(tr("Tamam")) } }
        )
    }
    }
}

@Composable
private fun LightCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = shape,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) { content() }
}

@Composable
private fun MainButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick, modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(15.dp)
    ) { Text(text, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(6.dp)) }
}

@Composable
private fun PlatformTile(
    platform: Platform, selected: Boolean, onSelect: () -> Unit
) {
    Column(
        modifier = Modifier.width(78.dp).clip(RoundedCornerShape(16.dp))
            .background(if (selected) MaterialTheme.colorScheme.surfaceVariant
                        else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onSelect).padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SocialAppIcon(platform.packageName, platform.name, Modifier.size(45.dp))
        Spacer(Modifier.height(6.dp))
        Text(platform.name, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

@Composable
private fun SocialAppIcon(
    packageName: String, platform: String, modifier: Modifier = Modifier,
    logoFile: String = ""
) {
    val context = LocalContext.current
    val image = remember(packageName, logoFile) {
        val custom = if (logoFile.isNotBlank()) LocalLogos.load(context, logoFile) else null
        if (custom != null) custom.asImageBitmap()
        else if (packageName.isBlank()) null else try {
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
private fun NewCreatorDialog(onDismiss: () -> Unit, onCreate: (String, Uri?) -> String?) {
    var name by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var logoUri by remember { mutableStateOf<Uri?>(null) }
    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        logoUri = uri
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Yeni içerik üreticisi")) },
        text = {
            Column {
                Text(tr("İçerik ismini yazın. İstediğiniz kadar ayrı sayfa oluşturabilirsiniz."))
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(name, onValueChange = { name = it.take(80) },
                    label = { Text(tr("İçerik adı")) }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                OutlinedButton(onClick = { logoPicker.launch("image/*") }) {
                    Text(if (logoUri != null) tr("Fotoğraf seçildi") else tr("Fotoğraf veya logo seç"))
                }
                if (error.isNotBlank()) Text(error, color = alertRed)
            }
        },
        confirmButton = {
            TextButton(onClick = { error = onCreate(name, logoUri).orEmpty() }) { Text(tr("Oluştur")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Vazgeç")) } }
    )
}

@Composable
private fun NewAccountDialog(
    initial: String,
    existingAccounts: List<SocialAccount>,
    onDismiss: () -> Unit,
    onMessage: (String) -> Unit,
    onAdd: (String, String, String, String, Uri?) -> String?
) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf(initial) }
    var customPlatform by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var customPackage by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var selectedLogo by remember { mutableStateOf<Uri?>(null) }
    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        selectedLogo = it
    }
    val isCustom = selected == "Diğer"

    // User consent and social login are not inferred from a visited website.
    // Once a valid profile URL/handle is entered, saving needs only one tap.
    fun saveAccount(openAfterSaving: String? = null): Boolean {
        return try {
            val platform = if (isCustom) customPlatform.trim() else selected
            require(platform.isNotBlank()) { "Önce sosyal medya platformunu seçin." }

            // A user can save an APP/WEB SHORTCUT without entering their username.
            // This is NOT a linked account and does not grant platform OAuth access.
            val shortcut = url.isBlank() && openAfterSaving != null && !isCustom
            val profile = if (shortcut) "" else SocialAccountInput.profileUrl(platform, url)
            val localCount = existingAccounts.count {
                it.platform.equals(platform, ignoreCase = true)
            } + 1
            val accountLabel = if (shortcut) {
                label.trim().ifBlank {
                    "$platform ${if (openAfterSaving == "app") "uygulama" else "web"} $localCount"
                }
            } else SocialAccountInput.displayLabel(platform, label, profile)
            require(accountLabel.length <= 80) { "Hesap adı en fazla 80 karakter olabilir." }

            val packageName = if (isCustom) customPackage.trim() else
                SocialCatalog.find(platform)?.packageName.orEmpty()
            val outcome = onAdd(
                platform, accountLabel, profile, packageName, if (isCustom) selectedLogo else null
            )
            error = outcome.orEmpty()
            if (outcome != null) return false

            // The record is saved before launching either external application.
            // Avoid success pop-ups that would obscure the target app (ACTION_SEND).
            if (openAfterSaving == "app") {
                val bookmarked = SocialAccount(
                    -1L, -1L, platform, accountLabel,
                    if (shortcut) SocialCatalog.find(platform)?.homeUrl.orEmpty() else profile,
                    packageName, ""
                )
                val opened = SocialIntents.openAccount(context, bookmarked)
                if (!opened) onMessage(
                    "Kısayol kaydedildi. Ancak telefondaki $platform uygulaması açılamadı."
                )
            } else if (openAfterSaving == "web") {
                val opened = OfficialSocialLinks.launchOfficialWebsite(
                    context, platform, if (shortcut) null else profile
                )
                if (!opened) onMessage(
                    "Web kısayolu kaydedildi. İnternet tarayıcısı açılamadı; " +
                        "telefonunuzdaki tarayıcı ayarlarını kontrol edin."
                )
            }
            true
        } catch (e: IllegalArgumentException) {
            error = e.message ?: "Profil bilgilerini kontrol edin."
            false
        } catch (_: Exception) {
            error = "Hesap kaydedilemedi. Bilgileri kontrol edip yeniden deneyin."
            false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Sosyal medya hesabı ekle")) },
        text = {
            Column(Modifier.heightIn(max = 500.dp).verticalScroll(rememberScrollState())) {
                Text(tr("Platform"), fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(SocialCatalog.presets.map { it.name } + "Diğer") { option ->
                        FilterChip(selected == option, onClick = {
                            selected = option
                            error = ""
                        }, label = { Text(option) })
                    }
                }
                if (isCustom) {
                    OutlinedTextField(customPlatform, { customPlatform = it.take(50) },
                        label = { Text(tr("Sosyal medya adı")) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(customPackage, { customPackage = it.trim().take(150) },
                        label = { Text(tr("Android paket adı (isteğe bağlı)")) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedButton(onClick = { logoPicker.launch("image/*") }) {
                        Text(if (selectedLogo == null) "＋ Logo seç (galeriden)" else "✓ Logo seçildi")
                    }
                }

                OutlinedTextField(
                    url, { url = it.take(2048); error = "" },
                    label = { Text(tr("Profil bağlantısı veya @kullanıcıadı")) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                OutlinedTextField(
                    label, { label = it.take(80); error = "" },
                    label = { Text(tr("Hesap adı (isteğe bağlı)")) },
                    supportingText = { Text(tr("Boş kalırsa profil bağlantısından alınır.")) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                if (!isCustom) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { saveAccount("app") },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(tr("Uygulama kısayolunu kaydet ve aç")) }
                    Spacer(Modifier.height(5.dp))
                    OutlinedButton(
                        onClick = { saveAccount("web") },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(tr("Web kısayolunu kaydet ve aç")) }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    tr("Sınırsız kısayol kaydedilebilir. Uygulamayı açmak hesaba OAuth bağlantısı veya otomatik gönderim izni vermez."),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                if (error.isNotBlank()) {
                    Spacer(Modifier.height(7.dp))
                    Text(error, color = alertRed, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { saveAccount() }) { Text(tr("Hesabı kaydet")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Vazgeç")) } }
    )
}

@Composable
private fun ConfirmDelete(title: String, body: String, onCancel: () -> Unit, onDelete: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { TextButton(onClick = onDelete) { Text(tr("Sil"), color = alertRed) } },
        dismissButton = { TextButton(onClick = onCancel) { Text(tr("Vazgeç")) } }
    )
}

