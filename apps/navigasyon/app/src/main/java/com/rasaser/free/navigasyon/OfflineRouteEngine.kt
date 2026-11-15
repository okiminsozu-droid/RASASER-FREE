package com.rasaser.free.navigasyon

import android.content.Context
import com.valhalla.valhalla.Valhalla
import com.valhalla.valhalla.config.ValhallaConfigFactory
import org.json.JSONObject
import org.json.JSONArray
import java.io.Closeable
import java.io.File
import java.io.IOException
import kotlin.math.abs

data class NavPoint(val lat: Double, val lon: Double)
data class RoutePlan(
    val shape: List<NavPoint>,
    val meters: Double,
    val seconds: Double,
    val maneuverInstructions: List<String>
)

/**
 * Routes are computed locally from a real Valhalla tile extract; never draw a
 * straight-line fallback, never send GPS to any server.
 * Call from a background thread, not from Compose or the main thread.
 */
class OfflineRouteEngine(private val context: Context) : Closeable {
    private val lock=Any()
    private var native:Valhalla?=null
    private val extract:File get()=File(context.filesDir,"routing/valhalla_tiles.tar")
    private val marker:File get()=File(context.filesDir,"routing/valhalla.manifest.json")

    fun installed(): Boolean = extract.isFile && marker.isFile && extract.length()>20*1024*1024

    fun route(origin:NavPoint,destination:NavPoint,lang:NavLanguage): RoutePlan? = synchronized(lock) {
        if(!installed()) return@synchronized null
        val instance=native ?: try {
            val config=ValhallaConfigFactory.usingTileExtract(extract.absolutePath)
            Valhalla(context,config).also { native=it }
        } catch (_:Exception) {return@synchronized null}
        try {
            val req=JSONObject()
                .put("locations",JSONArray()
                    .put(JSONObject().put("lat",origin.lat).put("lon",origin.lon).put("type","break"))
                    .put(JSONObject().put("lat",destination.lat).put("lon",destination.lon).put("type","break")))
                .put("costing","auto")
                .put("directions_options",JSONObject()
                    .put("units","kilometers")
                    .put("language",valhallaLanguage(lang)))
            val response=JSONObject(instance.routeRaw(req.toString()))
            val trip=response.optJSONObject("trip") ?: return@synchronized null
            val legs=trip.optJSONArray("legs") ?: return@synchronized null
            val points=mutableListOf<NavPoint>()
            val instructions=mutableListOf<String>()
            for(i in 0 until legs.length()) {
                val leg=legs.optJSONObject(i) ?:continue
                val geometry=decodePolyline6(leg.optString("shape",""))
                if(points.isNotEmpty() && geometry.isNotEmpty() && points.last()==geometry.first())
                    points.addAll(geometry.drop(1)) else points.addAll(geometry)
                val maneuvers=leg.optJSONArray("maneuvers")
                if(maneuvers!=null) for(j in 0 until maneuvers.length()) {
                    val text=maneuvers.optJSONObject(j)?.optString("instruction","") ?: ""
                    if(text.isNotBlank())instructions+=text
                }
            }
            if(points.size<2)return@synchronized null
            val summary=trip.optJSONObject("summary") ?:return@synchronized null
            val distance=summary.optDouble("length",Double.NaN)*1000.0
            val duration=summary.optDouble("time",Double.NaN)
            if(!distance.isFinite() || !duration.isFinite() || distance<=0.0)return@synchronized null
            RoutePlan(points,distance,duration,instructions)
        } catch (_: Exception) { null }
    }

    override fun close()=synchronized(lock) {
        native?.close()
        native=null
    }

    // Valhalla routing language support may differ from the app's six UI languages.
    private fun valhallaLanguage(lang:NavLanguage):String=when(lang){
        NavLanguage.TR->"tr-TR"
        NavLanguage.EN->"en-US"
        NavLanguage.DE->"de-DE"
        NavLanguage.AR->"ar"
        NavLanguage.ES->"es-ES"
        NavLanguage.JA->"ja-JP"
    }
}

/** Valhalla encoded polylines use 1e6 precision, NOT Google polyline5. */
fun decodePolyline6(encoded:String): List<NavPoint> {
    if(encoded.length>8_000_000)return emptyList()
    val output=ArrayList<NavPoint>()
    var index=0;var lat=0L;var lon=0L
    fun next():Long? {
        var shift=0
        var result=0L
        while(index<encoded.length && shift<=35) {
            val b=encoded[index++].code-63
            if(b !in 0..63) return null
            result=result or ((b and 31).toLong() shl shift)
            if((b and 32)==0) return if ((result and 1L)!=0L) (result shr 1).inv() else result shr 1
            shift+=5
        }
        return null
    }
    while(index<encoded.length) {
        lat += next() ?: return emptyList()
        lon += next() ?: return emptyList()
        val a=lat/1_000_000.0; val b=lon/1_000_000.0
        if(a !in -90.0..90.0 || b !in -180.0..180.0)return emptyList()
        if(output.size>=1_000_000)return emptyList()
        output+=NavPoint(a,b)
    }
    return output
}
