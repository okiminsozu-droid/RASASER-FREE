package com.rasaser.free.navigasyon

import android.Manifest
import android.app.DownloadManager
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.SystemClock
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.compose.material3.LinearProgressIndicator
import java.util.Calendar

private val Sand=Color(0xFFE9D9BB)
private val DeepSand=Color(0xFFD1AF7C)
private val Ink=Color(0xFF302E2A)
private val Panel=Color(0xFAFFFFFF)
private enum class NavScreen { HOME, DRIVE }

/**
 * v0.3 development build: offline Türkiye map display, device-side GPS camera,
 * download+verified install, on-device voice where supported, six localized UIs.
 * Navigation directions, speed-limit graph and live radar remain future modules.
 */
class MainActivity: ComponentActivity() {
    private var screen by mutableStateOf(NavScreen.HOME)
    private var language by mutableStateOf(NavLanguage.TR)
    private var onlineAllowed by mutableStateOf(true)
    private var wifiOnly by mutableStateOf(true)
    private var autoDrive by mutableStateOf(true)
    private var voiceEnabled by mutableStateOf(true)
    private var showSettings by mutableStateOf(false)
    private var showAlerts by mutableStateOf(false)
    private var showPermissions by mutableStateOf(false)
    private var showLocationHelp by mutableStateOf(false)
    private var recenterRequest by mutableIntStateOf(0)
    private var showWifiDialog by mutableStateOf(false)
    private var mapReady by mutableStateOf(false)
    private var checkingMap by mutableStateOf(true)
    private var installing by mutableStateOf(false)
    private var connection by mutableStateOf(Connection.OFFLINE)
    private var gpsFix by mutableStateOf<Location?>(null)
    private var downloadId by mutableLongStateOf(-1L)
    private var downloadPercent by mutableIntStateOf(0)
    private var allPackagesRequested by mutableStateOf(false)
    private var bundleWaiting by mutableStateOf(false)
    private var bundleStep by mutableIntStateOf(0)
    private var bundlePercent by mutableIntStateOf(0)
    private var bundleFailure by mutableStateOf<String?>(null)
    private var bundleRefreshStarted=false
    private var mapR2Fallback by mutableStateOf(false)
    private var routingR2Fallback by mutableStateOf(false)
    private var searchR2Fallback by mutableStateOf(false)
    private var routingDownloadId by mutableLongStateOf(-1L)
    private var routingInstallActive by mutableStateOf(false)
    private var routingMessage by mutableStateOf<RoutingDownloadStatus?>(null)
    private var showRoutingPrompt by mutableStateOf(false)
    private var statusKey by mutableStateOf<TextKey?>(null)
    private var statusExtra by mutableStateOf<String?>(null)
    private var selectedPlace by mutableStateOf<String?>(null)
    private var routePlan by mutableStateOf<RoutePlan?>(null)
    private var routeInfo by mutableStateOf<DriveInfo?>(null)
    private var routeSnapshot by mutableStateOf<GuidanceSnapshot?>(null)
    private var activeGuidance:RouteGuidance?=null
    private var routeDestination:NavPoint?=null
    private var routeRequestGeneration=0
    private var routeRequestRunning=false
    private var arrivalAnnounced=false
    private var resumeSavedRoute=false
    private var postedSpeedLimit by mutableStateOf<RoadSpeedLimit?>(null)
    private var roadLimitUpdatedAtMs=0L
    private val speedCaution=SpeedCautionGate()
    private val tracePoints=ArrayDeque<NavPoint>()
    private var lastRoadScanAtMs=0L
    private var roadScanRunning=false
    private lateinit var placeSearch:OfflinePlaceSearch
    private lateinit var searchInstaller:TurkeySearchInstaller
    private var searchDownloadId by mutableLongStateOf(-1L)
    private var searchInstalling by mutableStateOf(false)
    private var searchReady by mutableStateOf(false)
    private var searchAutoAttempted=false
    private var pendingSearchSpeech:String?=null
    private var searchFailureCode:String?=null
    private var searchNotice by mutableStateOf<SearchNotice?>(null)
    private var searchQuery by mutableStateOf("")
    private var searchBusy by mutableStateOf(false)
    private var searchResults by mutableStateOf<List<OfflinePlace>>(emptyList())
    private var showSearchDialog by mutableStateOf(false)
    private var showSearchResults by mutableStateOf(false)
    private var showSpeechConsent by mutableStateOf(false)
    private lateinit var network: DefaultNetworkMonitor
    private lateinit var gps: GpsTracker
    private lateinit var voice: VoiceController
    private lateinit var installer: TurkeyMapInstaller
    private lateinit var routingInstaller: TurkeyRoutingInstaller
    private lateinit var router:OfflineRouteEngine

    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs=getSharedPreferences("offline_first_settings",MODE_PRIVATE)
        language=NavLanguage.fromTag(prefs.getString("lang","tr") ?: "tr")
        onlineAllowed=prefs.getBoolean("online_allowed",true)
        wifiOnly=prefs.getBoolean("wifi_only",true)
        autoDrive=prefs.getBoolean("auto_drive",true)
        voiceEnabled=prefs.getBoolean("voice_enabled",true)
        if(prefs.contains("saved_destination_lat") &&
            prefs.contains("saved_destination_lon")) {
            routeDestination=NavPoint(
                Double.fromBits(prefs.getLong("saved_destination_lat",0L)),
                Double.fromBits(prefs.getLong("saved_destination_lon",0L)))
            selectedPlace=prefs.getString("saved_destination_name",null)
            resumeSavedRoute=true
        }
        downloadId=prefs.getLong("map_download_id",-1L)
        routingDownloadId=prefs.getLong("routing_download_id",-1L)
        searchDownloadId=prefs.getLong("place_search_download_id",-1L)
        network=DefaultNetworkMonitor(this){ connection=it }
        gps=GpsTracker(this){ onGpsPosition(it) }
        voice=VoiceController(this, { key,extra ->
            statusKey=key
            statusExtra=extra
            if(key==TextKey.VOICE_ERROR)
                showSearchDialog=true
        }, { phrase -> handleVoiceNavigation(phrase) })
            .also { it.init(); it.setEnabled(voiceEnabled); it.setLanguage(language) }
        installer=TurkeyMapInstaller(this)
        routingInstaller=TurkeyRoutingInstaller(this)
        router=OfflineRouteEngine(this)
        placeSearch=OfflinePlaceSearch(this)
        searchInstaller=TurkeySearchInstaller(this)
        searchReady=placeSearch.installed()
        // Reuse the already-downloaded archive after a background interruption.
        if(!searchReady &&
            (searchInstaller.bundledAvailable() || searchInstaller.cachedZipExists())){
            searchInstalling=true
            Thread {
                val ok=if(searchInstaller.bundledAvailable())
                    searchInstaller.installBundled()
                else searchInstaller.installWithResult().installed
                runOnUiThread {
                    searchInstalling=false
                    searchReady=ok || placeSearch.installed()
                    searchFailureCode=if(searchReady)null else searchInstaller.lastFailure().name
                    searchNotice=if(searchReady)null else SearchNotice.DOWNLOAD_ERROR
                    if(searchReady)pendingSearchSpeech?.let { phrase->
                        pendingSearchSpeech=null
                        startDestinationSearch(phrase)
                    }
                }
            }.start()
        }

