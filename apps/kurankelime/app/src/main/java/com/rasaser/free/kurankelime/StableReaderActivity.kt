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
    private val selectableLayer by lazy { SelectableTextLayer(applicationContext) }
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
    private lateinit var selectionBar: LinearLayout
    private lateinit var selectionLabel: TextView
    private var page = 1 // The complete 1..605 Quran reading page
    private var readerControlsVisible = true
    private val green = Color.rgb(23,66,47)
    private var bitmap: Bitmap? = null
    private var selectedWord: BundledWordIndex.Selection? = null
    private var selectedRangeStart=0
    private var selectedRangeEnd=0
    private var selectedRangeLang:String?=null
    private var wordActionMode: ActionMode? = null
    private val translationDelegate = lazy { OnDeviceWordTranslator(this) }
    private val translation by translationDelegate

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
        pageView.onDragHandle = { handle,x,y -> extendWordSelection(handle,x,y) }
        pageView.onToggle = {
            readerControlsVisible = !readerControlsVisible
            selectedInfo.visibility=View.GONE
            if(::selectionBar.isInitialized) selectionBar.visibility=View.GONE
            selectedWord=null
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
        // An explicit selection bar, independent of Android's optional floating
        // ActionMode. It remains visible in immersive mode and on vendor devices.
        selectionBar=LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL
            gravity=Gravity.CENTER_VERTICAL
            setPadding(dp(8),dp(2),dp(4),dp(2))
            setBackgroundColor(0xFFF9F0DC.toInt())
            elevation=dp(6).toFloat()
            visibility=View.GONE
            contentDescription="Seçilen kelime işlemleri"
        }
        selectionLabel=TextView(this).apply {
            setTextColor(green)
            textSize=14f
            maxLines=1
            ellipsize=android.text.TextUtils.TruncateAt.END
            setPadding(dp(8),0,dp(6),0)
            contentDescription="Seçilen kelime"
        }
        selectionBar.addView(selectionLabel,LinearLayout.LayoutParams(0,dp(50),1f))
        fun selectionAction(icon:Int, title:String, action:()->Unit) {
            val btn=android.widget.ImageButton(this).apply {
                setImageResource(icon)
                imageTintList=android.content.res.ColorStateList.valueOf(green)
                scaleType=android.widget.ImageView.ScaleType.CENTER_INSIDE
                setPadding(dp(13),dp(13),dp(13),dp(13))
                background=android.graphics.drawable.GradientDrawable().apply {
                    setColor(0xFFF0E1C2.toInt())
                    cornerRadius=dp(13).toFloat()
                }
                isClickable=true
                isFocusable=true
                contentDescription=title
                setOnClickListener { action() }
            }
            selectionBar.addView(btn,LinearLayout.LayoutParams(dp(52),dp(48)).apply {
                leftMargin=dp(5)
            })
        }
        selectionAction(R.drawable.ic_word_copy,"Kopyala") {
            selectedWord?.let { selected ->
                (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                    .setPrimaryClip(ClipData.newPlainText("Kur’an kelimesi",selected.text))
                Toast.makeText(this,"Kelime kopyalandı",Toast.LENGTH_SHORT).show()
            }
            dismissWordSelection()
        }
        selectionAction(R.drawable.ic_word_translate,"Çevir") {
            selectedWord?.let { selected ->
                translation.translate(selected,page,selectedRangeStart,selectedRangeEnd)
            }
        }
        val closeSelection=label("×",21f).apply {
            contentDescription="Seçimi kapat"
            setOnClickListener { dismissWordSelection() }
        }
        selectionBar.addView(closeSelection,LinearLayout.LayoutParams(dp(40),dp(48)))
        val actionParams=FrameLayout.LayoutParams(-1,dp(58),Gravity.BOTTOM).apply {
            bottomMargin=dp(80)
        }
        main.addView(selectionBar,actionParams)
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
        if(::selectionBar.isInitialized)selectionBar.visibility=View.GONE
        selectedWord=null
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
        dismissWordSelection()
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
            // Primary: direct pixel coordinates of the VISIBLE 605-page image.
            val hit=try { selectableLayer.pick(current,x,y) } catch(_:Exception){null}
            // Legacy fallback, only for public code-only previews without the new private asset.
            val selected=if(hit!=null) hit.toSelection(sourcePage) else try {
                single.tapPosition(single.page(current),x,y)?.let { (sx,sy) ->
                    bundledIndex.selectNearest(sourcePage,sx,sy)
                        ?: pageIndex.nearestWord(sourcePage,sx,sy)?.let {
                            BundledWordIndex.Selection(it,it.gloss,"tr",100f)
                        }
                }
            } catch(_:Exception){null}
            runOnUiThread {
                if(isFinishing||current!=page||selectionRequest.get()!=selectSeq)return@runOnUiThread
                dismissWordSelection()
                selectedWord=selected
                if(selected!=null) {
                    if(hit!=null) {
                        selectedRangeStart=hit.id
                        selectedRangeEnd=hit.id
                        selectedRangeLang=hit.lang
                        pageView.highlightSpan(listOf(hit.rect),hit.lang=="ara")
                    } else pageView.highlight(selected.word)
                    hideWordInfo()
                    showWordSelectionMenu()
                } else {
                    pageView.highlightClear()
                    hideWordInfo()
                    // Do not put technical OCR error banners over Quran text.
                }
            }
        }
    }

    /** A handle may be dragged across words and lines. The original OCR word
     * order is preserved; only text within one script on the SAME page is copied.
     */
    private fun extendWordSelection(handle:Int,x:Float,y:Float) {
        val language=selectedRangeLang ?: return
        if(handle !in 1..2 || selectedRangeStart==0 || selectedRangeEnd==0)return
        val current=page
        val first=selectedRangeStart
        val last=selectedRangeEnd
        val seq=selectionRequest.incrementAndGet()
        worker.execute {
            val touched=try { selectableLayer.pick(current,x,y,language) }catch(_:Exception){null}
            if(touched==null)return@execute
            val newFirst=if(handle==1)touched.id else first
            val newLast=if(handle==2)touched.id else last
            val span=try { selectableLayer.range(current,newFirst,newLast,language) }
                catch(_:Exception){null} ?: return@execute
            val sourcePdf=single.page(current).sourcePdfPage
            runOnUiThread {
                if(isFinishing||isDestroyed||page!=current||selectionRequest.get()!=seq)return@runOnUiThread
                selectedRangeStart=newFirst
                selectedRangeEnd=newLast
                selectedWord=span.words.first().toSelection(sourcePdf).copy(text=span.text)
                pageView.highlightSpan(span.boxes,language=="ara")
                selectionLabel.text=span.text
                selectionBar.visibility=View.VISIBLE
            }
        }
    }

    private fun mark(word:PageWord,showMeaning:Boolean) {
        // Search navigation silently highlights the hit without opening a toolbar.
        pageView.highlight(word)
        hideWordInfo()
    }

    private fun dismissWordSelection() {
        if(::selectionBar.isInitialized) selectionBar.visibility=View.GONE
        selectedWord=null
        selectedRangeStart=0
        selectedRangeEnd=0
        selectedRangeLang=null
        if(::pageView.isInitialized) pageView.highlightClear()
        if(::viewportLayout.isInitialized && ::toolbar.isInitialized)
            applyReaderLayout()
    }

    private fun showWordSelectionMenu() {
        val selected=selectedWord ?: return
        if(selected.text.isBlank())return
        selectionLabel.text=selected.text
        selectionBar.visibility=View.VISIBLE
        val params=selectionBar.layoutParams as FrameLayout.LayoutParams
        params.bottomMargin=if(resources.configuration.orientation ==
            Configuration.ORIENTATION_LANDSCAPE && !readerControlsVisible)dp(24)
            else dp(80)
        selectionBar.layoutParams=params
        // Keep the actual Quran page readable; selection controls must not obscure it.
        viewportLayout.bottomMargin += dp(58)
        pageView.requestLayout()
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
        if(::selectionBar.isInitialized)selectionBar.visibility=View.GONE
        request.incrementAndGet()
        selectionRequest.incrementAndGet()
        worker.shutdownNow()
        if (translationDelegate.isInitialized()) translation.close()
        bitmap?.let { if(!it.isRecycled)it.recycle() }
        super.onDestroy()
    }

    private inner class PageView(ctx:Context):View(ctx) {
        private var source:Bitmap?=null
        private var currentEntry=single.page(1)
        private var message:String?=null
        private var word:PageWord?=null
        private var selectedRect:RectF?=null
        private var rangeBoxes:List<RectF> = emptyList()
        private var rangeRtl=false
        private var activeHandle=0
        private var lastHandleMove=0L
        private val handlePaint=Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color=0xFF2585E6.toInt()
            style=Paint.Style.FILL
        }
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
        var onDragHandle:(Int,Float,Float)->Unit={ _,_,_-> }
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
                onTapWord(x,y)
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
        fun highlightClear(){
            word=null;selectedRect=null;rangeBoxes=emptyList();activeHandle=0;invalidate()
        }
        fun highlightRectangle(rect:RectF){
            word=null;selectedRect=RectF(rect);rangeBoxes=emptyList();invalidate()
        }
        fun highlightSpan(boxes:List<RectF>,rtl:Boolean){
            word=null;selectedRect=null
            rangeBoxes=boxes.map { RectF(it) }
            rangeRtl=rtl
            invalidate()
        }
        fun setPage(img:Bitmap,entry:SinglePageSource.Entry){
            source=img;currentEntry=entry;message=null;highlightClear()
        }
        fun clearPage(){
            source=null;message=null;zoom=1f;fitWholePage=true;panX=0f;panY=0f;highlightClear()
        }
        fun showError(msg:String){source=null;message=msg;invalidate()}
        fun highlight(w:PageWord){word=w;selectedRect=null;rangeBoxes=emptyList();invalidate()}
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
        private fun boxOnScreen(b:RectF):RectF = RectF(
            imageRect.left+b.left*imageRect.width(),
            imageRect.top+b.top*imageRect.height(),
            imageRect.left+b.right*imageRect.width(),
            imageRect.top+b.bottom*imageRect.height()
        )

        private fun handleLocation(which:Int):Pair<Float,Float>? {
            if(rangeBoxes.isEmpty())return null
            val src=if(which==1)rangeBoxes.first() else rangeBoxes.last()
            val r=boxOnScreen(src)
            val hx=if(rangeRtl) {
                if(which==1)r.right else r.left
            } else {
                if(which==1)r.left else r.right
            }
            return hx to r.bottom
        }

        private fun hitHandle(x:Float,y:Float):Int {
            val b=source ?: return 0
            if(rangeBoxes.isEmpty())return 0
            updateRect(b)
            val radius=dp(31).toFloat()
            for(which in 1..2) {
                val (hx,hy)=handleLocation(which) ?: continue
                if((x-hx)*(x-hx)+(y-hy)*(y-hy)<radius*radius)return which
            }
            return 0
        }

        private fun dispatchDrag(handle:Int,e:MotionEvent){
            val b=source ?: return
            updateRect(b)
            if(imageRect.width()<=0f || imageRect.height()<=0f)return
            val nx=((e.x-imageRect.left)/imageRect.width()).coerceIn(0f,1f)
            val ny=((e.y-imageRect.top)/imageRect.height()).coerceIn(0f,1f)
            onDragHandle(handle,nx,ny)
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
                selectedRect?.let { region ->
                    val r=RectF(
                        imageRect.left+region.left*imageRect.width(),
                        imageRect.top+region.top*imageRect.height(),
                        imageRect.left+region.right*imageRect.width(),
                        imageRect.top+region.bottom*imageRect.height()
                    )
                    c.drawRoundRect(r,5f,5f,fill)
                    c.drawRoundRect(r,5f,5f,line)
                }
                rangeBoxes.forEach { box ->
                    val rect=boxOnScreen(box)
                    c.drawRoundRect(rect,5f,5f,fill)
                    c.drawRoundRect(rect,5f,5f,line)
                }
                if(rangeBoxes.isNotEmpty()) {
                    for(which in 1..2) {
                        val (hx,hy)=handleLocation(which) ?: continue
                        c.drawCircle(hx,hy,dp(7).toFloat(),handlePaint)
                        c.drawCircle(hx,hy+dp(12),dp(5).toFloat(),handlePaint)
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
            // Selection handles take priority over swipe-to-next-page and pan/zoom.
            if(e.actionMasked==MotionEvent.ACTION_DOWN){
                activeHandle=hitHandle(e.x,e.y)
                lastHandleMove=0L
                if(activeHandle!=0)return true
            }
            if(activeHandle!=0){
                when(e.actionMasked){
                    MotionEvent.ACTION_MOVE->{
                        if(e.eventTime-lastHandleMove>=90L){
                            lastHandleMove=e.eventTime
                            dispatchDrag(activeHandle,e)
                        }
                    }
                    MotionEvent.ACTION_UP->{
                        dispatchDrag(activeHandle,e)
                        activeHandle=0
                    }
                    MotionEvent.ACTION_CANCEL->activeHandle=0
                }
                return true
            }
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
