package com.rasaser.free.navigasyon

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Environment
import android.os.StatFs
import androidx.core.app.NotificationCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * ONE persistent and user-initiated Türkiye installation.
 * WorkManager + a durable .part + HTTP Range survive connectivity changes,
 * process termination and device restarts. No R2 URL is shown in the UI.
 */
class TurkeyCountryPackWorker(context: Context, params: WorkerParameters):
    CoroutineWorker(context,params) {

    companion object {
        const val UNIQUE = "rasaser-turkiye-complete-v1"
        const val PERCENT = "progress_percent"
        const val STAGE = "stage"
        const val ERROR = "error"
        const val BYTES = "progress_bytes"
        const val TOTAL = "total_bytes"
        const val STAGE_WAIT = 0
        const val STAGE_MAP = 1
        const val STAGE_ROUTE = 2
        const val STAGE_SEARCH = 3
        const val STAGE_DONE = 4
        const val WIFI_ONLY = "wifi_only"
        private const val NOTIFICATION_ID = 9154
        private const val CHANNEL_ID = "rasaser_turkiye_transfer"

        fun enqueue(context: Context, wifiOnly: Boolean = true) {
            val request = OneTimeWorkRequestBuilder<TurkeyCountryPackWorker>()
                .setConstraints(Constraints.Builder()
                    .setRequiredNetworkType(if(wifiOnly) NetworkType.UNMETERED
                        else NetworkType.CONNECTED).build())
                .setInputData(workDataOf(WIFI_ONLY to wifiOnly))
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.SECONDS)
                .addTag(UNIQUE)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE,ExistingWorkPolicy.KEEP,request)
        }
    }

    private var currentStage = STAGE_WAIT
    private var bytesComplete = 0L
    private var lastProgress = -1
    private var lastNotificationAt = 0L

    override suspend fun getForegroundInfo(): ForegroundInfo =
        foreground("Türkiye çevrimdışı paketleri hazırlanıyor",0)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val context = applicationContext
        val root = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: return@withContext fail("storage")
        val transfer = ResumableTurkeyTransfer(File(root,"rasaser-turkiye-resumable-v1"))
        try {
            setForeground(foreground("Türkiye indiriliyor",0))
            fun progress(stage:Int, current:Long) {
                currentStage=stage
                val complete = (bytesComplete+current).coerceAtMost(TurkeyAssets.totalBytes)
                val percent = ((complete*100L)/TurkeyAssets.totalBytes).toInt().coerceIn(0,99)
                if(percent != lastProgress || System.currentTimeMillis()-lastNotificationAt>4_000L) {
                    lastProgress=percent
                    setProgressAsync(workDataOf(
                        PERCENT to percent,STAGE to stage,BYTES to complete,
                        TOTAL to TurkeyAssets.totalBytes))
                    if(System.currentTimeMillis()-lastNotificationAt>8_000L) {
                        setForegroundAsync(foreground(stageText(stage),percent))
                        lastNotificationAt=System.currentTimeMillis()
                    }
                }
            }

            fun checkStorage(needed:Long) {
                val free = StatFs(context.filesDir.absolutePath).availableBytes
                if(free < needed) throw InsufficientStorageException()
            }

            fun get(asset:TurkeyAsset, stage:Int, installReserve:Long=0L):File {
                val partial=File(root,"rasaser-turkiye-resumable-v1/" + asset.name + ".part")
                val missing=(asset.size-partial.length().coerceIn(0,asset.size)).coerceAtLeast(0)
                checkStorage(missing + installReserve + 80_000_000L)
                val file = transfer.download(asset, {isStopped}) { progress(stage,it) }
                bytesComplete += asset.size
                progress(stage,0L)
                return file
            }

            // All four metadata files are downloaded and SHA256-pinned as well.
            get(TurkeyAssets.mapManifest,STAGE_MAP)
            val mapInstaller=TurkeyMapInstaller(context)
            if(!LocalMapVerifier.turkeyMapReady(context)) {
                val archive=get(TurkeyAssets.map,STAGE_MAP,
                    TurkeyMapInstaller.MAP_BYTES+150_000_000L)
                setForegroundAsync(foreground("Türkiye haritası doğrulanıyor",lastProgress.coerceAtLeast(0)))
                if(!mapInstaller.installZip(archive) || !LocalMapVerifier.turkeyMapReady(context))
                    return@withContext fail("map_install")
                archive.delete()
            } else {
                bytesComplete += TurkeyAssets.map.size
                progress(STAGE_MAP,0L)
            }

            val routeManifest=get(TurkeyAssets.routeManifest,STAGE_ROUTE)
            val routeConfig=get(TurkeyAssets.routeConfig,STAGE_ROUTE)
            val routeEngine=OfflineRouteEngine(context)
            if(!routeEngine.installed()) {
                val archive=get(TurkeyAssets.route,STAGE_ROUTE,
                    TurkeyRoutingInstaller.GRAPH_BYTES+150_000_000L)
                setForegroundAsync(foreground("Türkiye rota paketi doğrulanıyor",
                    lastProgress.coerceAtLeast(0)))
                val metadata=JSONObject(routeManifest.readText(Charsets.UTF_8))
                val config=JSONObject(routeConfig.readText(Charsets.UTF_8))
                if(!TurkeyRoutingInstaller(context).installDownloaded(
                        archive,metadata,config) || !routeEngine.installed())
                    return@withContext fail("route_install")
                archive.delete()
            } else {
                bytesComplete += TurkeyAssets.route.size
                progress(STAGE_ROUTE,0L)
            }

            get(TurkeyAssets.searchManifest,STAGE_SEARCH)
            val searchInstaller=TurkeySearchInstaller(context)
            if(!searchInstaller.installed()) {
                val archive=get(TurkeyAssets.search,STAGE_SEARCH,280_000_000L)
                setForegroundAsync(foreground("Türkiye yer arama doğrulanıyor",
                    lastProgress.coerceAtLeast(0)))
                val result=searchInstaller.installWithResult(sourceArchive=archive)
                if(!result.installed || !searchInstaller.installed())
                    return@withContext fail("search_" + result.stage.name)
                archive.delete()
            } else {
                bytesComplete += TurkeyAssets.search.size
                progress(STAGE_SEARCH,0L)
            }

            setProgressAsync(workDataOf(PERCENT to 100,STAGE to STAGE_DONE,
                BYTES to TurkeyAssets.totalBytes,TOTAL to TurkeyAssets.totalBytes))
            setForegroundAsync(foreground("Türkiye çevrimdışı verileri hazır",100))
            Result.success(workDataOf(PERCENT to 100,STAGE to STAGE_DONE))
        } catch (_: InsufficientStorageException) {
            fail("storage")
        } catch (_: IOException) {
            // Keep .part bytes: WorkManager retries when Wi-Fi becomes available.
            if(runAttemptCount >= 7) fail("download_retry")
            else Result.retry()
        } catch (_: Exception) {
            if(runAttemptCount >= 3) fail("verify_install")
            else Result.retry()
        }
    }

    private fun fail(code:String):Result =
        Result.failure(workDataOf(ERROR to code, STAGE to currentStage,
            PERCENT to lastProgress.coerceAtLeast(0)))

    private fun stageText(stage:Int):String = when(stage) {
        STAGE_MAP -> "Türkiye haritası indiriliyor"
        STAGE_ROUTE -> "Çevrimdışı rota verisi indiriliyor"
        STAGE_SEARCH -> "Çevrimdışı yer arama indiriliyor"
        else -> "Wi-Fi bağlantısı bekleniyor"
    }

    private fun foreground(message:String,percent:Int):ForegroundInfo {
        val notifications=applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notifications.createNotificationChannel(NotificationChannel(
            CHANNEL_ID,"Türkiye harita indirmeleri",NotificationManager.IMPORTANCE_LOW))
        val intent=Intent(applicationContext,MainActivity::class.java)
        val pending=PendingIntent.getActivity(applicationContext,NOTIFICATION_ID,intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification=NotificationCompat.Builder(applicationContext,CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("RaSaSeR Navigasyon")
            .setContentText(message)
            .setContentIntent(pending)
            .setProgress(100,percent,false)
            .setOngoing(percent<100)
            .setOnlyAlertOnce(true)
            .build()
        return if(Build.VERSION.SDK_INT >= 29)
            ForegroundInfo(NOTIFICATION_ID,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        else ForegroundInfo(NOTIFICATION_ID,notification)
    }

    private class InsufficientStorageException: IOException()
}
