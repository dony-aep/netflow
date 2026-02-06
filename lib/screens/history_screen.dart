import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../models/models.dart';
import '../services/services.dart';
import '../theme/app_theme.dart';

/// Tipos de filtro disponibles
enum FilterType {
  last24Hours,
  last7Days,
  last30Days,
  last3Months,
  byMonth,
}

/// Pantalla de historial de uso de datos
/// Diseño: Material Design 3 Expressive - Vista de tabla mensual
class HistoryScreen extends StatefulWidget {
  const HistoryScreen({super.key});

  @override
  State<HistoryScreen> createState() => _HistoryScreenState();
}

class _HistoryScreenState extends State<HistoryScreen> {
  Map<String, DailyUsage> _usageByDate = {};
  List<DailyUsage> _usageList = [];
  bool _isLoading = true;
  late DateTime _currentMonth;
  bool _showMonthSelector = false;
  FilterType _currentFilter = FilterType.byMonth;

  @override
  void initState() {
    super.initState();
    _currentMonth = DateTime(DateTime.now().year, DateTime.now().month);
    _loadHistory();
  }

  Future<void> _loadHistory() async {
    setState(() => _isLoading = true);

    try {
      // Cargar datos de los últimos 365 días para tener historial completo
      final history = await DatabaseService.getRecentDailyUsage(365);
      if (mounted) {
        setState(() {
          _usageList = history;
          _usageByDate = {
            for (var usage in history) _dateKey(usage.date): usage,
          };
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() => _isLoading = false);
      }
    }
  }

  String _dateKey(DateTime date) {
    return '${date.year}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}';
  }

  void _selectMonth(int month) {
    final now = DateTime.now();
    int year = _currentMonth.year;
    
    // Si el mes seleccionado es futuro en el año actual, usar el año anterior
    if (year == now.year && month > now.month) {
      year = now.year - 1;
    }
    
    setState(() {
      _currentMonth = DateTime(year, month);
      _showMonthSelector = false;
    });
  }

  void _changeYear(int delta) {
    final now = DateTime.now();
    final newYear = _currentMonth.year + delta;
    
    // No permitir años futuros
    if (newYear > now.year) return;
    
    setState(() {
      // Si cambiamos al año actual y el mes actual es futuro, ajustar al mes actual
      if (newYear == now.year && _currentMonth.month > now.month) {
        _currentMonth = DateTime(newYear, now.month);
      } else {
        _currentMonth = DateTime(newYear, _currentMonth.month);
      }
    });
  }

  String get _monthLabel {
    try {
      final format = DateFormat('MMMM yyyy', 'es_ES');
      final text = format.format(_currentMonth);
      return text[0].toUpperCase() + text.substring(1);
    } catch (e) {
      return DateFormat('MMMM yyyy').format(_currentMonth);
    }
  }

  String get _filterLabel {
    switch (_currentFilter) {
      case FilterType.last24Hours:
        return '24 horas';
      case FilterType.last7Days:
        return '7 días';
      case FilterType.last30Days:
        return '30 días';
      case FilterType.last3Months:
        return '3 meses';
      case FilterType.byMonth:
        return _monthLabel;
    }
  }

  int get _filterDays {
    switch (_currentFilter) {
      case FilterType.last24Hours:
        return 1;
      case FilterType.last7Days:
        return 7;
      case FilterType.last30Days:
        return 30;
      case FilterType.last3Months:
        return 90;
      case FilterType.byMonth:
        return 0;
    }
  }

  List<DailyUsage> get _filteredUsage {
    if (_currentFilter == FilterType.byMonth) {
      return _usageList.where((u) => 
        u.date.year == _currentMonth.year && 
        u.date.month == _currentMonth.month
      ).toList();
    }
    
    final days = _filterDays;
    final now = DateTime.now();
    final cutoff = DateTime(now.year, now.month, now.day).subtract(Duration(days: days - 1));
    
    return _usageList.where((u) => !u.date.isBefore(cutoff)).toList()
      ..sort((a, b) => b.date.compareTo(a.date));
  }

  void _showFilterBottomSheet() {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;

    showModalBottomSheet(
      context: context,
      backgroundColor: Colors.transparent,
      builder: (context) => _FilterBottomSheet(
        currentFilter: _currentFilter,
        theme: theme,
        colorScheme: colorScheme,
        onSelectFilter: (filter) {
          Navigator.pop(context);
          setState(() {
            _currentFilter = filter;
            _showMonthSelector = false;
          });
        },
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;

    return Scaffold(
      appBar: AppBar(
        titleSpacing: 0,
        title: _currentFilter == FilterType.byMonth
            ? InkWell(
                onTap: () {
                  setState(() {
                    _showMonthSelector = !_showMonthSelector;
                  });
                },
                borderRadius: BorderRadius.circular(8),
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(
                        _monthLabel,
                        style: theme.textTheme.titleMedium?.copyWith(
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                      const SizedBox(width: 4),
                      Icon(
                        _showMonthSelector ? Icons.expand_less : Icons.expand_more,
                        size: 20,
                        color: colorScheme.onSurface,
                      ),
                    ],
                  ),
                ),
              )
            : Text(_filterLabel),
        actions: [
          // Botón selector de filtro
          Padding(
            padding: const EdgeInsets.only(right: 8),
            child: IconButton(
              icon: Icon(
                Icons.filter_list_rounded,
                color: colorScheme.primary,
              ),
              tooltip: 'Filtrar',
              onPressed: _showFilterBottomSheet,
            ),
          ),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : Column(
              children: [
                // Selector de meses (solo visible en modo byMonth)
                if (_currentFilter == FilterType.byMonth && _showMonthSelector)
                  _MonthSelectorBar(
                    currentMonth: _currentMonth,
                    onSelectMonth: _selectMonth,
                    onChangeYear: _changeYear,
                    theme: theme,
                    colorScheme: colorScheme,
                  ),
                // Resumen
                _currentFilter == FilterType.byMonth
                    ? _MonthSummary(
                        usageByDate: _usageByDate,
                        currentMonth: _currentMonth,
                        theme: theme,
                        colorScheme: colorScheme,
                      )
                    : _PeriodSummary(
                        usageList: _filteredUsage,
                        filterLabel: _filterLabel,
                        theme: theme,
                        colorScheme: colorScheme,
                      ),
                // Leyenda
                _Legend(theme: theme, colorScheme: colorScheme),
                // Contenido principal
                Expanded(
                  child: _currentFilter == FilterType.byMonth
                      ? _MonthTable(
                          usageByDate: _usageByDate,
                          currentMonth: _currentMonth,
                          theme: theme,
                          colorScheme: colorScheme,
                        )
                      : _DaysList(
                          usageList: _filteredUsage,
                          theme: theme,
                          colorScheme: colorScheme,
                        ),
                ),
              ],
            ),
    );
  }
}

/// Bottom sheet para seleccionar filtro
class _FilterBottomSheet extends StatelessWidget {
  final FilterType currentFilter;
  final ThemeData theme;
  final ColorScheme colorScheme;
  final ValueChanged<FilterType> onSelectFilter;

  const _FilterBottomSheet({
    required this.currentFilter,
    required this.theme,
    required this.colorScheme,
    required this.onSelectFilter,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      constraints: BoxConstraints(
        maxHeight: MediaQuery.of(context).size.height * 0.7,
      ),
      decoration: BoxDecoration(
        color: colorScheme.surface,
        borderRadius: const BorderRadius.vertical(
          top: Radius.circular(AppTheme.radiusLarge),
        ),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          // Handle
          Container(
            margin: const EdgeInsets.only(top: 12),
            width: 40,
            height: 4,
            decoration: BoxDecoration(
              color: colorScheme.onSurfaceVariant.withValues(alpha: 0.4),
              borderRadius: BorderRadius.circular(2),
            ),
          ),
          // Título
          Padding(
            padding: const EdgeInsets.all(16),
            child: Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(8),
                  decoration: BoxDecoration(
                    color: colorScheme.primaryContainer,
                    shape: BoxShape.circle,
                  ),
                  child: Icon(
                    Icons.filter_list_rounded,
                    color: colorScheme.primary,
                    size: 20,
                  ),
                ),
                const SizedBox(width: 12),
                Text(
                  'Filtrar por período',
                  style: theme.textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ],
            ),
          ),
          // Opciones con scroll
          Flexible(
            child: SingleChildScrollView(
              padding: const EdgeInsets.fromLTRB(16, 0, 16, 24),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  _FilterOption(
                    filter: FilterType.last24Hours,
                    label: 'Últimas 24 horas',
                    subtitle: 'Hoy',
                    icon: Icons.today_rounded,
                    isSelected: currentFilter == FilterType.last24Hours,
                    onTap: () => onSelectFilter(FilterType.last24Hours),
                    theme: theme,
                    colorScheme: colorScheme,
                  ),
                  const SizedBox(height: 8),
                  _FilterOption(
                    filter: FilterType.last7Days,
                    label: 'Últimos 7 días',
                    subtitle: 'Una semana',
                    icon: Icons.view_week_rounded,
                    isSelected: currentFilter == FilterType.last7Days,
                    onTap: () => onSelectFilter(FilterType.last7Days),
                    theme: theme,
                    colorScheme: colorScheme,
                  ),
                  const SizedBox(height: 8),
                  _FilterOption(
                    filter: FilterType.last30Days,
                    label: 'Últimos 30 días',
                    subtitle: 'Un mes',
                    icon: Icons.date_range_rounded,
                    isSelected: currentFilter == FilterType.last30Days,
                    onTap: () => onSelectFilter(FilterType.last30Days),
                    theme: theme,
                    colorScheme: colorScheme,
                  ),
                  const SizedBox(height: 8),
                  _FilterOption(
                    filter: FilterType.last3Months,
                    label: 'Últimos 3 meses',
                    subtitle: 'Trimestre',
                    icon: Icons.calendar_view_month_rounded,
                    isSelected: currentFilter == FilterType.last3Months,
                    onTap: () => onSelectFilter(FilterType.last3Months),
                    theme: theme,
                    colorScheme: colorScheme,
                  ),
                  const SizedBox(height: 12),
                  Divider(color: colorScheme.outlineVariant),
                  const SizedBox(height: 8),
                  _FilterOption(
                    filter: FilterType.byMonth,
                    label: 'Por mes',
                    subtitle: 'Vista de calendario',
                    icon: Icons.calendar_month_rounded,
                    isSelected: currentFilter == FilterType.byMonth,
                    onTap: () => onSelectFilter(FilterType.byMonth),
                    theme: theme,
                    colorScheme: colorScheme,
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

/// Opción de filtro
class _FilterOption extends StatelessWidget {
  final FilterType filter;
  final String label;
  final String subtitle;
  final IconData icon;
  final bool isSelected;
  final VoidCallback onTap;
  final ThemeData theme;
  final ColorScheme colorScheme;

  const _FilterOption({
    required this.filter,
    required this.label,
    required this.subtitle,
    required this.icon,
    required this.isSelected,
    required this.onTap,
    required this.theme,
    required this.colorScheme,
  });

  @override
  Widget build(BuildContext context) {
    return Material(
      color: isSelected
          ? colorScheme.primaryContainer
          : colorScheme.surfaceContainerHighest.withValues(alpha: 0.5),
      borderRadius: BorderRadius.circular(AppTheme.radiusMedium),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(AppTheme.radiusMedium),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: isSelected
                      ? colorScheme.primary.withValues(alpha: 0.15)
                      : colorScheme.surfaceContainerHighest,
                  shape: BoxShape.circle,
                ),
                child: Icon(
                  icon,
                  color: isSelected
                      ? colorScheme.primary
                      : colorScheme.onSurfaceVariant,
                  size: 22,
                ),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      label,
                      style: theme.textTheme.titleSmall?.copyWith(
                        fontWeight: isSelected
                            ? FontWeight.w600
                            : FontWeight.w500,
                        color: isSelected
                            ? colorScheme.onPrimaryContainer
                            : colorScheme.onSurface,
                      ),
                    ),
                    Text(
                      subtitle,
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: isSelected
                            ? colorScheme.onPrimaryContainer.withValues(alpha: 0.8)
                            : colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
              if (isSelected)
                Icon(
                  Icons.check_circle_rounded,
                  color: colorScheme.primary,
                  size: 24,
                ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Resumen del período (para filtros de tiempo)
class _PeriodSummary extends StatelessWidget {
  final List<DailyUsage> usageList;
  final String filterLabel;
  final ThemeData theme;
  final ColorScheme colorScheme;

  const _PeriodSummary({
    required this.usageList,
    required this.filterLabel,
    required this.theme,
    required this.colorScheme,
  });

  @override
  Widget build(BuildContext context) {
    int totalWifi = 0;
    int totalMobile = 0;

    for (final usage in usageList) {
      totalWifi += usage.totalWifi;
      totalMobile += usage.totalMobile;
    }

    final total = totalWifi + totalMobile;

    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      decoration: BoxDecoration(
        color: colorScheme.primaryContainer,
        borderRadius: BorderRadius.circular(AppTheme.radiusMedium),
      ),
      child: Row(
        children: [
          Expanded(
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text(
                  'Total: ',
                  style: theme.textTheme.labelMedium?.copyWith(
                    color: colorScheme.onPrimaryContainer.withValues(alpha: 0.8),
                  ),
                ),
                Text(
                  DataFormatter.formatBytes(total),
                  style: theme.textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.w700,
                    color: colorScheme.onPrimaryContainer,
                  ),
                ),
              ],
            ),
          ),
          Container(
            width: 1,
            height: 24,
            color: colorScheme.onPrimaryContainer.withValues(alpha: 0.2),
          ),
          Expanded(
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: [
                _SummaryItemSmall(
                  icon: Icons.wifi_rounded,
                  value: totalWifi,
                  color: colorScheme.primary,
                  theme: theme,
                  colorScheme: colorScheme,
                ),
                _SummaryItemSmall(
                  icon: Icons.signal_cellular_alt_rounded,
                  value: totalMobile,
                  color: colorScheme.tertiary,
                  theme: theme,
                  colorScheme: colorScheme,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

/// Item de resumen pequeño
class _SummaryItemSmall extends StatelessWidget {
  final IconData icon;
  final int value;
  final Color color;
  final ThemeData theme;
  final ColorScheme colorScheme;

  const _SummaryItemSmall({
    required this.icon,
    required this.value,
    required this.color,
    required this.theme,
    required this.colorScheme,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(icon, color: color, size: 16),
        const SizedBox(width: 4),
        Text(
          DataFormatter.formatBytes(value),
          style: theme.textTheme.labelMedium?.copyWith(
            fontWeight: FontWeight.w600,
            color: colorScheme.onPrimaryContainer,
          ),
        ),
      ],
    );
  }
}

/// Lista de días (para filtros de tiempo)
class _DaysList extends StatelessWidget {
  final List<DailyUsage> usageList;
  final ThemeData theme;
  final ColorScheme colorScheme;

  const _DaysList({
    required this.usageList,
    required this.theme,
    required this.colorScheme,
  });

  @override
  Widget build(BuildContext context) {
    if (usageList.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              Icons.inbox_rounded,
              size: 48,
              color: colorScheme.onSurfaceVariant.withValues(alpha: 0.5),
            ),
            const SizedBox(height: 16),
            Text(
              'Sin datos en este período',
              style: theme.textTheme.bodyLarge?.copyWith(
                color: colorScheme.onSurfaceVariant,
              ),
            ),
          ],
        ),
      );
    }

    return ListView.builder(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      itemCount: usageList.length,
      itemBuilder: (context, index) {
        return _DayCard(
          usage: usageList[index],
          theme: theme,
          colorScheme: colorScheme,
        );
      },
    );
  }
}

/// Tarjeta de día
class _DayCard extends StatelessWidget {
  final DailyUsage usage;
  final ThemeData theme;
  final ColorScheme colorScheme;

  const _DayCard({
    required this.usage,
    required this.theme,
    required this.colorScheme,
  });

  bool get _isToday {
    final now = DateTime.now();
    return usage.date.year == now.year &&
        usage.date.month == now.month &&
        usage.date.day == now.day;
  }

  String get _formattedDate {
    try {
      final dateFormat = DateFormat('EEEE, d MMM', 'es_ES');
      final text = dateFormat.format(usage.date);
      return text[0].toUpperCase() + text.substring(1);
    } catch (e) {
      return DateFormat('dd/MM/yyyy').format(usage.date);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 6),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: _isToday
            ? colorScheme.primaryContainer.withValues(alpha: 0.5)
            : colorScheme.surfaceContainerHighest.withValues(alpha: 0.5),
        borderRadius: BorderRadius.circular(AppTheme.radiusMedium),
        border: _isToday
            ? Border.all(color: colorScheme.primary.withValues(alpha: 0.5))
            : null,
      ),
      child: Row(
        children: [
          // Fecha
          Expanded(
            flex: 2,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  _isToday ? 'Hoy' : _formattedDate,
                  style: theme.textTheme.titleSmall?.copyWith(
                    fontWeight: FontWeight.w600,
                    color: colorScheme.onSurface,
                  ),
                ),
                Text(
                  DataFormatter.formatBytes(usage.totalBytes),
                  style: theme.textTheme.bodySmall?.copyWith(
                    color: colorScheme.onSurfaceVariant,
                  ),
                ),
              ],
            ),
          ),
          // WiFi
          Expanded(
            child: Column(
              children: [
                Icon(Icons.wifi_rounded, size: 16, color: colorScheme.primary),
                const SizedBox(height: 2),
                Text(
                  DataFormatter.formatBytes(usage.totalWifi),
                  style: theme.textTheme.labelSmall?.copyWith(
                    fontWeight: FontWeight.w600,
                    color: colorScheme.primary,
                  ),
                ),
              ],
            ),
          ),
          // Móvil
          Expanded(
            child: Column(
              children: [
                Icon(Icons.signal_cellular_alt_rounded, size: 16, color: colorScheme.tertiary),
                const SizedBox(height: 2),
                Text(
                  DataFormatter.formatBytes(usage.totalMobile),
                  style: theme.textTheme.labelSmall?.copyWith(
                    fontWeight: FontWeight.w600,
                    color: colorScheme.tertiary,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

/// Barra de selector de meses horizontal
class _MonthSelectorBar extends StatelessWidget {
  final DateTime currentMonth;
  final ValueChanged<int> onSelectMonth;
  final ValueChanged<int> onChangeYear;
  final ThemeData theme;
  final ColorScheme colorScheme;

  const _MonthSelectorBar({
    required this.currentMonth,
    required this.onSelectMonth,
    required this.onChangeYear,
    required this.theme,
    required this.colorScheme,
  });

  static const _monthNames = [
    'Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun',
    'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'
  ];

  @override
  Widget build(BuildContext context) {
    final now = DateTime.now();
    final isCurrentYear = currentMonth.year == now.year;

    return Container(
      color: colorScheme.surfaceContainerHighest.withValues(alpha: 0.5),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          // Selector de año
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                IconButton(
                  onPressed: () => onChangeYear(-1),
                  icon: const Icon(Icons.chevron_left_rounded),
                  iconSize: 20,
                  style: IconButton.styleFrom(
                    backgroundColor: colorScheme.surfaceContainerHighest,
                    minimumSize: const Size(32, 32),
                    padding: EdgeInsets.zero,
                  ),
                ),
                const SizedBox(width: 16),
                Text(
                  '${currentMonth.year}',
                  style: theme.textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.w600,
                  ),
                ),
                const SizedBox(width: 16),
                IconButton(
                  onPressed: isCurrentYear ? null : () => onChangeYear(1),
                  icon: const Icon(Icons.chevron_right_rounded),
                  iconSize: 20,
                  style: IconButton.styleFrom(
                    backgroundColor: isCurrentYear
                        ? colorScheme.surfaceContainerHighest.withValues(alpha: 0.3)
                        : colorScheme.surfaceContainerHighest,
                    minimumSize: const Size(32, 32),
                    padding: EdgeInsets.zero,
                  ),
                ),
              ],
            ),
          ),
          // Chips de meses
          SizedBox(
            height: 40,
            child: ListView.builder(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.symmetric(horizontal: 12),
              itemCount: 12,
              itemBuilder: (context, index) {
                final month = index + 1;
                final isSelected = currentMonth.month == month;
                final isFuture = isCurrentYear && month > now.month;

                return Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 4),
                  child: ChoiceChip(
                    label: Text(_monthNames[index]),
                    selected: isSelected,
                    onSelected: isFuture ? null : (_) => onSelectMonth(month),
                    labelStyle: TextStyle(
                      fontSize: 12,
                      fontWeight: isSelected ? FontWeight.w600 : FontWeight.w500,
                      color: isFuture
                          ? colorScheme.onSurface.withValues(alpha: 0.3)
                          : isSelected
                              ? colorScheme.onPrimaryContainer
                              : colorScheme.onSurfaceVariant,
                    ),
                    backgroundColor: colorScheme.surface,
                    selectedColor: colorScheme.primaryContainer,
                    showCheckmark: false,
                    padding: const EdgeInsets.symmetric(horizontal: 8),
                    materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
                    visualDensity: VisualDensity.compact,
                  ),
                );
              },
            ),
          ),
          const SizedBox(height: 8),
        ],
      ),
    );
  }
}

/// Resumen del mes
class _MonthSummary extends StatelessWidget {
  final Map<String, DailyUsage> usageByDate;
  final DateTime currentMonth;
  final ThemeData theme;
  final ColorScheme colorScheme;

  const _MonthSummary({
    required this.usageByDate,
    required this.currentMonth,
    required this.theme,
    required this.colorScheme,
  });

  @override
  Widget build(BuildContext context) {
    int totalWifi = 0;
    int totalMobile = 0;

    // Calcular totales del mes
    final daysInMonth = DateTime(currentMonth.year, currentMonth.month + 1, 0).day;
    for (int day = 1; day <= daysInMonth; day++) {
      final date = DateTime(currentMonth.year, currentMonth.month, day);
      final key = '${date.year}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}';
      final usage = usageByDate[key];
      if (usage != null) {
        totalWifi += usage.totalWifi;
        totalMobile += usage.totalMobile;
      }
    }

    final total = totalWifi + totalMobile;

    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      decoration: BoxDecoration(
        color: colorScheme.primaryContainer,
        borderRadius: BorderRadius.circular(AppTheme.radiusMedium),
      ),
      child: Row(
        children: [
          Expanded(
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text(
                  'Total: ',
                  style: theme.textTheme.labelMedium?.copyWith(
                    color: colorScheme.onPrimaryContainer.withValues(alpha: 0.8),
                  ),
                ),
                Text(
                  DataFormatter.formatBytes(total),
                  style: theme.textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.w700,
                    color: colorScheme.onPrimaryContainer,
                  ),
                ),
              ],
            ),
          ),
          Container(
            width: 1,
            height: 24,
            color: colorScheme.onPrimaryContainer.withValues(alpha: 0.2),
          ),
          Expanded(
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: [
                _SummaryItem(
                  icon: Icons.wifi_rounded,
                  value: totalWifi,
                  color: colorScheme.primary,
                  theme: theme,
                  colorScheme: colorScheme,
                ),
                _SummaryItem(
                  icon: Icons.signal_cellular_alt_rounded,
                  value: totalMobile,
                  color: colorScheme.tertiary,
                  theme: theme,
                  colorScheme: colorScheme,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

/// Item de resumen
class _SummaryItem extends StatelessWidget {
  final IconData icon;
  final int value;
  final Color color;
  final ThemeData theme;
  final ColorScheme colorScheme;

  const _SummaryItem({
    required this.icon,
    required this.value,
    required this.color,
    required this.theme,
    required this.colorScheme,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(icon, color: color, size: 16),
        const SizedBox(width: 4),
        Text(
          DataFormatter.formatBytes(value),
          style: theme.textTheme.labelMedium?.copyWith(
            fontWeight: FontWeight.w600,
            color: colorScheme.onPrimaryContainer,
          ),
        ),
      ],
    );
  }
}

/// Leyenda de colores
class _Legend extends StatelessWidget {
  final ThemeData theme;
  final ColorScheme colorScheme;

  const _Legend({required this.theme, required this.colorScheme});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.wifi_rounded, size: 14, color: colorScheme.primary),
          const SizedBox(width: 3),
          Text(
            'WiFi',
            style: theme.textTheme.labelSmall?.copyWith(
              color: colorScheme.onSurfaceVariant,
            ),
          ),
          const SizedBox(width: 16),
          Icon(
            Icons.signal_cellular_alt_rounded,
            size: 14,
            color: colorScheme.tertiary,
          ),
          const SizedBox(width: 3),
          Text(
            'Móvil',
            style: theme.textTheme.labelSmall?.copyWith(
              color: colorScheme.onSurfaceVariant,
            ),
          ),
        ],
      ),
    );
  }
}

/// Tabla del mes
class _MonthTable extends StatelessWidget {
  final Map<String, DailyUsage> usageByDate;
  final DateTime currentMonth;
  final ThemeData theme;
  final ColorScheme colorScheme;

  const _MonthTable({
    required this.usageByDate,
    required this.currentMonth,
    required this.theme,
    required this.colorScheme,
  });

  String _dateKey(DateTime date) {
    return '${date.year}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}';
  }

  @override
  Widget build(BuildContext context) {
    final daysInMonth = DateTime(currentMonth.year, currentMonth.month + 1, 0).day;
    final firstDayOfMonth = DateTime(currentMonth.year, currentMonth.month, 1);
    // Ajustar para que la semana empiece en lunes (1=lunes, 7=domingo)
    int firstWeekday = firstDayOfMonth.weekday; // 1-7 donde 1=lunes

    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);

    return Column(
      children: [
        // Encabezado de días de la semana
        Container(
          padding: const EdgeInsets.symmetric(vertical: 8, horizontal: 4),
          child: Row(
            children: ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom']
                .map(
                  (day) => Expanded(
                    child: Center(
                      child: Text(
                        day,
                        style: theme.textTheme.labelSmall?.copyWith(
                          fontWeight: FontWeight.w600,
                          color: colorScheme.onSurfaceVariant,
                        ),
                      ),
                    ),
                  ),
                )
                .toList(),
          ),
        ),
        // Celdas del calendario
        Expanded(
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 2),
            child: Column(
              children: _buildWeeks(
                daysInMonth,
                firstWeekday,
                today,
              ),
            ),
          ),
        ),
      ],
    );
  }

  List<Widget> _buildWeeks(int daysInMonth, int firstWeekday, DateTime today) {
    List<Widget> weeks = [];
    List<Widget> currentWeek = [];

    // Añadir celdas vacías al principio
    for (int i = 1; i < firstWeekday; i++) {
      currentWeek.add(const Expanded(child: SizedBox()));
    }

    // Añadir días del mes
    for (int day = 1; day <= daysInMonth; day++) {
      final date = DateTime(currentMonth.year, currentMonth.month, day);
      final key = _dateKey(date);
      final usage = usageByDate[key];
      final isToday = date.isAtSameMomentAs(today);
      final isFuture = date.isAfter(today);

      currentWeek.add(
        Expanded(
          child: _DayCell(
            day: day,
            date: date,
            usage: usage,
            isToday: isToday,
            isFuture: isFuture,
            theme: theme,
            colorScheme: colorScheme,
          ),
        ),
      );

      // Si completamos una semana, la añadimos
      if (currentWeek.length == 7) {
        weeks.add(
          Expanded(
            child: Row(children: currentWeek),
          ),
        );
        currentWeek = [];
      }
    }

    // Añadir celdas vacías al final si es necesario
    if (currentWeek.isNotEmpty) {
      while (currentWeek.length < 7) {
        currentWeek.add(const Expanded(child: SizedBox()));
      }
      weeks.add(Expanded(child: Row(children: currentWeek)));
    }

    return weeks;
  }
}

/// Celda de un día
class _DayCell extends StatelessWidget {
  final int day;
  final DateTime date;
  final DailyUsage? usage;
  final bool isToday;
  final bool isFuture;
  final ThemeData theme;
  final ColorScheme colorScheme;

  const _DayCell({
    required this.day,
    required this.date,
    required this.usage,
    required this.isToday,
    required this.isFuture,
    required this.theme,
    required this.colorScheme,
  });

  void _showDayDetails(BuildContext context) {
    if (isFuture) return;
    
    showModalBottomSheet(
      context: context,
      backgroundColor: Colors.transparent,
      builder: (context) => _DayDetailBottomSheet(
        date: date,
        usage: usage,
        isToday: isToday,
        theme: theme,
        colorScheme: colorScheme,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final hasData = usage != null && usage!.totalBytes > 0;

    return GestureDetector(
      onTap: () => _showDayDetails(context),
      child: Container(
        margin: const EdgeInsets.all(1),
        padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 4),
      decoration: BoxDecoration(
        color: isToday
            ? colorScheme.primaryContainer
            : colorScheme.surfaceContainerHighest.withValues(alpha: isFuture ? 0.3 : 0.5),
        borderRadius: BorderRadius.circular(8),
        border: isToday
            ? Border.all(color: colorScheme.primary, width: 2)
            : null,
      ),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          // Número del día
          Text(
            '$day',
            style: theme.textTheme.titleSmall?.copyWith(
              fontWeight: isToday ? FontWeight.w700 : FontWeight.w600,
              color: isFuture
                  ? colorScheme.onSurface.withValues(alpha: 0.3)
                  : isToday
                      ? colorScheme.onPrimaryContainer
                      : colorScheme.onSurface,
            ),
          ),
          if (hasData && !isFuture) ...[
            const SizedBox(height: 3),
            // WiFi
            _UsageRow(
              icon: Icons.wifi_rounded,
              value: usage!.totalWifi,
              color: colorScheme.primary,
              theme: theme,
            ),
            const SizedBox(height: 1),
            // Móvil
            _UsageRow(
              icon: Icons.signal_cellular_alt_rounded,
              value: usage!.totalMobile,
              color: colorScheme.tertiary,
              theme: theme,
            ),
          ] else if (!isFuture) ...[
            const SizedBox(height: 3),
            Text(
              '-',
              style: theme.textTheme.bodySmall?.copyWith(
                color: colorScheme.onSurfaceVariant.withValues(alpha: 0.5),
              ),
            ),
          ],
        ],
      ),
    ),
    );
  }
}

/// Bottom sheet con detalles del día
class _DayDetailBottomSheet extends StatelessWidget {
  final DateTime date;
  final DailyUsage? usage;
  final bool isToday;
  final ThemeData theme;
  final ColorScheme colorScheme;

  const _DayDetailBottomSheet({
    required this.date,
    required this.usage,
    required this.isToday,
    required this.theme,
    required this.colorScheme,
  });

  String get _formattedDate {
    try {
      final dateFormat = DateFormat('EEEE, d MMMM yyyy', 'es_ES');
      final text = dateFormat.format(date);
      return text[0].toUpperCase() + text.substring(1);
    } catch (e) {
      return DateFormat('dd/MM/yyyy').format(date);
    }
  }

  @override
  Widget build(BuildContext context) {
    final hasData = usage != null && usage!.totalBytes > 0;
    
    return ConstrainedBox(
      constraints: BoxConstraints(
        maxHeight: MediaQuery.of(context).size.height * 0.75,
      ),
      child: Container(
        decoration: BoxDecoration(
          color: colorScheme.surface,
          borderRadius: const BorderRadius.vertical(
            top: Radius.circular(AppTheme.radiusLarge),
          ),
        ),
        child: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
          // Handle
          Container(
            margin: const EdgeInsets.only(top: 12),
            width: 40,
            height: 4,
            decoration: BoxDecoration(
              color: colorScheme.onSurfaceVariant.withValues(alpha: 0.4),
              borderRadius: BorderRadius.circular(2),
            ),
          ),
          // Fecha
          Padding(
            padding: const EdgeInsets.all(20),
            child: Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: isToday 
                        ? colorScheme.primaryContainer 
                        : colorScheme.surfaceContainerHighest,
                    shape: BoxShape.circle,
                  ),
                  child: Icon(
                    Icons.calendar_today_rounded,
                    color: isToday 
                        ? colorScheme.primary 
                        : colorScheme.onSurfaceVariant,
                    size: 24,
                  ),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      if (isToday)
                        Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 8,
                            vertical: 2,
                          ),
                          margin: const EdgeInsets.only(bottom: 4),
                          decoration: BoxDecoration(
                            color: colorScheme.primary,
                            borderRadius: BorderRadius.circular(4),
                          ),
                          child: Text(
                            'HOY',
                            style: theme.textTheme.labelSmall?.copyWith(
                              color: colorScheme.onPrimary,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ),
                      Text(
                        _formattedDate,
                        style: theme.textTheme.titleMedium?.copyWith(
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          
          if (!hasData) ...[
            // Sin datos
            Padding(
              padding: const EdgeInsets.fromLTRB(20, 0, 20, 32),
              child: Container(
                padding: const EdgeInsets.all(24),
                decoration: BoxDecoration(
                  color: colorScheme.surfaceContainerHighest.withValues(alpha: 0.5),
                  borderRadius: BorderRadius.circular(AppTheme.radiusMedium),
                ),
                child: Column(
                  children: [
                    Icon(
                      Icons.inbox_rounded,
                      size: 40,
                      color: colorScheme.onSurfaceVariant.withValues(alpha: 0.5),
                    ),
                    const SizedBox(height: 12),
                    Text(
                      'Sin datos registrados',
                      style: theme.textTheme.bodyLarge?.copyWith(
                        color: colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ] else ...[
            // Total
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 20),
              child: Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: colorScheme.primaryContainer,
                  borderRadius: BorderRadius.circular(AppTheme.radiusMedium),
                ),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Text(
                      'Total consumido: ',
                      style: theme.textTheme.titleSmall?.copyWith(
                        color: colorScheme.onPrimaryContainer.withValues(alpha: 0.8),
                      ),
                    ),
                    Text(
                      DataFormatter.formatBytes(usage!.totalBytes),
                      style: theme.textTheme.headlineSmall?.copyWith(
                        fontWeight: FontWeight.w700,
                        color: colorScheme.onPrimaryContainer,
                      ),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 16),
            // WiFi
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 20),
              child: _DetailCard(
                title: 'WiFi',
                icon: Icons.wifi_rounded,
                color: colorScheme.primary,
                received: usage!.wifiReceived,
                sent: usage!.wifiSent,
                total: usage!.totalWifi,
                theme: theme,
                colorScheme: colorScheme,
              ),
            ),
            const SizedBox(height: 12),
            // Móvil
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 20),
              child: _DetailCard(
                title: 'Datos móviles',
                icon: Icons.signal_cellular_alt_rounded,
                color: colorScheme.tertiary,
                received: usage!.mobileReceived,
                sent: usage!.mobileSent,
                total: usage!.totalMobile,
                theme: theme,
                colorScheme: colorScheme,
              ),
            ),
            const SizedBox(height: 24),
          ],
        ],
      ),
        ),
      ),
    );
  }
}

/// Tarjeta de detalle (WiFi o Móvil)
class _DetailCard extends StatelessWidget {
  final String title;
  final IconData icon;
  final Color color;
  final int received;
  final int sent;
  final int total;
  final ThemeData theme;
  final ColorScheme colorScheme;

