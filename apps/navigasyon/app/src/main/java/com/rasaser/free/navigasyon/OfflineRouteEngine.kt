package com.rasaser.free.navigasyon

import android.content.Context
import com.valhalla.valhalla.Valhalla
import com.valhalla.valhalla.ValhallaResponse
import com.valhalla.api.models.RouteRequest
import com.valhalla.api.models.RouteResponse
import com.valhalla.config.models.ValhallaConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
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
    val maneuverInstructions: List<String>,
    val maneuvers: List<RouteManeuver> = emptyList()
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
    private val configFile:File get()=File(context.filesDir,"routing/valhalla.json")
    private val moshi=Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    fun installed(): Boolean = extract.isFile && marker.isFile && configFile.isFile && extract.length()>20*1024*1024

    private fun activeEngine(): Valhalla? {
        native?.let { return it }
        if(!installed())return null
        return try {
            val json=JSONObject(configFile.readText())
            val mjolnir=json.optJSONObject("mjolnir") ?:return null
            mjolnir.put("tile_extract",extract.absolutePath)
            mjolnir.put("tile_dir",File(context.filesDir,"routing/valhalla_tiles").absolutePath)
            mjolnir.remove("tile_url")
            val config=moshi.adapter(ValhallaConfig::class.java).fromJson(json.toString())
                ?:return null
            Valhalla(context,config).also { native=it }
        } catch (_:Exception) { null }
    }

    fun route(origin:NavPoint,destination:NavPoint,lang:NavLanguage): RoutePlan? = synchronized(lock) {
        val instance=activeEngine() ?:return@synchronized null
        try {
            val req=JSONObject()
                .put("locations",JSONArray()
                    .put(JSONObject().put("lat",origin.lat).put("lon",origin.lon).put("type","break"))
                    .put(JSONObject().put("lat",destination.lat).put("lon",destination.lon).put("type","break")))
                .put("costing","auto")
                .put("directions_options",JSONObject()
                    .put("units","kilometers")
                    .put("language",valhallaLanguage(lang)))
            val parsed=moshi.adapter(RouteRequest::class.java).fromJson(req.toString())
                ?: return@synchronized null
            val responseObject=when (val output=instance.route(parsed)) {
                is ValhallaResponse.Json ->output.jsonResponse
                else->return@synchronized null
            }
            val response=JSONObject(moshi.adapter(RouteResponse::class.java).toJson(responseObject))
            val trip=response.optJSONObject("trip") ?: return@synchronized null
            val legs=trip.optJSONArray("legs") ?: return@synchronized null
            val points=mutableListOf<NavPoint>()
            val instructions=mutableListOf<String>()
            val turns=mutableListOf<RouteManeuver>()
            for(i in 0 until legs.length()) {
                val leg=legs.optJSONObject(i) ?:continue
                val geometry=decodePolyline6(leg.optString("shape",""))
                if(geometry.isEmpty())return@synchronized null
                val overlap=if(points.isNotEmpty() && geometry.first()==points.last()) 1 else 0
                val offset=points.size-overlap
                points.addAll(geometry.drop(overlap))
                val legsManeuvers=leg.optJSONArray("maneuvers")
                if(legsManeuvers!=null)for(j in 0 until legsManeuvers.length()) {
                    val maneuver=legsManeuvers.optJSONObject(j) ?:continue
                    val text=maneuver.optString("instruction","").trim()
                    if(text.isBlank())continue
                    val localIndex=maneuver.optInt("begin_shape_index",-1)
                    if(localIndex !in geometry.indices) continue
                    val index=offset+localIndex
                    if(index !in points.indices)continue
                    instructions+=text
                    val warnings=buildList {
                        if(maneuver.optBoolean("toll",false))add(RouteHazard.TOLL)
                        if(maneuver.optBoolean("rough",false))add(RouteHazard.ROUGH_ROAD)
                        if(maneuver.optBoolean("ferry",false))add(RouteHazard.FERRY)
                        if(maneuver.optBoolean("gate",false))add(RouteHazard.GATE)
                    }
                    turns+=RouteManeuver(index,text,
                        maneuver.optString("verbal_transition_alert_instruction","").trim(),
                        maneuver.optString("verbal_pre_transition_instruction","").trim(),warnings)
                }
            }
            if(points.size<2)return@synchronized null
            val summary=trip.optJSONObject("summary") ?:return@synchronized null
            val distance=summary.optDouble("length",Double.NaN)*1000.0
            val duration=summary.optDouble("time",Double.NaN)
            if(!distance.isFinite() || !duration.isFinite() || distance<=0.0)return@synchronized null
            RoutePlan(points,distance,duration,instructions,turns)
        } catch (_: Exception) { null }
    }


    /**
     * Optional current-road posted speed limit derived from Valhalla trace_attributes.
     * Called off the main thread using a short, recent and accurate GPS trace.
     * Do not substitute edge.speed: it is the routing-speed estimate, not the legal limit.
     */
    fun matchedPostedSpeedLimit(points:List<NavPoint>, accuracyMeters:Float): RoadSpeedLimit? = synchronized(lock) {
        if(points.size !in 2..8 || accuracyMeters !in 0.1f..25f) return@synchronized null
        val actor=activeEngine() ?:return@synchronized null
        try {
            val shape=JSONArray()
            for(point in points) {
                if(point.lat !in -90.0..90.0 || point.lon !in -180.0..180.0)
                    return@synchronized null
                shape.put(JSONObject().put("lat",point.lat).put("lon",point.lon))
            }
            val fields=JSONArray().put("edge.names").put("edge.speed_limit")
                .put("edge.speed").put("edge.speed_type")
                .put("matched.point").put("matched.type")
                .put("matched.edge_index").put("matched.distance_from_trace_point")
            val request=JSONObject().put("shape",shape).put("costing","auto")
                .put("shape_match","map_snap")
                .put("trace_options",JSONObject()
                    .put("gps_accuracy",accuracyMeters.toDouble())
                    .put("search_radius",35))
                .put("filters",JSONObject().put("attributes",fields).put("action","include"))
            val response=JSONObject(actor.traceAttributesRaw(request.toString()))
            val matches=response.optJSONArray("matched_points") ?:return@synchronized null
            val last=matches.optJSONObject(matches.length()-1) ?:return@synchronized null
            if(last.optString("type")!="matched" ||
                last.optDouble("distance_from_trace_point",1000.0)>30.0 ||
                !last.has("edge_index"))return@synchronized null
            val edgeIndex=last.optInt("edge_index",-1)
            val edges=response.optJSONArray("edges") ?:return@synchronized null
            if(edgeIndex !in 0 until edges.length())return@synchronized null
            val edge=edges.optJSONObject(edgeIndex) ?:return@synchronized null
            val limit=edge.optDouble("speed_limit",Double.NaN)
            if(!limit.isFinite() || limit<10.0 || limit>150.0) return@synchronized null
            val name=edge.optJSONArray("names")?.optString(0)?.takeIf {it.isNotBlank()}
            RoadSpeedLimit(limit.toInt(),name)
        } catch (_:Exception) { null }
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
