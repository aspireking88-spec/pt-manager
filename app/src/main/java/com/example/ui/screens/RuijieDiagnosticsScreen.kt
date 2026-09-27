package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.diagnostics.RuijieDiagnosticCollector
import com.example.data.model.DiagnosticResultCategory
import com.example.data.model.ProjectDiagnosticSummary
import com.example.data.model.RuijieRequestDiagnostic
import com.example.data.model.SessionDiagnosticInfo
import com.example.data.model.VoucherGenDiagnosticSummary
import com.example.ui.PrinterV1ViewModel
import kotlinx.coroutines.launch
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RuijieDiagnosticsScreen(
    viewModel: PrinterV1ViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val sessionInfo by RuijieDiagnosticCollector.sessionInfo.collectAsState()
    val projectSummary by RuijieDiagnosticCollector.projectSummary.collectAsState()
    val voucherGenSummary by RuijieDiagnosticCollector.voucherGenSummary.collectAsState()
    val latestOps by RuijieDiagnosticCollector.latestOperationsFlow.collectAsState()
    val requestHistory by RuijieDiagnosticCollector.requestHistory.collectAsState()

    val activeAccount by viewModel.activeAccount.collectAsState()
    val selectedProject by viewModel.selectedProject.collectAsState()

    var isRunningQuickTest by remember { mutableStateOf(false) }
    var saveStatusMessage by remember { mutableStateOf<String?>(null) }

    // SAF Document Creator launcher for exporting TXT report
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
                    saveStatusMessage = "Diagnostic report successfully saved."
                    Toast.makeText(context, "Report saved to file", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    saveStatusMessage = "Failed to save report: ${e.localizedMessage}"
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Ruijie Cloud Diagnostics",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Session telemetry & sync root cause analysis",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("diag_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { triggerShareReport() },
                        modifier = Modifier.testTag("diag_share_action")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Report"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Save status notification banner
            if (saveStatusMessage != null) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = saveStatusMessage ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { saveStatusMessage = null }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Dismiss",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // SECTION 1: Action Controls & Fast Diagnostic Run
            item {
                ActionControlsCard(
                    onExportTxt = { triggerTxtExport() },
                    onShareReport = { triggerShareReport() },
                    onRefreshSession = { viewModel.refreshRuijieSession() },
                    onRefreshAccount = { viewModel.refreshAccountInfo() },
                    onRefreshTenants = { viewModel.refreshAccountInfo() },
                    onRefreshProjects = { viewModel.refreshProjectTree() },
                    onRefreshProfiles = { viewModel.refreshUserGroups() },
                    onRefreshVouchers = { viewModel.loadVouchers(page = 1) },
                    onRunQuickDiagnostics = {
                        scope.launch {
                            isRunningQuickTest = true
                            viewModel.runReadOnlyDiagnostics()
                            isRunningQuickTest = false
                        }
                    },
                    onClearHistory = {
                        RuijieDiagnosticCollector.clearHistory()
                        Toast.makeText(context, "Diagnostic history cleared", Toast.LENGTH_SHORT).show()
                    },
                    isRunningQuickTest = isRunningQuickTest
                )
            }

            // SECTION 2: Safe Recommendation & Assessment
            item {
                SafeRecommendationCard(
                    sessionInfo = sessionInfo,
                    projectSummary = projectSummary,
                    voucherGenSummary = voucherGenSummary,
                    latestOps = latestOps
                )
            }

            // SECTION 3: Session Status
            item {
                SessionStatusCard(sessionInfo = sessionInfo)
            }

            // SECTION 4: Account & Tenant Info
            item {
                AccountTenantCard(
                    activeAccount = activeAccount,
                    projectSummary = projectSummary
                )
            }

            // SECTION 5: Project & Group Status
            item {
                ProjectSummaryCard(
                    selectedProject = selectedProject,
                    projectSummary = projectSummary
                )
            }

            // SECTION 6: Voucher Generation State
            item {
                VoucherGenStatusCard(voucherGenSummary = voucherGenSummary)
            }

            // SECTION 7: Request Results Per Operation
            item {
                Text(
                    text = "Latest Request Telemetry",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            val operationsList = listOf(
                RuijieDiagnosticCollector.OP_ACCOUNT to "Account Info",
                RuijieDiagnosticCollector.OP_TENANT to "Tenant List",
                RuijieDiagnosticCollector.OP_GROUP_TREE to "Group / Project Tree",
                RuijieDiagnosticCollector.OP_USER_GROUPS to "User Groups & Profiles",
                RuijieDiagnosticCollector.OP_VOUCHER_STATUS to "Voucher Status Totals",
                RuijieDiagnosticCollector.OP_VOUCHER_LIST to "Voucher List",
                RuijieDiagnosticCollector.OP_VOUCHER_CREATE to "Voucher Create"
            )

            items(operationsList) { (opKey, opLabel) ->
                val diag = latestOps[opKey]
                OperationTelemetryCard(operationLabel = opLabel, record = diag)
            }

            // SECTION 8: Recent Request Timeline
            if (requestHistory.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent Request Timeline (${requestHistory.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                items(requestHistory.take(10)) { req ->
                    RequestHistoryItem(req = req)
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActionControlsCard(
    onExportTxt: () -> Unit,
    onShareReport: () -> Unit,
    onRefreshSession: () -> Unit,
    onRefreshAccount: () -> Unit,
    onRefreshTenants: () -> Unit,
    onRefreshProjects: () -> Unit,
    onRefreshProfiles: () -> Unit,
    onRefreshVouchers: () -> Unit,
    onRunQuickDiagnostics: () -> Unit,
    onClearHistory: () -> Unit,
    isRunningQuickTest: Boolean
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier.fillMaxWidth().testTag("diag_actions_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Diagnostic Controls & Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Primary Export & Run Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onExportTxt,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).testTag("diag_export_txt_button")
                ) {
                    Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export TXT")
                }

                OutlinedButton(
                    onClick = onShareReport,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).testTag("diag_share_button")
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onRunQuickDiagnostics,
                enabled = !isRunningQuickTest,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().testTag("diag_run_quick_test_button")
            ) {
                if (isRunningQuickTest) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Running telemetry tests...")
                } else {
                    Icon(imageVector = Icons.Default.Sync, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Run Read-Only Diagnostics")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Granular Synchronizations",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(onClick = onRefreshSession, shape = RoundedCornerShape(8.dp)) {
                    Text("Session", fontSize = 12.sp)
                }
                OutlinedButton(onClick = onRefreshAccount, shape = RoundedCornerShape(8.dp)) {
                    Text("Account", fontSize = 12.sp)
                }
                OutlinedButton(onClick = onRefreshTenants, shape = RoundedCornerShape(8.dp)) {
                    Text("Tenants", fontSize = 12.sp)
                }
                OutlinedButton(onClick = onRefreshProjects, shape = RoundedCornerShape(8.dp)) {
                    Text("Projects", fontSize = 12.sp)
                }
                OutlinedButton(onClick = onRefreshProfiles, shape = RoundedCornerShape(8.dp)) {
                    Text("Profiles", fontSize = 12.sp)
                }
                OutlinedButton(onClick = onRefreshVouchers, shape = RoundedCornerShape(8.dp)) {
                    Text("Vouchers", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onClearHistory,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear Logs", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun SafeRecommendationCard(
    sessionInfo: SessionDiagnosticInfo,
    projectSummary: ProjectDiagnosticSummary,
    voucherGenSummary: VoucherGenDiagnosticSummary,
    latestOps: Map<String, RuijieRequestDiagnostic>
) {
    val createOp = latestOps[RuijieDiagnosticCollector.OP_VOUCHER_CREATE]
    val isCode1014 = createOp?.resultCategory == DiagnosticResultCategory.GROUP_NOT_SYNCHRONIZED ||
                     createOp?.ruijieCode == 1014
    val isWrongGroupId = createOp?.resultCategory == DiagnosticResultCategory.WRONG_GROUP_ID ||
                         createOp?.resultCategory == DiagnosticResultCategory.GROUP_ID_ZERO

    val (cardColor, titleColor, icon) = when {
        isCode1014 -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            Icons.Default.CloudSync
        )
        isWrongGroupId || createOp?.resultCategory == DiagnosticResultCategory.PERMISSION_ERROR -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            Icons.Default.Warning
        )
        sessionInfo.authenticatedNavigationDetected && projectSummary.groupIdStr.isNotBlank() -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            Icons.Default.CheckCircle
        )
        else -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Default.Info
        )
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        modifier = Modifier.fillMaxWidth().testTag("diag_recommendation_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = titleColor)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Diagnostic Recommendation",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = titleColor
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            val message = when {
                !sessionInfo.authenticatedNavigationDetected ->
                    "SSO Session is not authenticated. Please tap 'Sign in with Ruijie SSO' and complete login on the official portal."
                projectSummary.groupIdStr.isBlank() || projectSummary.groupIdStr == "0" ->
                    "The selected project group ID is missing or invalid. Please select an active project from the project list."
                isCode1014 ->
                    "Ruijie is still synchronizing this project group. Please refresh projects and wait 15-30 seconds before retrying."
                createOp?.resultCategory == DiagnosticResultCategory.REQUEST_TIMEOUT ->
                    "Ruijie request timed out. Your login session was kept. Check the voucher list after refreshing."
                createOp?.resultCategory == DiagnosticResultCategory.PERMISSION_ERROR ->
                    "This account does not have permission for this project or voucher operation."
                else ->
                    "All diagnostic telemetry indicates healthy session, valid project group (${projectSummary.groupIdStr}), and ready WebView bridge."
            }

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = titleColor
            )
        }
    }
}

@Composable
private fun SessionStatusCard(sessionInfo: SessionDiagnosticInfo) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Session & WebView Bridge",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            DiagKeyValue("Session State", sessionInfo.sessionState)
            DiagKeyValue("SSO Navigation Detected", sessionInfo.authenticatedNavigationDetected.toString())
            DiagKeyValue("WebView Alive", sessionInfo.webViewAlive.toString())
            DiagKeyValue("Bridge Ready", sessionInfo.bridgeReady.toString())
            DiagKeyValue("Cookie Present", sessionInfo.cookiePresent.toString())
            DiagKeyValue("Current Host/Path", sessionInfo.currentWebViewHostPath.ifBlank { "None" })
            DiagKeyValue("Last Auth Check", sessionInfo.formattedLastAuthTime)
            if (!sessionInfo.lastSessionError.isNullOrBlank()) {
                DiagKeyValue("Last Session Error", sessionInfo.lastSessionError ?: "", isError = true)
            }
        }
    }
}

@Composable
private fun AccountTenantCard(
    activeAccount: com.example.data.model.RuijieAccount,
    projectSummary: ProjectDiagnosticSummary
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Account & Tenant",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            DiagKeyValue("Account Name", activeAccount.accountName.ifBlank { "Account 1" })
            DiagKeyValue("Username / Email", (activeAccount.username ?: activeAccount.email ?: "SSO Connected").ifBlank { "SSO Connected" })
            DiagKeyValue("Tenant Name", (projectSummary.tenantName ?: activeAccount.tenantName ?: "Not detected").ifBlank { "Not detected" })
            DiagKeyValue("Tenant ID", projectSummary.tenantId.ifBlank { if (activeAccount.tenantId > 0L) activeAccount.tenantId.toString() else "Not detected" })
            DiagKeyValue("Auth Mode", activeAccount.authMode.name)
        }
    }
}

@Composable
private fun ProjectSummaryCard(
    selectedProject: com.example.data.model.RuijieProject?,
    projectSummary: ProjectDiagnosticSummary
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Project & Group Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            DiagKeyValue("Project Name", selectedProject?.effectiveName ?: projectSummary.projectName.ifBlank { "Not selected" })
            DiagKeyValue("Group ID String", selectedProject?.effectiveGidStr ?: projectSummary.groupIdStr.ifBlank { "Not selected" })
            DiagKeyValue("Numeric Group ID", selectedProject?.effectiveId?.toString() ?: projectSummary.numericGroupId?.toString() ?: "None")
            DiagKeyValue("Parent Group ID", selectedProject?.parentGroupId ?: projectSummary.parentGroupId?.toString() ?: "None")
            DiagKeyValue("Display Path", selectedProject?.displayPath ?: projectSummary.displayPath.ifBlank { "None" })
            DiagKeyValue("Selection Mode", "${projectSummary.selectionSource} (Auto-selected: ${projectSummary.isAutoSelected})")
            DiagKeyValue("Total Projects in Tree", projectSummary.totalProjectsInTree.toString())
            DiagKeyValue("Tree Loaded Time", projectSummary.formattedLoadedTime)
            DiagKeyValue("Exists in Latest Tree", projectSummary.existsInLatestTree.toString())
        }
    }
}

