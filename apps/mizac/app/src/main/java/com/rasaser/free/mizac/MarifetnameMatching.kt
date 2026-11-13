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
 * These are independently written summaries of the HISTORICAL claims.
 * The user selects a descriptor: photography does NOT establish a character label.
 * Do not invent quantitative boundaries on the author's behalf.
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
                "Metindeki yuvarlak yüz tasvirinde tebessüm ve olumlu görünüşle bağlantı kurulmuştur.",
                "The historical text associates a rounded face with smiling and pleasant appearance.",
                "Der historische Text verbindet ein rundes Gesicht mit Lächeln und angenehmem Erscheinungsbild.",
                "El texto histórico relaciona el rostro redondo con la sonrisa y una apariencia agradable.",
                "يربط النص التاريخي الوجه المستدير بالابتسام والمظهر اللطيف."
            )),
        HistoricalChoice("face_long", words("Uzun yüz","Long face","Längliches Gesicht","Rostro alargado","وجه طويل"),
            words(
                "Metin uzun yüzü, kişinin sözlerinin doğruluğuna ilişkin olumsuz bir hükümle ilişkilendirir; bu, tarihî bir iddiadır.",
                "The text links a long face with a negative judgment about truthfulness; this is a historical claim.",
                "Der Text verbindet ein langes Gesicht mit einer negativen Aussage über Wahrhaftigkeit; das ist eine historische Behauptung.",
                "El texto vincula el rostro alargado con una opinión negativa sobre la veracidad; es una afirmación histórica.",
                "يربط النص الوجه الطويل بحكم سلبي عن صدق الكلام، وهذا اعتقاد تاريخي."
            ))
    )),
    HistoricalGroup("eyes", words("Gözlerin görünümü","Eye appearance","Augenform","Aspecto de los ojos","مظهر العينين"), listOf(
        HistoricalChoice("eyes_big", words("Büyük göz","Large-looking eyes","Groß wirkende Augen","Ojos grandes","عينان كبيرتان"),
            words(
                "Eserde büyük göz, zariflik kavramıyla ilişkilendirilen eski bir tasvirdir.",
                "The text historically associates large eyes with refinement.",
                "Der Text verbindet große Augen historisch mit Feinheit.",
                "El texto relaciona históricamente los ojos grandes con delicadeza.",
                "يربط النص تاريخيًا العينين الكبيرتين بالرقة."
            )),
        HistoricalChoice("eyes_small", words("Küçük göz","Small-looking eyes","Klein wirkende Augen","Ojos pequeños","عينان صغيرتان"),
            words(
                "Eserde küçük göz hakkında olumlu olmayan bir huy tasviri bulunur. Bu, insanları değerlendirmek için geçerli kanıt değildir.",
                "The work makes an unfavorable temperament claim about small eyes. It is not valid evidence for judging anyone.",
                "Das Werk stellt kleine Augen mit ungünstigen Charakterzügen in Verbindung; das ist kein gültiger Beleg.",
                "La obra hace una afirmación desfavorable sobre ojos pequeños; no es prueba válida para juzgar a alguien.",
                "تذكر المخطوطة ادعاءً سلبيًا عن العينين الصغيرتين، ولا يعد دليلًا صالحًا للحكم على الناس."
            ))
    )),
    HistoricalGroup("nose", words("Burun görünümü","Nose appearance","Nasenform","Aspecto de la nariz","مظهر الأنف"), listOf(
        HistoricalChoice("nose_long", words("Uzun burun","Long nose","Lange Nase","Nariz larga","أنف طويل"),
            words(
                "Eski metin, uzun burun için anlayışla ilgili olumsuz bir özellik ileri sürer. Bu bilimsel bir zekâ ölçümü değildir.",
                "The historical text attributes a negative understanding-related trait to a long nose. This is not an intelligence measure.",
                "Der alte Text behauptet bei langer Nase eine verminderte Auffassung; das misst keine Intelligenz.",
                "El texto antiguo atribuye a la nariz larga una menor comprensión; no es una medición de inteligencia.",
                "ينسب النص القديم للأنف الطويل ضعفًا في الفهم، وهذا ليس قياسًا علميًا للذكاء."
            )),
        HistoricalChoice("nose_short", words("Kısa burun","Short nose","Kurze Nase","Nariz corta","أنف قصير"),
            words(
                "Tarihî kıyâfet anlatımında kısa burun, korku ve çekingenlikle ilişkilendirilmiştir.",
                "Historical physiognomy associates a short nose with fearfulness.",
                "Die historische Physiognomik verbindet eine kurze Nase mit Ängstlichkeit.",
                "La fisiognomía histórica asocia la nariz corta con el temor.",
                "تربط القيافة التاريخية الأنف القصير بالخوف."
            ))
    )),
    HistoricalGroup("forehead", words("Alın görünümü","Forehead appearance","Stirnform","Aspecto de la frente","مظهر الجبهة"), listOf(
        HistoricalChoice("forehead_medium", words("Dengeli alın","Proportionate forehead","Ausgewogene Stirn","Frente proporcionada","جبهة متناسقة"),
            words(
                "Mârifetnâme'de orta ve dengeli alın, güvenilirlik kavramıyla ilişkilendirilen tarihî bir ifadedir.",
                "In Marifetname a balanced forehead is historically linked with trustworthiness.",
                "In der Mârifetnâme wird eine ausgewogene Stirn historisch mit Vertrauenswürdigkeit verbunden.",
                "En Marifetname se relaciona históricamente una frente proporcionada con la confianza.",
                "يربط كتاب معرفتنامه تاريخيًا الجبهة المتناسقة بالأمانة."
            )),
        HistoricalChoice("forehead_narrow", words("Dar alın","Narrow forehead","Schmale Stirn","Frente estrecha","جبهة ضيقة"),
            words(
                "Eserde dar alın, dar huylulukla ilişkilendirilir; bu kişi hakkında doğrulanmış bir çıkarım değildir.",
                "The text links a narrow forehead with a restricted temperament; this is not a validated personal inference.",
                "Der Text deutet eine schmale Stirn als enges Gemüt; das ist keine wissenschaftliche Aussage.",
                "El texto vincula la frente estrecha con un carácter limitado; no es una inferencia validada.",
                "يربط النص الجبهة الضيقة بضيق الطبع، وليس ذلك استنتاجًا علميًا."
            ))
    )),
    HistoricalGroup("mouth", words("Ağız görünümü","Mouth appearance","Mundform","Aspecto de la boca","مظهر الفم"), listOf(
        HistoricalChoice("mouth_big", words("Büyük ağız","Large mouth","Großer Mund","Boca grande","فم كبير"),
            words(
                "Metinde büyük ağız için cesaretle bağlantı kuran tarihî bir ifade bulunur.",
                "The historical text associates a large mouth with courage.",
                "Der historische Text verbindet einen großen Mund mit Mut.",
                "El texto histórico relaciona la boca grande con valentía.",
                "يربط النص التاريخي الفم الكبير بالشجاعة."
            )),
        HistoricalChoice("mouth_small", words("Küçük ağız","Small mouth","Kleiner Mund","Boca pequeña","فم صغير"),
            words(
                "Metinde küçük ağız, çekingenlikle ilişkilendirilen tarihî bir yorum olarak geçer.",
                "The text historically associates a small mouth with timidity.",
                "Der Text verbindet einen kleinen Mund historisch mit Schüchternheit.",
                "El texto relaciona históricamente la boca pequeña con timidez.",
                "يربط النص تاريخيًا الفم الصغير بالخجل."
            ))
    ))
)

