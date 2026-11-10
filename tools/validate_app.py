#!/usr/bin/env python3
"""Validate app slug, package identity, and public artifact policy."""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
VALID_SLUG = re.compile(r"[a-z][a-z0-9]{2,39}\Z")

def validate(slug: str, artifact: bool = False) -> dict:
    if not VALID_SLUG.fullmatch(slug):
        raise ValueError("Geçersiz uygulama yolu")
    apps = (ROOT / 'apps').resolve()
    path = (apps / slug).resolve()
    if path.parent != apps or not path.is_dir() or (apps / slug).is_symlink():
        raise ValueError("Uygulama apps/ altında bulunamadı")
    metadata = path / 'app.json'
    if metadata.is_symlink():
        raise ValueError("app.json sembolik bağlantı olamaz")
    record = json.loads(metadata.read_text(encoding='utf-8'))
    if record.get('slug') != slug or record.get('package_id') != f'com.rasaser.free.{slug}':
        raise ValueError("Uygulama kimlikleri birbiriyle uyumsuz")
    if record.get('release_signing_location') != 'PRIVATE_ONLY':
        raise ValueError("Release imzalama yalnız PRIVATE olmalıdır")
    if artifact and (record.get('private_engine_required') or not record.get('allow_public_debug_artifacts')):
        raise ValueError("Public debug artifact bu uygulama için yasak")
    if not (path / 'app/build.gradle.kts').is_file():
        raise ValueError("Android Gradle proje dosyası bulunamadı")
    return record

if __name__ == '__main__':
    try:
        if len(sys.argv) not in (2, 3):
            raise ValueError('Kullanım: validate_app.py slug [true|false]')
        record = validate(sys.argv[1], len(sys.argv) == 3 and sys.argv[2].lower() == 'true')
        print('Onaylandı:', record['slug'], record['package_id'])
    except (ValueError, OSError, json.JSONDecodeError) as err:
        print('HATA:', err, file=sys.stderr)
        sys.exit(1)
