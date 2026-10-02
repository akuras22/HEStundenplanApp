package de.hsesslingen.stundenplan.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import de.hsesslingen.stundenplan.BuildConfig
import de.hsesslingen.stundenplan.data.RoomLocation
import de.hsesslingen.stundenplan.data.osmTileUrl
import de.hsesslingen.stundenplan.data.osmWebUrl
import de.hsesslingen.stundenplan.data.tileIndex
import de.hsesslingen.stundenplan.data.tilePosition
import de.hsesslingen.stundenplan.ui.theme.PillShape

private const val MAP_ZOOM = 17
// OSM tiles are 256 px; drawn at 128 dp they come out at roughly their native sharpness on a
// typical phone, with street names still readable.
private val TILE_SIZE = 128.dp
private val MAP_HEIGHT = 150.dp

// OpenStreetMap's tile usage policy asks every app to identify itself instead of sending a
// library's generic User-Agent (which gets blocked).
private val TILE_USER_AGENT = "HEStundenplan/${BuildConfig.VERSION_NAME} (+https://github.com/akuras22/HEStundenplanApp)"

/** "Wo ist das?" for a room in the event detail dialog: campus, building and floor, a small map
 *  with the building pinned, and a way to open it in a real map app for directions. */
@Composable
internal fun RoomLocationSection(location: RoomLocation, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text("Wo ist das?", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            listOfNotNull(location.buildingName, location.floorLabel).joinToString(" · "),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
        Text(
            location.campusName + " · " + location.address + if (location.isApproximate) " (ungefähre Lage)" else "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        MiniMap(
            latitude = location.latitude,
            longitude = location.longitude,
            onClick = { openInMaps(context, location) },
        )
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier
                .clip(PillShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                .clickable { openInMaps(context, location) }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Outlined.Map, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Text("In Karten öffnen", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

/** A few OpenStreetMap tiles stitched together around the spot, with a pin in the middle. Not an
 *  interactive map — a tap opens a real one. */
@Composable
private fun MiniMap(latitude: Double, longitude: Double, onClick: () -> Unit) {
    val context = LocalContext.current
    val position = tilePosition(latitude, longitude, MAP_ZOOM)
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(MAP_HEIGHT)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clipToBounds()
            .clickable(onClick = onClick),
    ) {
        val centerX = maxWidth / 2
        val centerY = maxHeight / 2
        val tilesAcross = (maxWidth / TILE_SIZE).toInt() / 2 + 2
        val tilesDown = (maxHeight / TILE_SIZE).toInt() / 2 + 2
        val baseX = tileIndex(position.x)
        val baseY = tileIndex(position.y)
        for (dx in -tilesAcross..tilesAcross) {
            for (dy in -tilesDown..tilesDown) {
                val x = baseX + dx
                val y = baseY + dy
                val left = centerX + TILE_SIZE * (x - position.x).toFloat()
                val top = centerY + TILE_SIZE * (y - position.y).toFloat()
                if (left > maxWidth || top > maxHeight || left + TILE_SIZE < 0.dp || top + TILE_SIZE < 0.dp) continue
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(osmTileUrl(MAP_ZOOM, x, y))
                        .addHeader("User-Agent", TILE_USER_AGENT)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.offset(x = left, y = top).size(TILE_SIZE),
                )
            }
        }
        Icon(
            Icons.Filled.Place,
            contentDescription = null,
            tint = Color(0xFFE0473D),
            // The pin's tip (bottom centre of the icon) sits on the spot, not the icon's centre.
            modifier = Modifier.size(36.dp).offset(x = centerX - 18.dp, y = centerY - 34.dp),
        )
        Text(
            "© OpenStreetMap-Mitwirkende",
            fontSize = 9.sp,
            color = Color.Black.copy(alpha = 0.75f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .background(Color.White.copy(alpha = 0.7f))
                .padding(horizontal = 4.dp, vertical = 1.dp),
        )
    }
}

/** Hands the spot to whatever map app is installed (Google Maps, OsmAnd, …) via a geo: link with
 *  the building as label — or openstreetmap.org in the browser when there's none. */
private fun openInMaps(context: Context, location: RoomLocation) {
    val label = Uri.encode("${location.buildingName}, Hochschule Esslingen")
    val geo = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${location.latitude},${location.longitude}($label)"))
    try {
        context.startActivity(geo)
    } catch (_: ActivityNotFoundException) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(osmWebUrl(location.latitude, location.longitude))))
    }
}
