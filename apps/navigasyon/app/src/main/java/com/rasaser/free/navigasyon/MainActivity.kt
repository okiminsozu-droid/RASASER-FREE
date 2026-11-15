package com.rasaser.free.navigasyon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Gold = Color(0xFFD7BA7A)
private val Dark = Color(0xFF0C1420)
private val Panel = Color(0xFF202936)

class MainActivity : ComponentActivity() {
    private var connection by mutableStateOf(Connection.OFFLINE)
    private var onlineAllowed by mutableStateOf(true)
    private var mapReady by mutableStateOf(false)
    private var checkingMap by mutableStateOf(true)
    private lateinit var network: DefaultNetworkMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferences = getSharedPreferences("offline_first_settings", MODE_PRIVATE)
        onlineAllowed = preferences.getBoolean("online_allowed", true)
        network = DefaultNetworkMonitor(this) { connection = it }
        setContent {
            val mode = OfflineFirstPolicy.decide(mapReady, connection, onlineAllowed)
            MaterialTheme {
                Column(
                    modifier = Modifier.fillMaxSize().background(Dark)
                        .verticalScroll(rememberScrollState()).padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    Text("RaSaSeR", color = Gold, fontWeight = FontWeight.Bold, fontSize = 27.sp)
                    Text("NAVİGASYON · TÜRKİYE", color = Color.White, fontSize = 17.sp)
                    Text("Çevrimdışı öncelikli sistem", color = Color(0xFFBBC7D5))
                    StatusCard("İnternet bağlantısı", when (connection) {
                        Connection.OFFLINE -> "Çevrimdışı · haritalar yerelde kalır"
                        Connection.METERED -> "Mobil / ölçülü ağ algılandı"
                        Connection.UNMETERED -> "Wi-Fi / ölçüsüz ağ algılandı"
                    })
                    StatusCard("Navigasyon politikası", when (mode.mode) {
                        NavigationMode.NEEDS_MAP -> "Türkiye haritası henüz yüklü değil"
                        NavigationMode.OFFLINE -> "Çevrimdışı çalışma"
                        NavigationMode.ONLINE_ASSISTED -> "Otomatik çevrimiçi destek açık"
                    })
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Panel),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("İnterneti otomatik kullan", color = Color.White, fontWeight = FontWeight.Bold)
                                Text("Bağlantı gelirse ek çevrimiçi veri kullanılabilir.",
                                    color = Color(0xFFBBC7D5), fontSize = 12.sp)
                            }
                            Switch(checked = onlineAllowed, onCheckedChange = {
                                onlineAllowed = it
                                preferences.edit().putBoolean("online_allowed", it).apply()
                            })
                        }
                    }
                    StatusCard("Çevrimdışı harita", when {
                        checkingMap -> "Cihazdaki harita dosyası kontrol ediliyor"
                        mapReady -> "Türkiye haritası SHA-256 kontrolünden geçti"
                        else -> "Harita indirme/aktarma henüz geliştirilmedi"
                    })
                    StatusCard("Güncellemeler", when {
                        !onlineAllowed -> "Kullanıcı internet özelliklerini kapattı"
                        mode.shouldCheckForUpdates -> "Wi-Fi uygun · güncelleme servisi bağlanacak"
                        else -> "Büyük indirmeler için Wi-Fi kullanılacak"
                    })
                    Text(
                        "Bu, bağlantı ve çevrimdışı öncelik prototipidir. Harita çizimi, " +
                            "GPS yönlendirme ve paket indirme henüz devreye alınmadı.",
                        color = Gold, fontSize = 13.sp
                    )
                    Text("Harita verisi © OpenStreetMap contributors · ODbL",
                        color = Color(0xFF9EADBD), fontSize = 11.sp)
                }
            }
        }
        Thread {
            val ok = LocalMapVerifier.turkeyMapReady(this)
            runOnUiThread {
                mapReady = ok
                checkingMap = false
            }
        }.start()
    }
    override fun onStart() {
        super.onStart()
        network.start()
    }
    override fun onStop() {
        network.stop()
        super.onStop()
    }
}

@Composable
private fun StatusCard(title: String, subtitle: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, color = Gold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color.White, fontSize = 15.sp)
        }
    }
}
