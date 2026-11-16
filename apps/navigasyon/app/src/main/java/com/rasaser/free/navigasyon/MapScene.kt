package com.rasaser.free.navigasyon

import android.content.Context
import android.location.Location
import android.os.Bundle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.geojson.Point
import org.maplibre.geojson.LineString
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

/** Entire map is read from private-device MBTiles; no external map service. */
class MapScene(private val context: Context, onPlace: (String, String, NavPoint)->Unit) : AutoCloseable {
    private val tileServer=LocalTileServer(context.applicationContext, File(context.filesDir,"maps/turkey.mbtiles"))
    private val mapView: MapView
    private var map: MapLibreMap? = null
    private var lastFix: Location?=null
    private var heading = 0.0
    private var closed=false
    private var night=false
    private var language=NavLanguage.TR
    private var routePoints:List<NavPoint> = emptyList()

    init {
        MapLibre.getInstance(context.applicationContext)
        mapView=MapView(context)
        mapView.onCreate(null)
        mapView.getMapAsync { ready ->
            map=ready
            ready.cameraPosition=CameraPosition.Builder()
                .target(LatLng(39.0,34.65)).zoom(5.2).tilt(50.0).build()
            ready.addOnMapClickListener { position ->
                val touches=ready.queryRenderedFeatures(ready.projection.toScreenLocation(position),"pois")
                val feature=touches.firstOrNull()
                if (feature != null) {
                    fun property(name:String):String =
                        if (feature.hasProperty(name))
                            feature.getStringProperty(name) ?: "" else ""
                    onPlace(property("name"), listOf("amenity","shop","tourism","leisure","historic","man_made")
                        .map(::property).firstOrNull { it.isNotBlank() } ?: "",
                        NavPoint(position.latitude, position.longitude))
                    true
                } else false
            }
            ready.addOnMapLongClickListener { location ->
                onPlace("", "selected", NavPoint(location.latitude, location.longitude))
                true
            }
            loadStyle()
        }
    }

    val view:MapView get()=mapView
    fun onLifecycle(event: Lifecycle.Event) {
        if (closed) return
        when(event) {
            Lifecycle.Event.ON_START->mapView.onStart()
            Lifecycle.Event.ON_RESUME->mapView.onResume()
            Lifecycle.Event.ON_PAUSE->mapView.onPause()
            Lifecycle.Event.ON_STOP->mapView.onStop()
            Lifecycle.Event.ON_DESTROY->close()
            else -> Unit
        }
    }
    private fun loadStyle() {
        val file=if(night) "rasaser-shortbread-night.json" else "rasaser-shortbread-day.json"
        val raw=context.assets.open("maps/$file").bufferedReader().use { it.readText() }
            .replace("127.0.0.1:8765","127.0.0.1:${tileServer.port}")
        val json=JSONObject(raw)
        // Arabic needs a script-aware Open Font License font (self-hosted loopback).
        // Android MapLibre Native supports font-faces; Japanese CJK falls back
        // to the device's own ideographic glyph renderer.
        if(language==NavLanguage.AR) {
            val arabic=JSONObject()
                .put("url","http://127.0.0.1:${tileServer.port}/fonts/noto-arabic.ttf")
                .put("unicode-range",JSONArray()
                    .put("U+0600-06FF").put("U+0750-077F").put("U+08A0-08FF")
                    .put("U+FB50-FDFF").put("U+FE70-FEFF"))
            json.put("font-faces",JSONObject()
                .put("Noto Sans Regular",JSONArray().put(arabic)))
        }
        val layers=json.getJSONArray("layers")
        for(i in 0 until layers.length()) {
            val layer=layers.optJSONObject(i) ?:continue
            if(layer.optString("type")!="symbol")continue
            val layout=layer.optJSONObject("layout") ?:continue
            val langs=JSONArray().put("coalesce")
            // Multilingual place names only where OSM contributors supplied them.
            langs.put(JSONArray().put("get").put("name_"+language.tag))
            langs.put(JSONArray().put("get").put("name"))
            langs.put(JSONArray().put("get").put("name_en"))
            langs.put("")
            layout.put("text-field",langs)
        }
        val style=json.toString()
        map?.setStyle(Style.Builder().fromJson(style)) {
            lastFix?.let(::follow)
            renderRoute()
        }
    }
    fun setLanguage(value:NavLanguage) {
        if(language==value)return
        language=value
        if(map!=null)loadStyle()
    }
    fun setNightMode(value:Boolean) {
        if(night==value) return
        night=value
        if(map!=null) loadStyle()
    }
    fun follow(location:Location) {
        if(closed || !location.isValidNavigationFix()) return
        lastFix=location
        if (location.hasBearing() && location.hasSpeed() && location.speed>=1.5f)
            heading=location.bearing.toDouble()
        val camera=CameraPosition.Builder()
            .target(LatLng(location.latitude,location.longitude))
            .zoom(17.3).tilt(60.0).bearing(heading).build()
        map?.animateCamera(CameraUpdateFactory.newCameraPosition(camera), 750)
    }