  const _DetailCard({
    required this.title,
    required this.icon,
    required this.color,
    required this.received,
    required this.sent,
    required this.total,
    required this.theme,
    required this.colorScheme,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: colorScheme.surfaceContainerHighest.withValues(alpha: 0.5),
        borderRadius: BorderRadius.circular(AppTheme.radiusMedium),
      ),
      child: Column(
        children: [
          // Título y total
          Row(
            children: [
              Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  color: color.withValues(alpha: 0.15),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Icon(icon, color: color, size: 20),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Text(
                  title,
                  style: theme.textTheme.titleSmall?.copyWith(
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
              Text(
                DataFormatter.formatBytes(total),
                style: theme.textTheme.titleMedium?.copyWith(
                  fontWeight: FontWeight.w700,
                  color: color,
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          // Recibido y enviado
          Row(
            children: [
              Expanded(
                child: _DataItem(
                  label: 'Recibido',
                  icon: Icons.arrow_downward_rounded,
                  value: received,
                  color: colorScheme.primary,
                  theme: theme,
                  colorScheme: colorScheme,
                ),
              ),
              Container(
                width: 1,
                height: 40,
                color: colorScheme.outlineVariant.withValues(alpha: 0.5),
              ),
              Expanded(
                child: _DataItem(
                  label: 'Enviado',
                  icon: Icons.arrow_upward_rounded,
                  value: sent,
                  color: colorScheme.tertiary,
                  theme: theme,
                  colorScheme: colorScheme,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

/// Item de datos (recibido/enviado)
class _DataItem extends StatelessWidget {
  final String label;
  final IconData icon;
  final int value;
  final Color color;
  final ThemeData theme;
  final ColorScheme colorScheme;

  const _DataItem({
    required this.label,
    required this.icon,
    required this.value,
    required this.color,
    required this.theme,
    required this.colorScheme,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(icon, size: 14, color: colorScheme.onSurfaceVariant),
            const SizedBox(width: 4),
            Text(
              label,
              style: theme.textTheme.labelSmall?.copyWith(
                color: colorScheme.onSurfaceVariant,
              ),
            ),
          ],
        ),
        const SizedBox(height: 4),
        Text(
          DataFormatter.formatBytes(value),
          style: theme.textTheme.titleSmall?.copyWith(
            fontWeight: FontWeight.w600,
            color: colorScheme.onSurface,
          ),
        ),
      ],
    );
  }
}

/// Fila de uso en celda
class _UsageRow extends StatelessWidget {
  final IconData icon;
  final int value;
  final Color color;
  final ThemeData theme;

  const _UsageRow({
    required this.icon,
    required this.value,
    required this.color,
    required this.theme,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Icon(icon, size: 11, color: color),
        const SizedBox(width: 2),
        Flexible(
          child: Text(
            _formatCompact(value),
            style: theme.textTheme.labelSmall?.copyWith(
              fontSize: 11,
              fontWeight: FontWeight.w600,
              color: color,
            ),
            overflow: TextOverflow.ellipsis,
          ),
        ),
      ],
    );
  }

  String _formatCompact(int bytes) {
    if (bytes >= 1073741824) {
      return '${(bytes / 1073741824).toStringAsFixed(1)}G';
    } else if (bytes >= 1048576) {
      return '${(bytes / 1048576).toStringAsFixed(0)}M';
    } else if (bytes >= 1024) {
      return '${(bytes / 1024).toStringAsFixed(0)}K';
    } else if (bytes > 0) {
      return '${bytes}B';
    }
    return '0';
  }
}
