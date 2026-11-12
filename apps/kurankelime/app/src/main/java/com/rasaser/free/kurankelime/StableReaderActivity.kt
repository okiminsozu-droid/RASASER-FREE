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
import android.graphics.Rect
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.os.Bundle
import android.os.Build
import android.view.WindowInsets
import android.view.WindowInsetsController
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
    private val selectionRequest = AtomicInteger()
    private val bundledIndex by lazy { BundledWordIndex(applicationContext) }
    private val pageIndex by lazy { PageWordIndex(applicationContext) }
    private lateinit var store: PageStore
    private lateinit var single: SinglePageSource
    private lateinit var pageView: PageView
    private lateinit var counter: TextView
    private lateinit var chapterTitle: TextView
    private lateinit var pageFooter: TextView
    private lateinit var viewportLayout: FrameLayout.LayoutParams
    private lateinit var bookmark: TextView
    private lateinit var toolbar: LinearLayout
    private lateinit var footer: LinearLayout
    private lateinit var selectedInfo: TextView
    private var page = 1 // The complete 1..605 Quran reading page
    private var readerControlsVisible = true
    private val green = Color.rgb(23,66,47)
    private var bitmap: Bitmap? = null
    private var selectedWord: BundledWordIndex.Selection? = null
    private var wordActionMode: ActionMode? = null
    private val translation by lazy { OnDeviceWordTranslator(this) }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
    private fun label(value: String, size: Float = 17f): TextView = TextView(this).apply {
        text = value; textSize = size; setTextColor(green)
        gravity = Gravity.CENTER; setPadding(dp(4),dp(4),dp(4),dp(4))
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            store = PageStore(this)
            single = SinglePageSource(this)
            val sourcePage = intent.getIntExtra("page",0)
            page = when {
                sourcePage in 1..304 && intent.hasExtra("hit_x") && intent.hasExtra("hit_y") ->
                    single.atPdf(sourcePage,intent.getFloatExtra("hit_x",0f),
                        intent.getFloatExtra("hit_y",0f))
                sourcePage in 1..304 -> single.firstForPdf(sourcePage)
                store.lastSinglePage() in 1..single.count -> store.lastSinglePage()
                else -> store.oldScreenAsSinglePage()
            }
            buildUi()
            showPage(page, intent.getIntExtra("highlight_word_id",0))
        } catch (e: Exception) {
            showFailure("Okuyucu başlatılamadı: " + e.javaClass.simpleName + " " + e.message)
        }
    }

    private fun buildUi() {
        val main = FrameLayout(this).apply { setBackgroundColor(Color.WHITE) }
        pageView = PageView(this)
        // Reserve real space for both toolbars and the source page metadata.
        // This prevents the first/last Quran lines from hiding behind overlays.
        viewportLayout = FrameLayout.LayoutParams(-1,-1).apply {
            topMargin=dp(98)
            bottomMargin=dp(80)
        }
        main.addView(pageView, viewportLayout)
        pageView.onSwipe = { direction -> showPage((page+direction).coerceIn(1,single.count)) }
        pageView.onTapWord = { x,y -> findWord(x,y) }
        pageView.onToggle = {
            readerControlsVisible = !readerControlsVisible
            selectedInfo.visibility=View.GONE
            applyReaderLayout()
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
        counter=top("1 / 605",2f){askPage()}
        bookmark=top("☆"){store.toggleFavorite(single.page(page).sourcePdfPage);updateToolbar()}
        top("⌂"){finish()}
        top("›"){showPage((page+1).coerceAtMost(single.count))}
        main.addView(toolbar,FrameLayout.LayoutParams(-1,dp(54),Gravity.TOP))
        chapterTitle=label("",14f).apply {
            setBackgroundColor(0xFFFDF9ED.toInt())
            maxLines=1
            contentDescription="Sûre adı"
        }
        val titleLp=FrameLayout.LayoutParams(-1,dp(42),Gravity.TOP).apply {
            topMargin=dp(54)
        }
        main.addView(chapterTitle,titleLp)
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
        pageFooter=label("",12f).apply {
            setBackgroundColor(0xFFFDF9ED.toInt())
            contentDescription="Okuma sayfasının numarası"
        }
        val pageFootLp=FrameLayout.LayoutParams(-1,dp(27),Gravity.BOTTOM).apply {
            bottomMargin=dp(52)
        }
        main.addView(pageFooter,pageFootLp)
        selectedInfo=label("",13f).apply {
            setBackgroundColor(0xF0F8EDDA.toInt())
            visibility=View.GONE
            setOnClickListener {
                val value=tag as? String
                if (value!=null) {
                    (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                        .setPrimaryClip(ClipData.newPlainText("Kur'an kelimesi",value))
                    Toast.makeText(this@StableReaderActivity,"Kelime kopyalandı",Toast.LENGTH_SHORT).show()
                    hideWordInfo()
                }
            }
        }
        val lp=FrameLayout.LayoutParams(-1,dp(42),Gravity.BOTTOM)
        lp.bottomMargin=dp(82)
        main.addView(selectedInfo,lp)
        setContentView(main)
        updateToolbar()
        readerControlsVisible = resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
        applyReaderLayout()
        applyImmersive()
    }

    private fun applyImmersive() {
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                window.setDecorFitsSystemWindows(false)
                window.insetsController?.apply {
                    hide(WindowInsets.Type.systemBars())
                    systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )
            }
        } catch (_: Exception) { /* Keep displaying pages on vendor Android versions. */ }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyImmersive()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyImmersive()
        // Landscape uses compact metadata and maximal image space.
        // Portrait reserves independent toolbar, chapter, image and footer areas.
        readerControlsVisible = newConfig.orientation != Configuration.ORIENTATION_LANDSCAPE
        selectedInfo.visibility=View.GONE
        applyReaderLayout()
        pageView.post { pageView.restoreFullscreen() }
    }

    private fun applyReaderLayout() {
        if(!::viewportLayout.isInitialized || !::chapterTitle.isInitialized) return
        val landscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val titleLp = chapterTitle.layoutParams as FrameLayout.LayoutParams
        val footerLp = pageFooter.layoutParams as FrameLayout.LayoutParams
        if (landscape && !readerControlsVisible) {
            toolbar.visibility=View.GONE
            footer.visibility=View.GONE
            chapterTitle.visibility=View.VISIBLE
            pageFooter.visibility=View.VISIBLE
            titleLp.topMargin=0
            titleLp.height=dp(26)
            footerLp.bottomMargin=0
            footerLp.height=dp(22)
            viewportLayout.topMargin=dp(28)
            viewportLayout.bottomMargin=dp(24)
        } else {
            toolbar.visibility=if(readerControlsVisible) View.VISIBLE else View.GONE
            footer.visibility=if(readerControlsVisible) View.VISIBLE else View.GONE
            chapterTitle.visibility=if(readerControlsVisible && !landscape) View.VISIBLE else View.GONE
            pageFooter.visibility=if(readerControlsVisible && !landscape) View.VISIBLE else View.GONE
            titleLp.topMargin=dp(54)
            titleLp.height=dp(42)
            footerLp.bottomMargin=dp(52)
            footerLp.height=dp(27)
            viewportLayout.topMargin=if(readerControlsVisible) {
                if(landscape) dp(54) else dp(98)
            } else 0
            viewportLayout.bottomMargin=if(readerControlsVisible) {
                if(landscape) dp(52) else dp(80)
            } else 0
        }
        chapterTitle.layoutParams=titleLp
        pageFooter.layoutParams=footerLp
        pageView.requestLayout()
    }
    private fun updateToolbar() {
        counter.text=page.toString()+" / "+single.count
        bookmark.text=if(store.isFavorite(single.page(page).sourcePdfPage)) "★" else "☆"
        chapterTitle.text=single.chapterHeading(page)
        pageFooter.text="Sayfa "+page+" / "+single.count
    }

    private fun hideWordInfo() {
        if(!::selectedInfo.isInitialized) return
        selectedInfo.visibility=View.GONE
        if(::viewportLayout.isInitialized && toolbar.visibility==View.VISIBLE) {
            viewportLayout.bottomMargin=dp(80)
            pageView.requestLayout()
        }
    }
    private fun showPage(next:Int, selectId:Int=0) {
        if(next !in 1..single.count) return
        val entry = single.page(next)
        wordActionMode?.finish()
        selectedWord=null
        selectionRequest.incrementAndGet()
        page=next
        store.setLastPage(entry.sourcePdfPage)
        store.setLastSinglePage(page)
        updateToolbar()
        hideWordInfo()
        pageView.clearPage()
        val sequence=request.incrementAndGet()
        worker.execute {
            var decoded:Bitmap?=null
            var problem:String?=null
            try {
                assets.open(entry.assetName).use {
                    decoded=BitmapFactory.decodeStream(it,null,BitmapFactory.Options().apply {
                        inPreferredConfig=Bitmap.Config.RGB_565
                    })
                }
                if(decoded==null) problem="Tekli sayfa görüntüsü açılamadı."
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
                    pageView.setPage(result, entry)
                    if(selectId>=1000000) findById(selectId, entry.sourcePdfPage, next)
                } else pageView.showError("Sayfa "+next+" açılamadı: "+detail)
                if(old!=null && old!==result && !old.isRecycled) old.recycle()
            }
        }
    }
    private fun findById(id:Int,expectedPdfPage:Int,expectedScreen:Int) {
        worker.execute {
            val result=try { bundledIndex.byId(id) } catch(_:Exception){null}
            runOnUiThread {
                if(!isFinishing && page==expectedScreen && result?.page==expectedPdfPage)
                    mark(result,showMeaning=false)
            }
        }
    }
    private fun findWord(x:Float,y:Float) {
        val current=page
        val sourcePage=single.page(current).sourcePdfPage
        val selectSeq=selectionRequest.incrementAndGet()
        worker.execute {
            // Arabic source words take precedence when the touch falls on their
            // glyphs; Turkish glosses can also be selected and copied.
            val selected=try {
                bundledIndex.selectNearest(sourcePage,x,y)
                    ?: pageIndex.nearestWord(sourcePage,x,y)?.let {
                        BundledWordIndex.Selection(it,it.gloss,"tr",100f)
                    }
            } catch(_:Exception){null}
            runOnUiThread {
                if(isFinishing||current!=page||selectionRequest.get()!=selectSeq)return@runOnUiThread
                wordActionMode?.finish()
                selectedWord=selected
                if(selected!=null) {
                    pageView.highlight(selected.word)
                    hideWordInfo()
                    showWordSelectionMenu()
                } else {
                    pageView.highlightClear()
                    hideWordInfo()
                }
            }
        }
    }

    private fun mark(word:PageWord,showMeaning:Boolean) {
        // Search navigation silently highlights the hit without opening a toolbar.
        pageView.highlight(word)
        hideWordInfo()
    }

    private fun showWordSelectionMenu() {
        val selection=selectedWord ?: return
        if (selection.text.isBlank()) return
        val callback=object:ActionMode.Callback2() {
            override fun onCreateActionMode(mode:ActionMode,menu:Menu):Boolean {
                menu.add(0,1,0,"Kopyala").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                menu.add(0,2,1,"Çevir").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                return true
            }
            override fun onPrepareActionMode(mode:ActionMode,menu:Menu):Boolean = false
            override fun onActionItemClicked(mode:ActionMode,item:MenuItem):Boolean {
                when(item.itemId) {
                    1 -> {
                        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                            .setPrimaryClip(ClipData.newPlainText("Seçilen Kur’an kelimesi",selection.text))
                        Toast.makeText(this@StableReaderActivity,"Kelime kopyalandı",Toast.LENGTH_SHORT).show()
                    }
                    2 -> translation.translate(selection)
                    else -> return false
                }
                mode.finish()
                return true
            }
            override fun onDestroyActionMode(mode:ActionMode) {
                wordActionMode=null
            }
            override fun onGetContentRect(mode:ActionMode,view:View,outRect:Rect) {
                pageView.wordRectangle(outRect)
            }
        }
        wordActionMode=pageView.startActionMode(callback,ActionMode.TYPE_FLOATING)
        wordActionMode?.invalidateContentRect()
    }

    private fun askPage() {
        val edit=android.widget.EditText(this).apply {
            inputType=android.text.InputType.TYPE_CLASS_NUMBER
            setText(page.toString())
        }
        AlertDialog.Builder(this).setTitle("Okuma sayfası (1–605)").setView(edit)
            .setNegativeButton("Vazgeç",null)
            .setPositiveButton("Git") { _, _ ->
                val target=edit.text.toString().toIntOrNull()
                if(target!=null&&target in 1..single.count)showPage(target)
            }.show()
    }
    private fun share() {
        val current=page
        val entry=single.page(current)
        worker.execute {
            try {
                val target=File(File(cacheDir,"shares").apply{mkdirs()},"sayfa_"+current+".webp")
                assets.open(entry.assetName).use { stream ->
                    target.outputStream().use { output -> stream.copyTo(output) }
                }
                val uri=FileProvider.getUriForFile(this,packageName+".provider",target)
                runOnUiThread {
                    if(!isFinishing) startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
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
        wordActionMode?.finish()
        request.incrementAndGet()
        selectionRequest.incrementAndGet()
        worker.shutdownNow()
        bitmap?.let { if(!it.isRecycled)it.recycle() }
        super.onDestroy()
    }

    private inner class PageView(ctx:Context):View(ctx) {
        private var source:Bitmap?=null
        private var currentEntry=single.page(1)
        private var message:String?=null
        private var word:PageWord?=null
        private val imageRect=RectF()
        private var zoom=1f
        private var fitWholePage=true
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
                selectAt(e)
                return true
            }
            override fun onLongPress(e:MotionEvent) {
                selectAt(e)
            }
            override fun onScroll(e1:MotionEvent?,e2:MotionEvent,dx:Float,dy:Float):Boolean {
                // In fullscreen mode the page extends outside the viewport, so pan
                // even at zoom=1. Swipe-release still advances pages as before.
                if(zoom>1.01f || (!fitWholePage && width>height)) {panX-=dx;panY-=dy;invalidate();return true}
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
        private fun selectAt(event:MotionEvent) {
            val img=source?:return
            updateRect(img)
            if(imageRect.width()<=0f || imageRect.height()<=0f)return
            val x=(event.x-imageRect.left)/imageRect.width()
            val y=(event.y-imageRect.top)/imageRect.height()
            if(x in 0f..1f&&y in 0f..1f) {
                single.tapPosition(currentEntry,x,y)?.let { (sheetX,sheetY) ->
                    onTapWord(sheetX,sheetY)
                }
            }
        }
        fun wordRectangle(outRect:Rect) {
            val selected=word
            val bm=source
            if(selected==null||bm==null) {
                outRect.set(width/2-5,height/2-5,width/2+5,height/2+5)
                return
            }
            updateRect(bm)
            val r=single.highlight(currentEntry,selected)
            if(r==null) {
                outRect.set(width/2-5,height/2-5,width/2+5,height/2+5)
                return
            }
            outRect.set(
                (imageRect.left+r.left*imageRect.width()).toInt().coerceIn(0,width),
                (imageRect.top+r.top*imageRect.height()).toInt().coerceIn(0,height),
                (imageRect.left+r.right*imageRect.width()).toInt().coerceIn(0,width),
                (imageRect.top+r.bottom*imageRect.height()).toInt().coerceIn(0,height)
            )
        }
        fun highlightClear(){word=null;invalidate()}
        fun setPage(img:Bitmap,entry:SinglePageSource.Entry){source=img;currentEntry=entry;message=null;word=null;invalidate()}
        fun clearPage(){source=null;message=null;word=null;zoom=1f;fitWholePage=true;panX=0f;panY=0f;invalidate()}
        fun showError(msg:String){source=null;message=msg;invalidate()}
        fun highlight(w:PageWord){word=w;invalidate()}
        fun restoreFullscreen(){
            fitWholePage=true
            zoom=1f
            panX=0f
            panY=0f
            invalidate()
        }
        fun toggleFit(){
            fitWholePage=true
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
            // Always fit the ENTIRE original page within the safe content viewport.
            // On any orientation, the first and final text rows remain visible.
            // Pinch/zoom buttons allow detail magnification without losing pixels.
            val baseScale=min(width.toFloat()/b.width,height.toFloat()/b.height)
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
                    single.highlight(currentEntry,w)?.let { position ->
                        val r=RectF(
                            imageRect.left+position.left*imageRect.width(),
                            imageRect.top+position.top*imageRect.height(),
                            imageRect.left+position.right*imageRect.width(),
                            imageRect.top+position.bottom*imageRect.height())
                        c.drawRoundRect(r,5f,5f,fill)
                        c.drawRoundRect(r,5f,5f,line)
                    }
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