@Composable
private fun VoucherGenStatusCard(voucherGenSummary: VoucherGenDiagnosticSummary) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ListAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Voucher Generation Telemetry",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            DiagKeyValue("Selected User Group", voucherGenSummary.userGroupId.ifBlank { "None" })
            DiagKeyValue("Selected Auth Profile", voucherGenSummary.authProfileId.ifBlank { "None" })
            DiagKeyValue("Quantity", voucherGenSummary.quantity.toString())
            DiagKeyValue("Code Type", voucherGenSummary.codeType.ifBlank { "Numeric" })
            DiagKeyValue("Code Size", voucherGenSummary.codeSize.toString())
            DiagKeyValue("Last Create Time", voucherGenSummary.formattedLastCreateTime)
            DiagKeyValue("Last Response Code", voucherGenSummary.lastCreateCode?.toString() ?: "None")
            DiagKeyValue("Last Message", voucherGenSummary.lastCreateMessage.ifBlank { "None" })
            DiagKeyValue("Last Result Category", voucherGenSummary.lastCreateResultCategory.displayName)
            DiagKeyValue("POST In Progress", voucherGenSummary.isPostInProgress.toString())
            DiagKeyValue("Result Unknown", voucherGenSummary.isCreationResultUnknown.toString())
        }
    }
}

