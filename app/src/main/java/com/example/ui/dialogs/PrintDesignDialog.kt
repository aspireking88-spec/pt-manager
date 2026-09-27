package com.example.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.printer.PaperConfiguration
import com.example.printer.PaperWidth
import com.example.printer.PrintDesignSettings

@Composable
fun PrintDesignDialog(
    initialSettings: PrintDesignSettings,
    onSave: (PrintDesignSettings) -> Unit,
    onDismiss: () -> Unit
) {
    var headerTitle by remember { mutableStateOf(initialSettings.headerTitle) }
    var subHeader by remember { mutableStateOf(initialSettings.subHeader) }
    var footerText by remember { mutableStateOf(initialSettings.footerText) }
    var supportContact by remember { mutableStateOf(initialSettings.supportContact) }
    var showFooter by remember { mutableStateOf(initialSettings.showFooter) }
    var showSupport by remember { mutableStateOf(initialSettings.showSupportContact) }
    var is80mm by remember { mutableStateOf(initialSettings.paperConfig.paperWidth == PaperWidth.MM_80) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("dialog_print_design"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Receipt Template & Design",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = headerTitle,
                    onValueChange = { headerTitle = it },
                    label = { Text("Header Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = subHeader,
                    onValueChange = { subHeader = it },
                    label = { Text("Sub-Header") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = supportContact,
                    onValueChange = { supportContact = it },
                    label = { Text("Support Contact / Help") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = footerText,
                    onValueChange = { footerText = it },
                    label = { Text("Footer Message") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Paper Width (80mm / 58mm)")
                    Switch(
                        checked = is80mm,
                        onCheckedChange = { is80mm = it }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Show Support Contact")
                    Switch(
                        checked = showSupport,
                        onCheckedChange = { showSupport = it }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Show Footer Text")
                    Switch(
                        checked = showFooter,
                        onCheckedChange = { showFooter = it }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val updated = initialSettings.copy(
                                headerTitle = headerTitle,
                                subHeader = subHeader,
                                footerText = footerText,
                                supportContact = supportContact,
                                showFooter = showFooter,
                                showSupportContact = showSupport,
                                paperConfig = initialSettings.paperConfig.copy(
                                    paperWidth = if (is80mm) PaperWidth.MM_80 else PaperWidth.MM_58
                                )
                            )
                            onSave(updated)
                            onDismiss()
                        },
                        modifier = Modifier.testTag("btn_save_design")
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}

@Composable
fun PrintDesignDialog(
    currentSettings: PrintDesignSettings,
    paperConfig: PaperConfiguration = currentSettings.paperConfig,
    siteName: String? = null,
    onSaveSettings: (PrintDesignSettings) -> Unit = {},
    onResetDefaults: () -> Unit = {},
    onOpenTypography: () -> Unit = {},
    onDismiss: () -> Unit
) {
    PrintDesignDialog(
        initialSettings = currentSettings,
        onSave = onSaveSettings,
        onDismiss = onDismiss
    )
}
