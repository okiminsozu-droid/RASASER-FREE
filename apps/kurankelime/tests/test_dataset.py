from pathlib import Path
import json
import unittest

DATA = Path(__file__).resolve().parents[1] / "app/src/main/assets/seed/fatiha_preview.json"

class ContentIntegrity(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.root = json.loads(DATA.read_text(encoding="utf-8"))

    def test_seven_fatiha_verses(self):
        self.assertEqual(self.root["sura"], 1)
        self.assertEqual([v["ayah"] for v in self.root["verses"]], list(range(1, 8)))
        self.assertEqual(sum(len(v["words"]) for v in self.root["verses"]), 29)

    def test_position_unique_and_all_five_meanings_present(self):
        for verse in self.root["verses"]:
            self.assertEqual([w["position"] for w in verse["words"]], list(range(1, len(verse["words"]) + 1)))
            self.assertEqual(verse["arabic"], " ".join(w["arabic"] for w in verse["words"]))
            for w in verse["words"]:
                for language in ("tr", "en", "de", "es", "zh"):
                    self.assertTrue(w[language].strip(), (verse["ayah"], w["position"], language))
                self.assertTrue(w["arabic"].strip())

    def test_correct_verse_boundaries_in_demo(self):
        first = self.root["verses"][0]["words"]
        self.assertEqual(first[0]["arabic"], "بِسْمِ")
        self.assertEqual(first[0]["tr"], "Adıyla")
        self.assertEqual(first[1]["arabic"], "اللَّهِ")
        self.assertEqual(first[2]["arabic"], "الرَّحْمَٰنِ")
        self.assertEqual(first[3]["arabic"], "الرَّحِيمِ")
        self.assertEqual(self.root["verses"][1]["words"][0]["arabic"], "الْحَمْدُ")

    def test_review_gate_not_falsely_marked_done(self):
        self.assertEqual(self.root["status"], "linguistic-review-required")

if __name__ == "__main__":
    unittest.main()
