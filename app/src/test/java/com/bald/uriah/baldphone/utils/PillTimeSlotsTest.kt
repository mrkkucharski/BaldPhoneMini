package com.bald.uriah.baldphone.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PillTimeSlotsTest {
    @Test
    fun defaultsPreserveExistingFirstThreeSlotsAndAddExtras() {
        assertEquals(6, PillTimeSlotDefaults.PILL_TIME_SLOT_COUNT)

        assertEquals(7, PillTimeSlotDefaults.getDefaultHour(PillTimeSlotDefaults.TIME_MORNING))
        assertEquals(30, PillTimeSlotDefaults.getDefaultMinute(PillTimeSlotDefaults.TIME_MORNING))
        assertEquals(12, PillTimeSlotDefaults.getDefaultHour(PillTimeSlotDefaults.TIME_AFTERNOON))
        assertEquals(30, PillTimeSlotDefaults.getDefaultMinute(PillTimeSlotDefaults.TIME_AFTERNOON))
        assertEquals(19, PillTimeSlotDefaults.getDefaultHour(PillTimeSlotDefaults.TIME_EVENING))
        assertEquals(30, PillTimeSlotDefaults.getDefaultMinute(PillTimeSlotDefaults.TIME_EVENING))

        assertEquals(10, PillTimeSlotDefaults.getDefaultHour(PillTimeSlotDefaults.TIME_EXTRA_1))
        assertEquals(0, PillTimeSlotDefaults.getDefaultMinute(PillTimeSlotDefaults.TIME_EXTRA_1))
        assertEquals(16, PillTimeSlotDefaults.getDefaultHour(PillTimeSlotDefaults.TIME_EXTRA_2))
        assertEquals(0, PillTimeSlotDefaults.getDefaultMinute(PillTimeSlotDefaults.TIME_EXTRA_2))
        assertEquals(22, PillTimeSlotDefaults.getDefaultHour(PillTimeSlotDefaults.TIME_EXTRA_3))
        assertEquals(0, PillTimeSlotDefaults.getDefaultMinute(PillTimeSlotDefaults.TIME_EXTRA_3))
    }

    @Test
    fun formatTimePadsSingleDigitHourAndMinute() {
        assertEquals("07:05", PillTimeSlots.formatTime(7, 5))
        assertEquals("16:00", PillTimeSlots.formatTime(16, 0))
    }

    @Test
    fun compareResolvedTimeSortsByClockTimeBeforeSlotOrder() {
        val morningAtTenComparedToAfternoonAtSeven = PillTimeSlots.compareResolvedTime(
            10,
            0,
            "Morning pill",
            7,
            0,
            "Afternoon pill"
        )

        assertTrue(morningAtTenComparedToAfternoonAtSeven > 0)
    }

    @Test
    fun compareResolvedTimeUsesNameAsTieBreaker() {
        val comparison = PillTimeSlots.compareResolvedTime(
            7,
            30,
            "Beta",
            7,
            30,
            "Alpha"
        )

        assertTrue(comparison > 0)
    }
}
