package com.rasaser.free.mizac

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale

private data class TraitItem(val statement: String, val factor: String, val reverse: Boolean)

private val traitLabels = listOf(
    "E" to "Dışadönüklük",
    "A" to "Uyumluluk",
    "C" to "Özdisiplin ve düzen",
    "S" to "Duygusal denge",
    "O" to "Deneyime açıklık"
)

// TIPI (Gosling, Rentfrow & Swann, 2003): 7 points, 10 items,
// exact five two-item domain key and reverse scoring.
// Turkish wording below is a provisional translation, not an independently validated Turkish form.
private val tipi10 = listOf(
    TraitItem("Sosyal ve coşkulu", "E", false),
    TraitItem("Eleştirel ve tartışmacı", "A", true),
    TraitItem("Güvenilir ve özdisiplinli", "C", false),
    TraitItem("Kaygılı, kolay üzülen", "S", true),
    TraitItem("Yeni deneyimlere açık, karmaşık düşüncelere yatkın", "O", false),
    TraitItem("Çekingen ve sessiz", "E", true),
    TraitItem("Anlayışlı ve sıcakkanlı", "A", false),
    TraitItem("Düzensiz ve dikkatsiz", "C", true),
    TraitItem("Sakin ve duygusal bakımdan dengeli", "S", false),
    TraitItem("Geleneksel ve yaratıcı olmayan", "O", true)
)

// The canonical 50 IPIP factor markers (Goldberg / IPIP); 10 per Big Five factor.
// Item polarity/order within each factor follows ipip.ori.org/newBigFive5broadKey.htm
// Turkish phrasing is provisional; no Turkish validity or norms are claimed here.
private val ipip50 = listOf(
    TraitItem("Bir topluluğun neşeli ve canlı kişisi olurum.", "E", false),
    TraitItem("İnsanların yanında kendimi rahat hissederim.", "E", false),
    TraitItem("Konuşma başlatırım.", "E", false),
    TraitItem("Toplantılarda çok sayıda farklı insanla sohbet ederim.", "E", false),
    TraitItem("İlginin merkezinde olmak beni rahatsız etmez.", "E", false),
    TraitItem("Çok konuşmam.", "E", true),
    TraitItem("Arka planda kalmayı tercih ederim.", "E", true),
    TraitItem("Söyleyecek pek az şeyim olur.", "E", true),
    TraitItem("Dikkati üzerime çekmekten hoşlanmam.", "E", true),
    TraitItem("Yabancıların yanında sessiz kalırım.", "E", true),
    TraitItem("İnsanlarla ilgilenirim.", "A", false),
    TraitItem("Başkalarının duygularını anlayabilirim.", "A", false),
    TraitItem("Yumuşak kalpliyimdir.", "A", false),
    TraitItem("Başkalarına zaman ayırırım.", "A", false),
    TraitItem("Başkalarının duygularını hissedebilirim.", "A", false),
    TraitItem("İnsanları rahat hissettiririm.", "A", false),
    TraitItem("Diğer insanlarla pek ilgilenmem.", "A", true),
    TraitItem("İnsanlara hakaret ederim.", "A", true),
    TraitItem("Başkalarının sorunlarıyla ilgilenmem.", "A", true),
    TraitItem("Başkaları için pek kaygılanmam.", "A", true),
    TraitItem("Her zaman hazırlıklı olurum.", "C", false),
    TraitItem("Ayrıntılara dikkat ederim.", "C", false),
    TraitItem("Ev işlerini hemen hallederim.", "C", false),
    TraitItem("Düzeni severim.", "C", false),
    TraitItem("Bir programa uyarım.", "C", false),
    TraitItem("İşimde titiz davranırım.", "C", false),
    TraitItem("Eşyalarımı etrafta bırakırım.", "C", true),
    TraitItem("Etrafı dağıtırım.", "C", true),
    TraitItem("Eşyaları yerlerine koymayı sık sık unuturum.", "C", true),
    TraitItem("Sorumluluklarımdan kaçarım.", "C", true),
    TraitItem("Çoğu zaman rahatım.", "S", false),
    TraitItem("Nadiren kendimi kederli hissederim.", "S", false),
    TraitItem("Kolayca strese girerim.", "S", true),
    TraitItem("Bazı şeyler hakkında sık sık endişelenirim.", "S", true),
    TraitItem("Kolayca rahatsız olurum.", "S", true),
    TraitItem("Kolayca üzülürüm.", "S", true),
    TraitItem("Ruh hâlim sık değişir.", "S", true),
    TraitItem("Sık sık duygu dalgalanmaları yaşarım.", "S", true),
    TraitItem("Kolayca sinirlenirim.", "S", true),
    TraitItem("Sık sık mutsuz hissederim.", "S", true),
    TraitItem("Geniş bir sözcük dağarcığım vardır.", "O", false),
    TraitItem("Canlı bir hayal gücüm vardır.", "O", false),
    TraitItem("Harika fikirlerim olur.", "O", false),
    TraitItem("Bir şeyleri çabuk anlarım.", "O", false),
    TraitItem("Zor kelimeler kullanırım.", "O", false),
    TraitItem("Bir şeyler üzerine düşünerek zaman geçiririm.", "O", false),
    TraitItem("Fikirlerle doluyumdur.", "O", false),
    TraitItem("Soyut düşünceleri anlamakta zorlanırım.", "O", true),
    TraitItem("Soyut fikirlerle ilgilenmem.", "O", true),
    TraitItem("Hayal gücüm pek iyi değildir.", "O", true)
)

