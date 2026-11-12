"""Regression: Android SQLite rawQuery binds strings; sums of columns have no numeric affinity.
An uncast text bind makes (x+w)>=? false for every real-valued word box.
"""
import sqlite3
import unittest
from pathlib import Path

JAVA = Path(__file__).resolve().parents[1] / "app/src/main/java/com/rasaser/free/kurankelime"


class TouchSelectionSqlTests(unittest.TestCase):
    def setUp(self):
        self.db = sqlite3.connect(":memory:")
        self.db.execute(
            "CREATE TABLE words (page INTEGER, x REAL, y REAL, "
            "w REAL, h REAL, lang TEXT, word TEXT)"
        )
        self.db.execute(
            "INSERT INTO words VALUES (3, 0.40, 0.25, 0.08, 0.03, 'tur', 'deneme')"
        )

    def tearDown(self):
        self.db.close()

    def test_selects_word_at_its_center_with_android_text_arguments(self):
        x, y = 0.44, 0.265
        args = ["3", str(x + 0.03), str(x - 0.03),
                str(y + 0.015), str(y - 0.015)]
        # The exact query used by the 605-page selection layer.
        sql = ("SELECT word FROM words WHERE page=? AND x<=? "
               "AND (x+w)>=CAST(? AS REAL) AND y<=? "
               "AND (y+h)>=CAST(? AS REAL)")
        self.assertEqual(
            self.db.execute(sql, args).fetchone()[0], "deneme"
        )

    def test_uncast_expression_would_silently_return_nothing(self):
        x, y = 0.44, 0.265
        args = ["3", str(x + 0.03), str(x - 0.03),
                str(y + 0.015), str(y - 0.015)]
        broken = ("SELECT word FROM words WHERE page=? AND x<=? "
                  "AND (x+w)>=? AND y<=? AND (y+h)>=?")
        self.assertIsNone(self.db.execute(broken, args).fetchone())

    def test_android_reader_sql_uses_numeric_cast_for_both_bounds(self):
        for source in ["SelectableTextLayer.kt", "BundledWordIndex.kt"]:
            contents = (JAVA / source).read_text(encoding="utf-8")
            self.assertIn("(x+w)>=CAST(? AS REAL)", contents)
            self.assertIn("(y+h)>=CAST(? AS REAL)", contents)
            self.assertNotIn("(x+w)>=?", contents)
            self.assertNotIn("(y+h)>=?", contents)


if __name__ == "__main__":
    unittest.main()
