package com.rasaser.free.mizac

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

data class MizacLang(val code: String, val flag: String, val name: String, val index: Int)

val MIZAC_LANGUAGES = listOf(
    MizacLang("tr", "🇹🇷", "Türkçe", 0),
    MizacLang("en", "🇬🇧", "English", 1),
    MizacLang("de", "🇩🇪", "Deutsch", 2),
    MizacLang("es", "🇪🇸", "Español", 3),
    MizacLang("ar", "🇸🇦", "العربية", 4)
)

fun mizacLang(code: String): MizacLang =
    MIZAC_LANGUAGES.firstOrNull { it.code == code } ?: MIZAC_LANGUAGES[0]

private fun translations(context: Context, path: String): List<List<String>> {
    return context.assets.open(path).bufferedReader(Charsets.UTF_8).useLines { lines ->
        lines.filter { it.isNotBlank() }
            .map { it.trim().split('|') }
            .toList()
    }
}

fun uiDictionary(context: Context, language: MizacLang): Map<String, String> {
    val index = language.index + 1
    val dictionary = mutableMapOf<String, String>()
    translations(context, "i18n/ui.txt").forEach { row ->
        if (row.size == 6 && row[0].isNotBlank()) {
            dictionary[row[0]] = row[index].ifBlank { row[1] }
        }
    }
    return dictionary
}

fun ui(dict: Map<String, String>, key: String): String = dict[key] ?: key

data class OriginalQuestion(
    val category: String,
    val reverse: Boolean,
    val versions: List<String>,
    val id: String
) {
    fun text(language: MizacLang): String = versions[language.index]
}

val MIZAC_FACTORS = listOf("O", "C", "E", "A", "S")

fun loadOriginalQuestions(context: Context): List<OriginalQuestion> =
    MIZAC_FACTORS.flatMap { factor ->
        translations(context, "questionbank/$factor.txt").mapIndexed { idx, cells ->
            require(cells.size == 6 && cells[0] in listOf("0", "1")) {
                "Original question data is incomplete at $factor/${idx + 1}"
            }
            require(cells.drop(1).all { it.isNotBlank() })
            OriginalQuestion(
                factor, cells[0] == "1", cells.drop(1),
                "$factor-${idx + 1}"
            )
        }
    }.also { all ->
        require(all.size == 120)
        require(MIZAC_FACTORS.all { factor -> all.count { it.category == factor } == 24 })
    }

fun selectOriginalQuestions(all: List<OriginalQuestion>, count: Int): List<OriginalQuestion> {
    require(count in listOf(10, 50, 120))
    val perFactor = count / 5
    return (0 until perFactor).flatMap { idx ->
        MIZAC_FACTORS.map { factor ->
            all.first { it.category == factor && it.id == "$factor-${idx + 1}" }
        }
    }
}

data class HistorySection(val title: String, val explanation: String)

fun loadHistory(context: Context, lang: MizacLang): List<HistorySection> {
    val lines = translations(context, "i18n/history.txt")
    require(lines.size % 2 == 0)
    return lines.chunked(2).map { pair ->
        require(pair[0].size == 5 && pair[1].size == 5)
        HistorySection(pair[0][lang.index], pair[1][lang.index])
    }
}

fun loadHistoryReflection(context: Context, lang: MizacLang): List<String> =
    translations(context, "i18n/history_reflection.txt").map { row ->
        require(row.size == 5 && row.all { it.isNotBlank() })
        row[lang.index]
    }.also { require(it.size == 10) }

fun formatLocalNumber(number: Float, lang: MizacLang): String {
    val locale = Locale.forLanguageTag(lang.code)
    return String.format(locale, "%.2f", number)
}
