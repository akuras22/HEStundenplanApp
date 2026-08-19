package de.hsesslingen.stundenplan.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.CalendarViewMonth
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.hsesslingen.stundenplan.BuildConfig
import de.hsesslingen.stundenplan.ui.ChangelogDialog
import de.hsesslingen.stundenplan.ui.StundenplanViewModel
import de.hsesslingen.stundenplan.ui.theme.PillShape

@Composable
fun AboutScreen(viewModel: StundenplanViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    var checking by remember { mutableStateOf(false) }
    var showChangelog by remember { mutableStateOf(false) }
    var changelogVersion by remember { mutableStateOf(BuildConfig.VERSION_NAME) }
    var changelogNotes by remember { mutableStateOf<String?>(null) }
    var loadingChangelog by remember { mutableStateOf(false) }

    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    SettingsPageScaffold(title = "Über die App", onBack = onBack) {
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            AppIdentityHeader()

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AboutActionRow(
                    icon = Icons.Filled.SystemUpdate,
                    title = "Nach Updates suchen",
                    subtitle = "Version ${BuildConfig.VERSION_NAME} installiert",
                    trailing = { if (checking) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp)) },
                ) {
                    if (!checking) {
                        checking = true
                        viewModel.checkForUpdate(force = true) { found ->
                            checking = false
                            viewModel.postFeedback(if (found) "Update gefunden!" else "Du hast bereits die neueste Version.")
                        }
                    }
                }
                AboutActionRow(
                    icon = Icons.Filled.DeleteSweep,
                    title = "Zwischenspeicher leeren",
                    subtitle = "Löscht zwischengespeicherte Stundenpläne",
                ) {
                    viewModel.clearCache()
                }
                AboutActionRow(
                    icon = Icons.Filled.Article,
                    title = "Änderungsprotokoll",
                    subtitle = "Was ist neu in dieser Version",
                    trailing = { if (loadingChangelog) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp)) },
                ) {
                    if (!loadingChangelog) {
                        loadingChangelog = true
                        viewModel.fetchChangelog { info ->
                            loadingChangelog = false
                            changelogVersion = info?.versionName ?: BuildConfig.VERSION_NAME
                            changelogNotes = info?.releaseNotes
                            showChangelog = true
                        }
                    }
                }
                AboutActionRow(
                    icon = Icons.Filled.Code,
                    title = "Quellcode",
                    subtitle = "Auf GitHub ansehen",
                ) {
                    openUrl("https://github.com/${BuildConfig.GITHUB_REPO}")
                }
            }
        }
    }

    if (showChangelog) {
        ChangelogDialog(
            versionName = changelogVersion,
            releaseNotes = changelogNotes,
            onOpenGithub = { openUrl("https://github.com/${BuildConfig.GITHUB_REPO}/releases") },
            onDismiss = { showChangelog = false },
        )
    }
}

@Composable
private fun AppIdentityHeader() {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(vertical = 28.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.CalendarViewMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        Text("HEStundenplan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(4.dp))
        Text(
            "Version ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Inoffizieller Stundenplan für Studierende der Hochschule Esslingen",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AboutActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(36.dp).clip(PillShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (trailing != null) trailing() else Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
