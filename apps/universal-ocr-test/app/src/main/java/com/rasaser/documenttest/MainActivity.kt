package com.rasaser.documenttest

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.text.Html
import android.text.Selection
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.googlecode.tesseract.android.TessBaseAPI
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File
import java.io.IOException
import java.util.concurrent.Executors
import java.util.zip.ZipFile
import kotlin.math.max

/** A deliberately separate, installable demo of the shared RaSaSeR core API
 * with REAL offline Tesseract OCR and native text extraction adapters.
 * It does NOT alter other apps and does NOT send document data to servers.
 */
class MainActivity : Activity() {
    private val worker=Executors.newSingleThreadExecutor()
    private val ui=Handler(Looper.getMainLooper())
    private val dark=Color.rgb(19,27,38)
    private val gold=Color.rgb(216,183,125)
    private var source:File?=null
    private var filename=""
    private var currentBitmap:Bitmap?=null
    private var page=1
    private var totalPages=1
    private lateinit var result:TextView
    private lateinit var status:TextView
    private lateinit var info:TextView
    private lateinit var picture:ImageView
    private lateinit var busy:ProgressBar
    private lateinit var ocr:Spinner
    private lateinit var from:Spinner
    private lateinit var to:Spinner
    private val ocrNames=arrayOf("Türkçe + English + عربي","Türkçe","English","العربية")
    private val ocrTags=arrayOf("tur+eng+ara","tur","eng","ara")
    private val langs=arrayOf("Türkçe","English","Deutsch","Español","العربية","中文","Français","Русский","日本語")
    private val langTags=arrayOf("tr","en","de","es","ar","zh","fr","ru","ja")
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun rounded(color:Int)=GradientDrawable().apply {
        setColor(color);cornerRadius=dp(12).toFloat()
    }
    private fun txt(s:String,size:Float,color:Int,bold:Boolean=false)=TextView(this).apply {
        text=s;textSize=size;setTextColor(color);setPadding(dp(2),dp(3),dp(2),dp(3))
        if(bold) typeface=android.graphics.Typeface.DEFAULT_BOLD
    }
    private fun btn(s:String,block:()->Unit)=Button(this).apply {
        text=s;isAllCaps=false;textSize=13f;setTextColor(dark);background=rounded(gold)
        setOnClickListener{block()}
    }
    private fun row(host:LinearLayout,vararg views:Button){
        val r=LinearLayout(this).apply {orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER}
        views.forEach {b->
            r.addView(b,LinearLayout.LayoutParams(0,dp(50),1f).apply{
                setMargins(dp(2),dp(4),dp(2),dp(4))
            })
        }
        host.addView(r)
    }
    private fun spin(host:LinearLayout,label:String,choices:Array<String>,selected:Int):Spinner {
        host.addView(txt(label,12f,gold,true))
        return Spinner(this).also{
            it.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,choices)
            it.setSelection(selected);it.background=rounded(Color.WHITE)
            host.addView(it,LinearLayout.LayoutParams(-1,dp(45)))
        }
    }
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        PDFBoxResourceLoader.init(applicationContext)
        window.statusBarColor=dark;window.navigationBarColor=dark
        val scroll=ScrollView(this).apply {setBackgroundColor(dark);isFillViewport=true}
        val host=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL
            setPadding(dp(16),dp(16),dp(16),dp(18))
        }
        scroll.addView(host)
        host.addView(txt("RaSaSeR  ◇  APPS & TECHNOLOGY",13f,gold,true))
        host.addView(txt("BELGE & OCR ATÖLYESİ",22f,Color.WHITE,true))
        host.addView(txt("PDF  ·  Görsel  ·  Word  ·  Excel  ·  EPUB",13f,0xFFD0DCE7.toInt()))
        host.addView(txt("OCR: Cihazda Türkçe, İngilizce, Arapça modelleri",12f,0xFF94DEB8.toInt()))
        row(host,btn("Dosya aç"){choose()},btn("‹ Sayfa"){move(-1)},btn("Sayfa ›"){move(1)})
        status=txt("Bir dosya aç. OCR gerekiyorsa otomatik yapılır.",13f,gold)
        info=txt("Sayfa: —",12f,Color.WHITE)
        host.addView(status);host.addView(info)
        busy=ProgressBar(this).apply{visibility=View.GONE}
        host.addView(busy,LinearLayout.LayoutParams(-1,dp(35)))
        picture=ImageView(this).apply {
            scaleType=ImageView.ScaleType.FIT_CENTER;adjustViewBounds=true
            setBackgroundColor(Color.WHITE);visibility=View.GONE
        }
        host.addView(picture,LinearLayout.LayoutParams(-1,ViewGroup.LayoutParams.WRAP_CONTENT))
        ocr=spin(host,"OCR tanıma dili",ocrNames,0)
        row(host,btn("Tekrar OCR"){repeatOcr()},btn("Kopyala"){copy()})
        from=spin(host,"Çeviri kaynak dili",langs,0)
        to=spin(host,"Çeviri hedef dili",langs,1)
        row(host,btn("Seçili metni çevir"){translate()})
        host.addView(txt("Kelimeye uzun basıp tutamaçları sürükle; Kopyala ve Çevir seçili aralığı kullanır.",12f,gold))
        result=txt("Henüz belge seçilmedi.",17f,0xFF163B32.toInt()).apply{
            setPadding(dp(14),dp(16),dp(14),dp(18))
            setBackgroundColor(0xFFFCF8F1.toInt());minHeight=dp(210)
            setTextIsSelectable(true)
        }
        host.addView(result,LinearLayout.LayoutParams(-1,ViewGroup.LayoutParams.WRAP_CONTENT))
        host.addView(txt("Çeviri için ilk kullanımda Wi-Fi ile model indirilebilir. Model cihazda olduktan sonra çeviri çevrimdışıdır. OCR her zaman yereldir.",11f,0xFFCED7E0.toInt()))
        setContentView(scroll)
    }
    private fun choose(){
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{
            type="*/*";addCategory(Intent.CATEGORY_OPENABLE)
        },7002)
    }
    @Deprecated("Intent is limited to the system SAF picker")
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){
        super.onActivityResult(requestCode,resultCode,data)
        if(requestCode==7002 && resultCode==RESULT_OK)data?.data?.let{importDocument(it)}
    }
    private fun state(message:String,loading:Boolean) = ui.post {
        busy.visibility=if(loading)View.VISIBLE else View.GONE
        status.text=message
    }
    private fun importDocument(uri:Uri) {
        state("Dosya cihaz içinde hazırlanıyor…",true)
        worker.execute{
            try {
                val name=contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use{
                    if(it.moveToFirst())it.getString(0)else null
                }?:"document"
                val ext=name.substringAfterLast('.', "").lowercase()
                if(ext !in setOf("pdf","jpg","jpeg","png","webp","bmp","txt","md","csv","tsv","xml","json","html","htm","docx","xlsx","pptx","odt","ods","odp","epub"))
                    throw IOException("Bu test sürümünde desteklenmeyen dosya: "+ext)
                val file=File(cacheDir,"sample-"+System.nanoTime()+"."+ext)
                try {
                    val input=contentResolver.openInputStream(uri) ?:throw IOException("Okuma izni yok")
                    input.use {source->
                        file.outputStream().use{output->
                            val bytes=ByteArray(32768);var used=0L
                            while(true){
                                val n=source.read(bytes)
                                if(n<0)break
                                used+=n
                                if(used>80L*1024*1024)throw IOException("Test sınırı 80 MB")
                                output.write(bytes,0,n)
                            }
                        }
                    }
                    if(file.length()==0L)throw IOException("Boş dosya")
                }catch(e:Exception){file.delete();throw e}
                source?.delete();source=file;filename=name;page=1
                display()
            }catch(e:Exception){state("Dosya açılamadı: "+e.message,false)}
        }
    }
    private fun move(n:Int){
        if(source==null)return
        if(page+n !in 1..totalPages){
            Toast.makeText(this,"Başka sayfa yok.",Toast.LENGTH_SHORT).show();return
        }
        page+=n;state("Sayfa okunuyor…",true)
        worker.execute{display()}
    }
    private fun display(){
        val file=source?:return
        try{
            val ext=file.extension.lowercase()
            var bmp:Bitmap?=null
            var scanned=false
            val text=when(ext){
                "pdf"->{
                    val pfd=ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY)
                    PdfRenderer(pfd).use{renderer->
                        totalPages=renderer.pageCount
                        renderer.openPage(page-1).use{sheet->
                            val scale=minOf(2.5f,2100f/max(sheet.width,sheet.height))
                            bmp=Bitmap.createBitmap(max(1,(sheet.width*scale).toInt()),
                                max(1,(sheet.height*scale).toInt()),Bitmap.Config.ARGB_8888)
                            bmp!!.eraseColor(Color.WHITE)
                            sheet.render(bmp!!,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        }
                    }
                    val native=PDDocument.load(file).use{doc->
                        PDFTextStripper().apply{startPage=page;endPage=page}.getText(doc).trim()
                    }
                    if(native.length>=8)native else{
                        scanned=true;recognize(bmp!!,mode())
                    }
                }
                "png","jpg","jpeg","webp","bmp"->{
                    totalPages=1;bmp=decode(file);scanned=true;recognize(bmp!!,mode())
                }
                "docx","xlsx","pptx","odt","ods","odp","epub"->{
                    totalPages=1;unzipText(file,ext)
                }
                else->{totalPages=1;file.inputStream().bufferedReader(Charsets.UTF_8).use{it.readText().take(300000)}}
            }
            val selected=text.ifBlank{"Metin bulunamadı. OCR dilini değiştirip tekrar dene."}.take(300000)
            ui.post{
                if(isDestroyed || isFinishing)return@post
                val previous=currentBitmap;currentBitmap=bmp
                if(bmp!=null){picture.setImageBitmap(bmp);picture.visibility=View.VISIBLE}
                else{picture.setImageDrawable(null);picture.visibility=View.GONE}
                if(previous!=null && previous!==bmp && !previous.isRecycled)previous.recycle()
                info.text=filename+" • "+page+"/"+totalPages
                result.text=selected
                busy.visibility=View.GONE
                status.text=if(scanned)"Çevrimdışı OCR tamamlandı. Metin hatalı olabilir."
                    else "Belgedeki Unicode metin cihaz içinde çıkarıldı."
            }
        }catch(e:Exception){state("Belge hatası: "+e.message,false)}
    }
    private fun decode(file:File):Bitmap{
        val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true}
        BitmapFactory.decodeFile(file.path,bounds)
        if(bounds.outWidth<=0 || bounds.outHeight<=0)throw IOException("Geçersiz görüntü")
        if(bounds.outWidth.toLong()*bounds.outHeight>80000000L)throw IOException("Görüntü çok büyük")
        var scale=1
        while(bounds.outWidth/scale>2300 || bounds.outHeight/scale>2300)scale*=2
        return BitmapFactory.decodeFile(file.path,BitmapFactory.Options().apply{
            inSampleSize=scale;inPreferredConfig=Bitmap.Config.ARGB_8888
        })?:throw IOException("Görüntü okunamadı")
    }
    private fun mode()=ocrTags.getOrElse(ocr.selectedItemPosition){"tur+eng+ara"}
    private fun recognize(bitmap:Bitmap,tags:String):String{
        val dir=File(filesDir,"tesseract")
        val trained=File(dir,"tessdata")
        if(!trained.exists() && !trained.mkdirs())throw IOException("Model dizini açılamadı")
        tags.split('+').forEach{lang->
            val model=File(trained,lang+".traineddata")
            if(!model.exists() || model.length()<250000L){
                val temp=File(trained,lang+".tmp")
                try{
                    assets.open("tessdata/"+lang+".traineddata").use{input->
                        temp.outputStream().use{output->input.copyTo(output,65536)}
                    }
                    if(temp.length()<250000L || !temp.renameTo(model))
                        throw IOException("OCR model dosyası bozuk: "+lang)
                }finally{temp.delete()}
            }
        }
        val tess=TessBaseAPI()
        try{
            if(!tess.init(dir.absolutePath,tags))throw IOException("OCR açılamadı: "+tags)
            tess.setImage(bitmap)
            return tess.getUTF8Text()?.trim().orEmpty()
        }finally{tess.recycle()}
    }
    private fun repeatOcr(){
        val bitmap=currentBitmap
        if(bitmap==null || bitmap.isRecycled){
            Toast.makeText(this,"Önce bir görüntü/PDF seç.",Toast.LENGTH_SHORT).show();return
        }
        val language=mode();state("Çevrimdışı OCR çalışıyor…",true)
        worker.execute{
            try{
                val text=recognize(bitmap,language)
                ui.post{
                    result.text=text.ifBlank{"Bu modelde metin okunamadı."}
                    status.text="OCR tamamlandı: "+language
                    busy.visibility=View.GONE
                }
            }catch(e:Exception){state("OCR hatası: "+e.message,false)}
        }
    }
    private fun unzipText(file:File,ext:String):String{
        val out=StringBuilder()
        val allowed:(String)->Boolean=when(ext){
            "docx"->{name->name=="word/document.xml"}
            "xlsx"->{name->name=="xl/sharedStrings.xml"||name.startsWith("xl/worksheets/sheet")&&name.endsWith(".xml")}
            "pptx"->{name->name.startsWith("ppt/slides/slide")&&name.endsWith(".xml")}
            "epub"->{name->name.endsWith(".html")||name.endsWith(".xhtml")}
            else->{name->name=="content.xml"}
        }
        ZipFile(file).use{zip->
            val entries=zip.entries();var num=0
            while(entries.hasMoreElements()){
                val entry=entries.nextElement()
                if(entry.isDirectory||!allowed(entry.name)||entry.size>3000000L)continue
                if(++num>150 || out.length>300000)break
                val xml=zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).use{it.readText().take(3000000)}
                val raw=xml.replace(Regex("</(?:w:p|a:p|text:p|p|h[1-6])>",RegexOption.IGNORE_CASE),"\n")
                    .replace(Regex("<[^>]*>")," ")
                out.append(Html.fromHtml(raw,Html.FROM_HTML_MODE_LEGACY).toString().trim()).append("\n")
            }
        }
        return out.toString().take(300000)
    }
    private fun selected():String{
        val t=result.text
        val a=Selection.getSelectionStart(t);val b=Selection.getSelectionEnd(t)
        return if(a>=0&&b>a&&b<=t.length)t.subSequence(a,b).toString() else t.toString()
    }
    private fun copy(){
        val t=selected().trim()
        if(t.isEmpty())return
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
            .setPrimaryClip(ClipData.newPlainText("RaSaSeR Belge",t))
        Toast.makeText(this,"Seçili metin kopyalandı.",Toast.LENGTH_SHORT).show()
    }
    private fun translate(){
        val t=selected().trim().take(3000)
        if(t.isBlank()||t=="Henüz belge seçilmedi."){
            Toast.makeText(this,"Önce metin seç.",Toast.LENGTH_SHORT).show();return
        }
        val src=langTags[from.selectedItemPosition];val dst=langTags[to.selectedItemPosition]
        if(src==dst){
            Toast.makeText(this,"Kaynak ve hedef dil aynı.",Toast.LENGTH_SHORT).show();return
        }
        AlertDialog.Builder(this).setTitle("Cihaz içi çeviri")
            .setMessage("İlk kullanımda dil modelleri Wi-Fi üzerinden indirilebilir. İndirildikten sonra çevrimdışı çalışır. Devam edilsin mi?")
            .setNegativeButton("Vazgeç",null)
            .setPositiveButton("Devam"){_,_->translateNow(t,src,dst)}.show()
    }
    private fun translateNow(t:String,src:String,dst:String){
        val tr=Translation.getClient(TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.fromLanguageTag(src)?:src)
            .setTargetLanguage(TranslateLanguage.fromLanguageTag(dst)?:dst).build())
        val dialog=AlertDialog.Builder(this).setTitle("Çeviri: "+src+" → "+dst)
            .setMessage("Yerel model kontrol ediliyor…")
            .setNegativeButton("Kapat",null).create()
        dialog.show()
        tr.downloadModelIfNeeded(DownloadConditions.Builder().requireWifi().build())
            .addOnSuccessListener{
                tr.translate(t).addOnSuccessListener{output->
                    if(dialog.isShowing && !isFinishing){
                        dialog.setMessage(output+"\n\nMakine çevirisi; doğrulanmış meal değildir.")
                        dialog.setButton(AlertDialog.BUTTON_POSITIVE,"Kopyala"){_,_->
                            (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                                .setPrimaryClip(ClipData.newPlainText("RaSaSeR Çeviri",output))
                        }
                    }
                }.addOnFailureListener{
                    if(dialog.isShowing)dialog.setMessage("Çevrilemedi: "+it.message)
                }.addOnCompleteListener{tr.close()}
            }.addOnFailureListener{
                tr.close()
                if(dialog.isShowing)dialog.setMessage("Model kurulu değil veya Wi-Fi ile indirilemedi: "+it.message)
            }
    }
    override fun onDestroy(){
        worker.shutdownNow()
        currentBitmap?.recycle()
        source?.delete()
        super.onDestroy()
    }
}
