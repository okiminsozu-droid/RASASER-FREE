#!/usr/bin/env python3
"""No-Android-SDK source policy smoke test; NOT a substitute for compilation/device tests."""
from pathlib import Path
import json
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ANDROID = "http://schemas.android.com/apk/res/android"

class OfflineContractTests(unittest.TestCase):
    def test_application_identity_and_private_signing(self):
        record = json.loads((ROOT / "app.json").read_text(encoding="utf-8"))
        self.assertEqual(record["slug"], "rasaserwidgets")
        self.assertEqual(record["package_id"], "com.rasaser.free.rasaserwidgets")
        self.assertEqual(record["release_signing_location"], "PRIVATE_ONLY")

    def test_permissions(self):
        root = ET.parse(ROOT / "app/src/main/AndroidManifest.xml").getroot()
        permissions = {el.attrib.get(f"{{{ANDROID}}}name") for el in root.findall("uses-permission")}
        self.assertNotIn("android.permission.INTERNET", permissions)
        self.assertNotIn("android.permission.READ_EXTERNAL_STORAGE", permissions)
        self.assertNotIn("android.permission.MANAGE_EXTERNAL_STORAGE", permissions)
        self.assertNotIn("android.permission.QUERY_ALL_PACKAGES", permissions)
        self.assertNotIn("android.permission.CALL_PHONE", permissions)
        self.assertEqual(permissions, {"android.permission.CAMERA"})
        app = root.find("application")
        self.assertEqual(app.attrib.get(f"{{{ANDROID}}}allowBackup"), "false")
        self.assertEqual(app.attrib.get(f"{{{ANDROID}}}usesCleartextTraffic"), "false")

    def test_widget_metadata(self):
        root = ET.parse(ROOT / "app/src/main/res/xml/widget_info.xml").getroot()
        self.assertIn("horizontal", root.attrib[f"{{{ANDROID}}}resizeMode"])
        self.assertIn("reconfigurable", root.attrib[f"{{{ANDROID}}}widgetFeatures"])
        self.assertIn("WidgetEditorActivity", root.attrib[f"{{{ANDROID}}}configure"])
        ET.parse(ROOT / "app/src/main/res/layout/widget_tile.xml")
        ET.parse(ROOT / "app/src/main/res/drawable/ic_brand.xml")
        ET.parse(ROOT / "app/src/main/res/values/styles.xml")
        ET.parse(ROOT / "app/src/main/res/values/strings.xml")

    def test_no_tracking_runtime_libraries(self):
        gradle = (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8").lower()
        for forbidden in ("firebase", "admob", "appsflyer", "facebook", "analytics", "revenuecat"):
            self.assertNotIn(forbidden, gradle)

    def test_offline_images_have_bounded_decode(self):
        source = (ROOT / "app/src/main/java/com/rasaser/free/rasaserwidgets/WidgetStore.kt").read_text(encoding="utf-8")
        self.assertIn("inJustDecodeBounds = true", source)
        self.assertIn("inSampleSize = sample", source)
        self.assertIn("maxSide", source)

    def test_varyok_single_action_and_languages(self):
        base = ROOT / "app/src/main/java/com/rasaser/free/rasaserwidgets"
        home = (base / "MainActivity.kt").read_text(encoding="utf-8")
        ui = (base / "VaryokText.kt").read_text(encoding="utf-8")
        editor = (base / "WidgetEditorActivity.kt").read_text(encoding="utf-8")
        for required in ('tx("create")', 'showWidgetTypes()', 'showLanguagePicker()', 'showSettings()',
                         'https://www.okiminsozu.com', 'WidgetStore.creationCount'):
            self.assertIn(required, home)
        self.assertNotIn("KISAYOL TİPLERİ", home)
        self.assertNotIn("EKLENEN WIDGET'LAR", home)
        self.assertIn('val codes = listOf("tr", "en", "de", "es", "ja", "ar")', ui)
        for locale in ('tr', 'en', 'de', 'es', 'ja', 'ar'):
            self.assertIn(f'"{locale}" to listOf(', ui)
        self.assertIn("VaryokText.takePreset(this)", editor)

    def test_three_creations_and_existing_editor(self):
        base = ROOT / "app/src/main/java/com/rasaser/free/rasaserwidgets"
        store = (base / "WidgetStore.kt").read_text(encoding="utf-8")
        editor = (base / "WidgetEditorActivity.kt").read_text(encoding="utf-8")
        self.assertIn("fun canCreate(ctx: Context) = creationCount(ctx) < 3", store)
        self.assertIn('if (firstSave) edit.putInt("total_created"', store)
        self.assertIn("countCreation = false", store)
        self.assertIn("!WidgetStore.exists(this, widgetId) && !WidgetStore.canCreate(this)", editor)

    def test_offline_catalog_auto_refresh_and_no_inventory_retention(self):
        base = ROOT / "app/src/main/java/com/rasaser/free/rasaserwidgets"
        catalog = (base / "WidgetCreateActivity.kt").read_text(encoding="utf-8")
        main = (base / "MainActivity.kt").read_text(encoding="utf-8")
        manifest = ET.parse(ROOT / "app/src/main/AndroidManifest.xml").getroot()
        app = manifest.find("application")
        activities = [a.attrib.get(f"{{{ANDROID}}}name") for a in app.findall("activity")]
        self.assertIn(".WidgetCreateActivity", activities)
        self.assertIn("Thread {", catalog)
        self.assertIn("Intent.ACTION_PACKAGE_ADDED", catalog)
        self.assertIn("Intent.ACTION_PACKAGE_REMOVED", catalog)
        self.assertIn("Intent.ACTION_PACKAGE_REPLACED", catalog)
        self.assertIn("pm.queryIntentActivities(query, 0)", catalog)
        self.assertIn("private val apps = mutableListOf<AppEntry>()", catalog)
        self.assertNotIn("getInstalledPackages", catalog)
        self.assertNotIn("QUERY_ALL_PACKAGES", catalog)
        self.assertIn("WidgetCreateActivity::class.java", main)
        self.assertIn('tx("privacy_desc")', main)
        self.assertLess(main.index('tx("about")'), main.index('tx("privacy_desc")'))

    def test_fresh_media_picker_and_nonblocking_decode(self):
        base = ROOT / "app/src/main/java/com/rasaser/free/rasaserwidgets"
        editor = (base / "WidgetEditorActivity.kt").read_text(encoding="utf-8")
        self.assertIn("MediaStore.ACTION_PICK_IMAGES", editor)
        self.assertIn("Intent.ACTION_OPEN_DOCUMENT", editor)
        self.assertIn("Thread {", editor)
        self.assertIn('preset?.target', editor)
        self.assertIn('preset?.caption', editor)
        self.assertIn('kinds.setSelection(codes.indexOf("app"))', editor)

if __name__ == "__main__":
    unittest.main()
