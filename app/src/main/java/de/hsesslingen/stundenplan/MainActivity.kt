package de.hsesslingen.stundenplan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import de.hsesslingen.stundenplan.ui.PlanScreen
import de.hsesslingen.stundenplan.ui.SettingsScreen
import de.hsesslingen.stundenplan.ui.StundenplanViewModel
import de.hsesslingen.stundenplan.ui.theme.StundenplanTheme

class MainActivity : ComponentActivity() {

    private val viewModel: StundenplanViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StundenplanTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    StundenplanApp(viewModel)
                }
            }
        }
    }
}

@Composable
private fun StundenplanApp(viewModel: StundenplanViewModel) {
    var showSettings by remember { mutableStateOf(false) }
    if (showSettings) {
        SettingsScreen(viewModel = viewModel, onBack = { showSettings = false })
    } else {
        PlanScreen(viewModel = viewModel, onOpenSettings = { showSettings = true })
    }
}
