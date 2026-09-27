package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Result classification categories for safe diagnostic logging and user messaging.
 */
enum class DiagnosticResultCategory(val displayName: String, val userActionableMessage: String) {
    SUCCESS(
        "Success",
        "Operation completed successfully."
    ),
    EMPTY(
        "Empty",
        "Operation returned no items or empty result."
    ),
    GROUP_NOT_SYNCHRONIZED(
        "Ruijie Cloud Synchronization In Progress",
        "Ruijie is still synchronizing this project group. Please refresh and try again."
    ),
    WRONG_GROUP_ID(
        "Invalid Group ID",
        "The selected project group ID is missing or invalid."
    ),
    TENANT_ID_AS_GROUP(
        "Tenant ID Used As Group ID",
        "Tenant ID was mistakenly provided instead of Group ID. Please select a project group."
    ),
    GROUP_ID_ZERO(
        "Root Group Selected",
        "Root group (ID 0) selected instead of a specific project group."
    ),
    STALE_PROJECT_SELECTION(
        "Stale Project Selection",
        "The previously selected project group no longer exists. Please re-select a project."
    ),
    PERMISSION_ERROR(
        "Permission Denied",
        "This account does not have permission for this project or voucher operation."
    ),
    SESSION_EXPIRED(
        "Session Expired",
        "Ruijie session expired. Please sign in again."
    ),
    REQUEST_TIMEOUT(
        "Request Timeout",
        "Ruijie request timed out. Your login session was kept. Please retry."
    ),
    NETWORK_ERROR(
        "Network Error",
        "Unable to connect to Ruijie Cloud. Please check your internet connection."
    ),
    PARSER_ERROR(
        "Response Parse Error",
        "Unexpected response format received from Ruijie Cloud."
    ),
    UNKNOWN_ERROR(
        "Unknown Error",
        "An unexpected error occurred while communicating with Ruijie Cloud."
    )
}

/**
 * Safe metadata for an individual Ruijie API / WebProxy request.
 * Strictly excludes passwords, raw cookies, tokens, auth headers, and sensitive HTML.
 */
data class RuijieRequestDiagnostic(
    val timestamp: Long = System.currentTimeMillis(),
    val operation: String,
    val endpoint: String,
    val method: String,
    val httpStatus: Int = 0,
    val contentType: String = "",
    val ruijieCode: Int? = null,
    val safeMessage: String = "",
    val tenantName: String = "",
    val projectName: String = "",
    val groupIdStr: String = "",
    val numericGroupId: Long? = null,
    val userGroupId: String = "",
    val authProfileId: String = "",
    val sessionState: String = "",
    val webViewAlive: Boolean = false,
    val cookiePresent: Boolean = false,
    val bridgeReady: Boolean = false,
    val durationMs: Long = 0L,
    val retryCount: Int = 0,
    val resultCategory: DiagnosticResultCategory = DiagnosticResultCategory.SUCCESS,
    val lastError: String? = null
) {
    val formattedTime: String
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}

/**
 * Event entry for session transition tracking in diagnostic history.
 */
data class SessionDiagnosticEvent(
    val timestamp: Long = System.currentTimeMillis(),
    val operation: String = "",
    val oldState: String = "",
    val newState: String = "",
    val trigger: String = "",
    val endpointPath: String = "",
    val httpStatus: Int = 0,
    val ruijieCode: Int? = null,
    val safeMessage: String = "",
    val actionTaken: String = ""
) {
    val formattedTime: String
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}

/**
 * Snapshot of current Ruijie session status for diagnostics.
 */
data class SessionDiagnosticInfo(
    val sessionState: String = "IDLE",
    val previousSessionState: String = "NONE",
    val lastStateChangeTime: Long = System.currentTimeMillis(),
    val lastStateChangeTrigger: String = "INITIALIZATION",
    val authenticatedNavigationDetected: Boolean = false,
    val currentWebViewHostPath: String = "",
    val webViewAlive: Boolean = false,
    val bridgeReady: Boolean = false,
    val cookiePresent: Boolean = false,
    val lastAuthVerificationTime: Long = 0L,
    val lastSuccessfulApiRequestTime: Long = 0L,
    val lastErrorCategory: String? = null,
    val isUserTriggeredLogout: Boolean = false,
    val isAutoLogout: Boolean = false,
    val logoutType: String = "None",
    val lastSessionError: String? = null
) {
    val formattedLastAuthTime: String
        get() = if (lastAuthVerificationTime > 0L) {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(lastAuthVerificationTime))
        } else {
            "Never"
        }

    val formattedLastSuccessTime: String
        get() = if (lastSuccessfulApiRequestTime > 0L) {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(lastSuccessfulApiRequestTime))
        } else {
            "None"
        }

    val formattedStateChangeTime: String
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(lastStateChangeTime))
}

