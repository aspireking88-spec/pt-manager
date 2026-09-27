package com.example.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ApiDiagnosticRecord
import com.example.ui.components.LocalNeumorphicColors
import com.example.ui.components.NeumorphicButton
import com.example.ui.components.NeumorphicButtonStyle
import com.example.ui.components.NeumorphicCard
import com.example.ui.components.NeumorphicDialog
import com.example.ui.components.NeumorphicIconButton
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusGreenContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ApiDiagnosticDialog(
    diagnostic: ApiDiagnosticRecord?,
    onCopyRawJson: (String) -> Unit,
    onExportReport: (() -> Unit)? = null,
    onCopyReport: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalNeumorphicColors.current

    NeumorphicDialog(
        title = "Ruijie API Diagnostics",
        subtitle = "Detailed HTTP status and payload inspection",
        icon = Icons.Default.BugReport,
        onDismiss = onDismiss,
        modifier = Modifier.testTag("api_diagnostic_dialog")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Action bar for full diagnostic report export & copy
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NeumorphicButton(
                    onClick = {
                        onExportReport?.invoke()
                    },
                    primary = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("export_diagnostic_txt_btn")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export TXT", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
                }

                NeumorphicButton(
                    onClick = {
                        onCopyReport?.invoke()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("copy_diagnostic_report_btn")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = colors.textPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Report", style = MaterialTheme.typography.labelMedium, color = colors.textPrimary)
                }
            }
            if (diagnostic == null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = colors.background
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No API calls recorded yet. Make an API request to see diagnostics.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                }
            } else {
                NeumorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    containerColor = colors.surface,
                    borderColor = colors.border
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${diagnostic.httpMethod} ${diagnostic.server}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Surface(
                                color = if (diagnostic.httpStatus in 200..299) StatusGreenContainer else MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "HTTP ${diagnostic.httpStatus}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (diagnostic.httpStatus in 200..299) StatusGreen else MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = diagnostic.endpoint,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = colors.textSecondary
                        )
                        Text(
                            text = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(diagnostic.timestamp)),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )

                        // Detailed Response Breakdown
                        if (diagnostic.responseCode != null || diagnostic.voucherDataCode != null || diagnostic.voucherCount != null) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = colors.border
                            )
                            Text(
                                text = "Response Breakdown:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (diagnostic.responseCode != null) {
                                Text(
                                    text = "• Root Code: ${diagnostic.responseCode} ${if (!diagnostic.responseMessage.isNullOrBlank()) "(${diagnostic.responseMessage})" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textPrimary
                                )
                            }
                            if (diagnostic.voucherDataCode != null) {
                                Text(
                                    text = "• VoucherData Code: ${diagnostic.voucherDataCode} ${if (!diagnostic.voucherDataMessage.isNullOrBlank()) "(${diagnostic.voucherDataMessage})" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textPrimary
                                )
                            }
                            if (diagnostic.voucherCount != null) {
                                Text(
                                    text = "• VoucherData Count: ${diagnostic.voucherCount}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textPrimary
                                )
                            }
                            if (diagnostic.returnedListCount != null) {
                                Text(
                                    text = "• Returned List Items: ${diagnostic.returnedListCount}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textPrimary
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Raw Response:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    NeumorphicButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Ruijie API Diagnostic", diagnostic.rawJson)
                            clipboard.setPrimaryClip(clip)
                            onCopyRawJson("Diagnostic JSON copied to clipboard")
                        },
                        modifier = Modifier.testTag("copy_diagnostic_json_btn")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp), tint = colors.textPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy JSON", style = MaterialTheme.typography.labelSmall, color = colors.textPrimary)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1E1E1E))
                        .padding(10.dp)
                ) {
                    val verticalScroll = rememberScrollState()
                    val horizontalScroll = rememberScrollState()
                    Text(
                        text = if (diagnostic.rawJson.isNotBlank()) diagnostic.rawJson else "(Empty body)",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFD4D4D4),
                        modifier = Modifier
                            .verticalScroll(verticalScroll)
                            .horizontalScroll(horizontalScroll)
                    )
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

