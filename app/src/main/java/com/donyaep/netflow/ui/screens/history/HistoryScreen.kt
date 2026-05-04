@file:OptIn(ExperimentalMaterial3Api::class)

package com.donyaep.netflow.ui.screens.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarViewMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.NetworkCell
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.donyaep.netflow.ui.theme.AppCodeFontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (uiState.selectedFilter == HistoryFilter.ByMonth)
                            uiState.monthLabel else "Historial",
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
                        Icon(Icons.Rounded.FilterList, contentDescription = "Filtrar", tint = cs.primary)
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
            item { HistorySummaryCard(uiState = uiState) }

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

// ── Summary Card ──────────────────────────────────────────────────────────────
@Composable
private fun HistorySummaryCard(uiState: HistoryUiState) {
    val cs = MaterialTheme.colorScheme
    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp)) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = uiState.summary.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurfaceVariant,
                )
                Text(
                    text = uiState.summary.totalLabel,
                    style = MaterialTheme.typography.headlineLarge.copy(fontFamily = AppCodeFontFamily),
                    fontWeight = FontWeight.Bold,
                    color = cs.primary,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SummaryNetRow(Icons.Rounded.Wifi,        "WiFi",  uiState.summary.wifiLabel,   cs.secondary)
                SummaryNetRow(Icons.Rounded.NetworkCell, "Móvil", uiState.summary.mobileLabel, cs.tertiary)
            }
        }
    }
}

@Composable
private fun SummaryNetRow(icon: ImageVector, label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(15.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.75f))
            Text(value, style = MaterialTheme.typography.titleSmall.copy(fontFamily = AppCodeFontFamily), fontWeight = FontWeight.Bold, color = color)
        }
    }
}

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
                                isToday  = isToday,
                                isFuture = isFuture,
                                onClick  = { if (!isFuture) onDayClick(day, calendarDays[day]) },
                            )
                        } else {
                            Spacer(modifier = Modifier.fillMaxWidth().height(72.dp))
                        }
                    }
                }
            }
        }
    }
}

