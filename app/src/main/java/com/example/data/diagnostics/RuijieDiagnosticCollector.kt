package com.example.data.diagnostics

import android.os.Build
import com.example.data.model.DiagnosticResultCategory
import com.example.data.model.ProjectDiagnosticSummary
import com.example.data.model.RuijieRequestDiagnostic
import com.example.data.model.SessionDiagnosticInfo
import com.example.data.model.VoucherGenDiagnosticSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe singleton collector for Ruijie API/SSO diagnostic telemetry.
 * Captures request metadata, error classifications, session state, and generates sanitized plain-text reports.
 */
object RuijieDiagnosticCollector {

    const val OP_ACCOUNT = "ACCOUNT"
    const val OP_TENANT = "TENANT"
    const val OP_GROUP_TREE = "GROUP_TREE"
    const val OP_USER_GROUPS = "USER_GROUPS"
    const val OP_VOUCHER_STATUS = "VOUCHER_STATUS"
    const val OP_VOUCHER_LIST = "VOUCHER_LIST"
    const val OP_VOUCHER_CREATE = "VOUCHER_CREATE"

    private const val MAX_HISTORY_SIZE = 60
    private const val MAX_SESSION_EVENTS_SIZE = 20

    private val _requestHistory = MutableStateFlow<List<RuijieRequestDiagnostic>>(emptyList())
    val requestHistory: StateFlow<List<RuijieRequestDiagnostic>> = _requestHistory.asStateFlow()

    private val _sessionEvents = MutableStateFlow<List<com.example.data.model.SessionDiagnosticEvent>>(emptyList())
    val sessionEvents: StateFlow<List<com.example.data.model.SessionDiagnosticEvent>> = _sessionEvents.asStateFlow()

    private val latestByOperation = ConcurrentHashMap<String, RuijieRequestDiagnostic>()
    private val _latestOperationsFlow = MutableStateFlow<Map<String, RuijieRequestDiagnostic>>(emptyMap())
    val latestOperationsFlow: StateFlow<Map<String, RuijieRequestDiagnostic>> = _latestOperationsFlow.asStateFlow()

    private val _sessionInfo = MutableStateFlow(SessionDiagnosticInfo())
    val sessionInfo: StateFlow<SessionDiagnosticInfo> = _sessionInfo.asStateFlow()

    private val _projectSummary = MutableStateFlow(ProjectDiagnosticSummary())
    val projectSummary: StateFlow<ProjectDiagnosticSummary> = _projectSummary.asStateFlow()

    private val _voucherGenSummary = MutableStateFlow(VoucherGenDiagnosticSummary())
    val voucherGenSummary: StateFlow<VoucherGenDiagnosticSummary> = _voucherGenSummary.asStateFlow()

    @Synchronized
    fun recordSessionEvent(event: com.example.data.model.SessionDiagnosticEvent) {
        val currentList = _sessionEvents.value.toMutableList()
        currentList.add(0, event)
        if (currentList.size > MAX_SESSION_EVENTS_SIZE) {
            _sessionEvents.value = currentList.take(MAX_SESSION_EVENTS_SIZE)
        } else {
            _sessionEvents.value = currentList
        }
    }

    /**
     * Record a safe diagnostic entry for a Ruijie request.
     */
    @Synchronized
    fun recordRequest(diagnostic: RuijieRequestDiagnostic) {
        val sanitized = sanitize(diagnostic)
        latestByOperation[sanitized.operation] = sanitized
        _latestOperationsFlow.value = HashMap(latestByOperation)

        val currentList = _requestHistory.value.toMutableList()
        currentList.add(0, sanitized)
        if (currentList.size > MAX_HISTORY_SIZE) {
            _requestHistory.value = currentList.take(MAX_HISTORY_SIZE)
        } else {
            _requestHistory.value = currentList
        }
    }

    /**
     * Update session diagnostic state snapshot.
     */
    fun updateSessionInfo(info: SessionDiagnosticInfo) {
        _sessionInfo.value = info
    }

    /**
     * Update project diagnostic summary snapshot.
     */
    fun updateProjectSummary(summary: ProjectDiagnosticSummary) {
        _projectSummary.value = summary
    }

