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
    fun pick(page:Int,px:Float,py:Float):Hit? {
        if(page !in 1..605||px !in 0f..1f||py !in 0f..1f)return null
        val d=db()?:return null
        val dimensions=d.rawQuery("SELECT width,height FROM page_info WHERE page=?",
            arrayOf(page.toString())).use{
            if(it.moveToFirst())it.getInt(0) to it.getInt(1)else return null
        }
        val mx=18f/dimensions.first
        val my=19f/dimensions.second
        val found=ArrayList<Hit>()
        try{
            d.rawQuery("SELECT x,y,w,h,lang,word,confidence,ocr_id FROM words "+
                "WHERE page=? AND x<=? AND (x+w)>=? AND y<=? AND (y+h)>=?",
                arrayOf(page.toString(),(px+mx).toString(),(px-mx).toString(),
                    (py+my).toString(),(py-my).toString())).use{c->
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
}
