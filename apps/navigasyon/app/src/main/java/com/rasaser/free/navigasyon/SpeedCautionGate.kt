package com.rasaser.free.navigasyon

/** Speed limit from a matched road edge, NEVER a guessed limit from road type. */
data class RoadSpeedLimit(val kmh:Int,val roadName:String?=null)

/**
 * A speed alert is allowed only with a fresh map-matched limit and multiple
 * accurate independent GPS speed samples. Debounced to avoid distracting drivers.
 */
class SpeedCautionGate {
    private var aboveCount=0
    private var lastSpokenAt:Long?=null

    fun evaluate(
        postedLimit:Int?,
        gpsSpeedKmh:Int?,
        gpsAccuracyMeters:Float,
        limitAgeMs:Long,
        nowMs:Long
    ):Boolean {
        val trustworthy=postedLimit != null && postedLimit in 10..150 &&
            gpsSpeedKmh != null && gpsSpeedKmh in 0..260 &&
            gpsAccuracyMeters.isFinite() && gpsAccuracyMeters in 0.1f..25f &&
            limitAgeMs in 0L..25_000L
        if(!trustworthy || gpsSpeedKmh!! < postedLimit!! + 8) {
            aboveCount=0
            return false
        }
        if(lastSpokenAt!=null && nowMs-lastSpokenAt!!<90_000L) {
            aboveCount=0
            return false
        }
        aboveCount++
        if(aboveCount<3) return false
        lastSpokenAt=nowMs
        aboveCount=0
        return true
    }
}
