package com.rasaser.free.kurankelime

import java.text.Normalizer
import java.util.Locale

/**
 * Small, locally reviewed GENERAL GRAMMAR aid for ambiguous Turkish OCR glosses.
 *
 * NOT a Quran word-by-word translation corpus, a tafsir, or a verified alignment.
 * The word "ondan" may mean "from him", "from her" or "from it"; ML Kit's
 * one-token output "It" loses the Turkish ablative suffix. Show ambiguity instead.
 *
 * All other words still use the user's downloaded ML Kit models, on device.
 */
internal object OfflineWordMeaning {
    data class Hint(val text: String, val needsContext: Boolean = true)

    private fun variants(
        en: String, de: String, es: String, ja: String, ar: String, zh: String
    ) = mapOf("en" to en, "de" to de, "es" to es, "ja" to ja, "ar" to ar, "zh" to zh)

    private val possibilities = mapOf(
        "ondan" to variants(
            "from him / her / it", "von ihm / ihr / daraus",
            "de él / ella / eso", "彼から／彼女から／そこから",
            "منه / منها", "从他／她／它那里"
        ),
        "ona" to variants(
            "to him / her / it", "zu ihm / zu ihr / dazu",
            "a él / a ella / a eso", "彼に／彼女に／それに",
            "إليه / إليها", "给他／她／它"
        ),
        "onu" to variants(
            "him / her / it (object)", "ihn / sie / es",
            "lo / la / ello", "彼を／彼女を／それを",
            "إياه / إياها", "他／她／它（宾语）"
        ),
        "onun" to variants(
            "his / her / its; of him / her / it",
            "sein / ihr / dessen", "su / de él / de ella",
            "彼の／彼女の／それの", "له / لها", "他的／她的／它的"
        ),
        "onunla" to variants(
            "with him / her / it", "mit ihm / ihr / damit",
            "con él / ella / eso", "彼と／彼女と／それと",
            "معه / معها", "和他／她／它一起"
        ),
        "onlar" to variants(
            "they / those", "sie / jene", "ellos / ellas / aquellos",
            "彼ら／それら", "هم / هن / أولئك", "他们／那些"
        ),
        "onlara" to variants(
            "to them", "ihnen / zu ihnen", "a ellos / a ellas",
            "彼らに／それらに", "إليهم / إليهن", "给他们／她们"
        ),
        "onlardan" to variants(
            "from them / among them", "von ihnen / unter ihnen",
            "de ellos / entre ellos", "彼らから／彼らの中から",
            "منهم / منهن", "从他们／她们当中"
        ),
        "bundan" to variants(
            "from this / because of this", "davon / deswegen",
            "de esto / por esto", "これから／このため",
            "من هذا / بسبب هذا", "从这个／因此"
        ),
        "buna" to variants(
            "to this / to it", "dazu / diesem", "a esto",
            "これに", "إلى هذا / لهذا", "对此／给这个"
        ),
        "bunu" to variants(
            "this / it (object)", "dies / das", "esto / lo",
            "これを", "هذا / إياه", "这个／它"
        ),
        "bunun" to variants(
            "of this / its", "dessen / davon", "de esto / su",
            "これの", "لهذا / منه", "这个的／它的"
        ),
        "bunlar" to variants(
            "these", "diese", "estos / estas",
            "これら", "هؤلاء / هذه", "这些"
        ),
        "bunlardan" to variants(
            "from these / among these", "von diesen / unter diesen",
            "de estos / entre estos", "これらから／これらの中から",
            "من هؤلاء / من هذه", "从这些当中"
        ),
        "bizden" to variants(
            "from us", "von uns", "de nosotros",
            "私たちから", "منا", "从我们这里"
        ),
        "sizden" to variants(
            "from you", "von euch / von Ihnen", "de ustedes / de vosotros",
            "あなたたちから", "منكم", "从你们那里"
        )
    )

    fun find(text: String, sourceLanguage: String, targetLanguage: String): Hint? {
        if (sourceLanguage != "tr" || targetLanguage == "tr") return null
        val word = Normalizer.normalize(text, Normalizer.Form.NFC)
            .trim().trim { !it.isLetterOrDigit() }.lowercase(Locale.forLanguageTag("tr-TR"))
        if (word.isBlank() || word.any { it.isWhitespace() }) return null
        return possibilities[word]?.get(targetLanguage)?.let(::Hint)
    }
}