        setContent {
            MaterialTheme {
                val requestPermissions=rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) {
                    gps.start()
                    showPermissions=true
                }
                val requestAudioPermission=rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted ->
                    if(!granted)showSearchDialog=true
                    else if(voice.canRecognizeOffline())voice.listenOffline()
                    else if(voice.canRecognizeSystem()) {
                        if(prefs.getBoolean("system_speech_consent",false))
                            voice.listenSystemWithConsent()
                        else showSpeechConsent=true
                    } else showSearchDialog=true
                }
                val requestLocationPermission=rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) {
                    if(gps.hasPermission())gps.locateNow()
                    if(!gps.hasPermission() || !gps.providersEnabled())showLocationHelp=true
                }
                val importPicker=rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument()
                ) { uri ->
                    if (uri!=null) installInThread {
                        contentResolver.openInputStream(uri)?.let { installer.importFrom(it) } ?: false
                    }
                }
                // Persistent WorkManager state, also observed after activity recreation.
                LaunchedEffect(Unit) {
                    WorkManager.getInstance(this@MainActivity)
                        .getWorkInfosForUniqueWorkFlow(TurkeyCountryPackWorker.UNIQUE)
                        .collectLatest { states ->
                            val info=states.firstOrNull { !it.state.isFinished }
                                ?: states.lastOrNull() ?: return@collectLatest
                            bundleStep=info.progress.getInt(TurkeyCountryPackWorker.STAGE,0)
                            bundlePercent=info.progress.getInt(TurkeyCountryPackWorker.PERCENT,0)
                            bundleWaiting=info.state==WorkInfo.State.ENQUEUED ||
                                info.state==WorkInfo.State.BLOCKED
                            allPackagesRequested=bundleWaiting || info.state==WorkInfo.State.RUNNING
                            bundleFailure=if(info.state==WorkInfo.State.FAILED)
                                info.outputData.getString(TurkeyCountryPackWorker.ERROR) ?: "download"
                                else null
                            if(info.state==WorkInfo.State.SUCCEEDED && !bundleRefreshStarted) {
                                bundleRefreshStarted=true
                                bundleStep=TurkeyCountryPackWorker.STAGE_DONE
                                bundlePercent=100
                                Thread {
                                    val map=LocalMapVerifier.turkeyMapReady(this@MainActivity)
                                    val search=placeSearch.installed()
                                    runOnUiThread {
                                        mapReady=map
                                        searchReady=search
                                        if(map) statusKey=TextKey.MAP_INSTALLED
                                        if(search) pendingSearchSpeech?.let { phrase ->
                                            pendingSearchSpeech=null
                                            startDestinationSearch(phrase)
                                        }
                                    }
                                }.start()
                            }
                        }
                }
                LaunchedEffect(downloadId) {
                    val id=downloadId
                    if(id>=0L) {
                        while(downloadId==id) {
                            val transfer=installer.transfer(id)
                            if(transfer.total>0L) downloadPercent=(100L*transfer.downloaded/transfer.total)
                                .toInt().coerceIn(0,100)
                            if(transfer.status==DownloadManager.STATUS_SUCCESSFUL) {
                                prefs.edit().remove("map_download_id").apply()
                                downloadId=-1L
                                installInThread { installer.installDownloaded() }
                                break
                            }
                            if(transfer.status==DownloadManager.STATUS_FAILED) {
                                prefs.edit().remove("map_download_id").apply()
                                downloadId=-1L
                                if (!mapR2Fallback) {
                                    mapR2Fallback=true
                                    try {
                                        downloadId=installer.download(if(allPackagesRequested) true else wifiOnly, false)
                                        prefs.edit().putLong("map_download_id", downloadId).apply()
                                    } catch (_:Exception) {statusKey=TextKey.MAP_ERROR}
                                } else statusKey=TextKey.MAP_ERROR
                                break
                            }
                            delay(1400)
                        }
                    }
                }
                LaunchedEffect(routingDownloadId) {
                    val current=routingDownloadId
                    if(current>=0L) {
                        while(routingDownloadId==current) {
                            when(routingInstaller.transferStatus(current)) {
                                DownloadManager.STATUS_SUCCESSFUL -> {
                                    prefs.edit().remove("routing_download_id").apply()
                                    routingDownloadId=-1L
                                    routingInstallActive=true
                                    routingMessage=RoutingDownloadStatus.VERIFYING
                                    Thread {
                                        val ok=routingInstaller.installDownloaded()
                                        runOnUiThread {
                                            routingInstallActive=false
                                            routingMessage=if(ok)RoutingDownloadStatus.READY
                                                else RoutingDownloadStatus.ERROR
                                            if(ok && routeDestination!=null){
                                                val destination=routeDestination!!
                                                val now=gpsFix
                                                if(now!=null && !routeRequestRunning)
                                                    calculateRoute(NavPoint(now.latitude,now.longitude),destination)
                                                else resumeSavedRoute=true
                                            }
                                        }
                                    }.start()
                                    break
                                }
                                DownloadManager.STATUS_FAILED -> {
                                    prefs.edit().remove("routing_download_id").apply()
                                    routingDownloadId=-1L
                                    if (!routingR2Fallback) {
                                        routingR2Fallback=true
                                        try {
                                            routingDownloadId=routingInstaller.beginWifiDownload(false)
                                            prefs.edit().putLong("routing_download_id",routingDownloadId).apply()
                                            routingMessage=RoutingDownloadStatus.DOWNLOADING
                                        } catch (_:Exception) {routingMessage=RoutingDownloadStatus.ERROR}
                                    } else routingMessage=RoutingDownloadStatus.ERROR
                                    break
                                }
                                else -> { }
                            }
                            delay(1800)
                        }
                    }
                }
                LaunchedEffect(searchDownloadId) {
                    val current=searchDownloadId
                    if(current>=0L) {
                        while(searchDownloadId==current) {
                            when(searchInstaller.transfer(current).status) {
                                DownloadManager.STATUS_SUCCESSFUL -> {
                                    searchDownloadId=-1L
                                    prefs.edit().remove("place_search_download_id").apply()
                                    searchInstalling=true
                                    searchNotice=SearchNotice.VERIFYING
                                    Thread {
                                        val result=searchInstaller.installWithResult(current)
                                        runOnUiThread {
                                            searchInstalling=false
                                            searchReady=result.installed || placeSearch.installed()
                                            searchFailureCode=if(searchReady)null else result.stage.name
                                            searchNotice=if(searchReady)null else SearchNotice.DOWNLOAD_ERROR
                                            if(searchReady)pendingSearchSpeech?.let { phrase->
                                                pendingSearchSpeech=null
                                                startDestinationSearch(phrase)
                                            }
                                        }
                                    }.start()
                                    break
                                }
                                DownloadManager.STATUS_FAILED -> {
                                    searchDownloadId=-1L
                                    prefs.edit().remove("place_search_download_id").apply()
                                    if (!searchR2Fallback) {
                                        searchR2Fallback=true
                                        try {
                                            searchDownloadId=searchInstaller.beginWifiDownload(false)
                                            prefs.edit().putLong("place_search_download_id",searchDownloadId).apply()
                                            searchNotice=SearchNotice.DOWNLOADING
                                        } catch (_:Exception) {
                                            searchNotice=SearchNotice.DOWNLOAD_ERROR
                                            searchFailureCode="TRANSFER_START"
                                        }
                                    } else {
                                        searchNotice=SearchNotice.DOWNLOAD_ERROR
                                        searchFailureCode="TRANSFER_"+searchInstaller.transfer(current).reason
                                    }
                                    break
                                }
                                else ->Unit
                            }
                            delay(1800)
                        }
                    }
                }
                LaunchedEffect(screen) {
                    if(screen==NavScreen.DRIVE)
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    while(screen==NavScreen.DRIVE) {
                        val now=SystemClock.elapsedRealtime()
                        if(postedSpeedLimit!=null && now-roadLimitUpdatedAtMs>25_000L)
                            postedSpeedLimit=null
                        if(gpsFix!=null && !gpsFix!!.isValidNavigationFix(30_000L))
                            gpsFix=null
                        delay(5_000L)
                    }
                }
                val currentDecision=OfflineFirstPolicy.decide(mapReady,connection,onlineAllowed)
                val t:(TextKey)->String={L10n.get(language,it)}
                fun beginDownload() {
                    try {
                        downloadId=installer.download(wifiOnly)
                        prefs.edit().putLong("map_download_id",downloadId).apply()
                        downloadPercent=0
                        statusKey=TextKey.MAP_DOWNLOADING
                    } catch (_:Exception) {
                        statusKey=TextKey.STORAGE_REQUIRED
                    }
                }
                fun requestAllPackages() {
                    if(allPackagesRequested)return
                    try {
                        TurkeyCountryPackWorker.enqueue(this@MainActivity,wifiOnly)
                        allPackagesRequested=true
                        bundleWaiting=true
                        bundleStep=TurkeyCountryPackWorker.STAGE_WAIT
                        bundleFailure=null
                        bundleRefreshStarted=false
                        statusKey=null
                    } catch (_:Exception) {
                        bundleFailure="start"
                        allPackagesRequested=false
                    }
                }
                fun loadMap() {
                    // This optional path imports USB files only; network uses WorkManager.
                    importPicker.launch(arrayOf("application/zip","application/octet-stream",
                        "application/x-zip-compressed"))
                }
                fun startDrive() {
                    if(!mapReady) {statusKey=TextKey.NO_MAP;return}
                    prefs.edit().putBoolean("drive_started",true).apply()
                    screen=NavScreen.DRIVE
                    gps.start()
                }
                fun askPermissions() {
                    val requested=mutableListOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.RECORD_AUDIO
                    )
                    if(Build.VERSION.SDK_INT>=33) requested+=Manifest.permission.POST_NOTIFICATIONS
                    requestPermissions.launch(requested.toTypedArray())
                }

                if(screen==NavScreen.DRIVE && mapReady) {
                    Box(Modifier.fillMaxSize().background(Color(0xFFEEEDE8))) {
                        OfflineDrivingMap(gpsFix, routePlan?.shape,language,recenterRequest) { name, _,destination ->
                            selectDestination(name.ifBlank {DriveWords.get(language,DriveInfo.TARGET_SELECTED)},destination)
                        }
                        // The actual GPS marker is rendered by MapLibre at its
                        // latitude/longitude. No false fixed-center car icon.
                        Column(Modifier.align(Alignment.TopStart).safeDrawingPadding().padding(12.dp),
                            verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            Card(colors=CardDefaults.cardColors(containerColor=Panel),
                                shape=RoundedCornerShape(18.dp)) {
                                Column(Modifier.padding(horizontal=16.dp,vertical=10.dp)) {
                                    Text(t(TextKey.SPEED),color=Ink,fontSize=12.sp)
                                    val speed=gpsFix?.reliableSpeedKmh()
                                    Text(if(speed==null) "— km/h" else "$speed km/h",
                                        color=Ink,fontWeight=FontWeight.Bold,fontSize=22.sp)
                                    val currentLimit=postedSpeedLimit
                                    if(currentLimit!=null) Text(
                                        GuidanceWords.text(language,GuidanceLabel.SPEED_LIMIT)+": "+
                                            currentLimit.kmh+" km/h",
                                        color=Color.DarkGray,fontSize=11.sp)
                                }
                            }
                            routeSnapshot?.let { guidance ->
                                if(guidance.hazards.isNotEmpty()) {
                                    Card(colors=CardDefaults.cardColors(containerColor=Panel),
                                        shape=RoundedCornerShape(18.dp)) {
                                        Column(Modifier.padding(12.dp)) {
                                            for(hazard in guidance.hazards) {
                                                Text("⚠ " + HazardWords.text(language,hazard),
                                                    color=Color(0xFF8C4C25),fontSize=13.sp)
                                            }
                                        }
                                    }
                                }
                                if (routePlan!=null && !guidance.arrived && guidance.nextInstruction!=null) {
                                    Card(colors=CardDefaults.cardColors(containerColor=Panel),
                                        shape=RoundedCornerShape(18.dp)) {
                                        Column(Modifier.padding(14.dp),
                                            verticalArrangement=Arrangement.spacedBy(4.dp)) {
                                            Text(GuidanceWords.text(language,GuidanceLabel.NEXT_TURN),
                                                fontWeight=FontWeight.Bold,color=Ink,fontSize=12.sp)
                                            Text(guidance.nextInstruction,color=Ink,fontSize=14.sp)
                                            guidance.nextTurnMeters?.let { meters ->
                                                Text("${meters.toInt()} m",color=Color(0xFF186AA2),fontSize=17.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if(selectedPlace!=null) {
                            Card(Modifier.align(Alignment.BottomCenter).padding(bottom=160.dp, start=12.dp,end=12.dp),
                                colors=CardDefaults.cardColors(containerColor=Panel)) {
                                Column(Modifier.padding(15.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                                    Text(selectedPlace.orEmpty(),color=Ink,fontWeight=FontWeight.Bold)
                                    routeInfo?.let { Text(DriveWords.get(language,it),color=Ink,fontSize=12.sp) }
                                    routePlan?.let {
                                        val remaining=(routeSnapshot?.remainingMeters ?: it.meters)/1000.0
                                        Text("${"%.1f".format(remaining)} km · ${(it.seconds/60.0).toInt()} min",
                                            color=Ink,fontSize=13.sp)
                                    }
                                }
                            }
                        }
                        Button(
                            onClick={
                                when {
                                    !gps.hasPrecisePermission() -> requestLocationPermission.launch(arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION))
                                    !gps.providersEnabled() ->showLocationHelp=true
                                    else ->{
                                        gps.locateNow()
                                        if(gpsFix!=null) recenterRequest++
                                        else showLocationHelp=true
                                    }
                                }
                            },
                            modifier=Modifier.align(Alignment.BottomEnd).safeDrawingPadding()
                                .padding(end=12.dp,bottom=88.dp).height(55.dp),
                            colors=ButtonDefaults.buttonColors(containerColor=Panel,contentColor=Ink),
                            shape=RoundedCornerShape(17.dp)
                        ){
                            Icon(Icons.Default.MyLocation,contentDescription=null,
                                modifier=Modifier.size(23.dp),tint=Color(0xFF157BC4))
                            Spacer(Modifier.width(6.dp))
                            Text(DriveLocationText.get(language,DriveLocationWord.MY_LOCATION),
                                fontSize=13.sp,fontWeight=FontWeight.SemiBold)
                        }
                        Row(
                            Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                                .safeDrawingPadding().padding(horizontal=12.dp,vertical=10.dp),
                            horizontalArrangement=Arrangement.spacedBy(10.dp)
                        ) {
                            DriveButton(Icons.Default.Settings,t(TextKey.SETTINGS),Modifier.weight(1f)){showSettings=true}
                            DriveButton(Icons.Default.Mic,t(TextKey.MICROPHONE),Modifier.weight(1f)){
                                when {
                                    checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=
                                        PackageManager.PERMISSION_GRANTED ->
                                        requestAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
                                    voice.canRecognizeOffline() ->voice.listenOffline()
                                    voice.canRecognizeSystem() -> {
                                        if(prefs.getBoolean("system_speech_consent",false))
                                            voice.listenSystemWithConsent()
                                        else showSpeechConsent=true
                                    }
                                    else ->showSearchDialog=true
                                }
                            }
                            DriveButton(Icons.Default.Warning,t(TextKey.ALERTS),Modifier.weight(1f)){
                                showAlerts=true
                            }
                        }
                        Text("© OpenStreetMap · BBBike",
                            modifier=Modifier.align(Alignment.BottomStart)
                                .padding(start=12.dp,bottom=96.dp),fontSize=9.sp,color=Ink)
                    }
                } else {
                    BoxWithConstraints(Modifier.fillMaxSize().background(Sand).safeDrawingPadding()) {
                        val landscape=maxWidth>maxHeight
                        Column(Modifier.align(Alignment.TopEnd).padding(top=12.dp,end=18.dp), horizontalAlignment=Alignment.End) {
                            var expanded by androidx.compose.runtime.remember { mutableStateOf(false) }
                            Button(onClick={expanded=true},
                                colors=ButtonDefaults.buttonColors(containerColor=Color.White),
                                shape=RoundedCornerShape(14.dp)) {
                                Text(language.nativeName+"  ▾",color=Ink,fontSize=14.sp)
                            }
                            DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
                                for (option in NavLanguage.entries) {
                                    DropdownMenuItem(text={Text(option.nativeName)},
                                        onClick={
                                            language=option
                                            prefs.edit().putString("lang",option.tag).apply()
                                            voice.setLanguage(option)
                                            expanded=false
                                        })
                                }
                            }
                        }
                        val content: @Composable ()->Unit={
                            Column(
                                verticalArrangement=Arrangement.spacedBy(if(landscape) 12.dp else 20.dp),
                                horizontalAlignment=Alignment.CenterHorizontally
                            ) {
                                Text("RaSaSeR",color=Ink,fontSize=if(landscape)30.sp else 43.sp,
                                    fontWeight=FontWeight.Bold)
                                Text(t(TextKey.TITLE),color=Ink,fontSize=if(landscape)18.sp else 22.sp,
                                    fontWeight=FontWeight.SemiBold,textAlign=TextAlign.Center)

                                Spacer(Modifier.height(8.dp))
                                HomeAction(Icons.Default.Map,
                                    TurkeyPackageUi.button(language,mapReady && router.installed() && searchReady),
                                    enabled=!allPackagesRequested && !installing && downloadId<0L &&
                                        routingDownloadId<0L && searchDownloadId<0L &&
                                        !(mapReady && router.installed() && searchReady)
                                ) {requestAllPackages()}
                                if(allPackagesRequested)
                                    Text(TurkeyPackageUi.workerStatus(language,bundleStep,bundlePercent,bundleWaiting),
                                        color=Ink,fontSize=12.sp,textAlign=TextAlign.Center)
                                if(allPackagesRequested) LinearProgressIndicator(
                                    progress={bundlePercent/100f},modifier=Modifier.fillMaxWidth())
                                bundleFailure?.let {
                                    Text(TurkeyPackageUi.error(language),
                                        color=Color(0xFF9A3F35),fontSize=12.sp)
                                }
                                HomeAction(Icons.Default.LocationOn,t(TextKey.GRANT_PERMISSIONS)){askPermissions()}
                                HomeAction(Icons.Default.PlayArrow,t(TextKey.START),enabled=mapReady){startDrive()}
                                val detail=when {
                                    installing -> t(TextKey.MAP_VERIFYING)
                                    downloadId>=0L -> t(TextKey.MAP_DOWNLOADING)+" • $downloadPercent%"
                                    checkingMap -> t(TextKey.MAP_VERIFYING)
                                    mapReady -> t(TextKey.MAP_READY)
                                    else -> t(TextKey.NO_MAP)
                                }
                                if(installing || downloadId>=0L || checkingMap)
                                     Text(detail,color=Ink,fontSize=13.sp,textAlign=TextAlign.Center)
                                statusKey?.let {
                                    Text(t(it)+(statusExtra?.let{suffix->": $suffix"} ?: ""),
                                        color=Color(0xFF67462B),fontSize=12.sp,
                                        textAlign=TextAlign.Center)
                                }
                            }
                        }
                        if(landscape) {
                            Row(Modifier.fillMaxSize().padding(start=26.dp,end=26.dp,top=46.dp,bottom=18.dp),
                                verticalAlignment=Alignment.CenterVertically) {
                                Box(Modifier.weight(1f),contentAlignment=Alignment.Center) {
                                    Column(horizontalAlignment=Alignment.CenterHorizontally) {
                                        Text("RaSaSeR",color=Ink,fontSize=40.sp,fontWeight=FontWeight.Bold)
                                        Text(t(TextKey.TITLE),color=Ink,fontSize=18.sp)

                                    }
                                }
                                Box(Modifier.weight(1f),contentAlignment=Alignment.Center) {
                                    Column(horizontalAlignment=Alignment.CenterHorizontally,
                                        verticalArrangement=Arrangement.spacedBy(10.dp)) {
                                        HomeAction(Icons.Default.Map,
                                            TurkeyPackageUi.button(language,mapReady && router.installed() && searchReady),
                                            enabled=!allPackagesRequested && !installing && downloadId<0L &&
                                                routingDownloadId<0L && searchDownloadId<0L &&
                                                !(mapReady && router.installed() && searchReady)
                                        ){requestAllPackages()}
                                        if(allPackagesRequested)
                                            Text(TurkeyPackageUi.workerStatus(language,bundleStep,bundlePercent,bundleWaiting),
                                                fontSize=11.sp,color=Ink)
                                        if(allPackagesRequested) LinearProgressIndicator(
                                            progress={bundlePercent/100f},modifier=Modifier.fillMaxWidth())
                                        bundleFailure?.let {
                                            Text(TurkeyPackageUi.error(language),
                                                color=Color(0xFF9A3F35),fontSize=11.sp)
                                        }
                                        HomeAction(Icons.Default.LocationOn,t(TextKey.GRANT_PERMISSIONS)){askPermissions()}
                                        HomeAction(Icons.Default.PlayArrow,t(TextKey.START),enabled=mapReady){startDrive()}
                                        if(installing || downloadId>=0L || checkingMap)
                                             Text(when{
                                                 installing || checkingMap ->t(TextKey.MAP_VERIFYING)
                                                 else ->t(TextKey.MAP_DOWNLOADING)+" $downloadPercent%"
                                             },fontSize=11.sp,color=Ink)
                                    }
                                }
                            }
                        } else {
                            Box(Modifier.fillMaxSize().padding(horizontal=25.dp,vertical=72.dp),
                                contentAlignment=Alignment.Center) { content() }
                        }
                    }
                }

                if(showRoutingPrompt) AlertDialog(
                    onDismissRequest={showRoutingPrompt=false},
                    title={Text(RoutingDownloadText.get(language,RoutingDownloadStatus.BUTTON))},
                    text={Text(RoutingDownloadText.get(language,RoutingDownloadStatus.EXPLANATION))},
                    confirmButton={TextButton(
                        enabled=!allPackagesRequested,
                        onClick={
                            showRoutingPrompt=false
                            requestAllPackages()
                        }
                    ){Text(RoutingDownloadText.get(language,RoutingDownloadStatus.BUTTON))}},
                    dismissButton={TextButton(onClick={showRoutingPrompt=false}){
                        Text(t(TextKey.BACK))
                    }}
                )
                if(showSpeechConsent) AlertDialog(
                    onDismissRequest={showSpeechConsent=false;showSearchDialog=true},
                    title={Text(SystemSpeechWords.get(language,SystemSpeechWord.TITLE))},
                    text={Text(SystemSpeechWords.get(language,SystemSpeechWord.DESCRIPTION))},
                    confirmButton={TextButton(onClick={
                        showSpeechConsent=false
                        prefs.edit().putBoolean("system_speech_consent",true).apply()
                        voice.listenSystemWithConsent()
                    }){Text(SystemSpeechWords.get(language,SystemSpeechWord.ALLOW))}},
                    dismissButton={TextButton(onClick={
                        showSpeechConsent=false
                        showSearchDialog=true
                    }){Text(SystemSpeechWords.get(language,SystemSpeechWord.TYPE_INSTEAD))}}
                )
                if(showSearchDialog) AlertDialog(
                    onDismissRequest={showSearchDialog=false},
                    title={Text(SearchWords.text(language,SearchNotice.TITLE))},
                    text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
                        TextField(
                            value=searchQuery,onValueChange={searchQuery=it},
                            label={Text(SearchWords.text(language,SearchNotice.ENTER_PLACE))},
                            singleLine=true,modifier=Modifier.fillMaxWidth()
                        )
                    }},
                    confirmButton={TextButton(onClick={
                        showSearchDialog=false
                        startDestinationSearch(searchQuery)
                    }){Text(SearchWords.text(language,SearchNotice.SEARCH))}},
                    dismissButton={TextButton(onClick={showSearchDialog=false}){Text(t(TextKey.BACK))}}
                )
                if(showSearchResults) AlertDialog(
                    onDismissRequest={showSearchResults=false},
                    title={Text(SearchWords.text(language,SearchNotice.RESULTS))},
                    text={Column(modifier=Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement=Arrangement.spacedBy(8.dp)){
                        if(!searchReady) {
                            OutlinedButton(onClick={
                                searchAutoAttempted=false
                                startPlaceSearchDownload()
                            },
                                enabled=connection==Connection.UNMETERED && onlineAllowed &&
                                    searchDownloadId<0 && !searchInstalling) {
                                Text(SearchWords.text(language,SearchNotice.DOWNLOAD))
                            }
                        } else if(searchResults.isEmpty()) {
                            Text(SearchWords.text(language,SearchNotice.NO_MATCH))
                        }
                        for(place in searchResults){
                            OutlinedButton(modifier=Modifier.fillMaxWidth(),onClick={
                                showSearchResults=false
                                selectDestination(place.name,NavPoint(place.lat,place.lon))
                            }){
                                Column(horizontalAlignment=Alignment.Start){
                                    Text(place.name,fontSize=13.sp)
                                    Text("${"%.4f".format(place.lat)}° · ${"%.4f".format(place.lon)}°",
                                    fontSize=10.sp)
                                }
                            }
                        }
                    }},
                    confirmButton={TextButton(onClick={showSearchResults=false}){Text(t(TextKey.BACK))}}
                )
                if(showLocationHelp) AlertDialog(
                    onDismissRequest={showLocationHelp=false},
                    title={Text(DriveLocationText.get(language,DriveLocationWord.MY_LOCATION))},
                    text={Text(DriveLocationText.get(language,
                        if(!gps.hasPrecisePermission())DriveLocationWord.GRANT_GPS
                        else if(!gps.providersEnabled())DriveLocationWord.GPS_DISABLED
                        else DriveLocationWord.GPS_PENDING))},
                    confirmButton={TextButton(onClick={
                        showLocationHelp=false
                        if(!gps.hasPrecisePermission())
                            requestLocationPermission.launch(arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION))
                        else startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    }){Text(DriveLocationText.get(language,DriveLocationWord.OPEN_GPS_SETTINGS))}},
                    dismissButton={TextButton(onClick={showLocationHelp=false}){
                        Text(t(TextKey.BACK))
                    }}
                )
                if(showWifiDialog) AlertDialog(onDismissRequest={showWifiDialog=false},
                    title={Text(t(TextKey.MAP_DOWNLOAD))},
                    text={Text(t(TextKey.MAP_DOWNLOAD_SIZE)+"\n"+t(TextKey.MAP_DOWNLOAD_EXPLAIN))},
                    confirmButton={TextButton(onClick={
                        showWifiDialog=false
                        // Requires explicit user selection to use mobile data
                        try { downloadId=installer.download(false)
                            prefs.edit().putLong("map_download_id",downloadId).apply() }
                        catch (_:Exception) {statusKey=TextKey.STORAGE_REQUIRED}
                    }){Text(t(TextKey.START))}},
                    dismissButton={TextButton(onClick={
                        showWifiDialog=false
                        importPicker.launch(arrayOf("application/zip","application/octet-stream"))
                    }){Text("USB")}}
                )
                if(showPermissions) AlertDialog(onDismissRequest={showPermissions=false},
                    title={Text(t(TextKey.PERMISSIONS_TITLE))},
                    text={
                        Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            Text(t(TextKey.PERMISSION_DETAILS))
                            PermissionLine(t(TextKey.LOCATION_PERMISSION),gps.hasPermission())
                            PermissionLine(t(TextKey.MICROPHONE_PERMISSION),
                                checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)
                            if(Build.VERSION.SDK_INT>=33) PermissionLine(t(TextKey.NOTIFICATION_PERMISSION),
                                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED)
                            val notifications=getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                            PermissionLine(t(TextKey.DND_PERMISSION),notifications.isNotificationPolicyAccessGranted)
                            OutlinedButton(onClick={startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))}) {
                                Text(t(TextKey.OPEN_DND_SETTINGS))
                            }
                        }
                    },
                    confirmButton={TextButton(onClick={showPermissions=false}){Text(t(TextKey.BACK))}}
                )
                if(showSettings) AlertDialog(onDismissRequest={showSettings=false},
                    title={Text(t(TextKey.SETTINGS))},
                    text={Column(
                        modifier=Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement=Arrangement.spacedBy(8.dp)
                    ){
                        if(!(mapReady && router.installed() && searchReady)) {
                            OutlinedButton(
                                onClick={requestAllPackages()},
                                enabled=!allPackagesRequested && downloadId<0L &&
                                    routingDownloadId<0L && searchDownloadId<0L
                            ) {Text(TurkeyPackageUi.button(language,false))}
                            if(!mapReady) {
                                OutlinedButton(onClick={loadMap()}) {
                                    Text(t(TextKey.MAP_DOWNLOAD)+" / USB")
                                }
                            }
                        }
                        if(!searchReady) {
                            OutlinedButton(
                                enabled=!allPackagesRequested,
                                onClick={requestAllPackages()}
                            ){Text(SearchWords.text(language,SearchNotice.DOWNLOAD))}
                        }
                        if(router.installed()){
                            Text(RoutingDownloadText.get(language,RoutingDownloadStatus.READY),
                                color=Color(0xFF266443),fontSize=12.sp)
                        } else {
                            OutlinedButton(
                                enabled=!allPackagesRequested,
                                onClick={requestAllPackages()}
                            ) {
                                Text(RoutingDownloadText.get(language,RoutingDownloadStatus.BUTTON))
                            }
                            if(connection!=Connection.UNMETERED){
                                Text(RoutingDownloadText.get(language,RoutingDownloadStatus.WIFI_ONLY),
                                    fontSize=12.sp)
                            }
                        }
                        SettingSwitch(t(TextKey.INTERNET_AUTO),onlineAllowed){
                            onlineAllowed=it; prefs.edit().putBoolean("online_allowed",it).apply()
                        }
                        SettingSwitch(t(TextKey.WIFI_ONLY),wifiOnly){
                            wifiOnly=it;prefs.edit().putBoolean("wifi_only",it).apply()
                        }
                        SettingSwitch(t(TextKey.AUTO_START),autoDrive){
                            autoDrive=it;prefs.edit().putBoolean("auto_drive",it).apply()
                        }
                        SettingSwitch(t(TextKey.MUTE),!voiceEnabled){
                            voiceEnabled=!it;voice.setEnabled(!it)
                            prefs.edit().putBoolean("voice_enabled",!it).apply()
                        }
                        if(routeDestination!=null) {
                            OutlinedButton(onClick={
                                clearCurrentDestination()
                                showSettings=false
                            }) {
                                Text(GuidanceWords.text(language,GuidanceLabel.CLEAR_TARGET))
                            }
                        }
                    }},
                    confirmButton={TextButton(onClick={showSettings=false}){Text(t(TextKey.BACK))}},
                    dismissButton={TextButton(onClick={showSettings=false;screen=NavScreen.HOME}){
                        Text(t(TextKey.HOME))
                    }}
                )
                if(showAlerts) AlertDialog(onDismissRequest={showAlerts=false},
                    title={Text(t(TextKey.ALERTS))},
                    text={Column(verticalArrangement=Arrangement.spacedBy(9.dp)){
                        routeSnapshot?.hazards?.forEach { hazard ->
                            Text(HazardWords.text(language,hazard),fontSize=13.sp)
                        }
                    }},
                    confirmButton={TextButton(onClick={showAlerts=false}){Text(t(TextKey.BACK))}}
                )
            }
        }
        Thread {
            val ready=LocalMapVerifier.turkeyMapReady(this)
            runOnUiThread {
                mapReady=ready
                checkingMap=false
                if (ready && (autoDrive || intent?.action == "com.rasaser.free.navigasyon.OPEN_DRIVE"))
                    screen=NavScreen.DRIVE
            }
        }.start()
    }

    private fun installInThread(operation:()->Boolean) {
        if(installing)return
        installing=true;statusKey=TextKey.MAP_VERIFYING
        Thread {
            val installed=runCatching {operation()}.getOrDefault(false)
            val ready=installed && LocalMapVerifier.turkeyMapReady(this)
            val existing=if(ready) true else LocalMapVerifier.turkeyMapReady(this)
            runOnUiThread {
                installing=false
                mapReady=existing
                statusKey=if(ready) TextKey.MAP_INSTALLED else TextKey.MAP_ERROR
                statusExtra=null
            }
        }.start()
    }

    override fun onNewIntent(intent:Intent) {
        super.onNewIntent(intent)
        if(intent.action == "com.rasaser.free.navigasyon.OPEN_DRIVE" && mapReady)
            screen=NavScreen.DRIVE
    }


    private fun clearCurrentDestination() {
        routeRequestGeneration++
        routeRequestRunning=false
        activeGuidance=null
        routePlan=null
        routeSnapshot=null
        routeDestination=null
        resumeSavedRoute=false
        selectedPlace=null
        routeInfo=null
        arrivalAnnounced=false
        getSharedPreferences("offline_first_settings",MODE_PRIVATE)
            .edit().remove("saved_destination_lat")
            .remove("saved_destination_lon")
            .remove("saved_destination_name").apply()
    }

    private fun startPlaceSearchDownload() {
        if(searchReady || allPackagesRequested)return
        try {
            TurkeyCountryPackWorker.enqueue(this,
                getSharedPreferences("offline_first_settings",MODE_PRIVATE)
                    .getBoolean("wifi_only",true))
            allPackagesRequested=true
            bundleWaiting=true
            bundleFailure=null
            bundleRefreshStarted=false
            searchNotice=SearchNotice.DOWNLOADING
        } catch (_:Exception) {
            searchNotice=SearchNotice.DOWNLOAD_ERROR
        }
    }

    private fun startPlaceSearchFresh() {
        if(searchReady || searchDownloadId>=0L || searchInstalling)return
        try {
            searchDownloadId=searchInstaller.beginWifiDownload()
            getSharedPreferences("offline_first_settings",MODE_PRIVATE)
                .edit().putLong("place_search_download_id",searchDownloadId).apply()
            searchNotice=SearchNotice.DOWNLOADING
        }catch (_:Exception) {
            searchNotice=SearchNotice.DOWNLOAD_ERROR
            searchFailureCode="DOWNLOAD_START"
        }
    }

    private fun startDestinationSearch(transcript:String) {
        val target=DestinationSpeech.destination(language,transcript)
        if(target==null) return
        searchQuery=target
        if(!searchReady || !placeSearch.installed()) {
            pendingSearchSpeech=target
            searchNotice=SearchNotice.MISSING
            searchResults=emptyList()
            showSearchResults=true
            if(connection==Connection.UNMETERED && onlineAllowed)
                startPlaceSearchDownload()
            return
        }
        if(searchBusy)return
        searchBusy=true
        searchNotice=null
        val position=gpsFix?.let {NavPoint(it.latitude,it.longitude)}
        Thread {
            val places=placeSearch.find(target,position)
            runOnUiThread {
                searchBusy=false
                searchResults=places
                if(places.isEmpty()) {
                    searchNotice=SearchNotice.NO_MATCH
                    showSearchResults=true
                } else {
                    // Navigate automatically only when a target is clearly unique.
                    val first=places[0]
                    val allSameArea=places.all {candidate ->
                        PlaceQuery.normalize(candidate.name)==PlaceQuery.normalize(first.name) &&
                            RouteGuidance.metersBetween(
                                NavPoint(first.lat,first.lon),NavPoint(candidate.lat,candidate.lon))<1_500.0
                    }
                    val uniquelyRanked=places.size==1 || allSameArea ||
                        (first.score>=450.0 && first.score-(places.getOrNull(1)?.score ?: 0.0)>80.0)
                    if(uniquelyRanked) {
                        searchNotice=null
                        showSearchResults=false
                        selectDestination(first.name,NavPoint(first.lat,first.lon))
                    }else {
                        searchNotice=SearchNotice.SELECT_ONE
                        showSearchResults=true
                    }
                }
            }
        }.start()
    }

    private fun selectDestination(name:String,destination:NavPoint) {
        selectedPlace=name
        routeRequestGeneration++
        routePlan=null
        activeGuidance=null
        routeSnapshot=null
        routeRequestRunning=false
        routeDestination=destination
        resumeSavedRoute=false
        arrivalAnnounced=false
        getSharedPreferences("offline_first_settings",MODE_PRIVATE).edit()
            .putLong("saved_destination_lat",destination.lat.toBits())
            .putLong("saved_destination_lon",destination.lon.toBits())
            .putString("saved_destination_name",name).apply()
        val current=gpsFix
        if(current==null) {
            resumeSavedRoute=true
            routeInfo=DriveInfo.NEEDS_GPS
            gps.locateNow()
        } else if(!router.installed()) {
            routeInfo=DriveInfo.NEEDS_ROUTING_DATA
            showRoutingPrompt=true
        } else calculateRoute(NavPoint(current.latitude,current.longitude),destination)
    }

    private fun handleVoiceNavigation(spoken:String) {
        when(VoiceNavigationCommands.match(language,spoken)) {
            VoiceNavigationCommand.SETTINGS -> showSettings=true
            VoiceNavigationCommand.ALERTS -> showAlerts=true
            VoiceNavigationCommand.HOME -> {
                showSettings=false
                showAlerts=false
                screen=NavScreen.HOME
            }
            VoiceNavigationCommand.MUTE -> {
                voiceEnabled=false
                voice.setEnabled(false)
                getSharedPreferences("offline_first_settings",MODE_PRIVATE)
                    .edit().putBoolean("voice_enabled",false).apply()
            }
            VoiceNavigationCommand.UNMUTE -> {
                voiceEnabled=true
                voice.setEnabled(true)
                getSharedPreferences("offline_first_settings",MODE_PRIVATE)
                    .edit().putBoolean("voice_enabled",true).apply()
            }
            VoiceNavigationCommand.CLEAR_DESTINATION ->clearCurrentDestination()
            null -> startDestinationSearch(spoken)
        }
    }

    private fun calculateRoute(origin:NavPoint,destination:NavPoint) {
        if(routeRequestRunning) return
        val id=++routeRequestGeneration
        routeRequestRunning=true
        routeInfo=if(routePlan==null)DriveInfo.WORKING else DriveInfo.REROUTING
        val lang=language
        Thread {
            val computed=runCatching { router.route(origin,destination,lang) }.getOrNull()
            runOnUiThread {
                if(isDestroyed || id!=routeRequestGeneration)return@runOnUiThread
                routeRequestRunning=false
                routePlan=computed
                activeGuidance=computed?.let(::RouteGuidance)
                routeSnapshot=null
                routeInfo=if(computed!=null)DriveInfo.ROUTE_FOUND else DriveInfo.NO_ROUTE
                arrivalAnnounced=false
                if(computed!=null && voiceEnabled)
                    computed.maneuverInstructions.firstOrNull()?.let { voice.speak(it) }
            }
        }.start()
    }


    /**
     * Optional OSM speed limits, even without a destination.
     * Low-accuracy fixes, missing speed tags, stale matches and online requests
     * NEVER cause warnings. Work runs on a background thread.
     */
    private fun checkPostedSpeedLimit(location:Location) {
        val now=SystemClock.elapsedRealtime()
        if(postedSpeedLimit!=null && now-roadLimitUpdatedAtMs>25_000L)
            postedSpeedLimit=null

        val speed=location.reliableSpeedKmh()
        if(speedCaution.evaluate(postedSpeedLimit?.kmh,speed,location.accuracy,
                now-roadLimitUpdatedAtMs,now) &&
            voiceEnabled && (routeSnapshot?.nextTurnMeters ?: 10000.0)>150.0) {
            voice.speak(GuidanceWords.text(language,GuidanceLabel.SPEED_CAUTION)+
                ". "+GuidanceWords.text(language,GuidanceLabel.SPEED_LIMIT)+" "+
                (postedSpeedLimit?.kmh ?: 0)+" km/h")
        }

        if(!router.installed() || location.accuracy>25f || speed==null) return
        val current=NavPoint(location.latitude,location.longitude)
        if(tracePoints.isEmpty() || RouteGuidance.metersBetween(tracePoints.last(),current)>8.0)
            tracePoints.addLast(current)
        while(tracePoints.size>6)tracePoints.removeFirst()
        if(roadScanRunning || tracePoints.size<3 || now-lastRoadScanAtMs<12_000L)return
        roadScanRunning=true
        lastRoadScanAtMs=now
        val captured=tracePoints.toList()
        val accuracy=location.accuracy
        Thread {
            val matched=runCatching {
                router.matchedPostedSpeedLimit(captured,accuracy)
            }.getOrNull()
            runOnUiThread {
                roadScanRunning=false
                if(isDestroyed || screen!=NavScreen.DRIVE)return@runOnUiThread
                postedSpeedLimit=matched
                roadLimitUpdatedAtMs=SystemClock.elapsedRealtime()
            }
        }.start()
    }

    private fun onGpsPosition(location:Location) {
        gpsFix=location
        if(screen!=NavScreen.DRIVE)return
        checkPostedSpeedLimit(location)
        if(resumeSavedRoute && routeDestination!=null && router.installed() &&
            !routeRequestRunning) {
            resumeSavedRoute=false
            val destination=routeDestination!!
            calculateRoute(NavPoint(location.latitude,location.longitude),destination)
        }
        val route=activeGuidance ?:return
        val result=route.advance(NavPoint(location.latitude,location.longitude),
            location.accuracy,SystemClock.elapsedRealtime())
        routeSnapshot=result
        if(result.arrived && !arrivalAnnounced) {
            arrivalAnnounced=true
            routeInfo=DriveInfo.ARRIVED
            if(voiceEnabled)voice.speak(GuidanceWords.text(language,GuidanceLabel.ARRIVED))
        } else if(result.shouldReroute && !routeRequestRunning) {
            val dest=routeDestination
            if(dest!=null && router.installed())
                calculateRoute(NavPoint(location.latitude,location.longitude),dest)
        } else if(voiceEnabled && !result.speech.isNullOrBlank()) {
            voice.speak(result.speech)
        } else if(voiceEnabled && result.hazardToAnnounce!=null) {
            voice.speak(HazardWords.text(language,result.hazardToAnnounce))
        }
    }

    override fun onStart() {
        super.onStart()
        network.start();gps.start()
    }

    override fun onResume() {
        super.onResume()
        if(::gps.isInitialized && screen==NavScreen.DRIVE && gps.hasPermission())
            gps.locateNow()
    }
    override fun onStop() {
        gps.stop();network.stop()
        super.onStop()
    }
    override fun onDestroy() {
        Thread {router.close()}.start()
        voice.close()
        super.onDestroy()
    }
}

