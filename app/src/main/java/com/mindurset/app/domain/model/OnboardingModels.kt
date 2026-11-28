package com.mindurset.app.domain.model

enum class SleepQuality {
    RESTFUL,
    MIXED,
    LIGHT_OR_BROKEN,
    TOO_LITTLE,
    ;

    companion object {
        fun fromName(name: String?): SleepQuality? = entries.firstOrNull { it.name == name }
    }
}

enum class SupportLevel {
    STRONG,
    SOME,
    ALONE,
    UNSURE,
    ;

    companion object {
        fun fromName(name: String?): SupportLevel? = entries.firstOrNull { it.name == name }
    }
}

enum class OnboardingFocus {
    CALMING_WORRY,
    SHIFTING_THOUGHTS,
    DAILY_RHYTHM,
    JUST_EXPLORING,
    ;

    companion object {
        fun fromName(name: String?): OnboardingFocus? = entries.firstOrNull { it.name == name }
    }
}

data class OnboardingBaseline(
    val id: String,
    val userId: String,
    val recentMood: Emotion,
    val sleepQuality: SleepQuality,
    val supportLevel: SupportLevel,
    val primaryFocus: OnboardingFocus,
    val createdAt: Long,
)

enum class OnboardingSummaryKey {
    HAPPY,
    CALM,
    LOW,
    ANXIOUS,
    STRESSED,
    ;

    companion object {
        fun fromEmotion(emotion: Emotion): OnboardingSummaryKey = when (emotion) {
            Emotion.HAPPY -> HAPPY
            Emotion.CALM -> CALM
            Emotion.LOW -> LOW
            Emotion.ANXIOUS -> ANXIOUS
            Emotion.STRESSED -> STRESSED
        }
    }
}

object ValidateOnboardingBaseline {
    fun isComplete(
        recentMood: Emotion?,
        sleepQuality: SleepQuality?,
        supportLevel: SupportLevel?,
        primaryFocus: OnboardingFocus?,
    ): Boolean = recentMood != null && sleepQuality != null && supportLevel != null && primaryFocus != null

    fun fromNames(
        id: String,
        userId: String,
        recentMoodName: String?,
        sleepQualityName: String?,
        supportLevelName: String?,
        primaryFocusName: String?,
        createdAt: Long,
    ): OnboardingBaseline? {
        if (id.isBlank() || userId.isBlank()) return null
        val mood = Emotion.entries.firstOrNull { it.name == recentMoodName } ?: return null
        val sleep = SleepQuality.fromName(sleepQualityName) ?: return null
        val support = SupportLevel.fromName(supportLevelName) ?: return null
        val focus = OnboardingFocus.fromName(primaryFocusName) ?: return null
        return OnboardingBaseline(
            id = id,
            userId = userId,
            recentMood = mood,
            sleepQuality = sleep,
            supportLevel = support,
            primaryFocus = focus,
            createdAt = createdAt,
        )
    }
}
