package com.example.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppDesignStyle
import com.example.data.model.AppLanguage
import com.example.data.model.AppThemeMode
import com.example.ui.components.LocalNeumorphicColors
import com.example.ui.components.NeumorphicButton
import com.example.ui.components.NeumorphicButtonStyle
import com.example.ui.components.NeumorphicCard
import com.example.ui.components.NeumorphicDialog

@Composable
fun ThemeSelectionDialog(
    currentTheme: AppThemeMode,
    onSelectTheme: (AppThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalNeumorphicColors.current

    NeumorphicDialog(
        title = "Select Theme",
        subtitle = "Choose app appearance",
        icon = Icons.Default.Palette,
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AppThemeMode.entries.forEach { mode ->
                val isSelected = mode == currentTheme
                NeumorphicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onSelectTheme(mode)
                            onDismiss()
                        }
                        .testTag("theme_option_${mode.name}"),
                    shape = RoundedCornerShape(12.dp),
                    containerColor = if (isSelected) colors.primary.copy(alpha = 0.12f) else colors.surface,
                    borderColor = if (isSelected) colors.primary else colors.border
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mode.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isSelected) colors.primary else colors.textPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                            )
                            Text(
                                text = mode.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                onSelectTheme(mode)
                                onDismiss()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            NeumorphicButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Close", color = colors.textSecondary)
            }
        }
    }
}

@Composable
fun DesignSelectionDialog(
    currentDesign: AppDesignStyle,
    onSelectDesign: (AppDesignStyle) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalNeumorphicColors.current

    NeumorphicDialog(
        title = "Select Design Style",
        subtitle = "Choose UI visual language",
        icon = Icons.Default.Style,
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AppDesignStyle.entries.forEach { style ->
                val isSelected = style == currentDesign
                NeumorphicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onSelectDesign(style)
                            onDismiss()
                        }
                        .testTag("design_option_${style.name}"),
                    shape = RoundedCornerShape(12.dp),
                    containerColor = if (isSelected) colors.primary.copy(alpha = 0.12f) else colors.surface,
                    borderColor = if (isSelected) colors.primary else colors.border
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = style.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isSelected) colors.primary else colors.textPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            Text(
                                text = style.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                onSelectDesign(style)
                                onDismiss()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            NeumorphicButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Close", color = colors.textSecondary)
            }
        }
    }
}

@Composable
fun LanguageSelectionDialog(
    currentLanguage: AppLanguage,
    onSelectLanguage: (AppLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalNeumorphicColors.current

    NeumorphicDialog(
        title = "Select Language",
        subtitle = "ဘာသာစကား ရွေးချယ်ပါ",
        icon = Icons.Default.Translate,
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AppLanguage.entries.forEach { lang ->
                val isSelected = lang == currentLanguage
                NeumorphicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onSelectLanguage(lang)
                            onDismiss()
                        }
                        .testTag("language_option_${lang.name}"),
                    shape = RoundedCornerShape(12.dp),
                    containerColor = if (isSelected) colors.primary.copy(alpha = 0.12f) else colors.surface,
                    borderColor = if (isSelected) colors.primary else colors.border
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = lang.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isSelected) colors.primary else colors.textPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            Text(
                                text = if (lang == AppLanguage.MYANMAR) "မြန်မာဘာသာ (Default Cloud Data preserved)" else "English (US)",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                onSelectLanguage(lang)
                                onDismiss()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            NeumorphicButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Close", color = colors.textSecondary)
            }
        }
    }
}

