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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DiagnosisVerdict
import com.example.data.model.VoucherCodeType
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
fun VoucherGenerationDiagnosisDialog(
    diagnostic: VoucherGenerationDiagnosticRecord?,
    isTesting: Boolean,
    onRunTest: (VoucherCodeType) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalNeumorphicColors.current
    var testConfirmationType by remember { mutableStateOf<VoucherCodeType?>(null) }
    var copiedNotice by remember { mutableStateOf<String?>(null) }

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        copiedNotice = "$label copied to clipboard"
    }

    NeumorphicDialog(
        title = "Voucher Generation Diagnosis",
        subtitle = "Cloud API request and response trace",
        icon = Icons.Default.BugReport,
        onDismiss = onDismiss,
        modifier = Modifier.testTag("voucher_generation_diagnosis_dialog")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Test Action Buttons Section
            Text(
                text = "Execute Safe Diagnosis Test (1 Voucher):",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textSecondary
            )

             Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NeumorphicButton(
                    onClick = { testConfirmationType = VoucherCodeType.ALPHANUMERIC },
                    modifier = Modifier.weight(1f).testTag("diag_test_alphanumeric_btn"),
                    enabled = !isTesting
                ) {
                    Text("Alphanumeric", fontSize = 11.sp, maxLines = 1, color = colors.textPrimary)
                }
                NeumorphicButton(
                    onClick = { testConfirmationType = VoucherCodeType.ALPHABETIC },
                    modifier = Modifier.weight(1f).testTag("diag_test_alphabetic_btn"),
                    enabled = !isTesting
                ) {
                    Text("Alphabetic", fontSize = 11.sp, maxLines = 1, color = colors.textPrimary)
                }
                NeumorphicButton(
                    onClick = { testConfirmationType = VoucherCodeType.NUMERIC },
                    modifier = Modifier.weight(1f).testTag("diag_test_numeric_btn"),
                    enabled = !isTesting
                ) {
                    Text("Numeric", fontSize = 11.sp, maxLines = 1, color = colors.textPrimary)
                }
            }

            if (isTesting) {
                NeumorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    containerColor = colors.surface,
                    borderColor = colors.border
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = colors.primary)
                        Text("Running Cloud diagnostic generation test...", style = MaterialTheme.typography.bodySmall, color = colors.textPrimary)
                    }
                }
            }

            if (copiedNotice != null) {
                Surface(
                    color = StatusGreenContainer,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = copiedNotice ?: "",
                        color = StatusGreen,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            if (diagnostic == null) {
                NeumorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    containerColor = colors.surface,
                    borderColor = colors.border
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No generation attempts recorded yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap one of the test buttons above or generate a voucher to view the complete diagnostic trace.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                }
            } else {
                // Result Header Card
                val verdictColor = when (diagnostic.codeTypeResult) {
                    DiagnosisVerdict.PASS -> StatusGreen
                    DiagnosisVerdict.FAIL -> StatusRed
                    DiagnosisVerdict.UNSUPPORTED -> Color(0xFFD97706)
                }
                val verdictBg = when (diagnostic.codeTypeResult) {
                    DiagnosisVerdict.PASS -> StatusGreenContainer
                    DiagnosisVerdict.FAIL -> StatusRedContainer
                    DiagnosisVerdict.UNSUPPORTED -> Color(0xFFFEF3C7)
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = verdictBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Result: ${diagnostic.codeTypeResult.displayName}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = verdictColor
                            )
                            Text(
                                text = diagnostic.formattedTime,
                                style = MaterialTheme.typography.labelSmall,
                                color = verdictColor.copy(alpha = 0.8f)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = diagnostic.reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF1E293B)
                        )
                    }
                }

                // Structured Diagnostic Details Card
                NeumorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    containerColor = colors.surface,
                    borderColor = colors.border
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DiagRow("Selected Code Type", diagnostic.selectedCodeType.label)
                        DiagRow("UI Value", diagnostic.uiValue)
                        DiagRow("Mapped API Value", diagnostic.mappedApiValue)
                        DiagRow("Endpoint", diagnostic.endpoint)
                        DiagRow("HTTP Method", diagnostic.httpMethod)
                        DiagRow("Request Query", diagnostic.requestQuery.ifBlank { "(none or masked)" })
                        DiagRow("HTTP Status", "${diagnostic.responseHttpStatus}")
                        DiagRow("Returned Voucher Field", diagnostic.returnedVoucherField)
                        DiagRow("Returned Voucher Code", diagnostic.returnedVoucherCode.ifBlank { "(none)" })
                    }
                }

                // Request Body Snippet
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Request Body (Sanitized):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        TextButton(onClick = { copyToClipboard("Request Body", diagnostic.sanitizedRequestBody) }) {
                            Text("Copy", fontSize = 11.sp, color = colors.primary)
                        }
                    }
                    CodeBox(text = diagnostic.sanitizedRequestBody)
                }

                // Response Body Snippet
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Response Body (Sanitized):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        TextButton(onClick = { copyToClipboard("Response Body", diagnostic.sanitizedResponseBody) }) {
                            Text("Copy", fontSize = 11.sp, color = colors.primary)
                        }
                    }
                    CodeBox(text = diagnostic.sanitizedResponseBody)
                }

                // Prominent Copy Buttons
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NeumorphicButton(
                        onClick = { copyToClipboard("Diagnosis Report", diagnostic.formatForChatGPT()) },
                        primary = true,
                        modifier = Modifier.fillMaxWidth().testTag("copy_diagnosis_btn")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("COPY DIAGNOSIS (Ready for ChatGPT)", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NeumorphicButton(
                            onClick = { copyToClipboard("API Request", diagnostic.formatSanitizedRequest()) },
                            modifier = Modifier.weight(1f).testTag("copy_request_btn")
                        ) {
                            Text("Copy Request", fontSize = 12.sp, color = colors.textPrimary)
                        }
                        NeumorphicButton(
                            onClick = { copyToClipboard("API Response", diagnostic.formatSanitizedResponse()) },
                            modifier = Modifier.weight(1f).testTag("copy_response_btn")
                        ) {
                            Text("Copy Response", fontSize = 12.sp, color = colors.textPrimary)
                        }
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

    // Safety confirmation dialog before executing a real voucher test on Ruijie Cloud
    testConfirmationType?.let { testType ->
        NeumorphicDialog(
            title = "Confirm Diagnostic Test",
            subtitle = "Ruijie Cloud API safety verification",
            icon = Icons.Default.Warning,
            onDismiss = { testConfirmationType = null }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "This test will send an API request to Ruijie Cloud to create exactly 1 voucher with code type '${testType.label}' to diagnose the cloud API behavior.\n\nDo you want to proceed?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NeumorphicButton(
                        onClick = { testConfirmationType = null },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = colors.textSecondary)
                    }
                    NeumorphicButton(
                        onClick = {
                            val chosen = testType
                            testConfirmationType = null
                            onRunTest(chosen)
                        },
                        primary = true,
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Text("Execute Test", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagRow(label: String, value: String) {
    val colors = LocalNeumorphicColors.current
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = colors.textPrimary
        )
    }
}

@Composable
private fun CodeBox(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF090D16))
            .padding(8.dp)
            .horizontalScroll(rememberScrollState())
    ) {
        Text(
            text = text.ifBlank { "(empty)" },
            color = Color(0xFF38BDF8),
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            lineHeight = 15.sp
        )
    }
}

