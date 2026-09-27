package com.example.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DiagnosisVerdict
import com.example.data.model.VoucherGenerationDiagnosticRecord
import com.example.ui.components.LocalNeumorphicColors
import com.example.ui.components.NeumorphicButton
import com.example.ui.components.NeumorphicButtonStyle
import com.example.ui.components.NeumorphicCard
import com.example.ui.components.NeumorphicDialog
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusGreenContainer
import com.example.ui.theme.StatusRed
import com.example.ui.theme.StatusRedContainer

@Composable
fun VoucherGenerationLogsDialog(
    logs: List<VoucherGenerationDiagnosticRecord>,
    onClearLogs: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalNeumorphicColors.current
    var copiedMsg by remember { mutableStateOf<String?>(null) }

    fun copyLog(log: VoucherGenerationDiagnosticRecord) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Voucher Generation Log", log.formatForChatGPT())
        clipboard.setPrimaryClip(clip)
        copiedMsg = "Log copied to clipboard"
    }

    NeumorphicDialog(
        title = "Voucher Generation Logs",
        subtitle = "History of voucher generation API calls",
        icon = Icons.Default.History,
        onDismiss = onDismiss,
        modifier = Modifier.testTag("voucher_generation_logs_dialog")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (logs.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    NeumorphicButton(
                        onClick = onClearLogs,
                        modifier = Modifier.testTag("clear_logs_btn")
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear Logs", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            }

            if (copiedMsg != null) {
                Surface(
                    color = StatusGreenContainer,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = copiedMsg ?: "",
                        color = StatusGreen,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No voucher generation logs recorded yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(logs, key = { it.id }) { log ->
                        VoucherGenLogItemCard(log = log, onCopy = { copyLog(log) })
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

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
private fun VoucherGenLogItemCard(
    log: VoucherGenerationDiagnosticRecord,
    onCopy: () -> Unit
) {
    val colors = LocalNeumorphicColors.current
    val verdictColor = when (log.codeTypeResult) {
        DiagnosisVerdict.PASS -> StatusGreen
        DiagnosisVerdict.FAIL -> StatusRed
        DiagnosisVerdict.UNSUPPORTED -> Color(0xFFD97706)
    }
    val verdictBg = when (log.codeTypeResult) {
        DiagnosisVerdict.PASS -> StatusGreenContainer
        DiagnosisVerdict.FAIL -> StatusRedContainer
        DiagnosisVerdict.UNSUPPORTED -> Color(0xFFFEF3C7)
    }

    NeumorphicCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        containerColor = colors.surface,
        borderColor = colors.border
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = log.formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary
                )
                Surface(
                    color = verdictBg,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = log.codeTypeResult.displayName,
                        color = verdictColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = "Type: ${log.selectedCodeType.label}  •  HTTP ${log.responseHttpStatus}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )

            Text(
                text = "Mapped: ${log.mappedApiValue}",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = colors.textSecondary
            )

            if (log.returnedVoucherCode.isNotBlank()) {
                Text(
                    text = "Code: ${log.returnedVoucherCode}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.primary
                )
            }

            Text(
                text = log.reason,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = colors.textSecondary
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onCopy) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = colors.primary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy Log", fontSize = 11.sp, color = colors.primary)
                }
            }
        }
    }
}
