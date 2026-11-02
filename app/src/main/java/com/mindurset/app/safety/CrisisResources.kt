package com.mindurset.app.safety

/** Crisis numbers bundled with the app (proposal Sec. 6.4): they must work with no network. */
object CrisisResources {
    const val PHONE = "119"
    const val EXTENSION = "8"
    const val LABEL = "119 ext. 8"

    fun display(): String = "$PHONE ext. $EXTENSION"
}
