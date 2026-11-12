package com.rasaser.free.kurankelime

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.graphics.RectF
import java.io.File
import kotlin.math.max
import kotlin.math.min

/** Pixel-aligned, selectable OCR candidates for all 605 complete page images.
 * Source Quran images and the existing search index are unchanged.
 * OCR output is NOT authenticated Quran transcription.
 */
internal class SelectableTextLayer(private val context:Context) {
    data class Hit(val token:String,val lang:String,val confidence:Float,val id:Int,
                   val rect:RectF) {
        fun toSelection(sourcePdf:Int) = BundledWordIndex.Selection(
            PageWord(1000000+id,sourcePdf,0,0f,0f,0f,0f,token,token),
            token, if(lang=="ara")"ar" else "tr",confidence)
    }
    private val file=File(context.noBackupFilesDir,"quran_selectable_605_v1.sqlite")
    private val lock=Any()
    private var connection:SQLiteDatabase?=null
    fun available()=try{context.assets.open("quran/selection_layer.sqlite").close();true}catch(_:Exception){false}
    private fun db():SQLiteDatabase?=synchronized(lock) {
        connection?.takeIf{it.isOpen}?.let{return@synchronized it}
        if(!available())return@synchronized null
        if(!file.exists()||file.length()<500000L){
            val temp=File(file.parentFile,file.name+".tmp")
            try {
                temp.delete()
                context.assets.open("quran/selection_layer.sqlite").use{input->
                    temp.outputStream().use{output->input.copyTo(output,65536)}
                }
                if(file.exists())file.delete()
                if(!temp.renameTo(file)){temp.copyTo(file,true);temp.delete()}
            }catch(_:Exception){temp.delete();return@synchronized null}
        }
        try{
            val db=SQLiteDatabase.openDatabase(file.path,null,SQLiteDatabase.OPEN_READONLY)
            val count=db.rawQuery("SELECT count(*) FROM page_info",null).use{
                if(it.moveToFirst())it.getInt(0)else 0
            }
            val source=db.rawQuery("SELECT value FROM meta WHERE key='source_pdf_sha256'",null).use{
                if(it.moveToFirst())it.getString(0)else ""
            }
            if(count!=605 || source!="387674c9a3bfb2c501c2dcca4b63946ea05f386dcd4d6fa4b63b4060b01397ea"){
                db.close();return@synchronized null
            }
            connection=db
            db
        }catch(_:Exception){null}
    }
    /**
     * OCR-recognized range in the source engine's word sequence. A range is not
     * presented as verified Quran text: damaged OCR/line-breaks remain possible.
     * Arabic OCR IDs generally follow RTL visual reading order; the original
     * order is preserved instead of sorting RTL characters into reverse text.
     */
    data class TextSpan(val words:List<Hit>,val lang:String) {
        val text:String get()=words.joinToString(" ") { it.token.trim() }
        val boxes:List<RectF> get()=words.map { it.rect }
    }

    fun range(page:Int,fromOcrId:Int,toOcrId:Int,lang:String):TextSpan? {
        if(page !in 1..605 || lang !in setOf("ara","tur"))return null
        if(fromOcrId<=0 || toOcrId<=0)return null
        val database=db() ?: return null
        val first=min(fromOcrId,toOcrId)
        val last=max(fromOcrId,toOcrId)
        val words=ArrayList<Hit>()
        try {
            database.rawQuery(
                "SELECT x,y,w,h,word,confidence,ocr_id FROM words " +
                "WHERE page=? AND lang=? AND ocr_id BETWEEN ? AND ? " +
                "ORDER BY ocr_id LIMIT 121",
                arrayOf(page.toString(),lang,first.toString(),last.toString())
            ).use { c ->
                while(c.moveToNext()) {
                    val token=c.getString(4)?.trim().orEmpty()
                    if(token.isBlank())continue
                    val x=c.getFloat(0);val y=c.getFloat(1)
                    val w=c.getFloat(2);val h=c.getFloat(3)
                    if(w<=0f || h<=0f)continue
                    words.add(Hit(token,lang,c.getFloat(5),c.getInt(6),
                        RectF(x,y,x+w,y+h)))
                }
            }
        } catch (_: Exception) {return null}
        // Do not silently truncate Quran text or accept unrelated selection boundaries.
        if(words.isEmpty() || words.size>120 ||
           words.first().id!=first || words.last().id!=last)return null
        return TextSpan(words,lang)
    }