    /**
     * Update voucher generation summary snapshot.
     */
    fun updateVoucherGenSummary(summary: VoucherGenDiagnosticSummary) {
        _voucherGenSummary.value = summary
    }

    /**
     * Get the latest diagnostic record for a specific operation.
     */
    fun getLatestOperation(operation: String): RuijieRequestDiagnostic? {
        return latestByOperation[operation]
    }

    /**
     * Clear non-sensitive diagnostic history.
     */
    @Synchronized
    fun clearHistory() {
        _requestHistory.value = emptyList()
        latestByOperation.clear()
        _latestOperationsFlow.value = emptyMap()
    }

    /**
     * Classify an error response into a safe actionable category.
     */
    fun classifyResult(
        operation: String,
        httpStatus: Int,
        ruijieCode: Int?,
        message: String?,
        groupIdStr: String?,
        numericGroupId: Long?,
        tenantId: String?,
        isStaleProject: Boolean = false,
        isEmptyResult: Boolean = false
    ): DiagnosticResultCategory {
        val msg = message?.lowercase(Locale.US) ?: ""

        if (isStaleProject) {
            return DiagnosticResultCategory.STALE_PROJECT_SELECTION
        }

        if (httpStatus == 408 || msg.contains("timeout") || ruijieCode == 3) {
            return DiagnosticResultCategory.REQUEST_TIMEOUT
        }

        if (httpStatus == 401 || httpStatus == 403 || msg.contains("permission") || msg.contains("forbidden") || msg.contains("unauthorized")) {
            return DiagnosticResultCategory.PERMISSION_ERROR
        }

        if (msg.contains("session expired") || msg.contains("login timeout") || (msg.contains("sso") && msg.contains("expire"))) {
            return DiagnosticResultCategory.SESSION_EXPIRED
        }

        // Code 1014 or synchronization message analysis
        if (ruijieCode == 1014 || msg.contains("not been synchronized") || msg.contains("not synchronized") || msg.contains("group has not been synchronized")) {
            if (groupIdStr == "0" || numericGroupId == 0L) {
                return DiagnosticResultCategory.GROUP_ID_ZERO
            }
            if (!tenantId.isNullOrBlank() && (groupIdStr == tenantId || numericGroupId?.toString() == tenantId)) {
                return DiagnosticResultCategory.TENANT_ID_AS_GROUP
            }
            return DiagnosticResultCategory.GROUP_NOT_SYNCHRONIZED
        }

        // Verified explicit invalid group error in response message or code
        if (msg.contains("invalid group") || msg.contains("group not found") || msg.contains("group does not exist") ||
            msg.contains("group id error") || msg.contains("group_id is invalid") || msg.contains("invalid groupid")) {
            return DiagnosticResultCategory.WRONG_GROUP_ID
        }

        // Standard HTTP 200 + Code 0/200 success response
        if (httpStatus in 200..299 && (ruijieCode == 0 || ruijieCode == 200 || ruijieCode == null)) {
            return if (isEmptyResult) {
                DiagnosticResultCategory.EMPTY
            } else {
                DiagnosticResultCategory.SUCCESS
            }
        }

        // Explicit group ID zero/missing check ONLY when response failed
        if (groupIdStr.isNullOrBlank() || numericGroupId == null || numericGroupId <= 0L) {
            if (operation in setOf(OP_USER_GROUPS, OP_VOUCHER_STATUS, OP_VOUCHER_LIST, OP_VOUCHER_CREATE)) {
                return if (groupIdStr == "0" || numericGroupId == 0L) {
                    DiagnosticResultCategory.GROUP_ID_ZERO
                } else {
                    DiagnosticResultCategory.WRONG_GROUP_ID
                }
            }
        }

        if (msg.contains("parse") || msg.contains("json") || msg.contains("malformed")) {
            return DiagnosticResultCategory.PARSER_ERROR
        }

        if (httpStatus == 0) {
            return DiagnosticResultCategory.NETWORK_ERROR
        }

        return DiagnosticResultCategory.UNKNOWN_ERROR
    }

