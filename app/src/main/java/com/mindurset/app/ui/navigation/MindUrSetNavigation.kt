package com.mindurset.app.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mindurset.app.R
import com.mindurset.app.di.AppContainer
import com.mindurset.app.di.LocalAppContainer
import com.mindurset.app.ui.auth.LoginScreen
import com.mindurset.app.ui.auth.RegisterScreen
import com.mindurset.app.ui.companion.CompanionScreen
import com.mindurset.app.ui.dashboard.DashboardScreen
import com.mindurset.app.ui.home.checkin.CheckInScreen
import com.mindurset.app.ui.info.AboutScreen
import com.mindurset.app.ui.info.DataConsentScreen
import com.mindurset.app.domain.model.UserProfile
import com.mindurset.app.ui.common.LogoutConfirmDialog
import com.mindurset.app.ui.menu.AppDrawer
import com.mindurset.app.ui.profile.ProfileEditScreen
import com.mindurset.app.ui.profile.ProfileScreen
import com.mindurset.app.ui.profile.SettingsScreen
import com.mindurset.app.ui.reflection.ReflectionDetailScreen
import com.mindurset.app.ui.reflection.ReflectionFormScreen
import com.mindurset.app.ui.reflection.ReflectionListScreen
import com.mindurset.app.ui.theme.AppIcons
import com.mindurset.app.ui.theme.CardShadow
import com.mindurset.app.ui.theme.SurfaceWhite
import com.mindurset.app.ui.therapy.TherapyScreen
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@Composable
fun MindUrSetNavigation(
    startDestination: String,
    container: AppContainer = LocalAppContainer.current,
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showShell = Routes.isTopLevel(currentRoute)

    val session by container.sessionStore.session.collectAsState(initial = null)
    val userProfile by remember(session?.userId) {
        session?.userId?.let { container.profileRepository.observe(it) } ?: flowOf<UserProfile?>(null)
    }.collectAsState(initial = null)
    val drawerName = userProfile?.displayName?.takeIf { it.isNotBlank() }
        ?: session?.email?.substringBefore('@')
        ?: "Guest"
    val drawerEmail = session?.email.orEmpty()

    val openDrawer: () -> Unit = { scope.launch { drawerState.open() } }
    val closeDrawer: () -> Unit = { scope.launch { drawerState.close() } }
    val navigate: (String) -> Unit = { route ->
        closeDrawer()
        if (route == "logout") {
            navController.navigate(Routes.LOGIN) {
                popUpTo(0) { inclusive = true }
            }
        } else if (route != currentRoute) {
            navController.navigate(route) {
                popUpTo(Routes.DASHBOARD) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    var logoutDialogVisible by remember { mutableStateOf(false) }
    val performLogout: (Boolean) -> Unit = { deleteLocalData ->
        scope.launch {
            val session = container.sessionStore.current()
            if (deleteLocalData && session != null) {
                container.authRepository.deleteAccount(session.userId)
            }
            container.sessionStore.signOut()
            logoutDialogVisible = false
            navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = showShell,
        drawerContent = {
            AppDrawer(
                currentRoute = currentRoute,
                userName = drawerName,
                userEmail = drawerEmail,
                onNavigate = navigate,
                onLogout = {
                    closeDrawer()
                    logoutDialogVisible = true
                },
            )
        },
    ) {
        Scaffold(
            bottomBar = {
                if (showShell) {
                    FloatingPillNav(currentRoute = currentRoute, onNavigate = navigate)
                }
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = startDestination,
                // Bottom inset is owned by each screen (FloatingNavSpace); taking it here as
                // well double-counted the pill height and lifted bottom-pinned content.
                modifier = Modifier.padding(PaddingValues(top = innerPadding.calculateTopPadding())),
            ) {
                composable(Routes.LOGIN) {
                    LoginScreen(
                        onSignedIn = { navController.toDashboard() },
                        onRegister = { navController.navigate(Routes.REGISTER) },
                        onSkip = { navController.toDashboard() },
                    )
                }
                composable(Routes.REGISTER) {
                    RegisterScreen(
                        onRegistered = { navController.toDashboard() },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Routes.DASHBOARD) {
                    DashboardScreen(
                        onOpenCheckIn = { navController.navigate(Routes.CHECK_IN) },
                        onOpenReflection = { navController.navigate(Routes.REFLECTION_LIST) },
                        onOpenCompanion = { navController.navigate(Routes.COMPANION) },
                        onOpenTherapy = { navController.navigate(Routes.THERAPY) },
                        onOpenDrawer = openDrawer,
                    )
                }
                composable(Routes.CHECK_IN) {
                    CheckInScreen(
                        onDone = { navController.popBackStack() },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Routes.REFLECTION_LIST) {
                    ReflectionListScreen(
                        onNew = { navController.navigate(Routes.reflectionForm()) },
                        onOpen = { id -> navController.navigate(Routes.reflectionDetail(id)) },
                        onBack = { navController.popBackStack() },
                        onOpenDrawer = openDrawer,
                    )
                }
                composable(
                    route = Routes.REFLECTION_FORM_ROUTE,
                    arguments = listOf(navArgument(Routes.ARG_ENTRY_ID) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }),
                ) { entry ->
                    ReflectionFormScreen(
                        entryId = entry.arguments?.getString(Routes.ARG_ENTRY_ID),
                        onSaved = { navController.popBackStack() },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(
                    route = Routes.REFLECTION_DETAIL_ROUTE,
                    arguments = listOf(navArgument(Routes.ARG_ENTRY_ID) { type = NavType.StringType }),
                ) { entry ->
                    val id = entry.arguments?.getString(Routes.ARG_ENTRY_ID).orEmpty()
                    ReflectionDetailScreen(
                        entryId = id,
                        onEdit = { navController.navigate(Routes.reflectionForm(id)) },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Routes.THERAPY) { TherapyScreen(onOpenDrawer = openDrawer) }
                composable(Routes.COMPANION) { CompanionScreen(onOpenDrawer = openDrawer) }
                composable(Routes.PROFILE) {
                    ProfileScreen(
                        onEditProfile = { navController.navigate(Routes.PROFILE_EDIT) },
                        onSettings = { navController.navigate(Routes.SETTINGS) },
                        onLogout = {
                            navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                        },
                        onOpenDrawer = openDrawer,
                    )
                }
                composable(Routes.PROFILE_EDIT) {
                    ProfileEditScreen(
                        onSaved = { navController.popBackStack() },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Routes.SETTINGS) { SettingsScreen(onBack = { navController.popBackStack() }) }
                composable(Routes.DATA_CONSENT) { DataConsentScreen(onBack = { navController.popBackStack() }) }
                composable(Routes.ABOUT) { AboutScreen(onBack = { navController.popBackStack() }) }
            }

            if (logoutDialogVisible) {
                LogoutConfirmDialog(
                    title = stringResource(R.string.profile_logout_title),
                    message = stringResource(R.string.profile_logout_message),
                    keepLabel = stringResource(R.string.profile_logout_keep),
                    cancelLabel = stringResource(R.string.profile_logout_cancel),
                    deleteLabel = stringResource(R.string.profile_logout_delete),
                    onKeepData = { performLogout(false) },
                    onCancel = { logoutDialogVisible = false },
                    onDeleteData = { performLogout(true) },
                )
            }
        }
    }
}

private fun NavHostController.toDashboard() {
    navigate(Routes.DASHBOARD) {
        popUpTo(0) { inclusive = true }
        launchSingleTop = true
    }
}

/** Floating pill navigation, as in the project mockups: white rounded bar with the
 *  selected destination expanded into a navy pill carrying its label. */
@Composable
private fun FloatingPillNav(currentRoute: String?, onNavigate: (String) -> Unit) {
    val items = listOf(
        Triple("Home", Routes.DASHBOARD, AppIcons.Home),
        Triple("Therapy", Routes.THERAPY, AppIcons.Therapy),
        Triple("Companion", Routes.COMPANION, AppIcons.Companion),
        Triple("Profile", Routes.PROFILE, AppIcons.Profile),
    )
    val pillShape = RoundedCornerShape(34.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = pillShape,
            color = SurfaceWhite,
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .shadow(12.dp, pillShape, ambientColor = CardShadow, spotColor = CardShadow),
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                items.forEach { (label, route, icon) ->
                    val selected = currentRoute == route
                    Box(
                        modifier = Modifier.weight(1f).padding(horizontal = 3.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary else SurfaceWhite,
                            onClick = { onNavigate(route) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    modifier = Modifier.size(24.dp),
                                    tint = if (selected) SurfaceWhite else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (selected) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = SurfaceWhite,
                                        maxLines = 1,
                                        softWrap = false,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
