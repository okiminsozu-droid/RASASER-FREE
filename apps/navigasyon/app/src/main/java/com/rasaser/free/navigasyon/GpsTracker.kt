package com.rasaser.free.navigasyon

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import java.util.concurrent.TimeUnit

/** Foreground tracking only. No location is sent to any web service or stored remotely. */
class GpsTracker(
    private val context: Context,
    private val onFix: (Location) -> Unit
) {
    private val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val callback = LocationListener { location ->
        if (location.isValidNavigationFix()) onFix(location)
    }
    private var running = false

    fun hasPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun providersEnabled():Boolean = listOf(LocationManager.GPS_PROVIDER,LocationManager.NETWORK_PROVIDER)
        .any { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }

    fun start() {
        if(running || !hasPermission())return
        val fine=context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED
        var started=false
        for(provider in listOf(LocationManager.GPS_PROVIDER,LocationManager.NETWORK_PROVIDER)) {
            if(provider==LocationManager.GPS_PROVIDER && !fine)continue
            try {
                if(manager.isProviderEnabled(provider)) {
                    manager.requestLocationUpdates(provider,800L,1f,callback,Looper.getMainLooper())
                    started=true
                    @Suppress("DEPRECATION")
                    val prior=manager.getLastKnownLocation(provider)
                    if(prior!=null && prior.isValidNavigationFix(90_000L))onFix(prior)
                }
            }catch (_:SecurityException){} catch (_:IllegalArgumentException){}
        }
        running=started
    }

    /**
     * One-shot fresh fix request when the user taps My location. This never
     * substitutes old coordinates for the user's live position.
     */
    fun locateNow() {
        if(!hasPermission())return
        start()
        if(Build.VERSION.SDK_INT>=30) {
            val fine=context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED
            val provider=when {
                fine && runCatching{manager.isProviderEnabled(LocationManager.GPS_PROVIDER)}.getOrDefault(false) ->LocationManager.GPS_PROVIDER
                runCatching{manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)}.getOrDefault(false) ->LocationManager.NETWORK_PROVIDER
                else ->null
            } ?:return
            try {
                manager.getCurrentLocation(provider,null,context.mainExecutor) { candidate ->
                    if(candidate!=null && candidate.isValidNavigationFix(30_000L))onFix(candidate)
                }
            }catch (_:SecurityException){}catch (_:IllegalArgumentException){}
        }
    }

    fun stop() {
        if (!running) return
        running = false
        manager.removeUpdates(callback)
    }
}
fun Location.isValidNavigationFix(maxAgeMs: Long = 30_000L): Boolean {
    if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return false
    if (!hasAccuracy() || accuracy <= 0f || accuracy > 100f) return false
    val age = TimeUnit.NANOSECONDS.toMillis(SystemClock.elapsedRealtimeNanos() - elapsedRealtimeNanos)
    return age in 0..maxAgeMs
}
fun Location.reliableSpeedKmh(): Int? {
    if (!hasSpeed() || speed < 0f || speed > 110f) return null
    if (Build.VERSION.SDK_INT >= 26 && hasSpeedAccuracy() && speedAccuracyMetersPerSecond > 3.5f) return null
    return (speed * 3.6f).toInt().coerceIn(0, 400)
}
