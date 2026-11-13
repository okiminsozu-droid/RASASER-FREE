"""Source-level guardrails for the isolated, offline Quran reader translation UI."""
from pathlib import Path
import unittest

APP = Path(__file__).resolve().parents[1]
CODE = APP / "app/src/main/java/com/rasaser/free/kurankelime"
RES = APP / "app/src/main/res/drawable"

class TestReaderTranslationContract(unittest.TestCase):
    def test_selection_icons_are_real_vector_resources(self):
        reader = (CODE / "StableReaderActivity.kt").read_text(encoding="utf-8")
        self.assertIn("R.drawable.ic_word_copy", reader)
        self.assertIn("R.drawable.ic_word_translate", reader)
        self.assertIn("translation.translate(selected,page,selectedRangeStart,selectedRangeEnd)", reader)
        for name in ("ic_word_copy.xml", "ic_word_translate.xml"):
            self.assertIn("<vector", (RES / name).read_text(encoding="utf-8"))

    def test_translation_stays_local_and_separates_meaning_sources(self):
        translator = (CODE / "OnDeviceWordTranslator.kt").read_text(encoding="utf-8")
        self.assertIn('getSharedPreferences("discovery_home"', translator)
        self.assertIn("TranslateLanguage.fromLanguageTag", translator)
        self.assertIn("downloadModelIfNeeded", translator)
        self.assertIn("dialog.setMessage(translatedWord() ?: status)", translator)
        self.assertNotIn("setPositiveButton(\"Google çeviri\"", translator)
        self.assertNotIn("append(\"Seçilen", translator)
        self.assertIn("setPositiveButton(\"Kopyala\"", translator)
        self.assertIn("setNegativeButton(\"Kapat\"", translator)
        self.assertIn("Doğrulanmış Kur’an kelime anlamı", translator)
        self.assertNotIn("cloudtranslate.googleapis.com", translator)
        self.assertNotIn("translate.googleapis.com", translator)
        self.assertIn("if (result == null && fromPrintedPdf == null && localHint == null &&", translator)
        self.assertIn("source != target) requestGoogle()", translator)
        self.assertIn("OfflineWordMeaning.find(selectedText, source, target)", translator)

    def test_preview_never_claims_ocr_is_verified(self):
        glossary = (CODE / "ReviewedQuranGlossary.kt").read_text(encoding="utf-8")
        self.assertIn("review_status='approved'", glossary)
        self.assertIn("canonical(original) != canonical(selectedText)", glossary)
        self.assertIn("hash != sourceSha", glossary)
        self.assertFalse(
            (APP / "app/src/main/assets/quran/verified_word_glosses.sqlite").exists(),
            "Public app must not accidentally ship unlicensed or unreviewed glosses",
        )

if __name__ == "__main__":
    unittest.main()
