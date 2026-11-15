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

    fun start() {
        if (running || !hasPermission()) return
        running = true
        try {
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            var any = false
            for (provider in providers) {
                if (manager.isProviderEnabled(provider)) {
                    manager.requestLocationUpdates(provider, 850L, 2f, callback, Looper.getMainLooper())
                    any = true
                }
            }
            if (!any) running = false
            for (provider in providers) {
                @Suppress("DEPRECATION")
                val recent = manager.getLastKnownLocation(provider)
                if (recent != null && recent.isValidNavigationFix(maxAgeMs = 120_000L)) onFix(recent)
            }
        } catch (_: SecurityException) { running = false }
        catch (_: IllegalArgumentException) { running = false }
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
