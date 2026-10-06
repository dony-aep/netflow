@file:OptIn(ExperimentalMaterial3Api::class)

package com.donyaep.netflow.ui.screens.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.graphics.shapes.Morph
import com.donyaep.netflow.R
import com.donyaep.netflow.ui.components.NetworkSplit
import com.donyaep.netflow.ui.components.PulseRow
import com.donyaep.netflow.ui.components.pulseShape
import com.donyaep.netflow.ui.components.rememberCookie12Morph
import com.donyaep.netflow.ui.components.rememberCookie9Morph
import com.donyaep.netflow.ui.theme.AppCodeFontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

// ── Shapes ──────────────────────────────────────────────────────────────────
private val topShape    = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
private val midShape    = RoundedCornerShape(4.dp)
private val bottomShape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 28.dp, bottomEnd = 28.dp)
private val singleShape = RoundedCornerShape(28.dp)

private fun segmentShapes(size: Int): List<RoundedCornerShape> {
    if (size == 1) return listOf(singleShape)
    return List(size) { i ->
        when (i) {
            0        -> topShape
            size - 1 -> bottomShape
            else     -> midShape
        }
    }
}

// ── Screen ───────────────────────────────────────────────────────────────────
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit,
    viewModel: HistoryViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cs = MaterialTheme.colorScheme
    var showFilterSheet by remember { mutableStateOf(false) }
    var detailState by remember { mutableStateOf<Pair<Int, HistoryCalendarDay?>?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Historial",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(ImageVector.vectorResource(R.drawable.ic_filter_list), contentDescription = "Filtrar", tint = cs.primary)
                    }
                },
                windowInsets = WindowInsets(0),
            )
        },
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 8.dp,
                start = 16.dp,
                end = 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Resumen ──────────────────────────────────────────────────
            item { HistoryHero(summary = uiState.summary) }

            if (uiState.selectedFilter == HistoryFilter.ByMonth) {
                // ── Navegador de mes ─────────────────────────────────────
                item {
                    MonthNavigator(
                        monthLabel = uiState.monthLabel,
                        canGoNext  = uiState.canGoToNextMonth,
                        onPrevious = viewModel::previousMonth,
                        onNext     = viewModel::nextMonth,
                    )
                }
                // ── Calendario ───────────────────────────────────────────
                item {
                    MonthCalendar(
                        daysInMonth       = uiState.daysInMonth,
                        firstWeekday      = uiState.firstWeekdayOfMonth,
                        calendarDays      = uiState.calendarDays,
                        selectedYearMonth = uiState.selectedYearMonth,
                        onDayClick        = { day, data -> detailState = day to data },
                        onPrevious        = viewModel::previousMonth,
                        onNext            = viewModel::nextMonth,
                    )
                }
                uiState.summary.peakDayLabel?.let { peakDay ->
                    item {
                        Text(
                            text = buildAnnotatedString {
                                append("$peakDay fue tu día más alto, con ")
                                withStyle(
                                    SpanStyle(
                                        color = cs.primary,
                                        fontFamily = AppCodeFontFamily,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                ) { append(uiState.summary.peakValueLabel) }
                                append(".")
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = cs.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                    }
                }
            } else {
                // ── Lista de días ─────────────────────────────────────────
                if (uiState.items.isEmpty()) {
                    item { HistoryEmptyState() }
                } else {
                    item { HistoryDayList(items = uiState.items) }
                }
            }
        }
    }

    // ── Filter Sheet ──────────────────────────────────────────────────────
    if (showFilterSheet) {
        HistoryFilterSheet(
            current  = uiState.selectedFilter,
            onSelect = { viewModel.selectFilter(it); showFilterSheet = false },
            onDismiss = { showFilterSheet = false },
        )
    }

    // ── Day Detail Sheet ──────────────────────────────────────────────────
    detailState?.let { (day, data) ->
        DayDetailSheet(
            dayOfMonth        = day,
            data              = data,
            selectedYearMonth = uiState.selectedYearMonth,
            onDismiss         = { detailState = null },
        )
    }
}

// ── Hero ──────────────────────────────────────────────────────────────────────
@Composable
private fun HistoryHero(summary: HistorySummaryUiState) {
    val cs = MaterialTheme.colorScheme
    val (value, unit) = summary.totalLabel.splitValueAndUnit()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Box(
            modifier = Modifier
                .size(188.dp)
                .pulseShape(morph = rememberCookie9Morph(), level = { 1f }, color = { cs.primaryContainer }),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = summary.heroLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = cs.onPrimaryContainer,
                    maxLines = 1,
                )
                ShapeFigure(value = value, color = cs.onPrimaryContainer)
                Text(
                    text = unit,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onPrimaryContainer,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LabeledFigure(label = "WiFi", value = summary.wifiLabel, color = cs.secondary)
            LabeledFigure(label = "Móvil", value = summary.mobileLabel, color = cs.tertiary)
            summary.averageLabel?.let { average ->
                Text(average, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
            }
        }
    }
}