    fun pick(page:Int,px:Float,py:Float,language:String?=null):Hit? {
        if(page !in 1..605||px !in 0f..1f||py !in 0f..1f)return null
        val d=db()?:return null
        val dimensions=d.rawQuery("SELECT width,height FROM page_info WHERE page=?",
            arrayOf(page.toString())).use{
            if(it.moveToFirst())it.getInt(0) to it.getInt(1)else return null
        }
        val mx=18f/dimensions.first
        val my=19f/dimensions.second
        val found=ArrayList<Hit>()
        if(language!=null && language !in setOf("ara","tur"))return null
        val languageClause=if(language==null) "" else " AND lang=?"
        val args=mutableListOf(page.toString(),(px+mx).toString(),(px-mx).toString(),
            (py+my).toString(),(py-my).toString())
        if(language!=null) args.add(language)
        try{
            d.rawQuery("SELECT x,y,w,h,lang,word,confidence,ocr_id FROM words "+
                "WHERE page=? AND x<=? AND (x+w)>=CAST(? AS REAL) AND y<=? AND (y+h)>=CAST(? AS REAL)"+
                languageClause,args.toTypedArray()).use{c->
                while(c.moveToNext()){
                    val l=c.getFloat(0);val t=c.getFloat(1)
                    val w=c.getFloat(2);val h=c.getFloat(3)
                    val value=c.getString(5)?.trim().orEmpty()
                    if(w>0&&h>0&&value.isNotEmpty())
                        found+=Hit(value,c.getString(4),c.getFloat(6),c.getInt(7),
                            RectF(l,t,l+w,t+h))
                }
            }
        }catch(_:Exception){return null}
        return found.minByOrNull{hit->
            val b=hit.rect
            val dx=max(0f,max(b.left-px,px-b.right))*dimensions.first
            val dy=max(0f,max(b.top-py,py-b.bottom))*dimensions.second
            val cx=(px-(b.left+b.right)*0.5f)*dimensions.first
            val cy=(py-(b.top+b.bottom)*0.5f)*dimensions.second
            // Prefer clearer OCR regions when two word boxes overlap.
            4f*(dx*dx+dy*dy)+0.025f*(cx*cx+cy*cy)+0.25f*(100f-hit.confidence)
        }
    }

    /**
     * Read the Turkish text physically printed underneath an Arabic OCR word
     * on the SAME source page. This is a geometric OCR aid, NOT a certified
     * religious translation or an Arabic/Turkish dictionary.
     *
     * Existing 605-page assets, user bookmarks and word selection stay untouched.
     */
    data class PrintedTurkishGloss(val text: String, val confidence: Float)

    fun printedTurkishBelow(page: Int, ocrId: Int, selectedArabic: String): PrintedTurkishGloss? {
        if (page !in 1..605 || ocrId <= 0 || selectedArabic.isBlank()) return null
        val database = db() ?: return null
        data class ArabicBox(val x: Float, val y: Float, val w: Float,
                             val h: Float, val word: String)
        data class TurkishWord(val word: String, val x: Float, val y: Float,
                               val confidence: Float)
        val source = try {
            database.rawQuery(
                "SELECT x,y,w,h,word FROM words WHERE page=? AND ocr_id=? AND lang='ara' LIMIT 1",
                arrayOf(page.toString(), ocrId.toString())
            ).use { c ->
                if (c.moveToFirst()) ArabicBox(
                    c.getFloat(0), c.getFloat(1), c.getFloat(2), c.getFloat(3), c.getString(4)
                ) else null
            }
        } catch (_: Exception) { null } ?: return null
        // Do not attach a gloss to another OCR token or a drag-selected phrase.
        if (source.word.trim() != selectedArabic.trim()) return null
        if (source.w <= 0f || source.h <= 0f) return null

        val bottom = source.y + source.h
        val candidates = ArrayList<TurkishWord>()
        try {
            database.rawQuery(
                "SELECT word,x,y,confidence FROM words WHERE page=? AND lang='tur' " +
                "AND confidence>=62 AND y>=CAST(? AS REAL) AND y<=CAST(? AS REAL) " +
                "AND (x+w*0.5)>=CAST(? AS REAL) " +
                "AND (x+w*0.5)<=CAST(? AS REAL) ORDER BY y,x LIMIT 24",
                arrayOf(
                    page.toString(), (bottom - .005f).toString(),
                    (bottom + .054f).toString(), (source.x - .001f).toString(),
                    (source.x + source.w + .001f).toString()
                )
            ).use { c ->
                while (c.moveToNext()) {
                    val word = c.getString(0)?.trim().orEmpty()
                    if (word.any { it.isLetter() }) candidates.add(
                        TurkishWord(word, c.getFloat(1), c.getFloat(2), c.getFloat(3))
                    )
                }
            }
        } catch (_: Exception) { return null }
        if (candidates.isEmpty()) return null

        // Keep the one- or two-line Turkish block immediately under this word,
        // rather than silently concatenating the next Arabic word's explanation.
        val firstY = candidates.first().y
        val lines = candidates.filter { it.y <= firstY + .019f }
        if (lines.isEmpty() || lines.size > 12) return null
        val ordered = lines.sortedWith(compareBy<TurkishWord> {
            ((it.y - firstY) / .012f).toInt()
        }.thenBy { it.x })
        val result = ordered.joinToString(" ") { it.word }.trim()
        if (result.isBlank() || result.length > 180) return null
        return PrintedTurkishGloss(result, ordered.map { it.confidence }.average().toFloat())
    }
}
