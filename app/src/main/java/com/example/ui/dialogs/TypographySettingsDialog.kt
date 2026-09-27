package com.example.ui.dialogs

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FormatColorReset
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.RuijieVoucherItem
import com.example.printer.AppFontFamily
import com.example.printer.AppFontStyle
import com.example.printer.AppFontWeight
import com.example.printer.LetterSpacingMode
import com.example.printer.PaperConfiguration
import com.example.printer.PrintAlignment
import com.example.printer.PrintDesignSettings
import com.example.printer.PrintRenderer
import com.example.printer.TypographyPreset
import com.example.ui.components.LocalNeumorphicColors
import com.example.ui.components.NeumorphicButton
import com.example.ui.components.NeumorphicButtonStyle
import com.example.ui.components.NeumorphicCard
import com.example.ui.components.NeumorphicDialog
import com.example.ui.theme.StatusGreen

private val COLOR_SWATCHES = listOf(
    0xFF000000L to "Black",
    0xFF1F2937L to "Charcoal",
    0xFF1E3A8AL to "Navy",
    0xFF2563EBL to "Blue",
    0xFF334155L to "Slate",
    0xFF991B1BL to "Dark Red",
    0xFF166534L to "Dark Green",
    0xFF6B21A8L to "Purple"
)

enum class TypographyFieldTab(val displayName: String) {
    CODE("Voucher Code"),
    PROFILE("Profile Name"),
    PERIOD("Period"),
    QUOTA("Quota"),
    DATE_TIME("Date/Time"),
    STATUS("Status"),
    HEADER("Header")
}

