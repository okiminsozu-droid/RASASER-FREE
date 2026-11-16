package com.rasaser.free.navigasyon

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest

/**
 * Real Android Valhalla JNI integration with the pinned Türkiye graph.
 * The 1.15 GB extract is pushed to Android emulated external files by CI,
 * never checked in or packed into the published small navigation APK.
 */
@RunWith(AndroidJUnit4::class)
class OfflineRouteEngineDeviceTest {
    @Test fun turkiyeOfflineRoutingFindsRealRoads() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val routing=File(context.filesDir,"routing").apply { mkdirs() }
        val graph=File(routing,"valhalla_tiles.tar")
        val config=File(routing,"valhalla.json")
        val metadata=File(routing,"valhalla.manifest.json")
        // The emulator test first stages pinned files directly into the app's
        // private directory. A fallback supports manual local device tests.
        if(!graph.isFile) {
            val fixture=File(requireNotNull(context.getExternalFilesDir(null)),"valhalla-fixture")
            val source=File(fixture,"valhalla_tiles.tar")
            assertTrue("No graph at "+graph.path+" or "+source.path,source.isFile)
            source.copyTo(graph,overwrite=false,bufferSize=1024*1024)
            File(fixture,"valhalla.json").copyTo(config,overwrite=true)
            File(fixture,"valhalla-turkiye-manifest.json").copyTo(metadata,overwrite=true)
        }
        assertTrue("Graph absent from private routing directory",graph.isFile)
        assertTrue("Graph size mismatch",graph.length()==TurkeyRoutingInstaller.GRAPH_BYTES)
        assertTrue("Valhalla config missing",config.isFile)
        assertTrue("Valhalla manifest missing",metadata.isFile)
        OfflineRouteEngine(context).use { engine ->
            assertTrue("Pinned graph must be installed",engine.installed())
            val istanbul=engine.route(
                NavPoint(41.0082,28.9784),NavPoint(41.0104,28.9870),NavLanguage.TR
            )
            assertNotNull("Istanbul route failed: "+engine.failureCode(),istanbul)
            assertTrue("Istanbul route must have real geometry",istanbul!!.shape.size>=2)
            assertTrue("Istanbul road distance positive",istanbul.meters>0)
            val ankara=engine.route(
                NavPoint(38.7330,29.7500),NavPoint(39.9334,32.8597),NavLanguage.TR
            )
            assertNotNull("Uşak-Ankara route failed: "+engine.failureCode(),ankara)
            assertTrue("Uşak-Ankara route must have geometry",ankara!!.shape.size>=2)
        }
    }
}