@Composable
private fun HomeAction(icon:ImageVector,label:String,enabled:Boolean=true,onClick:()->Unit) {
    Button(onClick=onClick,enabled=enabled,modifier=Modifier.fillMaxWidth().height(65.dp),
        colors=ButtonDefaults.buttonColors(containerColor=Color.White,contentColor=Ink,
            disabledContainerColor=Color(0xFFEDE7DC),disabledContentColor=Color(0xFF746B5D)),
        shape=RoundedCornerShape(19.dp),
        elevation=ButtonDefaults.buttonElevation(defaultElevation=3.dp)) {
        Icon(icon,contentDescription=null,modifier=Modifier.size(26.dp),tint=DeepSand)
        Spacer(Modifier.width(13.dp))
        Text(label,fontWeight=FontWeight.SemiBold,fontSize=16.sp)
    }
}

@Composable
private fun DriveButton(icon:ImageVector,label:String,modifier:Modifier=Modifier,onClick:()->Unit) {
    Button(onClick=onClick,modifier=modifier.height(58.dp),
        colors=ButtonDefaults.buttonColors(containerColor=Panel,contentColor=Ink),
        shape=RoundedCornerShape(18.dp)) {
        Column(horizontalAlignment=Alignment.CenterHorizontally,
            verticalArrangement=Arrangement.Center) {
            Icon(icon,contentDescription=label,modifier=Modifier.size(20.dp))
            Text(label,fontSize=10.sp,maxLines=1)
        }
    }
}

@Composable
private fun InfoPill(text:String) {
    Card(colors=CardDefaults.cardColors(containerColor=Panel),shape=RoundedCornerShape(12.dp)) {
        Text(text,Modifier.padding(10.dp),color=Ink,fontSize=12.sp)
    }
}

@Composable
private fun PermissionLine(label:String,granted:Boolean) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.SpaceBetween) {
        Text(label,modifier=Modifier.weight(1f),fontSize=12.sp)
        Icon(if(granted)Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription=null,tint=if(granted)Color(0xFF33805C)else Color(0xFFB17642))
    }
}

@Composable
private fun SettingSwitch(label:String,value:Boolean,onChange:(Boolean)->Unit) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.SpaceBetween) {
        Text(label,modifier=Modifier.weight(1f),fontSize=13.sp)
        Switch(checked=value,onCheckedChange=onChange)
    }
}
