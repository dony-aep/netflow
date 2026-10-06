@file:OptIn(ExperimentalMaterial3Api::class)

package com.donyaep.netflow.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import com.donyaep.netflow.BuildConfig
import com.donyaep.netflow.R
import com.donyaep.netflow.ui.components.PulseRow
import com.donyaep.netflow.ui.components.pulseShape
import com.donyaep.netflow.ui.components.rememberCookie9Morph
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
import kotlin.math.roundToInt

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
                    ThemeMode.System -> ImageVector.vectorResource(R.drawable.ic_sync)
                    ThemeMode.Light  -> ImageVector.vectorResource(R.drawable.ic_light_mode)
                    ThemeMode.Dark   -> ImageVector.vectorResource(R.drawable.ic_dark_mode)
                }
                SettingsSection(title = "Apariencia") {
                    PulseRow(
                        onClick = { showThemeSheet = true },
                        shape = singleSegmentShape,
                        icon = themeIcon,
                        title = "Tema",
                        value = settings.themeMode.displayName,
                    )
                }
            }

            // ── 2. Monitoreo ──────────────────────────────────────────────
            item {
                SettingsSection(title = "Monitoreo") {
                    PulseRow(
                        onClick = { showSpeedUnitSheet = true },
                        shape = topSegmentShape,
                        icon = ImageVector.vectorResource(R.drawable.ic_speed),
                        title = "Unidad de velocidad",
                        value = settings.speedUnit.displayName,
                    )
                    Spacer(Modifier.height(2.dp))
                    PulseRow(
                        onClick = viewModel::toggleHideOnLockscreen,
                        shape = bottomSegmentShape,
                        icon = Icons.Rounded.Lock,
                        title = "Ocultar en la pantalla de bloqueo",
                        supporting = if (settings.hideOnLockscreen) {
                            "La velocidad no se ve con el teléfono bloqueado."
                        } else {
                            "Ahora la velocidad se ve con el teléfono bloqueado."
                        },
                        trailing = { Switch(checked = settings.hideOnLockscreen, onCheckedChange = null) },
                    )
                }
            }

            // ── 3. Límite de datos ─────────────────────────────────────────
            item {
                val limitItemShape = if (settings.dataLimitEnabled) topSegmentShape else singleSegmentShape
                val limitLabel = "${formatLimit(settings.dataLimitValue)} ${settings.dataLimitUnit.name}"
                SettingsSection(title = "Límite de datos") {
                    PulseRow(
                        onClick = viewModel::toggleDataLimitEnabled,
                        shape = limitItemShape,
                        icon = ImageVector.vectorResource(R.drawable.ic_data_usage),
                        title = "Avisarme al llegar a un límite",
                        supporting = if (settings.dataLimitEnabled) {
                            "Te aviso al llegar a $limitLabel al mes."
                        } else {
                            "Sin límite configurado."
                        },
                        trailing = { Switch(checked = settings.dataLimitEnabled, onCheckedChange = null) },
                    )
                    AnimatedVisibility(
                        visible = settings.dataLimitEnabled,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        Column {
                            Spacer(Modifier.height(2.dp))
                            PulseRow(
                                onClick = { showDataLimitSheet = true },
                                shape = bottomSegmentShape,
                                icon = ImageVector.vectorResource(R.drawable.ic_tune),
                                title = "Configurar límite",
                                value = "$limitLabel · día ${settings.billingCycleDay}",
                            )
                        }
                    }
                }
            }

            // ── 4. Más opciones ───────────────────────────────────────────
            item {
                SettingsSection(title = "Más opciones") {
                    PulseRow(
                        onClick = onOpenAdvanced,
                        shape = topSegmentShape,
                        icon = ImageVector.vectorResource(R.drawable.ic_tune),
                        title = "Permisos y batería",
                    )
                    Spacer(Modifier.height(2.dp))
                    PulseRow(
                        onClick = onOpenUpdates,
                        shape = middleSegmentShape,
                        icon = ImageVector.vectorResource(R.drawable.ic_system_update),
                        title = "Actualizaciones",
                        value = "v${BuildConfig.VERSION_NAME}",
                    )
                    Spacer(Modifier.height(2.dp))
                    PulseRow(
                        onClick = onOpenAbout,
                        shape = bottomSegmentShape,
                        icon = Icons.Rounded.Info,
                        title = "Acerca de NetFlow",
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
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp),
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
                Triple(ThemeMode.System, ImageVector.vectorResource(R.drawable.ic_sync), "Automático" to "Sigue la configuración del sistema"),
                Triple(ThemeMode.Light, ImageVector.vectorResource(R.drawable.ic_light_mode), "Claro" to "Siempre usar tema claro"),
                Triple(ThemeMode.Dark, ImageVector.vectorResource(R.drawable.ic_dark_mode), "Oscuro" to "Siempre usar tema oscuro"),
            )
            modes.forEachIndexed { index, (mode, icon, labels) ->
                val shape = when (index) {
                    0              -> topSegmentShape
                    modes.size - 1 -> bottomSegmentShape
                    else           -> middleSegmentShape
                }
                if (index > 0) Spacer(Modifier.height(2.dp))
                PulseRow(
                    onClick = { onSelect(mode) },
                    shape = shape,
                    icon = icon,
                    title = labels.first,
                    supporting = labels.second,
                    selected = current == mode,
                    container = cs.surfaceContainerHigh,
                    trailing = { if (current == mode) Icon(Icons.Rounded.Check, contentDescription = null) },
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
                PulseRow(
                    onClick = { onSelect(unit) },
                    shape = shape,
                    icon = ImageVector.vectorResource(R.drawable.ic_speed),
                    title = label,
                    supporting = subtitle,
                    selected = current == unit,
                    container = cs.surfaceContainerHigh,
                    trailing = { if (current == unit) Icon(Icons.Rounded.Check, contentDescription = null) },
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
    val dayState = rememberSliderState(
        value = settings.billingCycleDay.coerceAtMost(currentMonthDays).toFloat(),
        steps = currentMonthDays - 2,
        trackRange = 1f..currentMonthDays.toFloat(),
    )
    val localDay = dayState.value.roundToInt()

    val parsedValue: Double? = localValueText.replace(",", ".").toDoubleOrNull()?.coerceAtLeast(0.1)
    val isValid = parsedValue != null
    val cs = MaterialTheme.colorScheme

    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
                        dayState.value = 1f
                    },
                ) {
                    Icon(
                        ImageVector.vectorResource(R.drawable.ic_restore),
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                    Text("Restablecer")
                }
            }

            // Preview del valor
            Box(
                modifier = Modifier
                    .size(168.dp)
                    .align(Alignment.CenterHorizontally)
                    .pulseShape(morph = rememberCookie9Morph(), level = { 1f }, color = { cs.tertiaryContainer }),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = parsedValue?.let { formatLimit(it) } ?: "—",
                        style = MaterialTheme.typography.displaySmall.copy(fontFamily = AppCodeFontFamily),
                        fontWeight = FontWeight.Bold,
                        color = cs.onTertiaryContainer,
                        maxLines = 1,
                    )
                    Text(
                        text = "${localUnit.name}/mes",
                        style = MaterialTheme.typography.titleMedium,
                        color = cs.onTertiaryContainer,
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
                state = dayState,
                onValueChange = { dayState.value = it },
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
