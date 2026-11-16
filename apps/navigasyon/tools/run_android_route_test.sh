#!/usr/bin/env bash
set -Eeuo pipefail

ROOT="apps/navigasyon"
TEST_OUTPUT="$RUNNER_TEMP/nav-android-routing-test.txt"
MINIFIED="${NAV_ROUTE_TEST_MINIFIED:-0}"

if [[ "$MINIFIED" == "1" ]]; then
  gradle --no-daemon --project-dir "$ROOT" -PnavMinifiedDeviceTest=true \
    :app:assembleFieldTest :app:assembleFieldTestAndroidTest
  APK="$ROOT/app/build/outputs/apk/fieldTest/app-fieldTest.apk"
  TEST_APK="$ROOT/app/build/outputs/apk/androidTest/fieldTest/app-fieldTest-androidTest.apk"
  TARGET_PACKAGE="com.rasaser.free.navigasyon.fieldtest14"
else
  gradle --no-daemon --project-dir "$ROOT" \
    :app:assembleDebug :app:assembleDebugAndroidTest
  APK="$ROOT/app/build/outputs/apk/debug/app-debug.apk"
  TEST_APK="$ROOT/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"
  TARGET_PACKAGE="com.rasaser.free.navigasyon"
fi

test -s "$APK"
test -s "$TEST_APK"
adb install -r "$APK"
adb install -r "$TEST_APK"

FIXTURE="$RUNNER_TEMP/nav-route-graph"
# The emulator is a dedicated, disposable userdebug device; stage the
# immutable test graph inside the target app so scoped storage cannot hide it.
adb root
adb wait-for-device
test "$(adb shell id -u | tr -d '\r')" = "0"
TARGET_DIR="/data/user/0/$TARGET_PACKAGE/files/routing"
adb shell mkdir -p "$TARGET_DIR"
adb push "$FIXTURE/valhalla_tiles.tar" "$TARGET_DIR/valhalla_tiles.tar"
adb push "$FIXTURE/valhalla.json" "$TARGET_DIR/valhalla.json"
adb push "$FIXTURE/valhalla-turkiye-manifest.json" "$TARGET_DIR/valhalla.manifest.json"
APP_UID="$(adb shell stat -c '%u' "/data/user/0/$TARGET_PACKAGE" | tr -d '\r')"
adb shell chown -R "$APP_UID:$APP_UID" "$TARGET_DIR"
adb shell ls -lh "$TARGET_DIR"

adb shell am instrument -w -r \
  -e class com.rasaser.free.navigasyon.OfflineRouteEngineDeviceTest \
  "$TARGET_PACKAGE.test/androidx.test.runner.AndroidJUnitRunner" \
  | tee "$TEST_OUTPUT"
grep -F "OK (1 test)" "$TEST_OUTPUT"