    /**
     * Sanitize a diagnostic entry to ensure no passwords, tokens, auth headers, or raw cookies are included.
     */
    private fun sanitize(input: RuijieRequestDiagnostic): RuijieRequestDiagnostic {
        return input.copy(
            safeMessage = redactSecrets(input.safeMessage),
            lastError = input.lastError?.let { redactSecrets(it) },
            endpoint = sanitizeEndpoint(input.endpoint)
        )
    }

    private fun sanitizeEndpoint(endpoint: String): String {
        // Strip out query tokens or sensitive parameters if any
        return endpoint.split("?").firstOrNull() ?: endpoint
    }

    fun redactSecrets(text: String): String {
        var result = text
        // Passwords
        result = result.replace(Regex("(?i)(password|passwd|secret|pwd)\\s*[=:]\\s*[^\\s&,;\"'\\}\\]]+"), "$1=[REDACTED]")
        // Tokens
        result = result.replace(Regex("(?i)(token|access_token|refresh_token|bearer)\\s*[=:]\\s*[^\\s&,;\"'\\}\\]]+"), "$1=[REDACTED]")
        // Cookie values
        result = result.replace(Regex("(?i)(cookie|set-cookie|sessionid|jsessionid)\\s*[=:]\\s*[^\\s&,;\"'\\}\\]]+"), "$1=[REDACTED]")
        // Authorization headers
        result = result.replace(Regex("(?i)Authorization:\\s*[^\\n\\r]+"), "Authorization: [REDACTED]")
        // HTML DOCTYPE or full tag dumps
        if (result.contains("<!DOCTYPE", ignoreCase = true) || result.contains("<html", ignoreCase = true)) {
            result = "[HTML Response Redacted: SSO/Web page detected]"
        }
        return result
    }

    /**
     * Generate plain-text diagnostic report suitable for exporting to a .txt file.
     */
    fun generatePlainTextReport(appVersion: String = "1.0.0", androidVersion: String = Build.VERSION.RELEASE ?: "Unknown"): String {
        val nowFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault()).format(Date())
        val session = _sessionInfo.value
        val project = _projectSummary.value
        val voucherGen = _voucherGenSummary.value
        val history = _requestHistory.value
        val ops = HashMap(latestByOperation)

        val sb = StringBuilder()
        sb.appendLine("===============================================================")
        sb.appendLine("                 RUIJIE DIAGNOSTIC REPORT                     ")
        sb.appendLine("===============================================================")
        sb.appendLine("Generated at:     $nowFormatted")
        sb.appendLine("App version:      $appVersion")
        sb.appendLine("Android version:  $androidVersion (API ${Build.VERSION.SDK_INT})")
        sb.appendLine("Device model:     ${Build.MANUFACTURER} ${Build.MODEL}")
        sb.appendLine()

        // 1. SESSION SECTION
        sb.appendLine("---------------------------------------------------------------")
        sb.appendLine("1. SESSION STATUS")
        sb.appendLine("---------------------------------------------------------------")
        val accountOp = ops[OP_ACCOUNT]
        val tenantOp = ops[OP_TENANT]
        val isSessionActive = session.authenticatedNavigationDetected || session.bridgeReady || session.cookiePresent ||
                (accountOp != null && accountOp.httpStatus in 200..299 && accountOp.ruijieCode == 0) ||
                (tenantOp != null && tenantOp.httpStatus in 200..299 && tenantOp.ruijieCode == 0)
        sb.appendLine("Current session state:     ${if (isSessionActive && session.sessionState.contains("Login", ignoreCase = true)) "Ruijie Cloud Session Ready" else session.sessionState}")
        sb.appendLine("Previous session state:    ${session.previousSessionState}")
        sb.appendLine("Last state change time:    ${session.formattedStateChangeTime}")
        sb.appendLine("Last state change trigger: ${session.lastStateChangeTrigger}")
        sb.appendLine("WebView alive:             ${session.webViewAlive || isSessionActive}")
        sb.appendLine("Bridge ready:              ${session.bridgeReady || isSessionActive}")
        sb.appendLine("Cookie present:            ${session.cookiePresent || isSessionActive}")
        sb.appendLine("Last auth verification:    ${session.formattedLastAuthTime}")
        sb.appendLine("Last successful API req:   ${session.formattedLastSuccessTime}")
        sb.appendLine("Last error category:       ${session.lastErrorCategory ?: "None"}")
        sb.appendLine("Logout type:               ${session.logoutType} (User-triggered: ${session.isUserTriggeredLogout}, Auto: ${session.isAutoLogout})")
        sb.appendLine("Last session error:        ${session.lastSessionError ?: "None"}")
        sb.appendLine()

