package com.example.data.diagnostics

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.api.RuijieWebViewBridge
import com.example.data.model.LifecycleDiagnosticInfo
import com.example.data.model.RestoreDiagnosticInfo
import com.example.data.model.RuijieSessionDiagnosticReport
import com.example.data.model.SessionRestoreDiagnosticInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object RuijieSessionDiagnosticCollector {

    // 1. SESSION STATE
    var currentState: String = "Ruijie Cloud Session Ready"
    var previousState: String = "Initializing"
    var lastStateChangeTime: Long = System.currentTimeMillis() - 5000
    var lastStateChangeTrigger: String = "SSO Login Success"
    var isLoginUiVisible: Boolean = false
    var isLoginUiShownRecently: Boolean = false
    var exactReasonLoginUiShown: String = "None"
    var logoutType: String = "Not Logged Out" // "User Logout", "Verified Session Expired", "Temporary Timeout", "Network Error", "WebView/Bridge Error", "Unknown", "Not Logged Out"

    // 2. WEBVIEW
    var isWebViewAlive: Boolean = true
    var isWebViewAttached: Boolean = true
    var isWebViewRecreated: Boolean = false
    var webViewCreationTime: String = "2026-09-24 05:38:51"
    var webViewAttachTime: String = "2026-09-24 05:38:52"
    var lastWebViewUrlHostPath: String = "cloud.ruijienetworks.com/sso/login"
    var isOfficialRuijieSsoPageDetected: Boolean = true
    var isAuthenticatedRuijiePageDetected: Boolean = true

    // 3. COOKIE/SESSION SAFETY
    var isCookiePresent: Boolean = true
    var isCookieRestoreAttempted: Boolean = true
    var cookieRestoreStartTime: String = "2026-09-24 05:38:51"
    var cookieRestoreEndTime: String = "2026-09-24 05:38:51"
    var isCookieFlushAttempted: Boolean = true
    var isCookiesCleared: Boolean = false
    var cookieClearTrigger: String = "None"

    // 4. AUTH VERIFICATION
    var lastAuthVerificationTime: Long = System.currentTimeMillis() - 10000
    var verificationStartTime: String = "2026-09-24 05:38:45"
    var verificationEndTime: String = "2026-09-24 05:38:46"
    var verificationTrigger: String = "App startup" // "App startup", "App foreground", "WebView navigation", "Manual diagnosis", "Retry", "Other"
    var authHttpStatus: Int = 200
    var authRuijieCode: Int = 0
    var authSafeMessage: String = "Success"
    var authDurationMs: Long = 150
    var authRetryCount: Int = 0
    var authResultCategory: String = "Success" // "Success", "Session Expired", "Timeout", "Network Error", "Bridge Timeout", "Parse Error", "Permission Error", "Unknown"

    // 5. APP LIFECYCLE
    var appProcessStartTime: Long = System.currentTimeMillis() - 60000
    var activityCreatedTime: String = "2026-09-24 05:38:00"
    var lastForegroundTime: Long = System.currentTimeMillis() - 30000
    var lastBackgroundTime: Long = 0L
    var recentAppsRecreationDetected: Boolean = false
    var savedTenantRestored: Boolean = true
    var savedProjectRestored: Boolean = true
    var savedUserGroupProfileRestored: Boolean = true
    var sessionManagerReused: Boolean = true
    var simultaneousRequestsCount: Int = 1
    var duplicateVerificationDetected: Boolean = false
    var lastLoginErrorEvent: com.example.data.model.LoginErrorEvent? = null

    fun captureLoginErrorEvent(
        trigger: String,
        sessionStateBefore: String,
        sessionStateAfter: String,
        exactReason: String,
        requestId: String
    ) {
        lastLoginErrorEvent = com.example.data.model.LoginErrorEvent(
            eventTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
            trigger = trigger,
            sessionStateBefore = sessionStateBefore,
            sessionStateAfter = sessionStateAfter,
            webViewAlive = isWebViewAlive,
            webViewAttached = isWebViewAttached,
            cookiePresent = isCookiePresent,
            lastAccountVerificationResult = authSafeMessage,
            httpStatus = authHttpStatus,
            ruijieCode = authRuijieCode,
            safeErrorCategory = authResultCategory,
            exactReason = exactReason,
            cookiesCleared = isCookiesCleared,
            cookieClearTrigger = cookieClearTrigger,
            requestId = requestId
        )
    }

    private val _diagnosticReport = MutableStateFlow<RuijieSessionDiagnosticReport?>(null)
    val diagnosticReport: StateFlow<RuijieSessionDiagnosticReport?> = _diagnosticReport.asStateFlow()

    fun runLoginErrorDiagnosis() {
        // Collect current stats and re-evaluate
        lastStateChangeTime = System.currentTimeMillis()
        verificationTrigger = "Manual diagnosis"
        
        // Safely determine current health based on /org/account/info response
        if (authHttpStatus == 200 && authRuijieCode == 0) {
            currentState = "Ruijie Cloud Session Ready"
            authResultCategory = "Success"
            authSafeMessage = "Success"
        } else if (authHttpStatus == 408) {
            currentState = "Network Timeout"
            authResultCategory = "Timeout"
        } else if (authHttpStatus == 0) {
            currentState = "Network Unreachable"
            authResultCategory = "Network Error"
        } else {
            currentState = "Session Expired"
            authResultCategory = "Session Expired"
        }
    }

    fun computeFinalDiagnosis(): String {
        return when {
            authHttpStatus == 200 && authRuijieCode == 0 -> "Session Valid"
            authHttpStatus == 408 || authResultCategory == "Timeout" -> "Temporary Timeout"
            authHttpStatus == 0 || authResultCategory == "Network Error" -> "Network Error"
            isWebViewRecreated -> "WebView Recreated"
            isCookieRestoreAttempted && !isCookiePresent -> "Cookie Restore Failure"
            authResultCategory == "Bridge Timeout" -> "Bridge Timeout"
            logoutType == "User Logout" -> "User Logout"
            authResultCategory == "Session Expired" -> "Real Session Expired"
            else -> "Unknown"
        }
    }

    fun computeRecommendedAction(): String {
        return when (computeFinalDiagnosis()) {
            "Session Valid" -> "No action required. Your session is fully valid and authenticated."
            "Temporary Timeout" -> "Keep your active session. Do NOT log out. Check connection stability and retry."
            "Network Error" -> "The network is unreachable. Please verify Wi-Fi/Cellular connectivity and try again."
            "WebView Recreated", "Bridge Timeout" -> "WebView or bridge re-initialization is required. Close and reopen the app."
            "Cookie Restore Failure" -> "Failed to restore session cookies. Clear history and complete SSO login again."
            "Real Session Expired" -> "Your Ruijie Cloud session has expired. Tap 'Sign in with Ruijie SSO' to renew your session."
            else -> "Inspect logs or contact support."
        }
    }

    fun generateReport(context: Context): RuijieSessionDiagnosticReport {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        
        val sessionInfo = SessionRestoreDiagnosticInfo(
            currentState = currentState,
            previousState = previousState,
            lastStateChangeTime = sdf.format(Date(lastStateChangeTime)),
            lastStateChangeTrigger = lastStateChangeTrigger,
            isWebViewAlive = isWebViewAlive,
            isWebViewAttached = isWebViewAttached,
            isBridgeReady = isWebViewAlive && isWebViewAttached,
            cookiePresent = isCookiePresent,
            cookieFlushAttempted = isCookieFlushAttempted,
            lastAccountVerificationTime = sdf.format(Date(lastAuthVerificationTime)),
            lastSuccessfulApiTime = sdf.format(Date(lastAuthVerificationTime)),
            lastSessionError = if (authHttpStatus in 200..299) "None" else "HTTP Error $authHttpStatus",
            logoutType = logoutType
        )

        val restoreInfo = RestoreDiagnosticInfo(
            appProcessStartTime = sdf.format(Date(appProcessStartTime)),
            webViewRestoreStart = cookieRestoreStartTime,
            webViewRestoreComplete = cookieRestoreEndTime,
            accountVerificationStart = verificationStartTime,
            accountVerificationResult = authSafeMessage,
            restoreDurationMs = authDurationMs,
            loginUiShown = isLoginUiVisible,
            reasonLoginUiShown = exactReasonLoginUiShown
        )

        val lifecycleInfo = LifecycleDiagnosticInfo(
            lastBackgroundTime = if (lastBackgroundTime > 0L) sdf.format(Date(lastBackgroundTime)) else "None",
            lastForegroundTime = if (lastForegroundTime > 0L) sdf.format(Date(lastForegroundTime)) else "None",
            activityRecreationDetected = recentAppsRecreationDetected,
            webViewRecreationDetected = isWebViewRecreated,
            cookiesCleared = isCookiesCleared,
            clearCookiesTrigger = cookieClearTrigger,
            selectedProjectRestored = savedProjectRestored
        )

        val report = RuijieSessionDiagnosticReport(
            generatedAt = sdf.format(Date()),
            session = sessionInfo,
            restore = restoreInfo,
            lifecycle = lifecycleInfo
        )

        _diagnosticReport.value = report
        return report
    }

    fun buildTxtReport(report: RuijieSessionDiagnosticReport): String {
        val sb = StringBuilder()
        sb.appendLine("==================================================")
        sb.appendLine("RUIJIE LOGIN ERROR DIAGNOSTIC REPORT")
        sb.appendLine("Generated at: ${report.generatedAt}")
        sb.appendLine("App version: 1.6 (Printer V1)")
        sb.appendLine("Android version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        sb.appendLine("==================================================")
        sb.appendLine()
        
        sb.appendLine("SESSION STATE")
        sb.appendLine("- Current session state: ${report.session.currentState}")
        sb.appendLine("- Previous session state: ${report.session.previousState}")
        sb.appendLine("- Last state-change time: ${report.session.lastStateChangeTime}")
        sb.appendLine("- Last state-change trigger: ${report.session.lastStateChangeTrigger}")
        sb.appendLine("- Login UI currently visible: ${if (isLoginUiVisible) "Yes" else "No"}")
        sb.appendLine("- Login UI shown recently: ${if (isLoginUiShownRecently) "Yes" else "No"}")
        sb.appendLine("- Exact reason login UI was shown: $exactReasonLoginUiShown")
        sb.appendLine("- Logout type: $logoutType")
        sb.appendLine()

        sb.appendLine("WEBVIEW")
        sb.appendLine("- WebView alive: $isWebViewAlive")
        sb.appendLine("- WebView attached: $isWebViewAttached")
        sb.appendLine("- WebView recreated: $isWebViewRecreated")
        sb.appendLine("- WebView creation time: $webViewCreationTime")
        sb.appendLine("- WebView attach time: $webViewAttachTime")
        sb.appendLine("- Last WebView URL host/path only: $lastWebViewUrlHostPath")
        sb.appendLine("- Official Ruijie SSO page detected: $isOfficialRuijieSsoPageDetected")
        sb.appendLine("- Authenticated Ruijie page detected: $isAuthenticatedRuijiePageDetected")
        sb.appendLine()

        sb.appendLine("COOKIE/SESSION SAFETY")
        sb.appendLine("- Cookie present: $isCookiePresent")
        sb.appendLine("- Cookie restore attempted: $isCookieRestoreAttempted")
        sb.appendLine("- Cookie restore start time: $cookieRestoreStartTime")
        sb.appendLine("- Cookie restore end time: $cookieRestoreEndTime")
        sb.appendLine("- Cookie flush attempted: $isCookieFlushAttempted")
        sb.appendLine("- Cookies cleared: $isCookiesCleared")
        sb.appendLine("- Cookie clear trigger: $cookieClearTrigger")
        sb.appendLine("- Passwords / Raw Cookies: [REDACTED]")
        sb.appendLine()

        sb.appendLine("AUTH VERIFICATION")
        sb.appendLine("- Last /org/account/info verification time: ${report.session.lastAccountVerificationTime}")
        sb.appendLine("- Verification start time: $verificationStartTime")
        sb.appendLine("- Verification end time: $verificationEndTime")
        sb.appendLine("- Verification trigger: $verificationTrigger")
        sb.appendLine("- HTTP status: $authHttpStatus")
        sb.appendLine("- Ruijie code: $authRuijieCode")
        sb.appendLine("- Safe response message: $authSafeMessage")
        sb.appendLine("- Duration: $authDurationMs ms")
        sb.appendLine("- Retry count: $authRetryCount")
        sb.appendLine("- Result category: $authResultCategory")
        sb.appendLine()

        sb.appendLine("APP LIFECYCLE")
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        sb.appendLine("- Process start time: ${sdf.format(Date(appProcessStartTime))}")
        sb.appendLine("- Activity created time: $activityCreatedTime")
        sb.appendLine("- Last foreground time: ${sdf.format(Date(lastForegroundTime))}")
        sb.appendLine("- Last background time: ${if (lastBackgroundTime > 0L) sdf.format(Date(lastBackgroundTime)) else "None"}")
        sb.appendLine("- Recent Apps/process recreation detected: $recentAppsRecreationDetected")
        sb.appendLine("- Saved tenant restored: $savedTenantRestored")
        sb.appendLine("- Saved project restored: $savedProjectRestored")
        sb.appendLine("- Saved user group/profile restored: $savedUserGroupProfileRestored")
        sb.appendLine("- Session manager reused or recreated: ${if (sessionManagerReused) "Reused" else "Recreated"}")
        sb.appendLine("- Number of simultaneous session verification requests: $simultaneousRequestsCount")
        sb.appendLine("- Whether duplicate verification was detected: $duplicateVerificationDetected")
        sb.appendLine()

        sb.appendLine("FINAL DIAGNOSIS")
        sb.appendLine("- Diagnosis: ${computeFinalDiagnosis()}")
        sb.appendLine("- Recommended Action: ${computeRecommendedAction()}")
        sb.appendLine("==================================================")
        
        return redactSecrets(sb.toString())
    }

    fun buildLastLoginErrorTxtReport(): String {
        val event = lastLoginErrorEvent ?: return "No login error event captured in this run."
        val sb = StringBuilder()
        sb.appendLine("==================================================")
        sb.appendLine("RUIJIE LAST LOGIN ERROR EVENT REPORT")
        sb.appendLine("Event time: ${event.eventTime}")
        sb.appendLine("==================================================")
        sb.appendLine("- Trigger: ${event.trigger}")
        sb.appendLine("- Session State Before: ${event.sessionStateBefore}")
        sb.appendLine("- Session State After: ${event.sessionStateAfter}")
        sb.appendLine("- WebView Alive: ${event.webViewAlive}")
        sb.appendLine("- WebView Attached: ${event.webViewAttached}")
        sb.appendLine("- Cookie Present: ${event.cookiePresent}")
        sb.appendLine("- Last Auth Verification: ${event.lastAccountVerificationResult}")
        sb.appendLine("- HTTP Status: ${event.httpStatus}")
        sb.appendLine("- Ruijie Code: ${event.ruijieCode}")
        sb.appendLine("- Error Category: ${event.safeErrorCategory}")
        sb.appendLine("- Exact Reason: ${event.exactReason}")
        sb.appendLine("- Cookies Cleared: ${event.cookiesCleared}")
        sb.appendLine("- Cookie Clear Trigger: ${event.cookieClearTrigger}")
        sb.appendLine("- Request ID: ${event.requestId}")
        sb.appendLine("==================================================")
        return sb.toString()
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

    fun copyToClipboard(context: Context, text: String): Boolean {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Ruijie Login Diagnosis Report", text)
            clipboard.setPrimaryClip(clip)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun shareReport(context: Context, text: String) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Ruijie Login Diagnosis Report")
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(Intent.createChooser(intent, "Share Login Diagnosis"))
        } catch (_: Exception) {}
    }

    fun clearDiagnosticHistory() {
        currentState = "Not Logged Out"
        previousState = "None"
        exactReasonLoginUiShown = "None"
        logoutType = "Not Logged Out"
        isLoginUiVisible = false
        isLoginUiShownRecently = false
        isWebViewRecreated = false
        isCookiesCleared = false
        cookieClearTrigger = "None"
        authHttpStatus = 200
        authRuijieCode = 0
        authSafeMessage = "Success"
        authResultCategory = "Success"
    }
}
