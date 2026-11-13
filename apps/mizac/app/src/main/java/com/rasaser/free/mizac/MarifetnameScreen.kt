package com.rasaser.free.mizac

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val darkText = Color(0xFF231A13)
private val cardColor = Color(0xFFFFFBF6)
private val secondaryText = Color(0xFF605344)

private data class HistoricalSection(val name: String, val content: String)

private val historicalSections = listOf(
    HistoricalSection(
        "Boy ve bedenin genel yapısı",
        "Kıyâfetnâme geleneğinde kısa, orta ve uzun boy için farklı ahlâkî " +
            "anlamlar kurulurdu. Orta boyun 'güzel huyluluk' ile bağdaştırılması " +
            "tarihî bir örnektir, insan hakkında geçerli bir kişilik ölçümü değildir."
    ),
    HistoricalSection(
        "Saç",
        "Saç rengi ve yapısı üzerinden sabır, atılganlık veya başka huy " +
            "yorumları yapılmıştır. Saç rengi ya da dokusu bu özellikleri kanıtlamaz."
    ),
    HistoricalSection(
        "Alın ve kaş",
        "Alın genişliği, görünümü ve kaşların biçimi hakkında çeşitli " +
            "karakter değerlendirmeleri aktarılmıştır. Bu varsayımlar doğrulanmış " +
            "psikometrik yöntemler değildir."
    ),
    HistoricalSection(
        "Göz",
        "Gözlerin biçimi, büyüklüğü ve rengiyle ilgili mizaç yorumları eserde " +
            "yer alan kıyâfet geleneğinin parçalarıdır. Göz rengi ve şekli " +
            "kişinin değerini veya dürüstlüğünü göstermez."
    ),
    HistoricalSection(
        "Yüz ve burun",
        "Yüzün yuvarlaklığı, burun uzunluğu ve ağız biçimi üzerinden sembolik " +
            "huy bağlantıları kurulmuştur. Çağdaş yüz geometrisi bu hükümleri " +
            "bilimsel olarak doğrulayan bir araç değildir."
    )
)

@Composable
fun MarifetnameScreen(onBack: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = onBack) { Text("‹ Ana sayfa") }
        Text(
            "Mârifetnâme • Tarihî Yüz Okuma",
            color = darkText,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Card(shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = cardColor)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tarihî görüş, bilimsel kişilik testi değildir", color = darkText,
                    fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Erzurumlu İbrahim Hakkı (1703–1780), Mârifetnâme'de ilm-i kıyâfet " +
                        "geleneğinden söz eder. Aşağıda bu geleneğin nasıl bir düşünce " +
                        "sistemi olduğu açıklanır. Yüzünüz taransa bile bu görüşlerden " +
                        "size karakter veya ahlâk hükmü üretilmez.",
                    color = secondaryText
                )
            }
        }
        historicalSections.forEach { item ->
            Card(modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(item.name, color = darkText, fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium)
                    Text(item.content, color = secondaryText)
                }
            }
        }
        Text("Kaynakça", color = darkText, fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium)
        Text(
            "• Türkiye Diyanet Vakfı İslâm Ansiklopedisi, “İbrâhim Hakkı Erzurûmî”.\n" +
                "• Erzurumlu İbrahim Hakkı, Mârifetnâme, ilm-i kıyâfet / " +
                "kıyâfetnâme bölümü.\n" +
                "• TDK Belleten, “Hamdullah Hamdî’nin Kıyafetname’sinin Bir Nüshası Üzerine” " +
                "(kıyâfetnâme türünün tarihi).",
            color = secondaryText, style = MaterialTheme.typography.bodySmall
        )
        Text(
            "Günümüzde kişilik değerlendirmesi, yüz görünümü yerine " +
                "geçerliği araştırılmış soru ölçekleri ve kişinin kendisinin verdiği " +
                "bilgiler üzerinden yapılmalıdır.",
            color = darkText, style = MaterialTheme.typography.bodyMedium
        )
    }
}
