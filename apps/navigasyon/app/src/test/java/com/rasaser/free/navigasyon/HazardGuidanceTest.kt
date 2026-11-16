package com.rasaser.free.navigasyon

import org.junit.Assert.*
import org.junit.Test

class HazardGuidanceTest {
    @Test fun sixLanguagesHaveAllRealRoadTags() {
        assertTrue(HazardWords.complete())
        for(lang in NavLanguage.entries)for(type in RouteHazard.entries)
            assertTrue(HazardWords.text(lang,type).isNotBlank())
    }
    @Test fun hazardNotInventedWhenNoRouteTag() {
        val points=(0..5).map { NavPoint(41.0,29.0+it*0.001) }
        val plan=RoutePlan(points,420.0,60.0,listOf("Continue"), listOf(
            RouteManeuver(3,"Turn right","Right ahead","Turn right now")
        ))
        val fix=RouteGuidance(plan).advance(points[0],4f,0L)
        assertTrue(fix.hazards.isEmpty())
        assertNull(fix.hazardToAnnounce)
    }
    @Test fun explicitlyTaggedHazardIsNotRepeated() {
        val points=(0..5).map { NavPoint(41.0,29.0+it*0.001) }
        val plan=RoutePlan(points,420.0,60.0,listOf("Continue"), listOf(
            RouteManeuver(3,"Turn right","Right ahead","Turn right now",
                listOf(RouteHazard.ROUGH_ROAD))
        ))
        val g=RouteGuidance(plan)
        g.advance(points[0],5f,0L) // approach instruction
        val next=g.advance(points[0],5f,1_000L)
        assertEquals(RouteHazard.ROUGH_ROAD,next.hazardToAnnounce)
        assertNull(g.advance(points[0],5f,2_000L).hazardToAnnounce)
    }
}
