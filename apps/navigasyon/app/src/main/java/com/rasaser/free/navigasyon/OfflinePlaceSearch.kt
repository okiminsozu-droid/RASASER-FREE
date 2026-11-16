package com.rasaser.free.navigasyon

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.text.Normalizer
import java.util.Locale
import kotlin.math.*

data class OfflinePlace(val name:String,val kind:String,val lat:Double,val lon:Double,val score:Double)

/** All results come from real OSM coordinates in a local SQLite FTS4 database. */
class OfflinePlaceSearch(private val context:Context) {
    private val file:File get()=File(context.filesDir,"search/turkey-places.sqlite")
    fun installed():Boolean=file.isFile && file.length()>=256*1024

    fun find(words:String,origin:NavPoint?):List<OfflinePlace> {
        if(!installed())return emptyList()
        val variants=PlaceQuery.tokens(words)
        if(variants.isEmpty())return emptyList()
        val db=try { SQLiteDatabase.openDatabase(file.path,null,SQLiteDatabase.OPEN_READONLY) }
            catch (_:Exception) { return emptyList() }
        try {
            val found=LinkedHashMap<String,OfflinePlace>()
            val desired=PlaceQuery.normalize(words)
            // Prefer exact named destinations country-wide before broad fuzzy
            // matching; never select a local shop called "Ankara" over Ankara.
            val regions:List<Double?> = if(origin==null) listOf(null)
                else listOf(25.0,150.0,null)
            for(variant in variants) {
                val matching=variant.joinToString(" ") { it+"*" }
                for(radius in regions) {
                    val clause=if(radius!=null && origin!=null)
                        " AND places.lat BETWEEN ? AND ? AND places.lon BETWEEN ? AND ? "
                        else " "
                    val params=if(radius!=null && origin!=null) {
                        val latSpan=radius/111.0
                        val lonSpan=radius/(111.0*cos(Math.toRadians(origin.lat)).coerceAtLeast(0.25))
                        arrayOf(matching,(origin.lat-latSpan).toString(),
                            (origin.lat+latSpan).toString(),
                            (origin.lon-lonSpan).toString(),(origin.lon+lonSpan).toString())
                    } else arrayOf(matching)
                    db.rawQuery(
                        "SELECT places.name,places.kind,places.lat,places.lon,places.normalized "+
                        "FROM places INNER JOIN places_fts ON places.id=places_fts.docid "+
                        "WHERE places_fts MATCH ?"+clause+"LIMIT 350",params).use {c->
                    while(c.moveToNext()){
                        val name=c.getString(0) ?:continue
                        val kind=c.getString(1) ?: "place"
                        val lat=c.getDouble(2);val lon=c.getDouble(3)
                        if(lat !in 35.0..43.5 || lon !in 25.0..46.0)continue
                        val normalized=c.getString(4) ?:continue
                        val exact=desired
                        val match=when {
                            normalized==exact->500.0
                            normalized.startsWith(exact)->370.0
                            exact.startsWith(normalized)->240.0
                            else ->160.0
                        }
                        val distance=if(origin==null)0.0 else
                            RouteGuidance.metersBetween(origin,NavPoint(lat,lon))/1000.0
                        // High-confidence names first; distance resolves duplicate shops.
                        val score=match - min(170.0,distance/3.0)
                        val key=normalized+":"+((lat*1000).toInt())+":"+((lon*1000).toInt())
                        val place=OfflinePlace(name,kind,lat,lon,score)
                        if(found[key]==null || found[key]!!.score<score)found[key]=place
                    }
                }
                if(found.isNotEmpty())break
                }
                if(found.isNotEmpty())break
            }
            return found.values.sortedWith(
                compareByDescending<OfflinePlace>{it.score}.thenBy{it.name}).take(8)
        } catch (_:Exception) {return emptyList()}
        finally {db.close()}
    }
}
object PlaceQuery {
    fun normalize(raw:String):String {
        val lower=raw.trim().lowercase(Locale.ROOT).replace("ı","i")
        val normalized=Normalizer.normalize(lower,Normalizer.Form.NFD)
        return normalized.replace(Regex("\\p{M}+"),"")
            .replace(Regex("[^\\p{L}\\p{N}]+")," ").trim()
    }
    fun tokens(text:String):List<List<String>> {
        val words=normalize(text).split(" ").filter{it.isNotBlank()}
        if(words.isEmpty() || words.size>12 || words.any{it.length>60})return emptyList()
        val variants=mutableListOf(words)
        if(words.last().length>=8) {
            // Turkish suffixes often differ: hastanesine -> hastane,
            // eczanesine -> eczane; fuzzy fallback only on final token.
            variants+=words.dropLast(1)+(words.last().dropLast(2))
            variants+=words.dropLast(1)+(words.last().dropLast(3))
            variants+=words.dropLast(1)+(words.last().dropLast(4))
            variants+=words.dropLast(1)+(words.last().take(6))
        }
        return variants.distinct().filter{it.all{token->token.length>=2}}
    }
}
