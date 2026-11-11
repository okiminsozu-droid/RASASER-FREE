#!/usr/bin/env python3
"""Prepare an unsigned Google Play candidate from the last user-approved V2.1 TV build.

This runs ONLY after unpack_original.py + v2/apply_v2.py.
Public Actions never receive signing passwords, keystores or private engines.
No code changes to UI or business features are made.
"""
from pathlib import Path
import re

HERE = Path(__file__).resolve().parent
PROJECT = HERE.parent / "original"
gradle = PROJECT / "app/build.gradle.kts"
root_gradle = PROJECT / "build.gradle.kts"
manifest = PROJECT / "app/src/main/AndroidManifest.xml"

def replace_once(path: Path, old: str, new: str, purpose: str):
    content = path.read_text(encoding="utf-8")
    hits = content.count(old)
    if hits != 1:
        raise RuntimeError(f"{purpose}: expected one match, found {hits} in {path}")
    path.write_text(content.replace(old, new), encoding="utf-8")

replace_once(root_gradle, 'id("com.android.application") version "8.5.2"',
    'id("com.android.application") version "8.13.2"', "Android 16-compatible AGP")
replace_once(gradle, "compileSdk = 34", "compileSdk = 36", "compileSdk 36")
replace_once(gradle, "targetSdk = 34", "targetSdk = 36", "Google Play targetSdk 36")
replace_once(gradle, 'versionName = "1.2.2-tv-playback-test"',
    'versionName = "1.2.2"', "Google Play release version name")
# Existing V2.1 patch removes debug signing from the release build. Assert it stays unsigned.
content = gradle.read_text(encoding="utf-8")
release_block = content.split("release {", 1)[1].split("\n        }", 1)[0]
if "signingConfig" in release_block and not release_block.lstrip().startswith("//"):
    raise RuntimeError("Public release must not contain any release signing key")
if 'applicationId = "com.rasaser.launcher"' not in content:
    raise RuntimeError("Production package ID has changed")
if "minSdk = 26" not in content:
    raise RuntimeError("Android 8+ compatibility has changed")

# Retain the original landscape dashboard on Android 16 large-screen test devices
# where temporary compatibility opt-out is still permitted (does not affect Android 15).
replace_once(
    manifest,
    '            android:screenOrientation="landscape"',
    '            android:screenOrientation="landscape"',
    "preserve landscape orientation"
)
replace_once(
    manifest,
    '            <intent-filter>\n                <action android:name="android.intent.action.MAIN" />',
    '            <property android:name="android.window.PROPERTY_COMPAT_ALLOW_RESTRICTED_RESIZABILITY" '
    'android:value="true" />\n\n'
    '            <intent-filter>\n                <action android:name="android.intent.action.MAIN" />',
    "Android 16 landscape compatibility"
)
print("Prepared unsigned AAB candidate: package=com.rasaser.launcher, API36, versionCode=4, versionName=1.2.2")
