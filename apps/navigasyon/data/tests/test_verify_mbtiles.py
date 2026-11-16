"""Yerel ve CI'da internet gerektirmeyen MBTiles yapısal testleri."""
import gzip
import sqlite3
import tempfile
import unittest
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from verify_mbtiles import validate, MapValidationError


class MbtilesTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.path = Path(self.tmp.name) / "small.mbtiles"

    def make(self, fmt="pbf", include_tile=True, contents=b"\x1a\x00"):
        db = sqlite3.connect(self.path)
        db.execute("CREATE TABLE metadata (name TEXT, value TEXT)")
        db.execute("CREATE TABLE tiles (zoom_level INTEGER, tile_column INTEGER, tile_row INTEGER, tile_data BLOB)")
        db.executemany("INSERT INTO metadata VALUES (?,?)",
                       [("format", fmt), ("minzoom", "0"), ("maxzoom", "14")])
        if include_tile:
            db.execute("INSERT INTO tiles VALUES (0, 0, 0, ?)", (gzip.compress(contents),))
        db.commit()
        db.close()

    def test_valid(self):
        self.make()
        result = validate(self.path)
        self.assertEqual(result["format"], "pbf")
        self.assertEqual(result["sample_zoom"], 0)

    def test_missing(self):
        with self.assertRaises(MapValidationError):
            validate(self.path)

    def test_raster_rejected(self):
        self.make("png")
        with self.assertRaises(MapValidationError):
            validate(self.path)

    def test_no_tiles_rejected(self):
        self.make(include_tile=False)
        with self.assertRaises(MapValidationError):
            validate(self.path)

    def test_corrupt_gzip_rejected(self):
        self.make()
        db = sqlite3.connect(self.path)
        db.execute("UPDATE tiles SET tile_data=?", (b"\x1f\x8bgarbage",))
        db.commit()
        db.close()
        with self.assertRaises(MapValidationError):
            validate(self.path)


if __name__ == "__main__":
    unittest.main()
