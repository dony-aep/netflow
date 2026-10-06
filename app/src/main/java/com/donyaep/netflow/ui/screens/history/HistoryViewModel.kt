package com.donyaep.netflow.ui.screens.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.donyaep.netflow.NetFlowApplication
import com.donyaep.netflow.core.monitoring.TrafficFormatter
import com.donyaep.netflow.data.model.DailyUsage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class HistoryFilter(val label: String) {
    Last24Hours("Hoy"),
    Last7Days("7 días"),
    Last30Days("30 días"),
    Last90Days("3 meses"),
    ByMonth("Por mes"),
}

data class HistoryItemUiState(
    val date: String,
    val totalLabel: String,
    val wifiLabel: String,
    val mobileLabel: String,
)

data class HistorySummaryUiState(
    // Frase que precede al total: «En octubre llevas», «Hoy llevas»…
    val heroLabel: String,
    val totalLabel: String,
    val wifiLabel: String,
    val mobileLabel: String,
    // Media diaria; solo con al menos dos días registrados
    val averageLabel: String? = null,
    // Día de mayor consumo del mes («El jueves 1») y su total
    val peakDayLabel: String? = null,
    val peakValueLabel: String = "",
)

data class HistoryComparisonUiState(
    val label: String,
    val valueLabel: String,
    val fraction: Float,
)

data class HistoryCalendarDay(
    val dayOfMonth: Int,
    val hasData: Boolean,
    val totalLabel: String,
    val totalCompactLabel: String,
    val wifiTotalLabel: String,
    val mobileTotalLabel: String,
    val receivedLabel: String,
    val sentLabel: String,
    // Parte de WiFi sobre el total del día, de 0 a 1
    val wifiShare: Float,
    // Consumo del día frente al mayor del mes, de 0 a 1
    val level: Float,
    val isPeak: Boolean,
)

