@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.donyaep.netflow.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import com.donyaep.netflow.ui.theme.AppCodeFontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.donyaep.netflow.data.model.AppSettings
import com.donyaep.netflow.data.model.DataLimitUnit
import com.donyaep.netflow.data.model.SpeedUnit
import com.donyaep.netflow.data.model.ThemeMode
import com.donyaep.netflow.data.model.displayName
import java.time.LocalDate

// ── Shapes para ítems segmentados ──────────────────────────────────────────
private val topSegmentShape    = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
private val middleSegmentShape = RoundedCornerShape(4.dp)
private val bottomSegmentShape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 28.dp, bottomEnd = 28.dp)
private val singleSegmentShape = RoundedCornerShape(28.dp)

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onOpenAdvanced: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenUpdates: () -> Unit,
) {
    val settings by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }

    var showThemeSheet     by remember { mutableStateOf(false) }
    var showSpeedUnitSheet by remember { mutableStateOf(false) }
    var showDataLimitSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumFlexibleTopAppBar(
                title = { Text("Ajustes", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                },
                windowInsets = WindowInsets(0),
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {

            // ── 1. Apariencia ─────────────────────────────────────────────
            item {
                val themeIcon = when (settings.themeMode) {
                    ThemeMode.System -> Icons.Rounded.Sync
                    ThemeMode.Light  -> Icons.Rounded.LightMode
                    ThemeMode.Dark   -> Icons.Rounded.DarkMode
                }
                SettingsSection(title = "Apariencia") {
                    SettingsItem(
                        onClick = { showThemeSheet = true },
                        shape = singleSegmentShape,
                        headlineContent = { Text("Tema") },
                        supportingContent = { Text(settings.themeMode.displayName) },
                        leadingContent = { Icon(themeIcon, contentDescription = null) },
                        trailingContent = { Icon(Icons.Rounded.ChevronRight, contentDescription = null) },
                    )
                }
            }

            // ── 2. Monitoreo ──────────────────────────────────────────────
            item {
                SettingsSection(title = "Monitoreo") {
                    SettingsItem(
                        onClick = { showSpeedUnitSheet = true },
                        shape = topSegmentShape,
                        headlineContent = { Text("Unidad de velocidad") },
                        supportingContent = { Text(settings.speedUnit.displayName) },
                        leadingContent = { Icon(Icons.Rounded.Speed, contentDescription = null) },
                        trailingContent = { Icon(Icons.Rounded.ChevronRight, contentDescription = null) },
                    )
                    Spacer(Modifier.height(2.dp))
                    SettingsItem(
                        onClick = viewModel::toggleHideOnLockscreen,
                        shape = bottomSegmentShape,
                        headlineContent = { Text("Ocultar en lockscreen") },
                        supportingContent = {
                            Text(
                                if (settings.hideOnLockscreen)
                                    "Velocidad oculta en pantalla de bloqueo"
                                else
                                    "Velocidad visible en pantalla de bloqueo",
                            )
                        },
                        leadingContent = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                        trailingContent = {
                            Switch(
                                checked = settings.hideOnLockscreen,
                                onCheckedChange = null,
                            )
                        },
                    )
                }
            }

            // ── 3. Límite de datos ─────────────────────────────────────────
            item {
                val limitItemShape = if (settings.dataLimitEnabled) topSegmentShape else singleSegmentShape
                SettingsSection(title = "Límite de datos") {
                    SettingsItem(
                        onClick = viewModel::toggleDataLimitEnabled,
                        shape = limitItemShape,
                        headlineContent = { Text("Habilitar límite") },
                        supportingContent = {
                            Text(
                                if (settings.dataLimitEnabled)
                                    "Activo · ${formatLimit(settings.dataLimitValue)} ${settings.dataLimitUnit.name}/mes"
                                else
                                    "Sin límite de datos configurado",
                            )
                        },
                        leadingContent = { Icon(Icons.Rounded.DataUsage, contentDescription = null) },
                        trailingContent = {
                            Switch(
                                checked = settings.dataLimitEnabled,
                                onCheckedChange = null,
                            )
                        },
                    )
                    AnimatedVisibility(
                        visible = settings.dataLimitEnabled,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        Column {
                            Spacer(Modifier.height(2.dp))
                            SettingsItem(
                                onClick = { showDataLimitSheet = true },
                                shape = bottomSegmentShape,
                                headlineContent = { Text("Configurar límite") },
                                supportingContent = {
                                    Text(
                                        "${formatLimit(settings.dataLimitValue)} ${settings.dataLimitUnit.name}" +
                                            " · Día ${settings.billingCycleDay} del mes",
                                    )
                                },
                                leadingContent = { Icon(Icons.Rounded.Tune, contentDescription = null) },
                                trailingContent = { Icon(Icons.Rounded.ChevronRight, contentDescription = null) },
                            )
                        }
                    }
                }
            }

            // ── 4. Más opciones ───────────────────────────────────────────
            item {
                SettingsSection(title = "Más opciones") {
                    SettingsItem(
                        onClick = onOpenAdvanced,
                        shape = topSegmentShape,
                        headlineContent = { Text("Opciones avanzadas") },
                        supportingContent = { Text("Configuración técnica del servicio") },
                        leadingContent = { Icon(Icons.Rounded.Tune, contentDescription = null) },
                        trailingContent = { Icon(Icons.Rounded.ChevronRight, contentDescription = null) },
                    )
                    Spacer(Modifier.height(2.dp))
                    SettingsItem(
                        onClick = onOpenUpdates,
                        shape = middleSegmentShape,
                        headlineContent = { Text("Buscar actualizaciones") },
                        supportingContent = { Text("Verificar nuevas versiones en GitHub") },
                        leadingContent = { Icon(Icons.Rounded.SystemUpdate, contentDescription = null) },
                        trailingContent = { Icon(Icons.Rounded.ChevronRight, contentDescription = null) },
                    )
                    Spacer(Modifier.height(2.dp))
                    SettingsItem(
                        onClick = onOpenAbout,
                        shape = bottomSegmentShape,
                        headlineContent = { Text("Acerca de NetFlow") },
                        supportingContent = { Text("Versión e información de la aplicación") },
                        leadingContent = { Icon(Icons.Rounded.Info, contentDescription = null) },
                        trailingContent = { Icon(Icons.Rounded.ChevronRight, contentDescription = null) },
                    )
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }

    // ── Bottom Sheets ──────────────────────────────────────────────────────
    if (showThemeSheet) {
        ThemePickerSheet(
            current = settings.themeMode,
            onSelect = { viewModel.setThemeMode(it); showThemeSheet = false },
            onDismiss = { showThemeSheet = false },
        )
    }

    if (showSpeedUnitSheet) {
        SpeedUnitSheet(
            current = settings.speedUnit,
            onSelect = { viewModel.setSpeedUnit(it); showSpeedUnitSheet = false },
            onDismiss = { showSpeedUnitSheet = false },
        )
    }

    if (showDataLimitSheet) {
        DataLimitSheet(
            settings = settings,
            onSave = { value, unit, day ->
                viewModel.setDataLimit(value, unit, day)
                showDataLimitSheet = false
            },
            onDismiss = { showDataLimitSheet = false },
        )
    }
}

// ── Ítem de configuración (Surface + ListItem) ────────────────────────────
@Composable
private fun SettingsItem(
    onClick: () -> Unit,
    shape: Shape,
    headlineContent: @Composable () -> Unit,
    supportingContent: @Composable (() -> Unit)? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    Surface(
        onClick = onClick,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        ListItem(
            headlineContent = headlineContent,
            supportingContent = supportingContent,
            leadingContent = leadingContent,
            trailingContent = trailingContent,
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent,
            ),
        )
    }
}

// ── Section Header ─────────────────────────────────────────────────────────
@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Column(content = content)
    }
}