// Cifra dentro de la forma: los valores largos bajan de cuerpo para caber.
@Composable
private fun ShapeFigure(value: String, color: Color) {
    val fontSize = when {
        value.length <= 4 -> 54.sp
        value.length == 5 -> 44.sp
        else -> 36.sp
    }
    Text(
        text = value,
        style = MaterialTheme.typography.displayMedium.copy(
            fontFamily = AppCodeFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = fontSize,
            lineHeight = fontSize * 1.12f,
            letterSpacing = (-2).sp,
        ),
        color = color,
        maxLines = 1,
    )
}

@Composable
private fun LabeledFigure(label: String, value: String, color: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontFamily = AppCodeFontFamily),
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

// "17.8 GB" → ("17.8", "GB")
private fun String.splitValueAndUnit(): Pair<String, String> =
    substringBeforeLast(' ') to substringAfterLast(' ', "")

// ── Month Navigator ───────────────────────────────────────────────────────────
@Composable
private fun MonthNavigator(monthLabel: String, canGoNext: Boolean, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Mes anterior")
        }
        Text(text = monthLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        IconButton(onClick = onNext, enabled = canGoNext) {
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Mes siguiente")
        }
    }
}

// ── Month Calendar ────────────────────────────────────────────────────────────
@Composable
private fun MonthCalendar(
    daysInMonth: Int,
    firstWeekday: Int,
    calendarDays: Map<Int, HistoryCalendarDay>,
    selectedYearMonth: YearMonth,
    onDayClick: (Int, HistoryCalendarDay?) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val today = remember { LocalDate.now() }
    val isCurrentMonth = selectedYearMonth == YearMonth.now()
    val cs = MaterialTheme.colorScheme
    val dayMorph = rememberCookie12Morph()

    val leadingEmpties = firstWeekday - 1
    val total = leadingEmpties + daysInMonth
    val trailingEmpties = (7 - total % 7) % 7
    val cells: List<Int?> = List(leadingEmpties) { null } +
        (1..daysInMonth).toList() +
        List(trailingEmpties) { null }

    Column(
        modifier = Modifier.pointerInput(selectedYearMonth) {
            var totalDrag = 0f
            detectHorizontalDragGestures(
                onDragEnd   = {
                    if (totalDrag > 60f) onPrevious()
                    else if (totalDrag < -60f) onNext()
                    totalDrag = 0f
                },
                onDragCancel    = { totalDrag = 0f },
                onHorizontalDrag = { _, delta -> totalDrag += delta },
            )
        },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Header
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("L", "M", "X", "J", "V", "S", "D").forEach { d ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = d,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = cs.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(2.dp))
        // Weeks
        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { day ->
                    Box(modifier = Modifier.weight(1f)) {
                        if (day != null) {
                            val isToday  = isCurrentMonth && day == today.dayOfMonth
                            val isFuture = isCurrentMonth && day > today.dayOfMonth ||
                                selectedYearMonth.isAfter(YearMonth.now())
                            DayCell(
                                day      = day,
                                data     = calendarDays[day],
                                morph    = dayMorph,
                                isToday  = isToday,
                                isFuture = isFuture,
                                onClick  = { if (!isFuture) onDayClick(day, calendarDays[day]) },
                            )
                        } else {
                            Spacer(modifier = Modifier.fillMaxWidth().aspectRatio(1f))
                        }
                    }
                }
            }
        }
    }
}

// ── Day Cell ──────────────────────────────────────────────────────────────────
// Cada día es una forma: más festoneada y más intensa cuanto más se consumió
// frente al día más alto del mes.
@Composable
private fun DayCell(
    day: Int,
    data: HistoryCalendarDay?,
    morph: Morph,
    isToday: Boolean,
    isFuture: Boolean,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val hasData = data != null && data.hasData && !isFuture
    val level = if (hasData) data.level else 0f
    val container = when {
        !hasData -> Color.Transparent
        isToday  -> cs.primary
        else     -> lerp(cs.surfaceContainerHigh, cs.primaryContainer, level)
    }
    val content = when {
        isFuture -> cs.onSurface.copy(alpha = 0.35f)
        !hasData -> if (isToday) cs.primary else cs.onSurfaceVariant
        isToday  -> cs.onPrimary
        else     -> cs.onSurface
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(CircleShape)
            .clickable(enabled = !isFuture, onClick = onClick)
            .pulseShape(morph = morph, level = { level }, color = { container }),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$day",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (hasData || isToday) FontWeight.Bold else FontWeight.Medium,
                color = content,
            )
            if (hasData) {
                Text(
                    text = data.totalCompactLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        fontFamily = AppCodeFontFamily,
                    ),
                    color = content,
                    maxLines = 1,
                )
            }
        }
    }
}

