#!/usr/bin/env python3
"""Create a new public Android app from the safe starter template."""
from pathlib import Path
import json
import re
import shutil
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
SLUG = re.compile(r"[a-z][a-z0-9]{2,39}\Z")

def create(slug: str, display_name: str) -> Path:
    if not SLUG.fullmatch(slug):
        raise ValueError("Slug: 3-40 karakter; yalnız küçük harf/rakam, harfle başlamalı.")
    if not display_name.strip() or len(display_name) > 80:
        raise ValueError("Görünür ad 1-80 karakter arasında olmalıdır.")
    target = ROOT / "apps" / slug
    if target.exists():
        raise FileExistsError(f"Mevcut uygulama korunuyor: {target}")
    template = ROOT / "templates" / "android-app"
    shutil.copytree(template, target)
    pkg = f"com.rasaser.free.{slug}"
    gradle = target / "app/build.gradle.kts"
    gradle.write_text(gradle.read_text().replace("com.rasaser.free.template", pkg), encoding="utf-8")
    src = target / "app/src/main/java/com/rasaser/free/template"
    dst = target / f"app/src/main/java/com/rasaser/free/{slug}"
    src.rename(dst)
    kt = dst / "MainActivity.kt"
    kt.write_text(kt.read_text().replace("com.rasaser.free.template", pkg), encoding="utf-8")
    strings = target / "app/src/main/res/values/strings.xml"
    tree = ET.parse(strings)
    tree.find("string").text = display_name
    tree.write(strings, encoding="unicode", xml_declaration=True)
    licenses = target / "app/src/main/assets/legal/licenses.txt"
    licenses.write_text(licenses.read_text().replace("RaSaSeR FREE Şablon", display_name), encoding="utf-8")
    metadata = target / "app.json"
    record = json.loads(metadata.read_text(encoding="utf-8"))
    record.update(slug=slug, display_name=display_name, package_id=pkg)
    metadata.write_text(json.dumps(record, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    settings = target / "settings.gradle.kts"
    settings.write_text(settings.read_text().replace("RaSaSeRFreeTemplate", "RaSaSeR" + slug.title()), encoding="utf-8")
    return target

if __name__ == "__main__":
    try:
        if len(sys.argv) < 3:
            raise ValueError('Kullanım: python3 tools/new_app.py uygulamaadi "Uygulama Adı"')
        print(f"Oluşturuldu: {create(sys.argv[1], sys.argv[2])}")
    except (ValueError, FileExistsError) as exc:
        print(f"HATA: {exc}", file=sys.stderr)
        sys.exit(1)
