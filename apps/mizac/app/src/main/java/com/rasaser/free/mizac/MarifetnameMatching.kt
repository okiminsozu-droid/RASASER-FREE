package com.rasaser.free.mizac

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Public-domain 18th century source: Erzurumlu İbrahim Hakkı, Mârifetnâme,
 * fifth division, fourth and fifth sections.
 * https://tr.wikisource.org/wiki/Marifetname/42
 *
 * Concise, independently worded source attributions; not individual character classifications.
 */
private data class HistoricalChoice(
    val id: String,
    val title: List<String>,
    val reading: List<String>
) {
    fun name(lang: MizacLang): String = title[lang.index]
    fun explanation(lang: MizacLang): String = reading[lang.index]
}

private data class HistoricalGroup(
    val id: String,
    val title: List<String>,
    val choices: List<HistoricalChoice>
) {
    fun name(lang: MizacLang): String = title[lang.index]
}

private fun words(vararg translations: String): List<String> =
    translations.toList().also { require(it.size == 5) }

private val matchingGroups = listOf(
    HistoricalGroup("face", words("Yüz görünümü","Face shape","Gesichtsform","Forma del rostro","شكل الوجه"), listOf(
        HistoricalChoice("face_round", words("Yuvarlak yüz","Rounded face","Rundes Gesicht","Rostro redondeado","وجه مستدير"),
            words(
                "Güler yüzlü ve tebessümlü oluşla bağdaştırılır.",
                "Associated with smiling and a cheerful appearance.",
                "Mit Lächeln und freundlichem Ausdruck verbunden.",
                "Se relaciona con la sonrisa y la expresión afable.",
                "يرتبط بالابتسام والبشاشة."
            )),
        HistoricalChoice("face_long", words("Uzun yüz","Long face","Längliches Gesicht","Rostro alargado","وجه طويل"),
            words(
                "Sözlerinde yalana ilişkin yorum yer alır.",
                "The text associates this shape with untruthful speech.",
                "Der Text verbindet diese Form mit unwahren Aussagen.",
                "Se vincula con la falta de veracidad al hablar.",
                "يربط النص هذا الشكل بالكذب في الكلام."
            ))
    )),
    HistoricalGroup("eyes", words("Gözlerin görünümü","Eye appearance","Augenform","Aspecto de los ojos","مظهر العينين"), listOf(
        HistoricalChoice("eyes_big", words("Büyük göz","Large-looking eyes","Groß wirkende Augen","Ojos grandes","عينان كبيرتان"),
            words(
                "Zariflikle bağdaştırılır.",
                "Linked with refinement.",
                "Mit Feinheit verbunden.",
                "Se relaciona con la delicadeza.",
                "يرتبط بالرقة."
            )),
        HistoricalChoice("eyes_small", words("Küçük göz","Small-looking eyes","Klein wirkende Augen","Ojos pequeños","عينان صغيرتان"),
            words(
                "Hafif huylulukla bağdaştırılır.",
                "Linked with frivolity.",
                "Mit Leichtfertigkeit verbunden.",
                "Se relaciona con la ligereza de carácter.",
                "يرتبط بخفة الطبع."
            ))
    )),
    HistoricalGroup("nose", words("Burun görünümü","Nose appearance","Nasenform","Aspecto de la nariz","مظهر الأنف"), listOf(
        HistoricalChoice("nose_long", words("Uzun burun","Long nose","Lange Nase","Nariz larga","أنف طويل"),
            words(
                "Anlayış kıtlığıyla bağdaştırılır.",
                "Linked with limited understanding.",
                "Mit geringem Verständnis verbunden.",
                "Se relaciona con poca comprensión.",
                "يرتبط بضعف الفهم."
            )),
        HistoricalChoice("nose_short", words("Kısa burun","Short nose","Kurze Nase","Nariz corta","أنف قصير"),
            words(
                "Korkaklıkla bağdaştırılır.",
                "Linked with fearfulness.",
                "Mit Ängstlichkeit verbunden.",
                "Se relaciona con el temor.",
                "يرتبط بالخوف."
            ))
    )),
    HistoricalGroup("forehead", words("Alın görünümü","Forehead appearance","Stirnform","Aspecto de la frente","مظهر الجبهة"), listOf(
        HistoricalChoice("forehead_medium", words("Dengeli alın","Proportionate forehead","Ausgewogene Stirn","Frente proporcionada","جبهة متناسقة"),
            words(
                "Güvenilirlikle bağdaştırılır.",
                "Linked with trustworthiness.",
                "Mit Vertrauenswürdigkeit verbunden.",
                "Se relaciona con la confianza.",
                "يرتبط بالأمانة."
            )),
        HistoricalChoice("forehead_narrow", words("Dar alın","Narrow forehead","Schmale Stirn","Frente estrecha","جبهة ضيقة"),
            words(
                "Dar huylulukla bağdaştırılır.",
                "Linked with a narrow disposition.",
                "Mit engem Gemüt verbunden.",
                "Se relaciona con un carácter estrecho.",
                "يرتبط بضيق الطبع."
            ))
    )),
    HistoricalGroup("mouth", words("Ağız görünümü","Mouth appearance","Mundform","Aspecto de la boca","مظهر الفم"), listOf(
        HistoricalChoice("mouth_big", words("Büyük ağız","Large mouth","Großer Mund","Boca grande","فم كبير"),
            words(
                "Cesaretle bağdaştırılır.",
                "Linked with courage.",
                "Mit Mut verbunden.",
                "Se relaciona con la valentía.",
                "يرتبط بالشجاعة."
            )),
        HistoricalChoice("mouth_small", words("Küçük ağız","Small mouth","Kleiner Mund","Boca pequeña","فم صغير"),
            words(
                "Güzellik ve korkaklıkla bağdaştırılır.",
                "Linked with beauty and timidity.",
                "Mit Schönheit und Ängstlichkeit verbunden.",
                "Se relaciona con la belleza y el temor.",
                "يرتبط بالجمال والخوف."
            ))
    ))
)

@Composable
fun MarifetnameMatching(lang: MizacLang, strings: Map<String,String>) {
    val context = LocalContext.current
    Card(
        modifier=Modifier.fillMaxWidth(),
        colors=CardDefaults.cardColors(containerColor=Color(0xFFFFFBF6)),
        shape=RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier=Modifier.padding(14.dp),
            verticalArrangement=Arrangement.spacedBy(10.dp)
        ) {
            matchingGroups.forEach { group ->
                Text(group.name(lang),fontWeight=FontWeight.Bold,
                    color=Color(0xFF1E1711),
                    style=MaterialTheme.typography.titleMedium)
                group.choices.forEach { item ->
                    Row(
                        modifier=Modifier.fillMaxWidth(),
                        horizontalArrangement=Arrangement.spacedBy(10.dp)
                    ) {
                        Text(item.name(lang),modifier=Modifier.weight(1f),
                            color=Color(0xFF1E1711),
                            style=MaterialTheme.typography.bodySmall)
                        Text(item.explanation(lang),modifier=Modifier.weight(2f),
                            color=Color(0xFF605344),
                            style=MaterialTheme.typography.bodySmall)
                    }
                }
            }
            TextButton(onClick={
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://tr.wikisource.org/wiki/Marifetname/42"))
                    )
                }
            }) {
                Text(ui(strings,"historyMatchSource"))
            }
        }
    }
}
