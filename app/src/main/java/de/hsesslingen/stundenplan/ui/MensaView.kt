package de.hsesslingen.stundenplan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NoMeals
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import de.hsesslingen.stundenplan.data.MensaLabel
import de.hsesslingen.stundenplan.data.MensaLocation
import de.hsesslingen.stundenplan.data.MensaLocations
import de.hsesslingen.stundenplan.data.MensaMeal
import de.hsesslingen.stundenplan.data.Weekday
import de.hsesslingen.stundenplan.ui.theme.LocalIsDarkTheme
import de.hsesslingen.stundenplan.ui.theme.OneUiAlertDialog
import de.hsesslingen.stundenplan.ui.theme.PillShape
import java.time.LocalDate

/**
 * The Mensa tab: the Studierendenwerk's Speiseplan for the chosen Mensa, one swipeable page per
 * weekday — same day pager and weekday chips as the Tag view, sharing its selected date, so
 * switching between "Tag" and "Mensa" stays on the same day.
 */
@Composable
internal fun MensaView(
    viewModel: StundenplanViewModel,
    locationId: Int?,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
) {
    when (locationId) {
        // Still reading the saved choice — an empty frame for a few ms beats flashing the picker.
        MENSA_LOCATION_LOADING -> Box(Modifier.fillMaxSize())
        null -> MensaLocationPicker(onSelect = viewModel::setMensaLocation)
        else -> MensaDaysPager(viewModel, locationId, selectedDate, onDateSelected)
    }
}

@Composable
private fun MensaDaysPager(
    viewModel: StundenplanViewModel,
    locationId: Int,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = dateToDayPage(selectedDate)) { DAY_PAGE_COUNT }
    // Same swipe <-> selectedDate syncing as DayView (see the comments there, also on why this
    // follows settledPage rather than currentPage).
    val currentSelectedDate by rememberUpdatedState(selectedDate)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            val date = dayPageToDate(page)
            if (date != currentSelectedDate) onDateSelected(date)
        }
    }
    LaunchedEffect(selectedDate) {
        val targetPage = dateToDayPage(selectedDate)
        if (pagerState.currentPage != targetPage) pagerState.animateScrollToPage(targetPage)
    }

    val days by viewModel.mensaDays.collectAsState()
    var selectedMeal by remember { mutableStateOf<MensaMeal?>(null) }
    val monday = selectedDate.weekMonday()

    Column(Modifier.fillMaxSize()) {
        Text(
            "Woche vom ${monday.format(SHORT_DATE)} – ${monday.plusDays(4).format(FULL_DATE)}",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            Weekday.entries.forEach { day ->
                val date = monday.plusDays(day.ordinal.toLong())
                DateChip(
                    day = day,
                    date = date,
                    selected = date == selectedDate,
                    isToday = date == LocalDate.now(),
                    modifier = Modifier.weight(1f),
                    onClick = { onDateSelected(date) },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 1) { page ->
            val date = dayPageToDate(page)
            LaunchedEffect(locationId, date) { viewModel.loadMensaDay(locationId, date) }
            val state = days[MensaKey(locationId, date)]
            val day = state?.day
            when {
                day == null && state?.error != null ->
                    ErrorState(state.error, onRetry = { viewModel.loadMensaDay(locationId, date, force = true) })
                day == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingGlyph("Lade Speiseplan …")
                }
                day.meals.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    EmptyScheduleBanner(
                        text = day.closedMessage ?: "Kein Speiseplan für diesen Tag.",
                        icon = Icons.Filled.NoMeals,
                        modifier = Modifier.padding(top = 16.dp, start = 20.dp, end = 20.dp),
                    )
                }
                else -> MealList(meals = day.meals, onMealClick = { selectedMeal = it })
            }
        }
    }

    selectedMeal?.let { meal ->
        MealDetailDialog(meal = meal, onDismiss = { selectedMeal = null })
    }
}

@Composable
private fun MealList(meals: List<MensaMeal>, onMealClick: (MensaMeal) -> Unit) {
    // One column of horizontal cards on a phone; wide screens (tablets, landscape) fit more side
    // by side. The cards have a fixed height, so a row of them never ends up ragged.
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 340.dp),
        modifier = Modifier.fillMaxSize(),
        // Bottom headroom so the last card can scroll clear of the floating nav bar.
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        meals.groupBy { it.category }.forEach { (category, categoryMeals) ->
            item(span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
                Text(
                    category,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                )
            }
            items(categoryMeals, contentType = { "meal" }) { meal ->
                MealCard(meal = meal, onClick = { onMealClick(meal) })
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }, contentType = "footer") {
            Text(
                "Preise für Studierende — Bedienstete und Gäste in den Details. " +
                    "Angaben ohne Gewähr, Quelle: Studierendenwerk Stuttgart.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp),
            )
        }
    }
}

private val MEAL_CARD_HEIGHT = 112.dp

