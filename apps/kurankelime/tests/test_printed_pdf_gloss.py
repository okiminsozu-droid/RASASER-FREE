"""Regression: visually printed Turkish below Arabic must be used before ML Kit.

The fixture models page 5: كفروا => 'Kafir oldular' in the printed PDF.
These are OCR-derived printed words, not a certified Quran translation.
"""
import sqlite3
import unittest
from pathlib import Path

APP = Path(__file__).resolve().parents[1] / "app/src/main/java/com/rasaser/free/kurankelime"

QUERY = (
    "SELECT word,x,y,confidence FROM words WHERE page=? AND lang='tur' "
    "AND confidence>=62 AND y>=CAST(? AS REAL) AND y<=CAST(? AS REAL) "
    "AND (x+w*0.5)>=CAST(? AS REAL) "
    "AND (x+w*0.5)<=CAST(? AS REAL) ORDER BY y,x LIMIT 24"
)


class PrintedGlossTests(unittest.TestCase):
    def setUp(self):
        self.db = sqlite3.connect(":memory:")
        self.db.execute(
            "CREATE TABLE words(page INTEGER,ocr_id INTEGER,lang TEXT,"
            "word TEXT,x REAL,y REAL,w REAL,h REAL,confidence REAL)"
        )
        rows = [
            (5, 356, 'ara', 'كفروا', .3564, .4078, .1182, .0175, 47.5),
            (5, 280, 'tur', 'Kafir', .353, .433, .047, .009, 96.9),
            (5, 281, 'tur', 'oldular', .409, .433, .071, .009, 95.9),
            (5, 283, 'tur', 'Rableri', .698, .433, .071, .009, 81.5),
            (5, 513, 'ara', 'وَيَقطعون', .5135, .6052, .1351, .0189, 79.0),
            (5, 402, 'tur', 'Ve', .522, .630, .025, .009, 74.9),
            (5, 403, 'tur', 'keserler', .556, .630, .083, .009, 74.9),
            (5, 409, 'tur', 'ki', .503, .641, .014, .009, 85.2),
        ]
        self.db.executemany("INSERT INTO words VALUES (?,?,?,?,?,?,?,?,?)",rows)

    def tearDown(self):
        self.db.close()

    def local(self, page, ocr_id):
        src = self.db.execute(
            "SELECT x,y,w,h,word FROM words WHERE page=? AND ocr_id=? AND lang='ara'",
            (page, ocr_id)
        ).fetchone()
        if not src:
            return None
        x, y, w, h, ar = src
        candidates = self.db.execute(
            QUERY,(str(page),str(y+h-.005),str(y+h+.054),
                   str(x-.001),str(x+w+.001))
        ).fetchall()
        if not candidates:
            return None
        first_y = candidates[0][2]
        chosen = [a for a in candidates if a[2] <= first_y+.019]
        chosen.sort(key=lambda a:(int((a[2]-first_y)/.012),a[1]))
        return " ".join(a[0] for a in chosen)

    def test_printed_turkish_on_page_five(self):
        self.assertEqual(self.local(5,356), "Kafir oldular")

    def test_second_word_below_without_neighbour(self):
        self.assertEqual(self.local(5,513), "Ve keserler")

    def test_source_and_other_page_not_mixed(self):
        self.assertIsNone(self.local(4,356))
        self.assertIsNone(self.local(5,999))

    def test_android_source_wired_to_local_gloss_first(self):
        lookup = (APP/"SelectableTextLayer.kt").read_text()
        translator = (APP/"OnDeviceWordTranslator.kt").read_text()
        reader = (APP/"StableReaderActivity.kt").read_text()
        self.assertIn("fun printedTurkishBelow(",lookup)
        self.assertIn('source == "ar" && target == "tr"',translator)
        self.assertIn('val fromPdf =',translator)
        self.assertIn('printed = fromPdf',translator)
        self.assertNotIn('if (result == null) requestGoogle()',translator)
        self.assertIn('OnDeviceWordTranslator(this, selectableLayer)',reader)
        self.assertIn('printed?.text',translator)


if __name__ == "__main__":
    unittest.main()
