#!/usr/bin/env python3
"""Basic public-repository secret-file guard; not a full credential scanner."""
import json
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FORBIDDEN_EXT = {'.jks', '.keystore', '.p12', '.pfx', '.pem', '.key', '.aab', '.apk'}
FORBIDDEN_NAMES = {'google-services.json', 'local.properties', 'keystore.properties', 'signing.properties', 'service-account.json'}
FORBIDDEN_DIRS = {'private-engines', 'private-release', '.gradle'}

def check(paths):
    issues = []
    for rel in paths:
        path = ROOT / rel
        if not path.is_file():
            continue
        parts = set(path.relative_to(ROOT).parts)
        if parts.intersection(FORBIDDEN_DIRS):
            issues.append(f'Yasak dizin: {rel}')
        if path.suffix.lower() in FORBIDDEN_EXT or path.name.lower() in FORBIDDEN_NAMES:
            issues.append(f'Yasak anahtar/artifact dosyası: {rel}')
        if path.is_symlink():
            issues.append(f'Sembolik dosya: {rel}')
        if path.stat().st_size > 5_000_000:
            issues.append(f'Büyük kaynak dosyası kontrol edilmeli: {rel}')
    for metadata in (ROOT / 'apps').glob('*/app.json'):
        try:
            data = json.loads(metadata.read_text(encoding='utf-8'))
            if data.get('release_signing_location') != 'PRIVATE_ONLY':
                issues.append(f'PUBLIC imzalama yasak: {metadata.relative_to(ROOT)}')
        except Exception as exc:
            issues.append(f'Hatalı metadata: {metadata.relative_to(ROOT)}: {exc}')
    return issues

def main():
    try:
        output = subprocess.check_output(['git', 'ls-files', '--cached', '--others', '--exclude-standard'], cwd=ROOT, text=True, stderr=subprocess.DEVNULL)
        paths = output.splitlines()
    except (FileNotFoundError, subprocess.CalledProcessError):
        paths = [str(p.relative_to(ROOT)) for p in ROOT.rglob('*') if p.is_file()]
    issues = check(paths)
    if issues:
        print('GÜVENLİK KONTROLÜ BAŞARISIZ:')
        print('\n'.join('- '+v for v in issues))
        sys.exit(1)
    print('Yapı kontrolü geçti. Ayrıca gerçek secret scanning uygulanmalıdır.')

if __name__ == '__main__':
    main()
