package com.mindurset.app.domain.usecase

enum class EntryDestination {
    LOGIN,
    CONSENT,
    ONBOARDING,
    DASHBOARD,
}

/**
 * Pure entry gate shared by launch routing and post-auth/post-consent navigation.
 * Once [onboardingCompleted] is true, [EntryDestination.ONBOARDING] is never returned.
 */
object ResolveEntryDestination {
    fun resolve(hasSession: Boolean, hasConsent: Boolean, onboardingCompleted: Boolean): EntryDestination = when {
        !hasSession -> EntryDestination.LOGIN
        !hasConsent -> EntryDestination.CONSENT
        !onboardingCompleted -> EntryDestination.ONBOARDING
        else -> EntryDestination.DASHBOARD
    }
}
