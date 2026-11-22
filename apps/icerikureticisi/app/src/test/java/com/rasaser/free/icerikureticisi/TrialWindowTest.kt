package com.rasaser.free.icerikureticisi

import org.junit.Assert.assertEquals
import org.junit.Test

class TrialWindowTest {
    private val start = 1_800_000_000_000L

    @Test fun firstDayIsFifteenDays() {
        assertEquals(15, TrialWindow.daysLeft(start, start))
        assertEquals(15, TrialWindow.daysLeft(start, start + 1_000L))
    }

    @Test fun countdownMatchesCalendarDays() {
        assertEquals(14, TrialWindow.daysLeft(start, start + TrialWindow.DAY_MS))
        assertEquals(1, TrialWindow.daysLeft(start, start + TrialWindow.DURATION_MS - 1L))
    }

    @Test fun trialCannotContinuePastExpiry() {
        assertEquals(0, TrialWindow.daysLeft(start, start + TrialWindow.DURATION_MS))
        assertEquals(0, TrialWindow.daysLeft(start, start + TrialWindow.DURATION_MS + TrialWindow.DAY_MS))
    }

    @Test fun rollbackIsNotNewTime() {
        assertEquals(0, TrialWindow.daysLeft(start, start - 1000L))
        assertEquals(0, TrialWindow.daysLeft(0L, start))
    }
}
