"""Search regression: raw OCR tabular records must not appear in result cards."""
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CODE = ROOT / "app/src/main/java/com/rasaser/free/kurankelime"

class SearchResultCleanliness(unittest.TestCase):
    def test_database_query_rejects_raw_ocr_tsv_blocks(self):
        code = (CODE / "BundledWordIndex.kt").read_text(encoding="utf-8")
        self.assertIn("instr(t.token,char(9))=0", code)
        self.assertIn("instr(t.token,char(10))=0", code)
        self.assertIn("instr(t.token,char(13))=0", code)
        self.assertIn("length(t.token) BETWEEN 1 AND 64", code)
        self.assertIn("isCleanSearchWord(word.gloss)", code)

    def test_result_cards_guard_all_providers(self):
        code = (CODE / "DiscoverHomeActivity.kt").read_text(encoding="utf-8")
        self.assertIn("fullHits = fullHits.filter { BundledWordIndex.isCleanSearchWord(it.gloss) }", code)
        self.assertIn("Sayfa", code)

    def test_safe_side_by_side_variant(self):
        code = (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8")
        self.assertIn('versionName = "0.3.13"', code)
        self.assertIn('applicationIdSuffix = ".ui312"', code)

if __name__ == "__main__":
    unittest.main()
