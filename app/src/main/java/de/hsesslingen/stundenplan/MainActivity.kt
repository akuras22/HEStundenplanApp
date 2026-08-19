package de.hsesslingen.stundenplan

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import de.hsesslingen.stundenplan.data.EXTRA_OPEN_DATE
import de.hsesslingen.stundenplan.data.ThemeMode
import de.hsesslingen.stundenplan.ui.PlanScreen
import de.hsesslingen.stundenplan.ui.StundenplanViewModel
import de.hsesslingen.stundenplan.ui.UpdateDialog
import de.hsesslingen.stundenplan.ui.settings.AboutScreen
import de.hsesslingen.stundenplan.ui.settings.AppearanceScreen
import de.hsesslingen.stundenplan.ui.settings.NotificationsScreen
import de.hsesslingen.stundenplan.ui.settings.SettingsHubScreen
import de.hsesslingen.stundenplan.ui.settings.SettingsRoute
import de.hsesslingen.stundenplan.ui.settings.StudiengangScreen
import de.hsesslingen.stundenplan.ui.theme.StundenplanTheme
import java.time.LocalDate

class MainActivity : ComponentActivity() {

    private val viewModel: StundenplanViewModel by viewModels()

    /** Reminder notifications (and the "Test-Benachrichtigung senden" button) launch with
     *  EXTRA_OPEN_DATE set — MainActivity is singleTask (see AndroidManifest) so a tap while the
     *  app is already running arrives here via onNewIntent instead of a fresh onCreate. */
    private fun handleOpenDateIntent(intent: Intent?) {
        val raw = intent?.getStringExtra(EXTRA_OPEN_DATE) ?: return
        val date = runCatching { LocalDate.parse(raw) }.getOrNull() ?: return
        viewModel.requestOpenDate(date)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOpenDateIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleOpenDateIntent(intent)
        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val dynamicColor by viewModel.dynamicColorEnabled.collectAsState()
            val accentPreset by viewModel.accentPreset.collectAsState()
            val customAccentColor by viewModel.customAccentColor.collectAsState()
            val customBackgroundColor by viewModel.customBackgroundColor.collectAsState()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            StundenplanTheme(
                darkTheme = darkTheme,
                dynamicColor = dynamicColor,
                accentPreset = accentPreset,
                customAccentColor = customAccentColor,
                customBackgroundColor = customBackgroundColor,
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    StundenplanApp(viewModel)
                }
            }
        }
    }
}

@Composable
private fun StundenplanApp(viewModel: StundenplanViewModel) {
    // The settings section is its own small back-stack (empty = showing the Plan screen); pushing
    // SettingsRoute.Hub is what "open settings" means, and each sub-page pushes on top of that.
    var settingsStack by remember { mutableStateOf<List<SettingsRoute>>(emptyList()) }
    val updateState by viewModel.updateState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val pendingOpenDate by viewModel.pendingOpenDate.collectAsState()

    // A reminder notification tap should always land on the Plan screen's Tag-Ansicht, even if
    // the user was mid-way through Einstellungen when they tapped it.
    LaunchedEffect(pendingOpenDate) {
        if (pendingOpenDate != null) settingsStack = emptyList()
    }

    // Action-feedback messages (e.g. "Zwischenspeicher geleert.") surface as a Snackbar here,
    // hoisted above the screen switch so they show no matter which Einstellungen sub-page (or the
    // Plan screen) triggered them.
    LaunchedEffect(Unit) {
        viewModel.feedback.collect { message -> snackbarHostState.showSnackbar(message) }
    }

    BackHandler(enabled = settingsStack.isNotEmpty()) {
        settingsStack = settingsStack.dropLast(1)
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    shape = MaterialTheme.shapes.large,
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                ) { Text(data.visuals.message) }
            }
        },
    ) { _ ->
        Box(Modifier.fillMaxSize()) {
            when (settingsStack.lastOrNull()) {
                null -> PlanScreen(viewModel = viewModel, onOpenSettings = { settingsStack = listOf(SettingsRoute.Hub) })
                SettingsRoute.Hub -> SettingsHubScreen(
                    onBack = { settingsStack = emptyList() },
                    onNavigate = { route -> settingsStack = settingsStack + route },
                )
                SettingsRoute.Studiengaenge -> StudiengangScreen(viewModel, onBack = { settingsStack = settingsStack.dropLast(1) })
                SettingsRoute.Notifications -> NotificationsScreen(viewModel, onBack = { settingsStack = settingsStack.dropLast(1) })
                SettingsRoute.Appearance -> AppearanceScreen(viewModel, onBack = { settingsStack = settingsStack.dropLast(1) })
                SettingsRoute.About -> AboutScreen(viewModel, onBack = { settingsStack = settingsStack.dropLast(1) })
            }
        }
    }

    // Hoisted above the screen switch (not inside PlanScreen) so it still shows while the user is
    // browsing any Einstellungen sub-page, not just while looking at the Plan screen.
    updateState.available?.let { info ->
        UpdateDialog(
            info = info,
            onInstall = { viewModel.openUpdateInBrowser() },
            onDismiss = { viewModel.dismissUpdate() },
        )
    }
}
