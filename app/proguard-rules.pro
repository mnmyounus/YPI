# ── YPI ProGuard / R8 Rules ─────────────────────────────────────

# System-bound component — Android binds it by class name via Manifest
-keep class com.mnmyounus.ypi.PrivacyAccessibilityService { *; }

# Custom View added to WindowManager by direct class reference
-keep class com.mnmyounus.ypi.SensorIndicatorView { *; }

# Tink crypto (used internally by androidx.security:security-crypto for the
# encrypted activity log). The library ships its own consumer rules, but
# this is kept explicitly to guard against edge-case R8 stripping issues.
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# Kotlin metadata (required for Kotlin stdlib internal reflection)
-keep class kotlin.Metadata { *; }

# Strip verbose logs from release builds (performance + privacy)
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
    public static int i(...);
}