    fun setRoute(points:List<NavPoint>) {
        routePoints=points
        renderRoute()
    }
    private fun renderRoute() {
        val ready=map ?:return
        val positions=routePoints
        if(positions.size<2) {
            ready.getStyle { style ->
                val old:GeoJsonSource?=style.getSourceAs("rasaser-route")
                old?.setGeoJson(org.maplibre.geojson.FeatureCollection.fromFeatures(emptyList()))
            }
            return
        }
        ready.getStyle { style ->
            if(closed)return@getStyle
            val line=LineString.fromLngLats(positions.map { Point.fromLngLat(it.lon,it.lat) })
            val source:GeoJsonSource?=style.getSourceAs("rasaser-route")
            if(source!=null) {
                source.setGeoJson(line)
            } else {
                style.addSource(GeoJsonSource("rasaser-route",line))
                style.addLayer(LineLayer("rasaser-route-line","rasaser-route")
                    .withProperties(
                        PropertyFactory.lineColor("#138CFA"),
                        PropertyFactory.lineWidth(8f),
                        PropertyFactory.lineOpacity(0.96f),
                        PropertyFactory.lineCap("round"),
                        PropertyFactory.lineJoin("round")
                    ))
            }
        }
    }

    override fun close() {
        if(closed) return
        closed=true
        map=null
        runCatching { mapView.onDestroy() }
        tileServer.close()
    }
}

@Composable
fun OfflineDrivingMap(fix: Location?, route:List<NavPoint>?, language:NavLanguage,
                      onPlace: (String,String,NavPoint)->Unit) {
    val owner=LocalLifecycleOwner.current
    val context=androidx.compose.ui.platform.LocalContext.current
    val currentCallback=androidx.compose.runtime.rememberUpdatedState(onPlace)
    val scene=remember(context) { MapScene(context){name,category,point-> currentCallback.value(name,category,point)} }
    AndroidView(factory={scene.view}, modifier=Modifier.fillMaxSize())
    DisposableEffect(owner,scene) {
        val observer=LifecycleEventObserver { _,event->scene.onLifecycle(event) }
        owner.lifecycle.addObserver(observer)
        // A map added after Activity reaches STARTED needs lifecycle catch-up.
        if(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) scene.onLifecycle(Lifecycle.Event.ON_START)
        if(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) scene.onLifecycle(Lifecycle.Event.ON_RESUME)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            scene.close()
        }
    }
    LaunchedEffect(fix?.time,fix?.latitude,fix?.longitude) { if(fix!=null) scene.follow(fix) }
    LaunchedEffect(route) { scene.setRoute(route.orEmpty()) }
    LaunchedEffect(language) { scene.setLanguage(language) }
    // Day/night mode remains fully offline and uses the device's local clock.
    LaunchedEffect(scene) {
        while(true) {
            val hour=java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            scene.setNightMode(hour < 7 || hour >= 19)
            kotlinx.coroutines.delay(60_000L)
        }
    }
}
