package com.example.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.DesignServices
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.printer.PrinterConnectionState
import com.example.ui.PrinterV1ViewModel

@Composable
fun PrinterScreen(
    printerState: PrinterConnectionState,
    onOpenPrinterDialog: () -> Unit,
    onOpenDesignDialog: () -> Unit,
    onOpenHistoryDialog: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Thermal Printer Management",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        val statusText = when (printerState) {
            is PrinterConnectionState.Connected -> "Connected to ${printerState.deviceName}"
            is PrinterConnectionState.Connecting -> "Connecting..."
            is PrinterConnectionState.Error -> "Error: ${printerState.message}"
            is PrinterConnectionState.Disconnected -> "Printer Disconnected"
        }

        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyLarge,
            color = if (printerState is PrinterConnectionState.Connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onOpenPrinterDialog,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("btn_open_printer_dialog"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.BluetoothConnected, contentDescription = null)
            Text(
                text = "Connect Bluetooth Printer",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onOpenDesignDialog,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("btn_open_design_dialog"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.DesignServices, contentDescription = null)
            Text(
                text = "Receipt Template & Design",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onOpenHistoryDialog,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("btn_open_history_dialog"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.History, contentDescription = null)
            Text(
                text = "View Print History Logs",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
fun PrinterScreen(
    viewModel: PrinterV1ViewModel,
    onOpenPrintDesign: () -> Unit = {},
    onOpenTypography: () -> Unit = {}
) {
    val printerState by viewModel.printerState.collectAsState(initial = PrinterConnectionState.Disconnected)
    PrinterScreen(
        printerState = printerState,
        onOpenPrinterDialog = { viewModel.showPrinterDialog.value = true },
        onOpenDesignDialog = onOpenPrintDesign,
        onOpenHistoryDialog = {}
    )
}
