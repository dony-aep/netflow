package com.donyaep.netflow.data.model

enum class ThemeMode {
    System,
    Light,
    Dark,
}

enum class SpeedUnit {
    BitsPerSecond,
    BytesPerSecond,
}

enum class DataLimitUnit {
    KB,
    MB,
    GB,
}

val ThemeMode.displayName: String
    get() = when (this) {
        ThemeMode.System -> "Sistema"
        ThemeMode.Light -> "Claro"
        ThemeMode.Dark -> "Oscuro"
    }

val SpeedUnit.displayName: String
    get() = when (this) {
        SpeedUnit.BitsPerSecond -> "Bits/s"
        SpeedUnit.BytesPerSecond -> "Bytes/s"
    }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val speedUnit: SpeedUnit = SpeedUnit.BytesPerSecond,
    val hideOnLockscreen: Boolean = false,
    val restoreMonitoringAfterBoot: Boolean = false,
    val dataLimitEnabled: Boolean = false,
    val dataLimitValue: Double = 5.0,
    val dataLimitUnit: DataLimitUnit = DataLimitUnit.GB,
    val billingCycleDay: Int = 1,
)