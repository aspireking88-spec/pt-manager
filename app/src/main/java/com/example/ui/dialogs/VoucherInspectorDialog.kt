package com.example.ui.dialogs

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.diagnostics.VoucherInspectorCollector
import com.example.data.model.FieldInspection
import com.example.data.model.RuijieVoucherItem
import com.example.data.model.VoucherInspectionReport
import com.example.ui.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoucherInspectorDialog(
    voucher: RuijieVoucherItem,
    vouchersState: UiState<List<RuijieVoucherItem>>? = null,
    onRefresh: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val targetId = remember { voucher.uuid ?: voucher.id }
    val targetCode = remember { voucher.voucherCode ?: voucher.codeNo ?: voucher.code }

    // Register selection inside the collector on startup
    LaunchedEffect(targetId, targetCode) {
        VoucherInspectorCollector.selectVoucher(targetId, targetCode)
    }

    val liveVouchers = remember(vouchersState) {
        when (vouchersState) {
            is UiState.Success -> vouchersState.data
            else -> emptyList()
        }
    }

    val currentVoucher = remember(liveVouchers, targetId, targetCode) {
        if (liveVouchers.isEmpty()) {
            voucher
        } else {
            liveVouchers.find { item ->
                val idMatch = !targetId.isNullOrBlank() && (item.uuid == targetId || item.id == targetId)
                val codeMatch = !targetCode.isNullOrBlank() && (item.voucherCode == targetCode || item.codeNo == targetCode || item.code == targetCode)
                idMatch || codeMatch
            } ?: voucher
        }
    }

    val isMissing = remember(liveVouchers, targetId, targetCode) {
        if (liveVouchers.isEmpty()) {
            false
        } else {
            liveVouchers.none { item ->
                val idMatch = !targetId.isNullOrBlank() && (item.uuid == targetId || item.id == targetId)
                val codeMatch = !targetCode.isNullOrBlank() && (item.voucherCode == targetCode || item.codeNo == targetCode || item.code == targetCode)
                idMatch || codeMatch
            }
        }
    }
    
    var report by remember(currentVoucher) { mutableStateOf(VoucherInspectorCollector.inspectVoucher(currentVoucher)) }
    var isRefreshing by remember { mutableStateOf(false) }
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
    
    val handleRefresh = {
        if (!VoucherInspectorCollector.isDuplicateRefresh()) {
            isRefreshing = true
            VoucherInspectorCollector.blockDuplicateRefresh {
                onRefresh?.invoke()
                // Update local report
                report = VoucherInspectorCollector.inspectVoucher(currentVoucher, sourceState = "live response")
                Toast.makeText(context, "Voucher details refreshed", Toast.LENGTH_SHORT).show()
            }
            isRefreshing = false
        } else {
            Toast.makeText(context, "Refresh already in progress...", Toast.LENGTH_SHORT).show()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .testTag("voucher_inspector_dialog"),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Toolbar
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Voucher Detail Inspector",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "UUID: ${currentVoucher.uuid ?: currentVoucher.id ?: "—"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss, modifier = Modifier.testTag("inspector_close_back")) {
                            Icon(Icons.Default.Close, contentDescription = "Close Inspector")
                        }
                    },
                    actions = {
                        IconButton(onClick = { handleRefresh() }, enabled = !isRefreshing, modifier = Modifier.testTag("inspector_refresh_top_btn")) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Data")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                // Main Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (isMissing) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("voucher_missing_warning_card")
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Warning",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "Selected voucher is no longer present in the live list.",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    // Package Type Detection Panel
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Package Type Info",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Package Type: ${report.detection.packageType.displayName}",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Reason: ${report.detection.reason}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Profile Name Hint: ${report.detection.profileName} (ID: ${report.detection.profileId})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                            )
                        }
                    }

                    // Metadata Summary
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "State: ${report.requestMeta.sourceState}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "Inspected: ${report.generatedAt}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }

                    // Field Sections
                    FieldCategorySection(title = "IDENTITY", fields = report.identities)
                    FieldCategorySection(title = "PACKAGE & PROFILE", fields = report.packages)
                    FieldCategorySection(title = "STATUS", fields = report.statuses)
                    FieldCategorySection(title = "DATA / GB USAGE", fields = report.dataGbs)
                    FieldCategorySection(title = "TIME / HOURS LIMIT", fields = report.timeHours)
                    FieldCategorySection(title = "CLIENT / MAC BINDING", fields = report.clients)
                    FieldCategorySection(title = "PROJECT METADATA", fields = report.projects)
                }

                // Action Buttons Footer
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { handleRefresh() },
                                modifier = Modifier.weight(1f).height(48.dp).testTag("refresh_selected_voucher_btn")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Refresh Selected Voucher", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val textReport = VoucherInspectorCollector.buildTxtReport(report)
                                    reportTextToExport = textReport
                                    val sdf = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date())
                                    createFileLauncher.launch("ruijie-voucher-detail-$sdf.txt")
                                },
                                modifier = Modifier.weight(1f).height(48.dp).testTag("export_voucher_detail_txt_btn")
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Export Voucher Detail TXT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val textReport = VoucherInspectorCollector.buildTxtReport(report)
                                    VoucherInspectorCollector.shareReport(context, textReport)
                                },
                                modifier = Modifier.weight(1f).height(48.dp).testTag("share_voucher_detail_btn")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share Voucher Detail", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val textReport = VoucherInspectorCollector.buildTxtReport(report)
                                    clipboardManager.setText(AnnotatedString(textReport))
                                    Toast.makeText(context, "Report copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f).height(48.dp).testTag("copy_report_btn")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy Report", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                modifier = Modifier.weight(1f).height(48.dp).testTag("close_inspector_btn")
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Close", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FieldCategorySection(
    title: String,
    fields: List<FieldInspection>
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        
        fields.forEach { field ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (field.isPresent) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = if (field.isPresent) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = field.fieldName,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            SuggestionChip(
                                onClick = {},
                                label = { Text(field.sourceKey, fontSize = 10.sp) },
                                modifier = Modifier.height(24.dp)
                            )
                            if (field.isDerived) {
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text("Derived", fontSize = 10.sp) },
                                    modifier = Modifier.height(24.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Raw value:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = field.rawValue,
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Normalized value:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = field.normalizedValue,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (field.isPresent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }
        }
    }
}