@Composable
fun TypographySettingsDialog(
    initialSettings: PrintDesignSettings,
    paperConfig: PaperConfiguration,
    siteName: String,
    realVoucher: RuijieVoucherItem?,
    onSave: (PrintDesignSettings) -> Unit,
    onDismiss: () -> Unit
) {
    var settings by remember { mutableStateOf(initialSettings) }
    var selectedTab by remember { mutableStateOf(TypographyFieldTab.CODE) }

    // Live Render Voucher Bitmap
    val previewBitmap: Bitmap? = remember(settings, paperConfig, siteName, realVoucher) {
        val voucher = realVoucher ?: RuijieVoucherItem(
            voucherCode = "c4p3j2x",
            packageName = "1Hour",
            timePeriod = 60,
            quota = "1024",
            status = 1
        )
        PrintRenderer.renderVoucher(
            voucher = voucher,
            settings = settings
        )
    }

    val colors = LocalNeumorphicColors.current

    NeumorphicDialog(
        title = "Typography / Font Settings",
        subtitle = "Configure fonts, sizes, and layout styles",
        icon = Icons.Default.TextFields,
        onDismiss = onDismiss,
        modifier = Modifier.testTag("typography_settings_dialog")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                NeumorphicButton(
                    onClick = { settings = settings.resetColors() },
                    modifier = Modifier.testTag("typography_reset_colors_btn")
                ) {
                    Icon(Icons.Default.FormatColorReset, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset Colors", fontSize = 11.sp, color = colors.textSecondary)
                }
            }

            // 1. LIVE PREVIEW CARD
            NeumorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("typography_live_preview_card"),
                shape = RoundedCornerShape(12.dp),
                containerColor = colors.surface,
                borderColor = colors.border
            ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LIVE VOUCHER PREVIEW (${paperConfig.paperWidthMm}mm)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (realVoucher != null) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "REAL CLOUD VOUCHER",
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            } else {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "SAMPLE PLACEHOLDER",
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Rendered thermal voucher bitmap
                        Surface(
                            shadowElevation = 2.dp,
                            shape = RoundedCornerShape(4.dp),
                            color = Color.White,
                            modifier = Modifier.border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
                        ) {
                            if (previewBitmap != null) {
                                Image(
                                    bitmap = previewBitmap.asImageBitmap(),
                                    contentDescription = "Live Voucher Preview",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Thermal print receipt preview", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                // 2. TYPOGRAPHY PRESETS
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Typography Presets:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TypographyPreset.entries.forEach { preset ->
                            FilterChip(
                                selected = settings.activePreset == preset,
                                onClick = {
                                    settings = settings.applyPreset(preset)
                                },
                                label = { Text(preset.displayName, fontSize = 12.sp) },
                                modifier = Modifier.testTag("preset_${preset.name.lowercase()}")
                            )
                        }
                    }
                }

                // 3. GLOBAL FONT FAMILY
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Global Font Family:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppFontFamily.entries.forEach { family ->
                            FilterChip(
                                selected = settings.fontFamily == family,
                                onClick = {
                                    settings = settings.copy(
                                        fontFamily = family,
                                        activePreset = TypographyPreset.CUSTOM
                                    )
                                },
                                label = { Text(family.displayName, fontSize = 12.sp) },
                                modifier = Modifier.testTag("font_family_${family.name.lowercase()}")
                            )
                        }
                    }
                }

                HorizontalDivider()

                // 4. INDEPENDENT FIELD CONFIGURATION TABS
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Field Typography & Styling:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    ScrollableTabRow(
                        selectedTabIndex = selectedTab.ordinal,
                        edgePadding = 4.dp
                    ) {
                        TypographyFieldTab.entries.forEach { tab ->
                            Tab(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                text = { Text(tab.displayName, fontSize = 12.sp, maxLines = 1) },
                                modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Active Tab Controller
                    when (selectedTab) {
                        TypographyFieldTab.CODE -> FieldTypographyControl(
                            label = "Voucher Code",
                            fontSize = settings.codeFontSize,
                            fontWeight = settings.codeFontWeight,
                            fontStyle = settings.codeFontStyle,
                            alignment = settings.codeAlignment,
                            colorLong = settings.codeColor,
                            onUpdate = { size, weight, style, align, color ->
                                settings = settings.copy(
                                    codeFontSize = size,
                                    codeFontWeight = weight,
                                    codeBold = (weight == AppFontWeight.BOLD),
                                    codeFontStyle = style,
                                    codeAlignment = align,
                                    codeColor = color,
                                    activePreset = TypographyPreset.CUSTOM
                                )
                            }
                        )
                        TypographyFieldTab.PROFILE -> FieldTypographyControl(
                            label = "Profile Name",
                            fontSize = settings.profileNameFontSize,
                            fontWeight = settings.profileNameFontWeight,
                            fontStyle = settings.profileNameFontStyle,
                            alignment = settings.profileNameAlignment,
                            colorLong = settings.profileNameColor,
                            onUpdate = { size, weight, style, align, color ->
                                settings = settings.copy(
                                    profileNameFontSize = size,
                                    profileNameFontWeight = weight,
                                    profileNameBold = (weight == AppFontWeight.BOLD),
                                    profileNameFontStyle = style,
                                    profileNameAlignment = align,
                                    profileNameColor = color,
                                    activePreset = TypographyPreset.CUSTOM
                                )
                            }
                        )
                        TypographyFieldTab.PERIOD -> FieldTypographyControl(
                            label = "Period",
                            fontSize = settings.periodFontSize,
                            fontWeight = settings.periodFontWeight,
                            fontStyle = settings.periodFontStyle,
                            alignment = settings.periodAlignment,
                            colorLong = settings.periodColor,
                            onUpdate = { size, weight, style, align, color ->
                                settings = settings.copy(
                                    periodFontSize = size,
                                    periodFontWeight = weight,
                                    periodBold = (weight == AppFontWeight.BOLD),
                                    periodFontStyle = style,
                                    periodAlignment = align,
                                    periodColor = color,
                                    activePreset = TypographyPreset.CUSTOM
                                )
                            }
                        )
                        TypographyFieldTab.QUOTA -> FieldTypographyControl(
                            label = "Quota",
                            fontSize = settings.quotaFontSize,
                            fontWeight = settings.quotaFontWeight,
                            fontStyle = settings.quotaFontStyle,
                            alignment = settings.quotaAlignment,
                            colorLong = settings.quotaColor,
                            onUpdate = { size, weight, style, align, color ->
                                settings = settings.copy(
                                    quotaFontSize = size,
                                    quotaFontWeight = weight,
                                    quotaBold = (weight == AppFontWeight.BOLD),
                                    quotaFontStyle = style,
                                    quotaAlignment = align,
                                    quotaColor = color,
                                    activePreset = TypographyPreset.CUSTOM
                                )
                            }
                        )
                        TypographyFieldTab.DATE_TIME -> FieldTypographyControl(
                            label = "Print Date / Time",
                            fontSize = settings.printDateTimeFontSize,
                            fontWeight = settings.printDateTimeFontWeight,
                            fontStyle = settings.printDateTimeFontStyle,
                            alignment = settings.printDateTimeAlignment,
                            colorLong = settings.printDateTimeColor,
                            onUpdate = { size, weight, style, align, color ->
                                settings = settings.copy(
                                    printDateTimeFontSize = size,
                                    printDateTimeFontWeight = weight,
                                    printDateTimeBold = (weight == AppFontWeight.BOLD),
                                    printDateTimeFontStyle = style,
                                    printDateTimeAlignment = align,
                                    printDateTimeColor = color,
                                    activePreset = TypographyPreset.CUSTOM
                                )
                            }
                        )
                        TypographyFieldTab.STATUS -> FieldTypographyControl(
                            label = "Voucher Status Line",
                            fontSize = settings.statusFontSize,
                            fontWeight = settings.statusFontWeight,
                            fontStyle = settings.statusFontStyle,
                            alignment = settings.statusAlignment,
                            colorLong = settings.statusColor,
                            onUpdate = { size, weight, style, align, color ->
                                settings = settings.copy(
                                    statusFontSize = size,
                                    statusFontWeight = weight,
                                    statusBold = (weight == AppFontWeight.BOLD),
                                    statusFontStyle = style,
                                    statusAlignment = align,
                                    statusColor = color,
                                    activePreset = TypographyPreset.CUSTOM
                                )
                            }
                        )
                        TypographyFieldTab.HEADER -> FieldTypographyControl(
                            label = "Header / Brand Name",
                            fontSize = settings.headerFontSize,
                            fontWeight = settings.headerFontWeight,
                            fontStyle = settings.headerFontStyle,
                            alignment = settings.headerAlignment,
                            colorLong = settings.headerColor,
                            onUpdate = { size, weight, style, align, color ->
                                settings = settings.copy(
                                    headerFontSize = size,
                                    headerFontWeight = weight,
                                    headerBold = (weight == AppFontWeight.BOLD),
                                    headerFontStyle = style,
                                    headerAlignment = align,
                                    headerColor = color,
                                    activePreset = TypographyPreset.CUSTOM
                                )
                            }
                        )
                    }
                }

                HorizontalDivider()

                // 5. LETTER SPACING CONTROLS
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Letter Spacing (Voucher Code & Fields):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LetterSpacingMode.entries.forEach { mode ->
                            FilterChip(
                                selected = settings.letterSpacingMode == mode,
                                onClick = {
                                    settings = settings.copy(
                                        letterSpacingMode = mode,
                                        activePreset = TypographyPreset.CUSTOM
                                    )
                                },
                                label = { Text(mode.displayName, fontSize = 12.sp) },
                                modifier = Modifier.testTag("spacing_mode_${mode.name.lowercase()}")
                            )
                        }
                    }

                    if (settings.activePreset == TypographyPreset.CUSTOM) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Slider(
                                value = settings.customLetterSpacing,
                                onValueChange = {
                                    settings = settings.copy(
                                        customLetterSpacing = it,
                                        activePreset = TypographyPreset.CUSTOM
                                    )
                                },
                                valueRange = -0.06f..0.22f,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = String.format("%.2f", settings.customLetterSpacing),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 6. EXTRA LINE SPACING (Independent control)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Extra Line Spacing:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "+${settings.lineSpacingExtra.toInt()} px",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = settings.lineSpacingExtra,
                        onValueChange = {
                            settings = settings.copy(
                                lineSpacingExtra = it,
                                activePreset = TypographyPreset.CUSTOM
                            )
                        },
                        valueRange = 0f..20f,
                        steps = 19,
                        modifier = Modifier.fillMaxWidth().testTag("line_spacing_extra_slider")
                    )
                }

                HorizontalDivider()

                // 7. TEXT SHADOW (Preview & Safe Output)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Text Shadow:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Rendered in preview; thermal print output remains crisp and safe.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.textShadowEnabled,
                            onCheckedChange = {
                                settings = settings.copy(
                                    textShadowEnabled = it,
                                    activePreset = TypographyPreset.CUSTOM
                                )
                            },
                            modifier = Modifier.testTag("text_shadow_switch")
                        )
                    }

                    if (settings.textShadowEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Blur:", fontSize = 12.sp, modifier = Modifier.width(45.dp))
                            Slider(
                                value = settings.shadowBlur,
                                onValueChange = { settings = settings.copy(shadowBlur = it) },
                                valueRange = 1f..10f,
                                modifier = Modifier.weight(1f)
                            )
                            Text("${settings.shadowBlur.toInt()}px", fontSize = 12.sp)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Offset:", fontSize = 12.sp, modifier = Modifier.width(45.dp))
                            Slider(
                                value = settings.shadowOffsetX,
                                onValueChange = {
                                    settings = settings.copy(shadowOffsetX = it, shadowOffsetY = it)
                                },
                                valueRange = 0f..8f,
                                modifier = Modifier.weight(1f)
                            )
                            Text("${settings.shadowOffsetX.toInt()}px", fontSize = 12.sp)
                        }
                    }
                }

                // 8. TEXT OUTLINE / STROKE
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Text Outline / Stroke:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Accents text edges cleanly.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.textOutlineEnabled,
                            onCheckedChange = {
                                settings = settings.copy(
                                    textOutlineEnabled = it,
                                    activePreset = TypographyPreset.CUSTOM
                                )
                            },
                            modifier = Modifier.testTag("text_outline_switch")
                        )
                    }

                    if (settings.textOutlineEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Stroke:", fontSize = 12.sp, modifier = Modifier.width(50.dp))
                            Slider(
                                value = settings.outlineWidth,
                                onValueChange = { settings = settings.copy(outlineWidth = it) },
                                valueRange = 1f..4f,
                                steps = 2,
                                modifier = Modifier.weight(1f)
                            )
                            Text("${settings.outlineWidth.toInt()}px", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NeumorphicButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).testTag("typography_cancel_btn")
                ) {
                    Text("Cancel", color = colors.textSecondary)
                }
                NeumorphicButton(
                    onClick = { onSave(settings) },
                    primary = true,
                    modifier = Modifier.weight(1.3f).testTag("typography_save_btn")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Typography", color = Color.White)
                }
            }
        }
    }
}

