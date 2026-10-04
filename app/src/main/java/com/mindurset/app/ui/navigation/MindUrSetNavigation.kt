package com.mindurset.app.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.mindurset.app.ui.menu.AppDrawer
import com.mindurset.app.ui.profile.ProfileEditScreen
import com.mindurset.app.ui.profile.ProfileScreen
import com.mindurset.app.ui.profile.SettingsScreen
import com.mindurset.app.ui.reflection.ReflectionDetailScreen
import com.mindurset.app.ui.reflection.ReflectionFormScreen
import com.mindurset.app.ui.reflection.ReflectionListScreen
import com.mindurset.app.ui.therapy.TherapyScreen
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
                    BottomBar(currentRoute = currentRoute, onNavigate = navigate)
                }
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.padding(innerPadding),
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
                AlertDialog(
                    onDismissRequest = { logoutDialogVisible = false },
                    title = { Text(stringResource(R.string.profile_logout_title)) },
                    text = {
                        Column {
                            Text(stringResource(R.string.profile_logout_message))
                            TextButton(onClick = { performLogout(true) }) {
                                Text(stringResource(R.string.profile_logout_delete))
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { performLogout(false) }) {
                            Text(stringResource(R.string.profile_logout_keep))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { logoutDialogVisible = false }) {
                            Text(stringResource(R.string.profile_logout_cancel))
                        }
                    },
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

@Composable
private fun BottomBar(currentRoute: String?, onNavigate: (String) -> Unit) {
    val items = listOf(
        Triple("Home", Routes.DASHBOARD, "\u2302"),
        Triple("Therapy", Routes.THERAPY, "\u2698"),
        Triple("Companion", Routes.COMPANION, "\u263A"),
        Triple("Profile", Routes.PROFILE, "\u263B"),
    )
    NavigationBar {
        items.forEach { (label, route, glyph) ->
            NavigationBarItem(
                selected = currentRoute == route,
                onClick = { onNavigate(route) },
                icon = { Text(glyph) },
                label = { Text(label) },
            )
        }
    }
}
