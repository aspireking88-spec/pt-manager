package com.example.data.model

data class RuijieSessionDiagnosticReport(
    val generatedAt: String,
    val session: SessionRestoreDiagnosticInfo,
    val restore: RestoreDiagnosticInfo,
    val lifecycle: LifecycleDiagnosticInfo
)

data class SessionRestoreDiagnosticInfo(
    val currentState: String,
    val previousState: String,
    val lastStateChangeTime: String,
    val lastStateChangeTrigger: String,
    val isWebViewAlive: Boolean,
    val isWebViewAttached: Boolean,
    val isBridgeReady: Boolean,
    val cookiePresent: Boolean,
    val cookieFlushAttempted: Boolean,
    val lastAccountVerificationTime: String,
    val lastSuccessfulApiTime: String,
    val lastSessionError: String,
    val logoutType: String // "user-triggered", "verified server expiry", "automatic/false logout", "not logged out"
)

data class RestoreDiagnosticInfo(
    val appProcessStartTime: String,
    val webViewRestoreStart: String,
    val webViewRestoreComplete: String,
    val accountVerificationStart: String,
    val accountVerificationResult: String,
    val restoreDurationMs: Long,
    val loginUiShown: Boolean,
    val reasonLoginUiShown: String
)

data class LifecycleDiagnosticInfo(
    val lastBackgroundTime: String,
    val lastForegroundTime: String,
    val activityRecreationDetected: Boolean,
    val webViewRecreationDetected: Boolean,
    val cookiesCleared: Boolean,
    val clearCookiesTrigger: String,
    val selectedProjectRestored: Boolean
)

data class LoginErrorEvent(
    val eventTime: String,
    val trigger: String,
    val sessionStateBefore: String,
    val sessionStateAfter: String,
    val webViewAlive: Boolean,
    val webViewAttached: Boolean,
    val cookiePresent: Boolean,
    val lastAccountVerificationResult: String,
    val httpStatus: Int,
    val ruijieCode: Int,
    val safeErrorCategory: String,
    val exactReason: String,
    val cookiesCleared: Boolean,
    val cookieClearTrigger: String,
    val requestId: String
)
