package com.example.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.TabletAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.DeviceLayoutMode
import com.example.ui.components.LocalNeumorphicColors
import com.example.ui.components.NeumorphicCard
import com.example.ui.components.NeumorphicDialog

/**
 * Device Mode Selection Dialog (Screen 6 in Reference Image)
 * Allows switching between:
 * - Phone Mode (Optimized for mobile phones)
 * - Tablet Mode (Optimized for tablets)
 * - Auto (Detected)
 */
@Composable
fun DeviceModeDialog(
    currentMode: DeviceLayoutMode,
    onSelectMode: (DeviceLayoutMode) -> Unit,
    onDismiss: () -> Unit
) {
    NeumorphicDialog(
        title = "Device Mode",
        subtitle = "Select layout display mode",
        icon = Icons.Default.Devices,
        onDismiss = onDismiss,
        modifier = Modifier.testTag("device_mode_dialog")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Phone Mode
            DeviceModeCard(
                title = "Phone Mode",
                subtitle = "Optimized for mobile phones",
                icon = Icons.Default.Smartphone,
                isSelected = currentMode == DeviceLayoutMode.PHONE,
                showRadio = true,
                onClick = {
                    onSelectMode(DeviceLayoutMode.PHONE)
                    onDismiss()
                }
            )

            // 2. Tablet Mode
            DeviceModeCard(
                title = "Tablet Mode",
                subtitle = "Optimized for tablets",
                icon = Icons.Default.TabletAndroid,
                isSelected = currentMode == DeviceLayoutMode.TABLET,
                showRadio = true,
                onClick = {
                    onSelectMode(DeviceLayoutMode.TABLET)
                    onDismiss()
                }
            )

            // 3. Auto Detected
            DeviceModeCard(
                title = "Auto (Detected)",
                subtitle = "Auto-detect layout based on screen width",
                icon = Icons.Default.AutoAwesome,
                isSelected = currentMode == DeviceLayoutMode.AUTO,
                showRadio = false,
                showArrow = true,
                onClick = {
                    onSelectMode(DeviceLayoutMode.AUTO)
                    onDismiss()
                }
            )
        }
    }
}

@Composable
private fun DeviceModeCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    showRadio: Boolean = true,
    showArrow: Boolean = false,
    onClick: () -> Unit
) {
    val colors = LocalNeumorphicColors.current

    NeumorphicCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        elevation = if (isSelected) 1.5.dp else 2.5.dp,
        containerColor = if (isSelected) colors.primary.copy(alpha = 0.08f) else colors.surface,
        borderColor = if (isSelected) colors.primary else colors.border
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) colors.primary.copy(alpha = 0.15f) else colors.background,
                modifier = Modifier.size(40.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) colors.primary else colors.textSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) colors.primary else colors.textPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }

            if (showRadio) {
                RadioButton(
                    selected = isSelected,
                    onClick = onClick,
                    colors = RadioButtonDefaults.colors(selectedColor = colors.primary)
                )
            } else if (showArrow) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = if (isSelected) colors.primary else colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

