package com.example.ui.dialogs

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.diagnostics.RuijieDiagnosticCollector
import com.example.data.model.DiagnosticResultCategory
import com.example.ui.PrinterV1ViewModel
import kotlinx.coroutines.launch
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RuijieDiagnosticDialog(
    viewModel: PrinterV1ViewModel,
    onDismiss: () -> Unit,
    onOpenFullDiagnostics: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val sessionInfo by RuijieDiagnosticCollector.sessionInfo.collectAsState()
    val projectSummary by RuijieDiagnosticCollector.projectSummary.collectAsState()
    val voucherGenSummary by RuijieDiagnosticCollector.voucherGenSummary.collectAsState()
    val latestOps by RuijieDiagnosticCollector.latestOperationsFlow.collectAsState()

    val createOp = latestOps[RuijieDiagnosticCollector.OP_VOUCHER_CREATE]

    // SAF launcher
    val exportDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val reportContent = RuijieDiagnosticCollector.generatePlainTextReport()
                    context.contentResolver.openOutputStream(uri)?.use { outputStream: OutputStream ->
                        outputStream.write(reportContent.toByteArray(Charsets.UTF_8))
                        outputStream.flush()
                    }
                    Toast.makeText(context, "Diagnostic report saved to file", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Save failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun triggerTxtExport() {
        val dateStamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val defaultFileName = "ruijie-diagnostic-$dateStamp.txt"
        exportDocumentLauncher.launch(defaultFileName)
    }

    fun triggerShareReport() {
        val report = RuijieDiagnosticCollector.generatePlainTextReport()
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Ruijie Diagnostic Report")
            putExtra(Intent.EXTRA_TEXT, report)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Diagnostic Report"))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Ruijie Diagnostic Quick View",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Assessment Banner
                val (bannerColor, bannerText) = when {
                    createOp?.resultCategory == DiagnosticResultCategory.GROUP_NOT_SYNCHRONIZED || createOp?.ruijieCode == 1014 ->
                        Pair(
                            MaterialTheme.colorScheme.tertiaryContainer,
                            "Ruijie Code 1014: Project group is still synchronizing on Ruijie Cloud. Please refresh projects and retry in a few moments."
                        )
                    createOp?.resultCategory == DiagnosticResultCategory.WRONG_GROUP_ID ->
                        Pair(
                            MaterialTheme.colorScheme.errorContainer,
                            "Invalid Group ID: Please re-select the project group."
                        )
                    createOp?.resultCategory == DiagnosticResultCategory.REQUEST_TIMEOUT ->
                        Pair(
                            MaterialTheme.colorScheme.tertiaryContainer,
                            "Request Timeout: Login session was preserved. Refresh voucher list to verify creation."
                        )
                    !sessionInfo.authenticatedNavigationDetected ->
                        Pair(
                            MaterialTheme.colorScheme.errorContainer,
                            "SSO Session Inactive: Please sign in via the official Ruijie portal."
                        )
                    else ->
                        Pair(
                            MaterialTheme.colorScheme.surfaceVariant,
                            "Session is authenticated and project '${projectSummary.projectName}' is ready."
                        )
                }

                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = bannerColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = bannerText,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                // Summary info
                Text("Project: ${projectSummary.projectName.ifBlank { "Not selected" }}", style = MaterialTheme.typography.bodySmall)
                Text("Group ID: ${projectSummary.groupIdStr.ifBlank { "None" }} (Numeric: ${projectSummary.numericGroupId ?: "None"})", style = MaterialTheme.typography.bodySmall)
                Text("Tenant: ${projectSummary.tenantName.ifBlank { "None" }}", style = MaterialTheme.typography.bodySmall)
                Text("Session State: ${sessionInfo.sessionState} (Bridge: ${sessionInfo.bridgeReady})", style = MaterialTheme.typography.bodySmall)

                if (createOp != null) {
                    HorizontalDivider()
                    Text("Last Create Attempt:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text("Endpoint: ${createOp.endpoint}", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                    Text("Status: HTTP ${createOp.httpStatus} | Code: ${createOp.ruijieCode ?: "None"}", style = MaterialTheme.typography.bodySmall)
                    if (createOp.safeMessage.isNotBlank()) {
                        Text("Message: ${createOp.safeMessage}", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider()

                // Fast action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { triggerTxtExport() },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("dialog_diag_export_button")
                    ) {
                        Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save TXT", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = { triggerShareReport() },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("dialog_diag_share_button")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share", fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    onOpenFullDiagnostics()
                },
                modifier = Modifier.testTag("dialog_diag_full_screen_button")
            ) {
                Text("Full Diagnostics")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