        // 1B. SESSION EVENTS HISTORY
        sb.appendLine("---------------------------------------------------------------")
        sb.appendLine("1B. RECENT SESSION EVENTS (LAST 20 EVENTS)")
        sb.appendLine("---------------------------------------------------------------")
        val events = _sessionEvents.value
        if (events.isEmpty()) {
            sb.appendLine("No session transition events recorded yet.")
        } else {
            for (evt in events) {
                sb.appendLine("[${evt.formattedTime}] ${evt.oldState} -> ${evt.newState}")
                sb.appendLine("  Trigger:    ${evt.trigger}")
                if (evt.endpointPath.isNotBlank()) sb.appendLine("  Endpoint:   ${evt.endpointPath}")
                if (evt.httpStatus > 0) sb.appendLine("  HTTP/Code:  HTTP ${evt.httpStatus} / Code ${evt.ruijieCode ?: "N/A"}")
                if (evt.safeMessage.isNotBlank()) sb.appendLine("  Message:    ${evt.safeMessage}")
                if (evt.actionTaken.isNotBlank()) sb.appendLine("  Action:     ${evt.actionTaken}")
            }
        }
        sb.appendLine()

        // 2. ACCOUNT AND TENANT SECTION
        sb.appendLine("---------------------------------------------------------------")
        sb.appendLine("2. ACCOUNT AND TENANT")
        sb.appendLine("---------------------------------------------------------------")
        val activeTenantName = project.tenantName
            .ifBlank { com.example.data.api.RuijieWebViewBridge.lastTenantName.value }
            .ifBlank { "Not selected" }
        val activeTenantId = project.tenantId
            .ifBlank { com.example.data.api.RuijieWebViewBridge.lastTenantId.value.takeIf { it != 0L }?.toString() ?: "Not selected" }
        val activeAccountName = accountOp?.tenantName
            .orEmpty()
            .ifBlank { com.example.data.api.RuijieWebViewBridge.lastAccountName.value }
            .ifBlank { "Not detected" }

        sb.appendLine("Account Name:              $activeAccountName")
        sb.appendLine("Tenant Name:               $activeTenantName")
        sb.appendLine("Tenant ID:                 $activeTenantId")
        sb.appendLine("Default/Current Tenant:    ${if (activeTenantName != "Not selected") "Active" else "None"}")
        sb.appendLine()

        // 3. PROJECT SECTION
        sb.appendLine("---------------------------------------------------------------")
        sb.appendLine("3. PROJECT & GROUP DETAILS")
        sb.appendLine("---------------------------------------------------------------")
        sb.appendLine("Project Name:              ${project.projectName.ifBlank { "Not selected" }}")
        sb.appendLine("Group ID String:           ${project.groupIdStr.ifBlank { "Not selected" }}")
        sb.appendLine("Numeric Group ID:          ${project.numericGroupId ?: "None"}")
        sb.appendLine("Parent Group ID:           ${project.parentGroupId ?: "None"}")
        sb.appendLine("Display Path:              ${project.displayPath.ifBlank { "None" }}")
        sb.appendLine("Selection Mode:            ${project.selectionSource} (Auto-selected: ${project.isAutoSelected})")
        sb.appendLine("Project Count in Tree:     ${project.totalProjectsInTree}")
        sb.appendLine("Project Tree Loaded:       ${project.formattedLoadedTime}")
        sb.appendLine("Exists in Latest Tree:     ${project.existsInLatestTree}")
        sb.appendLine()

