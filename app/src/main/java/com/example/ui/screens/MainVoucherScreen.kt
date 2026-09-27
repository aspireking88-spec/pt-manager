package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import android.widget.Toast
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RuijieVoucherItem
import com.example.data.model.VoucherFilter
import com.example.data.model.VoucherStatus
import com.example.data.session.DeviceLayoutMode
import com.example.printer.PrinterConnectionState
import com.example.ui.PrinterV1ViewModel
import com.example.ui.UiState
import com.example.ui.components.AppBottomNav
import com.example.ui.components.LocalNeumorphicColors
import com.example.ui.components.NeumorphicButton
import com.example.ui.components.NeumorphicCard
import com.example.ui.components.NeumorphicFilterChip
import com.example.ui.components.NeumorphicIconButton
import com.example.ui.components.NeumorphicOutlinedButton
import com.example.ui.components.NeumorphicStatusBadge
import com.example.ui.components.NeumorphicTextField
import com.example.ui.components.NeumorphicTopBar
import com.example.ui.dialogs.AccountManagerDialog
import com.example.ui.dialogs.AllExpiredDialog
import com.example.ui.dialogs.ApiConfigDialog
import com.example.ui.dialogs.ApiDiagnosticDialog
import com.example.ui.dialogs.DeviceModeDialog
import com.example.ui.dialogs.PrintDesignDialog
import com.example.ui.dialogs.PrintHistoryDialog
import com.example.ui.dialogs.PrintPreviewDialog
import com.example.ui.dialogs.ProjectSelectorDialog
import com.example.ui.dialogs.RuijieDiagnosticDialog
import com.example.ui.dialogs.RuijieSSODialog
import com.example.ui.dialogs.TypographySettingsDialog
import com.example.ui.dialogs.VoucherDetailsPreviewDialog
import com.example.ui.screens.RuijieDiagnosticsScreen
import com.example.ui.theme.RuijieBlue
import com.example.ui.theme.StatusBlue
import com.example.ui.theme.StatusBlueContainer
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusGreenContainer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainVoucherScreen(viewModel: PrinterV1ViewModel) {
    val activeAccount by viewModel.activeAccount.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val isLoadingProjects by viewModel.isLoadingProjects.collectAsState()
    val projectUiState by viewModel.projectUiState.collectAsState()
    val selectedProject by viewModel.selectedProject.collectAsState()
    val vouchersState by viewModel.vouchersState.collectAsState()
    val activeFilter by viewModel.activeFilter.collectAsState()
    val selectedCodes by viewModel.selectedVoucherCodes.collectAsState()
    val filterCounts by viewModel.filterCounts.collectAsState()
    val printProgress by viewModel.printProgress.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val pageSize by viewModel.pageSize.collectAsState()
    val totalCount by viewModel.totalVoucherCount.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val printerState by viewModel.printerConnectionState.collectAsState()
    val deviceLayoutMode by viewModel.deviceLayoutMode.collectAsState()
    val printDesignSettings by viewModel.printDesignSettings.collectAsState()
    val paperConfig by viewModel.paperConfig.collectAsState()
    val historyList by viewModel.historyList.collectAsState()
    val diagnostic by viewModel.latestDiagnostic.collectAsState()
    val isLoggingIn by viewModel.isLoggingIn.collectAsState()
    val loginError by viewModel.loginError.collectAsState()
    val printerErrorDialog by viewModel.printerErrorDialog.collectAsState()
    val selectedPreviewVoucher by viewModel.selectedPreviewVoucher.collectAsState()
    val isPreviewRefreshing by viewModel.isPreviewRefreshing.collectAsState()
    val previewRefreshMessage by viewModel.previewRefreshMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Dialog control states
    var showAccountDialog by remember { mutableStateOf(false) }
    var showProjectDialog by remember { mutableStateOf(false) }
    var showApiConfigDialog by remember { mutableStateOf(false) }
    var showDesignDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showDiagnosticDialog by remember { mutableStateOf(false) }
    var showRuijieDiagnosticsScreen by remember { mutableStateOf(false) }
    var showRuijieSessionDiagnosticScreen by remember { mutableStateOf(false) }
    var showDeviceModeDialog by remember { mutableStateOf(false) }
    var showTypographyDialog by remember { mutableStateOf(false) }
    var showSSODialog by remember { mutableStateOf(false) }
    var showVoucherInspectorSettingsDialog by remember { mutableStateOf(false) }

    // Print Preview dialog state (ESC/POS thermal rendering)
    var previewVouchers by remember { mutableStateOf<List<RuijieVoucherItem>?>(null) }

    // Deletion confirmation dialogs
    var showDeleteSelectedConfirm by remember { mutableStateOf(false) }
    var showDeleteExpiredConfirm by remember { mutableStateOf(false) }
    var showAllExpiredDialog by remember { mutableStateOf(false) }
    var singleVoucherToDelete by remember { mutableStateOf<RuijieVoucherItem?>(null) }

    // Top bar Search mode
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Show snackbars from ViewModel messages
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    // Auto-load vouchers when entering Vouchers tab if currently idle
    LaunchedEffect(selectedTab) {
        if (selectedTab == 0 && vouchersState is UiState.Idle) {
            viewModel.loadVouchers(page = 1)
        }
    }

    // Official Ruijie SSO Login Dialog (Native WebView Session Bridge)
    if (showSSODialog) {
        RuijieSSODialog(
            onDismiss = { showSSODialog = false },
            onAuthenticated = {
                viewModel.onSSOLoginSuccess()
            }
        )
    }

    // Account Manager Modal
    if (showAccountDialog) {
        AccountManagerDialog(
            accounts = accounts,
            activeAccount = activeAccount,
            onSelectAccount = { viewModel.switchAccount(it) },
            onAddAccount = { name, appId, secret, server, customUrl ->
                viewModel.addAccount(name, appId, secret, server, customUrl)
            },
            onUpdateAccount = { viewModel.updateAccount(it) },
            onDeleteAccount = { viewModel.deleteAccount(it) },
            onDismiss = { showAccountDialog = false }
        )
    }

    // Project Selector Modal
    if (showProjectDialog) {
        ProjectSelectorDialog(
            projects = projects,
            selectedProjectId = selectedProject?.effectiveId ?: activeAccount.selectedGroupId?.toLongOrNull(),
            isLoading = isLoadingProjects,
            projectState = projectUiState,
            selectedProject = selectedProject,
            onSelectProject = { proj ->
                viewModel.selectProject(proj)
                showProjectDialog = false
            },
            onRefreshProjects = { viewModel.loadProjects() },
            onDismiss = { showProjectDialog = false }
        )
    }

    // API Configuration / Ruijie Cloud Login Modal
    if (showApiConfigDialog) {
        ApiConfigDialog(
            account = activeAccount,
            isLoggingIn = isLoggingIn,
            loginError = loginError,
            onLogin = { username, password, server, customUrl, mode ->
                viewModel.login(username, password, server, customUrl, mode) {
                    showApiConfigDialog = false
                }
            },
            onLogout = {
                viewModel.logout()
            },
            onOpenSSO = {
                showApiConfigDialog = false
                showSSODialog = true
            },
            onDismiss = { showApiConfigDialog = false }
        )
    }

    // Print Design Modal
    if (showDesignDialog) {
        PrintDesignDialog(
            currentSettings = printDesignSettings,
            paperConfig = paperConfig,
            siteName = selectedProject?.effectiveName ?: activeAccount.accountName,
            onSaveSettings = { viewModel.updatePrintDesignSettings(it) },
            onResetDefaults = { viewModel.resetPrintDesignSettings() },
            onOpenTypography = {
                showDesignDialog = false
                showTypographyDialog = true
            },
            onDismiss = { showDesignDialog = false }
        )
    }

    // Advanced Typography / Font Settings Modal
    if (showTypographyDialog) {
        val sampleRealVoucher = (vouchersState as? UiState.Success)?.data?.firstOrNull()
        TypographySettingsDialog(
            initialSettings = printDesignSettings,
            paperConfig = paperConfig,
            siteName = selectedProject?.effectiveName?.ifBlank { null } ?: activeAccount.accountName,
            realVoucher = sampleRealVoucher,
            onSave = { updated ->
                viewModel.updatePrintDesignSettings(updated)
                showTypographyDialog = false
            },
            onDismiss = { showTypographyDialog = false }
        )
    }

    // Print History Modal
    if (showHistoryDialog) {
        PrintHistoryDialog(
            historyList = historyList,
            onReprint = { entity -> viewModel.reprintHistoryItem(entity) },
            onDeleteRecord = { id -> viewModel.deleteHistoryRecord(id) },
            onClearAll = { viewModel.clearAllHistory() },
            onDismiss = { showHistoryDialog = false }
        )
    }

    // API Diagnostics Modal (Quick Diagnostics View + Plain Text Export + Share + Full Diagnostics)
    if (showDiagnosticDialog) {
        RuijieDiagnosticDialog(
            viewModel = viewModel,
            onDismiss = { showDiagnosticDialog = false },
            onOpenFullDiagnostics = {
                showDiagnosticDialog = false
                showRuijieDiagnosticsScreen = true
            }
        )
    }

    // Device Mode Dialog
    if (showDeviceModeDialog) {
        DeviceModeDialog(
            currentMode = deviceLayoutMode,
            onSelectMode = { mode -> viewModel.setDeviceLayoutMode(mode) },
            onDismiss = { showDeviceModeDialog = false }
        )
    }

    // Single Voucher Real Cloud Preview Dialog
    selectedPreviewVoucher?.let { voucher ->
        androidx.compose.runtime.key(voucher.uuid ?: voucher.voucherCode ?: voucher.codeNo ?: voucher.effectiveCode) {
            VoucherDetailsPreviewDialog(
                voucher = voucher,
                selectedProject = selectedProject,
                activeAccount = activeAccount,
                settings = printDesignSettings,
                isRefreshingExternal = isPreviewRefreshing,
                refreshMessage = previewRefreshMessage,
                onCopyCode = { viewModel.copyVoucherCode(it) },
                onPrint = { v -> previewVouchers = listOf(v) },
                onUnbind = { v ->
                    viewModel.unbindVoucher(v) { success, _, updated ->
                        if (success) {
                            viewModel.updateSelectedPreviewVoucher(updated)
                        }
                    }
                },
                onRefresh = { v ->
                    viewModel.refreshSelectedPreviewVoucher()
                },
                onDismiss = { viewModel.clearPreviewVoucher() }
            )
        }
    }

    // Batch or Single Thermal Print Preview Modal
    previewVouchers?.let { vList ->
        PrintPreviewDialog(
            vouchers = vList,
            settings = printDesignSettings,
            paperConfig = paperConfig,
            siteName = selectedProject?.effectiveName ?: activeAccount.accountName,
            onPrint = { vouchersToPrint, copies ->
                if (vouchersToPrint.size == 1) {
                    viewModel.printSingle(vouchersToPrint.first(), copies)
                } else {
                    viewModel.printBatch(vouchersToPrint, copies)
                }
            },
            onDismiss = { previewVouchers = null }
        )
    }

    if (showVoucherInspectorSettingsDialog) {
        val successVouchers = (vouchersState as? UiState.Success)?.data ?: emptyList()
        val targetCode = com.example.data.diagnostics.VoucherInspectorCollector.selectedVoucherCode.collectAsState().value
        val targetId = com.example.data.diagnostics.VoucherInspectorCollector.selectedVoucherId.collectAsState().value
        val context = LocalContext.current
        val matchedVoucher = successVouchers.find { 
            it.uuid == targetId || it.id == targetId || it.voucherCode == targetCode || it.codeNo == targetCode || it.code == targetCode
        }
        
        var lastInspectedVoucher by remember { mutableStateOf<RuijieVoucherItem?>(null) }
        
        LaunchedEffect(matchedVoucher) {
            if (matchedVoucher != null) {
                lastInspectedVoucher = matchedVoucher
            }
        }
        
        val voucherToInspect = matchedVoucher ?: lastInspectedVoucher

        if (voucherToInspect != null) {
            com.example.ui.dialogs.VoucherInspectorDialog(
                voucher = voucherToInspect,
                vouchersState = vouchersState,
                onRefresh = { viewModel.loadVouchers(page = currentPage) },
                onDismiss = { showVoucherInspectorSettingsDialog = false }
            )
        } else if (successVouchers.isNotEmpty()) {
            LaunchedEffect(Unit) {
                Toast.makeText(context, "Selected voucher is no longer available.", Toast.LENGTH_LONG).show()
                showVoucherInspectorSettingsDialog = false
            }
        } else {
            LaunchedEffect(Unit) {
                Toast.makeText(context, "Please select a voucher from the list first to inspect details.", Toast.LENGTH_LONG).show()
                showVoucherInspectorSettingsDialog = false
            }
        }
    }

    // Printer Error Alert Dialog
    printerErrorDialog?.let { errorMsg ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissPrinterErrorDialog() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Printer Error",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            },
            text = {
                Text(
                    text = errorMsg,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissPrinterErrorDialog() },
                    modifier = Modifier.testTag("printer_error_ok_btn")
                ) {
                    Text("OK")
                }
            }
        )
    }

    // Delete Selected Confirmation Dialog
    if (showDeleteSelectedConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteSelectedConfirm = false },
            title = { Text("Delete Selected Vouchers?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Are you sure you want to delete ${selectedCodes.size} selected voucher(s) from Ruijie Cloud?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteSelectedConfirm = false
                        viewModel.attemptDeleteSelectedVouchers { msg ->
                            scope.launch { snackbarHostState.showSnackbar(msg) }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSelectedConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Expired Confirmation Dialog
    if (showDeleteExpiredConfirm) {
        val allVouchers = (vouchersState as? UiState.Success)?.data ?: emptyList()
        val expiredCount = allVouchers.count { it.normalizedStatus == VoucherStatus.EXPIRED }

        AlertDialog(
            onDismissRequest = { showDeleteExpiredConfirm = false },
            title = { Text("Delete All Expired Vouchers?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Expired vouchers found: $expiredCount\n\nAre you sure you want to delete all expired vouchers from Ruijie Cloud?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteExpiredConfirm = false
                        viewModel.attemptDeleteExpiredVouchers { msg ->
                            scope.launch { snackbarHostState.showSnackbar(msg) }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Expired")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteExpiredConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // All Expired Dialog with Date Selection & Real Cloud Count Calculation
    if (showAllExpiredDialog) {
        val allVouchers = viewModel.allCloudVouchers.collectAsState().value
        AllExpiredDialog(
            allVouchers = allVouchers,
            onDismiss = { showAllExpiredDialog = false },
            onConfirmDelete = { matchingVouchers ->
                viewModel.deleteExpiredVouchersBeforeDate(matchingVouchers) { msg ->
                    scope.launch { snackbarHostState.showSnackbar(msg) }
                }
            }
        )
    }

    // Individual Voucher Deletion Confirmation Dialog
    singleVoucherToDelete?.let { voucher ->
        AlertDialog(
            onDismissRequest = { singleVoucherToDelete = null },
            title = {
                Text(
                    text = "Delete Voucher?",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column {
                    Text(
                        text = "Are you sure you want to delete voucher code \"${voucher.effectiveCode}\" from Ruijie Cloud?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Profile: ${voucher.effectiveProfile} • Status: ${voucher.normalizedStatus.name} • Period: ${voucher.effectivePeriod}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toDelete = voucher
                        singleVoucherToDelete = null
                        viewModel.deleteSingleVoucher(toDelete) { msg ->
                            scope.launch { snackbarHostState.showSnackbar(msg) }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { singleVoucherToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    val isPrinterConnected = printerState is PrinterConnectionState.Connected

    if (showRuijieDiagnosticsScreen) {
        RuijieDiagnosticsScreen(
            viewModel = viewModel,
            onNavigateBack = { showRuijieDiagnosticsScreen = false }
        )
        return
    }

    if (showRuijieSessionDiagnosticScreen) {
        RuijieSessionDiagnosticScreen(
            onNavigateBack = { showRuijieSessionDiagnosticScreen = false }
        )
        return
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTabletLayout = when (deviceLayoutMode) {
            DeviceLayoutMode.PHONE -> false
            DeviceLayoutMode.TABLET, DeviceLayoutMode.DESKTOP -> true
            DeviceLayoutMode.AUTO -> maxWidth >= 720.dp
        }

        if (isTabletLayout) {
            // =========================================================
            // TABLET RESPONSIVE LAYOUT (Screen 5 in Reference Image)
            // Navigation Rail on Left, Main Content on Right
            // =========================================================
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    modifier = Modifier.testTag("tablet_navigation_rail"),
                    header = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = RuijieBlue,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.ConfirmationNumber,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Printer V1",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    NavigationRailItem(
                        selected = selectedTab == 0,
                        onClick = { viewModel.setTab(0) },
                        icon = { Icon(Icons.Default.ConfirmationNumber, contentDescription = "Vouchers") },
                        label = { Text("Vouchers") },
                        modifier = Modifier.testTag("tab_vouchers")
                    )
                    NavigationRailItem(
                        selected = selectedTab == 1,
                        onClick = { viewModel.setTab(1) },
                        icon = { Icon(Icons.Default.AddCircleOutline, contentDescription = "Generate") },
                        label = { Text("Generate") },
                        modifier = Modifier.testTag("tab_generate")
                    )
                    NavigationRailItem(
                        selected = selectedTab == 2,
                        onClick = { viewModel.setTab(2) },
                        icon = { Icon(Icons.Default.Print, contentDescription = "Printer") },
                        label = { Text("Printer") },
                        modifier = Modifier.testTag("tab_printer")
                    )
                    NavigationRailItem(
                        selected = selectedTab == 3,
                        onClick = { viewModel.setTab(3) },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
                        modifier = Modifier.testTag("tab_settings")
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Tablet Mode Dialog Trigger
                    IconButton(
                        onClick = { showDeviceModeDialog = true },
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Icon(Icons.Default.Devices, contentDescription = "Device Layout Mode", tint = RuijieBlue)
                    }
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    modifier = Modifier.weight(1f)
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (selectedTab) {
                            1 -> {
                                GenerateVoucherScreen(
                                    viewModel = viewModel,
                                    onOpenProjectSelector = { showProjectDialog = true },
                                    onPreviewVouchers = { previewVouchers = it },
                                    onOpenDiagnostics = { showDiagnosticDialog = true }
                                )
                            }
                            2 -> {
                                PrinterScreen(
                                    viewModel = viewModel,
                                    onOpenPrintDesign = { showDesignDialog = true },
                                    onOpenTypography = { showTypographyDialog = true }
                                )
                            }
                            3 -> {
                                SettingsScreen(
                                    viewModel = viewModel,
                                    onOpenAccounts = { showAccountDialog = true },
                                    onOpenApiConfig = { showApiConfigDialog = true },
                                    onOpenProjectSelector = { showProjectDialog = true },
                                    onOpenPrintDesign = { showDesignDialog = true },
                                    onOpenDiagnostics = { showDiagnosticDialog = true },
                                    onOpenHistory = { showHistoryDialog = true },
                                    onOpenTypography = { showTypographyDialog = true },
                                    onOpenSSO = { showSSODialog = true },
                                    onOpenSessionDiagnostic = { showRuijieSessionDiagnosticScreen = true },
                                    onOpenVoucherInspector = { showVoucherInspectorSettingsDialog = true }
                                )
                            }
                            else -> {
                                TabletVoucherTableView(
                                    viewModel = viewModel,
                                    vouchersState = vouchersState,
                                    activeFilter = activeFilter,
                                    filterCounts = filterCounts,
                                    selectedCodes = selectedCodes,
                                    currentPage = currentPage,
                                    pageSize = pageSize,
                                    totalCount = totalCount,
                                    onPreviewSingle = { viewModel.selectVoucherForPreview(it) },
                                    onPrintSingle = { previewVouchers = listOf(it) },
                                    onPrintBatch = { previewVouchers = it },
                                    onDeleteSingle = { singleVoucherToDelete = it },
                                    onDeleteSelected = { showDeleteSelectedConfirm = true },
                                    onDeleteExpired = { showAllExpiredDialog = true }
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // =========================================================
            // PHONE CLEAN SINGLE-COLUMN LAYOUT (Screen 1 in Reference Image)
            // =========================================================
            Scaffold(
                modifier = Modifier.statusBarsPadding(),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    val colors = LocalNeumorphicColors.current
                    if (isSearchActive && selectedTab == 0) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = colors.surface
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                NeumorphicIconButton(
                                    onClick = { isSearchActive = false; searchQuery = "" },
                                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Close Search",
                                    size = 40.dp,
                                    tint = colors.textPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                NeumorphicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = "Search voucher code...",
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("topbar_search_input"),
                                    trailingIcon = if (searchQuery.isNotEmpty()) {
                                        {
                                            IconButton(
                                                onClick = { searchQuery = "" },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Clear,
                                                    contentDescription = "Clear search",
                                                    tint = colors.textSecondary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    } else null
                                )
                            }
                        }
                    } else {
                        // Neumorphic Top Bar: [Printer V1] ... [Refresh] [Search] [Print] [Device Mode]
                        NeumorphicTopBar(
                            title = when (selectedTab) {
                                0 -> "Printer V1"
                                1 -> "Generate"
                                2 -> "Thermal Printer"
                                3 -> "Settings"
                                else -> "Printer V1"
                            },
                            subtitle = if (selectedTab == 0) {
                                selectedProject?.effectiveName ?: activeAccount.accountName
                            } else null,
                            navigationIcon = {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = colors.primary,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.ConfirmationNumber,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            actions = {
                                if (selectedTab == 0) {
                                    NeumorphicIconButton(
                                        onClick = { viewModel.loadVouchers(page = currentPage) },
                                        icon = Icons.Default.Refresh,
                                        contentDescription = "Refresh Vouchers",
                                        tint = colors.primary,
                                        size = 38.dp,
                                        modifier = Modifier.testTag("topbar_refresh_btn")
                                    )
                                    NeumorphicIconButton(
                                        onClick = { isSearchActive = true },
                                        icon = Icons.Default.Search,
                                        contentDescription = "Search Vouchers",
                                        tint = colors.textSecondary,
                                        size = 38.dp,
                                        modifier = Modifier.testTag("topbar_search_btn")
                                    )
                                }

                                NeumorphicIconButton(
                                    onClick = { viewModel.setTab(2) },
                                    icon = Icons.Default.Print,
                                    contentDescription = "Printer Status",
                                    tint = if (isPrinterConnected) StatusGreen else colors.textSecondary,
                                    size = 38.dp,
                                    modifier = Modifier.testTag("topbar_printer_btn")
                                )

                                NeumorphicIconButton(
                                    onClick = { showDeviceModeDialog = true },
                                    icon = Icons.Default.Devices,
                                    contentDescription = "Device Mode",
                                    tint = colors.primary,
                                    size = 38.dp,
                                    modifier = Modifier.testTag("topbar_device_mode_btn")
                                )
                            }
                        )
                    }
                },
                bottomBar = {
                    AppBottomNav(
                        selectedTab = selectedTab,
                        onTabSelected = { viewModel.setTab(it) }
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    // Batch print progress bar if active
                    if (printProgress.isPrinting) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(RuijieBlue.copy(alpha = 0.1f))
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = printProgress.statusMessage,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = RuijieBlue,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${printProgress.current} / ${printProgress.total}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = RuijieBlue
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = {
                                    if (printProgress.total > 0) printProgress.current.toFloat() / printProgress.total else 0f
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    when (selectedTab) {
                        1 -> {
                            GenerateVoucherScreen(
                                viewModel = viewModel,
                                onOpenProjectSelector = { showProjectDialog = true },
                                onPreviewVouchers = { previewVouchers = it },
                                onOpenDiagnostics = { showDiagnosticDialog = true }
                            )
                        }
                        2 -> {
                            PrinterScreen(
                                viewModel = viewModel,
                                onOpenPrintDesign = { showDesignDialog = true },
                                onOpenTypography = { showTypographyDialog = true }
                            )
                        }
                        3 -> {
                            SettingsScreen(
                                viewModel = viewModel,
                                onOpenAccounts = { showAccountDialog = true },
                                onOpenApiConfig = { showApiConfigDialog = true },
                                onOpenProjectSelector = { showProjectDialog = true },
                                onOpenPrintDesign = { showDesignDialog = true },
                                onOpenDiagnostics = { showDiagnosticDialog = true },
                                onOpenHistory = { showHistoryDialog = true },
                                onOpenTypography = { showTypographyDialog = true },
                                onOpenSSO = { showSSODialog = true },
                                onOpenSessionDiagnostic = { showRuijieSessionDiagnosticScreen = true },
                                onOpenVoucherInspector = { showVoucherInspectorSettingsDialog = true }
                            )
                        }
                        else -> {
                            PhoneVoucherLayout(
                                viewModel = viewModel,
                                vouchersState = vouchersState,
                                activeFilter = activeFilter,
                                filterCounts = filterCounts,
                                selectedCodes = selectedCodes,
                                searchQuery = searchQuery,
                                currentPage = currentPage,
                                pageSize = pageSize,
                                totalCount = totalCount,
                                onPreviewSingle = { viewModel.selectVoucherForPreview(it) },
                                onPrintSingle = { previewVouchers = listOf(it) },
                                onPrintBatch = { previewVouchers = it },
                                onDeleteSingle = { singleVoucherToDelete = it },
                                onDeleteSelected = { showDeleteSelectedConfirm = true },
                                onDeleteExpired = { showAllExpiredDialog = true }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Standard Phone Single-Column Layout matching Screen 1 in Reference Image:
 * - Direct Status Filter Pills: All | Not Used | Used | Expired
 * - Action toolbar when items selected
 * - Minimalist voucher cards
 * - Pagination bar: 10 / page, Page 1 of 3 < [1] 2 3 >
 */
@Composable
private fun PhoneVoucherLayout(
    viewModel: PrinterV1ViewModel,
    vouchersState: UiState<List<RuijieVoucherItem>>,
    activeFilter: VoucherFilter,
    filterCounts: Map<VoucherFilter, Int>,
    selectedCodes: Set<String>,
    searchQuery: String,
    currentPage: Int,
    pageSize: Int,
    totalCount: Int,
    onPreviewSingle: (RuijieVoucherItem) -> Unit,
    onPrintSingle: (RuijieVoucherItem) -> Unit,
    onPrintBatch: (List<RuijieVoucherItem>) -> Unit,
    onDeleteSingle: (RuijieVoucherItem) -> Unit,
    onDeleteSelected: () -> Unit,
    onDeleteExpired: () -> Unit
) {
    val vouchersList = (vouchersState as? UiState.Success)?.data ?: emptyList()
    val activeListCount = vouchersList.size
    val colors = LocalNeumorphicColors.current
    val voucherDiag by viewModel.latestVoucherDiagnostic.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Direct Status Filter Pills under Top Bar (Screen 1) with real Cloud counts
        StatusFilterBar(
            activeFilter = activeFilter,
            filterCounts = filterCounts,
            hasData = vouchersState is UiState.Success,
            onFilterSelected = { viewModel.setFilter(it) }
        )

        // Safe Diagnostic Metadata Display before/during loading or on error
        if (voucherDiag != null && vouchersState !is UiState.Success) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "Selected project: ${voucherDiag?.selectedProjectName?.ifBlank { "None" }}\nSelected group ID: ${voucherDiag?.selectedGroupIdStr?.ifBlank { "0" }}\nSelected tenant: ${voucherDiag?.selectedTenantName?.ifBlank { "default" }}\nSession state: ${voucherDiag?.sessionState?.ifBlank { "Unknown" }}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = colors.textSecondary
                    )
                }
            }
        }

        // Main List Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            VoucherContentBody(
                viewModel = viewModel,
                vouchersState = vouchersState,
                searchQuery = searchQuery,
                selectedCodes = selectedCodes,
                onPreviewSingle = onPreviewSingle,
                onPrintSingle = onPrintSingle,
                onPrintBatch = onPrintBatch,
                onDeleteSingle = onDeleteSingle,
                onDeleteSelected = onDeleteSelected,
                onDeleteExpired = onDeleteExpired
            )
        }
    }
}

/**
 * Filter Bar: Single-line horizontal scrollable row with Stadium Pills (Screen 1).
 * Strictly prevents vertical wrapping under any circumstances.
 */
@Composable
private fun StatusFilterBar(
    activeFilter: VoucherFilter,
    filterCounts: Map<VoucherFilter, Int> = emptyMap(),
    hasData: Boolean = false,
    onFilterSelected: (VoucherFilter) -> Unit
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        VoucherFilter.entries.forEach { filter ->
            val isSelected = activeFilter == filter
            val count = filterCounts[filter] ?: 0

            NeumorphicFilterChip(
                selected = isSelected,
                onClick = { onFilterSelected(filter) },
                label = filter.displayName,
                count = if (hasData) count else null,
                modifier = Modifier.testTag("filter_chip_${filter.name}")
            )
        }
    }
}

/**
 * Main Voucher Content State & List
 */
@Composable
private fun VoucherContentBody(
    viewModel: PrinterV1ViewModel,
    vouchersState: UiState<List<RuijieVoucherItem>>,
    searchQuery: String,
    selectedCodes: Set<String>,
    onItemClick: ((RuijieVoucherItem) -> Unit)? = null,
    onPreviewSingle: (RuijieVoucherItem) -> Unit,
    onPrintSingle: (RuijieVoucherItem) -> Unit,
    onPrintBatch: (List<RuijieVoucherItem>) -> Unit,
    onDeleteSingle: (RuijieVoucherItem) -> Unit,
    onDeleteSelected: () -> Unit,
    onDeleteExpired: () -> Unit
) {
    when (val state = vouchersState) {
        is UiState.Idle -> {
            EmptyStateCard(
                title = "Vouchers Ready",
                description = "Ruijie Cloud vouchers load automatically.",
                buttonText = "Load Vouchers",
                onAction = { viewModel.loadVouchers(page = 1) }
            )
        }
        is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(modifier = Modifier.size(36.dp), color = RuijieBlue)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Loading Ruijie Cloud Vouchers...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        is UiState.Error -> {
            val colors = LocalNeumorphicColors.current
            val voucherDiag by viewModel.latestVoucherDiagnostic.collectAsState()
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                NeumorphicCard(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = 3.dp,
                    containerColor = colors.surface,
                    borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Failed to load vouchers",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                        if (voucherDiag != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = voucherDiag?.formattedDisplay ?: "",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        NeumorphicButton(
                            onClick = { viewModel.loadVouchers(page = 1) },
                            primary = true,
                            modifier = Modifier.testTag("retry_get_vouchers_btn")
                        ) {
                            Text("Retry", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
        is UiState.Success -> {
            val allVouchers = state.data
            val filteredList = remember(allVouchers, searchQuery) {
                if (searchQuery.isBlank()) {
                    allVouchers
                } else {
                    val q = searchQuery.trim().lowercase()
                    allVouchers.filter {
                        it.effectiveCode.lowercase().contains(q) ||
                                it.effectiveProfile.lowercase().contains(q)
                    }
                }
            }

            if (filteredList.isEmpty()) {
                EmptyStateCard(
                    title = "No Vouchers",
                    description = if (searchQuery.isNotBlank()) "No vouchers match '$searchQuery'." else "No vouchers found for this project.",
                    buttonText = "Refresh",
                    onAction = { viewModel.loadVouchers(page = 1) }
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    val expiredCount = remember(allVouchers) {
                        allVouchers.count { it.normalizedStatus == VoucherStatus.EXPIRED }
                    }

                    // Multi-select and Delete Action Bar
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = selectedCodes.size == filteredList.size && filteredList.isNotEmpty(),
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            viewModel.selectAllVouchers(filteredList)
                                        } else {
                                            viewModel.deselectAllVouchers()
                                        }
                                    },
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (selectedCodes.isNotEmpty()) {
                                        "Selected (${selectedCodes.size})"
                                    } else {
                                        "Select All (${filteredList.size})"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (selectedCodes.isNotEmpty()) {
                                    // Copy selected codes
                                    IconButton(
                                        onClick = { viewModel.copySelectedVouchers() },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy codes", modifier = Modifier.size(16.dp))
                                    }

                                    // Print Selected button
                                    Button(
                                        onClick = {
                                            val selectedVouchers = filteredList.filter { selectedCodes.contains(it.effectiveCode) }
                                            onPrintBatch(selectedVouchers)
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier
                                            .height(32.dp)
                                            .testTag("print_selected_btn")
                                    ) {
                                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Print (${selectedCodes.size})", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                                    }

                                    // Delete Selected button
                                    OutlinedButton(
                                        onClick = onDeleteSelected,
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier
                                            .height(32.dp)
                                            .testTag("delete_selected_btn")
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Delete", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                                    }
                                } else if (expiredCount > 0) {
                                    // Delete Expired button
                                    TextButton(
                                        onClick = onDeleteExpired,
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                        modifier = Modifier.testTag("delete_expired_btn")
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Delete Expired ($expiredCount)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Simplified Voucher Cards List (Screen 1) with Pull-to-Refresh
                    val isRefreshing = vouchersState is UiState.Loading
                    @OptIn(ExperimentalMaterial3Api::class)
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.loadVouchers(page = 1) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("voucher_list"),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredList, key = { it.uuid ?: it.voucherCode ?: it.codeNo ?: it.effectiveCode }) { voucher ->
                                val isSelected = selectedCodes.contains(voucher.effectiveCode)
                                SimplifiedVoucherCard(
                                    voucher = voucher,
                                    isSelected = isSelected,
                                    onToggleSelect = {
                                        viewModel.toggleVoucherSelection(voucher.effectiveCode)
                                        onItemClick?.invoke(voucher)
                                    },
                                    onPreview = { onPreviewSingle(voucher) },
                                    onDelete = { onDeleteSingle(voucher) },
                                    onPrint = { onPrintSingle(voucher) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Voucher Card with 3 responsive icon-only actions:
 * [ Preview 👁 ] [ Delete 🗑 ] [ Print 🖨 ]
 * - Left Checkbox
 * - Middle: Voucher Code + Status Pill, Generated Date, Period
 * - Right: Action icons row strictly icon-only without text labels
 */
@Composable
private fun SimplifiedVoucherCard(
    voucher: RuijieVoucherItem,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onPreview: () -> Unit,
    onDelete: () -> Unit,
    onPrint: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalNeumorphicColors.current

    NeumorphicCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("voucher_card_${voucher.effectiveCode}"),
        shape = RoundedCornerShape(16.dp),
        elevation = if (isSelected) 1.5.dp else 3.dp,
        containerColor = if (isSelected) colors.primary.copy(alpha = 0.08f) else colors.surface,
        borderColor = if (isSelected) colors.primary else colors.border,
        onClick = onToggleSelect
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Checkbox and Voucher Information
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    modifier = Modifier.size(28.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = voucher.effectiveCode,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (isSelected) colors.primary else colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        NeumorphicStatusBadge(status = voucher.normalizedStatus)
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    val dateText = voucher.formattedCreateTime.ifBlank { "Date N/A" }
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = voucher.effectivePeriod,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = StatusGreen,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val quota = voucher.effectiveQuota
                        if (!quota.isNullOrBlank() && quota != "Unlimited") {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodySmall,
                                color = StatusGreen.copy(alpha = 0.7f)
                            )
                            Text(
                                text = quota,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = StatusGreen,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Action icons: [ Preview 👁 ] [ Delete 🗑 ] [ Print 🖨 ]
            // STRICTLY ICONS ONLY. No text labels: Preview, Delete, Print.
            // Horizontal row that fits cleanly on phones and tablets.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 1. Preview 👁
                IconButton(
                    onClick = onPreview,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("preview_voucher_btn_${voucher.effectiveCode}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Preview",
                        tint = colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // 2. Delete 🗑
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("delete_voucher_btn_${voucher.effectiveCode}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // 3. Print 🖨
                IconButton(
                    onClick = onPrint,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("print_voucher_btn_${voucher.effectiveCode}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = "Print",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Status Pill (Screen 1 & 5):
 * NOT USED (Green container + Green text)
 * USED (Blue container + Blue text)
 * EXPIRED (Red container + Red text)
 */
@Composable
private fun StatusPill(status: VoucherStatus) {
    NeumorphicStatusBadge(status = status)
}

/**
 * Pagination Control Bar matching Screen 1 & 5:
 * Left: [ 10 / page v ]
 * Right: Page X of Y  < [ 1 ] 2 3 >
 */
@Composable
private fun PaginationBar(
    currentPage: Int,
    pageSize: Int,
    totalCount: Int,
    onPageSizeChange: (Int) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSelectPage: (Int) -> Unit
) {
    val pageSizes = listOf(10, 20, 30, 50, 100, 200)
    var showPageSizeDropdown by remember { mutableStateOf(false) }
    val totalPages = maxOf(1, (totalCount + pageSize - 1) / pageSize)

    val colors = LocalNeumorphicColors.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.surface,
        border = BorderStroke(0.5.dp, colors.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Page size dropdown selector
            Box {
                Surface(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showPageSizeDropdown = true }
                        .testTag("page_size_btn"),
                    shape = RoundedCornerShape(8.dp),
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.border),
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "$pageSize / page",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                            maxLines = 1
                        )
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = showPageSizeDropdown,
                    onDismissRequest = { showPageSizeDropdown = false }
                ) {
                    pageSizes.forEach { size ->
                        DropdownMenuItem(
                            text = { Text("$size per page", style = MaterialTheme.typography.bodyMedium) },
                            onClick = {
                                onPageSizeChange(size)
                                showPageSizeDropdown = false
                            },
                            leadingIcon = if (pageSize == size) {
                                { Icon(Icons.Default.Check, contentDescription = null, tint = colors.primary) }
                            } else null
                        )
                    }
                }
            }

            // Right: Page indicators: < [1] 2 3 >
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onPrevious,
                    enabled = currentPage > 1,
                    modifier = Modifier.size(32.dp).testTag("prev_page_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous Page",
                        tint = if (currentPage > 1) colors.textPrimary else colors.textSecondary.copy(alpha = 0.35f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Show page pills (up to 3 adjacent pages)
                val startPage = maxOf(1, currentPage - 1)
                val endPage = minOf(totalPages, startPage + 2)
                for (p in startPage..endPage) {
                    val isCurrent = p == currentPage
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelectPage(p) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isCurrent) colors.primary else colors.surface,
                        border = BorderStroke(
                            1.dp,
                            if (isCurrent) colors.primary else colors.border
                        ),
                        shadowElevation = if (isCurrent) 2.dp else 0.5.dp
                    ) {
                        Text(
                            text = "$p",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                            color = if (isCurrent) Color.White else colors.textPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onNext,
                    enabled = currentPage < totalPages,
                    modifier = Modifier.size(32.dp).testTag("next_page_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next Page",
                        tint = if (currentPage < totalPages) colors.textPrimary else colors.textSecondary.copy(alpha = 0.35f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Tablet Voucher Data Table View strictly matching Screen 5 in Reference Image:
 * - Top Bar: Search input + [All Expired] [Print] [Refresh]
 * - Filter Tabs: All (10) | Not Used (6) | Used (3) | Expired (1)
 * - Table Header: [ ] Voucher Code | Profile | Period | Quota | Status | Actions (or Batch Action Buttons when selected)
 * - Table Rows (Full-height scrollable table)
 */
@Composable
private fun TabletVoucherTableView(
    viewModel: PrinterV1ViewModel,
    vouchersState: UiState<List<RuijieVoucherItem>>,
    activeFilter: VoucherFilter,
    filterCounts: Map<VoucherFilter, Int>,
    selectedCodes: Set<String>,
    currentPage: Int,
    pageSize: Int,
    totalCount: Int,
    onPreviewSingle: (RuijieVoucherItem) -> Unit,
    onPrintSingle: (RuijieVoucherItem) -> Unit,
    onPrintBatch: (List<RuijieVoucherItem>) -> Unit,
    onDeleteSingle: (RuijieVoucherItem) -> Unit,
    onDeleteSelected: () -> Unit,
    onDeleteExpired: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val printerState by viewModel.printerConnectionState.collectAsState()
    val isPrinterConnected = printerState is PrinterConnectionState.Connected

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Tablet Top Bar: Search field + Action icons
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Search Input Field - Properly sized to prevent text clipping
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search voucher code or profile...",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            softWrap = false
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.outline) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    textStyle = MaterialTheme.typography.bodyMedium,
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .widthIn(min = 300.dp, max = 420.dp)
                        .testTag("tablet_search_input")
                )

                // Top right actions: All Expired, Print, Refresh
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onDeleteExpired,
                        modifier = Modifier.testTag("tablet_delete_expired_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "All Expired",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    IconButton(onClick = { viewModel.setTab(2) }) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = "Printer",
                            tint = if (isPrinterConnected) StatusGreen else MaterialTheme.colorScheme.outline
                        )
                    }
                    IconButton(onClick = { viewModel.loadVouchers(page = currentPage) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = RuijieBlue)
                    }
                }
            }
        }

        // Filter Pills Row (Screen 5) with real Cloud counts
        val tabletVouchersList = (vouchersState as? UiState.Success)?.data ?: emptyList()
        val activeTabletCount = tabletVouchersList.size

        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VoucherFilter.entries.forEach { filter ->
                    val isSelected = activeFilter == filter
                    val count = filterCounts[filter] ?: 0

                    NeumorphicFilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setFilter(filter) },
                        label = filter.displayName,
                        count = if (vouchersState is UiState.Success) count else null,
                        modifier = Modifier.testTag("tablet_filter_chip_${filter.name}")
                    )
                }
            }
        }

        // Table Content
        when (val state = vouchersState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RuijieBlue)
                }
            }
            is UiState.Error -> {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is UiState.Success -> {
                val allVouchers = state.data
                val filteredList = remember(allVouchers, searchQuery) {
                    if (searchQuery.isBlank()) allVouchers else {
                        val q = searchQuery.trim().lowercase()
                        allVouchers.filter { it.effectiveCode.lowercase().contains(q) || it.effectiveProfile.lowercase().contains(q) }
                    }
                }

                if (filteredList.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No vouchers match the filter or search query.", color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    // Table Header Row (Screen 5)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedCodes.size == filteredList.size && filteredList.isNotEmpty(),
                                onCheckedChange = { checked ->
                                    if (checked) viewModel.selectAllVouchers(filteredList) else viewModel.deselectAllVouchers()
                                },
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            if (selectedCodes.isNotEmpty()) {
                                Text(
                                    text = "Selected (${selectedCodes.size})",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                OutlinedButton(
                                    onClick = onDeleteSelected,
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Delete (${selectedCodes.size})", fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        val selected = allVouchers.filter { selectedCodes.contains(it.effectiveCode) }
                                        onPrintBatch(selected)
                                    },
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(containerColor = RuijieBlue),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Print Selected (${selectedCodes.size})", fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Text("Voucher Code", modifier = Modifier.weight(1.3f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                Text("Profile", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                Text("Period", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                Text("Quota", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                Text("Status", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                Text("Actions", modifier = Modifier.width(120.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                            }
                        }
                    }

                    // Table Rows (Screen 5) with Pull-to-Refresh
                    val isRefreshingTable = vouchersState is UiState.Loading
                    @OptIn(ExperimentalMaterial3Api::class)
                    PullToRefreshBox(
                        isRefreshing = isRefreshingTable,
                        onRefresh = { viewModel.loadVouchers(page = 1) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("voucher_list")
                        ) {
                            items(
                                items = filteredList,
                                key = { it.uuid ?: it.voucherCode ?: it.codeNo ?: it.effectiveCode }
                            ) { v ->
                                val isSelected = selectedCodes.contains(v.effectiveCode)
                                Surface(
                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.toggleVoucherSelection(v.effectiveCode) }
                                        .testTag("tablet_voucher_row_${v.effectiveCode}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { viewModel.toggleVoucherSelection(v.effectiveCode) },
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = v.effectiveCode,
                                            modifier = Modifier.weight(1.3f),
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = v.effectiveProfile,
                                            modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = v.effectivePeriod,
                                            modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = StatusGreen,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = v.effectiveQuota ?: "Unlimited",
                                            modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = StatusGreen,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Box(modifier = Modifier.weight(1f)) {
                                            StatusPill(status = v.normalizedStatus)
                                        }
                                        Row(
                                            modifier = Modifier.width(120.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(
                                                onClick = { onPreviewSingle(v) },
                                                modifier = Modifier.size(36.dp).testTag("preview_voucher_btn_${v.effectiveCode}")
                                            ) {
                                                Icon(Icons.Default.Visibility, contentDescription = "Preview", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
                                            }
                                            IconButton(
                                                onClick = { onDeleteSingle(v) },
                                                modifier = Modifier.size(36.dp).testTag("delete_voucher_btn_${v.effectiveCode}")
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(19.dp))
                                            }
                                            IconButton(
                                                onClick = { onPrintSingle(v) },
                                                modifier = Modifier.size(36.dp).testTag("print_voucher_btn_${v.effectiveCode}")
                                            ) {
                                                Icon(Icons.Default.Print, contentDescription = "Print", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(19.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun EmptyStateCard(
    title: String,
    description: String,
    buttonText: String,
    onAction: () -> Unit
) {
    val colors = LocalNeumorphicColors.current

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        NeumorphicCard(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = 3.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = colors.primary.copy(alpha = 0.12f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                NeumorphicButton(
                    onClick = onAction,
                    primary = true
                ) {
                    Text(buttonText, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun PullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier, content = content)
}
