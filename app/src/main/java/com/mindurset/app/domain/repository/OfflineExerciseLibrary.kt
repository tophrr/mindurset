package com.mindurset.app.domain.repository

import com.mindurset.app.domain.model.OfflineExercise

interface OfflineExerciseLibrary {
    fun loadAll(language: String): List<OfflineExercise>
}
