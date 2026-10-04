package com.mindurset.app.domain.model

/** One emotion scale for the whole app (proposal Sec. 6.3). */
enum class Emotion(val score: Int) {
    HAPPY(5),
    CALM(4),
    LOW(3),
    ANXIOUS(2),
    STRESSED(1);

    val isNegative: Boolean get() = this == LOW || this == ANXIOUS || this == STRESSED

    companion object {
        fun fromName(name: String?): Emotion = entries.firstOrNull { it.name == name } ?: LOW
    }
}
