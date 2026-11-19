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

if __name__ == "__main__":
    unittest.main()
