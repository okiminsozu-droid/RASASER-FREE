"""Exactly three user-requested UI changes; no alteration to Quran data or reader logic."""
import unittest
from pathlib import Path

APP = Path(__file__).resolve().parents[1]
CODE = APP / "app/src/main/java/com/rasaser/free/kurankelime"

class ReaderUiAndLogoRegression(unittest.TestCase):
    def read(self, name):
        return (CODE / name).read_text(encoding="utf-8")

    def test_reader_uses_home_screen_language_selection(self):
        reader = self.read("StableReaderActivity.kt")
        data = self.read("ReaderUiText.kt")
        self.assertIn('getSharedPreferences("discovery_home", Context.MODE_PRIVATE)', data)
        self.assertIn('getString("lang", "tr")', data)
        for tag in ("en","de","es","ar","ja","zh"):
            self.assertIn(f'"{tag}" -> ReaderUiText(', data)
        self.assertIn("bottom(ui.fit)", reader)
        self.assertIn("bottom(ui.share)", reader)
        self.assertIn("bottom(ui.hide)", reader)
        self.assertIn("selectionAction(R.drawable.ic_word_copy,ui.copy)", reader)
        self.assertIn("selectionAction(R.drawable.ic_word_translate,ui.translate)", reader)
        self.assertIn("setTitle(ui.choosePage)", reader)
        self.assertIn(".setPositiveButton(ui.go)", reader)
        self.assertIn("ui.sharePage", reader)
        self.assertIn('pageFooter.text=ui.page+', reader)
        self.assertIn("translation.translate(selected,page,selectedRangeStart,selectedRangeEnd)", reader)

    def test_dialog_actions_match_language(self):
        translator = self.read("OnDeviceWordTranslator.kt")
        self.assertIn("ReaderUiText.load(activity)", translator)
        self.assertIn(".setPositiveButton(readerUi.copy, null)", translator)
        self.assertIn(".setNegativeButton(readerUi.closeDialog, null)", translator)
        self.assertIn("client.translate(selectedText)", translator)

    def test_home_top_right_logo_links_to_site_but_image_is_unchanged(self):
        home = self.read("DiscoverHomeActivity.kt")
        self.assertIn('context.assets.open("brand/logo.webp")', home)
        self.assertIn('modifier=Modifier.size(68.dp)', home)
        self.assertIn('contentDescription="www.okiminsozu.com"', home)
        self.assertIn('Text("www.okiminsozu.com", fontSize=9.sp', home)
        self.assertIn('Uri.parse("https://www.okiminsozu.com")', home)
        self.assertIn('modifier=Modifier.clickable {', home)

    def test_remove_only_search_ocr_warning_without_disabling_search(self):
        home = self.read("DiscoverHomeActivity.kt")
        self.assertNotIn("OCR sonuçları doğrulama gerektirir", home)
        self.assertIn('bundledReady -> ""', home)
        self.assertIn("if (searchMessage.isNotBlank())", home)
        self.assertIn("bundledReady -> bundledIndex.search(query)", home)
        self.assertIn("fullHits = fullHits.filter", home)

if __name__ == "__main__":
    unittest.main()
