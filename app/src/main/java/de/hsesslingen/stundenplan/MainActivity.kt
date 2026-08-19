package de.hsesslingen.stundenplan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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

class MainActivity : ComponentActivity() {

    private val viewModel: StundenplanViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val dynamicColor by viewModel.dynamicColorEnabled.collectAsState()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            StundenplanTheme(darkTheme = darkTheme, dynamicColor = dynamicColor) {
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

    BackHandler(enabled = settingsStack.isNotEmpty()) {
        settingsStack = settingsStack.dropLast(1)
    }

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
