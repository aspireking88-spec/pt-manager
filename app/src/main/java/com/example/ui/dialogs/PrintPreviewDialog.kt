package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Voucher
import com.example.printer.PaperConfiguration
import com.example.printer.PrintDesignSettings
import com.example.printer.PrintRenderer

@Composable
fun PrintPreviewDialog(
    voucher: Voucher,
    settings: PrintDesignSettings = PrintDesignSettings(),
    onPrint: () -> Unit,
    onDismiss: () -> Unit
) {
    val previewText = PrintRenderer.renderPlainText(voucher, settings)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("dialog_print_preview"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Thermal Receipt Preview",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Simulated Receipt Paper
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFFDF5), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFE0D8C8), RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = previewText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = Color(0xFF1A1A1A)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Close")
                    }
                    Button(
                        onClick = {
                            onPrint()
                            onDismiss()
                        },
                        modifier = Modifier.testTag("btn_confirm_print")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null)
                        Text("Print", modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun PrintPreviewDialog(
    vouchers: List<Voucher>,
    settings: PrintDesignSettings = PrintDesignSettings(),
    paperConfig: PaperConfiguration = settings.paperConfig,
    siteName: String? = null,
    onPrint: (List<Voucher>, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val firstVoucher = vouchers.firstOrNull()
    val previewText = if (firstVoucher != null) {
        PrintRenderer.renderPlainText(firstVoucher, settings)
    } else {
        "No vouchers selected for preview"
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("dialog_print_preview_batch"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Thermal Receipt Preview (${vouchers.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Simulated Receipt Paper
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFFDF5), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFE0D8C8), RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = previewText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = Color(0xFF1A1A1A)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Close")
                    }
                    Button(
                        onClick = {
                            onPrint(vouchers, 1)
                            onDismiss()
                        },
                        modifier = Modifier.testTag("btn_confirm_print_batch")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null)
                        Text("Print All (${vouchers.size})", modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }
        }
    }
}
