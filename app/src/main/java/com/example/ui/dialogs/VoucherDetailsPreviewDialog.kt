package com.example.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.widget.Toast
import com.example.data.diagnostics.VoucherInspectorCollector
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.platform.LocalContext
import com.example.printer.PrintDesignSettings
import com.example.data.model.RuijieAccount
import com.example.data.model.RuijieProject
import com.example.data.model.RuijieVoucherItem
import com.example.data.model.VoucherStatus
import com.example.ui.components.LocalNeumorphicColors
import com.example.ui.components.NeumorphicButton
import com.example.ui.components.NeumorphicCard
import com.example.ui.components.NeumorphicIconButton
import com.example.ui.components.NeumorphicInsetBox
import com.example.ui.components.NeumorphicOutlinedButton
import com.example.ui.components.NeumorphicStatusBadge
import com.example.ui.theme.RuijieBlue
import com.example.ui.theme.StatusBlue
import com.example.ui.theme.StatusBlueContainer
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusGreenContainer
import com.example.ui.theme.StatusRed
import com.example.ui.theme.StatusRedContainer

/**
 * Voucher Preview Dialog:
 * - Redesigned with WiFi Voucher Scanner Neumorphic Design System
 * - Soft raised surfaces, inset code box, tactile buttons
 * - Realtime accurate fields: Quota, Current Data Usage, Remaining Data, Period, Expiry, Status, MAC Address
 * - Activation start time detection and display
 * - Separate MAC Address display and dedicated Unbind Action Button
 * - Thermal Print Voucher support
 */