@Composable
private fun OperationTelemetryCard(
    operationLabel: String,
    record: RuijieRequestDiagnostic?
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (record != null && record.resultCategory == DiagnosticResultCategory.SUCCESS)
                MaterialTheme.colorScheme.surfaceContainerHigh
            else if (record != null && record.resultCategory != DiagnosticResultCategory.SUCCESS)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = operationLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (record != null) {
                    val statusColor = when (record.resultCategory) {
                        DiagnosticResultCategory.SUCCESS -> Color(0xFF2E7D32)
                        DiagnosticResultCategory.GROUP_NOT_SYNCHRONIZED -> Color(0xFFE65100)
                        DiagnosticResultCategory.REQUEST_TIMEOUT -> Color(0xFFF57F17)
                        else -> MaterialTheme.colorScheme.error
                    }
                    Text(
                        text = record.resultCategory.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                } else {
                    Text(
                        text = "Idle / Not executed",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (record != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${record.method} ${record.endpoint}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "HTTP ${record.httpStatus} | Code ${record.ruijieCode ?: "-"} | ${record.durationMs}ms",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = record.formattedTime.substringAfter(" "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (record.safeMessage.isNotBlank()) {
                    Text(
                        text = "Message: ${record.safeMessage}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                if (!record.lastError.isNullOrBlank()) {
                    Text(
                        text = "Error: ${record.lastError}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RequestHistoryItem(req: RuijieRequestDiagnostic) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${req.operation} (${req.method})",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = req.endpoint,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "HTTP ${req.httpStatus}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (req.httpStatus in 200..299) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                )
                Text(
                    text = "${req.durationMs}ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DiagKeyValue(
    key: String,
    value: String,
    isError: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = key,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            maxLines = 2
        )
    }
}
