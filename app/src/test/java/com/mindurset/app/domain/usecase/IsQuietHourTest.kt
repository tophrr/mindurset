package com.mindurset.app.domain.usecase

import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class IsQuietHourTest {
    @Test
    fun `quiet hours run from ten in the evening to seven in the morning`() {
        assertFalse(IsQuietHour.isQuiet(21))
        assertTrue(IsQuietHour.isQuiet(22))
        assertTrue(IsQuietHour.isQuiet(23))
        assertTrue(IsQuietHour.isQuiet(0))
        assertTrue(IsQuietHour.isQuiet(6))
        assertFalse(IsQuietHour.isQuiet(7))
        assertFalse(IsQuietHour.isQuiet(12))
    }

    @Test
    fun `canNotify is the inverse of quiet`() {
        assertTrue(IsQuietHour.canNotify(8))
        assertFalse(IsQuietHour.canNotify(23))
    }

    @Test
    fun `rejects an hour that does not exist`() {
        assertThrows(IllegalArgumentException::class.java) { IsQuietHour.isQuiet(24) }
        assertThrows(IllegalArgumentException::class.java) { IsQuietHour.isQuiet(-1) }
    }
}