data class HistoryUiState(
    val selectedFilter: HistoryFilter = HistoryFilter.ByMonth,
    val monthLabel: String = "",
    val canGoToNextMonth: Boolean = false,
    val selectedYearMonth: YearMonth = YearMonth.now(),
    val daysInMonth: Int = 31,
    val firstWeekdayOfMonth: Int = 1,
    val summary: HistorySummaryUiState = HistorySummaryUiState(
        heroLabel = "Sin datos",
        totalLabel = TrafficFormatter.formatBytes(0),
        wifiLabel = TrafficFormatter.formatBytes(0),
        mobileLabel = TrafficFormatter.formatBytes(0),
    ),
    val comparisons: List<HistoryComparisonUiState> = emptyList(),
    val items: List<HistoryItemUiState> = emptyList(),
    val calendarDays: Map<Int, HistoryCalendarDay> = emptyMap(),
)

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as NetFlowApplication).appContainer.dailyUsageRepository
    private var allEntries: List<DailyUsage> = emptyList()
    private var selectedFilter: HistoryFilter = HistoryFilter.ByMonth
    private var selectedMonth: YearMonth = YearMonth.now()

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        // El primer estado sale de lo que el repositorio ya tiene en memoria, para que la
        // pantalla no se dibuje vacía mientras entra deslizando.
        repository.recentUsage.value?.let { entries ->
            allEntries = entries
            refreshUiState()
        }
        viewModelScope.launch {
            repository.recentUsage.filterNotNull().collect { entries ->
                allEntries = entries
                refreshUiState()
            }
        }
    }

    fun selectFilter(filter: HistoryFilter) {
        selectedFilter = filter
        refreshUiState()
    }

    fun previousMonth() {
        selectedMonth = selectedMonth.minusMonths(1)
        refreshUiState()
    }

    fun nextMonth() {
        val nextMonth = selectedMonth.plusMonths(1)
        if (!nextMonth.isAfter(YearMonth.now())) {
            selectedMonth = nextMonth
            refreshUiState()
        }
    }

    private fun refreshUiState() {
        val filteredEntries = when (selectedFilter) {
            HistoryFilter.Last24Hours -> filterSince(LocalDate.now())
            HistoryFilter.Last7Days   -> filterSince(LocalDate.now().minusDays(6))
            HistoryFilter.Last30Days  -> filterSince(LocalDate.now().minusDays(29))
            HistoryFilter.Last90Days  -> filterSince(LocalDate.now().minusDays(89))
            HistoryFilter.ByMonth     -> allEntries.filter { entry ->
                YearMonth.from(LocalDate.parse(entry.date)) == selectedMonth
            }
        }.sortedByDescending { it.date }

        val locale = Locale.forLanguageTag("es-ES")
        val heroLabel = when (selectedFilter) {
            HistoryFilter.Last24Hours -> "Hoy llevas"
            HistoryFilter.Last7Days   -> "En 7 días llevas"
            HistoryFilter.Last30Days  -> "En 30 días llevas"
            HistoryFilter.Last90Days  -> "En 3 meses llevas"
            HistoryFilter.ByMonth     -> {
                val month = selectedMonth.format(DateTimeFormatter.ofPattern("MMMM", locale))
                if (selectedMonth == YearMonth.now()) "En $month llevas" else "En $month usaste"
            }
        }

        val maxTotal = filteredEntries.maxOfOrNull { entry -> entry.totalBytes }?.coerceAtLeast(1L) ?: 1L
        val totalBytes = filteredEntries.sumOf { entry -> entry.totalBytes }
        // Con un solo día no hay con qué comparar: ni media ni día más alto.
        val peakEntry = filteredEntries
            .takeIf { selectedFilter == HistoryFilter.ByMonth && it.size >= 2 }
            ?.maxByOrNull { entry -> entry.totalBytes }

        val calendarDays: Map<Int, HistoryCalendarDay> = if (selectedFilter == HistoryFilter.ByMonth) {
            filteredEntries.associate { entry ->
                val ld = LocalDate.parse(entry.date)
                ld.dayOfMonth to HistoryCalendarDay(
                    dayOfMonth        = ld.dayOfMonth,
                    hasData           = entry.totalBytes > 0,
                    totalLabel        = TrafficFormatter.formatBytes(entry.totalBytes),
                    totalCompactLabel = formatCompact(entry.totalBytes),
                    wifiTotalLabel    = TrafficFormatter.formatBytes(entry.wifiTotalBytes),
                    mobileTotalLabel  = TrafficFormatter.formatBytes(entry.mobileTotalBytes),
                    receivedLabel     = TrafficFormatter.formatBytes(entry.totalReceivedBytes),
                    sentLabel         = TrafficFormatter.formatBytes(entry.totalSentBytes),
                    wifiShare         = entry.wifiTotalBytes.toFloat() / entry.totalBytes.coerceAtLeast(1L),
                    level             = entry.totalBytes.toFloat() / maxTotal,
                    isPeak            = entry === peakEntry,
                )
            }
        } else emptyMap()

        _uiState.update {
            it.copy(
                selectedFilter       = selectedFilter,
                monthLabel           = formatMonth(selectedMonth),
                canGoToNextMonth     = selectedMonth.isBefore(YearMonth.now()),
                selectedYearMonth    = selectedMonth,
                daysInMonth          = selectedMonth.lengthOfMonth(),
                firstWeekdayOfMonth  = LocalDate.of(selectedMonth.year, selectedMonth.monthValue, 1).dayOfWeek.value,
                summary = HistorySummaryUiState(
                    heroLabel = heroLabel,
                    totalLabel = TrafficFormatter.formatBytes(totalBytes),
                    wifiLabel = TrafficFormatter.formatBytes(filteredEntries.sumOf { entry -> entry.wifiTotalBytes }),
                    mobileLabel = TrafficFormatter.formatBytes(filteredEntries.sumOf { entry -> entry.mobileTotalBytes }),
                    averageLabel = filteredEntries.takeIf { entries -> entries.size >= 2 }?.let { entries ->
                        "Unos ${TrafficFormatter.formatBytes(totalBytes / entries.size)} al día."
                    },
                    peakDayLabel = peakEntry?.let { entry ->
                        "El ${LocalDate.parse(entry.date).format(DateTimeFormatter.ofPattern("EEEE d", locale))}"
                    },
                    peakValueLabel = peakEntry?.let { entry -> TrafficFormatter.formatBytes(entry.totalBytes) }.orEmpty(),
                ),
                comparisons = filteredEntries.take(7).map { entry ->
                    HistoryComparisonUiState(
                        label = LocalDate.parse(entry.date).format(shortDayFormatter),
                        valueLabel = TrafficFormatter.formatBytes(entry.totalBytes),
                        fraction = (entry.totalBytes.toFloat() / maxTotal.toFloat()).coerceIn(0f, 1f),
                    )
                },
                items = filteredEntries.map { entry ->
                    HistoryItemUiState(
                        date = LocalDate.parse(entry.date).format(dayFormatter),
                        totalLabel = TrafficFormatter.formatBytes(entry.totalBytes),
                        wifiLabel  = TrafficFormatter.formatBytes(entry.wifiTotalBytes),
                        mobileLabel = TrafficFormatter.formatBytes(entry.mobileTotalBytes),
                    )
                },
                calendarDays = calendarDays,
            )
        }
    }

    private fun filterSince(cutoff: LocalDate): List<DailyUsage> = allEntries.filter { entry ->
        !LocalDate.parse(entry.date).isBefore(cutoff)
    }

    companion object {
        private val dayFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("es-ES"))
        private val shortDayFormatter = DateTimeFormatter.ofPattern("dd MMM", Locale.forLanguageTag("es-ES"))

        fun formatCompact(bytes: Long): String = when {
            bytes >= 1_073_741_824L -> "${ "%.1f".format(bytes / 1_073_741_824.0) }G"
            bytes >= 1_048_576L     -> "${bytes / 1_048_576}M"
            bytes >= 1_024L         -> "${bytes / 1_024}K"
            bytes > 0L              -> "${bytes}B"
            else                    -> "0"
        }

        private fun formatMonth(yearMonth: YearMonth): String {
            val locale = Locale.forLanguageTag("es-ES")
            val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", locale)
            val text = yearMonth.format(formatter)
            return text.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
        }
    }
}