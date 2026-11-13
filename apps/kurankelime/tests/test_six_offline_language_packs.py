"""Regression contract for the optional ML Kit offline models on the preserved 0.3.4 reader."""
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "app/src/main/java/com/rasaser/free/kurankelime"


class SixOfflineLanguagePacksTest(unittest.TestCase):
    def test_downloads_only_five_remote_models_with_builtin_english(self):
        src = (SRC / "OfflineLanguagePacks.kt").read_text(encoding="utf-8")
        for language in ("ar", "tr", "de", "es", "ja"):
            self.assertIn(f'Pack("{language}",', src)
        self.assertNotIn('Pack("en",', src)
        self.assertIn('setOf("en")', src)
        self.assertIn('getDownloadedModels', src)
        self.assertIn('requireWifi()', src)
        self.assertIn('downloadAll(current)', src)
        self.assertIn('if (pack.code in installed)', src)

    def test_one_tap_button_below_homepage_language_menu(self):
        src = (SRC / "DiscoverHomeActivity.kt").read_text(encoding="utf-8")
        self.assertIn("Çevrimdışı dil paketlerini indir", src)
        self.assertIn('showOfflinePacks = true', src)
        self.assertIn('OfflineLanguagePacksDialog', src)
        self.assertIn('"ja" to "🇯🇵 日本語"', src)
        self.assertIn('"zh" to "🇨🇳 中文"', src)

    def test_translation_respects_existing_reader_and_downloaded_models(self):
        src = (SRC / "OnDeviceWordTranslator.kt").read_text(encoding="utf-8")
        self.assertIn('"ja" to "日本語"', src)
        self.assertIn("OfflineLanguagePacks.pairReady(source, target)", src)
        self.assertIn("performGoogle(sourceTag, targetTag)", src)
        self.assertIn('glossary.find(readerPage, firstOcrId, source, target, selectedText)', src)

    def test_does_not_ship_private_assets(self):
        assets = ROOT / "app/src/main/assets/quran"
        for name in ("kelime_mealli.pdf", "verified_word_glosses.sqlite"):
            self.assertFalse((assets / name).exists(), name + " must not enter public GitHub")


if __name__ == "__main__":
    unittest.main()