@Composable
fun MarifetnameMatching(lang: MizacLang, strings: Map<String, String>) {
    val context = LocalContext.current
    var showChoices by rememberSaveable { mutableStateOf(false) }
    val selected = remember { mutableStateOf<Map<String,String>>(emptyMap()) }
    Card(
        colors = CardDefaults.cardColors(containerColor=Color(0xFFFFFBF6)),
        shape=RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(ui(strings,"historyMatchIntro"),
                style=MaterialTheme.typography.bodyMedium,color=Color(0xFF493B2F))
            TextButton(onClick={showChoices=!showChoices}) {
                Text(ui(strings, if(showChoices)"historyMatchHide" else "historyMatchOpen"))
            }
            if(showChoices) {
                matchingGroups.forEach { group ->
                    var expanded by remember { mutableStateOf(false) }
                    val choice=group.choices.firstOrNull{it.id==selected.value[group.id]}
                    Row(
                        horizontalArrangement=Arrangement.spacedBy(6.dp),
                        modifier=Modifier.fillMaxWidth()
                    ){
                        Text(group.name(lang),modifier=Modifier.weight(1f),
                            color=Color(0xFF1E1711),fontWeight=FontWeight.SemiBold)
                        TextButton(onClick={expanded=true}) {
                            Text(choice?.name(lang) ?: ui(strings,"historyMatchChoose"))
                        }
                        DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
                            group.choices.forEach { item ->
                                DropdownMenuItem(
                                    text={Text(item.name(lang))},
                                    onClick={
                                        selected.value=selected.value+(group.id to item.id)
                                        expanded=false
                                    }
                                )
                            }
                        }
                    }
                    if(choice!=null) {
                        Text(choice.explanation(lang),
                            style=MaterialTheme.typography.bodySmall,
                            color=Color(0xFF605344))
                    }
                }
                Text(ui(strings,"historyMatchSelectedNotAuto"),
                    style=MaterialTheme.typography.bodySmall,color=Color(0xFF605344))
                TextButton(onClick={
                    runCatching {
                        context.startActivity(Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://tr.wikisource.org/wiki/Marifetname/42")
                        ))
                    }
                }) {
                    Text(ui(strings,"historyMatchSource"))
                }
            }
        }
    }
}
