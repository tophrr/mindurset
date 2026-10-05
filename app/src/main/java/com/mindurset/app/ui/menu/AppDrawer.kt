package com.mindurset.app.ui.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mindurset.app.ui.navigation.Routes
import com.mindurset.app.ui.theme.AppIcons
import com.mindurset.app.ui.theme.CardShadow
import com.mindurset.app.ui.theme.NavyDeep
import com.mindurset.app.ui.theme.SurfaceWhite

/**
 * Application drawer: profile header, then every destination with its monoline icon.
 * Every screen in the app is reachable from here.
 */
@Composable
fun AppDrawer(
    currentRoute: String?,
    userName: String,
    userEmail: String,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
) {
    ModalDrawerSheet(
        drawerContainerColor = SurfaceWhite,
        modifier = Modifier.width(304.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 18.dp)) {
            DrawerHeader(name = userName, email = userEmail)
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            drawerItem("Home", Routes.DASHBOARD, AppIcons.Home, currentRoute, onNavigate)
            drawerItem("Therapy", Routes.THERAPY, AppIcons.Therapy, currentRoute, onNavigate)
            drawerItem("Companion", Routes.COMPANION, AppIcons.Companion, currentRoute, onNavigate)
            drawerItem("Profile", Routes.PROFILE, AppIcons.Profile, currentRoute, onNavigate)
            drawerItem("Daily reflection", Routes.REFLECTION_LIST, AppIcons.Book, currentRoute, onNavigate)
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            drawerItem("Settings", Routes.SETTINGS, AppIcons.Settings, currentRoute, onNavigate)
            drawerItem("Data & consent", Routes.DATA_CONSENT, AppIcons.Shield, currentRoute, onNavigate)
            drawerItem("About", Routes.ABOUT, AppIcons.Info, currentRoute, onNavigate)
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            drawerItem("Log out", "logout", AppIcons.Logout, currentRoute) { onLogout() }
        }
    }
}

@Composable
private fun DrawerHeader(name: String, email: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .shadow(4.dp, CircleShape, ambientColor = CardShadow, spotColor = CardShadow),
            contentAlignment = Alignment.Center,
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = name.trim().take(1).uppercase().ifEmpty { "M" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = NavyDeep,
                    )
                }
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (email.isNotBlank()) {
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun drawerItem(
    label: String,
    route: String,
    icon: ImageVector,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
) {
    val selected = currentRoute == route
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp)) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (selected) MaterialTheme.colorScheme.primaryContainer else SurfaceWhite,
            onClick = { onNavigate(route) },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = if (selected) NavyDeep else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (selected) NavyDeep else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