// ── Theme Picker Sheet ─────────────────────────────────────────────────────
@Composable
private fun ThemePickerSheet(
    current: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
        ) {
            Text(
                text = "Apariencia",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Elige el tema de la aplicación",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            val modes = listOf(
                Triple(ThemeMode.System, Icons.Rounded.Sync, "Automático" to "Sigue la configuración del sistema"),
                Triple(ThemeMode.Light, Icons.Rounded.LightMode, "Claro" to "Siempre usar tema claro"),
                Triple(ThemeMode.Dark, Icons.Rounded.DarkMode, "Oscuro" to "Siempre usar tema oscuro"),
            )
            modes.forEachIndexed { index, (mode, icon, labels) ->
                val shape = when (index) {
                    0              -> topSegmentShape
                    modes.size - 1 -> bottomSegmentShape
                    else           -> middleSegmentShape
                }
                if (index > 0) Spacer(Modifier.height(2.dp))
                SettingsItem(
                    onClick = { onSelect(mode) },
                    shape = shape,
                    headlineContent = {
                        Text(
                            text = labels.first,
                            fontWeight = if (current == mode) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    },
                    supportingContent = { Text(labels.second) },
                    leadingContent = { Icon(icon, contentDescription = null) },
                    trailingContent = {
                        if (current == mode) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = cs.primary,
                            )
                        }
                    },
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Speed Unit Sheet ───────────────────────────────────────────────────────
@Composable
private fun SpeedUnitSheet(
    current: SpeedUnit,
    onSelect: (SpeedUnit) -> Unit,
    onDismiss: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
        ) {
            Text(
                text = "Unidad de velocidad",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Elige cómo se muestra la velocidad de red",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            val options = listOf(
                Triple(SpeedUnit.BytesPerSecond, "Bytes por segundo", "KB/s, MB/s — Común en apps de archivos"),
                Triple(SpeedUnit.BitsPerSecond, "Bits por segundo", "Kbps, Mbps — Común en pruebas de velocidad"),
            )
            options.forEachIndexed { index, (unit, label, subtitle) ->
                val shape = if (index == 0) topSegmentShape else bottomSegmentShape
                if (index > 0) Spacer(Modifier.height(2.dp))
                SettingsItem(
                    onClick = { onSelect(unit) },
                    shape = shape,
                    headlineContent = {
                        Text(
                            text = label,
                            fontWeight = if (current == unit) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    },
                    supportingContent = { Text(subtitle) },
                    leadingContent = { Icon(Icons.Rounded.Speed, contentDescription = null) },
                    trailingContent = {
                        if (current == unit) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = cs.primary,
                            )
                        }
                    },
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Data Limit Sheet ───────────────────────────────────────────────────────
@Composable
private fun DataLimitSheet(
    settings: AppSettings,
    onSave: (value: Double, unit: DataLimitUnit, billingDay: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var localValueText by remember { mutableStateOf(formatLimit(settings.dataLimitValue)) }
    var localUnit  by remember { mutableStateOf(settings.dataLimitUnit) }
    val currentMonthDays = LocalDate.now().lengthOfMonth()
    var localDay   by remember { mutableIntStateOf(settings.billingCycleDay.coerceAtMost(currentMonthDays)) }

    val parsedValue: Double? = localValueText.replace(",", ".").toDoubleOrNull()?.coerceAtLeast(0.1)
    val isValid = parsedValue != null
    val cs = MaterialTheme.colorScheme

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Límite de datos",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                androidx.compose.material3.TextButton(
                    onClick = {
                        localValueText = "5"
                        localUnit = DataLimitUnit.GB
                        localDay = 1
                    },
                ) {
                    Icon(
                        Icons.Rounded.Restore,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                    Text("Restablecer")
                }
            }

            // Preview del valor
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = cs.errorContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = parsedValue?.let { formatLimit(it) } ?: "—",
                        style = MaterialTheme.typography.displaySmall.copy(fontFamily = AppCodeFontFamily),
                        fontWeight = FontWeight.Bold,
                        color = cs.error,
                    )
                    Text(
                        text = "${localUnit.name}/mes",
                        style = MaterialTheme.typography.titleMedium,
                        color = cs.onErrorContainer,
                    )
                }
            }

            // Presets rápidos
            Text(
                text = "Presets rápidos",
                style = MaterialTheme.typography.labelLarge,
                color = cs.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    500.0 to DataLimitUnit.MB,
                    1.0   to DataLimitUnit.GB,
                    2.0   to DataLimitUnit.GB,
                    5.0   to DataLimitUnit.GB,
                    10.0  to DataLimitUnit.GB,
                    20.0  to DataLimitUnit.GB,
                    25.0  to DataLimitUnit.GB,
                    50.0  to DataLimitUnit.GB,
                ).forEach { (v, u) ->
                    SuggestionChip(
                        onClick = { localValueText = formatLimit(v); localUnit = u },
                        label = { Text("${formatLimit(v)} ${u.name}") },
                    )
                }
            }

            // Selector de unidad
            Text(
                text = "Unidad",
                style = MaterialTheme.typography.labelLarge,
                color = cs.onSurfaceVariant,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            ) {
                DataLimitUnit.entries.forEach { unit ->
                    ToggleButton(
                        checked = localUnit == unit,
                        onCheckedChange = { if (it) localUnit = unit },
                    ) {
                        Text(unit.name)
                    }
                }
            }

            // Campo de cantidad
            OutlinedTextField(
                value = localValueText,
                onValueChange = { localValueText = it },
                label = { Text("Cantidad") },
                suffix = { Text(localUnit.name) },
                isError = !isValid,
                supportingText = if (!isValid) {{ Text("Ingresa un número válido mayor que 0") }} else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // Día de inicio del ciclo
            Text(
                text = "Día de inicio del ciclo de facturación: $localDay",
                style = MaterialTheme.typography.labelLarge,
                color = cs.onSurfaceVariant,
            )
            Slider(
                value = localDay.toFloat(),
                onValueChange = { localDay = it.toInt().coerceIn(1, currentMonthDays) },
                valueRange = 1f..currentMonthDays.toFloat(),
                steps = currentMonthDays - 2,
            )

            // Botones
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Cancelar")
                }
                Button(
                    onClick = { onSave((parsedValue ?: settings.dataLimitValue).coerceAtLeast(0.1), localUnit, localDay) },
                    enabled = isValid,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Guardar")
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Helpers ────────────────────────────────────────────────────────────────
private fun formatLimit(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString()
    else String.format("%.1f", value)
