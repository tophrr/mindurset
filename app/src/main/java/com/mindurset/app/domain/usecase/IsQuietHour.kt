package com.mindurset.app.domain.usecase

/** Quiet hours from proposal Sec. 7.5: no notification between 22:00 and 07:00. */
object IsQuietHour {
    const val START_HOUR = 22
    const val END_HOUR = 7

    fun isQuiet(hour: Int): Boolean {
        require(hour in 0..23) { "hour must be between 0 and 23, was $hour" }
        return hour >= START_HOUR || hour < END_HOUR
    }

    fun canNotify(hour: Int): Boolean = !isQuiet(hour)
}
