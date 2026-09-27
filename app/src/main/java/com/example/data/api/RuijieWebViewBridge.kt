package com.example.data.api

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.data.model.RuijieBridgeErrorCode
import com.example.data.model.RuijieBridgeResponse
import com.example.data.model.RuijieSafeLog
import com.example.data.model.RuijieSessionState
import com.example.data.model.SessionDiagnosticEvent
import com.example.data.model.SessionDiagnosticInfo
import com.example.data.diagnostics.RuijieDiagnosticCollector
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@SuppressLint("StaticFieldLeak", "SetJavaScriptEnabled")
object RuijieWebViewBridge {

    private const val TAG = "RuijieWebBridge"
    private const val TIMEOUT_MS = 15000L

    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Main)

    // Persistent retained WebView instance that survives Compose dialog open/dismiss
    private var persistentWebView: WebView? = null
    private val pendingRequests = ConcurrentHashMap<String, CompletableDeferred<RuijieBridgeResponse>>()
    private val verificationMutex = kotlinx.coroutines.sync.Mutex()
    private var lastVerificationAttemptTime = 0L
    private var verificationJob: kotlinx.coroutines.Deferred<Boolean>? = null

    private val _sessionState = MutableStateFlow(RuijieSessionState.RESTORING_SESSION)
    val sessionState: StateFlow<RuijieSessionState> = _sessionState.asStateFlow()

    init {
        // Record process start
        RuijieDiagnosticCollector.recordSessionEvent(SessionDiagnosticEvent(
            timestamp = System.currentTimeMillis(),
            operation = "APP_START",
            actionTaken = "Process initialized"
        ))

        // Start session restoration check
        scope.launch {
            RuijieDiagnosticCollector.recordSessionEvent(SessionDiagnosticEvent(
                timestamp = System.currentTimeMillis(),
                operation = "COOKIE_RESTORE_START",
                actionTaken = "Checking WebView cookies"
            ))
            
            Log.d(TAG, "Starting session restoration check...")
            val hasCookies = hasValidCookieSession()
            
            RuijieDiagnosticCollector.recordSessionEvent(SessionDiagnosticEvent(
                timestamp = System.currentTimeMillis(),
                operation = "COOKIE_RESTORE_END",
                actionTaken = if (hasCookies) "Cookies present" else "No cookies found"
            ))

            if (hasCookies) {
                Log.i(TAG, "Valid cookies found. Attempting to restore session.")
                // We'll let the WebViewClient handle the navigation and verification
                // when it hits the authenticated URL
            } else {
                Log.i(TAG, "No cookies found. Requiring login.")
                updateSessionState(RuijieSessionState.LOGIN_REQUIRED, trigger = "Initial Startup - No Cookies")
            }
        }
    }

    private val _currentUrl = MutableStateFlow(RuijieProtocol.SSO_LOGIN_URL)
    val currentUrl: StateFlow<String> = _currentUrl.asStateFlow()

    private val _diagnosticLogs = MutableStateFlow<List<RuijieSafeLog>>(emptyList())
    val diagnosticLogs: StateFlow<List<RuijieSafeLog>> = _diagnosticLogs.asStateFlow()

    private val _lastAccountName = MutableStateFlow("")
    val lastAccountName: StateFlow<String> = _lastAccountName.asStateFlow()

    private val _lastTenantName = MutableStateFlow("")
    val lastTenantName: StateFlow<String> = _lastTenantName.asStateFlow()

    private val _lastTenantId = MutableStateFlow(0L)
    val lastTenantId: StateFlow<Long> = _lastTenantId.asStateFlow()

    var onSessionVerifiedCallback: (() -> Unit)? = null

    fun updateSessionState(
        newState: RuijieSessionState,
        trigger: String = "System Update",
        endpointPath: String = "",
        httpStatus: Int = 0,
        ruijieCode: Int? = null,
        safeMessage: String = "",
        actionTaken: String = ""
    ) {
        val oldState = _sessionState.value
        if (oldState == newState && safeMessage.isBlank()) return

        _sessionState.value = newState
        val now = System.currentTimeMillis()
        Log.i(TAG, "Ruijie Session Transition: [${oldState.displayName}] -> [${newState.displayName}] Trigger: $trigger Action: $actionTaken")

        // Record session event history entry
        val sessionEvent = SessionDiagnosticEvent(
            timestamp = now,
            operation = "SESSION_CHANGE",
            oldState = oldState.displayName,
            newState = newState.displayName,
            trigger = trigger,
            endpointPath = endpointPath,
            httpStatus = httpStatus,
            ruijieCode = ruijieCode,
            safeMessage = safeMessage,
            actionTaken = actionTaken
        )
        RuijieDiagnosticCollector.recordSessionEvent(sessionEvent)

        val currentInfo = RuijieDiagnosticCollector.sessionInfo.value
        val isUserLogout = trigger.contains("User", ignoreCase = true) && newState == RuijieSessionState.LOGIN_REQUIRED
        val isAuto = newState == RuijieSessionState.SESSION_EXPIRED || (newState == RuijieSessionState.LOGIN_REQUIRED && !isUserLogout)

        val updatedInfo = currentInfo.copy(
            sessionState = newState.displayName,
            previousSessionState = oldState.displayName,
            lastStateChangeTime = now,
            lastStateChangeTrigger = trigger,
            authenticatedNavigationDetected = newState.isConnected || newState == RuijieSessionState.AUTHENTICATED_NAVIGATION_DETECTED || newState == RuijieSessionState.SESSION_READY || newState == RuijieSessionState.ACCOUNT_VERIFIED || newState == RuijieSessionState.ACCOUNT_LOADED,
            currentWebViewHostPath = sanitizeUrlForLog(_currentUrl.value),
            webViewAlive = isWebViewAttached(),
            bridgeReady = isWebViewAttached() && hasValidCookieSession(),
            cookiePresent = hasValidCookieSession(),
            lastAuthVerificationTime = if (newState.isConnected && (newState == RuijieSessionState.SESSION_READY || newState == RuijieSessionState.ACCOUNT_VERIFIED || newState == RuijieSessionState.ACCOUNT_LOADED || newState == RuijieSessionState.LIVE_READY)) now else currentInfo.lastAuthVerificationTime,
            lastErrorCategory = if (!newState.isConnected && newState != RuijieSessionState.LOGIN_REQUIRED) newState.displayName else currentInfo.lastErrorCategory,
            isUserTriggeredLogout = isUserLogout || currentInfo.isUserTriggeredLogout,
            isAutoLogout = isAuto || currentInfo.isAutoLogout,
            logoutType = when {
                isUserLogout -> "User-Triggered"
                isAuto -> "Automatic"
                else -> currentInfo.logoutType
            },
            lastSessionError = if (newState == RuijieSessionState.ERROR || newState == RuijieSessionState.SESSION_EXPIRED || newState == RuijieSessionState.LOGIN_FAILED) newState.displayName else currentInfo.lastSessionError
        )
        RuijieDiagnosticCollector.updateSessionInfo(updatedInfo)
    }

    fun isWebViewAttached(): Boolean = persistentWebView != null

    fun hasValidCookieSession(): Boolean {
        val cookieManager = CookieManager.getInstance()
        val cookie1 = cookieManager.getCookie("https://cloud.ruijienetworks.com")
        val cookie2 = cookieManager.getCookie("https://cloud-as.ruijienetworks.com")
        return !cookie1.isNullOrBlank() || !cookie2.isNullOrBlank()
    }

    /**
     * Obtains or creates the retained persistent WebView instance.
     * Prevents recreation on Compose recomposition.
     */
    fun getOrCreatePersistentWebView(context: Context): WebView {
        persistentWebView?.let { existing ->
            // Detach from previous parent if needed before re-attaching in Compose AndroidView
            (existing.parent as? ViewGroup)?.removeView(existing)
            return existing
        }

        val appContext = context.applicationContext
        val webView = WebView(appContext).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        setupWebView(webView)
        persistentWebView = webView
        webView.loadUrl(RuijieProtocol.SSO_LOGIN_URL)
        return webView
    }

    /**
     * Initializes the Android WebView with security-hardened settings and bridge interface.
     */
    fun attachWebView(webView: WebView, context: Context) {
        if (persistentWebView !== webView) {
            persistentWebView = webView
            setupWebView(webView)
        }
    }

    private fun setupWebView(webView: WebView) {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            // Prevent SSO from refusing embedded webviews
            userAgentString = userAgentString.replace("; wv", "")
        }

        webView.addJavascriptInterface(BridgeJsInterface(), "RuijieBridgeInterface")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                val safeUrl = url ?: ""
                _currentUrl.value = safeUrl
                if (RuijieProtocol.isLoginPage(safeUrl)) {
                    if (!_sessionState.value.isConnected) {
                        updateSessionState(RuijieSessionState.SSO_PAGE, trigger = "Navigation to SSO Page")
                    }
                } else {
                    if (!_sessionState.value.isConnected) {
                        updateSessionState(RuijieSessionState.WEBVIEW_LOADING, trigger = "Navigation to Admin Area")
                    }
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                val safeUrl = url ?: ""
                _currentUrl.value = safeUrl
                handleUrlChanged(safeUrl)
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    val desc = error?.description?.toString() ?: "Unknown WebView error"
                    Log.w(TAG, "WebView error on main frame: $desc")
                    if (!_sessionState.value.isConnected) {
                        updateSessionState(RuijieSessionState.ERROR, trigger = "WebView MainFrame Error", safeMessage = desc)
                    }
                }
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
                if (request?.isForMainFrame == true) {
                    val code = errorResponse?.statusCode ?: 0
                    Log.w(TAG, "WebView HTTP error on main frame: $code")
                }
            }
        }

        Log.d(TAG, "Persistent WebView setup completed.")
    }

    private fun handleUrlChanged(url: String) {
        Log.d(TAG, "WebView navigation changed: ${sanitizeUrlForLog(url)}")

        if (RuijieProtocol.isAuthenticatedUrl(url)) {
            Log.i(TAG, "Authenticated Ruijie Cloud area reached: ${sanitizeUrlForLog(url)}")
            updateSessionState(RuijieSessionState.AUTHENTICATED_NAVIGATION_DETECTED, trigger = "Admin Navigation Detected", actionTaken = "Triggering session verification")
            scope.launch {
                verifySessionAndSync("Navigation Detected")
            }
        } else if (RuijieProtocol.isLoginPage(url)) {
            if (!_sessionState.value.isConnected) {
                updateSessionState(RuijieSessionState.SSO_PAGE, trigger = "SSO Login Page Detected")
            }
        }
    }

    /**
     * Silently re-verifies session if necessary (e.g. on app foregrounding or before critical operation).
     */
    suspend fun reverifySessionIfNeeded(trigger: String = "Silent Check"): Boolean {
        val lastAuth = RuijieDiagnosticCollector.sessionInfo.value.lastAuthVerificationTime
        val now = System.currentTimeMillis()
        if (_sessionState.value.isConnected && (now - lastAuth) < 60_000L) {
            Log.d(TAG, "Session re-verification skipped: verified ${(now - lastAuth) / 1000}s ago ($trigger).")
            return true
        }
        return verifySessionAndSync(trigger = trigger)
    }

    /**
     * Verifies the live session by executing smallest authenticated WebProxy request.
     * State transition: AUTHENTICATED_NAVIGATION_DETECTED -> SESSION_VERIFYING -> ACCOUNT_VERIFIED
     */
    suspend fun verifySessionAndSync(trigger: String = "Verification Request"): Boolean {
        val now = System.currentTimeMillis()
        if ((now - lastVerificationAttemptTime) < 30_000L) {
            Log.d(TAG, "Session verification skipped: recently attempted ${(now - lastVerificationAttemptTime) / 1000}s ago ($trigger).")
            return _sessionState.value.isConnected
        }

        val existingJob = verificationMutex.withLock {
            verificationJob
        }
        if (existingJob != null) {
            Log.d(TAG, "Session verification already in progress ($trigger). Reusing flight.")
            return existingJob.await()
        }

        val newJob = scope.async(Dispatchers.Main) {
            try {
                lastVerificationAttemptTime = System.currentTimeMillis()
                performVerification(trigger)
            } finally {
                verificationMutex.withLock {
                    verificationJob = null
                }
            }
        }

        verificationMutex.withLock {
            verificationJob = newJob
        }

        return newJob.await()
    }
    
    private suspend fun performVerification(trigger: String): Boolean {
        Log.d(TAG, "Starting Ruijie Cloud Session verification via WebProxy bridge ($trigger)...")

        if (!isWebViewAttached()) {
            Log.w(TAG, "Session verification deferred: WebView not attached ($trigger).")
            if (!_sessionState.value.isConnected) {
                updateSessionState(RuijieSessionState.WEBVIEW_LOADING, trigger = trigger, actionTaken = "Waiting for WebView attach")
            } else {
                updateSessionState(RuijieSessionState.TEMPORARY_ERROR, trigger = trigger, safeMessage = "WebView not attached", actionTaken = "Preserving session while detached")
            }
            return false
        }

        val hasCookie = hasValidCookieSession()
        if (!hasCookie && !_sessionState.value.isConnected) {
            Log.w(TAG, "Session verification deferred: Cookie unavailable and not currently connected ($trigger).")
            updateSessionState(RuijieSessionState.LOGIN_REQUIRED, trigger = trigger, actionTaken = "Cookie missing, login required")
            return false
        }

        RuijieDiagnosticCollector.recordSessionEvent(SessionDiagnosticEvent(
            timestamp = System.currentTimeMillis(),
            operation = "ACCOUNT_VERIFICATION_START",
            actionTaken = "Calling /org/account/info"
        ))
        updateSessionState(RuijieSessionState.SESSION_VERIFYING, trigger = trigger, actionTaken = "Calling /org/account/info")

        // Step 1: Account info verification (smallest authenticated request)
        val accountEnvelope = RuijieProtocol.buildAccountInfoEnvelope()
        val response = executeWebProxyRequest(accountEnvelope)

        RuijieDiagnosticCollector.recordSessionEvent(SessionDiagnosticEvent(
            timestamp = System.currentTimeMillis(),
            operation = "ACCOUNT_VERIFICATION_END",
            actionTaken = "Result: ${if (response.isSuccess) "SUCCESS" else "FAILURE: ${response.error}"}",
            httpStatus = response.httpStatus,
            ruijieCode = response.errorCode?.ordinal
        ))

        if (response.isSuccess && !response.rawBody.isNullOrBlank()) {
            val (accName, tenantName, tenantIdStr) = RuijieProtocol.parseAccountInfo(response.rawBody)
            if (!accName.isNullOrBlank()) _lastAccountName.value = accName
            if (!tenantName.isNullOrBlank() && tenantName != "default") _lastTenantName.value = tenantName
            val tenantIdLong = tenantIdStr?.toLongOrNull() ?: 0L
            if (tenantIdLong != 0L) _lastTenantId.value = tenantIdLong

            // Always fetch tenant list to select active/default tenant if tenantName is blank or missing
            val tenantEnvelope = RuijieProtocol.buildTenantListEnvelope()
            val tenantResp = executeWebProxyRequest(tenantEnvelope)
            if (tenantResp.isSuccess && !tenantResp.rawBody.isNullOrBlank()) {
                val tenantItems = RuijieProtocol.parseTenantListItems(tenantResp.rawBody)
                if (tenantItems.isNotEmpty()) {
                    val activeTenant = tenantItems.firstOrNull { it.isCurrent || it.isDefault } ?: tenantItems.first()
                    val activeTenantName = activeTenant.tenantName
                    if (!activeTenantName.isNullOrBlank() && activeTenantName != "default") {
                        _lastTenantName.value = activeTenantName
                        _lastTenantId.value = activeTenant.tenantId ?: 0L
                    }
                }
            }

            if (_lastTenantName.value.isBlank() && !accName.isNullOrBlank()) {
                _lastTenantName.value = accName
            }

            // Flush cookie manager upon successful verification
            try {
                CookieManager.getInstance().flush()
            } catch (_: Exception) {}

            updateSessionState(RuijieSessionState.ACCOUNT_VERIFIED, trigger = trigger, actionTaken = "Session verified successfully")
            Log.i(TAG, "Session verification SUCCEEDED! User: ${_lastAccountName.value} Tenant: ${_lastTenantName.value}")
            onSessionVerifiedCallback?.invoke()
            return true
        }

        // Fallback: Tenant list verification
        val tenantEnvelope = RuijieProtocol.buildTenantListEnvelope()
        val tenantResp = executeWebProxyRequest(tenantEnvelope)
        if (tenantResp.isSuccess && !tenantResp.rawBody.isNullOrBlank()) {
            val tenantItems = RuijieProtocol.parseTenantListItems(tenantResp.rawBody)
            if (tenantItems.isNotEmpty()) {
                val activeTenant = tenantItems.firstOrNull { it.isCurrent || it.isDefault } ?: tenantItems.first()
                val fallbackTenantName = activeTenant.tenantName
                if (!fallbackTenantName.isNullOrBlank() && fallbackTenantName != "default") {
                    _lastTenantName.value = fallbackTenantName
                    _lastTenantId.value = activeTenant.tenantId ?: 0L
                }
            }

            try {
                CookieManager.getInstance().flush()
            } catch (_: Exception) {}

            updateSessionState(RuijieSessionState.ACCOUNT_VERIFIED, trigger = trigger, actionTaken = "Session verified via tenant fallback")
            Log.i(TAG, "Session verification SUCCEEDED via Tenant fallback! Tenant: ${_lastTenantName.value}")
            onSessionVerifiedCallback?.invoke()
            return true
        }

        Log.w(TAG, "Session verification FAILED: ${response.error} [errorCode: ${response.errorCode}]")
        val isVerifiedExpiry = response.errorCode == RuijieBridgeErrorCode.SESSION_EXPIRED
        if (isVerifiedExpiry) {
            updateSessionState(
                newState = RuijieSessionState.SESSION_EXPIRED,
                trigger = trigger,
                httpStatus = response.httpStatus,
                safeMessage = response.error ?: "Session expired",
                actionTaken = "Verified session expired from cloud response"
            )
            return false
        } else {
            // For network errors, timeouts, or temporary bridge issues: PRESERVE SESSION!
            updateSessionState(
                newState = RuijieSessionState.TEMPORARY_ERROR,
                trigger = trigger,
                httpStatus = response.httpStatus,
                safeMessage = response.error ?: "Temporary request failure",
                actionTaken = "Preserving session during network/bridge error"
            )
            return true
        }
    }

    /**
     * Injects a fetch call into the authenticated WebView to communicate with Ruijie WebProxy.
     * Enforces: 15-second timeout, request cleanup, error classification, safe privacy logging.
     */
    suspend fun executeWebProxyRequest(
        envelope: String,
        timeoutMs: Long = TIMEOUT_MS
    ): RuijieBridgeResponse {
        val envelopeJson = try { JSONObject(envelope) } catch (_: Exception) { JSONObject() }
        return executeWebProxyRequestJson(envelopeJson, timeoutMs)
    }

    private suspend fun executeWebProxyRequestJson(
        envelope: JSONObject,
        timeoutMs: Long = TIMEOUT_MS
    ): RuijieBridgeResponse {
        val startTime = System.currentTimeMillis()
        val webView = persistentWebView
        val apiPath = envelope.optString("api", "")
        val fullUrl = RuijieProtocol.buildWebProxyUrl(apiPath)
        val method = envelope.optString("method", "POST")

        if (webView == null) {
            val errLog = RuijieSafeLog(
                timestamp = startTime,
                requestPath = apiPath,
                httpMethod = method,
                httpStatus = 0,
                contentType = "",
                responseCode = null,
                responseMsg = RuijieBridgeErrorCode.WEBVIEW_NOT_READY.userFriendlyMessage,
                currentWebViewUrl = sanitizeUrlForLog(_currentUrl.value),
                hasSessionCookie = hasValidCookieSession(),
                isWebViewAttached = false,
                elapsedTimeMs = 0L,
                requestId = "",
                errorCode = RuijieBridgeErrorCode.WEBVIEW_NOT_READY,
                isSuccess = false,
                isBridgeReady = false,
                jsCallbackReceived = false
            )
            recordDiagnosticLog(errLog)
            return RuijieBridgeResponse(
                requestId = "",
                httpStatus = 0,
                contentType = "",
                rawBody = null,
                error = RuijieBridgeErrorCode.WEBVIEW_NOT_READY.userFriendlyMessage,
                errorCode = RuijieBridgeErrorCode.WEBVIEW_NOT_READY,
                elapsedTimeMs = 0L,
                jsCallbackReceived = false,
                isSuccess = false
            )
        }

        val requestId = UUID.randomUUID().toString()
        val deferred = CompletableDeferred<RuijieBridgeResponse>()
        pendingRequests[requestId] = deferred

        val escapedEnvelope = JSONObject.quote(envelope.toString())
        val escapedUrl = JSONObject.quote(fullUrl)
        val script = """
            (function() {
                var reqId = '$requestId';
                try {
                    var targetUrl = $escapedUrl;
                    var envelopeObj = JSON.parse($escapedEnvelope);
                    fetch(targetUrl, {
                        method: 'POST',
                        credentials: 'include',
                        headers: {
                            'Content-Type': 'application/json',
                            'Accept': 'application/json, text/plain, */*'
                        },
                        body: JSON.stringify(envelopeObj)
                    }).then(function(res) {
                        var status = res.status;
                        var cType = res.headers.get('content-type') || '';
                        return res.text().then(function(body) {
                            if (window.RuijieBridgeInterface) {
                                window.RuijieBridgeInterface.onResponse(reqId, status, cType, body);
                            }
                        });
                    }).catch(function(err) {
                        if (window.RuijieBridgeInterface) {
                            window.RuijieBridgeInterface.onError(reqId, (err && err.message) ? err.message : 'Network fetch failed');
                        }
                    });
                } catch(e) {
                    if (window.RuijieBridgeInterface) {
                        window.RuijieBridgeInterface.onError(reqId, (e && e.message) ? e.message : 'Script execution error');
                    }
                }
            })();
        """.trimIndent()

        mainHandler.post {
            webView.evaluateJavascript(script, null)
        }

        val result = withTimeoutOrNull(timeoutMs) {
            deferred.await()
        }

        // Ensure pending request is cleaned up
        pendingRequests.remove(requestId)
        val elapsed = System.currentTimeMillis() - startTime

        if (result == null) {
            val timeoutLog = RuijieSafeLog(
                timestamp = System.currentTimeMillis(),
                requestPath = apiPath,
                httpMethod = method,
                httpStatus = 408,
                contentType = "",
                responseCode = 408,
                responseMsg = RuijieBridgeErrorCode.REQUEST_TIMEOUT.userFriendlyMessage,
                currentWebViewUrl = sanitizeUrlForLog(_currentUrl.value),
                hasSessionCookie = hasValidCookieSession(),
                isWebViewAttached = true,
                elapsedTimeMs = elapsed,
                requestId = requestId,
                errorCode = RuijieBridgeErrorCode.REQUEST_TIMEOUT,
                isSuccess = false,
                isBridgeReady = true,
                jsCallbackReceived = false
            )
            recordDiagnosticLog(timeoutLog)
            return RuijieBridgeResponse(
                requestId = requestId,
                httpStatus = 408,
                contentType = "",
                rawBody = null,
                error = RuijieBridgeErrorCode.REQUEST_TIMEOUT.userFriendlyMessage,
                errorCode = RuijieBridgeErrorCode.REQUEST_TIMEOUT,
                elapsedTimeMs = elapsed,
                jsCallbackReceived = false,
                isSuccess = false
            )
        }

        // Validate and log response safely
        val (errorCode, errorMsg) = RuijieProtocol.classifyError(
            httpStatus = result.httpStatus,
            contentType = result.contentType,
            body = result.rawBody,
            url = _currentUrl.value
        )

        val isSuccess = result.httpStatus in 200..299 &&
                result.error == null &&
                (errorCode == null || result.rawBody?.contains("\"code\":0") == true || result.rawBody?.contains("\"code\":200") == true)

        val finalResponse = result.copy(
            elapsedTimeMs = elapsed,
            errorCode = if (isSuccess) null else errorCode,
            error = if (isSuccess) null else errorMsg,
            isSuccess = isSuccess
        )

        val safeLog = RuijieSafeLog(
            timestamp = System.currentTimeMillis(),
            requestPath = apiPath,
            httpMethod = method,
            httpStatus = result.httpStatus,
            contentType = result.contentType,
            responseCode = extractResponseCode(result.rawBody),
            responseMsg = if (isSuccess) "OK" else errorMsg,
            currentWebViewUrl = sanitizeUrlForLog(_currentUrl.value),
            hasSessionCookie = hasValidCookieSession(),
            isWebViewAttached = true,
            elapsedTimeMs = elapsed,
            requestId = requestId,
            errorCode = if (isSuccess) null else errorCode,
            isSuccess = isSuccess,
            isBridgeReady = true,
            jsCallbackReceived = true
        )
        recordDiagnosticLog(safeLog)

        if (isSuccess) {
            val currentInfo = RuijieDiagnosticCollector.sessionInfo.value
            RuijieDiagnosticCollector.updateSessionInfo(
                currentInfo.copy(lastSuccessfulApiRequestTime = System.currentTimeMillis())
            )
            if (_sessionState.value == RuijieSessionState.TEMPORARY_ERROR || _sessionState.value == RuijieSessionState.SESSION_VERIFYING) {
                updateSessionState(RuijieSessionState.ACCOUNT_VERIFIED, trigger = "API Request Success", endpointPath = apiPath, actionTaken = "Restored Account Verified state after successful API call")
            }
        } else if (errorCode == RuijieBridgeErrorCode.SESSION_EXPIRED) {
            updateSessionState(
                newState = RuijieSessionState.SESSION_EXPIRED,
                trigger = "API Session Expired",
                endpointPath = apiPath,
                httpStatus = result.httpStatus,
                safeMessage = errorMsg,
                actionTaken = "Setting Session Expired state from cloud API response"
            )
        }

        return finalResponse
    }

    private fun extractResponseCode(rawBody: String?): Int? {
        if (rawBody.isNullOrBlank()) return null
        return try {
            val json = JSONObject(rawBody)
            if (json.has("code")) json.optInt("code") else null
        } catch (_: Exception) {
            null
        }
    }

    private fun recordDiagnosticLog(log: RuijieSafeLog) {
        val current = _diagnosticLogs.value.toMutableList()
        if (current.size >= 50) current.removeAt(0)
        current.add(log)
        _diagnosticLogs.value = current
        Log.d(TAG, "WebProxy Log [${log.httpMethod} ${log.requestPath}] Status: ${log.httpStatus} Code: ${log.responseCode} Msg: ${log.responseMsg}")

        val pathLower = log.requestPath.lowercase(java.util.Locale.US)
        val opType = when {
            pathLower.contains("account/info") || pathLower.contains("org/account/info") -> RuijieDiagnosticCollector.OP_ACCOUNT
            pathLower.contains("tenant/list") || pathLower.contains("account/tenant") || pathLower.contains("org/account/tenant") -> RuijieDiagnosticCollector.OP_TENANT
            pathLower.contains("group/tree") || pathLower.contains("group/all/tree") || pathLower.contains("grouptree") -> RuijieDiagnosticCollector.OP_GROUP_TREE
            pathLower.contains("usergroup") || pathLower.contains("intlsamusergroup") -> RuijieDiagnosticCollector.OP_USER_GROUPS
            pathLower.contains("getstatus") || pathLower.contains("samvoucher/getstatus") -> RuijieDiagnosticCollector.OP_VOUCHER_STATUS
            pathLower.contains("getlist") || pathLower.contains("samvoucher/getlist") -> RuijieDiagnosticCollector.OP_VOUCHER_LIST
            pathLower.contains("create") || pathLower.contains("samvoucher/create") -> RuijieDiagnosticCollector.OP_VOUCHER_CREATE
            else -> "OTHER"
        }

        val pathGroupIdStr = Regex(".*[=/]([0-9]+)$").find(log.requestPath)?.groupValues?.get(1)
            ?: Regex("group_id=([0-9]+)").find(log.requestPath)?.groupValues?.get(1)
            ?: RuijieDiagnosticCollector.projectSummary.value.groupIdStr
        val pathNumericGroupId = pathGroupIdStr.toLongOrNull()
            ?: RuijieDiagnosticCollector.projectSummary.value.numericGroupId

        val resultCategory = RuijieDiagnosticCollector.classifyResult(
            operation = opType,
            httpStatus = log.httpStatus,
            ruijieCode = log.responseCode,
            message = log.responseMsg,
            groupIdStr = pathGroupIdStr,
            numericGroupId = pathNumericGroupId,
            tenantId = _lastTenantId.value.takeIf { it > 0L }?.toString()
        )

        RuijieDiagnosticCollector.recordRequest(
            com.example.data.model.RuijieRequestDiagnostic(
                timestamp = log.timestamp,
                operation = opType,
                endpoint = log.requestPath,
                method = log.httpMethod,
                httpStatus = log.httpStatus,
                contentType = log.contentType ?: "",
                ruijieCode = log.responseCode,
                safeMessage = log.responseMsg ?: "",
                tenantName = _lastTenantName.value,
                groupIdStr = pathGroupIdStr,
                numericGroupId = pathNumericGroupId,
                userGroupId = RuijieDiagnosticCollector.voucherGenSummary.value.effectiveUserGroupId,
                authProfileId = RuijieDiagnosticCollector.voucherGenSummary.value.effectiveAuthProfileId,
                sessionState = _sessionState.value.displayName,
                webViewAlive = log.isWebViewAttached,
                cookiePresent = log.hasSessionCookie,
                bridgeReady = log.isBridgeReady,
                durationMs = log.elapsedTimeMs,
                retryCount = 0,
                resultCategory = resultCategory,
                lastError = if (!log.isSuccess) log.responseMsg else null
            )
        )
    }

    fun onAppBackground() {
        Log.d(TAG, "App moved to background. Preserving active session.")
        updateSessionState(
            newState = _sessionState.value,
            trigger = "App Background",
            actionTaken = "Preserving session in background"
        )
    }

    fun onAppForeground() {
        Log.d(TAG, "App returned to foreground. Performing silent session re-verification.")
        updateSessionState(
            newState = _sessionState.value,
            trigger = "App Foreground",
            actionTaken = "Re-verifying session silently"
        )
        scope.launch {
            reverifySessionIfNeeded("App Foreground")
        }
    }

    fun logout(userInitiated: Boolean = true) {
        Log.i(TAG, "Ruijie Cloud session logout requested. User initiated: $userInitiated")
        updateSessionState(
            newState = RuijieSessionState.LOGGING_OUT,
            trigger = if (userInitiated) "User Tap Logout" else "Session Expired Auto Logout",
            actionTaken = "Clearing session cookies"
        )
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies(null)
        cookieManager.flush()

        _lastAccountName.value = ""
        _lastTenantName.value = ""
        _lastTenantId.value = 0L

        updateSessionState(
            newState = RuijieSessionState.LOGIN_REQUIRED,
            trigger = "Logout Complete",
            actionTaken = "Displaying SSO login screen"
        )

        mainHandler.post {
            persistentWebView?.loadUrl(RuijieProtocol.SSO_LOGIN_URL)
        }
    }

    fun reloadSSO() {
        mainHandler.post {
            persistentWebView?.loadUrl(RuijieProtocol.SSO_LOGIN_URL)
        }
    }

    fun sanitizeUrlForLog(url: String): String {
        return try {
            val uri = android.net.Uri.parse(url)
            val scheme = uri.scheme ?: "https"
            val host = uri.host ?: "cloud.ruijienetworks.com"
            val path = uri.path ?: ""
            "$scheme://$host$path"
        } catch (_: Exception) {
            "https://cloud.ruijienetworks.com"
        }
    }

    class BridgeJsInterface {
        @JavascriptInterface
        fun onResponse(reqId: String, httpStatus: Int, contentType: String, body: String?) {
            val deferred = pendingRequests.remove(reqId)
            deferred?.complete(
                RuijieBridgeResponse(
                    requestId = reqId,
                    httpStatus = httpStatus,
                    contentType = contentType,
                    rawBody = body,
                    error = if (httpStatus in 200..299) null else "HTTP $httpStatus error",
                    isSuccess = httpStatus in 200..299
                )
            )
        }

        @JavascriptInterface
        fun onError(reqId: String, error: String) {
            val deferred = pendingRequests.remove(reqId)
            deferred?.complete(
                RuijieBridgeResponse(
                    requestId = reqId,
                    httpStatus = 0,
                    contentType = "",
                    rawBody = null,
                    error = error,
                    isSuccess = false
                )
            )
        }
    }
}
