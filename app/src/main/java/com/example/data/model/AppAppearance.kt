package com.example.data.model

enum class AppThemeMode(val displayName: String, val description: String) {
    SYSTEM("System Default", "Follow device theme"),
    LIGHT("Light Theme", "Bright interface"),
    DARK("Dark Theme", "Night mode")
}

enum class VoucherFontScale(val scale: Float, val label: String) {
    COMPACT(0.85f, "Compact"),
    STANDARD(1.0f, "Standard"),
    LARGE(1.2f, "Large")
}

enum class AppDesignStyle(val displayName: String, val description: String) {
    MODERN("Modern", "Clean Material 3 design with soft cards"),
    CLASSIC("Classic", "Traditional layout")
}

enum class AppLanguage(val displayName: String, val code: String) {
    ENGLISH("English", "en"),
    CHINESE("Chinese", "zh"),
    MYANMAR("Myanmar", "my")
}

enum class DeviceLayoutMode(val displayName: String) {
    AUTO("Auto"),
    PHONE("Phone"),
    TABLET("Tablet"),
    DESKTOP("Desktop")
}

data class AppAppearanceSettings(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val fontScale: VoucherFontScale = VoucherFontScale.STANDARD,
    val enableNeumorphism: Boolean = false,
    val primaryColorHex: String = "#0066FF",
    val designStyle: AppDesignStyle = AppDesignStyle.MODERN,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val layoutMode: DeviceLayoutMode = DeviceLayoutMode.AUTO
)
