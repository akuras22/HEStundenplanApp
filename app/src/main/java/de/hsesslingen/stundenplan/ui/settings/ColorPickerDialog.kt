package de.hsesslingen.stundenplan.ui.settings

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** A square saturation/value picker + hue bar + hex field, like the standard Android/Google color
 *  picker — used instead of plain RGB sliders so picking an exact shade is actually intuitive. */
@Composable
fun ColorPickerDialog(
    title: String,
    initialColor: Color,
    onConfirm: (Color) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialHsv = remember(initialColor) {
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(initialColor.toArgb(), hsv)
        hsv
    }
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var sat by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }
    val color = Color(AndroidColor.HSVToColor(floatArrayOf(hue, sat, value)))
    var hexText by remember { mutableStateOf(hexOf(color)) }

    fun applyHsv(newHue: Float, newSat: Float, newValue: Float) {
        hue = newHue; sat = newSat; value = newValue
        hexText = hexOf(Color(AndroidColor.HSVToColor(floatArrayOf(newHue, newSat, newValue))))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(color) }) { Text("Übernehmen", fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                SaturationValueSquare(
                    hue = hue,
                    sat = sat,
                    value = value,
                    onChange = { s, v -> applyHsv(hue, s, v) },
                )
                Spacer(Modifier.height(16.dp))
                HueBar(hue = hue, onChange = { h -> applyHsv(h, sat, value) })
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(36.dp).clip(CircleShape).background(color).border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape))
                    OutlinedTextField(
                        value = hexText,
                        onValueChange = { input ->
                            val clean = input.removePrefix("#").take(6)
                            hexText = clean
                            if (clean.length == 6) {
                                runCatching { android.graphics.Color.parseColor("#$clean") }.getOrNull()?.let { argb ->
                                    val hsv = FloatArray(3)
                                    AndroidColor.colorToHSV(argb, hsv)
                                    hue = hsv[0]; sat = hsv[1]; value = hsv[2]
                                }
                            }
                        },
                        label = { Text("Hex") },
                        prefix = { Text("#") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
    )
}

private fun hexOf(color: Color) = "%02X%02X%02X".format(
    (color.red * 255).roundToInt(), (color.green * 255).roundToInt(), (color.blue * 255).roundToInt(),
)

@Composable
private fun SaturationValueSquare(hue: Float, sat: Float, value: Float, onChange: (sat: Float, value: Float) -> Unit) {
    val hueColor = Color(AndroidColor.HSVToColor(floatArrayOf(hue, 1f, 1f)))
    Box(
        Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(hueColor)
            .background(Brush.horizontalGradient(listOf(Color.White, Color.Transparent)))
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
            .pointerInput(Unit) {
                fun update(offset: Offset) {
                    val s = (offset.x / size.width).coerceIn(0f, 1f)
                    val v = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                    onChange(s, v)
                }
                detectTapGestures { update(it) }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val s = (change.position.x / size.width).coerceIn(0f, 1f)
                    val v = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                    onChange(s, v)
                }
            },
    ) {
        Canvas(Modifier.fillMaxWidth().height(180.dp)) {
            val x = sat * size.width
            val y = (1f - value) * size.height
            drawCircle(
                color = Color.White,
                radius = 10.dp.toPx(),
                center = Offset(x, y),
                style = Stroke(width = 2.dp.toPx()),
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.4f),
                radius = 11.dp.toPx(),
                center = Offset(x, y),
                style = Stroke(width = 1.dp.toPx()),
            )
        }
    }
}

@Composable
private fun HueBar(hue: Float, onChange: (Float) -> Unit) {
    val hueColors = remember { (0..360 step 30).map { Color(AndroidColor.HSVToColor(floatArrayOf(it.toFloat(), 1f, 1f))) } }
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(28.dp)
            .clip(MaterialTheme.shapes.small)
            .background(Brush.horizontalGradient(hueColors))
            .pointerInput(Unit) {
                fun update(x: Float) = onChange((x / size.width).coerceIn(0f, 1f) * 360f)
                detectTapGestures { update(it.x) }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ -> onChange((change.position.x / size.width).coerceIn(0f, 1f) * 360f) }
            },
    ) {
        val thumbX = (maxWidth - 4.dp) * (hue / 360f)
        Box(
            Modifier
                .offset(x = thumbX)
                .width(4.dp)
                .height(28.dp)
                .background(Color.White),
        )
    }
}