// ── Day Cell ──────────────────────────────────────────────────────────────────
@Composable
private fun DayCell(
    day: Int,
    data: HistoryCalendarDay?,
    isToday: Boolean,
    isFuture: Boolean,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val containerColor = when {
        isToday  -> cs.primaryContainer
        isFuture -> cs.surfaceContainerHigh.copy(alpha = 0.35f)
        else     -> cs.surfaceContainerHigh
    }
    val hasData = data != null && data.hasData

    Surface(
        onClick = onClick,
        enabled = !isFuture,
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        modifier = Modifier.fillMaxWidth().height(72.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            Text(
                text = "$day",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                color = when {
                    isFuture -> cs.onSurface.copy(alpha = 0.35f)
                    isToday  -> cs.onPrimaryContainer
                    else     -> cs.onSurface
                },
            )
            if (hasData && !isFuture) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = data!!.wifiCompactLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontFamily = AppCodeFontFamily),
                    color = cs.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = data.mobileCompactLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontFamily = AppCodeFontFamily),
                    color = cs.tertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    textAlign = TextAlign.Center,
                )
            } else if (!isFuture) {
                Spacer(Modifier.height(6.dp))
                Text(text = "·", style = MaterialTheme.typography.labelSmall, color = cs.onSurface.copy(alpha = 0.25f))
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
                    headlineContent   = { Text(item.date, fontWeight = FontWeight.SemiBold) },
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
                )
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
            Icon(Icons.Rounded.BarChart, contentDescription = null, tint = cs.onSurfaceVariant, modifier = Modifier.size(32.dp))
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
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
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
                        Icons.Rounded.Restore,
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
                Triple(HistoryFilter.Last24Hours, Icons.Rounded.Schedule,         "Hoy"),
                Triple(HistoryFilter.Last7Days,   Icons.Rounded.DateRange,        "Últimos 7 días"),
                Triple(HistoryFilter.Last30Days,  Icons.Rounded.DateRange,        "Últimos 30 días"),
                Triple(HistoryFilter.Last90Days,  Icons.Rounded.BarChart,         "Últimos 3 meses"),
                Triple(HistoryFilter.ByMonth,     Icons.Rounded.CalendarViewMonth,"Por mes"),
            )
            val shapes = segmentShapes(options.size)

            options.forEachIndexed { index, (filter, icon, label) ->
                if (index > 0) Spacer(Modifier.height(2.dp))
                val selected = current == filter
                Surface(
                    onClick = { onSelect(filter) },
                    shape   = shapes[index],
                    color   = if (selected) cs.secondaryContainer else cs.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ListItem(
                        headlineContent = {
                            Text(label, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                        },
                        leadingContent = {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (selected) cs.onSecondaryContainer else cs.onSurfaceVariant,
                            )
                        },
                        trailingContent = {
                            if (selected) Icon(Icons.Rounded.Check, contentDescription = null, tint = cs.onSecondaryContainer)
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
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

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Fecha ─────────────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (isToday) {
                    Surface(shape = RoundedCornerShape(4.dp), color = cs.primary) {
                        Text(
                            "HOY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = cs.onPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
                Text(dateLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            if (!hasData) {
                // ── Sin datos ─────────────────────────────────────────────
                Surface(shape = RoundedCornerShape(16.dp), color = cs.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Rounded.BarChart, contentDescription = null, tint = cs.onSurfaceVariant, modifier = Modifier.size(28.dp))
                        Text("Sin datos registrados", style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant)
                    }
                }
            } else {
                val d = data!!
                // ── Total ─────────────────────────────────────────────────
                Surface(shape = RoundedCornerShape(16.dp), color = cs.primaryContainer, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text("Total: ", style = MaterialTheme.typography.titleSmall, color = cs.onPrimaryContainer.copy(alpha = 0.8f))
                        Text(d.totalLabel, style = MaterialTheme.typography.headlineSmall.copy(fontFamily = AppCodeFontFamily), fontWeight = FontWeight.Bold, color = cs.onPrimaryContainer)
                    }
                }
                // ── WiFi ──────────────────────────────────────────────────
                DetailSection(
                    icon = Icons.Rounded.Wifi, label = "WiFi",
                    total = d.wifiTotalLabel, received = d.wifiReceivedLabel, sent = d.wifiSentLabel,
                    color = cs.secondary,
                )
                // ── Móvil ─────────────────────────────────────────────────
                DetailSection(
                    icon = Icons.Rounded.NetworkCell, label = "Datos móviles",
                    total = d.mobileTotalLabel, received = d.mobileReceivedLabel, sent = d.mobileSentLabel,
                    color = cs.tertiary,
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Detail Section ────────────────────────────────────────────────────────────
@Composable
private fun DetailSection(icon: ImageVector, label: String, total: String, received: String, sent: String, color: Color) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.10f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Cabecera: icono + etiqueta + total
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(shape = RoundedCornerShape(12.dp), color = color.copy(alpha = 0.18f)) {
                        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.padding(10.dp))
                    }
                    Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = color)
                }
                Text(total, style = MaterialTheme.typography.titleLarge.copy(fontFamily = AppCodeFontFamily), fontWeight = FontWeight.Bold, color = color)
            }
            // Tarjetas Recibido / Enviado
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = cs.surfaceContainerHigh,
                    modifier = Modifier.weight(1f),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Rounded.ArrowDownward, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
                            Text("Recibido", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                        }
                        Text(received, style = MaterialTheme.typography.titleSmall.copy(fontFamily = AppCodeFontFamily), fontWeight = FontWeight.Bold)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = cs.surfaceContainerHigh,
                    modifier = Modifier.weight(1f),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Rounded.ArrowUpward, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
                            Text("Enviado", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                        }
                        Text(sent, style = MaterialTheme.typography.titleSmall.copy(fontFamily = AppCodeFontFamily), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}