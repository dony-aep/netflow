# ── NetFlow ProGuard / R8 Rules ──────────────────────────────────────────────

# Preserve line numbers for readable crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Room ─────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# ── DataStore enums (ThemeMode, SpeedUnit, DataLimitUnit) ────────────────────
-keepclassmembers enum com.donyaep.netflow.data.model.** {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ── Kotlin serialization & coroutines ────────────────────────────────────────
-keepattributes *Annotation*
-dontwarn kotlinx.coroutines.**

# ── Compose ──────────────────────────────────────────────────────────────────
-dontwarn androidx.compose.**