package com.example.printer

enum class AppFontFamily(val displayName: String) {
    MONOSPACE("Monospace"),
    SANS_SERIF("Sans Serif"),
    SERIF("Serif")
}

enum class AppFontStyle(val displayName: String) {
    NORMAL("Normal"),
    ITALIC("Italic")
}

enum class AppFontWeight(val displayName: String) {
    NORMAL("Normal"),
    BOLD("Bold")
}

enum class LetterSpacingMode(val displayName: String) {
    NORMAL("Normal"),
    WIDE("Wide"),
    EXTRA_WIDE("Extra Wide"),
    CUSTOM("Custom")
}

enum class PrintAlignment(val displayName: String) {
    LEFT("Left"),
    CENTER("Center"),
    RIGHT("Right")
}

enum class TypographyPreset(val displayName: String) {
    DEFAULT("Default"),
    COMPACT("Compact"),
    LARGE("Large"),
    CUSTOM("Custom")
}

data class PrintDesignSettings(
    val headerTitle: String = "GUEST WI-FI ACCESS",
    val subHeader: String = "Ruijie Cloud Hotspot",
    val showQrCode: Boolean = true,
    val showBorder: Boolean = true,
    val showFooter: Boolean = true,
    val footerText: String = "Thank you for visiting! Enjoy your stay.",
    val showSupportContact: Boolean = true,
    val supportContact: String = "Support: Front Desk ext 100",
    val paperConfig: PaperConfiguration = PaperConfiguration(),
    val fontFamily: AppFontFamily = AppFontFamily.MONOSPACE,
    val activePreset: TypographyPreset = TypographyPreset.DEFAULT,
    val letterSpacingMode: LetterSpacingMode = LetterSpacingMode.NORMAL,
    val customLetterSpacing: Float = 0f,
    val lineSpacingExtra: Float = 0f,
    val textShadowEnabled: Boolean = false,
    val shadowBlur: Float = 0f,
    val shadowOffsetX: Float = 0f,
    val shadowOffsetY: Float = 0f,
    val textOutlineEnabled: Boolean = false,
    val outlineWidth: Float = 0f,
    val codeFontSize: Float = 16f,
    val codeFontWeight: AppFontWeight = AppFontWeight.BOLD,
    val codeBold: Boolean = true,
    val codeFontStyle: AppFontStyle = AppFontStyle.NORMAL,
    val codeAlignment: PrintAlignment = PrintAlignment.CENTER,
    val codeColor: Long = 0xFF000000L,
    val profileNameFontSize: Float = 12f,
    val profileNameFontWeight: AppFontWeight = AppFontWeight.NORMAL,
    val profileNameBold: Boolean = false,
    val profileNameFontStyle: AppFontStyle = AppFontStyle.NORMAL,
    val profileNameAlignment: PrintAlignment = PrintAlignment.LEFT,
    val profileNameColor: Long = 0xFF000000L,
    val periodFontSize: Float = 12f,
    val periodFontWeight: AppFontWeight = AppFontWeight.NORMAL,
    val periodBold: Boolean = false,
    val periodFontStyle: AppFontStyle = AppFontStyle.NORMAL,
    val periodAlignment: PrintAlignment = PrintAlignment.LEFT,
    val periodColor: Long = 0xFF000000L,
    val quotaFontSize: Float = 12f,
    val quotaFontWeight: AppFontWeight = AppFontWeight.NORMAL,
    val quotaBold: Boolean = false,
    val quotaFontStyle: AppFontStyle = AppFontStyle.NORMAL,
    val quotaAlignment: PrintAlignment = PrintAlignment.LEFT,
    val quotaColor: Long = 0xFF000000L,
    val printDateTimeFontSize: Float = 10f,
    val printDateTimeFontWeight: AppFontWeight = AppFontWeight.NORMAL,
    val printDateTimeBold: Boolean = false,
    val printDateTimeFontStyle: AppFontStyle = AppFontStyle.NORMAL,
    val printDateTimeAlignment: PrintAlignment = PrintAlignment.LEFT,
    val printDateTimeColor: Long = 0xFF000000L,
    val statusFontSize: Float = 12f,
    val statusFontWeight: AppFontWeight = AppFontWeight.NORMAL,
    val statusBold: Boolean = false,
    val statusFontStyle: AppFontStyle = AppFontStyle.NORMAL,
    val statusAlignment: PrintAlignment = PrintAlignment.LEFT,
    val statusColor: Long = 0xFF000000L,
    val headerFontSize: Float = 14f,
    val headerFontWeight: AppFontWeight = AppFontWeight.BOLD,
    val headerBold: Boolean = true,
    val headerFontStyle: AppFontStyle = AppFontStyle.NORMAL,
    val headerAlignment: PrintAlignment = PrintAlignment.CENTER,
    val headerColor: Long = 0xFF000000L
) {
    fun resetColors(): PrintDesignSettings = copy(
        codeColor = 0xFF000000L,
        profileNameColor = 0xFF000000L,
        periodColor = 0xFF000000L,
        quotaColor = 0xFF000000L,
        printDateTimeColor = 0xFF000000L,
        statusColor = 0xFF000000L,
        headerColor = 0xFF000000L
    )

    fun applyPreset(preset: TypographyPreset): PrintDesignSettings = copy(activePreset = preset)
}
