package com.rasaser.free.navigasyon

import org.junit.Assert.*
import org.junit.Test

class RouteGuidanceTest {
    private val shape = (0..5).map { NavPoint(41.0, 29.0 + it * 0.001) }
    private val maneuvers = listOf(
        RouteManeuver(3, "Turn right", "Right ahead", "Turn right now"),
        RouteManeuver(5, "Arrive", "Approaching destination", "Arrived")
    )
    private fun tracker() = RouteGuidance(RoutePlan(shape,420.0,60.0, emptyList(),maneuvers))

    @Test fun announcesOncePerApproachStage() {
        val s = tracker()
        val a = s.advance(shape[0],8f,0L)
        assertEquals("Right ahead",a.speech)
        assertNull(s.advance(shape[0],8f,1_000L).speech)
        val b = s.advance(shape[2],8f,2_000L)
        assertEquals("Turn right now",b.speech)
        assertNull(s.advance(shape[2],8f,3_000L).speech)
    }
    @Test fun rerouteRequiresThreeGoodFixesAndCooldown() {
        val s=tracker()
        val off=NavPoint(41.004,29.001)
        assertFalse(s.advance(off,6f,0L).shouldReroute)
        assertFalse(s.advance(off,6f,2_000L).shouldReroute)
        assertTrue(s.advance(off,6f,4_000L).shouldReroute)
        assertFalse(s.advance(off,6f,5_000L).shouldReroute)
        assertFalse(s.advance(off,6f,6_000L).shouldReroute)
        assertFalse(s.advance(off,6f,7_000L).shouldReroute)
        assertFalse(s.advance(off,6f,50_000L).shouldReroute)
        assertFalse(s.advance(off,6f,51_000L).shouldReroute)
        assertTrue(s.advance(off,6f,52_000L).shouldReroute)
    }
    @Test fun badGpsNeverCausesReroute() {
        val s=tracker()
        repeat(10) {
            val state=s.advance(NavPoint(41.004,29.001),60f,it*1_000L)
            assertFalse(state.shouldReroute)
        }
    }
    @Test fun destinationDetectedOnlyNearEnd() {
        val s=tracker()
        assertFalse(s.advance(shape[0],4f,0L).arrived)
        assertTrue(s.advance(shape[5],4f,9_000L).arrived)
    }
    @Test fun routeGeometryMetersAreFinite() {
        val s=tracker()
        val snap=s.advance(shape[1],5f,0L)
        assertTrue(snap.remainingMeters in 310.0..350.0)
        assertTrue(snap.nearestRouteMeters < 1.0)
    }
}