@Composable
fun VoucherDetailsPreviewDialog(
    voucher: RuijieVoucherItem,
    settings: PrintDesignSettings = PrintDesignSettings(),
    isRefreshingExternal: Boolean = false,
    refreshMessage: String? = null,
    selectedProject: RuijieProject? = null,
    activeAccount: RuijieAccount? = null,
    onCopyCode: (String) -> Unit,
    onPrint: (RuijieVoucherItem) -> Unit,
    onUnbind: ((RuijieVoucherItem) -> Unit)? = null,
    onRefresh: ((RuijieVoucherItem) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val colors = LocalNeumorphicColors.current
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val stableKey = voucher.uuid?.ifBlank { null } ?: voucher.voucherCode?.ifBlank { null } ?: voucher.codeNo?.ifBlank { null } ?: voucher.code?.ifBlank { null } ?: voucher.effectiveCode
    var currentVoucher by remember(stableKey) { mutableStateOf(voucher) }
    var isUnbinding by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var actionMessage by remember { mutableStateOf<String?>(null) }

    // Keep currentVoucher updated when refreshed with new data for the same voucher
    LaunchedEffect(voucher) {
        val currentIdentity = currentVoucher.uuid?.ifBlank { null } ?: currentVoucher.voucherCode?.ifBlank { null } ?: currentVoucher.codeNo?.ifBlank { null } ?: currentVoucher.effectiveCode
        val newIdentity = voucher.uuid?.ifBlank { null } ?: voucher.voucherCode?.ifBlank { null } ?: voucher.codeNo?.ifBlank { null } ?: voucher.effectiveCode
        if (currentIdentity == newIdentity) {
            currentVoucher = voucher
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 480.dp)
                .padding(vertical = 16.dp)
                .testTag("voucher_details_preview_dialog"),
            contentAlignment = Alignment.Center
        ) {
            NeumorphicCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = 6.dp,
                containerColor = colors.surfaceRaised,
                borderColor = colors.border
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Dialog Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            NeumorphicIconButton(
                                onClick = onDismiss,
                                icon = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                size = 36.dp,
                                elevation = 1.dp
                            )
                            Text(
                                text = "Voucher Preview",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (onRefresh != null) {
                                val canRefresh = !isRefreshing && !isRefreshingExternal
                                NeumorphicIconButton(
                                    onClick = {
                                        if (canRefresh) {
                                            isRefreshing = true
                                            onRefresh(currentVoucher)
                                            isRefreshing = false
                                        }
                                    },
                                    icon = Icons.Default.Refresh,
                                    contentDescription = "Refresh Realtime Data",
                                    tint = colors.primary,
                                    size = 36.dp,
                                    elevation = 1.dp,
                                    enabled = canRefresh
                                )
                            }

                            NeumorphicIconButton(
                                onClick = {
                                    onPrint(currentVoucher)
                                    onDismiss()
                                },
                                icon = Icons.Default.Print,
                                contentDescription = "Print Voucher",
                                tint = colors.primary,
                                size = 36.dp,
                                elevation = 1.dp
                            )
                        }
                    }

                    // Warning / Status Banner (e.g. if voucher no longer exists after refresh)
                    if (!refreshMessage.isNullOrBlank()) {
                        Surface(
                            color = StatusRedContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("preview_refresh_warning_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = StatusRed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = refreshMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StatusRed,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Scrollable Voucher Details
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(weight = 1f, fill = false)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 1. Voucher Code Sunken Inset Box
                        NeumorphicInsetBox(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "VOUCHER CODE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.primary,
                                    letterSpacing = 1.5.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = currentVoucher.effectiveCode,
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 2.sp
                                    ),
                                    color = colors.textPrimary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "UUID: ${currentVoucher.effectiveUuid}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = colors.textSecondary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                NeumorphicOutlinedButton(
                                    onClick = { onCopyCode(currentVoucher.effectiveCode) },
                                    modifier = Modifier.height(34.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy",
                                        modifier = Modifier.size(13.dp),
                                        tint = colors.primary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Copy Code",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.primary
                                    )
                                }
                            }
                        }

                        // 2. Primary Voucher Details Card with required fields
                        NeumorphicCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            elevation = 1.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Voucher Details",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.primary
                                    )
                                    NeumorphicStatusBadge(status = currentVoucher.normalizedStatus)
                                }

                                HorizontalDivider(thickness = 0.5.dp, color = colors.border.copy(alpha = 0.4f))

                                DetailRowWithIcon(
                                    icon = Icons.Default.Key,
                                    label = "Voucher Code",
                                    value = currentVoucher.effectiveCode.ifBlank { "—" }
                                )

                                HorizontalDivider(thickness = 0.5.dp, color = colors.border.copy(alpha = 0.4f))

                                DetailRowWithIcon(
                                    icon = Icons.Default.CheckCircle,
                                    label = "Status",
                                    value = currentVoucher.normalizedStatus.displayName
                                )

                                HorizontalDivider(thickness = 0.5.dp, color = colors.border.copy(alpha = 0.4f))

                                DetailRowWithIcon(
                                    icon = Icons.Default.Info,
                                    label = "User Group",
                                    value = currentVoucher.effectiveUserGroupName
                                )

                                HorizontalDivider(thickness = 0.5.dp, color = colors.border.copy(alpha = 0.4f))

                                DetailRowWithIcon(
                                    icon = Icons.Default.Key,
                                    label = "Package",
                                    value = currentVoucher.effectiveProfile
                                )

                                HorizontalDivider(thickness = 0.5.dp, color = colors.border.copy(alpha = 0.4f))

                                DetailRowWithIcon(
                                    icon = Icons.Default.AccessTime,
                                    label = "Duration",
                                    value = currentVoucher.effectivePeriod
                                )

                                HorizontalDivider(thickness = 0.5.dp, color = colors.border.copy(alpha = 0.4f))

                                DetailRowWithIcon(
                                    icon = Icons.Default.CalendarToday,
                                    label = "Create Time",
                                    value = currentVoucher.formattedCreateTime
                                )

                                HorizontalDivider(thickness = 0.5.dp, color = colors.border.copy(alpha = 0.4f))

                                DetailRowWithIcon(
                                    icon = Icons.Default.PlayArrow,
                                    label = "Start Time",
                                    value = currentVoucher.formattedStartTimeDisplay
                                )

                                HorizontalDivider(thickness = 0.5.dp, color = colors.border.copy(alpha = 0.4f))

                                DetailRowWithIcon(
                                    icon = Icons.Default.HourglassEmpty,
                                    label = "Expired Time",
                                    value = currentVoucher.formattedExpiryTimeDisplay
                                )

                                HorizontalDivider(thickness = 0.5.dp, color = colors.border.copy(alpha = 0.4f))

                                DetailRowWithIcon(
                                    icon = Icons.Default.DataUsage,
                                    label = "Total Quota",
                                    value = currentVoucher.effectiveQuotaDisplay
                                )

                                HorizontalDivider(thickness = 0.5.dp, color = colors.border.copy(alpha = 0.4f))

                                DetailRowWithIcon(
                                    icon = Icons.Default.PieChart,
                                    label = "Use Quota",
                                    value = currentVoucher.effectiveUsedQuotaDisplay
                                )

                                HorizontalDivider(thickness = 0.5.dp, color = colors.border.copy(alpha = 0.4f))

                                DetailRowWithIcon(
                                    icon = Icons.Default.DataUsage,
                                    label = "Remaining Data",
                                    value = currentVoucher.effectiveRemainingDataDisplay
                                )
                            }
                        }

                        // 3. Additional Network & Traffic Details Card
                        NeumorphicCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            elevation = 1.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Network & Traffic Details",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )

                                HorizontalDivider(thickness = 0.5.dp, color = colors.border.copy(alpha = 0.4f))

                                DetailRowWithIcon(
                                    icon = Icons.Default.Info,
                                    label = "Project Group ID",
                                    value = currentVoucher.effectiveGroupId
                                )

                                HorizontalDivider(thickness = 0.5.dp, color = colors.border.copy(alpha = 0.4f))

                                DetailRowWithIcon(
                                    icon = Icons.Default.Info,
                                    label = "User Group ID",
                                    value = currentVoucher.effectiveUserGroupId
                                )

                                HorizontalDivider(thickness = 0.5.dp, color = colors.border.copy(alpha = 0.4f))

                                DetailRowWithIcon(
                                    icon = Icons.Default.PlayArrow,
                                    label = "Upload Traffic",
                                    value = currentVoucher.effectiveUploadTrafficDisplay
                                )

                                HorizontalDivider(thickness = 0.5.dp, color = colors.border.copy(alpha = 0.4f))

                                DetailRowWithIcon(
                                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                                    label = "Download Traffic",
                                    value = currentVoucher.effectiveDownloadTrafficDisplay
                                )
                            }
                        }

                        // 5. Device & MAC Binding Card
                        val canUnbind = currentVoucher.isDeviceBound || currentVoucher.normalizedStatus == VoucherStatus.USED
                        NeumorphicCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            elevation = 1.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                DetailRowWithIcon(
                                    icon = Icons.Default.Key,
                                    label = "Bound MAC",
                                    value = currentVoucher.effectiveBoundMacDisplay
                                )

                                if (onUnbind != null) {
                                    HorizontalDivider(thickness = 0.5.dp, color = colors.border.copy(alpha = 0.4f))

                                    NeumorphicOutlinedButton(
                                        onClick = {
                                            isUnbinding = true
                                            onUnbind(currentVoucher)
                                            currentVoucher = currentVoucher.copy(
                                                bindMac = null,
                                                bindingMac = null,
                                                mac = null,
                                                macAddress = null,
                                                clientMac = null,
                                                terminalMac = null,
                                                userMac = null
                                            )
                                            actionMessage = "Device unbound. Please enter voucher code again."
                                            isUnbinding = false
                                        },
                                        enabled = canUnbind && !isUnbinding,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(42.dp),
                                        borderColor = if (canUnbind) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else colors.border
                                    ) {
                                        if (isUnbinding) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = colors.primary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "Unbinding Device...",
                                                fontSize = 13.sp,
                                                color = colors.textSecondary
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.LinkOff,
                                                contentDescription = "Unbind",
                                                modifier = Modifier.size(16.dp),
                                                tint = if (canUnbind) MaterialTheme.colorScheme.error else colors.textSecondary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (currentVoucher.isDeviceBound) "Unbind Current Device" else "Unbind Voucher",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (canUnbind) MaterialTheme.colorScheme.error else colors.textSecondary
                                            )
                                        }
                                    }
                                }

                                if (actionMessage != null) {
                                    Surface(
                                        color = StatusGreenContainer,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = StatusGreen,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = actionMessage ?: "",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = StatusGreen,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    var showInspector by remember { mutableStateOf(false) }
                    if (showInspector) {
                        VoucherInspectorDialog(
                            voucher = currentVoucher,
                            onRefresh = {
                                if (onRefresh != null) {
                                    onRefresh(currentVoucher)
                                }
                            },
                            onDismiss = { showInspector = false }
                        )
                    }

                    var reportTextToExport by remember { mutableStateOf<String?>(null) }
                    val createFileLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.CreateDocument("text/plain")
                    ) { uri: Uri? ->
                        if (uri != null) {
                            try {
                                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                                    outputStream.write(reportTextToExport?.toByteArray(Charsets.UTF_8) ?: byteArrayOf())
                                    outputStream.flush()
                                }
                                Toast.makeText(context, "Voucher detail TXT saved successfully.", Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Export failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Inspector Action Buttons Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            NeumorphicButton(
                                onClick = { showInspector = true },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1.3f).height(48.dp).testTag("preview_dialog_inspect_btn"),
                                primary = false
                            ) {
                                Text("Inspect Voucher Detail", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            NeumorphicButton(
                                onClick = {
                                    val rep = VoucherInspectorCollector.inspectVoucher(currentVoucher)
                                    val textReport = VoucherInspectorCollector.buildTxtReport(rep)
                                    reportTextToExport = textReport
                                    val sdf = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date())
                                    createFileLauncher.launch("ruijie-voucher-detail-$sdf.txt")
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(48.dp).testTag("preview_dialog_export_txt_btn"),
                                primary = false
                            ) {
                                Text("Export TXT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            NeumorphicButton(
                                onClick = {
                                    val rep = VoucherInspectorCollector.inspectVoucher(currentVoucher)
                                    val textReport = VoucherInspectorCollector.buildTxtReport(rep)
                                    VoucherInspectorCollector.shareReport(context, textReport)
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(48.dp).testTag("preview_dialog_share_btn"),
                                primary = false
                            ) {
                                Text("Share Report", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Print Voucher Action Button
                        NeumorphicButton(
                            onClick = {
                                onPrint(currentVoucher)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("preview_dialog_print_btn"),
                            primary = true
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Print Voucher",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRowWithIcon(
    icon: ImageVector,
    label: String,
    value: String
) {
    val colors = LocalNeumorphicColors.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

