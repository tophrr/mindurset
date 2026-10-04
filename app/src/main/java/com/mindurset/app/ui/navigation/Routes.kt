package com.mindurset.app.ui.navigation

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val DASHBOARD = "dashboard"
    const val CHECK_IN = "checkin"
    const val REFLECTION_LIST = "reflection_list"
    const val REFLECTION_FORM = "reflection_form"
    const val REFLECTION_DETAIL = "reflection_detail"

    const val ARG_ENTRY_ID = "entryId"

    const val THERAPY = "therapy"
    const val COMPANION = "companion"
    const val PROFILE = "profile"
    const val PROFILE_EDIT = "profile_edit"
    const val SETTINGS = "settings"
    const val DATA_CONSENT = "data_consent"
    const val ABOUT = "about"

    const val REFLECTION_FORM_ROUTE = "$REFLECTION_FORM?$ARG_ENTRY_ID={$ARG_ENTRY_ID}"
    const val REFLECTION_DETAIL_ROUTE = "$REFLECTION_DETAIL/{$ARG_ENTRY_ID}"

    fun reflectionForm(entryId: String? = null): String =
        if (entryId == null) REFLECTION_FORM else "$REFLECTION_FORM?$ARG_ENTRY_ID=$entryId"

    fun reflectionDetail(entryId: String): String = "$REFLECTION_DETAIL/$entryId"

    val topLevelDestinations: List<String> = listOf(DASHBOARD, THERAPY, COMPANION, PROFILE)

    fun isTopLevel(route: String?): Boolean = route != null && route in topLevelDestinations
}
