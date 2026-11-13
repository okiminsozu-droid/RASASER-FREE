"""Read-only regression checks: Quran OCR selection vs contextual language hints.

Run: python3 -m unittest discover -s apps/kurankelime/tests -v
"""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
KOTLIN = ROOT / "app/src/main/java/com/rasaser/free/kurankelime"


class QuranContextualTranslationTest(unittest.TestCase):
    def source(self, name):
        return (KOTLIN / name).read_text(encoding="utf-8")

    def test_turkish_ablative_is_not_collapsed_to_it(self):
        src = self.source("OfflineWordMeaning.kt")
        self.assertIn('"ondan" to variants(', src)
        self.assertIn('"from him / her / it"', src)
        self.assertIn('Locale.forLanguageTag("tr-TR")', src)
        self.assertIn('sourceLanguage != "tr"', src)
        self.assertNotIn('"ondan" to variants(\n            "It"', src)

    def test_ambiguous_words_are_a_grammar_aid_not_quran_meal(self):
        src = self.source("OfflineWordMeaning.kt")
        self.assertIn("NOT a Quran word-by-word translation corpus", src)
        for language in ("en", "de", "es", "ja", "ar", "zh"):
            self.assertIn(f'"{language}" to ', src)

    def test_reader_preserves_selected_language_and_identical_page(self):
        reader = self.source("StableReaderActivity.kt")
        self.assertIn("OnDeviceWordTranslator(this, selectableLayer)", reader)
        self.assertIn("translation.translate(selected,page,selectedRangeStart,selectedRangeEnd)", reader)
        self.assertIn("val hit=try { selectableLayer.pick(current,x,y)", reader)

    def test_translator_precedence_is_checked_before_machine_translation(self):
        src = self.source("OnDeviceWordTranslator.kt")
        self.assertIn("val localHint = OfflineWordMeaning.find(selectedText, source, target)", src)
        self.assertIn("printed = fromPrintedPdf", src)
        self.assertIn("printed?.text?.takeIf { it.isNotBlank() }", src)
        self.assertIn("localHint?.text?.takeIf { it.isNotBlank() }", src)
        self.assertIn('source == "ar" && target == "tr"', src)
        self.assertIn("selectableLayer.printedTurkishBelow(readerPage, firstOcrId, selectedText)", src)
        self.assertIn("if (result == null && fromPrintedPdf == null && localHint == null &&", src)
        self.assertIn("fun translatedWord(): String? = reviewed?.meaning?.takeIf", src)
        self.assertIn("machineResult?.takeIf { it.isNotBlank() }", src)
        self.assertIn("dialog.setMessage(translatedWord() ?: status)", src)

    def test_pdf_printed_gloss_never_claimed_as_reviewed(self):
        src = self.source("SelectableTextLayer.kt")
        self.assertIn("data class PrintedTurkishGloss", src)
        self.assertIn("source.word.trim() != selectedArabic.trim()", src)
        self.assertIn("confidence>=62", src)
        self.assertIn("(x+w*0.5)>=CAST(? AS REAL)", src)
        self.assertIn("if (lines.isEmpty() || lines.size > 12)", src)
        self.assertIn("NOT a certified Quran translation", src)

    def test_original_private_content_remains_external_to_public_branch(self):
        assets = ROOT / "app/src/main/assets/quran"
        for name in ("selection_layer.sqlite", "kelime_mealli.pdf"):
            self.assertFalse((assets / name).exists())

if __name__ == "__main__":
    unittest.main()