@Composable
private fun MealCard(meal: MensaMeal, onClick: () -> Unit) {
    // "Veganer Renner" is both a category and a label on every meal in it — repeating it on the
    // card right under that very header would just be noise (the details still show it).
    val labels = meal.labels.filterNot { it.title.equals(meal.category, ignoreCase = true) }
    Row(
        Modifier
            .fillMaxWidth()
            .height(MEAL_CARD_HEIGHT)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick),
    ) {
        MealPhoto(url = meal.imageUrl, modifier = Modifier.width(120.dp).fillMaxHeight())
        Column(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            meal.subcategory?.let { sub ->
                Text(
                    sub,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                )
            }
            Text(
                meal.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = if (meal.subcategory != null) 2 else 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                meal.prices.firstOrNull()?.let { price ->
                    Text(
                        "$price €",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.weight(1f))
                labels.take(2).forEach { LabelPill(it) }
            }
        }
    }
}

/** The dish photo, cropped to fill its box. Sits on a placeholder (a fork-and-knife glyph) that
 *  shows while the photo loads — and stays for the dishes the site has no photo of. */
@Composable
private fun MealPhoto(url: String?, modifier: Modifier = Modifier) {
    Box(modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest), contentAlignment = Alignment.Center) {
        Icon(
            Icons.Outlined.Restaurant,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(28.dp),
        )
        if (url != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(url).crossfade(true).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

// The dark-theme green is lighter: the light one is too dim to read on a dark card.
private val VeggieLabelColorLight = Color(0xFF2E9E5B)
private val VeggieLabelColorDark = Color(0xFF78E9AB)

@Composable
private fun LabelPill(label: MensaLabel) {
    val color = when {
        !label.isVeggie -> MaterialTheme.colorScheme.onSurfaceVariant
        LocalIsDarkTheme.current -> VeggieLabelColorDark
        else -> VeggieLabelColorLight
    }
    Text(
        label.title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(PillShape)
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MealDetailDialog(meal: MensaMeal, onDismiss: () -> Unit) {
    OneUiAlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Schließen", fontWeight = FontWeight.Bold) } },
        title = { Text(meal.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (meal.imageUrl != null) {
                    MealPhoto(
                        url = meal.imageUrl,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(MaterialTheme.shapes.medium),
                    )
                    Spacer(Modifier.height(12.dp))
                }
                Text(
                    listOfNotNull(meal.category.ifBlank { null }, meal.subcategory).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (meal.labels.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        meal.labels.forEach { LabelPill(it) }
                    }
                }
                Spacer(Modifier.height(4.dp))
                PriceRows(meal.prices)
                DetailRow("Allergene", meal.allergens.joinToString(", "))
                DetailRow("Zusatzstoffe", meal.additives.joinToString(", "))
                DetailRow("Weitere Kennzeichnungen", meal.unknownCodes.joinToString(", "))
                if (meal.nutrition.isNotEmpty()) {
                    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Text(
                            "Nährwerte" + (meal.nutritionBasis?.let { " pro $it" } ?: ""),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                        meal.nutrition.forEach { value ->
                            Row(Modifier.fillMaxWidth().padding(start = if (value.isSubItem) 12.dp else 0.dp, top = 2.dp)) {
                                Text(value.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                Text(value.value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        },
    )
}

private val PRICE_GROUPS = listOf("Studierende", "Bedienstete", "Gäste")

@Composable
private fun PriceRows(prices: List<String>) {
    if (prices.isEmpty()) return
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text("Preise", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        prices.forEachIndexed { index, price ->
            // The site always lists Studierende / Bedienstete / Gäste; anything else gets no
            // made-up group name.
            val group = if (prices.size == PRICE_GROUPS.size) PRICE_GROUPS[index] else "Preis ${index + 1}"
            Row(Modifier.fillMaxWidth().padding(top = 2.dp)) {
                Text(group, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(
                    "$price €",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

/** First visit to the Mensa tab: which Mensa? There's no telling from the Studiengang which
 *  campus someone eats at, so this asks once — switching later goes through the header title. */
@Composable
private fun MensaLocationPicker(onSelect: (Int) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            IconBadge(Icons.Filled.Restaurant)
        }
        Text(
            "Welche Mensa?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Text(
            "Der Speiseplan kommt live vom Studierendenwerk Stuttgart. Wechseln kannst du jederzeit oben über den Namen der Mensa.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LocationGroup("Hochschule Esslingen", MensaLocations.all.filter { it.atHsEsslingen }, onSelect)
        LocationGroup("Weitere Mensen", MensaLocations.all.filterNot { it.atHsEsslingen }, onSelect)
        Spacer(Modifier.height(120.dp))
    }
}

@Composable
private fun LocationGroup(title: String, locations: List<MensaLocation>, onSelect: (Int) -> Unit) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 12.dp),
    )
    locations.forEach { location ->
        Row(
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable { onSelect(location.id) }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Outlined.Restaurant, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(location.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
    }
}
