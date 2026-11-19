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
        self.assertEqual(permissions, {"android.permission.CAMERA", "android.permission.USE_BIOMETRIC"})
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
        for required in ('tx("create")', 'showWidgetTypes()', 'showLanguagePicker()',
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
        self.assertIn("fun canCreate(ctx: Context): Boolean = creationCount(ctx) < 3", store)
        self.assertIn("fun canCreateFor(ctx: Context, targetPackage: String): Boolean", store)
        self.assertIn('putStringSet("free_distinct_app_packages"', store)
        self.assertIn('putBoolean("free_distinct_migrated", true)', store)
        self.assertIn("countCreation = false", store)
        self.assertNotIn("!WidgetStore.exists(this, widgetId) && !WidgetStore.canCreate(this)", editor)

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
        self.assertIn("pm.queryIntentActivities(intent, 0)", catalog)
        self.assertIn("private val apps = mutableListOf<AppEntry>()", catalog)
        self.assertNotIn("getInstalledPackages", catalog)
        self.assertNotIn("QUERY_ALL_PACKAGES", catalog)
        self.assertIn("WidgetCreateActivity::class.java", main)
        self.assertIn('tx("privacy_desc")', main)
        self.assertIn('text("ⓘ", 26f, true)', main)

    def test_fresh_media_picker_and_nonblocking_decode(self):
        base = ROOT / "app/src/main/java/com/rasaser/free/rasaserwidgets"
        editor = (base / "WidgetEditorActivity.kt").read_text(encoding="utf-8")
        self.assertIn("MediaStore.ACTION_PICK_IMAGES", editor)
        self.assertIn("Intent.ACTION_OPEN_DOCUMENT", editor)
        self.assertIn("Thread {", editor)
        self.assertIn('preset?.target', editor)
        self.assertIn('preset?.caption', editor)
        self.assertIn('kinds.setSelection(codes.indexOf("app"))', editor)

    def test_only_installed_apps_with_icons_and_customization(self):
        base = ROOT / "app/src/main/java/com/rasaser/free/rasaserwidgets"
        catalog = (base / "WidgetCreateActivity.kt").read_text(encoding="utf-8")
        icon = (base / "AppIconKit.kt").read_text(encoding="utf-8")
        self.assertIn("AppIconKit.bitmap(", catalog)
        self.assertIn("iconWorkers = Executors.newFixedThreadPool(2)", catalog)
        self.assertIn("pm.queryIntentActivities(intent, 0)", catalog)
        self.assertIn("showCustomize(filtered[position])", catalog)
        self.assertIn("WidgetStore.stageSelectedPhoto(", catalog)
        self.assertIn("SeekBar(this)", catalog)
        self.assertIn('pinApp(app)', catalog)
        self.assertNotIn('kinds.forEach', catalog)
        self.assertNotIn('showTemplates()', catalog)
        self.assertIn("getApplicationIcon(packageName)", icon)
        self.assertIn("LruCache<String, Bitmap>", icon)

    def test_pin_success_callback_persists_real_widget_id_and_photo(self):
        base = ROOT / "app/src/main/java/com/rasaser/free/rasaserwidgets"
        picker = (base / "WidgetCreateActivity.kt").read_text(encoding="utf-8")
        callback = (base / "PinResultReceiver.kt").read_text(encoding="utf-8")
        provider = (base / "ActionWidgetProvider.kt").read_text(encoding="utf-8")
        store = (base / "WidgetStore.kt").read_text(encoding="utf-8")
        self.assertIn("manager.requestPinAppWidget(provider, null, onSuccess)", picker)
        self.assertNotIn("manager.requestPinAppWidget(provider, null, null)", picker)
        self.assertIn("PinResultReceiver.callback(", picker)
        self.assertIn("PendingIntent.FLAG_MUTABLE", callback)
        self.assertIn("AppWidgetManager.EXTRA_APPWIDGET_ID", callback)
        self.assertIn("WidgetStore.attachStagedPhoto(context, token, widgetId)", callback)
        self.assertIn("WidgetStore.commitPending(context, widgetId)", callback)
        self.assertIn("WidgetStore.save(", callback)
        self.assertIn("ActionWidgetProvider.render(context, widgetId)", callback)
        self.assertIn("WidgetStore.attachStagedPhoto(this, stagingId, token)", picker)
        self.assertIn("fun attachStagedPhoto(", store)
        self.assertIn("AppIconKit.bitmap(", provider)

    def test_widget_click_opens_chosen_app_and_no_settings(self):
        base = ROOT / "app/src/main/java/com/rasaser/free/rasaserwidgets"
        home = (base / "MainActivity.kt").read_text(encoding="utf-8")
        provider = (base / "ActionWidgetProvider.kt").read_text(encoding="utf-8")
        launcher = (base / "AppShortcutActivity.kt").read_text(encoding="utf-8")
        manifest = ET.parse(ROOT / "app/src/main/AndroidManifest.xml").getroot()
        self.assertNotIn("showSettings()", home)
        self.assertNotIn("SettingsActivity::class.java", home)
        self.assertNotIn('text("⚙"', home)
        self.assertIn('setOnClickListener { info(tx("privacy"), tx("privacy_desc")) }', home)
        self.assertIn('Intent(context, AppShortcutActivity::class.java)', provider)
        self.assertIn('putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)', provider)
        self.assertIn("packageManager.getLaunchIntentForPackage(widget.target)", launcher)
        self.assertIn("startActivity(it)", launcher)
        self.assertIn("R.drawable.varyok_app_icon", provider)
        self.assertIn('rv.setViewVisibility(R.id.widget_image, View.VISIBLE)', provider)
        self.assertFalse((base / "SettingsActivity.kt").exists())
        app = manifest.find("application")
        activities = [a.attrib.get(f"{{{ANDROID}}}name") for a in app.findall("activity")]
        receivers = [r.attrib.get(f"{{{ANDROID}}}name") for r in app.findall("receiver")]
        self.assertNotIn(".SettingsActivity", activities)
        self.assertIn(".AppShortcutActivity", activities)
        self.assertIn(".PinResultReceiver", receivers)

    def test_three_distinct_apps_and_legacy_migration(self):
        base = ROOT / "app/src/main/java/com/rasaser/free/rasaserwidgets"
        store = (base / "WidgetStore.kt").read_text(encoding="utf-8")
        home = (base / "MainActivity.kt").read_text(encoding="utf-8")
        picker = (base / "WidgetCreateActivity.kt").read_text(encoding="utf-8")
        receiver = (base / "PinResultReceiver.kt").read_text(encoding="utf-8")
        self.assertIn("free_distinct_app_packages", store)
        self.assertIn('Regex("^widget_([0-9]+)_exists$")', store)
        self.assertIn("current + activeTargets", store)
        self.assertIn('target in usedApplicationPackages(ctx)', store)
        self.assertIn('editor.putStringSet("free_distinct_app_packages", existingPackages + target)', store)
        self.assertIn('startActivity(Intent(this, WidgetCreateActivity::class.java))', home)
        self.assertNotIn('if (!WidgetStore.canCreate(this))', home)
        self.assertIn('WidgetStore.canCreateFor(this, app.packageName)', picker)
        self.assertIn('WidgetStore.canCreateFor(context, packageName)', receiver)

    def test_offline_security_pin_fingerprint_and_one_time_button(self):
        base = ROOT / "app/src/main/java/com/rasaser/free/rasaserwidgets"
        security = (base / "AppSecurity.kt").read_text(encoding="utf-8")
        ui = (base / "AppLockUi.kt").read_text(encoding="utf-8")
        local = (base / "AppLockText.kt").read_text(encoding="utf-8")
        main = (base / "MainActivity.kt").read_text(encoding="utf-8")
        self.assertIn('SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")', security)
        self.assertIn("SecureRandom().nextBytes(it)", security)
        self.assertIn("MessageDigest.isEqual", security)
        self.assertIn('getSharedPreferences(PREFS, Context.MODE_PRIVATE)', security)
        self.assertIn("BiometricPrompt.Builder(activity)", ui)
        self.assertIn("onAuthenticationSucceeded(", ui)
        self.assertIn("AppSecurity.enableBiometric(activity)", ui)
        self.assertIn("AppSecurity.enablePin(activity, pin)", ui)
        self.assertIn("AppSecurity.checkPin(activity, submitted)", ui)
        self.assertIn("AppSecurity.isEnabled(this)", main)
        self.assertIn('AppLockText.t(this, "setup")', main)
        self.assertIn("AppLockUi.setup(this@MainActivity)", main)
        self.assertIn("AppLockUi.unlock(", main)
        self.assertIn("deviceUnlockRequest", main)
        self.assertIn("showLockedScreen()", main)
        for locale in ('tr', 'en', 'de', 'es', 'ja', 'ar'):
            self.assertIn(f'"{locale}" to listOf(', local)

if __name__ == "__main__":
    unittest.main()
