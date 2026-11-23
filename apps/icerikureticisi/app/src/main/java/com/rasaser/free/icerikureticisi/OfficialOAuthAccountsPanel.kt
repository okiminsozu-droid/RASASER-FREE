package com.rasaser.free.icerikureticisi

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A verified account is displayed ONLY after an approved provider OAuth exchange.
 * Profile URL bookmarks do not appear in this panel and cannot be published.
 *
 * The OAuth endpoint adapters are intentionally OFF until app registrations
 * and official publish scopes are configured. This screen never requests a
 * social network account's password.
 */
@Composable
fun OfficialOAuthAccountsPanel(creatorId: Long, revision: Int) {
    val context = LocalContext.current
    var accounts by remember(creatorId, revision) {
        mutableStateOf(runCatching { DeviceOAuthVault.accounts(context, creatorId) }
            .getOrElse { emptyList() })
    }
    var selected by remember(creatorId) { mutableStateOf<Set<String>>(emptySet()) }
    val four = listOf("Facebook", "Instagram", "X", "YouTube")
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🔐 Resmî API hesapları • " + accounts.size,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)
            Text("Her hesabın izni, kimliği ve bağlantı süresi telefonda ayrı ve şifreli tutulur.",
                style = MaterialTheme.typography.bodySmall)
            if (accounts.isEmpty()) {
                Text("Facebook, Instagram, X ve YouTube resmî geliştirici uygulama izinleri henüz " +
                    "yapılandırılmadı. Profil kısayolu, yetkilendirilmiş hesap değildir.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else accounts.sortedWith(
                compareBy<DeviceOAuthAccount> { four.indexOf(it.provider).takeIf { index -> index >= 0 } ?: 99 }
                    .thenBy { it.displayName }
            ).forEach { account ->
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()) {
                    brandIconRes(account.provider)?.let { res ->
                        Image(painterResource(res), contentDescription = account.provider,
                            modifier = Modifier.size(30.dp))
                    }
                    Checkbox(
                        checked = account.id in selected,
                        onCheckedChange = { checked ->
                            selected = if (checked) selected + account.id else selected - account.id
                        },
                        enabled = !account.authorizationRequired &&
                            account.expiresAtMillis > System.currentTimeMillis()
                    )
                    Column(Modifier.weight(1f)) {
                        Text(account.provider + " • " + account.displayName,
                            fontWeight = FontWeight.SemiBold)
                        Text(
                            if (account.authorizationRequired) "Yeniden yetki gerekli"
                            else if (account.expiresAtMillis <= System.currentTimeMillis())
                                "Oturum yenileme gerekli"
                            else "Resmî hesap kimliği doğrulandı",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = {
                        DeviceOAuthVault.remove(context, creatorId, account.id)
                        accounts = DeviceOAuthVault.accounts(context, creatorId)
                        selected = selected - account.id
                    }) { Text("Kaldır") }
                }
            }
            Text("Gizli anahtarlar APK'ya yazılmaz. API izinleri olmadan otomatik yayın başlatılmaz.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
