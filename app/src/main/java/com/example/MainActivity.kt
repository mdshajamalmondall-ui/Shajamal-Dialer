package com.example

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.model.CallerProfileTarget
import com.example.data.model.FavoriteGroup
import com.example.data.telephony.PhoneNumberInfoHelper
import com.example.ui.ShajamalViewModel
import com.example.ui.components.SecurityPinLockOverlay
import com.example.ui.components.SimPickerBottomSheet
import com.example.ui.components.ThemeBackdrop
import com.example.ui.navigation.ShajamalRoutes
import com.example.ui.screens.BlockListScreen
import com.example.ui.screens.ContactProfileScreen
import com.example.ui.screens.ContactsScreen
import com.example.ui.screens.DialerScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.HiddenHistoryScreen
import com.example.ui.screens.PrivacyDashboardScreen
import com.example.ui.screens.PrivateContactsScreen
import com.example.ui.screens.RecentsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SpamDatabaseScreen
import com.example.ui.screens.ThemesScreen
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.ShajamalTheme
import com.example.ui.theme.ThemeCatalog

class MainActivity : ComponentActivity() {

    private val viewModel: ShajamalViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val openedFromTelIntent = handleIncomingDialIntent(intent)

        setContent {
            ShajamalApp(
                viewModel = viewModel,
                initialRoute = if (openedFromTelIntent) ShajamalRoutes.KEYPAD else ShajamalRoutes.RECENTS
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingDialIntent(intent)
    }

    private fun handleIncomingDialIntent(intent: Intent?): Boolean {
        val action = intent?.action ?: return false
        if (action == Intent.ACTION_DIAL || action == Intent.ACTION_VIEW) {
            val data: Uri? = intent.data
            if (data?.scheme == "tel") {
                val number = data.schemeSpecificPart
                if (!number.isNullOrBlank()) {
                    viewModel.setDialNumber(number)
                    return true
                }
            }
        }
        return false
    }
}

private data class NavDestinationItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
private fun ShajamalApp(
    viewModel: ShajamalViewModel,
    initialRoute: String = ShajamalRoutes.RECENTS
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val blockedNumbers by viewModel.blockedNumbers.collectAsStateWithLifecycle()
    val blockedCanonicalKeys by viewModel.blockedCanonicalKeys.collectAsStateWithLifecycle()
    val communityReports by viewModel.communityReports.collectAsStateWithLifecycle()
    val privateContacts by viewModel.privateContacts.collectAsStateWithLifecycle()
    val hiddenCalls by viewModel.hiddenCalls.collectAsStateWithLifecycle()
    val isAppLocked by viewModel.isAppLocked.collectAsStateWithLifecycle()
    val isSettingsLockRequired by viewModel.isSettingsLockRequired.collectAsStateWithLifecycle()
    val numberNotes by viewModel.numberNotes.collectAsStateWithLifecycle()
    val numberGroups by viewModel.numberGroups.collectAsStateWithLifecycle()
    val activeProfileTarget by viewModel.activeProfileTarget.collectAsStateWithLifecycle()
    val dialField by viewModel.dialField.collectAsStateWithLifecycle()
    val simSlots by viewModel.simSlots.collectAsStateWithLifecycle()
    val simBalances by viewModel.simBalances.collectAsStateWithLifecycle()
    val isDefaultDialer by viewModel.isDefaultDialer.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val groupedCallLogs by viewModel.groupedCallLogs.collectAsStateWithLifecycle()
    val recentsCallerReports by viewModel.recentsCallerReports.collectAsStateWithLifecycle()
    val todayCallCount by viewModel.todayCallCount.collectAsStateWithLifecycle()
    val recentsSearchQuery by viewModel.recentsSearchQuery.collectAsStateWithLifecycle()
    val callAnalytics by viewModel.callAnalytics.collectAsStateWithLifecycle()
    val callLogFilter by viewModel.callLogFilter.collectAsStateWithLifecycle()
    val contacts by viewModel.filteredContacts.collectAsStateWithLifecycle()
    val favorites by viewModel.favoriteContacts.collectAsStateWithLifecycle()
    val exactDialedContact by viewModel.exactDialedContact.collectAsStateWithLifecycle()
    val contactSearchQuery by viewModel.contactSearchQuery.collectAsStateWithLifecycle()
    val simPickerNumber by viewModel.simPickerNumber.collectAsStateWithLifecycle()

    val activeTheme = ThemeCatalog.byId(settings.selectedThemeId)
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ShajamalRoutes.KEYPAD
    val snackbarHostState = remember { SnackbarHostState() }

    val requiredPermissions = remember {
        buildList {
            add(Manifest.permission.CALL_PHONE)
            add(Manifest.permission.READ_PHONE_STATE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                add(Manifest.permission.READ_PHONE_NUMBERS)
            }
            add(Manifest.permission.READ_CALL_LOG)
            add(Manifest.permission.WRITE_CALL_LOG)
            add(Manifest.permission.READ_CONTACTS)
            add(Manifest.permission.WRITE_CONTACTS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshDeviceState(force = true)
    }

    val defaultDialerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshDeviceState(force = true)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshDeviceState(force = false)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.permissionPromptTrigger.collect {
            permissionsLauncher.launch(requiredPermissions)
        }
    }

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun requestDefaultDialer() {
        val intent = viewModel.createSetDefaultDialerIntent()
        if (intent != null) {
            runCatching { defaultDialerLauncher.launch(intent) }
                .onFailure { viewModel.emitMessage("Could not open Default Phone App settings.") }
        }
    }

    val currentCanonical = PhoneNumberInfoHelper.canonicalKey(dialField.text)
    val isCurrentNumberBlocked = currentCanonical.isNotBlank() && blockedNumbers.any {
        PhoneNumberInfoHelper.canonicalKey(it.number) == currentCanonical
    }

    ShajamalTheme(
        pictureTheme = activeTheme,
        language = settings.language,
        displayScale = settings.displayScale,
        fontScale = settings.fontScale
    ) {
        val strings = LocalAppStrings.current
        val mainDestinations = listOf(
            NavDestinationItem(
                route = ShajamalRoutes.RECENTS,
                label = strings.navRecents,
                selectedIcon = Icons.Rounded.History,
                unselectedIcon = Icons.Outlined.History,
                testTag = "nav_recents"
            ),
            NavDestinationItem(
                route = ShajamalRoutes.KEYPAD,
                label = strings.navKeypad,
                selectedIcon = Icons.Rounded.Dialpad,
                unselectedIcon = Icons.Outlined.Dialpad,
                testTag = "nav_keypad"
            ),
            NavDestinationItem(
                route = ShajamalRoutes.CONTACTS,
                label = strings.navContacts,
                selectedIcon = Icons.Rounded.Contacts,
                unselectedIcon = Icons.Outlined.Contacts,
                testTag = "nav_contacts"
            ),
            NavDestinationItem(
                route = ShajamalRoutes.FAVORITES,
                label = strings.navFavorites,
                selectedIcon = Icons.Rounded.Star,
                unselectedIcon = Icons.Outlined.StarOutline,
                testTag = "nav_favorites"
            )
        )

        ThemeBackdrop(
            theme = activeTheme,
            doodleDesign = settings.selectedDoodle,
            customImagePath = settings.customThemeImagePath,
            dimAlpha = settings.overlayDimAlpha
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useNavRail = maxWidth >= 700.dp
                val showChrome = currentRoute != ShajamalRoutes.SETTINGS &&
                    currentRoute != ShajamalRoutes.THEMES &&
                    currentRoute != ShajamalRoutes.BLOCK_LIST &&
                    currentRoute != ShajamalRoutes.CONTACT_PROFILE &&
                    currentRoute != ShajamalRoutes.PRIVACY_DASHBOARD &&
                    currentRoute != ShajamalRoutes.PRIVATE_CONTACTS &&
                    currentRoute != ShajamalRoutes.HIDDEN_HISTORY &&
                    currentRoute != ShajamalRoutes.SPAM_DATABASE

                LaunchedEffect(showChrome) {
                    if (showChrome) {
                        viewModel.lockSettingsSession()
                    }
                }

                Scaffold(
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    bottomBar = {
                        if (!useNavRail && showChrome) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                tonalElevation = 0.dp
                            ) {
                                mainDestinations.forEach { dest ->
                                    val selected = currentRoute == dest.route
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { navigateTopLevel(dest.route) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                contentDescription = dest.label
                                            )
                                        },
                                        label = { Text(dest.label, maxLines = 1) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primary,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.testTag(dest.testTag)
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (useNavRail && showChrome) {
                            NavigationRail(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                modifier = Modifier.fillMaxHeight()
                            ) {
                                mainDestinations.forEach { dest ->
                                    val selected = currentRoute == dest.route
                                    NavigationRailItem(
                                        selected = selected,
                                        onClick = { navigateTopLevel(dest.route) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                contentDescription = dest.label
                                            )
                                        },
                                        label = { Text(dest.label, maxLines = 1) },
                                        colors = NavigationRailItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primary,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.testTag(dest.testTag)
                                    )
                                }
                            }
                        }

                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            NavHost(
                                navController = navController,
                                startDestination = initialRoute,
                                enterTransition = { fadeIn(animationSpec = tween(130)) },
                                exitTransition = { fadeOut(animationSpec = tween(90)) },
                                popEnterTransition = { fadeIn(animationSpec = tween(130)) },
                                popExitTransition = { fadeOut(animationSpec = tween(90)) }
                            ) {
                                composable(ShajamalRoutes.RECENTS) {
                                    RecentsScreen(
                                        groupedLogs = groupedCallLogs,
                                        analytics = callAnalytics,
                                        todayCallCount = todayCallCount,
                                        activeFilter = callLogFilter,
                                        searchQuery = recentsSearchQuery,
                                        blockedNumbers = blockedNumbers,
                                        numberNotes = numberNotes,
                                        permissions = permissions,
                                        settings = settings,
                                        communityReports = communityReports,
                                        privateContacts = privateContacts,
                                        simBalances = simBalances,
                                        blockedCanonicalKeys = blockedCanonicalKeys,
                                        callerReports = recentsCallerReports,
                                        onSelectFilter = viewModel::setCallLogFilter,
                                        onSearchQueryChange = viewModel::setRecentsSearchQuery,
                                        onCallChooseSim = viewModel::openSimPickerForNumber,
                                        onCallWithSim = viewModel::placeCall,
                                        onEditInDialer = { number ->
                                            viewModel.setDialNumber(number)
                                            navigateTopLevel(ShajamalRoutes.KEYPAD)
                                        },
                                        onSaveNote = viewModel::saveNumberNote,
                                        onToggleBlock = { num, blocked ->
                                            if (blocked) {
                                                viewModel.unblockNumber(num)
                                            } else {
                                                viewModel.blockNumber(num)
                                            }
                                        },
                                        onSubmitSpamReport = viewModel::reportSpamNumber,
                                        onRefreshSimBalance = { slotIdx ->
                                            viewModel.refreshSimBalance(slotIdx, silent = false)
                                        },
                                        onUpdateSimUssdCode = viewModel::updateSimUssdCode,
                                        onDialSimUssd = viewModel::dialSimUssd,
                                        onDeleteEntry = viewModel::deleteCallLogItem,
                                        onDeleteMultiple = viewModel::deleteMultipleCallLogs,
                                        onOpenFullProfile = { group ->
                                            viewModel.openCallerProfile(
                                                CallerProfileTarget(
                                                    number = group.number,
                                                    displayTitle = group.displayTitle,
                                                    photoUri = group.photoUri,
                                                    contactId = group.contactId
                                                )
                                            )
                                            navController.navigate(ShajamalRoutes.CONTACT_PROFILE)
                                        },
                                        onOpenSettings = {
                                            navController.navigate(ShajamalRoutes.SETTINGS)
                                        },
                                        onOpenPrivacyDashboard = {
                                            navController.navigate(ShajamalRoutes.PRIVACY_DASHBOARD)
                                        },
                                        onRequestPermissions = {
                                            permissionsLauncher.launch(requiredPermissions)
                                        }
                                    )
                                }

                                composable(ShajamalRoutes.KEYPAD) {
                                    DialerScreen(
                                        dialField = dialField,
                                        simSlots = simSlots,
                                        exactMatchedContact = exactDialedContact,
                                        numberNotes = numberNotes,
                                        isCurrentNumberBlocked = isCurrentNumberBlocked,
                                        settings = settings,
                                        permissions = permissions,
                                        isDefaultDialer = isDefaultDialer,
                                        onDialFieldChange = viewModel::onDialFieldChange,
                                        onAppendChar = viewModel::appendDialChar,
                                        onLongPressZero = viewModel::replaceLastCharWithPlus,
                                        onBackspace = viewModel::backspaceDialChar,
                                        onClearAll = viewModel::clearDialNumber,
                                        onCallSlot = viewModel::callCurrentNumberOnSlot,
                                        onAskSimCall = viewModel::requestAskSimForCurrentNumber,
                                        onSaveNote = viewModel::saveNumberNote,
                                        onToggleBlock = { num, blocked ->
                                            if (blocked) {
                                                viewModel.unblockNumber(num)
                                            } else {
                                                viewModel.blockNumber(num)
                                            }
                                        },
                                        onRequestPermissions = {
                                            permissionsLauncher.launch(requiredPermissions)
                                        },
                                        onOpenSettings = {
                                            navController.navigate(ShajamalRoutes.SETTINGS)
                                        },
                                        onRequestDefaultDialer = ::requestDefaultDialer
                                    )
                                }

                                composable(ShajamalRoutes.CONTACTS) {
                                    ContactsScreen(
                                        contacts = contacts,
                                        searchQuery = contactSearchQuery,
                                        permissions = permissions,
                                        onSearchQueryChange = viewModel::setContactSearchQuery,
                                        onCallWithSim = viewModel::placeCall,
                                        onEditInDialer = { number ->
                                            viewModel.setDialNumber(number)
                                            navigateTopLevel(ShajamalRoutes.KEYPAD)
                                        },
                                        onCopyNumber = { num ->
                                            viewModel.emitMessage("Copied $num to clipboard")
                                        },
                                        onOpenProfile = { contact ->
                                            viewModel.openCallerProfile(
                                                CallerProfileTarget(
                                                    number = contact.primaryNumber,
                                                    displayTitle = contact.name,
                                                    photoUri = contact.photoUri,
                                                    contactId = contact.id,
                                                    isStarred = contact.isStarred
                                                )
                                            )
                                            navController.navigate(ShajamalRoutes.CONTACT_PROFILE)
                                        },
                                        onOpenSettings = {
                                            navController.navigate(ShajamalRoutes.SETTINGS)
                                        },
                                        onRequestPermissions = {
                                            permissionsLauncher.launch(requiredPermissions)
                                        }
                                    )
                                }

                                composable(ShajamalRoutes.FAVORITES) {
                                    FavoritesScreen(
                                        favorites = favorites,
                                        permissions = permissions,
                                        onCallContact = viewModel::openSimPickerForNumber,
                                        onOpenSettings = {
                                            navController.navigate(ShajamalRoutes.SETTINGS)
                                        },
                                        onRequestPermissions = {
                                            permissionsLauncher.launch(requiredPermissions)
                                        }
                                    )
                                }

                                composable(ShajamalRoutes.SETTINGS) {
                                    if (isSettingsLockRequired) {
                                        SecurityPinLockOverlay(
                                            title = strings.pinProtectedSettingsTitle,
                                            subtitle = strings.unlockSettingsPrompt,
                                            allowDismiss = true,
                                            onDismiss = { navController.popBackStack() },
                                            onVerifyPin = viewModel::verifyPin,
                                            onUnlocked = viewModel::unlockSettings
                                        )
                                    } else {
                                        SettingsScreen(
                                            settings = settings,
                                            simSlots = simSlots,
                                            blockedCount = blockedNumbers.size,
                                            isDefaultDialer = isDefaultDialer,
                                            permissions = permissions,
                                            onBack = { navController.popBackStack() },
                                            onOpenThemesGallery = {
                                                navController.navigate(ShajamalRoutes.THEMES)
                                            },
                                            onOpenBlockList = {
                                                navController.navigate(ShajamalRoutes.BLOCK_LIST)
                                            },
                                            onOpenPrivacyDashboard = {
                                                navController.navigate(ShajamalRoutes.PRIVACY_DASHBOARD)
                                            },
                                            onSelectTheme = viewModel::selectTheme,
                                            onSelectDoodle = viewModel::selectDoodle,
                                            onSelectLanguage = viewModel::setLanguage,
                                            onPickSimRingtone = viewModel::setSimRingtone,
                                            onResetSimRingtone = viewModel::clearSimRingtone,
                                            onRequestDefaultDialer = ::requestDefaultDialer,
                                            onRequestPermissions = {
                                                permissionsLauncher.launch(requiredPermissions)
                                            },
                                            onToggleDualSimMode = viewModel::setDualSimMode,
                                            onToggleDtmfTones = viewModel::setDtmfTonesEnabled,
                                            onToggleHapticFeedback = viewModel::setHapticFeedbackEnabled,
                                            onToggleAutoCleanPaste = viewModel::setAutoCleanPastedNumber,
                                            onDisplayScaleChange = viewModel::setDisplayScale,
                                            onFontScaleChange = viewModel::setFontScale
                                        )
                                    }
                                }

                                composable(ShajamalRoutes.PRIVACY_DASHBOARD) {
                                    if (isSettingsLockRequired) {
                                        SecurityPinLockOverlay(
                                            title = strings.pinProtectedSettingsTitle,
                                            subtitle = strings.unlockSettingsPrompt,
                                            allowDismiss = true,
                                            onDismiss = { navController.popBackStack() },
                                            onVerifyPin = viewModel::verifyPin,
                                            onUnlocked = viewModel::unlockSettings
                                        )
                                    } else {
                                        PrivacyDashboardScreen(
                                            settings = settings,
                                            isDefaultDialer = isDefaultDialer,
                                            blockedCount = blockedNumbers.size,
                                            spamReportCount = communityReports.size,
                                            privateContactCount = privateContacts.size,
                                            hiddenCallCount = hiddenCalls.size,
                                            onBack = { navController.popBackStack() },
                                            onOpenPrivateContacts = {
                                                navController.navigate(ShajamalRoutes.PRIVATE_CONTACTS)
                                            },
                                            onOpenHiddenHistory = {
                                                navController.navigate(ShajamalRoutes.HIDDEN_HISTORY)
                                            },
                                            onOpenSpamDatabase = {
                                                navController.navigate(ShajamalRoutes.SPAM_DATABASE)
                                            },
                                            onToggleCallerId = viewModel::setCallerIdEnabled,
                                            onToggleSpamDetection = viewModel::setSpamDetectionEnabled,
                                            onToggleFraudWarning = viewModel::setFraudWarningEnabled,
                                            onToggleAutoSpamCategory = viewModel::setAutoSpamCategoryEnabled,
                                            onToggleAppLock = viewModel::setAppLockEnabled,
                                            onTogglePinProtectedSettings = viewModel::setPinProtectedSettingsEnabled,
                                            onToggleSensitiveProtection = viewModel::setSensitiveContactProtectionEnabled,
                                            onSetSecurityPin = viewModel::setSecurityPin,
                                            onRemoveSecurityPin = viewModel::removeSecurityPin,
                                            onExportBackup = viewModel::exportEncryptedBackupToUri,
                                            onRestoreBackup = viewModel::restoreEncryptedBackupFromUri,
                                            onShowMessage = viewModel::emitMessage
                                        )
                                    }
                                }

                                composable(ShajamalRoutes.PRIVATE_CONTACTS) {
                                    PrivateContactsScreen(
                                        privateContacts = privateContacts,
                                        onBack = { navController.popBackStack() },
                                        onAddPrivateContact = viewModel::addOrUpdatePrivateContact,
                                        onToggleSensitiveMask = viewModel::togglePrivateContactMask,
                                        onDeletePrivateContact = viewModel::removePrivateContact,
                                        onCallChooseSim = viewModel::openSimPickerForNumber
                                    )
                                }

                                composable(ShajamalRoutes.HIDDEN_HISTORY) {
                                    HiddenHistoryScreen(
                                        hiddenCalls = hiddenCalls,
                                        onBack = { navController.popBackStack() },
                                        onCallChooseSim = viewModel::openSimPickerForNumber,
                                        onDeleteHiddenCall = viewModel::deleteHiddenCall,
                                        onClearAll = viewModel::clearHiddenHistory
                                    )
                                }

                                composable(ShajamalRoutes.SPAM_DATABASE) {
                                    SpamDatabaseScreen(
                                        language = settings.language,
                                        communityReports = communityReports.values.sortedByDescending { it.reportedAtMillis },
                                        blockedNumbers = blockedNumbers,
                                        onBack = { navController.popBackStack() },
                                        onSubmitReport = viewModel::reportSpamNumber,
                                        onRemoveReport = viewModel::removeSpamReport,
                                        onUnblockNumber = viewModel::unblockNumber
                                    )
                                }

                                composable(ShajamalRoutes.THEMES) {
                                    ThemesScreen(
                                        settings = settings,
                                        onSelectTheme = viewModel::selectTheme,
                                        onSelectDoodle = viewModel::selectDoodle,
                                        onCustomPhotoPicked = viewModel::saveCustomPhotoTheme,
                                        onDimAlphaChange = viewModel::setOverlayDimAlpha,
                                        onBack = { navController.popBackStack() }
                                    )
                                }

                                composable(ShajamalRoutes.BLOCK_LIST) {
                                    BlockListScreen(
                                        blockedNumbers = blockedNumbers,
                                        onAddBlockedNumber = viewModel::blockNumber,
                                        onUnblockNumber = viewModel::unblockNumber,
                                        onBack = { navController.popBackStack() }
                                    )
                                }

                                composable(ShajamalRoutes.CONTACT_PROFILE) {
                                    activeProfileTarget?.let { target ->
                                        val key = PhoneNumberInfoHelper.canonicalKey(target.number)
                                        val savedNote = if (key.isNotBlank()) numberNotes[key] else null
                                        val group = if (key.isNotBlank()) {
                                            numberGroups[key] ?: FavoriteGroup.NONE
                                        } else {
                                            FavoriteGroup.NONE
                                        }
                                        val blocked = key.isNotBlank() && blockedNumbers.any {
                                            PhoneNumberInfoHelper.canonicalKey(it.number) == key
                                        }
                                        val privateMatch = privateContacts.firstOrNull {
                                            val pk = PhoneNumberInfoHelper.canonicalKey(it.number)
                                            (key.isNotBlank() && pk == key) || it.number == target.number
                                        }
                                        ContactProfileScreen(
                                            target = target,
                                            savedNote = savedNote,
                                            selectedGroup = group,
                                            isBlocked = blocked,
                                            isPrivateContact = privateMatch != null,
                                            language = settings.language,
                                            onBack = { navController.popBackStack() },
                                            onToggleStar = {
                                                viewModel.toggleContactFavorite(
                                                    target.contactId,
                                                    target.isStarred
                                                )
                                            },
                                            onCallSim1 = { viewModel.placeCall(target.number, 0) },
                                            onCallSim2 = { viewModel.placeCall(target.number, 1) },
                                            onSelectGroup = { newGroup ->
                                                viewModel.setNumberGroup(target.number, newGroup)
                                            },
                                            onSaveNote = { newNote ->
                                                viewModel.saveNumberNote(target.number, newNote)
                                            },
                                            onToggleBlock = {
                                                if (blocked) {
                                                    viewModel.unblockNumber(target.number)
                                                } else {
                                                    viewModel.blockNumber(target.number, target.displayTitle)
                                                }
                                            },
                                            onTogglePrivateContact = { makePrivate ->
                                                if (makePrivate) {
                                                    viewModel.addOrUpdatePrivateContact(
                                                        name = target.displayTitle,
                                                        number = target.number,
                                                        note = savedNote.orEmpty(),
                                                        sensitiveMask = true
                                                    )
                                                } else if (privateMatch != null) {
                                                    viewModel.removePrivateContact(privateMatch.id)
                                                }
                                            },
                                            onSubmitSpamReport = { category, note, alsoBlock ->
                                                viewModel.reportSpamNumber(target.number, category, note, alsoBlock)
                                            },
                                            onShowMessage = viewModel::emitMessage
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isAppLocked) {
                SecurityPinLockOverlay(
                    title = strings.unlockAppPrompt,
                    subtitle = strings.appLockSub,
                    allowDismiss = false,
                    onVerifyPin = viewModel::verifyPin,
                    onUnlocked = viewModel::unlockApp
                )
            }

            simPickerNumber?.let { number ->
                SimPickerBottomSheet(
                    phoneNumber = number,
                    simSlots = simSlots,
                    onSelectSlot = { slotIndex ->
                        viewModel.placeCall(number, slotIndex)
                    },
                    onDismiss = viewModel::dismissSimPicker
                )
            }
        }
    }
}
