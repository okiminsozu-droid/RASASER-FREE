"""Checks only the private-content assembler format, not any copyrighted Quran data."""
import json
from pathlib import Path
import sqlite3
import sys
import tempfile
import unittest
import zipfile
from contextlib import closing

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "tools"))
from build_private_apk import PDF_SHA, verify_pages, verify_selection


class PrivateAssemblyTests(unittest.TestCase):
    def make_zip(self, folder, attack=False):
        path = Path(folder) / "test.zip"
        pages = []
        with zipfile.ZipFile(path, "w", zipfile.ZIP_DEFLATED) as z:
            for n in range(1,606):
                pages.append({
                    "n": n, "source_pdf_page": min(n, 304),
                    "dimensions": [600, 800],
                    "segments": [{
                        "panel_index": 0,
                        "source_bbox": [0, 0, 100, 100],
                        "source_pixels": [300, 400],
                        "output_origin": [0, 0]
                    }]
                })
                z.writestr(f"quran/single_pages/page_{n:04}.webp",
                           b"RIFF\x04\x00\x00\x00WEBPTEST")
            z.writestr("quran/single_pages/pages.json",
                       json.dumps({"page_count":605, "pages":pages}))
            if attack:
                z.writestr("../../keystore.jks", "invalid entry")
        return path

    def test_all_pages(self):
        with tempfile.TemporaryDirectory() as d:
            verify_pages(self.make_zip(d))

    def test_rejects_path_traversal(self):
        with tempfile.TemporaryDirectory() as d:
            with self.assertRaisesRegex(ValueError, "İzin verilmeyen ZIP yolu"):
                verify_pages(self.make_zip(d, attack=True))

    def test_selection_database_must_match_source(self):
        with tempfile.TemporaryDirectory() as d:
            path = Path(d) / "test.sqlite"
            with closing(sqlite3.connect(path)) as db:
                db.executescript(
                    "CREATE TABLE page_info(page INTEGER); "
                    "CREATE TABLE meta(key TEXT,value TEXT); "
                    "CREATE TABLE words(id INTEGER);"
                )
                db.executemany("INSERT INTO page_info VALUES (?)",
                               [(n,) for n in range(605)])
                db.executemany("INSERT INTO words VALUES (?)",
                               [(n,) for n in range(60000)])
                db.execute("INSERT INTO meta VALUES (?,?)", ("source_pdf_sha256", PDF_SHA))
                db.commit()
            verify_selection(path)
            with closing(sqlite3.connect(path)) as db:
                db.execute("UPDATE meta SET value='tampered'")
                db.commit()
            with self.assertRaises(ValueError):
                verify_selection(path)


if __name__ == "__main__":
    unittest.main()