/**
 * Snapshot of currently selected / auto-detected project for diagnostics.
 */
data class ProjectDiagnosticSummary(
    val projectName: String = "",
    val groupIdStr: String = "",
    val numericGroupId: Long? = null,
    val parentGroupId: Long? = null,
    val tenantName: String = "",
    val tenantId: String = "",
    val displayPath: String = "",
    val isAutoSelected: Boolean = false,
    val selectionSource: String = "Manual",
    val projectTreeLoadedTime: Long = 0L,
    val totalProjectsInTree: Int = 0,
    val existsInLatestTree: Boolean = false
) {
    val formattedLoadedTime: String
        get() = if (projectTreeLoadedTime > 0L) {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(projectTreeLoadedTime))
        } else {
            "Not loaded"
        }
}

/**
 * Snapshot of voucher generation state for diagnostics.
 */
data class VoucherGenDiagnosticSummary(
    val selectedTenant: String = "",
    val targetTenantName: String = "",
    val selectedProject: String = "",
    val targetProjectName: String = "",
    val groupIdStr: String = "",
    val targetGroupIdStr: String = "",
    val numericGroupId: Long? = null,
    val targetNumericGroupId: Long? = null,
    val selectedPackageName: String = "",
    val userGroupId: String = "",
    val selectedUserGroupId: String = "",
    val authProfileId: String = "",
    val selectedAuthProfileId: String = "",
    val quantity: Int = 1,
    val requestedQuantity: Int = 1,
    val codeType: String = "",
    val requestedCodeType: String = "",
    val codeSize: Int = 8,
    val requestedCodeLength: Int = 8,
    val lastAttemptTime: Long = 0L,
    val lastCreateTime: Long = 0L,
    val lastCreateCode: Int? = null,
    val lastCreateMessage: String = "",
    val parsedCreatedCode: String = "",
    val confirmationStatus: String = "None",
    val lastResultCategory: DiagnosticResultCategory? = null,
    val lastCreateResultCategory: DiagnosticResultCategory = DiagnosticResultCategory.SUCCESS,
    val lastResultSummary: String = "",
    val isPostInProgress: Boolean = false,
    val isCreationResultUnknown: Boolean = false
) {
    val effectiveProjectName: String
        get() = targetProjectName.ifBlank { selectedProject }

    val effectiveGroupIdStr: String
        get() = targetGroupIdStr.ifBlank { groupIdStr }

    val effectiveTenantName: String
        get() = targetTenantName.ifBlank { selectedTenant }

    val effectiveUserGroupId: String
        get() = selectedUserGroupId.ifBlank { userGroupId }

    val effectiveAuthProfileId: String
        get() = selectedAuthProfileId.ifBlank { authProfileId }

    val effectiveQuantity: Int
        get() = if (requestedQuantity > 0) requestedQuantity else quantity

    val effectiveCodeType: String
        get() = requestedCodeType.ifBlank { codeType }

    val effectiveCodeLength: Int
        get() = if (requestedCodeLength > 0) requestedCodeLength else codeSize

    val effectiveResultSummary: String
        get() = lastResultSummary.ifBlank { lastCreateMessage }

    val effectiveResultCategory: DiagnosticResultCategory
        get() = lastResultCategory ?: lastCreateResultCategory

    val formattedLastCreateTime: String
        get() {
            val t = if (lastAttemptTime > 0L) lastAttemptTime else lastCreateTime
            return if (t > 0L) {
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(t))
            } else {
                "No generation attempted"
            }
        }
}

/**
 * Saved project preference for persistence in SharedPreferences.
 */
data class SavedProjectPreference(
    val tenantId: String = "",
    val tenantName: String = "",
    val projectName: String = "",
    val groupIdStr: String = "",
    val numericGroupId: Long = 0L,
    val displayPath: String = "",
    val savedTimestamp: Long = System.currentTimeMillis()
) {
    val isValid: Boolean
        get() = groupIdStr.isNotBlank() && groupIdStr != "0" && numericGroupId > 0L
}
