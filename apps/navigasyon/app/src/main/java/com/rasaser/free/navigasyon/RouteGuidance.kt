package com.rasaser.free.navigasyon

import kotlin.math.*

/** One real Valhalla maneuver with a precise route geometry index. */
enum class RouteHazard { TOLL, ROUGH_ROAD, FERRY, GATE }

data class RouteManeuver(
    val shapeIndex: Int,
    val instruction: String,
    val earlyInstruction: String,
    val nearInstruction: String,
    val hazards:List<RouteHazard> = emptyList()
)

data class GuidanceSnapshot(
    val remainingMeters: Double,
    val nearestRouteMeters: Double,
    val nextTurnMeters: Double?,
    val nextInstruction: String?,
    val speech: String?,
    val shouldReroute: Boolean,
    val arrived: Boolean,
    val hazards:List<RouteHazard> = emptyList(),
    val hazardToAnnounce:RouteHazard? = null
)

/**
 * Pure Kotlin foreground route progression. No Internet, no GPS retention and
 * no invented turn alerts. Invalid/inaccurate fixes cannot cause rerouting.
 */
class RouteGuidance(private val route: RoutePlan) {
    private val points = route.shape
    private val distances = DoubleArray(points.size)
    private var lastSegment = 0
    private var missedFixes = 0
    private var lastRerouteMs: Long? = null
    private val spoken = HashSet<String>()
    private var arrivalAnnounced = false

    init {
        for (i in 1 until points.size) {
            distances[i] = distances[i - 1] + metersBetween(points[i - 1], points[i])
        }
    }

    fun advance(point: NavPoint, accuracyMeters: Float, nowMs: Long): GuidanceSnapshot {
        val valid = point.lat in -90.0..90.0 && point.lon in -180.0..180.0 &&
            accuracyMeters.isFinite() && accuracyMeters in 0.1f..75f && points.size >= 2

        if (!valid) return GuidanceSnapshot(
            remainingMeters = distances.lastOrNull() ?: 0.0,
            nearestRouteMeters = Double.POSITIVE_INFINITY,
            nextTurnMeters = null, nextInstruction = null, speech = null,
            shouldReroute = false, arrived = false
        )

        val from = max(0, lastSegment - 12)
        val to = min(points.size - 2, lastSegment + 150)
        var bestDistance = Double.POSITIVE_INFINITY
        var bestSegment = lastSegment
        var bestFraction = 0.0

        for (i in from..to) {
            val start = points[i]
            val end = points[i + 1]
            val latitudeScale = 111_320.0
            val longitudeScale = latitudeScale * cos(Math.toRadians(point.lat)).coerceAtLeast(0.01)
            val ax = (start.lon - point.lon) * longitudeScale
            val ay = (start.lat - point.lat) * latitudeScale
            val bx = (end.lon - point.lon) * longitudeScale
            val by = (end.lat - point.lat) * latitudeScale
            val vx = bx - ax
            val vy = by - ay
            val fraction = if (vx * vx + vy * vy < 0.01) 0.0
                else ((-ax * vx - ay * vy) / (vx * vx + vy * vy)).coerceIn(0.0, 1.0)
            val distance = hypot(ax + fraction * vx, ay + fraction * vy)
            if (distance < bestDistance) {
                bestDistance = distance
                bestSegment = i
                bestFraction = fraction
            }
        }

        val offRoute = bestDistance > max(55.0, accuracyMeters * 2.5)
        if (offRoute && accuracyMeters <= 35f) missedFixes++ else missedFixes = 0
        val shouldReroute = missedFixes >= 3 &&
            (lastRerouteMs == null || nowMs - lastRerouteMs!! >= 45_000L)
        if (missedFixes >= 3) missedFixes = 0 // require three fresh, independent fixes
        if (shouldReroute) lastRerouteMs = nowMs

        if (!offRoute && bestSegment >= lastSegment) lastSegment = bestSegment
        val progress = distances[bestSegment] +
            (distances[bestSegment + 1] - distances[bestSegment]) * bestFraction
        val remaining = (distances.last() - progress).coerceAtLeast(0.0)
        val arrived = !offRoute && accuracyMeters <= 35f && remaining < 35 &&
            metersBetween(point, points.last()) < 45.0
        if (arrived) arrivalAnnounced = true

        val upcoming = route.maneuvers.withIndex().firstOrNull {
            it.value.shapeIndex in 0 until points.size &&
                distances[it.value.shapeIndex] - progress >= -12
        }
        val distanceToTurn = upcoming?.let {
            (distances[it.value.shapeIndex] - progress).coerceAtLeast(0.0)
        }
        var speech: String? = null
        if (!offRoute && !arrived && upcoming != null && distanceToTurn != null) {
            val key = upcoming.index
            if (distanceToTurn <= 90.0 && spoken.add("$key:near")) {
                speech = upcoming.value.nearInstruction.ifBlank { upcoming.value.instruction }
            } else if (distanceToTurn in 90.0..450.0 && spoken.add("$key:early")) {
                speech = upcoming.value.earlyInstruction.ifBlank { upcoming.value.instruction }
            }
        }

        val hazards=upcoming?.value?.hazards?.takeIf { (distanceToTurn ?: 100000.0) < 650.0 }
            ?: emptyList()
        var hazardToAnnounce:RouteHazard?=null
        if(speech==null && !offRoute && !arrived) {
            for(hazard in hazards) {
                if(spoken.add("${upcoming!!.index}:hazard:${hazard.name}")) {
                    hazardToAnnounce=hazard
                    break
                }
            }
        }
        return GuidanceSnapshot(remaining, bestDistance, distanceToTurn,
            upcoming?.value?.instruction?.takeIf { it.isNotBlank() },
            speech, shouldReroute, arrived,hazards,hazardToAnnounce)
    }

    companion object {
        fun metersBetween(a: NavPoint, b: NavPoint): Double {
            val rad1 = Math.toRadians(a.lat)
            val rad2 = Math.toRadians(b.lat)
            val dy = rad2 - rad1
            val dx = Math.toRadians(b.lon - a.lon)
            val h = sin(dy / 2).pow(2) + cos(rad1) * cos(rad2) * sin(dx / 2).pow(2)
            return 12_742_000.0 * asin(sqrt(h.coerceIn(0.0, 1.0)))
        }
    }
}
