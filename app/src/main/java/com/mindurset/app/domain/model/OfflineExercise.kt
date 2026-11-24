package com.mindurset.app.domain.model

data class OfflineExerciseSection(val heading: String, val body: String)

data class OfflineExercise(
    val topic: String,
    val title: String,
    val keywords: List<String>,
    val sections: List<OfflineExerciseSection>,
    val language: String = "en",
)

object OfflineExerciseTopics {
    val ALL: List<String> = listOf(
        "action",
        "all-or-nothing",
        "rumination",
        "safety-plan",
        "self-compassion",
        "sleep",
        "stress",
    )
}