/**
 * Reusable control section for a specific voucher field
 */
@Composable
private fun FieldTypographyControl(
    label: String,
    fontSize: Float,
    fontWeight: AppFontWeight,
    fontStyle: AppFontStyle,
    alignment: PrintAlignment,
    colorLong: Long,
    onUpdate: (Float, AppFontWeight, AppFontStyle, PrintAlignment, Long) -> Unit
) {
    val colors = LocalNeumorphicColors.current

    NeumorphicCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        containerColor = colors.surface,
        borderColor = colors.border
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "$label Configuration",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = colors.primary
            )

            // 1. Font Size: Presets + Stepper / Custom Value Input
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Font Size: ${fontSize.toInt()} pt",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                var isCustomInputOpen by remember { mutableStateOf(false) }
                var customInputText by remember(fontSize) { mutableStateOf(fontSize.toInt().toString()) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = !isCustomInputOpen && fontSize == 20f,
                        onClick = {
                            isCustomInputOpen = false
                            onUpdate(20f, fontWeight, fontStyle, alignment, colorLong)
                        },
                        label = { Text("Small (20)", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = !isCustomInputOpen && fontSize == 26f,
                        onClick = {
                            isCustomInputOpen = false
                            onUpdate(26f, fontWeight, fontStyle, alignment, colorLong)
                        },
                        label = { Text("Medium (26)", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = !isCustomInputOpen && fontSize == 32f,
                        onClick = {
                            isCustomInputOpen = false
                            onUpdate(32f, fontWeight, fontStyle, alignment, colorLong)
                        },
                        label = { Text("Large (32)", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = isCustomInputOpen || (fontSize != 20f && fontSize != 26f && fontSize != 32f),
                        onClick = { isCustomInputOpen = !isCustomInputOpen },
                        label = { Text("Custom", fontSize = 11.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Stepper buttons
                    IconButton(
                        onClick = {
                            val newSize = (fontSize - 2f).coerceIn(14f, 48f)
                            onUpdate(newSize, fontWeight, fontStyle, alignment, colorLong)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease size")
                    }

                    if (isCustomInputOpen) {
                        OutlinedTextField(
                            value = customInputText,
                            onValueChange = { input ->
                                val filtered = input.filter { it.isDigit() }.take(2)
                                customInputText = filtered
                                val parsed = filtered.toFloatOrNull()
                                if (parsed != null && parsed in 12f..60f) {
                                    onUpdate(parsed, fontWeight, fontStyle, alignment, colorLong)
                                }
                            },
                            modifier = Modifier
                                .width(70.dp)
                                .height(48.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text("pt", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { isCustomInputOpen = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "${fontSize.toInt()} pt",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            val newSize = (fontSize + 2f).coerceIn(14f, 48f)
                            onUpdate(newSize, fontWeight, fontStyle, alignment, colorLong)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase size")
                    }
                }
            }

            // 2. Font Weight
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Font Weight:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AppFontWeight.entries.forEach { weight ->
                        FilterChip(
                            selected = fontWeight == weight,
                            onClick = { onUpdate(fontSize, weight, fontStyle, alignment, colorLong) },
                            label = { Text(weight.displayName, fontSize = 11.sp) }
                        )
                    }
                }
            }

            // 3. Font Style (Normal vs Italic)
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Font Style:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = fontStyle == AppFontStyle.NORMAL,
                        onClick = { onUpdate(fontSize, fontWeight, AppFontStyle.NORMAL, alignment, colorLong) },
                        label = { Text("Normal", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = fontStyle == AppFontStyle.ITALIC,
                        onClick = { onUpdate(fontSize, fontWeight, AppFontStyle.ITALIC, alignment, colorLong) },
                        label = { Text("Italic", fontSize = 11.sp) }
                    )
                }
            }

            // 4. Alignment
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Alignment:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PrintAlignment.entries.forEach { align ->
                        FilterChip(
                            selected = alignment == align,
                            onClick = { onUpdate(fontSize, fontWeight, fontStyle, align, colorLong) },
                            label = { Text(align.displayName, fontSize = 11.sp) }
                        )
                    }
                }
            }

            // 5. Font Color Palette
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Text Color:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    COLOR_SWATCHES.forEach { (colorVal, name) ->
                        val isSelected = colorLong == colorVal
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(colorVal))
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                    shape = CircleShape
                                )
                                .clickable {
                                    onUpdate(fontSize, fontWeight, fontStyle, alignment, colorVal)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (colorVal == 0xFF000000L || colorVal == 0xFF1E3A8AL) Color.White else Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScrollableTabRow(
    selectedTabIndex: Int,
    edgePadding: androidx.compose.ui.unit.Dp = 0.dp,
    tabs: @Composable () -> Unit
) {
    androidx.compose.material3.ScrollableTabRow(
        selectedTabIndex = selectedTabIndex,
        edgePadding = edgePadding,
        modifier = Modifier.fillMaxWidth(),
        tabs = tabs
    )
}
