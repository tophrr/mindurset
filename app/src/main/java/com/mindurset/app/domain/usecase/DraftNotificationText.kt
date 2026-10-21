package com.mindurset.app.domain.usecase

/**
 * The one reminder we allow, in non-punitive wording (proposal Sec. 7.5 and NFR-13): it invites a
 * check-in and never counts missed days.
 */
object DraftNotificationText {
    private val bannedFragments =
        listOf("streak", "missed", "you lost", "don't break", "days in a row", "kehilangan")

    fun draft(displayName: String?): String {
        val name = displayName?.trim()
        return if (name.isNullOrEmpty()) {
            "A quiet minute? Check in with how you feel."
        } else {
            "$name, a quiet minute? Check in with how you feel."
        }
    }

    fun isNonPunitive(text: String): Boolean = bannedFragments.none { text.contains(it, ignoreCase = true) }
}
