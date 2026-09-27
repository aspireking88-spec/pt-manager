package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class NeumorphicColors(
    val background: Color = Color(0xFFF5F7FA),
    val surface: Color = Color.White,
    val surfaceRaised: Color = Color.White,
    val primary: Color = Color(0xFF3B82F6),
    val border: Color = Color(0xFFE2E8F0),
    val textPrimary: Color = Color(0xFF1E293B),
    val textSecondary: Color = Color(0xFF64748B),
    val lightShadow: Color = Color.White,
    val darkShadow: Color = Color(0xFFCBD5E1)
) {
    val secondaryTextColor: Color get() = textSecondary
    val primaryTextColor: Color get() = textPrimary
    val accentRed: Color get() = Color(0xFFEF4444)
    val textMuted: Color get() = textSecondary
    val cardBackground: Color get() = surface
    val inset: Color get() = Color(0xFFF1F5F9)
    val SURFACE: Color get() = surface
    val PRIMARY: Color get() = primary
}

val LocalNeumorphicColors = staticCompositionLocalOf { NeumorphicColors() }

val Dp.Companion.inset: Dp get() = 8.dp

enum class NeumorphicButtonStyle {
    Filled, Outlined, Text, PRIMARY, SURFACE, SECONDARY, DANGER, TONAL, ELEVATED
}

val Color.Companion.SURFACE: Color @Composable get() = MaterialTheme.colorScheme.surface
val Color.Companion.PRIMARY: Color @Composable get() = MaterialTheme.colorScheme.primary
val Color.Companion.StatusGreen: Color get() = Color(0xFF10B981)
val Color.Companion.StatusGreenContainer: Color get() = Color(0xFFD1FAE5)
val Color.Companion.accentRed: Color get() = Color(0xFFEF4444)

@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    elevation: Dp = 3.dp,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation, RoundedCornerShape(cornerRadius)),
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
fun NeumorphicCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    shape: Shape = RoundedCornerShape(cornerRadius),
    elevation: Dp = 3.dp,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    backgroundColor: Color = containerColor,
    borderColor: Color = Color.Transparent,
    border: BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit
) {
    val actualContainerColor = if (containerColor != Color.Unspecified) containerColor else backgroundColor
    val actualBorder = border ?: if (borderColor != Color.Transparent) BorderStroke(1.dp, borderColor) else null

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier
                .fillMaxWidth()
                .shadow(elevation, shape),
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = actualContainerColor),
            border = actualBorder
        ) {
            Box(modifier = Modifier.padding(contentPadding)) {
                content()
            }
        }
    } else {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .shadow(elevation, shape),
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = actualContainerColor),
            border = actualBorder
        ) {
            Box(modifier = Modifier.padding(contentPadding)) {
                content()
            }
        }
    }
}

@Composable
fun NeumorphicButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: NeumorphicButtonStyle = NeumorphicButtonStyle.Filled,
    primary: Boolean = false,
    containerColor: Color = Color.Unspecified,
    backgroundColor: Color = containerColor,
    shape: Shape = RoundedCornerShape(12.dp),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    val actualColors = if (containerColor != Color.Unspecified) {
        ButtonDefaults.buttonColors(containerColor = containerColor)
    } else if (backgroundColor != Color.Unspecified) {
        ButtonDefaults.buttonColors(containerColor = backgroundColor)
    } else if (primary || style == NeumorphicButtonStyle.PRIMARY) {
        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    } else {
        ButtonDefaults.buttonColors()
    }
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = actualColors,
        contentPadding = contentPadding,
        content = content
    )
}

@Composable
fun NeumorphicOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(12.dp),
    borderColor: Color = MaterialTheme.colorScheme.outline,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        border = BorderStroke(1.dp, borderColor),
        contentPadding = contentPadding,
        content = content
    )
}

@Composable
fun NeumorphicIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    elevation: Dp = 0.dp,
    shape: Shape = androidx.compose.foundation.shape.CircleShape,
    borderColor: Color = Color.Transparent,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    icon: ImageVector? = null,
    contentDescription: String? = null,
    tint: Color = Color.Unspecified,
    size: Dp = 24.dp,
    content: @Composable (() -> Unit)? = null
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled
    ) {
        if (content != null) {
            content()
        } else if (icon != null) {
            Icon(icon, contentDescription = contentDescription, tint = tint)
        }
    }
}

@Composable
fun NeumorphicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    labelText: String? = null,
    placeholder: (@Composable () -> Unit)? = null,
    placeholderText: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    isError: Boolean = false,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None
) {
    val actualLabel: (@Composable () -> Unit)? = label ?: if (!labelText.isNullOrBlank()) {
        { Text(labelText) }
    } else null

    val actualPlaceholder: (@Composable () -> Unit)? = placeholder ?: if (!placeholderText.isNullOrBlank()) {
        { Text(placeholderText) }
    } else null

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = actualLabel,
        placeholder = actualPlaceholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        singleLine = singleLine,
        isError = isError,
        visualTransformation = visualTransformation
    )
}

@Composable
fun NeumorphicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String,
    labelText: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    isError: Boolean = false,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None
) {
    NeumorphicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = null,
        placeholderText = placeholder,
        labelText = labelText,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        singleLine = singleLine,
        isError = isError,
        visualTransformation = visualTransformation
    )
}

@Composable
fun NeumorphicInsetBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    borderColor: Color = Color.Transparent,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(contentPadding)
    ) {
        content()
    }
}

@Composable
fun NeumorphicStatusBadge(
    status: com.example.data.model.VoucherStatus,
    modifier: Modifier = Modifier
) {
    val (bg, txt) = when (status) {
        com.example.data.model.VoucherStatus.NOT_USED -> Color(0xFFD1FAE5) to Color(0xFF059669)
        com.example.data.model.VoucherStatus.USED -> Color(0xFFDBEAFE) to Color(0xFF2563EB)
        com.example.data.model.VoucherStatus.EXPIRED -> Color(0xFFFEE2E2) to Color(0xFFDC2626)
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Text(
            text = status.displayName,
            color = txt,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeumorphicFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String? = null,
    count: Int? = null,
    modifier: Modifier = Modifier,
    labelComposable: (@Composable () -> Unit)? = null
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            if (labelComposable != null) {
                labelComposable()
            } else if (label != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(label)
                    if (count != null && count > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "($count)",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeumorphicTopBar(
    title: String,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    navigationIcon: (@Composable () -> Unit)? = null
) {
    TopAppBar(
        title = {
            Column {
                Text(title)
                if (!subtitle.isNullOrBlank()) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        actions = actions,
        navigationIcon = {
            navigationIcon?.invoke()
        }
    )
}

@Composable
fun NeumorphicDialog(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    titleIcon: ImageVector? = null,
    onDismiss: () -> Unit = {},
    onDismissRequest: (() -> Unit)? = null,
    buttons: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val dismissAction = onDismissRequest ?: onDismiss
    AlertDialog(
        onDismissRequest = dismissAction,
        modifier = modifier,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val actIcon = icon ?: titleIcon
                if (actIcon != null) {
                    Icon(actIcon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Column {
                    Text(text = title, style = MaterialTheme.typography.titleLarge)
                    if (!subtitle.isNullOrBlank()) {
                        Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        text = content,
        confirmButton = {
            if (buttons != null) {
                buttons()
            }
        }
    )
}
