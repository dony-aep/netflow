# =============================================================================
# ProGuard / R8 rules for NetFlow
# =============================================================================

# --- Flutter ---
-keep class io.flutter.app.** { *; }
-keep class io.flutter.plugin.** { *; }
-keep class io.flutter.util.** { *; }
-keep class io.flutter.view.** { *; }
-keep class io.flutter.** { *; }
-keep class io.flutter.plugins.** { *; }

# --- Flutter Foreground Task ---
-keep class com.pravera.flutter_foreground_task.** { *; }

# --- Flutter Local Notifications ---
-keep class com.dexterous.** { *; }

# --- NetFlow Traffic Stats Plugin ---
-keep class com.netflow.netflow_traffic_stats.** { *; }

# --- AndroidX / Google ---
-keep class androidx.** { *; }
-keep class com.google.android.material.** { *; }

# --- Kotlin ---
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
-dontwarn kotlin.**
-dontwarn kotlinx.**

# --- Suppress common warnings ---
-dontwarn com.google.android.play.core.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