private fun personalityScores(items: List<TraitItem>, answers: List<Int>, max: Int): List<Pair<String, Float>> {
    require(items.size == answers.size && answers.all { it in 1..max })
    return traitLabels.map { (factor, label) ->
        val values = items.mapIndexedNotNull { i, item ->
            if (item.factor != factor) null
            else (if (item.reverse) max + 1 - answers[i] else answers[i]).toFloat()
        }
        label to values.average().toFloat()
    }
}

@Composable
fun PersonalityTestScreen(questionCount: Int, onBack: () -> Unit) {
    val dark = Color(0xFF231A13)
    val faded = Color(0xFF605344)
    val white = Color(0xFFFFFBF6)
    val questions = when (questionCount) {
        10 -> tipi10
        50 -> ipip50
        else -> emptyList()
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = onBack) { Text("‹ Analiz Seçimi") }
        Text("Bilimsel Kişilik Ölçekleri", color = dark,
            style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        if (questions.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = white)) {
                Text(
                    "120 soruluk IPIP-NEO için özgün 120 madde, 30 alt boyut " +
                        "puanlama anahtarı ve Türkçe çeviri doğrulaması henüz tamamlanmadı. " +
                        "Yanlış rapor üretmemek için bu seçenek şimdilik puanlama yapmaz. " +
                        "Lütfen 10 veya 50 soruluk sürümü seçin.",
                    modifier = Modifier.padding(18.dp), color = dark
                )
            }
            return@Column
        }
        Text(
            if (questionCount == 10)
                "TIPI-10 (Gosling ve ark., 2003). 7 aşamalı yanıt, 5 boyut. " +
                    "Türkçe metin ön çeviridir; kısa ölçek daha sınırlı hassasiyete sahiptir."
            else
                "IPIP-50 kamu malı ölçeğinin özgün 50 maddelik yapısının Türkçe ön çevirisi. " +
                    "Bu çeviriye ait Türkçe norm ve geçerlik henüz doğrulanmamıştır.",
            color = faded, style = MaterialTheme.typography.bodyMedium
        )
        var answers by rememberSaveable(questionCount) {
            mutableStateOf(List(questions.size) { 0 })
        }
        var index by rememberSaveable(questionCount) { mutableStateOf(0) }
        var showResults by rememberSaveable(questionCount) { mutableStateOf(false) }
        if (showResults && answers.all { it > 0 }) {
            Text("Kendi Yanıtlarınızın Özeti", color = dark,
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            personalityScores(questions, answers, if (questionCount == 10) 7 else 5)
                .forEach { (label, score) ->
                    val maximum = if (questionCount == 10) 7f else 5f
                    Text(label, color = dark, style = MaterialTheme.typography.titleMedium)
                    LinearProgressIndicator(
                        progress = { (score / maximum).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        String.format(Locale.getDefault(), "%.2f / %.0f", score, maximum),
                        color = faded, style = MaterialTheme.typography.bodySmall
                    )
                }
            Text(
                "Sonuçlar özgün ölçeklerin yanıt anahtarıyla hesaplanan alan " +
                    "ortalamalarıdır; toplumsal yüzdelik, klinik teşhis veya " +
                    "değişmez karakter hükmü değildir. Türkçe ön çevirilerin " +
                    "psikometrik doğrulaması yapılmamıştır.",
                color = faded, style = MaterialTheme.typography.bodySmall
            )
            Button(onClick = {
                answers = List(questions.size) { 0 }
                index = 0
                showResults = false
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Yeniden Başla")
            }
        } else {
            val max = if (questionCount == 10) 7 else 5
            val question = questions[index]
            Text("Soru ${index + 1} / ${questions.size}", color = dark,
                style = MaterialTheme.typography.titleMedium)
            LinearProgressIndicator(
                progress = { (index + 1f) / questions.size },
                modifier = Modifier.fillMaxWidth()
            )
            Card(shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = white)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (questionCount == 10) {
                        Text("Kendimi şöyle tanımlarım:",
                            color = faded, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(question.statement, color = dark,
                        style = MaterialTheme.typography.titleLarge)
                    Text(
                        if (questionCount == 10)
                            "1 = Kesinlikle katılmıyorum   ·   7 = Kesinlikle katılıyorum"
                        else "1 = Hiç uygun değil   ·   5 = Tamamen uygun",
                        color = faded, style = MaterialTheme.typography.bodySmall
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        (1..max).forEach { n ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                RadioButton(
                                    selected = answers[index] == n,
                                    onClick = {
                                        answers = answers.toMutableList().also { it[index] = n }
                                    }
                                )
                                Text(n.toString(), color = dark)
                            }
                        }
                    }
                }
            }
            Text(
                "Yanıtlar bu oturumda telefonda kalır. RaSaSeR'a gönderilmez.",
                color = faded, style = MaterialTheme.typography.bodySmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { if (index > 0) index-- },
                    enabled = index > 0, modifier = Modifier.weight(1f)) {
                    Text("Geri")
                }
                Button(
                    onClick = {
                        if (index < questions.lastIndex) index++
                        else if (answers.all { it > 0 }) showResults = true
                    },
                    enabled = answers[index] != 0 &&
                        (index < questions.lastIndex || answers.all { it != 0 }),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = white, contentColor = dark)
                ) { Text(if (index == questions.lastIndex) "Sonucu Gör" else "İleri") }
            }
        }
    }
}
