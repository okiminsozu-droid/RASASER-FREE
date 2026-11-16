package com.rasaser.free.navigasyon

import org.junit.Assert.*
import org.junit.Test

class SpeedCautionGateTest {
    @Test fun warningOnlyForThreeAccurateFreshOverLimitSamples() {
        val gate=SpeedCautionGate()
        assertFalse(gate.evaluate(50,61,6f,500,0))
        assertFalse(gate.evaluate(50,61,6f,800,1_000))
        assertTrue(gate.evaluate(50,61,6f,1000,2_000))
        repeat(4) { assertFalse(gate.evaluate(50,65,6f,1000,3_000+it*1000L)) }
        assertFalse(gate.evaluate(50,65,6f,1000,95_000))
        assertFalse(gate.evaluate(50,65,6f,1000,96_000))
        assertTrue(gate.evaluate(50,65,6f,1000,97_000))
    }
    @Test fun noLimitOrStalePositionNeverWarns() {
        val gate=SpeedCautionGate()
        repeat(5){
            assertFalse(gate.evaluate(null,120,5f,1000,it*1000L))
            assertFalse(gate.evaluate(50,100,45f,1000,it*1000L))
            assertFalse(gate.evaluate(50,100,3f,30_000,it*1000L))
        }
    }
    @Test fun smallDifferenceIsNotAlarm() {
        val gate=SpeedCautionGate()
        repeat(10){assertFalse(gate.evaluate(80,84,6f,1200,it*1000L))}
    }
}
