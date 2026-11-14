package com.rasaser.free.mizac

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Offline content integrity tests. These inspect checked-in assets rather than
 * screenshots or personal data. They do not claim device camera testing.
 */
class ContentIntegrityTest {
    private val root = File("src/main/assets")

    private fun lines(path: String): List<String> {
        val asset = File(root, path)
        assertTrue("Missing asset: $path", asset.isFile)
        return asset.readLines(Charsets.UTF_8)
            .filter { it.isNotBlank() && !it.startsWith("#") }
    }

    @Test fun allMarifetnameRecordsAreBundled() {
        val counts = linkedMapOf(
            "history/catalog_4.tsv" to 103,
            "history/catalog_5.tsv" to 54,
            "history/catalog_6.tsv" to 11,
            "history/catalog_7.tsv" to 5,
            "history/catalog_8.tsv" to 56,
            "history/catalog_mizac.tsv" to 19
        )
        var total = 0
        for ((asset, expected) in counts) {
            val records = lines(asset)
            assertEquals("Wrong count for $asset", expected, records.size)
            val fields = if (asset.endsWith("catalog_8.tsv")) 5 else 4
            records.forEachIndexed { i, line ->
                val parts = line.split('\t')
                assertEquals("Bad row $i in $asset", fields, parts.size)
                assertTrue("Empty field in $asset row $i", parts.all { it.isNotBlank() })
            }
            total += records.size
        }
        assertEquals(248, total)
    }

    @Test fun marifetnameQuickReportPassagesExistExactlyOnce() {
        val features = lines("history/catalog_4.tsv")
            .map { it.split('\t') }
            .map { it[2] }
        listOf(
            "Tebessümlü yüz",
            "Yuvarlak, güleç yüz",
            "Güleç yüz ve tatlı söz"
        ).forEach { name ->
            assertEquals("Source missing or repeated: $name", 1, features.count { it == name })
        }
    }

    @Test fun allOneHundredSeventySixEthicsItemsArePresentAndOrdered() {
        val items = lines("ethics/quran_176.tsv").map { it.split('\t') }
        assertEquals(176, items.size)
        for ((kind, offset) in listOf("+" to 0, "-" to 88)) {
            val group = items.drop(offset).take(88)
            assertEquals(88, group.size)
            group.forEachIndexed { i, parts ->
                assertEquals("Bad source columns", 5, parts.size)
                assertEquals("Wrong section", kind, parts[0])
                assertEquals("Wrong order", i + 1, parts[1].toInt())
                assertTrue("Empty feature title", parts[3].isNotBlank())
                assertTrue("Missing verse attribution", parts[4].isNotBlank())
            }
        }
    }

    @Test fun allOriginalQuestionSetsHaveTwentyFourFiveLanguageItems() {
        val factors = listOf("O", "C", "E", "A", "S")
        var total = 0
        factors.forEach { factor ->
            val questions = lines("questionbank/$factor.txt")
            assertEquals("Not 24 questions for $factor", 24, questions.size)
            questions.forEachIndexed { index, row ->
                val parts = row.split('|')
                assertEquals("Incorrect language columns at $factor/$index", 6, parts.size)
                assertTrue("Missing translation at $factor/$index", parts.all { it.isNotBlank() })
                assertTrue("Incorrect reverse flag", parts[0] in listOf("0", "1"))
            }
            total += questions.size
        }
        assertEquals(120, total)
        assertEquals(10, factors.size * 2)
        assertEquals(50, factors.size * 10)
        assertEquals(120, factors.size * 24)
    }

    @Test fun localizedUiKeysHaveAllFiveLanguagesWithoutDuplicates() {
        val rows = lines("i18n/ui.txt")
        assertTrue("Expected substantial UI translations", rows.size > 200)
        val keys = mutableSetOf<String>()
        rows.forEachIndexed { i, row ->
            val parts = row.split('|')
            assertEquals("Incomplete UI translation row $i", 6, parts.size)
            assertTrue("Blank translation at row $i", parts.all { it.isNotBlank() })
            assertTrue("Repeated translation key ${parts[0]}", keys.add(parts[0]))
        }
        assertFalse("Quick result labels missing", keys.intersect(
            setOf("quickMeasuredFacts", "quickBookReport", "quickBookSourceItems")
        ).isEmpty())
    }

    @Test fun allEightyEightGrowthTipsAreIndividualAndSourceIndexed() {
        val rows = lines("ethics/growth_actions_tr.tsv").map { it.split('\t',limit=2) }
        assertEquals(88,rows.size)
        assertEquals((1..88).toList(), rows.map {it[0].toInt()})
        assertEquals(88,rows.map {it[1]}.distinct().size)
        assertTrue(rows.all{ it[1].length>=24 })
    }

    @Test fun quickGrowthIsDerivedOnlyFromActualHighSelfRatings() {
        val allAdvice = lines("ethics/growth_actions_tr.tsv").associate {line->
            val p=line.split('\t',limit=2)
            p[0].toInt() to p[1]
        }
        val answers = MutableList(88){ 1 }
        answers[16] = 5 // Yalan söylemek
        answers[40] = 4 // Öfkeyle saldırgan davranmak
        val selected = selectSelfReportedConcerns(answers,allAdvice)
        assertEquals(2,selected.size)
        assertEquals(listOf(17,41),selected.map{it.itemNo})
        assertEquals(listOf(5,4),selected.map{it.score})
        assertTrue(selected.all{it.action==allAdvice[it.itemNo]})
        assertTrue(selectSelfReportedConcerns(List(88){1},allAdvice).isEmpty())
    }
}
