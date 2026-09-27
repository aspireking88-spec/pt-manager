package com.example.data.model

import com.squareup.moshi.JsonClass

typealias Voucher = RuijieVoucherItem
typealias VoucherPackage = RuijiePackageItem
typealias RuijieTenant = RuijieProject
typealias RuijieNetworkGroup = RuijieProject

@JsonClass(generateAdapter = true)
data class RuijieApiResponse<T>(
    val code: Int = 0,
    val msg: String? = null,
    val message: String? = null,
    val data: T? = null,
    val success: Boolean? = null
) {
    val isSuccess: Boolean
        get() = success == true || code == 0 || code == 200
}

@JsonClass(generateAdapter = true)
data class RuijieTokenData(
    val accessToken: String? = null,
    val token: String? = null,
    val refreshToken: String? = null
) {
    val effectiveToken: String?
        get() = accessToken?.ifBlank { null } ?: token?.ifBlank { null }
}

enum class RuijieBridgeErrorCode(val code: Int, val userFriendlyMessage: String) {
    SESSION_EXPIRED(401, "Session expired. Please log in again."),
    PERMISSION_DENIED(403, "Permission denied."),
    RUIJIE_PERMISSION_ERROR(403, "Ruijie permission error."),
    HTTP_ERROR(500, "Server error occurred."),
    HTML_RESPONSE(502, "Invalid HTML response received."),
    REQUEST_TIMEOUT(408, "Request timed out."),
    GROUP_NOT_SYNCHRONIZED(404, "Network group not synchronized."),
    MALFORMED_RESPONSE(400, "Malformed response received."),
    WEBVIEW_NOT_READY(503, "WebView is not ready."),
    JS_INJECTION_TIMEOUT(409, "JavaScript execution timed out."),
    UNKNOWN(-1, "An unknown error occurred.")
}

class RuijieGroupNotSynchronizedException(message: String = "Network group not synchronized") : Exception(message)

data class RuijieBridgeResponse(
    val requestId: String = "",
    val httpStatus: Int = 200,
    val contentType: String? = null,
    val rawBody: String? = null,
    val body: String? = rawBody,
    val currentUrl: String? = null,
    val currentWebViewUrl: String? = null,
    val error: String? = null,
    val errorCode: RuijieBridgeErrorCode? = null,
    val isSuccess: Boolean = false,
    val success: Boolean = isSuccess,
    val data: Any? = null,
    val responseMsg: String? = null,
    val hasSessionCookie: Boolean = false,
    val isWebViewAttached: Boolean = false,
    val elapsedTimeMs: Long = 0L,
    val isBridgeReady: Boolean = false,
    val jsCallbackReceived: Boolean = false
)

data class RuijieSafeLog(
    val timestamp: Long = System.currentTimeMillis(),
    val requestPath: String = "",
    val httpMethod: String = "",
    val httpStatus: Int = 200,
    val contentType: String? = null,
    val responseCode: Int? = null,
    val responseMsg: String? = null,
    val currentWebViewUrl: String? = null,
    val hasSessionCookie: Boolean = false,
    val isWebViewAttached: Boolean = false,
    val elapsedTimeMs: Long = 0L,
    val requestId: String? = null,
    val errorCode: RuijieBridgeErrorCode? = null,
    val isSuccess: Boolean = false,
    val isBridgeReady: Boolean = false,
    val jsCallbackReceived: Boolean = false
)

enum class RuijieSessionState(val isConnected: Boolean, val displayName: String) {
    RESTORING_SESSION(false, "Restoring Session"),
    LOGIN_REQUIRED(false, "Login Required"),
    SESSION_EXPIRED(false, "Session Expired"),
    SESSION_VERIFYING(true, "Verifying Session"),
    AUTHENTICATED_NAVIGATION_DETECTED(true, "Authenticated Navigation"),
    SESSION_READY(true, "Session Ready"),
    ACCOUNT_VERIFIED(true, "Account Verified"),
    ACCOUNT_LOADED(true, "Account Loaded"),
    LIVE_READY(true, "Live Ready"),
    ERROR(false, "Error"),
    TEMPORARY_ERROR(false, "Temporary Error"),
    LOGIN_FAILED(false, "Login Failed"),
    SSO_PAGE(false, "SSO Page"),
    WEBVIEW_LOADING(false, "Loading WebView"),
    PROJECTS_LOADING(true, "Loading Projects"),
    VOUCHERS_LOADING(true, "Loading Vouchers"),
    LOGGING_OUT(false, "Logging Out")
}
