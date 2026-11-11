package com.rasaser.free.kurankelime

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.content.res.Configuration
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Crash-isolated native reader. Keeps the existing 304 rendered pages, SQLite search,
 * bookmarks, text selection and sharing, without initializing Compose or the PDF engine.
 */
class StableReaderActivity : Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    private val request = AtomicInteger()
    private lateinit var store: PageStore
    private lateinit var pageView: PageView
    private lateinit var counter: TextView
    private lateinit var bookmark: TextView
    private lateinit var toolbar: LinearLayout
    private lateinit var footer: LinearLayout
    private lateinit var selectedInfo: TextView
    private var page = 1
    private val green = Color.rgb(23,66,47)
    private var bitmap: Bitmap? = null

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
    private fun label(value: String, size: Float = 17f): TextView = TextView(this).apply {
        text = value; textSize = size; setTextColor(green)
        gravity = Gravity.CENTER; setPadding(dp(4),dp(4),dp(4),dp(4))
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = PageStore(this)
        page = intent.getIntExtra("page",0).takeIf { it in 1..304 } ?: store.lastPage()
        try {
            buildUi()
            showPage(page, intent.getIntExtra("highlight_word_id",0))
        } catch (e: Exception) {
            showFailure("Okuyucu başlatılamadı: " + e.javaClass.simpleName + " " + e.message)
        }
    }

    private fun buildUi() {
        val main = FrameLayout(this).apply { setBackgroundColor(Color.WHITE) }
        pageView = PageView(this)
        main.addView(pageView, FrameLayout.LayoutParams(-1,-1))
        pageView.onSwipe = { direction -> showPage((page+direction).coerceIn(1,304)) }
        pageView.onTapWord = { x,y -> findWord(x,y) }
        pageView.onToggle = {
            val visibility = if (toolbar.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            toolbar.visibility = visibility
            footer.visibility = visibility
        }
        toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER
            setBackgroundColor(0xF5F8F0DF.toInt())
        }
        fun top(value: String, weight: Float = 1f, click: () -> Unit): TextView {
            val t = label(value).apply { setOnClickListener { click() } }
            toolbar.addView(t, LinearLayout.LayoutParams(0,dp(54),weight))
            return t
        }
        top("‹"){showPage((page-1).coerceAtLeast(1))}
        counter=top("1 / 304",2f){askPage()}
        bookmark=top("☆"){store.toggleFavorite(page);updateToolbar()}
        top("⌂"){finish()}
        top("›"){showPage((page+1).coerceAtMost(304))}
        main.addView(toolbar,FrameLayout.LayoutParams(-1,dp(54),Gravity.TOP))
        footer=LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER
            setBackgroundColor(0xF8FFFFFF.toInt())
        }
        fun bottom(value: String,click: () -> Unit) {
            footer.addView(label(value,15f).apply { setOnClickListener { click() } },
                LinearLayout.LayoutParams(0,dp(52),1f))
        }
        bottom("−"){pageView.zoomBy(-.5f)}
        bottom("+"){pageView.zoomBy(.5f)}
        bottom("Sığdır"){pageView.toggleFit()}
        bottom("Paylaş"){share()}
        bottom("Gizle"){pageView.onToggle()}
        main.addView(footer,FrameLayout.LayoutParams(-1,dp(52),Gravity.BOTTOM))
        selectedInfo=label("",13f).apply {
            setBackgroundColor(0xF0F8EDDA.toInt())
            visibility=View.GONE
            setOnClickListener {
                val value=tag as? String
                if (value!=null) {
                    (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                        .setPrimaryClip(ClipData.newPlainText("Kur'an kelimesi",value))
                    Toast.makeText(this@StableReaderActivity,"Kelime kopyalandı",Toast.LENGTH_SHORT).show()
                }
            }
        }
        val lp=FrameLayout.LayoutParams(-1,dp(70),Gravity.BOTTOM)
        lp.bottomMargin=dp(52)
        main.addView(selectedInfo,lp)
        setContentView(main)
        updateToolbar()
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        )
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Existing reader and current page survive device rotation.
        pageView.post { pageView.invalidate() }
    }
    private fun updateToolbar() {
        counter.text=page.toString()+" / 304"
        bookmark.text=if(store.isFavorite(page)) "★" else "☆"
    }
    private fun showPage(next:Int, selectId:Int=0) {
        if(next !in 1..304) return
        page=next; store.setLastPage(page); updateToolbar()
        selectedInfo.visibility=View.GONE
        pageView.clearPage()
        val sequence=request.incrementAndGet()
        worker.execute {
            var decoded:Bitmap?=null
            var problem:String?=null
            try {
                assets.open("quran/rendered/page_"+next.toString().padStart(4,'0')+".webp").use {
                    decoded=BitmapFactory.decodeStream(it,null,BitmapFactory.Options().apply {
                        inPreferredConfig=Bitmap.Config.RGB_565
                    })
                }
                if(decoded==null) problem="Görsel açılamadı."
            } catch(e:Exception) { problem=e.javaClass.simpleName+": "+e.message }
            val result=decoded
            val detail=problem
            runOnUiThread {
                if(isFinishing || isDestroyed || request.get()!=sequence) {
                    result?.recycle();return@runOnUiThread
                }
                val old=bitmap
                bitmap=result
                if(result!=null) {
                    pageView.setPage(result)
                    if(selectId>=1000000) findById(selectId,next)
                } else pageView.showError("Sayfa "+next+" açılamadı: "+detail)
                if(old!=null && old!==result && !old.isRecycled) old.recycle()
            }
        }
    }
    private fun findById(id:Int,expectedPage:Int) {
        worker.execute {
            val result=try { BundledWordIndex(applicationContext).byId(id) } catch(_:Exception){null}
            runOnUiThread { if(!isFinishing && page==expectedPage && result?.page==page) mark(result) }
        }
    }
    private fun findWord(x:Float,y:Float) {
        val current=page
        worker.execute {
            val word=try {
                PageWordIndex(applicationContext).nearestWord(current,x,y)
                    ?: BundledWordIndex(applicationContext).nearest(current,x,y)
            } catch(_:Exception){null}
            runOnUiThread {
                if(isFinishing||current!=page) return@runOnUiThread
                if(word!=null) mark(word) else {
                    selectedInfo.tag=null
                    selectedInfo.text="Bu noktada bir kelime bulunamadı."
                    selectedInfo.visibility=View.VISIBLE
                }
            }
        }
    }
    private fun mark(word:PageWord) {
        pageView.highlight(word)
        selectedInfo.tag=word.gloss
        selectedInfo.text=word.gloss+"\nOCR kelime adayı · kopyalamak için dokun"
        selectedInfo.visibility=View.VISIBLE
    }
    private fun askPage() {
        val edit=android.widget.EditText(this).apply {
            inputType=android.text.InputType.TYPE_CLASS_NUMBER
            setText(page.toString())
        }
        AlertDialog.Builder(this).setTitle("Sayfa (1–304)").setView(edit)
            .setNegativeButton("Vazgeç",null)
            .setPositiveButton("Git") { _, _ ->
                val target=edit.text.toString().toIntOrNull()
                if(target!=null&&target in 1..304)showPage(target)
            }.show()
    }
    private fun share() {
        val current=page
        worker.execute {
            try {
                val bytes=assets.open("quran/rendered/page_"+current.toString().padStart(4,'0')+".webp").use {it.readBytes()}
                val target=File(File(cacheDir,"shares").apply{mkdirs()},"sayfa_"+current+".webp")
                target.writeBytes(bytes)
                val uri=FileProvider.getUriForFile(this,packageName+".provider",target)
                runOnUiThread {
                    startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                        type="image/webp"
                        putExtra(Intent.EXTRA_STREAM,uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    },"Sayfayı paylaş"))
                }
            } catch(e:Exception) {
                runOnUiThread{Toast.makeText(this,"Paylaşım yapılamadı",Toast.LENGTH_SHORT).show()}
            }
        }
    }
    private fun showFailure(message:String) {
        setContentView(TextView(this).apply{
            text=message+"\n\nÖnceki verileriniz korunuyor."
            textSize=17f
            setTextColor(Color.BLACK)
            setPadding(dp(24),dp(80),dp(24),dp(24))
        })
    }
    override fun onDestroy() {
        request.incrementAndGet()
        worker.shutdownNow()
        bitmap?.let { if(!it.isRecycled)it.recycle() }
        super.onDestroy()
    }

    private inner class PageView(ctx:Context):View(ctx) {
        private var source:Bitmap?=null
        private var message:String?=null
        private var word:PageWord?=null
        private val imageRect=RectF()
        private var zoom=1f
        private var fitWholePage=false
        private var panX=0f
        private var panY=0f
        private var downX=0f
        private var downY=0f
        private var fingers=1
        private val paint=Paint(Paint.FILTER_BITMAP_FLAG)
        private val line=Paint(Paint.ANTI_ALIAS_FLAG).apply{
            color=0xFFE8A13A.toInt();style=Paint.Style.STROKE;strokeWidth=dp(2).toFloat()
        }
        private val fill=Paint().apply{color=0x33E8A13A}
        private val textPaint=Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color=green;textSize=dp(15).toFloat();textAlign=Paint.Align.CENTER
        }
        var onSwipe:(Int)->Unit={}
        var onTapWord:(Float,Float)->Unit={ _,_-> }
        var onToggle:()->Unit={}
        private val gestures=GestureDetector(ctx,object:GestureDetector.SimpleOnGestureListener(){
            override fun onDown(e:MotionEvent)=true
            override fun onDoubleTap(e:MotionEvent):Boolean { onToggle();return true }
            override fun onSingleTapConfirmed(e:MotionEvent):Boolean {
                val img=source?:return true
                updateRect(img)
                val x=(e.x-imageRect.left)/imageRect.width()
                val y=(e.y-imageRect.top)/imageRect.height()
                if(x in 0f..1f&&y in 0f..1f) onTapWord(x*595f,y*842f)
                return true
            }
            override fun onScroll(e1:MotionEvent?,e2:MotionEvent,dx:Float,dy:Float):Boolean {
                // In fullscreen mode the page extends outside the viewport, so pan
                // even at zoom=1. Swipe-release still advances pages as before.
                if(zoom>1.01f || !fitWholePage) {panX-=dx;panY-=dy;invalidate();return true}
                return false
            }
        })
        private val pinch=ScaleGestureDetector(ctx,object:ScaleGestureDetector.SimpleOnScaleGestureListener(){
            override fun onScale(d:ScaleGestureDetector):Boolean {
                zoom=(zoom*d.scaleFactor).coerceIn(1f,3f)
                if(zoom<=1.01f){panX=0f;panY=0f}
                invalidate();return true
            }
        })
        fun setPage(img:Bitmap){source=img;message=null;word=null;invalidate()}
        fun clearPage(){source=null;message=null;word=null;zoom=1f;fitWholePage=false;panX=0f;panY=0f;invalidate()}
        fun showError(msg:String){source=null;message=msg;invalidate()}
        fun highlight(w:PageWord){word=w;invalidate()}
        fun toggleFit(){
            fitWholePage=!fitWholePage
            zoom=1f
            panX=0f
            panY=0f
            invalidate()
        }
        fun zoomBy(delta:Float){
            zoom=(zoom+delta).coerceIn(1f,3f)
            if(zoom==1f){panX=0f;panY=0f}
            invalidate()
        }
        private fun updateRect(b:Bitmap) {
            // Fullscreen uses CENTER_CROP without stretching Arabic glyphs.
            // Reader can pan clipped portions; Sığdır toggles the entire page overview.
            val baseScale=if(fitWholePage) min(width.toFloat()/b.width,height.toFloat()/b.height)
                else max(width.toFloat()/b.width,height.toFloat()/b.height)
            val scale=baseScale*zoom
            val w=b.width*scale;val h=b.height*scale
            panX=panX.coerceIn(-max(0f,(w-width)/2f),max(0f,(w-width)/2f))
            panY=panY.coerceIn(-max(0f,(h-height)/2f),max(0f,(h-height)/2f))
            val x=(width-w)/2f+panX;val y=(height-h)/2f+panY
            imageRect.set(x,y,x+w,y+h)
        }
        override fun onDraw(c:Canvas) {
            c.drawColor(Color.WHITE)
            val b=source
            if(b!=null&&!b.isRecycled) {
                updateRect(b)
                c.drawBitmap(b,null,imageRect,paint)
                word?.let { w ->
                    val r=RectF(
                        imageRect.left+w.x/595f*imageRect.width(),
                        imageRect.top+(w.y-7f)/842f*imageRect.height(),
                        imageRect.left+(w.x+w.width)/595f*imageRect.width(),
                        imageRect.top+(w.y+w.height+2f)/842f*imageRect.height())
                    c.drawRoundRect(r,5f,5f,fill)
                    c.drawRoundRect(r,5f,5f,line)
                }
            } else c.drawText((message?:"Sayfa açılıyor…").take(100),width/2f,height/2f,textPaint)
        }
        override fun onSizeChanged(w:Int,h:Int,oldw:Int,oldh:Int){
            super.onSizeChanged(w,h,oldw,oldh)
            panX=0f;panY=0f
            invalidate()
        }
        override fun onTouchEvent(e:MotionEvent):Boolean {
            pinch.onTouchEvent(e);gestures.onTouchEvent(e)
            when(e.actionMasked) {
                MotionEvent.ACTION_DOWN->{downX=e.x;downY=e.y;fingers=1}
                MotionEvent.ACTION_POINTER_DOWN->fingers=max(fingers,e.pointerCount)
                MotionEvent.ACTION_UP->{
                    val dx=e.x-downX;val dy=e.y-downY
                    if(fingers==1&&zoom<=1.01f&&abs(dx)>dp(65)&&abs(dx)>abs(dy)*1.3f)
                        onSwipe(if(dx<0)1 else -1)
                }
            }
            return true
        }
    }
}