// ── Day List (period view) ────────────────────────────────────────────────────
@Composable
private fun HistoryDayList(items: List<HistoryItemUiState>) {
    val cs = MaterialTheme.colorScheme
    val shapes = segmentShapes(items.size)
    Column {
        items.forEachIndexed { index, item ->
            if (index > 0) Spacer(Modifier.height(2.dp))
            Surface(color = cs.surfaceContainerHigh, shape = shapes[index], modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    supportingContent = { Text("WiFi: ${item.wifiLabel}  ·  Móvil: ${item.mobileLabel}") },
                    trailingContent   = {
                        Text(
                            item.totalLabel,
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = AppCodeFontFamily),
                            fontWeight = FontWeight.Bold,
                            color = cs.primary,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                ) {
                    Text(item.date, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ── Empty State ────────────────────────────────────────────────────────────────
@Composable
private fun HistoryEmptyState() {
    val cs = MaterialTheme.colorScheme
    Surface(color = cs.surfaceContainerHigh, shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(ImageVector.vectorResource(R.drawable.ic_bar_chart), contentDescription = null, tint = cs.onSurfaceVariant, modifier = Modifier.size(32.dp))
            Text("Sin datos para este rango", style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant)
        }
    }
}

// ── Filter Sheet ──────────────────────────────────────────────────────────────
@Composable
private fun HistoryFilterSheet(
    current: HistoryFilter,
    onSelect: (HistoryFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Período", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                TextButton(
                    onClick = { onSelect(HistoryFilter.ByMonth) },
                    enabled = current != HistoryFilter.ByMonth,
                ) {
                    Icon(
                        ImageVector.vectorResource(R.drawable.ic_restore),
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp).size(18.dp),
                    )
                    Text("Restablecer")
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Selecciona el rango de tiempo",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))

            val options = listOf(
                Triple(HistoryFilter.Last24Hours, ImageVector.vectorResource(R.drawable.ic_schedule),         "Hoy"),
                Triple(HistoryFilter.Last7Days,   Icons.Rounded.DateRange,        "Últimos 7 días"),
                Triple(HistoryFilter.Last30Days,  Icons.Rounded.DateRange,        "Últimos 30 días"),
                Triple(HistoryFilter.Last90Days,  ImageVector.vectorResource(R.drawable.ic_bar_chart),         "Últimos 3 meses"),
                Triple(HistoryFilter.ByMonth,     ImageVector.vectorResource(R.drawable.ic_calendar_view_month),"Por mes"),
            )
            val shapes = segmentShapes(options.size)

            options.forEachIndexed { index, (filter, icon, label) ->
                if (index > 0) Spacer(Modifier.height(2.dp))
                val selected = current == filter
                PulseRow(
                    onClick = { onSelect(filter) },
                    shape = shapes[index],
                    icon = icon,
                    title = label,
                    selected = selected,
                    container = cs.surfaceContainerHigh,
                    trailing = { if (selected) Icon(Icons.Rounded.Check, contentDescription = null) },
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Day Detail Sheet ──────────────────────────────────────────────────────────
@Composable
private fun DayDetailSheet(
    dayOfMonth: Int,
    data: HistoryCalendarDay?,
    selectedYearMonth: YearMonth,
    onDismiss: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val locale = Locale.forLanguageTag("es-ES")
    val dateLabel = remember(dayOfMonth, selectedYearMonth) {
        runCatching {
            val ld = LocalDate.of(selectedYearMonth.year, selectedYearMonth.month, dayOfMonth)
            val text = ld.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", locale))
            text.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
        }.getOrElse { "$dayOfMonth" }
    }
    val isToday = remember(dayOfMonth, selectedYearMonth) {
        val today = LocalDate.now()
        selectedYearMonth == YearMonth.now() && dayOfMonth == today.dayOfMonth
    }
    val hasData = data != null && data.hasData

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).navigationBarsPadding(),
        ) {
            Text(dateLabel, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            val note = when {
                !hasData    -> "No hay datos de este día."
                isToday     -> "Hoy, hasta ahora."
                data.isPeak -> "Tu día más alto del mes."
                else        -> null
            }
            note?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (hasData) {
                val (value, unit) = data.totalLabel.splitValueAndUnit()
                Spacer(Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(176.dp)
                            .pulseShape(
                                morph = rememberCookie12Morph(),
                                level = { data.level },
                                color = { cs.primaryContainer },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            ShapeFigure(value = value, color = cs.onPrimaryContainer)
                            Text(
                                text = unit,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = cs.onPrimaryContainer,
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        LabeledFigure(label = "Bajaste", value = data.receivedLabel, color = cs.primary)
                        LabeledFigure(label = "Subiste", value = data.sentLabel, color = cs.tertiary)
                    }
                }
                Spacer(Modifier.height(22.dp))
                NetworkSplit(
                    wifiShare = data.wifiShare,
                    wifiLabel = data.wifiTotalLabel,
                    mobileLabel = data.mobileTotalLabel,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
