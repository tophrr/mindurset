package com.mindurset.app.ui.reflection

import androidx.annotation.StringRes
import com.mindurset.app.R
import com.mindurset.app.domain.usecase.ValidateReflection

/** Resource keys for non-validation (runtime) failures in the reflection module. */
internal object ReflectionErrorKeys {
    const val NO_SESSION = "reflection_error_no_session"
    const val LOAD = "reflection_error_load"
}

/**
 * Maps a validator / runtime error key to a localised string resource id. The
 * domain layer only ever produces keys, so this is where they become text.
 */
@StringRes
internal fun reflectionErrorRes(key: String?): Int = when (key) {
    ValidateReflection.ERROR_CHALLENGE_REQUIRED -> R.string.reflection_error_challenge_required
    ValidateReflection.ERROR_CHALLENGE_SHORT -> R.string.reflection_error_challenge_short
    ValidateReflection.ERROR_CHALLENGE_LONG -> R.string.reflection_error_challenge_long
    ValidateReflection.ERROR_REFRAME_LONG -> R.string.reflection_error_reframe_long
    ValidateReflection.ERROR_GRATITUDE_LONG -> R.string.reflection_error_gratitude_long
    ReflectionErrorKeys.NO_SESSION -> R.string.reflection_error_no_session
    ReflectionErrorKeys.LOAD -> R.string.reflection_error_load
    else -> R.string.reflection_error_generic
}
