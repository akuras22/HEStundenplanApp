package de.hsesslingen.stundenplan.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.hsesslingen.stundenplan.BuildConfig
import de.hsesslingen.stundenplan.ui.StundenplanViewModel

@Composable
fun AboutScreen(viewModel: StundenplanViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    var checking by remember { mutableStateOf(false) }
    var checkResultMessage by remember { mutableStateOf<String?>(null) }
    var clearedCacheMessage by remember { mutableStateOf<String?>(null) }

    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    SettingsPageScaffold(title = "Über die App", onBack = onBack) {
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "Version ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))

            AboutActionRow(title = "Nach Updates suchen", trailing = { if (checking) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.padding(4.dp)) }) {
                if (!checking) {
                    checking = true
                    checkResultMessage = null
                    viewModel.checkForUpdate(force = true) { found ->
                        checking = false
                        checkResultMessage = if (found) null else "Du hast bereits die neueste Version."
                    }
                }
            }
            checkResultMessage?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp))
            }

            AboutActionRow(title = "Zwischenspeicher leeren") {
                viewModel.clearCache()
                clearedCacheMessage = "Zwischenspeicher geleert."
            }
            clearedCacheMessage?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp))
            }

            AboutActionRow(title = "Änderungsprotokoll (GitHub)") {
                openUrl("https://github.com/${BuildConfig.GITHUB_REPO}/releases")
            }
            AboutActionRow(title = "Quellcode (GitHub)") {
                openUrl("https://github.com/${BuildConfig.GITHUB_REPO}")
            }
        }
    }
}

@Composable
private fun AboutActionRow(title: String, trailing: @Composable (() -> Unit)? = null, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}
