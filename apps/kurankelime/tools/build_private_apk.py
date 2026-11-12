#!/usr/bin/env python3
"""Locally assemble a TEST APK from the user's privately licensed Quran assets.

Usage:
 python3 tools/build_private_apk.py --pages 605-pages.zip \
     --selection quran_605_selectable_text_layer.sqlite --output complete-trial.apk

This script never uploads assets, never overwrites an existing APK and
never alters any existing installed application or signing keystore.
"""
import argparse
from contextlib import closing
import hashlib
import json
import shutil
import sqlite3
import subprocess
import tempfile
from pathlib import Path
import zipfile

PDF_SHA = "387674c9a3bfb2c501c2dcca4b63946ea05f386dcd4d6fa4b63b4060b01397ea"
PAGE_COUNT = 605
ALLOWED = {"quran/single_pages/pages.json", "quran/single_pages/page_labels.json"}
ALLOWED.update(f"quran/single_pages/page_{n:04}.webp" for n in range(1,606))

def verify_pages(pack: Path):
    if not pack.is_file() or pack.stat().st_size > 350_000_000:
        raise ValueError("605 sayfa ZIP'i eksik veya şüpheli boyutta")
    with zipfile.ZipFile(pack) as z:
        entries = [e for e in z.infolist() if not e.is_dir()]
        names = [e.filename for e in entries]
        if len(entries) > 610 or len(names) != len(set(names)):
            raise ValueError("Paket dosyaları geçersiz veya yineleniyor")
        if sum(e.file_size for e in entries) > 800_000_000:
            raise ValueError("Paket beklenmeyen büyüklükte")
        for entry in entries:
            name = entry.filename
            if (name not in ALLOWED or "\\" in name or ".." in Path(name).parts or
                ((entry.external_attr >> 16) & 0o170000) == 0o120000):
                raise ValueError("İzin verilmeyen ZIP yolu: " + name)
        required = ALLOWED - {"quran/single_pages/page_labels.json"}
        if not required.issubset(names):
            raise ValueError("605 sayfalık içerik eksik")
        manifest = json.loads(z.read("quran/single_pages/pages.json").decode("utf-8"))
        pages = manifest.get("pages", [])
        if manifest.get("page_count") != PAGE_COUNT or len(pages) != PAGE_COUNT:
            raise ValueError("pages.json 605 sayfayı tanımlamıyor")
        for number, page in enumerate(pages, 1):
            if (page.get("n") != number or page.get("source_pdf_page", 0) not in range(1, 305) or
                    len(page.get("dimensions", [])) != 2 or not page.get("segments")):
                raise ValueError("Geçersiz sayfa eşleştirmesi: " + str(number))
            for seg in page["segments"]:
                if (not isinstance(seg.get("panel_index"), int) or
                    len(seg.get("source_bbox", [])) != 4 or
                    len(seg.get("source_pixels", [])) != 2 or
                    len(seg.get("output_origin", [])) != 2):
                    raise ValueError("Geçersiz sayfa koordinatı: " + str(number))
            with z.open(f"quran/single_pages/page_{number:04}.webp") as stream:
                header = stream.read(12)
            if not (header.startswith(b"RIFF") and header[8:12] == b"WEBP"):
                raise ValueError("Görüntü WebP biçiminde değil: " + str(number))

def verify_selection(db_path: Path):
    if not db_path.is_file() or db_path.stat().st_size < 100000:
        raise ValueError("605 sayfa seçim veritabanı eksik")
    with closing(sqlite3.connect("file:" + db_path.resolve().as_posix() + "?mode=ro", uri=True)) as db:
        if db.execute("PRAGMA quick_check").fetchone()[0] != "ok":
            raise ValueError("Seçim veritabanı bozulmuş")
        pages = db.execute("SELECT count(*) FROM page_info").fetchone()[0]
        source = db.execute("SELECT value FROM meta WHERE key='source_pdf_sha256'").fetchone()
        words = db.execute("SELECT count(*) FROM words").fetchone()[0]
        if pages != PAGE_COUNT or source is None or source[0] != PDF_SHA or words < 60000:
            raise ValueError("Seçim verisi eksik veya özgün PDF kaynağıyla uyumsuz")

def build(project: Path, page_zip: Path, selection: Path, output: Path, gradle: str,
          ocr: Path | None = None, logo: Path | None = None):
    verify_pages(page_zip)
    verify_selection(selection)
    if output.exists():
        raise FileExistsError("Var olan APK asla üzerine yazılmaz")
    if ocr and not ocr.is_file():
        raise FileNotFoundError("Ek OCR dosyası bulunamadı")
    if logo and not logo.is_file():
        raise FileNotFoundError("Logo dosyası bulunamadı")
    if not (project / "settings.gradle.kts").is_file():
        raise ValueError("Bağımsız Android proje klasörü bulunamadı")
    with tempfile.TemporaryDirectory(prefix="kurani_private_") as temp:
        app = Path(temp) / "project"
        shutil.copytree(project, app, ignore=shutil.ignore_patterns(
            "build", ".gradle", "*.apk", "*.aab", "*.jks", "*.keystore"))
        assets = app / "app/src/main/assets"
        assets.mkdir(parents=True, exist_ok=True)
        with zipfile.ZipFile(page_zip) as z:
            for entry in z.infolist():
                if entry.is_dir():
                    continue
                target = assets / entry.filename
                target.parent.mkdir(parents=True, exist_ok=True)
                with z.open(entry) as source, target.open("wb") as dest:
                    shutil.copyfileobj(source, dest)
        (assets / "quran").mkdir(exist_ok=True)
        shutil.copyfile(selection, assets / "quran/selection_layer.sqlite")
        if ocr:
            shutil.copyfile(ocr, assets / "quran/word_ocr.sqlite")
        if logo:
            (assets / "brand").mkdir(exist_ok=True)
            shutil.copyfile(logo, assets / "brand/logo.webp")
        subprocess.run([gradle, "--no-daemon", "--project-dir", str(app),
                        ":app:assembleContentPreview"], cwd=app, check=True)
        apk = app / "app/build/outputs/apk/contentPreview/app-contentPreview.apk"
        if not apk.is_file():
            raise FileNotFoundError("Optimize test APK üretilmedi")
        with zipfile.ZipFile(apk) as z:
            names = set(z.namelist())
            required = {"AndroidManifest.xml", "classes.dex", "assets/quran/selection_layer.sqlite",
                        "assets/quran/single_pages/pages.json"}
            required.update(f"assets/quran/single_pages/page_{n:04}.webp" for n in range(1,606))
            if not required.issubset(names):
                raise ValueError("Derlenmiş APK'de sayfa/kelime dosyaları eksik")
        output.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(apk, output)
    print("Tam içerikli .trial test APK:", output)
    print("SHA-256:", hashlib.sha256(output.read_bytes()).hexdigest())

if __name__ == "__main__":
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--project", type=Path, default=Path(__file__).resolve().parents[1])
    p.add_argument("--pages", type=Path, required=True)
    p.add_argument("--selection", type=Path, required=True)
    p.add_argument("--ocr", type=Path)
    p.add_argument("--logo", type=Path)
    p.add_argument("--output", type=Path, required=True)
    p.add_argument("--gradle", default="gradle")
    args = p.parse_args()
    build(args.project, args.pages, args.selection, args.output,
          args.gradle, args.ocr, args.logo)