        // 4. REQUEST RESULTS SECTION
        sb.appendLine("---------------------------------------------------------------")
        sb.appendLine("4. LATEST REQUEST RESULTS PER OPERATION")
        sb.appendLine("---------------------------------------------------------------")
        val standardOps = listOf(
            OP_ACCOUNT to "Account Info",
            OP_TENANT to "Tenant List",
            OP_GROUP_TREE to "Group/Project Tree",
            OP_USER_GROUPS to "User Groups / Profiles",
            OP_VOUCHER_STATUS to "Voucher Status Totals",
            OP_VOUCHER_LIST to "Voucher List",
            OP_VOUCHER_CREATE to "Voucher Create"
        )
        for ((opKey, opLabel) in standardOps) {
            val record = ops[opKey]
            sb.appendLine("[$opLabel]")
            if (record != null) {
                sb.appendLine("  Timestamp:       ${record.formattedTime}")
                sb.appendLine("  Endpoint:        ${record.endpoint}")
                sb.appendLine("  Method:          ${record.method}")
                sb.appendLine("  HTTP Status:     ${record.httpStatus}")
                sb.appendLine("  Content-Type:    ${record.contentType.ifBlank { "None" }}")
                sb.appendLine("  Ruijie Code:     ${record.ruijieCode ?: "None"}")
                sb.appendLine("  Safe Message:    ${record.safeMessage.ifBlank { "None" }}")
                sb.appendLine("  Duration:        ${record.durationMs} ms")
                sb.appendLine("  Retry Count:     ${record.retryCount}")
                sb.appendLine("  Result Category: ${record.resultCategory.displayName}")
                sb.appendLine("  Last Error:      ${record.lastError ?: "None"}")
            } else {
                sb.appendLine("  Status:          No requests recorded for this operation yet.")
            }
            sb.appendLine()
        }

        // 5. VOUCHER GENERATION SECTION
        sb.appendLine("---------------------------------------------------------------")
        sb.appendLine("5. VOUCHER GENERATION PARAMETERS & RECENT ATTEMPT")
        sb.appendLine("---------------------------------------------------------------")
        sb.appendLine("Selected User Group ID:    ${voucherGen.effectiveUserGroupId.ifBlank { "None" }}")
        sb.appendLine("Selected Auth Profile ID:  ${voucherGen.effectiveAuthProfileId.ifBlank { "None" }}")
        sb.appendLine("Quantity:                  ${voucherGen.quantity}")
        sb.appendLine("Code Type:                 ${voucherGen.codeType.ifBlank { "Numeric" }}")
        sb.appendLine("Code Size:                 ${voucherGen.codeSize}")
        sb.appendLine("Last Create Time:          ${voucherGen.formattedLastCreateTime}")
        sb.appendLine("Last Create Code:          ${voucherGen.lastCreateCode ?: "None"}")
        sb.appendLine("Last Create Message:       ${voucherGen.lastCreateMessage.ifBlank { "None" }}")
        sb.appendLine("Last Create Category:      ${voucherGen.lastCreateResultCategory.displayName}")
        sb.appendLine("POST In Progress:          ${voucherGen.isPostInProgress}")
        sb.appendLine("Creation Result Unknown:   ${voucherGen.isCreationResultUnknown}")
        sb.appendLine()

        // 6. SAFE RECOMMENDATION
        sb.appendLine("---------------------------------------------------------------")
        sb.appendLine("6. DIAGNOSTIC ASSESSMENT & RECOMMENDATION")
        sb.appendLine("---------------------------------------------------------------")
        val recommendation = computeRecommendation(session, project, voucherGen, ops)
        sb.appendLine(recommendation)
        sb.appendLine()

        // 7. RECENT REQUEST LOG (LAST 15 ENTRIES)
        sb.appendLine("---------------------------------------------------------------")
        sb.appendLine("7. RECENT REQUEST TIMELINE (LAST ${history.size.coerceAtMost(15)} EVENTS)")
        sb.appendLine("---------------------------------------------------------------")
        for (item in history.take(15)) {
            sb.appendLine("[${item.formattedTime}] ${item.operation} -> ${item.method} ${item.endpoint} (HTTP ${item.httpStatus}, Code ${item.ruijieCode ?: "-"}) -> ${item.resultCategory.displayName}")
            if (!item.lastError.isNullOrBlank()) {
                sb.appendLine("    Error: ${item.lastError}")
            }
        }
        sb.appendLine()
        sb.appendLine("===============================================================")
        sb.appendLine("                 END OF DIAGNOSTIC REPORT                      ")
        sb.appendLine("===============================================================")

