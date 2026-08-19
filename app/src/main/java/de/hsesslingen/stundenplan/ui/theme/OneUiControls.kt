package de.hsesslingen.stundenplan.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

private val SwitchTrackWidth = 52.dp
private val SwitchTrackHeight = 32.dp
private val SwitchThumbSize = 24.dp
private val SwitchThumbPadding = 4.dp

/** Samsung One UI's switch reads noticeably different from stock Material3: a wider, fully rounded
 *  track with a large thumb that nearly fills its height (Material3's default thumb is smaller and
 *  leaves a visible ring around it), and a flat vivid-blue/gray track instead of an outlined one. */
@Composable
fun OneUiSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val trackColor by animateColorAsState(
        if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
        animationSpec = tween(150),
        label = "oneUiSwitchTrack",
    )
    val thumbOffset by animateDpAsState(
        if (checked) SwitchTrackWidth - SwitchThumbSize - SwitchThumbPadding else SwitchThumbPadding,
        animationSpec = tween(150),
        label = "oneUiSwitchThumb",
    )
    Box(
        modifier
            .size(SwitchTrackWidth, SwitchTrackHeight)
            .clip(PillShape)
            .background(trackColor)
            .toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Switch),
    ) {
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset(x = thumbOffset)
                .size(SwitchThumbSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
        )
    }
}

/** Shared dialog chrome for the whole app — flat (no tonal-elevation tint), generously rounded
 *  corners on a slightly-elevated container, matching how One UI 8 draws its popups instead of
 *  stock Material3's more subdued/shadowed dialog card. Every AlertDialog in the app should go
 *  through this rather than calling AlertDialog directly, so they all read as one consistent style. */
@Composable
fun OneUiAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        modifier = modifier,
        title = title,
        text = text,
        shape = OneUiShapes.large,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp,
    )
}
