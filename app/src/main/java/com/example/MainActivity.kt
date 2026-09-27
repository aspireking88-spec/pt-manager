package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.data.model.AppThemeMode
import com.example.data.model.VoucherPackage
import com.example.ui.PrinterV1ViewModel
import com.example.ui.components.AppBottomNav
import com.example.ui.components.NavScreen
import com.example.ui.dialogs.ApiConfigDialog
import com.example.ui.dialogs.PrintDesignDialog
import com.example.ui.dialogs.PrintHistoryDialog
import com.example.ui.dialogs.PrintPreviewDialog
import com.example.ui.dialogs.PrinterDialog
import com.example.ui.dialogs.ProjectSelectorDialog
import com.example.ui.dialogs.VoucherDetailsPreviewDialog
import com.example.ui.screens.GenerateVoucherScreen
import com.example.ui.screens.MainVoucherScreen
import com.example.ui.screens.PrinterScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: PrinterV1ViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val appearance by viewModel.appearance.collectAsState()
            val isDark = appearance.themeMode == AppThemeMode.DARK

            MyApplicationTheme(darkTheme = isDark) {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: PrinterV1ViewModel) {
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    var currentScreen by remember { mutableStateOf(NavScreen.VOUCHERS) }
    val activeAccount by viewModel.activeAccount.collectAsState(initial = null)
    val vouchers by viewModel.vouchers.collectAsState(initial = emptyList())
    val packages by viewModel.packages.collectAsState(initial = emptyList())
    val tenants by viewModel.tenants.collectAsState(initial = emptyList())
    val networkGroups by viewModel.networkGroups.collectAsState(initial = emptyList())
    val selectedVoucher by viewModel.selectedVoucher.collectAsState(initial = null)
    val printHistory by viewModel.printHistory.collectAsState(initial = emptyList())
    val printDesign by viewModel.printDesign.collectAsState(initial = com.example.printer.PrintDesignSettings())
    val printerState by viewModel.printerState.collectAsState(initial = com.example.printer.PrinterConnectionState.Disconnected)
    val appearance by viewModel.appearance.collectAsState(initial = com.example.data.model.AppAppearanceSettings())
    val statusMessage by viewModel.statusMessage.collectAsState(initial = null)

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    // Dialog flags
    var showProjectDialog by remember { mutableStateOf(false) }
    var showApiDialog by remember { mutableStateOf(false) }
    var showPrinterDialog by remember { mutableStateOf(false) }
    var showPrintPreview by remember { mutableStateOf(false) }
    var showPrintHistory by remember { mutableStateOf(false) }
    var showPrintDesign by remember { mutableStateOf(false) }
    var showVoucherDetails by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            AppBottomNav(
                currentScreen = currentScreen,
                onScreenSelected = { currentScreen = it }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                NavScreen.VOUCHERS -> MainVoucherScreen(viewModel = viewModel)
                NavScreen.GENERATE -> GenerateVoucherScreen(
                    viewModel = viewModel,
                    onOpenProjectSelector = { showProjectDialog = true },
                    onPreviewVouchers = { currentScreen = NavScreen.VOUCHERS }
                )
                NavScreen.PRINTER -> PrinterScreen(
                    printerState = printerState,
                    onOpenPrinterDialog = { showPrinterDialog = true },
                    onOpenDesignDialog = { showPrintDesign = true },
                    onOpenHistoryDialog = { showPrintHistory = true }
                )
                NavScreen.SETTINGS -> SettingsScreen(
                    account = activeAccount ?: com.example.data.model.RuijieAccount(),
                    appearance = appearance,
                    onUpdateAppearance = { viewModel.settingsStore.updateAppearance(it) },
                    onOpenApiConfig = { showApiDialog = true }
                )
            }
        }
    }

    // Dialogs
    if (showProjectDialog) {
        ProjectSelectorDialog(
            account = activeAccount ?: com.example.data.model.RuijieAccount(),
            tenants = tenants,
            groups = networkGroups,
            onSelectTenant = { viewModel.selectTenant(it) },
            onSelectGroup = { viewModel.selectGroup(it) },
            onDismiss = { showProjectDialog = false }
        )
    }

    if (showApiDialog) {
        ApiConfigDialog(
            account = activeAccount ?: com.example.data.model.RuijieAccount(),
            onDismiss = { showApiDialog = false },
            onLogin = { user, secret, server, customUrl, mode ->
                viewModel.login(user, secret, server, customUrl, mode) {
                    showApiDialog = false
                }
            }
        )
    }

    if (showPrinterDialog) {
        PrinterDialog(
            printerManager = viewModel.printerManager,
            connectionState = printerState,
            printDesign = printDesign,
            onDismiss = { showPrinterDialog = false }
        )
    }

    if (showPrintPreview && selectedVoucher != null) {
        PrintPreviewDialog(
            voucher = selectedVoucher!!,
            settings = printDesign,
            onPrint = { viewModel.printVoucher(selectedVoucher!!) },
            onDismiss = { showPrintPreview = false }
        )
    }

    if (showVoucherDetails && selectedVoucher != null) {
        VoucherDetailsPreviewDialog(
            voucher = selectedVoucher!!,
            onCopyCode = { code ->
                clipboardManager.setText(AnnotatedString(code))
                scope.launch {
                    snackbarHostState.showSnackbar("Code copied")
                }
            },
            onPrint = { _ ->
                showVoucherDetails = false
                showPrintPreview = true
            },
            onDismiss = { showVoucherDetails = false }
        )
    }

    if (showPrintHistory) {
        PrintHistoryDialog(
            historyList = printHistory,
            onDelete = { id ->
                scope.launch {
                    viewModel.historyRepository.delete(id)
                }
            },
            onClearAll = {
                scope.launch {
                    viewModel.historyRepository.clear()
                }
            },
            onDismiss = { showPrintHistory = false }
        )
    }

    if (showPrintDesign) {
        PrintDesignDialog(
            initialSettings = printDesign,
            onSave = { viewModel.updatePrintDesign(it) },
            onDismiss = { showPrintDesign = false }
        )
    }
}