        return redactSecrets(sb.toString())
    }

    private fun computeRecommendation(
        session: SessionDiagnosticInfo,
        project: ProjectDiagnosticSummary,
        voucherGen: VoucherGenDiagnosticSummary,
        ops: Map<String, RuijieRequestDiagnostic>
    ): String {
        val accountOp = ops[OP_ACCOUNT]
        val tenantOp = ops[OP_TENANT]
        val isSessionActive = session.authenticatedNavigationDetected || session.bridgeReady || session.cookiePresent ||
                (accountOp != null && accountOp.httpStatus == 200 && (accountOp.ruijieCode == 0 || accountOp.ruijieCode == 200)) ||
                (tenantOp != null && tenantOp.httpStatus == 200 && (tenantOp.ruijieCode == 0 || tenantOp.ruijieCode == 200)) ||
                session.sessionState in setOf(
                    "Ruijie Cloud Session Ready", "Account Verified", "Projects Loaded", "Live Ruijie Cloud (Active)"
                )

        if (!isSessionActive) {
            return "Assessment: Official SSO session is not active or bridge is disconnected.\n" +
                   "Action: Tap 'Sign in with Ruijie SSO' and complete authentication on the official login page."
        }

        if (project.projectName.isBlank() || project.groupIdStr.isBlank() || project.numericGroupId == null || project.numericGroupId <= 0L) {
            return "Assessment: No valid project group selected (Group ID is missing, 0, or invalid).\n" +
                   "Action: Tap 'Select Project' to choose an active project group before generating vouchers."
        }

        val createDiag = ops[OP_VOUCHER_CREATE]
        if (createDiag != null) {
            when (createDiag.resultCategory) {
                DiagnosticResultCategory.GROUP_NOT_SYNCHRONIZED -> {
                    return "Assessment: Ruijie Cloud returned Code 1014 (Group has not been synchronized).\n" +
                           "Root Cause: Ruijie Cloud backend synchronization for group '${project.projectName}' (ID: ${project.numericGroupId}) is still pending or recently modified on cloud.\n" +
                           "Action: Tap 'Refresh Projects' to synchronize the group tree with Ruijie Cloud, wait 15-30 seconds, then retry voucher generation."
                }
                DiagnosticResultCategory.WRONG_GROUP_ID, DiagnosticResultCategory.GROUP_ID_ZERO -> {
                    return "Assessment: Invalid Group ID (${project.groupIdStr}) sent in voucher generation request.\n" +
                           "Action: Re-select the project group from the Project list to update the numeric group ID."
                }
                DiagnosticResultCategory.TENANT_ID_AS_GROUP -> {
                    return "Assessment: Tenant ID was mistakenly sent as Group ID.\n" +
                           "Action: Select a specific project group under the tenant, not the tenant root."
                }
                DiagnosticResultCategory.REQUEST_TIMEOUT -> {
                    return "Assessment: The generation request timed out waiting for Ruijie Cloud.\n" +
                           "Action: Your login session was kept intact. Check the voucher list after refreshing to see if the voucher was created on cloud."
                }
                DiagnosticResultCategory.PERMISSION_ERROR -> {
                    return "Assessment: Account does not have write/voucher creation permissions for this project group.\n" +
                           "Action: Verify your Ruijie Cloud account role (Admin/Voucher Operator) on the Ruijie portal."
                }
                DiagnosticResultCategory.SESSION_EXPIRED -> {
                    return "Assessment: Ruijie Cloud session expired.\n" +
                           "Action: Sign in again via the official Ruijie SSO page to renew the authentication cookie."
                }
                DiagnosticResultCategory.SUCCESS -> {
                    return "Assessment: Last voucher generation succeeded on Ruijie Cloud."
                }
                else -> {}
            }
        }

        if (voucherGen.userGroupId.isBlank() || voucherGen.authProfileId.isBlank()) {
            return "Assessment: User Group or Authentication Profile not selected.\n" +
                   "Action: Select a user group and package/profile on the Generate Voucher screen."
        }

        return "Assessment: All parameters and session states appear valid.\n" +
               "Action: You can proceed with voucher generation."
    }
}
