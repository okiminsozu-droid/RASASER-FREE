package com.rasaser.free.navigasyon

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Real stock Android SQLite + FTS4 integration test.
 * The 66 MB pinned Cloudflare ZIP is a transient CI-only androidTest asset;
 * it is NEVER bundled in the normal small navigation APK.
 */
@RunWith(AndroidJUnit4::class)
class SearchInstallDeviceTest {
    @Test fun realAndroidSqliteCanInstallPinnedIndexAndFindAnkara() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext
        val archive = File(app.cacheDir, "turkey-place-integrity-test.zip")
        try {
            instrumentation.context.assets.open("turkey-places.zip").use { input ->
                archive.outputStream().buffered().use { output ->
                    input.copyTo(output, 256 * 1024)
                }
            }
            val installer = TurkeySearchInstaller(app)
            val installed = installer.installWithResult(sourceArchive=archive)
            assertTrue("Search installer stage: ${installed.stage}", installed.installed)
            assertTrue("Validated SQLite FTS index is not marked ready", installer.installed())
            val results=OfflinePlaceSearch(app).find("Ankara",null)
            assertTrue("FTS4 search index returned zero Ankara results",results.isNotEmpty())
        } finally {
            archive.delete()
        }
    }
}
