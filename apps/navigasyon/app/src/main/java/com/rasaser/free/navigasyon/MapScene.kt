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
import org.maplibre.android.style.layers.CircleLayer
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
            // The map remains in a true close 3D driving perspective even
            // without a destination; GPS updates re-center it automatically.
            ready.setMinPitchPreference(55.0)
            ready.setMaxPitchPreference(75.0)
            val focus=initialMapFocus()
            ready.cameraPosition=CameraPosition.Builder()
                .target(focus).zoom(18.0).tilt(70.0).bearing(heading).build()
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
            ensureGpsDot()
            lastFix?.let(::follow)
            renderRoute()
        }
    }

    private fun initialMapFocus():LatLng {
        // No location information is invented. When a recent usable GPS fix
        // exists, the vehicle starts close to it; otherwise show the saved
        // camera area, not a false blue vehicle marker.
        val prefs=context.getSharedPreferences("map_camera",Context.MODE_PRIVATE)
        val lat=Double.fromBits(prefs.getLong("lat",39.0.toBits()))
        val lon=Double.fromBits(prefs.getLong("lon",34.65.toBits()))
        return LatLng(lat.coerceIn(35.0,43.0),lon.coerceIn(25.0,46.0))
    }

    fun recenterOnLocation(location:Location) {
        if(!closed && location.isValidNavigationFix())follow(location)
    }

    private fun ensureGpsDot() {
        val m=map ?:return
        m.getStyle { style ->
            if(style.getSource("rasaser-gps-dot")==null) {
                style.addSource(GeoJsonSource("rasaser-gps-dot"))
                style.addLayer(CircleLayer("rasaser-gps-position","rasaser-gps-dot")
                    .withProperties(PropertyFactory.circleRadius(9f),
                        PropertyFactory.circleColor("#177EDD"),
                        PropertyFactory.circleStrokeColor("#FFFFFF"),
                        PropertyFactory.circleStrokeWidth(3f)))
            }
            lastFix?.let(::drawGpsDot)
        }
    }

    private fun drawGpsDot(location:Location) {
        map?.getStyle { style ->
            val source:GeoJsonSource?=style.getSourceAs("rasaser-gps-dot")
            source?.setGeoJson(Point.fromLngLat(location.longitude,location.latitude))
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
        drawGpsDot(location)
        if (location.hasBearing() && location.hasSpeed() && location.speed>=1.5f)
            heading=location.bearing.toDouble()
        val camera=CameraPosition.Builder()
            .target(LatLng(location.latitude,location.longitude))
            .zoom(18.3).tilt(70.0).bearing(heading).build()
        map?.animateCamera(CameraUpdateFactory.newCameraPosition(camera), 750)
        // Camera area persists locally only, never sent over the internet.
        context.getSharedPreferences("map_camera",Context.MODE_PRIVATE).edit()
            .putLong("lat",location.latitude.toBits())
            .putLong("lon",location.longitude.toBits()).apply()
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
                      recenterRequest:Int, onPlace: (String,String,NavPoint)->Unit) {
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
    LaunchedEffect(recenterRequest) { if(fix!=null)scene.recenterOnLocation(fix) }
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
