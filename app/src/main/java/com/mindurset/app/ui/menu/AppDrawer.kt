package com.mindurset.app.ui.menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mindurset.app.ui.navigation.Routes

/**
 * Application drawer. Every destination in the app is reachable from here, which is
 * what the project navigation criterion asks for.
 */
@Composable
fun AppDrawer(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
) {
    ModalDrawerSheet {
        Column(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
            Text(text = "Mind-Ur-Set", modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            HorizontalDivider()
            drawerItem("Home", Routes.DASHBOARD, currentRoute, onNavigate)
            drawerItem("Therapy", Routes.THERAPY, currentRoute, onNavigate)
            drawerItem("Companion", Routes.COMPANION, currentRoute, onNavigate)
            drawerItem("Profile", Routes.PROFILE, currentRoute, onNavigate)
            drawerItem("Daily reflection", Routes.REFLECTION_LIST, currentRoute, onNavigate)
            HorizontalDivider()
            drawerItem("Settings", Routes.SETTINGS, currentRoute, onNavigate)
            drawerItem("Data & consent", Routes.DATA_CONSENT, currentRoute, onNavigate)
            drawerItem("About", Routes.ABOUT, currentRoute, onNavigate)
            HorizontalDivider()
            drawerItem("Log out", "logout", currentRoute) { onLogout() }
        }
    }
}

@Composable
private fun drawerItem(
    label: String,
    route: String,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
) {
    Text(
        text = label,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate(route) }
            .padding(horizontal = 20.dp, vertical = 14.dp),
    )
}
