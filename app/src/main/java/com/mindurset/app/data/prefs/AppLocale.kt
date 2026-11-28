package com.mindurset.app.data.prefs

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.annotation.RequiresApi
import java.util.Locale

/**
 * Resolves and applies the user's in-app language preference without AppCompat:
 * - On API 33+, delegates to the platform [LocaleManager].
 * - On API 26-32, wraps the activity base [Context] with [Context.createConfigurationContext]
 *   before the first composition and recreates the activity once when the choice changes.
 */
object AppLocale {
    const val SYSTEM = "system"
    const val ENGLISH = "en"
    const val INDONESIAN = "id"

    val SUPPORTED_TAGS: Set<String> = setOf(ENGLISH, INDONESIAN)

    /**
     * Normalizes a stored or external tag to `"system"`, `"en"`, or `"id"`.
     * Unsupported, blank, or null tags fall back to `"system"` without throwing.
     */
    fun normalizeStoredLanguage(raw: String?): String = when (raw?.trim()?.lowercase()) {
        ENGLISH -> ENGLISH
        INDONESIAN, "in" -> INDONESIAN
        else -> SYSTEM
    }

    /**
     * Maps a stored preference value to the BCP-47 tag to apply, or `null` when the app should
     * follow the system locale (including `"system"` and any unsupported tag).
     */
    fun resolveLocaleTag(storedLanguage: String?): String? =
        when (val normalized = normalizeStoredLanguage(storedLanguage)) {
            ENGLISH, INDONESIAN -> normalized
            else -> null
        }

    /**
     * Wraps [base] with a locale configuration override for [storedLanguage] on API 26-32.
     * On API 33+, returns [base] unchanged because [LocaleManager] owns per-app locales.
     */
    fun wrapContext(base: Context, storedLanguage: String?): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val locales = base.resources.configuration.locales
        val systemLocale = if (locales.isEmpty) Locale.getDefault() else locales[0]
        val tag = resolveLocaleTag(storedLanguage)
        if (tag == null) {
            Locale.setDefault(systemLocale)
            return base
        }
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }

    /**
     * Applies [storedLanguage] to the running [activity].
     * On API 33+, updates [LocaleManager.setApplicationLocales] (which recreates if needed).
     * On API 26-32, calls [Activity.recreate] once so `attachBaseContext` picks up the new tag.
     */
    fun applyLanguage(activity: Activity, storedLanguage: String?) {
        if (activity.isFinishing || activity.isDestroyed) return
        val tag = resolveLocaleTag(storedLanguage)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = activity.getSystemService(LocaleManager::class.java) ?: return
            localeManager.applicationLocales =
                if (tag == null) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
        } else {
            activity.recreate()
        }
    }

    /** Reads the active platform per-app language on API 33+, normalized to `"system"`, `"en"`, or `"id"`. */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun readPlatformLanguage(context: Context): String {
        val localeManager = context.getSystemService(LocaleManager::class.java) ?: return SYSTEM
        val locales = localeManager.applicationLocales
        if (locales.isEmpty) return SYSTEM
        return normalizeStoredLanguage(locales[0].language)
    }
}
