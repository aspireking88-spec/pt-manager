package com.example.data.repository

import android.util.Log
import com.example.data.api.RuijieApiClient
import com.example.data.model.ApiDiagnosticRecord
import com.example.data.model.AuthMode
import com.example.data.model.DiagnosisVerdict
import com.example.data.model.RuijieAccount
import com.example.data.model.RuijieCreateVoucherRequest
import com.example.data.model.RuijiePackageItem
import com.example.data.model.RuijieProject
import com.example.data.model.RuijieServer
import com.example.data.model.RuijieTokenRequestBody
import com.example.data.model.RuijieVoucherItem
import com.example.data.model.VoucherCodeType
import com.example.data.model.VoucherFilter
import com.example.data.model.VoucherGenerationDiagnosticRecord
import com.example.data.model.VoucherStatus
import com.example.data.model.effectiveBaseUrl
import com.example.data.model.isConfigured
import com.example.data.model.maskedToken
import com.example.data.model.resolveBaseUrl
import com.example.data.session.AccountManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Response

sealed class ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : ApiResult<Nothing>()
}

class RuijieRepository(
    private val apiClient: RuijieApiClient,
    private val accountManager: AccountManager
) {
    private val _latestDiagnostic = MutableStateFlow<ApiDiagnosticRecord?>(null)
    val latestDiagnostic: StateFlow<ApiDiagnosticRecord?> = _latestDiagnostic.asStateFlow()

    private val _latestVoucherGenDiagnostic = MutableStateFlow<VoucherGenerationDiagnosticRecord?>(null)
    val latestVoucherGenDiagnostic: StateFlow<VoucherGenerationDiagnosticRecord?> = _latestVoucherGenDiagnostic.asStateFlow()

    private val _voucherGenLogs = MutableStateFlow<List<VoucherGenerationDiagnosticRecord>>(emptyList())
    val voucherGenLogs: StateFlow<List<VoucherGenerationDiagnosticRecord>> = _voucherGenLogs.asStateFlow()

    private val _latestProjectDiagnostic = MutableStateFlow<com.example.data.model.ProjectDiagnosticInfo?>(null)
    val latestProjectDiagnostic: StateFlow<com.example.data.model.ProjectDiagnosticInfo?> = _latestProjectDiagnostic.asStateFlow()

    private val _latestVoucherDiagnostic = MutableStateFlow<com.example.data.model.VoucherDiagnosticInfo?>(null)
    val latestVoucherDiagnostic: StateFlow<com.example.data.model.VoucherDiagnosticInfo?> = _latestVoucherDiagnostic.asStateFlow()

    fun clearVoucherGenLogs() {
        _voucherGenLogs.value = emptyList()
    }

    fun resolveTenantName(account: RuijieAccount): String {
        val tName = (account.tenantName ?: "")
            .ifBlank { com.example.data.api.RuijieWebViewBridge.lastTenantName.value }
            .ifBlank { account.email ?: "" }
            .ifBlank { account.username }
            .ifBlank { com.example.data.api.RuijieWebViewBridge.lastAccountName.value }
        return if (tName.isBlank()) "default" else tName
    }

    /**
     * Safely parse a response body into a JSONObject, checking HTTP status, Content-Type, and body format.
     * Prevents org.json.JSONException: Value <!DOCTYPE of type java.lang.String cannot be converted to JSONObject.
     */
    private fun parseJsonObjectSafely(
        rawBody: String,
        httpCode: Int = 200,
        contentType: String? = null,
        endpoint: String = ""
    ): Pair<JSONObject?, String?> {
        val trimmed = rawBody.trim()
        if (trimmed.isBlank()) {
            return Pair(null, "Server returned empty response (HTTP $httpCode) for $endpoint")
        }
        if (trimmed.startsWith("<!DOCTYPE", ignoreCase = true) || trimmed.startsWith("<html", ignoreCase = true)) {
            val snippet = redactSensitive(trimmed.take(200))
            return Pair(null, "Ruijie authentication endpoint returned HTTP $httpCode HTML instead of JSON ($endpoint). Server response: $snippet")
        }
        if (!trimmed.startsWith("{")) {
            val snippet = redactSensitive(trimmed.take(200))
            return Pair(null, "Ruijie server returned HTTP $httpCode non-JSON response ($endpoint, Content-Type: ${contentType ?: "unknown"}). Response: $snippet")
        }
        return try {
            Pair(JSONObject(trimmed), null)
        } catch (e: Exception) {
            val snippet = redactSensitive(trimmed.take(200))
            Pair(null, "Invalid JSON structure from Ruijie server (HTTP $httpCode, $endpoint): ${e.localizedMessage}. Response: $snippet")
        }
    }

    /**
     * MODE A — Open API Credentials Authentication Flow:
     * App ID + App Secret -> POST /service/api/oauth20/client/access_token?token=d63dss0a81e4415a889ac5b78fsc904a
     * Followed by Account Info query: GET /service/api/org/account/info
     */
    suspend fun authenticateWithOpenApi(
        appId: String,
        secret: String,
        server: RuijieServer = accountManager.getActiveAccount().server,
        customUrl: String = accountManager.getActiveAccount().customServerUrl
    ): ApiResult<RuijieAccount> = withContext(Dispatchers.IO) {
        val trimmedAppId = appId.trim()
        val trimmedSecret = secret.trim()

        if (trimmedAppId.isBlank() || trimmedSecret.isBlank()) {
            return@withContext ApiResult.Error("Ruijie Open API App ID and App Secret are required.")
        }

        val baseUrl = server.resolveBaseUrl(customUrl)
        val apiService = apiClient.createService(baseUrl)

        try {
            val payload = RuijieTokenRequestBody(
                appid = trimmedAppId,
                secret = trimmedSecret
            )

            Log.d("RuijieAuth", ">>> [Mode A: Open API] Access Token Request: POST $baseUrl/service/api/oauth20/client/access_token?token=d63dss0a81e4415a889ac5b78fsc904a [appid: $trimmedAppId]")

            val response = apiService.getClientAccessToken(body = payload)

            val httpStatus = response.code()
            val contentType = response.headers()["Content-Type"] ?: ""
            val rawBody = response.body()?.string() ?: response.errorBody()?.string() ?: ""

            Log.d("RuijieAuth", "<<< [Mode A: Open API] Response: HTTP $httpStatus Body: ${redactSensitive(rawBody)}")

            val diagnostic = ApiDiagnosticRecord(
                httpMethod = "POST",
                server = server.displayName,
                endpoint = "$baseUrl/service/api/oauth20/client/access_token?token=d63dss0a81e4415a889ac5b78fsc904a",
                httpStatus = httpStatus,
                rawJson = redactSensitive(rawBody)
            )
            _latestDiagnostic.value = diagnostic

            val (json, jsonErr) = parseJsonObjectSafely(
                rawBody = rawBody,
                httpCode = httpStatus,
                contentType = contentType,
                endpoint = "$baseUrl/service/api/oauth20/client/access_token"
            )

            if (json == null) {
                val failureReason = jsonErr ?: "Ruijie Open API returned HTTP $httpStatus"
                Log.e("RuijieAuth", "Open API authentication failed: $failureReason")
                return@withContext ApiResult.Error(failureReason)
            }

            val code = json.optInt("code", if (response.isSuccessful) 0 else -1)
            val msg = json.optString("msg", json.optString("message", "Authentication response"))
            val dataObj = json.optJSONObject("data")

            val accessToken = json.optString("accessToken")
                .ifBlank { json.optString("access_token") }
                .ifBlank { json.optString("token") }
                .ifBlank { dataObj?.optString("accessToken") ?: "" }
                .ifBlank { dataObj?.optString("access_token") ?: "" }
                .ifBlank { dataObj?.optString("token") ?: "" }

            val refreshToken = json.optString("refreshToken")
                .ifBlank { json.optString("refresh_token") }
                .ifBlank { dataObj?.optString("refreshToken") ?: "" }
                .ifBlank { dataObj?.optString("refresh_token") ?: "" }

            val expiresIn = if (json.has("expires_in")) json.optLong("expires_in")
                else if (json.has("expiresIn")) json.optLong("expiresIn")
                else if (dataObj?.has("expires_in") == true) dataObj.optLong("expires_in")
                else if (dataObj?.has("expiresIn") == true) dataObj.optLong("expiresIn")
                else 7200L

            var tenantName = json.optString("tenantName")
                .ifBlank { json.optString("tenant_name") }
                .ifBlank { dataObj?.optString("tenantName") ?: "" }
                .ifBlank { dataObj?.optString("tenant_name") ?: "" }

            var tenantId = if (json.has("tenantId")) json.optLong("tenantId")
                else if (json.has("tenant_id")) json.optLong("tenant_id")
                else if (dataObj?.has("tenantId") == true) dataObj.optLong("tenantId")
                else if (dataObj?.has("tenant_id") == true) dataObj.optLong("tenant_id")
                else 0L

            if (code != 0 || accessToken.isBlank()) {
                val displayMsg = if (msg.isNotBlank() && msg != "Authentication response") {
                    msg
                } else {
                    "Invalid App ID/Secret or authentication failure (code: $code, HTTP $httpStatus)"
                }
                Log.e("RuijieAuth", "Ruijie Open API authentication failed: $displayMsg")
                return@withContext ApiResult.Error("Ruijie Cloud: $displayMsg")
            }

            // Stage 2: Fetch Account Info (documented: GET /service/api/org/account/info?access_token={token})
            var userEmail = ""
            var userRole = ""
            var userNickname = ""
            var defaultTenant: String? = null

            try {
                Log.d("RuijieAuth", ">>> [Mode A] Stage 2 Account Info: GET $baseUrl/service/api/org/account/info?access_token=********")
                val infoResponse = apiService.getAccountInfoByToken(accessToken)
                val infoHttpCode = infoResponse.code()
                val infoContentType = infoResponse.headers()["Content-Type"] ?: ""
                val infoRawBody = infoResponse.body()?.string() ?: infoResponse.errorBody()?.string() ?: ""

                val (infoJson, _) = parseJsonObjectSafely(
                    rawBody = infoRawBody,
                    httpCode = infoHttpCode,
                    contentType = infoContentType,
                    endpoint = "$baseUrl/service/api/org/account/info"
                )

                if (infoJson != null) {
                    val infoData = infoJson.optJSONObject("data") ?: infoJson
                    // Documented fields: account, userName, role, defaultTenant
                    val docAccount = infoData.optString("account", "")
                    userEmail = docAccount.ifBlank { infoData.optString("email", infoData.optString("mail", "")) }
                    userRole = infoData.optString("role", infoData.optString("userRole", ""))
                    userNickname = infoData.optString("userName",
                        infoData.optString("nickname", infoData.optString("nickName", infoData.optString("username", ""))))
                    defaultTenant = infoData.optString("defaultTenant", "").ifBlank { null }
                    if (tenantName.isBlank()) {
                        tenantName = infoData.optString("tenantName", infoData.optString("tenant_name", ""))
                    }
                    if (tenantId == 0L) {
                        tenantId = infoData.optLong("tenantId", infoData.optLong("tenant_id", 0L))
                    }
                    if (tenantId == 0L && defaultTenant != null) {
                        tenantId = defaultTenant.toLongOrNull() ?: 0L
                    }
                }
            } catch (e: Exception) {
                Log.w("RuijieAuth", "Optional Account Info retrieval failed: ${e.message}")
            }

            // Stage 3: Atomically update session in AccountManager
            val currentActive = accountManager.getActiveAccount()
            val updatedActive = currentActive.copy(
                appId = trimmedAppId,
                username = trimmedAppId,
                secret = trimmedSecret,
                authMode = AuthMode.OPEN_API
            )
            accountManager.updateAccount(updatedActive)

            accountManager.setSession(
                username = trimmedAppId,
                accessToken = accessToken,
                refreshToken = refreshToken,
                expiresInSeconds = expiresIn,
                email = userEmail,
                tenantName = tenantName,
                tenantId = tenantId,
                userRole = userRole,
                nickname = userNickname,
                defaultTenant = defaultTenant,
                server = server,
                customUrl = customUrl,
                authMode = AuthMode.OPEN_API
            )

            Log.d("RuijieAuth", "Open API Authentication Successful! App ID: $trimmedAppId, Server: ${server.displayName}")
            return@withContext ApiResult.Success(accountManager.getActiveAccount())
        } catch (e: Exception) {
            Log.e("RuijieAuth", "Ruijie Open API network error: ${e.localizedMessage}", e)
            return@withContext ApiResult.Error("Connection error: ${e.localizedMessage}", e)
        }
    }

    /**
     * MODE B — Ruijie Cloud User Account Authentication Flow (Stage 1 Policy -> Stage 2 Global Login -> Stage 3 Account Info):
     * Implements official Ruijie Cloud authentication sequence discovered from reference app:
     * Stage 1: POST /service/api/intl/auth/v2/policy/ (Resolves tenant and policy configuration)
     * Stage 2: POST /service/api/intl/auth/v2/global/ (Authenticates user credentials and returns tokens)
     * Stage 3: GET /service/api/org/account/info (Retrieves user identity and account details)
     */
    suspend fun loginWithUserAccount(
        username: String,
        password: String,
        server: RuijieServer = accountManager.getActiveAccount().server,
        customUrl: String = accountManager.getActiveAccount().customServerUrl
    ): ApiResult<RuijieAccount> = withContext(Dispatchers.IO) {
        val trimmedUser = username.trim()
        val trimmedPass = password.trim()

        if (trimmedUser.isBlank() || trimmedPass.isBlank()) {
            return@withContext ApiResult.Error("Username and password are required.")
        }

        val baseUrl = server.resolveBaseUrl(customUrl)
        val apiService = apiClient.createService(baseUrl)

        try {
            var tenantName = ""
            var tenantId = 0L

            // ==========================================
            // STAGE 1: Policy Request
            // ==========================================
            val policyJsonObj = JSONObject().apply {
                put("username", trimmedUser)
                put("account", trimmedUser)
            }
            val policyRequestBody = policyJsonObj.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

            Log.d("RuijieAuth", ">>> Stage 1 Policy Request: POST $baseUrl/service/api/intl/auth/v2/policy/ Body: $policyJsonObj")

            var stage1Success = false
            try {
                val policyResponse = apiService.authPolicy(policyRequestBody)
                val pStatus = policyResponse.code()
                val pContentType = policyResponse.headers()["Content-Type"] ?: ""
                val pRawBody = policyResponse.body()?.string() ?: policyResponse.errorBody()?.string() ?: ""

                Log.d("RuijieAuth", "<<< Stage 1 Policy Response: HTTP $pStatus Body: ${redactSensitive(pRawBody)}")

                _latestDiagnostic.value = ApiDiagnosticRecord(
                    httpMethod = "POST",
                    server = server.displayName,
                    endpoint = "$baseUrl/service/api/intl/auth/v2/policy/",
                    httpStatus = pStatus,
                    rawJson = redactSensitive(pRawBody)
                )

                val (pJson, _) = parseJsonObjectSafely(
                    rawBody = pRawBody,
                    httpCode = pStatus,
                    contentType = pContentType,
                    endpoint = "$baseUrl/service/api/intl/auth/v2/policy/"
                )

                if (pJson != null) {
                    val pData = pJson.optJSONObject("data") ?: pJson
                    tenantName = pData.optString("tenantName")
                        .ifBlank { pData.optString("tenant_name") }
                        .ifBlank { pJson.optString("tenantName", pJson.optString("tenant_name", "")) }

                    tenantId = if (pData.has("tenantId")) pData.optLong("tenantId")
                        else if (pData.has("tenant_id")) pData.optLong("tenant_id")
                        else if (pJson.has("tenantId")) pJson.optLong("tenantId")
                        else if (pJson.has("tenant_id")) pJson.optLong("tenant_id")
                        else 0L
                    stage1Success = true
                }
            } catch (e: Exception) {
                Log.w("RuijieAuth", "Stage 1 Policy call encountered non-fatal error: ${e.message}")
            }

            // ==========================================
            // STAGE 2: Global / Tenant Login Request
            // ==========================================
            val globalJsonObj = JSONObject().apply {
                put("username", trimmedUser)
                put("account", trimmedUser)
                put("auth.account", trimmedUser)
                put("auth.username", trimmedUser)
                put("password", trimmedPass)
                put("auth.password", trimmedPass)
                if (tenantName.isNotBlank()) {
                    put("tenantName", tenantName)
                    put("tenant_name", tenantName)
                }
                if (tenantId != 0L) {
                    put("tenantId", tenantId)
                    put("tenant_id", tenantId)
                }
            }
            val globalRequestBody = globalJsonObj.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

            Log.d("RuijieAuth", ">>> Stage 2 Authentication Request [password REDACTED]")

            // Try primary endpoints in order of specificity
            var httpStatus = 0
            var contentType = ""
            var rawBody = ""
            var resolvedEndpoint = ""

            // Attempt 1: If tenant resolved, try /service/api/intl/tenant/auth/
            if (tenantName.isNotBlank() || tenantId != 0L) {
                try {
                    val tenantResponse = apiService.authTenant(globalRequestBody)
                    httpStatus = tenantResponse.code()
                    contentType = tenantResponse.headers()["Content-Type"] ?: ""
                    rawBody = tenantResponse.body()?.string() ?: tenantResponse.errorBody()?.string() ?: ""
                    resolvedEndpoint = "$baseUrl/service/api/intl/tenant/auth/"
                } catch (e: Exception) {
                    Log.w("RuijieAuth", "Tenant auth attempt failed: ${e.message}")
                }
            }

            // Attempt 2: Try /service/api/intl/auth/v2/global/
            if (httpStatus == 0 || httpStatus == 404 || httpStatus == 405) {
                try {
                    val globalResponse = apiService.authGlobal(globalRequestBody)
                    httpStatus = globalResponse.code()
                    contentType = globalResponse.headers()["Content-Type"] ?: ""
                    rawBody = globalResponse.body()?.string() ?: globalResponse.errorBody()?.string() ?: ""
                    resolvedEndpoint = "$baseUrl/service/api/intl/auth/v2/global/"
                } catch (e: Exception) {
                    Log.w("RuijieAuth", "Global auth attempt failed: ${e.message}")
                }
            }

            // Attempt 3: Try POST /service/api/login
            if (httpStatus == 0 || httpStatus == 404 || httpStatus == 405) {
                try {
                    val fallbackResponse = apiService.userLogin(globalRequestBody)
                    httpStatus = fallbackResponse.code()
                    contentType = fallbackResponse.headers()["Content-Type"] ?: ""
                    rawBody = fallbackResponse.body()?.string() ?: fallbackResponse.errorBody()?.string() ?: ""
                    resolvedEndpoint = "$baseUrl/service/api/login"
                } catch (e: Exception) {
                    Log.w("RuijieAuth", "Legacy login fallback failed: ${e.message}")
                }
            }

            var (json, jsonErr) = parseJsonObjectSafely(
                rawBody = rawBody,
                httpCode = httpStatus,
                contentType = contentType,
                endpoint = resolvedEndpoint
            )

            // Attempt 4: If code 9 ("Parameter is null") or null, try GET /service/api/login?account=...&password=...
            if (json == null || json.optInt("code") == 9) {
                try {
                    Log.d("RuijieAuth", ">>> Attempting GET /service/api/login fallback")
                    val getResponse = apiService.userLoginGet(trimmedUser, trimmedPass)
                    val getHttpStatus = getResponse.code()
                    val getContentType = getResponse.headers()["Content-Type"] ?: ""
                    val getRawBody = getResponse.body()?.string() ?: getResponse.errorBody()?.string() ?: ""
                    val (getJson, getErr) = parseJsonObjectSafely(
                        rawBody = getRawBody,
                        httpCode = getHttpStatus,
                        contentType = getContentType,
                        endpoint = "$baseUrl/service/api/login"
                    )
                    if (getJson != null && getJson.optInt("code") != 9) {
                        json = getJson
                        jsonErr = getErr
                        httpStatus = getHttpStatus
                        contentType = getContentType
                        rawBody = getRawBody
                        resolvedEndpoint = "$baseUrl/service/api/login [GET]"
                    }
                } catch (e: Exception) {
                    Log.w("RuijieAuth", "GET login fallback failed: ${e.message}")
                }
            }

            Log.d("RuijieAuth", "<<< Stage 2 Response ($resolvedEndpoint): HTTP $httpStatus Body: ${redactSensitive(rawBody)}")

            _latestDiagnostic.value = ApiDiagnosticRecord(
                httpMethod = if (resolvedEndpoint.contains("[GET]")) "GET" else "POST",
                server = server.displayName,
                endpoint = resolvedEndpoint.ifBlank { "$baseUrl/service/api/intl/auth/v2/global/" },
                httpStatus = httpStatus,
                rawJson = redactSensitive(rawBody)
            )

            if (json == null) {
                val failureReason = jsonErr ?: "Ruijie authentication returned HTTP $httpStatus"
                Log.e("RuijieAuth", "Login failed: $failureReason")
                return@withContext ApiResult.Error("Ruijie Cloud: $failureReason")
            }

            val code = json.optInt("code", if (httpStatus in 200..299) 0 else -1)
            val msg = json.optString("msg", json.optString("message", ""))
            val dataObj = json.optJSONObject("data")

            val accessToken = json.optString("accessToken")
                .ifBlank { json.optString("access_token") }
                .ifBlank { json.optString("token") }
                .ifBlank { json.optString("sessionToken") }
                .ifBlank { dataObj?.optString("accessToken") ?: "" }
                .ifBlank { dataObj?.optString("access_token") ?: "" }
                .ifBlank { dataObj?.optString("token") ?: "" }
                .ifBlank { dataObj?.optString("sessionToken") ?: "" }

            val refreshToken = json.optString("refreshToken")
                .ifBlank { json.optString("refresh_token") }
                .ifBlank { dataObj?.optString("refreshToken") ?: "" }
                .ifBlank { dataObj?.optString("refresh_token") ?: "" }

            val expiresIn = if (json.has("expires_in")) json.optLong("expires_in")
                else if (json.has("expiresIn")) json.optLong("expiresIn")
                else if (dataObj?.has("expires_in") == true) dataObj.optLong("expires_in")
                else if (dataObj?.has("expiresIn") == true) dataObj.optLong("expiresIn")
                else 7200L

            if (tenantName.isBlank()) {
                tenantName = json.optString("tenantName")
                    .ifBlank { json.optString("tenant_name") }
                    .ifBlank { dataObj?.optString("tenantName") ?: "" }
                    .ifBlank { dataObj?.optString("tenant_name") ?: "" }
            }

            if (tenantId == 0L) {
                tenantId = if (json.has("tenantId")) json.optLong("tenantId")
                    else if (json.has("tenant_id")) json.optLong("tenant_id")
                    else if (dataObj?.has("tenantId") == true) dataObj.optLong("tenantId")
                    else if (dataObj?.has("tenant_id") == true) dataObj.optLong("tenant_id")
                    else 0L
            }

            if (accessToken.isBlank() || (code != 0 && code != -1 && httpStatus !in 200..299)) {
                val displayMsg = if (msg.isNotBlank()) msg else "Login failed (code: $code, HTTP $httpStatus)"
                Log.e("RuijieAuth", "Ruijie User Login failed: $displayMsg")
                return@withContext ApiResult.Error("Ruijie Cloud: $displayMsg")
            }

            // ==========================================
            // STAGE 3: Account Info Request
            // ==========================================
            var userEmail = if (trimmedUser.contains("@")) trimmedUser else ""
            var userRole = ""
            var userNickname = ""
            var defaultTenant: String? = null

            try {
                Log.d("RuijieAuth", ">>> Stage 3 Account Info Request [Authorization: Bearer ********]")
                var infoResponse = apiService.getUserInfo("Bearer $accessToken")
                var infoHttpCode = infoResponse.code()
                var infoContentType = infoResponse.headers()["Content-Type"] ?: ""
                var infoRawBody = infoResponse.body()?.string() ?: infoResponse.errorBody()?.string() ?: ""

                if (infoHttpCode == 404 || infoHttpCode == 405) {
                    // Documented form: GET /service/api/org/account/info?access_token={token}
                    infoResponse = apiService.getAccountInfoByToken(accessToken)
                    infoHttpCode = infoResponse.code()
                    infoContentType = infoResponse.headers()["Content-Type"] ?: ""
                    infoRawBody = infoResponse.body()?.string() ?: infoResponse.errorBody()?.string() ?: ""
                }

                val (infoJson, _) = parseJsonObjectSafely(
                    rawBody = infoRawBody,
                    httpCode = infoHttpCode,
                    contentType = infoContentType,
                    endpoint = "$baseUrl/service/api/user/info/"
                )

                if (infoJson != null) {
                    val infoData = infoJson.optJSONObject("data") ?: infoJson
                    // Documented fields: account, userName, role, defaultTenant
                    val docAccount = infoData.optString("account", "")
                    val parsedEmail = docAccount.ifBlank { infoData.optString("email", infoData.optString("mail", "")) }
                    if (parsedEmail.isNotBlank()) userEmail = parsedEmail
                    userRole = infoData.optString("role", infoData.optString("userRole", ""))
                    userNickname = infoData.optString("userName",
                        infoData.optString("nickname", infoData.optString("nickName", infoData.optString("username", ""))))
                    defaultTenant = infoData.optString("defaultTenant", "").ifBlank { null }
                    if (tenantName.isBlank()) {
                        tenantName = infoData.optString("tenantName", infoData.optString("tenant_name", ""))
                    }
                    if (tenantId == 0L) {
                        tenantId = infoData.optLong("tenantId", infoData.optLong("tenant_id", 0L))
                    }
                    if (tenantId == 0L && defaultTenant != null) {
                        tenantId = defaultTenant.toLongOrNull() ?: 0L
                    }
                }
            } catch (e: Exception) {
                Log.w("RuijieAuth", "Optional Account Info retrieval failed: ${e.message}")
            }

            // Save active account and session
            val currentActive = accountManager.getActiveAccount()
            val updatedActive = currentActive.copy(
                username = trimmedUser,
                email = userEmail.ifBlank { trimmedUser },
                secret = trimmedPass,
                authMode = AuthMode.USER_ACCOUNT
            )
            accountManager.updateAccount(updatedActive)

            accountManager.setSession(
                username = trimmedUser,
                accessToken = accessToken,
                refreshToken = refreshToken,
                expiresInSeconds = expiresIn,
                email = userEmail,
                tenantName = tenantName,
                tenantId = tenantId,
                userRole = userRole,
                nickname = userNickname,
                defaultTenant = defaultTenant,
                server = server,
                customUrl = customUrl,
                authMode = AuthMode.USER_ACCOUNT
            )

            Log.d("RuijieAuth", "Ruijie Login Successful! User: $trimmedUser, Server: ${server.displayName}")
            return@withContext ApiResult.Success(accountManager.getActiveAccount())
        } catch (e: Exception) {
            Log.e("RuijieAuth", "Ruijie Login network error: ${e.localizedMessage}", e)
            return@withContext ApiResult.Error("Connection error: ${e.localizedMessage}", e)
        }
    }

    /**
     * Primary Login function in RuijieRepository.
     * Authenticates using real Open API credentials (appid and secret) according to
     * official Ruijie Cloud API documentation, section 2.1.1 "Get an Access Token":
     * POST https://{cloudserver}/service/api/oauth20/client/access_token?token=d63dss0a81e4415a889ac5b78fsc904a
     * Fixed constant token query parameter: d63dss0a81e4415a889ac5b78fsc904a
     * Content-Type: application/json
     * Body: {"appid": "...", "secret": "..."}
     * Real login errors are returned without fake fallback tokens.
     */
    suspend fun login(
        appId: String,
        secret: String,
        server: RuijieServer = accountManager.getActiveAccount().server,
        customUrl: String = accountManager.getActiveAccount().customServerUrl
    ): ApiResult<RuijieAccount> {
        return authenticateWithOpenApi(appId, secret, server, customUrl)
    }

    /**
     * Unified Login with credentials, dynamically routing to Open API or User Account authentication.
     */
    suspend fun loginWithCredentials(
        username: String,
        password: String,
        server: RuijieServer = accountManager.getActiveAccount().server,
        customUrl: String = accountManager.getActiveAccount().customServerUrl,
        mode: AuthMode = AuthMode.OPEN_API
    ): ApiResult<RuijieAccount> = withContext(Dispatchers.IO) {
        val trimmedUser = username.trim()
        val trimmedPass = password.trim()

        if (trimmedUser.isBlank() || trimmedPass.isBlank()) {
            return@withContext ApiResult.Error("Credentials are required.")
        }

        if (mode == AuthMode.USER_ACCOUNT || trimmedUser.contains("@")) {
            return@withContext loginWithUserAccount(trimmedUser, trimmedPass, server, customUrl)
        } else {
            return@withContext authenticateWithOpenApi(trimmedUser, trimmedPass, server, customUrl)
        }
    }

    /**
     * Authenticate active Ruijie Account using its configured AuthMode.
     */
    suspend fun authenticate(account: RuijieAccount = accountManager.getActiveAccount()): ApiResult<RuijieAccount> =
        withContext(Dispatchers.IO) {
            if (account.authMode == AuthMode.USER_ACCOUNT) {
                val effectiveUser = (account.username.ifBlank { account.email ?: "" }).trim()
                val effectiveSecret = account.secret.trim()
                if (effectiveUser.isBlank() || effectiveSecret.isBlank()) {
                    return@withContext ApiResult.Error("Account Email/Username and Password are required.")
                }
                return@withContext loginWithUserAccount(
                    username = effectiveUser,
                    password = effectiveSecret,
                    server = account.server,
                    customUrl = account.customServerUrl
                )
            } else {
                val effectiveAppId = (account.appId.ifBlank { account.username }).trim()
                val effectiveSecret = account.secret.trim()
                if (effectiveAppId.isBlank() || effectiveSecret.isBlank()) {
                    return@withContext ApiResult.Error("App ID and Secret are required for Ruijie Open API authentication.")
                }
                val _existingAccessToken = account.accessToken
                if (!_existingAccessToken.isNullOrBlank()) {
                    val refreshRes = refreshOpenApiToken(
                        appId = effectiveAppId,
                        secret = effectiveSecret,
                        accessToken = _existingAccessToken ?: "",
                        server = account.server,
                        customUrl = account.customServerUrl
                    )
                    if (refreshRes is ApiResult.Success) {
                        return@withContext refreshRes
                    }
                }
                return@withContext authenticateWithOpenApi(
                    appId = effectiveAppId,
                    secret = effectiveSecret,
                    server = account.server,
                    customUrl = account.customServerUrl
                )
            }
        }

    /**
     * Refresh access token for Open API using GET /service/api/token/refresh
     */
    suspend fun refreshOpenApiToken(
        appId: String,
        secret: String,
        accessToken: String,
        server: RuijieServer = accountManager.getActiveAccount().server,
        customUrl: String = accountManager.getActiveAccount().customServerUrl
    ): ApiResult<RuijieAccount> = withContext(Dispatchers.IO) {
        val baseUrl = server.resolveBaseUrl(customUrl)
        val apiService = apiClient.createService(baseUrl)

        try {
            Log.d("RuijieAuth", ">>> Open API Refresh Token: GET $baseUrl/service/api/token/refresh")
            val response = apiService.refreshToken(
                appid = appId.trim(),
                secret = secret.trim(),
                accessToken = accessToken.trim()
            )
            val httpCode = response.code()
            val rawBody = response.body()?.string() ?: response.errorBody()?.string() ?: ""
            Log.d("RuijieAuth", "<<< Open API Refresh Token HTTP $httpCode Body: ${redactSensitive(rawBody)}")

            val trimmed = rawBody.trim()
            if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                val json = JSONObject(trimmed)
                val code = json.optInt("code", if (response.isSuccessful) 0 else -1)
                if (code == 0) {
                    val dataObj = json.optJSONObject("data")
                    val newToken = json.optString("accessToken")
                        .ifBlank { json.optString("token") }
                        .ifBlank { dataObj?.optString("accessToken") ?: "" }
                        .ifBlank { dataObj?.optString("token") ?: "" }

                    if (newToken.isNotBlank()) {
                        val expiresIn = json.optLong("expiresIn", dataObj?.optLong("expiresIn", 7200L) ?: 7200L)
                        val newRefreshToken = json.optString("refreshToken")
                            .ifBlank { dataObj?.optString("refreshToken") ?: "" }
                        accountManager.updateActiveToken(
                            accessToken = newToken,
                            refreshToken = newRefreshToken,
                            expiresInSeconds = expiresIn
                        )
                        return@withContext ApiResult.Success(accountManager.getActiveAccount())
                    }
                }
            }
            ApiResult.Error("Open API token refresh failed.")
        } catch (e: Exception) {
            ApiResult.Error("Open API token refresh error: ${e.localizedMessage}", e)
        }
    }

    /**
     * Retrieve projects/network groups from Ruijie Cloud:
     * GET /service/api/group/single/tree?depth=BUILDING or WebProxy /intlMaccGroup/getGroupTree
     */
    suspend fun getProjects(
        account: RuijieAccount = accountManager.getActiveAccount(),
        retryCount: Int = 0
    ): ApiResult<List<RuijieProject>> = withContext(Dispatchers.IO) {
        // 1. Primary: If WebView session is active or cookies are present, use WebProxy bridge
        if (com.example.data.api.RuijieWebViewBridge.hasValidCookieSession() || com.example.data.api.RuijieWebViewBridge.sessionState.value.isConnected) {
            val groupEnvelope = com.example.data.api.RuijieProtocol.buildGroupTreeEnvelope()
            val bridgeResp = com.example.data.api.RuijieWebViewBridge.executeWebProxyRequest(groupEnvelope)

            if (!bridgeResp.isSuccess) {
                val errorMsg = bridgeResp.error 
                    ?: bridgeResp.errorCode?.userFriendlyMessage 
                    ?: "group-tree HTTP error: ${bridgeResp.httpStatus}"
                _latestProjectDiagnostic.value = com.example.data.model.ProjectDiagnosticInfo(
                    endpointPath = "/group/all/tree?1=1&group_id=0",
                    httpMethod = "GET",
                    httpStatus = bridgeResp.httpStatus,
                    responseCode = bridgeResp.errorCode?.ordinal,
                    responseMsg = errorMsg,
                    responseWrapperDetected = "none",
                    flattenedProjectCount = 0
                )
                return@withContext ApiResult.Error(errorMsg)
            }

            val body = bridgeResp.rawBody
            if (body.isNullOrBlank()) {
                val err = "Empty response received from Ruijie group-tree API"
                return@withContext ApiResult.Error(err)
            }

            try {
                val details = com.example.data.api.RuijieProtocol.parseProjectsFromGroupTreeWithDetails(body)
                val projects = details.projects

                val currentSelectedGid = account.selectedGroupId.toString()
                _latestProjectDiagnostic.value = com.example.data.model.ProjectDiagnosticInfo(
                    endpointPath = "/group/all/tree?1=1&group_id=0",
                    httpMethod = "GET",
                    httpStatus = bridgeResp.httpStatus,
                    responseCode = 0,
                    responseMsg = "OK",
                    responseWrapperDetected = details.wrapperDetected,
                    rootGroupId = details.rootGroupId,
                    rootGroupCount = details.rootGroupCount,
                    flattenedProjectCount = projects.size,
                    visibleProjects = projects.map { Pair(it.name, it.groupId) },
                    selectedProjectName = account.selectedGroupName,
                    selectedGroupId = currentSelectedGid
                )

                if (projects.isEmpty()) {
                    return@withContext ApiResult.Error("No Ruijie projects were returned for this account.")
                }

                val converted: List<RuijieProject> = projects.map { p: com.example.data.model.RuijieProjectItem ->
                    RuijieProject(
                        id = p.numericGroupId,
                        groupId = p.numericGroupId,
                        groupIdStr = p.groupId,
                        name = p.name,
                        groupName = p.groupName,
                        parentGroupId = p.parentGroupId?.toString(),
                        depth = p.depth,
                        displayPath = p.displayPath,
                        tenantName = (p.tenantName ?: "").ifBlank { account.tenantName ?: "" }
                    )
                }

                val currentGroupId = account.selectedGroupId?.toLongOrNull() ?: 0L
                val match = if (currentGroupId > 0L) {
                    converted.find { it.effectiveId == currentGroupId } ?: converted.first()
                } else {
                    converted.first()
                }

                accountManager.updateActiveProject(match.effectiveId, match.effectiveName, match.tenantName)

                // Update diagnostic with selected project
                _latestProjectDiagnostic.value = _latestProjectDiagnostic.value?.copy(
                    selectedProjectName = match.effectiveName,
                    selectedGroupId = match.effectiveGroupId
                )

                return@withContext ApiResult.Success(converted)
            } catch (e: IllegalArgumentException) {
                return@withContext ApiResult.Error(e.message ?: "Project parser error")
            } catch (e: Exception) {
                return@withContext ApiResult.Error("Project parser error: ${e.localizedMessage ?: e.message}", e)
            }
        }

        if (account.accessToken.isNullOrBlank() || account.accessToken == "SESSION_COOKIE_ACTIVE") {
            if (retryCount == 0 && account.isConfigured) {
                val authRes = authenticate(account)
                if (authRes is ApiResult.Success) {
                    return@withContext getProjects(accountManager.getActiveAccount(), retryCount = 1)
                }
            }
            return@withContext ApiResult.Error("WebView not authenticated. Please log in with official Ruijie Cloud SSO.")
        }

        val baseUrl = account.effectiveBaseUrl
        val apiService = apiClient.createService(baseUrl)

        try {
            Log.d("RuijieAuth", ">>> Project Tree Request: GET $baseUrl/service/api/group/single/tree?depth=BUILDING [token: ${account.maskedToken}]")
            val response = apiService.getGroupSingleTree(
                depth = "BUILDING",
                accessToken = account.accessToken ?: ""
            )
            val httpStatus = response.code()
            val rawBody = response.body()?.string() ?: response.errorBody()?.string() ?: ""

            Log.d("RuijieAuth", "<<< Project Tree Response: HTTP $httpStatus Body: ${redactSensitive(rawBody)}")

            _latestDiagnostic.value = ApiDiagnosticRecord(
                httpMethod = "GET",
                server = account.server.displayName,
                endpoint = "$baseUrl/service/api/group/single/tree?depth=BUILDING",
                httpStatus = httpStatus,
                rawJson = redactSensitive(rawBody)
            )

            if (isSessionExpired(httpStatus, rawBody)) {
                if (retryCount == 0 && account.isConfigured) {
                    val authRes = authenticate(account)
                    if (authRes is ApiResult.Success) {
                        return@withContext getProjects(accountManager.getActiveAccount(), retryCount = 1)
                    }
                }
                return@withContext ApiResult.Error("Ruijie Cloud session expired / login timeout. Please log in again.")
            }

            if (!response.isSuccessful) {
                return@withContext ApiResult.Error("Ruijie Project Tree API HTTP $httpStatus: ${response.message()}")
            }

            val projects = parseProjectsFromTreeJson(rawBody)
            if (projects.isNotEmpty()) {
                val currentGroupId = account.selectedGroupId?.toLongOrNull() ?: 0L
                val match = projects.find { it.effectiveId == currentGroupId } ?: projects.first()
                accountManager.updateActiveProject(match.effectiveId, match.effectiveName, match.tenantName)
                ApiResult.Success(projects)
            } else {
                ApiResult.Error("No network projects found in Ruijie Cloud group tree.")
            }
        } catch (e: Exception) {
            ApiResult.Error("Failed to fetch projects: ${e.localizedMessage}", e)
        }
    }

    private suspend fun fetchSingleVoucherPage(
        account: RuijieAccount,
        groupIdStr: String,
        numericGroupId: Long,
        start: Int,
        pageSize: Int
    ): ApiResult<Pair<List<RuijieVoucherItem>, Int>> {
        val safePageSize = pageSize.coerceIn(1, 200)

        val isWebViewActive = com.example.data.api.RuijieWebViewBridge.hasValidCookieSession() ||
                com.example.data.api.RuijieWebViewBridge.sessionState.value.isConnected ||
                account.accessToken == "SESSION_COOKIE_ACTIVE"

        // 1. Primary: Use WebProxy bridge if session or cookies active
        if (isWebViewActive) {
            val tenant = resolveTenantName(account)
            val envelope = com.example.data.api.RuijieProtocol.buildVoucherListEnvelope(
                groupId = groupIdStr,
                tenantId = account.selectedTenantId
            )
            val bridgeResp = com.example.data.api.RuijieWebViewBridge.executeWebProxyRequest(envelope)

            _latestVoucherDiagnostic.value = _latestVoucherDiagnostic.value?.copy(
                httpStatus = bridgeResp.httpStatus,
                responseMsg = bridgeResp.error ?: if (bridgeResp.isSuccess) "Success" else "Error"
            )

            if (bridgeResp.isSuccess && !bridgeResp.rawBody.isNullOrBlank()) {
                return try {
                    val (vouchers, total) = com.example.data.api.RuijieProtocol.parseVoucherList(bridgeResp.rawBody)
                    ApiResult.Success(Pair(vouchers, total))
                } catch (e: Exception) {
                    if (e is com.example.data.model.RuijieGroupNotSynchronizedException || e.message?.contains("synchroniz", ignoreCase = true) == true) {
                        ApiResult.Error("Ruijie is still synchronizing this project group. Refresh and try again.")
                    } else {
                        ApiResult.Error("Failed to parse vouchers: ${e.localizedMessage ?: e.message}")
                    }
                }
            } else {
                val errorMsg = when (bridgeResp.errorCode) {
                    com.example.data.model.RuijieBridgeErrorCode.GROUP_NOT_SYNCHRONIZED ->
                        "Ruijie is still synchronizing this project group. Refresh and try again."
                    com.example.data.model.RuijieBridgeErrorCode.REQUEST_TIMEOUT,
                    com.example.data.model.RuijieBridgeErrorCode.JS_INJECTION_TIMEOUT ->
                        "Ruijie request timed out. Your login session was kept. Please retry."
                    com.example.data.model.RuijieBridgeErrorCode.PERMISSION_DENIED,
                    com.example.data.model.RuijieBridgeErrorCode.RUIJIE_PERMISSION_ERROR ->
                        "The account is authenticated but does not have permission for this operation."
                    com.example.data.model.RuijieBridgeErrorCode.SESSION_EXPIRED ->
                        "Ruijie session expired. Please sign in again."
                    else -> bridgeResp.error ?: bridgeResp.errorCode?.userFriendlyMessage ?: "Ruijie WebProxy HTTP ${bridgeResp.httpStatus}"
                }
                return ApiResult.Error(errorMsg)
            }
        }

        val baseUrl = account.effectiveBaseUrl
        val apiService = apiClient.createService(baseUrl)

        try {
            var response = try {
                apiService.getVoucherListOpen(
                    groupId = numericGroupId,
                    accessToken = account.accessToken ?: "",
                    start = start,
                    pageSize = safePageSize
                )
            } catch (_: Exception) {
                null
            }

            var endpointCalled = "$baseUrl/service/api/open/auth/voucher/getList/$numericGroupId"

            if (response == null || response.code() == 404 || response.code() == 400) {
                val tenant = (account.tenantName ?: "").ifBlank { "default" }
                endpointCalled = "$baseUrl/service/api/intlSamVoucher/getList/$tenant/$groupIdStr"
                response = apiService.getVoucherListTenant(
                    tenantName = tenant,
                    groupId = numericGroupId,
                    accessToken = account.accessToken ?: "",
                    start = start,
                    pageSize = safePageSize,
                    tenantId = if (account.tenantId > 0L) account.tenantId.toString() else null
                )
            }

            val httpStatus = response.code()
            val rawBody = response.body()?.string() ?: response.errorBody()?.string() ?: ""

            _latestDiagnostic.value = ApiDiagnosticRecord(
                httpMethod = "GET",
                server = account.server.displayName,
                endpoint = endpointCalled,
                httpStatus = httpStatus,
                rawJson = redactSensitive(rawBody)
            )

            if (isSessionExpired(httpStatus, rawBody)) {
                return ApiResult.Error("SESSION_EXPIRED")
            }

            if (!response.isSuccessful) {
                return ApiResult.Error("Ruijie Voucher API HTTP $httpStatus: ${response.message()}")
            }

            val parsed = parseVouchersFromResponse(rawBody)
            return ApiResult.Success(parsed)
        } catch (e: Exception) {
            return ApiResult.Error("Error retrieving Ruijie Cloud vouchers: ${e.localizedMessage}", e)
        }
    }

    /**
     * Retrieve ACTUAL vouchers from Ruijie Cloud for the selected project group.
     * Supports documented primary endpoint: /service/api/open/auth/voucher/getList/{groupId}
     * with fallback to /service/api/intlSamVoucher/getList/{tenantName}/{groupId}.
     * Automatically handles pagination to retrieve the complete dataset.
     */
    suspend fun getVouchers(
        account: RuijieAccount = accountManager.getActiveAccount(),
        groupId: Long = account.selectedGroupId?.toLongOrNull() ?: 0L,
        groupIdStr: String = "",
        start: Int = 0,
        pageSize: Int = 100,
        filter: VoucherFilter = VoucherFilter.ALL,
        fetchAll: Boolean = true,
        retryCount: Int = 0
    ): ApiResult<Pair<List<RuijieVoucherItem>, Int>> = withContext(Dispatchers.IO) {
        val effectiveGidStr = groupIdStr.ifBlank {
            if (groupId > 0L) groupId.toString() else account.selectedGroupIdStr.ifBlank {
                val selGid = account.selectedGroupId?.toLongOrNull() ?: 0L
                if (selGid > 0L) selGid.toString() else ""
            }
        }
        val effectiveNumericGid: Long = effectiveGidStr.toLongOrNull() ?: groupId.takeIf { it > 0L } ?: (account.selectedGroupId?.toLongOrNull() ?: 0L)

        if (effectiveGidStr.isBlank() || effectiveGidStr == "0") {
            return@withContext ApiResult.Error("No project selected. Please select a project first.")
        }

        val isWebViewActive = com.example.data.api.RuijieWebViewBridge.hasValidCookieSession() ||
                com.example.data.api.RuijieWebViewBridge.sessionState.value.isConnected ||
                account.accessToken == "SESSION_COOKIE_ACTIVE"

        val effectiveTenant = resolveTenantName(account)

        val sessionStateStr = com.example.data.api.RuijieWebViewBridge.sessionState.value.displayName
        val endpointPath = "/intlSamVoucher/getList/$effectiveTenant/$effectiveGidStr"

        // Safe diagnostic record
        val diag = com.example.data.model.VoucherDiagnosticInfo(
            sessionState = sessionStateStr,
            webViewAlive = com.example.data.api.RuijieWebViewBridge.isWebViewAttached(),
            cookiePresent = com.example.data.api.RuijieWebViewBridge.hasValidCookieSession(),
            selectedTenantName = effectiveTenant,
            selectedProjectName = account.selectedGroupName,
            selectedGroupIdStr = effectiveGidStr,
            selectedGroupIdNumeric = effectiveNumericGid,
            voucherEndpointPath = endpointPath
        )
        _latestVoucherDiagnostic.value = diag

        Log.i("RuijieVoucher", """
            Selected project: ${account.selectedGroupName}
            Selected group ID: $effectiveGidStr
            Selected tenant: $effectiveTenant
            Session state: $sessionStateStr
        """.trimIndent())

        var currentAccount = account
        if (!isWebViewActive) {
            if (currentAccount.accessToken.isNullOrBlank()) {
                if (retryCount == 0 && currentAccount.isConfigured) {
                    val authRes = authenticate(currentAccount)
                    if (authRes is ApiResult.Success) {
                        currentAccount = accountManager.getActiveAccount()
                    } else {
                        return@withContext ApiResult.Error("Not authenticated. Please authenticate first.")
                    }
                } else {
                    return@withContext ApiResult.Error("Not authenticated. Please authenticate first.")
                }
            }
        }

        // Fetch initial page (start = 0, pageSize = 200 for full sync)
        val initialPageSize = if (fetchAll) 200 else pageSize
        var page1Result = fetchSingleVoucherPage(
            account = currentAccount,
            groupIdStr = effectiveGidStr,
            numericGroupId = effectiveNumericGid,
            start = if (fetchAll) 0 else start,
            pageSize = initialPageSize
        )

        if (page1Result is ApiResult.Error && retryCount == 0 && (page1Result.message.contains("timed out", ignoreCase = true) || page1Result.message.contains("timeout", ignoreCase = true))) {
            return@withContext getVouchers(
                account = currentAccount,
                groupId = groupId,
                groupIdStr = groupIdStr,
                start = start,
                pageSize = pageSize,
                filter = filter,
                fetchAll = fetchAll,
                retryCount = 1
            )
        }

        if (page1Result is ApiResult.Error && page1Result.message == "SESSION_EXPIRED" && retryCount == 0 && currentAccount.isConfigured && !isWebViewActive) {
            val authRes = authenticate(currentAccount)
            if (authRes is ApiResult.Success) {
                return@withContext getVouchers(
                    account = accountManager.getActiveAccount(),
                    groupId = groupId,
                    groupIdStr = groupIdStr,
                    start = start,
                    pageSize = pageSize,
                    filter = filter,
                    fetchAll = fetchAll,
                    retryCount = 1
                )
            }
            return@withContext ApiResult.Error("Ruijie Cloud token expired. Please re-authenticate.")
        }

        if (page1Result !is ApiResult.Success) {
            return@withContext page1Result
        }

        val (firstList, totalCount) = page1Result.data

        if (!fetchAll) {
            val filteredVouchers = when (filter) {
                VoucherFilter.ALL -> firstList
                VoucherFilter.NOT_USED -> firstList.filter { it.normalizedStatus == VoucherStatus.NOT_USED }
                VoucherFilter.USED -> firstList.filter { it.normalizedStatus == VoucherStatus.USED }
                VoucherFilter.EXPIRED -> firstList.filter { it.normalizedStatus == VoucherStatus.EXPIRED }
            }
            return@withContext ApiResult.Success(Pair(filteredVouchers, totalCount))
        }

        // Multi-page fetch to aggregate the COMPLETE dataset across all pages
        val allVouchersMap = LinkedHashMap<String, RuijieVoucherItem>()
        var itemCounter = 0
        fun addItemsToMap(items: List<RuijieVoucherItem>) {
            for (item in items) {
                val u = item.uuid?.trim()
                val idStr = item.id?.trim()
                val code = item.effectiveCode.trim()
                val key = when {
                    !u.isNullOrEmpty() -> "uuid_$u"
                    !idStr.isNullOrEmpty() -> "id_$idStr"
                    code.isNotEmpty() -> "code_$code"
                    else -> "idx_${itemCounter++}"
                }
                if (!allVouchersMap.containsKey(key)) {
                    allVouchersMap[key] = item
                }
            }
        }

        addItemsToMap(firstList)

        var currentStart = firstList.size
        val effectiveTotal = maxOf(totalCount, allVouchersMap.size)
        var consecutiveEmptyPages = 0
        var loopCount = 0

        // Fetch remaining pages until all vouchers are retrieved
        while (currentStart < effectiveTotal && allVouchersMap.size < effectiveTotal && consecutiveEmptyPages < 2 && loopCount < 50) {
            loopCount++
            val pageFetchSize = 200.coerceAtMost(effectiveTotal - allVouchersMap.size).coerceAtLeast(50)
            val nextPageResult = fetchSingleVoucherPage(
                account = currentAccount,
                groupIdStr = effectiveGidStr,
                numericGroupId = effectiveNumericGid,
                start = currentStart,
                pageSize = pageFetchSize
            )

            if (nextPageResult is ApiResult.Success) {
                val (nextList, _) = nextPageResult.data
                if (nextList.isEmpty()) {
                    consecutiveEmptyPages++
                } else {
                    consecutiveEmptyPages = 0
                    val prevSize = allVouchersMap.size
                    addItemsToMap(nextList)
                    currentStart += nextList.size
                }
            } else {
                // If a subsequent page fetch encounters an issue, break and return vouchers collected so far
                break
            }
        }

        val allVouchersList = allVouchersMap.values.toList()
        val finalTotal = maxOf(totalCount, allVouchersList.size)

        val filteredVouchers = when (filter) {
            VoucherFilter.ALL -> allVouchersList
            VoucherFilter.NOT_USED -> allVouchersList.filter { it.normalizedStatus == VoucherStatus.NOT_USED }
            VoucherFilter.USED -> allVouchersList.filter { it.normalizedStatus == VoucherStatus.USED }
            VoucherFilter.EXPIRED -> allVouchersList.filter { it.normalizedStatus == VoucherStatus.EXPIRED }
        }

        ApiResult.Success(Pair(filteredVouchers, finalTotal))
    }

    /**
     * Retrieve Cloud Voucher Packages / Profiles from Ruijie Cloud.
     * NEVER hardcodes profiles — Cloud is the Single Source of Truth.
     */
    suspend fun getVoucherPackages(
        account: RuijieAccount = accountManager.getActiveAccount(),
        groupId: Long = account.selectedGroupId?.toLongOrNull() ?: 0L,
        groupIdStr: String = "",
        retryCount: Int = 0
    ): ApiResult<List<RuijiePackageItem>> = withContext(Dispatchers.IO) {
        val effectiveGidStr = when {
            groupIdStr.isNotBlank() && groupIdStr != "0" -> groupIdStr
            account.selectedGroupIdStr.isNotBlank() && account.selectedGroupIdStr != "0" -> account.selectedGroupIdStr
            groupId > 0L -> groupId.toString()
            else -> ""
        }
        if (effectiveGidStr.isBlank() || effectiveGidStr == "0") {
            return@withContext ApiResult.Error("No project selected. Please select a project first.")
        }

        // 1. Primary: Use WebProxy bridge if session or cookies active
        val isWebViewActive = com.example.data.api.RuijieWebViewBridge.hasValidCookieSession() ||
                com.example.data.api.RuijieWebViewBridge.sessionState.value.isConnected ||
                account.accessToken == "SESSION_COOKIE_ACTIVE"

        if (isWebViewActive) {
            val envelope = com.example.data.api.RuijieProtocol.buildUserGroupListEnvelope(effectiveGidStr)
            val bridgeResp = com.example.data.api.RuijieWebViewBridge.executeWebProxyRequest(envelope)
            if (bridgeResp.isSuccess && !bridgeResp.rawBody.isNullOrBlank()) {
                val userGroups = com.example.data.api.RuijieProtocol.parseUserGroups(bridgeResp.rawBody)
                if (userGroups.isNotEmpty()) {
                    val packages = userGroups.map { ug: com.example.data.model.RuijieUserGroupItem ->
                        RuijiePackageItem(
                            id = ug.id?.toLongOrNull() ?: 0L,
                            name = ug.name,
                            userGroupName = ug.name,
                            userGroupId = ug.id?.toLongOrNull(),
                            packageName = ug.packageName.ifBlank { ug.name },
                            timePeriod = ug.periodTime,
                            duration = "${ug.periodTime / 60} mins",
                            maxClients = ug.maxUsers,
                            authprofileid = ug.profileId.ifBlank { ug.id }
                        )
                    }
                    return@withContext ApiResult.Success(packages)
                }
            } else if (bridgeResp.errorCode != null) {
                // Safe 1 retry for idempotent GET
                if (retryCount == 0 && (bridgeResp.errorCode == com.example.data.model.RuijieBridgeErrorCode.REQUEST_TIMEOUT || bridgeResp.errorCode == com.example.data.model.RuijieBridgeErrorCode.JS_INJECTION_TIMEOUT)) {
                    return@withContext getVoucherPackages(account, groupId, groupIdStr, retryCount = 1)
                }
                val msg = when (bridgeResp.errorCode) {
                    com.example.data.model.RuijieBridgeErrorCode.GROUP_NOT_SYNCHRONIZED ->
                        "Ruijie is still synchronizing this project group. Refresh and try again."
                    com.example.data.model.RuijieBridgeErrorCode.REQUEST_TIMEOUT,
                    com.example.data.model.RuijieBridgeErrorCode.JS_INJECTION_TIMEOUT ->
                        "Ruijie request timed out. Your login session was kept. Please retry."
                    com.example.data.model.RuijieBridgeErrorCode.PERMISSION_DENIED,
                    com.example.data.model.RuijieBridgeErrorCode.RUIJIE_PERMISSION_ERROR ->
                        "The account is authenticated but does not have permission for this operation."
                    com.example.data.model.RuijieBridgeErrorCode.SESSION_EXPIRED ->
                        "Ruijie session expired. Please sign in again."
                    else -> bridgeResp.error ?: bridgeResp.errorCode.userFriendlyMessage
                }
                return@withContext ApiResult.Error(msg)
            }
        }

        if (account.accessToken.isNullOrBlank()) {
            if (retryCount == 0 && account.isConfigured) {
                val authRes = authenticate(account)
                if (authRes is ApiResult.Success) {
                    return@withContext getVoucherPackages(
                        account = accountManager.getActiveAccount(),
                        groupId = groupId,
                        retryCount = 1
                    )
                }
            }
            return@withContext ApiResult.Error("Not authenticated. Please authenticate first.")
        }

        val baseUrl = account.effectiveBaseUrl
        val apiService = apiClient.createService(baseUrl)

        try {
            // 1. Try Primary Open API: /service/api/open/auth/package/getList/{groupId}
            var endpointCalled = "$baseUrl/service/api/open/auth/package/getList/$groupId"
            var response = try {
                apiService.getPackageListOpen(groupId = groupId, accessToken = account.accessToken ?: "")
            } catch (_: Exception) {
                null
            }

            // 2. Try Alternative Open API: /service/api/open/auth/package/list/{groupId}
            if (response == null || response.code() == 404 || response.code() == 400) {
                endpointCalled = "$baseUrl/service/api/open/auth/package/list/$groupId"
                response = try {
                    apiService.getPackageListOpenAlt(groupId = groupId, accessToken = account.accessToken ?: "")
                } catch (_: Exception) {
                    null
                }
            }

            // 3. Try Regional Tenant API: /service/api/intlSamVoucher/getPackageList/{tenantName}/{groupId}
            if (response == null || response.code() == 404 || response.code() == 400) {
                val tenant = (account.tenantName ?: "").ifBlank { "default" }
                endpointCalled = "$baseUrl/service/api/intlSamVoucher/getPackageList/$tenant/$groupId"
                response = try {
                    apiService.getPackageListTenant(tenantName = tenant, groupId = groupId, accessToken = account.accessToken ?: "")
                } catch (_: Exception) {
                    null
                }
            }

            // 4. Try User Group API: /service/api/intl/usergroup/list/{groupId}
            if (response == null || response.code() == 404 || response.code() == 400) {
                endpointCalled = "$baseUrl/service/api/intl/usergroup/list/$groupId"
                response = try {
                    apiService.getUserGroupListIntl(groupId = groupId, accessToken = account.accessToken)
                } catch (_: Exception) {
                    null
                }
            }

            if (response == null) {
                return@withContext ApiResult.Error("Could not reach Ruijie Cloud package endpoints.")
            }

            val httpStatus = response.code()
            val rawBody = response.body()?.string() ?: response.errorBody()?.string() ?: ""

            _latestDiagnostic.value = ApiDiagnosticRecord(
                httpMethod = "GET",
                server = account.server.displayName,
                endpoint = endpointCalled,
                httpStatus = httpStatus,
                rawJson = redactSensitive(rawBody)
            )

            if (isSessionExpired(httpStatus, rawBody)) {
                if (retryCount == 0 && account.isConfigured) {
                    val authRes = authenticate(account)
                    if (authRes is ApiResult.Success) {
                        return@withContext getVoucherPackages(
                            account = accountManager.getActiveAccount(),
                            groupId = groupId,
                            retryCount = 1
                        )
                    }
                }
                return@withContext ApiResult.Error("Ruijie Cloud token expired. Please re-authenticate.")
            }

            if (!response.isSuccessful) {
                return@withContext ApiResult.Error("Ruijie Cloud Package API HTTP $httpStatus: ${response.message()}")
            }

            var packages = parsePackagesFromResponse(rawBody)

            // Ensure packages have Cloud authprofileid and userGroupId populated:
            // If authprofileid or userGroupId is missing, query Cloud User Group API
            val needsEnrichment = packages.isEmpty() || packages.any { it.authprofileid.isNullOrBlank() || it.userGroupId == null }
            if (needsEnrichment) {
                try {
                    val ugRes = apiService.getUserGroupListIntl(groupId = groupId, accessToken = account.accessToken)
                    val ugBody = ugRes.body()?.string() ?: ""
                    if (ugRes.isSuccessful && ugBody.isNotBlank()) {
                        val userGroups = parsePackagesFromResponse(ugBody)
                        if (packages.isEmpty()) {
                            packages = userGroups
                        } else if (userGroups.isNotEmpty()) {
                            packages = packages.map { pkg ->
                                if (!pkg.authprofileid.isNullOrBlank() && pkg.userGroupId != null) {
                                    pkg
                                } else {
                                    val matched = userGroups.firstOrNull { ug ->
                                        ug.effectiveName.equals(pkg.effectiveName, ignoreCase = true) ||
                                        ug.effectiveId == pkg.effectiveId ||
                                        (!ug.authprofileid.isNullOrBlank() && ug.authprofileid == pkg.authprofileid)
                                    }
                                    if (matched != null) {
                                        pkg.copy(
                                            authprofileid = pkg.authprofileid ?: matched.authprofileid ?: matched.effectiveProfileUuid.ifBlank { null },
                                            userGroupId = pkg.userGroupId ?: matched.userGroupId ?: matched.id
                                        )
                                    } else {
                                        pkg
                                    }
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            ApiResult.Success(packages)
        } catch (e: Exception) {
            ApiResult.Error("Failed to retrieve Cloud voucher packages: ${e.localizedMessage}", e)
        }
    }

    /**
     * Send voucher generation request directly to Ruijie Cloud.
     * Documented endpoint: POST /service/api/open/auth/voucher/create/{groupId}
     * Request: { "quantity": N, "profile": "<authprofileid>", "userGroupId": <id> }
     * Expected response: { "code": 0, "voucherData": { "code": 0, "count": N, "list": [...] } }
     */
    suspend fun createVouchers(
        account: RuijieAccount = accountManager.getActiveAccount(),
        groupId: Long = account.selectedGroupId?.toLongOrNull() ?: 0L,
        groupIdStr: String = "",
        packageItem: RuijiePackageItem,
        quantity: Int = 1,
        amount: Int = quantity,
        comment: String = "",
        remark: String = comment,
        voucherLength: Int = 8,
        voucherCodeType: VoucherCodeType = VoucherCodeType.ALPHANUMERIC,
        firstName: String? = null,
        lastName: String? = null,
        email: String? = null,
        phone: String? = null,
        knownPackages: List<RuijiePackageItem> = emptyList(),
        retryCount: Int = 0
    ): ApiResult<List<RuijieVoucherItem>> = withContext(Dispatchers.IO) {
        val effectiveGidStr = when {
            groupIdStr.isNotBlank() && groupIdStr != "0" -> groupIdStr
            account.selectedGroupIdStr.isNotBlank() && account.selectedGroupIdStr != "0" -> account.selectedGroupIdStr
            groupId > 0L -> groupId.toString()
            else -> ""
        }
        if (effectiveGidStr.isBlank() || effectiveGidStr == "0") {
            return@withContext ApiResult.Error("No project selected. Please select a project first.")
        }

        val effectiveQuantity = if (quantity != 1) quantity else amount
        if (effectiveQuantity < 1 || effectiveQuantity > 500) {
            return@withContext ApiResult.Error("Quantity must be between 1 and 500 (Ruijie Cloud maximum is 500).")
        }

        val validLength = when (voucherLength) {
            6, 7, 8, 9 -> voucherLength
            else -> 8
        }

        val ugId = packageItem.userGroupId?.toString()?.ifBlank { null }
            ?: packageItem.id?.toString()?.ifBlank { null }
            ?: ""
        if (ugId.isBlank() || ugId == "0") {
            return@withContext ApiResult.Error("Please select a Cloud package / user group first.")
        }
        val profId = packageItem.authprofileid?.ifBlank { null }
            ?: packageItem.id?.toString()?.ifBlank { null }
            ?: ugId

        // 1. Primary: Use WebProxy bridge if session or cookies active
        val isWebViewActive = com.example.data.api.RuijieWebViewBridge.hasValidCookieSession() ||
                com.example.data.api.RuijieWebViewBridge.sessionState.value.isConnected ||
                account.accessToken == "SESSION_COOKIE_ACTIVE"

        if (isWebViewActive) {
            val tenant = resolveTenantName(account)
            val userName = com.example.data.api.RuijieWebViewBridge.lastAccountName.value.ifBlank {
                account.username.ifBlank { (account.email ?: "").ifBlank { "admin" } }
            }
            val envelope = com.example.data.api.RuijieProtocol.buildVoucherCreateEnvelope(
                tenantName = tenant,
                userName = userName,
                groupId = effectiveGidStr,
                count = effectiveQuantity,
                codeType = when (voucherCodeType) {
                    VoucherCodeType.ALPHABETIC -> "Alphabetic"
                    VoucherCodeType.NUMERIC -> "Numeric"
                    else -> "Alphanumeric"
                },
                length = validLength,
                userGroupId = ugId,
                profileId = profId
            )
            val bridgeResp = com.example.data.api.RuijieWebViewBridge.executeWebProxyRequest(envelope, timeoutMs = 20_000L)
            if (bridgeResp.isSuccess && !bridgeResp.rawBody.isNullOrBlank()) {
                return@withContext try {
                    val parsed = com.example.data.api.RuijieProtocol.parseVoucherCreateResponseParsed(bridgeResp.rawBody, bridgeResp.httpStatus)

                    com.example.data.diagnostics.RuijieDiagnosticCollector.updateVoucherGenSummary(
                        com.example.data.diagnostics.RuijieDiagnosticCollector.voucherGenSummary.value.copy(
                            lastCreateCode = parsed.ruijieCode,
                            lastCreateMessage = parsed.safeMsg,
                            parsedCreatedCode = parsed.vouchers.firstOrNull()?.effectiveCode ?: "",
                            confirmationStatus = if (parsed.vouchers.isNotEmpty()) "Confirmed" else "Accepted, awaiting list confirmation",
                            isCreationResultUnknown = parsed.isAcceptedWithoutCode,
                            lastResultCategory = if (parsed.vouchers.isNotEmpty()) com.example.data.model.DiagnosticResultCategory.SUCCESS else com.example.data.model.DiagnosticResultCategory.EMPTY
                        )
                    )

                    if (parsed.vouchers.isNotEmpty()) {
                        ApiResult.Success(parsed.vouchers)
                    } else if (parsed.isAcceptedWithoutCode) {
                        // Refresh live list once to confirm creation
                        val refreshResult = getVouchers(
                            account = account,
                            groupId = groupId,
                            groupIdStr = effectiveGidStr,
                            start = 0,
                            pageSize = 20
                        )
                        val confirmedVoucher = if (refreshResult is ApiResult.Success) {
                            val list = refreshResult.data.first
                            list.firstOrNull { v ->
                                ugId.isBlank() || v.userGroupId == ugId || v.userGroupName == packageItem.name
                            } ?: list.firstOrNull()
                        } else null

                        if (confirmedVoucher != null) {
                            com.example.data.diagnostics.RuijieDiagnosticCollector.updateVoucherGenSummary(
                                com.example.data.diagnostics.RuijieDiagnosticCollector.voucherGenSummary.value.copy(
                                    parsedCreatedCode = confirmedVoucher.effectiveCode,
                                    confirmationStatus = "Confirmed",
                                    isCreationResultUnknown = false
                                )
                            )
                            ApiResult.Success(listOf(confirmedVoucher))
                        } else {
                            ApiResult.Error("Ruijie accepted the request. Refresh the live voucher list to confirm whether the voucher was created. Create result could not be confirmed from the live voucher list.")
                        }
                    } else {
                        ApiResult.Error("Ruijie accepted the create request, but no voucher was returned.")
                    }
                } catch (e: Exception) {
                    if (e is com.example.data.model.RuijieGroupNotSynchronizedException || e.message?.contains("synchroniz", ignoreCase = true) == true) {
                        ApiResult.Error("Ruijie is still synchronizing this project group. Please try again in a moment.")
                    } else {
                        ApiResult.Error(e.message ?: "Failed to parse created vouchers")
                    }
                }
            } else {
                val errorMsg = when (bridgeResp.errorCode) {
                    com.example.data.model.RuijieBridgeErrorCode.REQUEST_TIMEOUT,
                    com.example.data.model.RuijieBridgeErrorCode.JS_INJECTION_TIMEOUT ->
                        "Voucher creation timed out. Refreshing list to verify if vouchers were created..."
                    com.example.data.model.RuijieBridgeErrorCode.GROUP_NOT_SYNCHRONIZED ->
                        "Ruijie is still synchronizing this project group. Please try again in a moment."
                    com.example.data.model.RuijieBridgeErrorCode.PERMISSION_DENIED,
                    com.example.data.model.RuijieBridgeErrorCode.RUIJIE_PERMISSION_ERROR ->
                        "Account lacks permission to create vouchers in this group."
                    com.example.data.model.RuijieBridgeErrorCode.SESSION_EXPIRED ->
                        "Ruijie session expired. Please sign in again."
                    else -> bridgeResp.error ?: bridgeResp.errorCode?.userFriendlyMessage ?: "Failed to generate vouchers in Ruijie Cloud."
                }
                return@withContext ApiResult.Error(errorMsg)
            }
        }

        if (account.accessToken.isNullOrBlank()) {
            if (retryCount == 0 && account.isConfigured) {
                val authRes = authenticate(account)
                if (authRes is ApiResult.Success) {
                    return@withContext createVouchers(
                        account = accountManager.getActiveAccount(),
                        groupId = groupId,
                        packageItem = packageItem,
                        quantity = effectiveQuantity,
                        comment = comment.ifBlank { remark },
                        voucherLength = voucherLength,
                        voucherCodeType = voucherCodeType,
                        firstName = firstName,
                        lastName = lastName,
                        email = email,
                        phone = phone,
                        knownPackages = knownPackages,
                        retryCount = 1
                    )
                }
            }
            return@withContext ApiResult.Error("Not authenticated. Please authenticate first.")
        }

        val baseUrl = account.effectiveBaseUrl
        val apiService = apiClient.createService(baseUrl)

        // 4. PROFILE ID & 5. USER GROUP ID
        // The generation request requires:
        // "profile" = Profile package UUID obtained from authprofileid
        // "userGroupId" = obtained from user group id
        var profileUuid = packageItem.effectiveProfileUuid
        var userGroupIdVal = packageItem.userGroupId ?: packageItem.id

        // If either profileUuid or userGroupIdVal is missing, query Cloud User Group API to resolve them
        if (profileUuid.isBlank() || userGroupIdVal == null || userGroupIdVal.toString().isBlank()) {
            try {
                val ugRes = apiService.getUserGroupListIntl(groupId = groupId, accessToken = account.accessToken)
                val ugBody = ugRes.body()?.string() ?: ""
                if (ugRes.isSuccessful && ugBody.isNotBlank()) {
                    val groups = parsePackagesFromResponse(ugBody)
                    val matchedGroup = groups.firstOrNull { g ->
                        g.effectiveId == packageItem.effectiveId ||
                        g.effectiveName.equals(packageItem.effectiveName, ignoreCase = true) ||
                        (!g.authprofileid.isNullOrBlank() && g.authprofileid == packageItem.authprofileid)
                    } ?: groups.firstOrNull { it.effectiveProfileUuid.isNotBlank() }

                    if (matchedGroup != null) {
                        if (profileUuid.isBlank()) profileUuid = matchedGroup.effectiveProfileUuid
                        if (userGroupIdVal == null) userGroupIdVal = matchedGroup.effectiveUserGroupId
                    }
                }
            } catch (_: Exception) {}
        }

        if (profileUuid.isBlank()) {
            return@withContext ApiResult.Error("Could not determine Cloud profile UUID (authprofileid) for package '${packageItem.effectiveName}'.")
        }

        if (userGroupIdVal == null || userGroupIdVal.toString().isBlank()) {
            return@withContext ApiResult.Error("Could not determine Cloud userGroupId for package '${packageItem.effectiveName}'.")
        }

        // Ruijie Cloud Voucher Code Type Mapping:
        // Confirmed working mapping from Nang Oo Voucher v5:
        // Alphanumeric -> createCodeType = "1"
        // Alphabetic   -> createCodeType = "2"
        // Numeric      -> createCodeType = "3"
        val createCodeTypeVal = when (voucherCodeType) {
            VoucherCodeType.ALPHANUMERIC -> "1"
            VoucherCodeType.ALPHABETIC -> "2"
            VoucherCodeType.NUMERIC -> "3"
        }
        val safeLength = voucherLength.coerceIn(6, 9)

        // 6. GENERATION REQUEST: Build documented JSON payload matching working Ruijie Cloud specification
        val jsonBody = JSONObject().apply {
            put("quantity", effectiveQuantity)
            put("profile", profileUuid)
            val ugLong = when (userGroupIdVal) {
                is Number -> userGroupIdVal.toLong()
                is String -> userGroupIdVal.toLongOrNull()
                else -> null
            }
            if (ugLong != null) {
                put("userGroupId", ugLong)
            } else {
                put("userGroupId", userGroupIdVal.toString())
            }
            put("createCodeType", createCodeTypeVal)
            put("codeSize", safeLength)
            val effectiveComment = comment.ifBlank { remark }.trim()
            if (effectiveComment.isNotBlank()) {
                put("comment", effectiveComment)
            }
            put("packageName", packageItem.effectiveName)
            put("profileName", packageItem.effectiveName)
            put("userGroupName", packageItem.effectiveName)
            if (!firstName.isNullOrBlank()) put("firstName", firstName.trim())
            if (!lastName.isNullOrBlank()) put("lastName", lastName.trim())
            if (!email.isNullOrBlank()) put("email", email.trim())
            if (!phone.isNullOrBlank()) put("phone", phone.trim())
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        try {
            val tenant = (account.tenantName ?: "").ifBlank { "default" }
            val userName = account.accountName.ifBlank { "admin" }

            // Primary Ruijie Cloud Open API specification (matches proven Nang Oo Voucher v5 implementation)
            // POST /service/api/open/auth/voucher/create/{groupId}?access_token={accessToken}
            var endpointCalled = "$baseUrl/service/api/open/auth/voucher/create/$groupId"
            var response = try {
                apiService.createVoucherOpen(
                    groupId = groupId,
                    accessToken = account.accessToken ?: "",
                    body = requestBody
                )
            } catch (_: Exception) {
                null
            }

            // Fallback 1: Web endpoint if open API returns 404 or fails
            if (response == null || !response.isSuccessful && response.code() == 404) {
                endpointCalled = "$baseUrl/service/api/intlSamVoucher/create/$tenant/$userName/$groupId"
                response = try {
                    apiService.createVoucherWeb(
                        tenantName = tenant,
                        userName = userName,
                        groupId = groupId,
                        accessToken = account.accessToken ?: "",
                        queryGroupId = groupId,
                        lang = "en",
                        body = requestBody
                    )
                } catch (_: Exception) {
                    null
                }
            }

            // Fallback 2: Tenant endpoint if 404
            if (response == null || !response.isSuccessful && response.code() == 404) {
                endpointCalled = "$baseUrl/service/api/intlSamVoucher/create/$tenant/$groupId"
                response = apiService.createVoucherTenant(
                    tenantName = tenant,
                    groupId = groupId,
                    accessToken = account.accessToken ?: "",
                    body = requestBody
                )
            }

            val httpStatus = response.code()
            val rawBody = response.body()?.string() ?: response.errorBody()?.string() ?: ""

            if (isSessionExpired(httpStatus, rawBody)) {
                if (retryCount == 0 && account.isConfigured) {
                    val authRes = authenticate(account)
                    if (authRes is ApiResult.Success) {
                        return@withContext createVouchers(
                            account = accountManager.getActiveAccount(),
                            groupId = groupId,
                            packageItem = packageItem,
                            quantity = effectiveQuantity,
                            comment = comment.ifBlank { remark },
                            voucherLength = voucherLength,
                            voucherCodeType = voucherCodeType,
                            firstName = firstName,
                            lastName = lastName,
                            email = email,
                            phone = phone,
                            knownPackages = knownPackages,
                            retryCount = 1
                        )
                    }
                }
                return@withContext ApiResult.Error("Ruijie Cloud token expired. Please re-authenticate.")
            }

            // Parse response JSON
            val jsonRoot = try { JSONObject(rawBody) } catch (_: Exception) { null }
            val rootCode = jsonRoot?.optInt("code", if (response.isSuccessful) 0 else httpStatus) ?: if (response.isSuccessful) 0 else httpStatus
            val rootMsg = jsonRoot?.optString("msg", jsonRoot.optString("message", response.message())) ?: ""

            // Nested voucherData object
            val voucherDataObj = jsonRoot?.optJSONObject("voucherData")
            val voucherDataCode = if (voucherDataObj != null && voucherDataObj.has("code")) voucherDataObj.optInt("code") else null
            val voucherDataMsg = voucherDataObj?.optString("msg", voucherDataObj.optString("message", ""))
            val voucherDataCount = if (voucherDataObj != null && voucherDataObj.has("count")) voucherDataObj.optInt("count") else null
            val voucherListArray = voucherDataObj?.optJSONArray("list")

            // 11. Record API Diagnostic (tokens & secrets redacted)
            _latestDiagnostic.value = ApiDiagnosticRecord(
                httpMethod = "POST",
                server = account.server.displayName,
                endpoint = endpointCalled,
                httpStatus = httpStatus,
                responseCode = rootCode,
                responseMessage = rootMsg,
                voucherDataCode = voucherDataCode,
                voucherDataMessage = voucherDataMsg,
                voucherCount = voucherDataCount,
                returnedListCount = voucherListArray?.length() ?: 0,
                rawJson = redactSensitive(rawBody)
            )

            // Extract first generated voucher code for deep character set analysis
            val firstCode = voucherListArray?.optJSONObject(0)?.let {
                it.optString("codeNo", it.optString("voucherCode", it.optString("code", "")))
            } ?: ""

            val (verdict, reasonText) = if (rootCode != 0) {
                Pair(DiagnosisVerdict.FAIL, "Ruijie Cloud returned error: Code $rootCode, Message: $rootMsg")
            } else if (voucherDataCode != null && voucherDataCode != 0) {
                Pair(DiagnosisVerdict.FAIL, "Ruijie Cloud Voucher API returned error: Code $voucherDataCode, Message: $voucherDataMsg")
            } else if (firstCode.isBlank()) {
                Pair(DiagnosisVerdict.FAIL, "Ruijie Cloud did not return a voucher code in the response list.")
            } else {
                when (voucherCodeType) {
                    VoucherCodeType.ALPHANUMERIC -> {
                        Pair(DiagnosisVerdict.PASS, "Ruijie Cloud generated code '$firstCode'. Alphanumeric format matches Cloud specification (createCodeType=1).")
                    }
                    VoucherCodeType.ALPHABETIC -> {
                        val hasDigits = firstCode.any { it.isDigit() }
                        if (hasDigits) {
                            Pair(
                                DiagnosisVerdict.FAIL,
                                "Ruijie Cloud returned code '$firstCode' containing digits when Alphabetic (createCodeType=2, a-z) was requested."
                            )
                        } else {
                            Pair(DiagnosisVerdict.PASS, "Ruijie Cloud generated alphabetic code '$firstCode' (createCodeType=2).")
                        }
                    }
                    VoucherCodeType.NUMERIC -> {
                        val hasLetters = firstCode.any { it.isLetter() }
                        if (hasLetters) {
                            Pair(
                                DiagnosisVerdict.FAIL,
                                "Ruijie Cloud returned code '$firstCode' containing letters when Numeric (createCodeType=3, 0-9) was requested."
                            )
                        } else {
                            Pair(DiagnosisVerdict.PASS, "Ruijie Cloud generated numeric code '$firstCode' (createCodeType=3).")
                        }
                    }
                }
            }

            val genDiagnostic = VoucherGenerationDiagnosticRecord(
                timestamp = System.currentTimeMillis(),
                selectedCodeType = voucherCodeType,
                uiValue = "${voucherCodeType.label} (${voucherCodeType.subtext})",
                mappedApiValue = "createCodeType=$createCodeTypeVal, codeSize=$safeLength",
                endpoint = endpointCalled,
                httpMethod = "POST",
                requestQuery = "access_token=********",
                sanitizedRequestBody = redactSensitive(jsonBody.toString(2)),
                responseHttpStatus = httpStatus,
                sanitizedResponseBody = redactSensitive(rawBody),
                returnedVoucherField = if (firstCode.isNotBlank()) "voucherData.list[0].codeNo" else "(none)",
                returnedVoucherCode = firstCode,
                codeTypeResult = verdict,
                reason = reasonText
            )
            _latestVoucherGenDiagnostic.value = genDiagnostic
            _voucherGenLogs.value = (listOf(genDiagnostic) + _voucherGenLogs.value).take(50)

            // 10. Check root error code: response.code != 0
            if (rootCode != 0) {
                val err = "Generation Failed\nCode: $rootCode\nMessage: ${rootMsg.ifBlank { "Parameter value is invalid or request rejected by Cloud." }}"
                return@withContext ApiResult.Error(err)
            }

            // 10. Check nested voucherData error code: response.code == 0 but voucherData.code != 0
            if (voucherDataObj != null && voucherDataCode != null && voucherDataCode != 0) {
                val err = "Generation Failed\nVoucher API Code: $voucherDataCode\nMessage: ${voucherDataMsg?.ifBlank { "Cloud Voucher API error" }}"
                return@withContext ApiResult.Error(err)
            }

            // 1. Check success: response.code == 0 AND voucherData exists AND voucherData.code == 0 AND voucherData.list is not empty
            if (voucherListArray != null && voucherListArray.length() > 0) {
                val vouchers = parseGeneratedVoucherList(
                    array = voucherListArray,
                    fallbackPackage = packageItem,
                    knownPackages = knownPackages
                )
                if (vouchers.isNotEmpty()) {
                    return@withContext ApiResult.Success(vouchers)
                }
            }

            // 9. If response.code == 0 but response.voucherData.list is missing or empty
            // Check if vouchers were created on Cloud by querying voucher list API
            try {
                val vouchersRes = getVouchers(
                    account = account,
                    groupId = groupId,
                    start = 0,
                    pageSize = (effectiveQuantity * 2).coerceAtLeast(20)
                )
                if (vouchersRes is ApiResult.Success && vouchersRes.data.first.isNotEmpty()) {
                    val candidateVouchers = vouchersRes.data.first.take(effectiveQuantity)
                    if (candidateVouchers.isNotEmpty()) {
                        return@withContext ApiResult.Success(candidateVouchers)
                    }
                }
            } catch (_: Exception) {}

            return@withContext ApiResult.Error("Cloud accepted the generation request, but no voucher records were returned.")
        } catch (e: Exception) {
            ApiResult.Error("Error generating Ruijie Cloud vouchers: ${e.localizedMessage}", e)
        }
    }

    /**
     * Builds the JSON payload for voucher deletion with full coverage of Ruijie Cloud identifier formats:
     * - "uuids": JSONArray of Cloud UUIDs (primary Ruijie parameter)
     * - "uuidList": JSONArray of Cloud UUIDs
     * - "ids": JSONArray of Cloud UUIDs / IDs
     * - "idList": JSONArray of Cloud UUIDs / IDs
     * - "uuid": single UUID string (if single voucher)
     * - "id": single UUID/ID string (if single voucher)
     * - "codes": JSONArray of Voucher Code strings
     * - "codeList": JSONArray of Voucher Code strings
     * - "voucherCodes": JSONArray of Voucher Code strings
     * - "codeNos": JSONArray of Voucher Code strings
     * - "code": single Code string (if single voucher)
     * - "codeNo": single Code string (if single voucher)
     * - "voucherCode": single Code string (if single voucher)
     * - "groupId" & "group_id": selected group ID
     */
    fun buildDeletePayload(vouchers: List<RuijieVoucherItem>, groupId: Long, account: RuijieAccount): JSONObject {
        val json = JSONObject()
        val uuids = vouchers.mapNotNull { it.uuid?.ifBlank { null } ?: it.id?.ifBlank { null } }.distinct()
        val codes = vouchers.map { it.effectiveCode }.filter { it.isNotBlank() }.distinct()

        val uuidArray = JSONArray()
        uuids.forEach { uuidArray.put(it) }

        val codeArray = JSONArray()
        codes.forEach { codeArray.put(it) }

        // Array of UUIDs
        json.put("uuids", uuidArray)
        json.put("uuidList", uuidArray)
        json.put("ids", uuidArray)
        json.put("idList", uuidArray)

        if (uuids.size == 1) {
            json.put("uuid", uuids.first())
            json.put("id", uuids.first())
        }

        // Array of Voucher Codes
        json.put("codes", codeArray)
        json.put("codeList", codeArray)
        json.put("voucherCodes", codeArray)
        json.put("codeNos", codeArray)

        if (codes.size == 1) {
            json.put("code", codes.first())
            json.put("codeNo", codes.first())
            json.put("voucherCode", codes.first())
        }

        json.put("groupId", groupId)
        json.put("group_id", groupId)
        if (!account.tenantName.isNullOrBlank()) {
            json.put("tenantName", account.tenantName)
        }
        if (account.tenantId != 0L) {
            json.put("tenantId", account.tenantId)
        }

        return json
    }

    /**
     * Executes Cloud voucher deletion for a batch or single list of vouchers.
     * Uses verified Ruijie Cloud Open API and Web/SAM routes in cascading priority.
     * Records API diagnostic records for full transparency in Diagnostics dialog.
     */
    suspend fun deleteVouchersBatch(
        account: RuijieAccount = accountManager.getActiveAccount(),
        groupId: Long = account.selectedGroupId?.toLongOrNull() ?: 0L,
        vouchers: List<RuijieVoucherItem>,
        retryCount: Int = 0
    ): ApiResult<Int> = withContext(Dispatchers.IO) {
        if (vouchers.isEmpty()) {
            return@withContext ApiResult.Success(0)
        }
        if (groupId == 0L) {
            return@withContext ApiResult.Error("No project selected.")
        }

        // 1. Primary: Use WebProxy bridge if session or cookies active
        if (com.example.data.api.RuijieWebViewBridge.hasValidCookieSession() || com.example.data.api.RuijieWebViewBridge.sessionState.value.isConnected || account.accessToken == "SESSION_COOKIE_ACTIVE") {
            val tenant = (account.tenantName ?: "").ifBlank { com.example.data.api.RuijieWebViewBridge.lastTenantName.value.ifBlank { "default" } }
            var deletedCount = 0
            for (voucher in vouchers) {
                val envelope = com.example.data.api.RuijieProtocol.buildVoucherDeleteEnvelope(
                    groupId = groupId.toString(),
                    ids = voucher.uuid ?: voucher.id ?: voucher.effectiveCode,
                    voucherCode = voucher.effectiveCode
                )
                val bridgeResp = com.example.data.api.RuijieWebViewBridge.executeWebProxyRequest(envelope)
                if (bridgeResp.isSuccess) {
                    deletedCount++
                }
            }
            if (deletedCount > 0) {
                return@withContext ApiResult.Success(deletedCount)
            } else {
                return@withContext ApiResult.Error("Failed to delete voucher(s) in Ruijie Cloud.")
            }
        }

        if (account.accessToken.isNullOrBlank()) {
            return@withContext ApiResult.Error("Not authenticated with Ruijie Cloud.")
        }

        val baseUrl = account.effectiveBaseUrl
        val apiService = apiClient.createService(baseUrl)
        val tenant = (account.tenantName ?: "").ifBlank { "default" }
        val userName = account.username.ifBlank { "admin" }

        try {
            val jsonPayload = buildDeletePayload(vouchers, groupId, account)
            val requestBody = jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

            var response: Response<ResponseBody>? = null
            var endpointCalled = "service/api/open/auth/voucher/del/$groupId"
            var lastHttpCode = -1
            var lastRawBody = ""

            // Endpoint Cascade:
            // 1. Open API del (Primary Open API delete route)
            try {
                endpointCalled = "service/api/open/auth/voucher/del/$groupId"
                response = apiService.deleteVoucherOpenDel(
                    groupId = groupId,
                    accessToken = account.accessToken ?: "",
                    body = requestBody
                )
            } catch (_: Exception) {}

            // 2. Open API delete (Alternative Open API delete route)
            if (response == null || !response.isSuccessful) {
                try {
                    endpointCalled = "service/api/open/auth/voucher/delete/$groupId"
                    response = apiService.deleteVoucherOpen(
                        groupId = groupId,
                        accessToken = account.accessToken ?: "",
                        body = requestBody
                    )
                } catch (_: Exception) {}
            }

            // 3. Open API batchDel
            if (response == null || !response.isSuccessful) {
                try {
                    endpointCalled = "service/api/open/auth/voucher/batchDel/$groupId"
                    response = apiService.batchDelVoucherOpen(
                        groupId = groupId,
                        accessToken = account.accessToken ?: "",
                        body = requestBody
                    )
                } catch (_: Exception) {}
            }

            // 4. Open API batchDelete
            if (response == null || !response.isSuccessful) {
                try {
                    endpointCalled = "service/api/open/auth/voucher/batchDelete/$groupId"
                    response = apiService.batchDeleteVoucherOpen(
                        groupId = groupId,
                        accessToken = account.accessToken ?: "",
                        body = requestBody
                    )
                } catch (_: Exception) {}
            }

            // 5. Ruijie SAM Web del (matching createVoucherWeb route pattern: /intlSamVoucher/del/{tenant}/{user}/{groupId})
            if (response == null || !response.isSuccessful) {
                try {
                    endpointCalled = "service/api/intlSamVoucher/del/$tenant/$userName/$groupId"
                    response = apiService.deleteVoucherWeb(
                        tenantName = tenant,
                        userName = userName,
                        groupId = groupId,
                        accessToken = account.accessToken ?: "",
                        queryGroupId = groupId,
                        body = requestBody
                    )
                } catch (_: Exception) {}
            }

            // 6. Ruijie SAM Web delete
            if (response == null || !response.isSuccessful) {
                try {
                    endpointCalled = "service/api/intlSamVoucher/delete/$tenant/$userName/$groupId"
                    response = apiService.deleteVoucherWebAction(
                        tenantName = tenant,
                        userName = userName,
                        groupId = groupId,
                        accessToken = account.accessToken ?: "",
                        queryGroupId = groupId,
                        body = requestBody
                    )
                } catch (_: Exception) {}
            }

            // 7. Ruijie SAM Web batchDel
            if (response == null || !response.isSuccessful) {
                try {
                    endpointCalled = "service/api/intlSamVoucher/batchDel/$tenant/$userName/$groupId"
                    response = apiService.batchDeleteVoucherWeb(
                        tenantName = tenant,
                        userName = userName,
                        groupId = groupId,
                        accessToken = account.accessToken ?: "",
                        queryGroupId = groupId,
                        body = requestBody
                    )
                } catch (_: Exception) {}
            }

            // 8. Ruijie SAM Web batchDelete
            if (response == null || !response.isSuccessful) {
                try {
                    endpointCalled = "service/api/intlSamVoucher/batchDelete/$tenant/$userName/$groupId"
                    response = apiService.batchDeleteVoucherWebAction(
                        tenantName = tenant,
                        userName = userName,
                        groupId = groupId,
                        accessToken = account.accessToken ?: "",
                        queryGroupId = groupId,
                        body = requestBody
                    )
                } catch (_: Exception) {}
            }

            // 9. Regional Tenant API del (/intlSamVoucher/del/{tenant}/{groupId})
            if (response == null || !response.isSuccessful) {
                try {
                    endpointCalled = "service/api/intlSamVoucher/del/$tenant/$groupId"
                    response = apiService.deleteVoucherTenant(
                        tenantName = tenant,
                        groupId = groupId,
                        accessToken = account.accessToken ?: "",
                        body = requestBody
                    )
                } catch (_: Exception) {}
            }

            // 10. Regional Tenant API delete (/intlSamVoucher/delete/{tenant}/{groupId})
            if (response == null || !response.isSuccessful) {
                try {
                    endpointCalled = "service/api/intlSamVoucher/delete/$tenant/$groupId"
                    response = apiService.deleteVoucherTenantAction(
                        tenantName = tenant,
                        groupId = groupId,
                        accessToken = account.accessToken ?: "",
                        body = requestBody
                    )
                } catch (_: Exception) {}
            }

            if (response != null) {
                lastHttpCode = response.code()
                val bodyStr = if (response.isSuccessful) {
                    response.body()?.string() ?: ""
                } else {
                    response.errorBody()?.string() ?: ""
                }
                lastRawBody = bodyStr

                // Parse response
                val rootObj = try { JSONObject(bodyStr) } catch (_: Exception) { null }
                val code = rootObj?.optInt("code", if (response.isSuccessful) 0 else lastHttpCode) ?: if (response.isSuccessful) 0 else lastHttpCode
                val msg = rootObj?.optString("msg", rootObj?.optString("message", response.message())) ?: ""

                // Record Diagnostics for user inspection
                val diagRecord = ApiDiagnosticRecord(
                    timestamp = System.currentTimeMillis(),
                    httpMethod = "POST",
                    server = account.server.displayName,
                    endpoint = endpointCalled,
                    httpStatus = lastHttpCode,
                    rawJson = redactSensitive(bodyStr),
                    responseCode = code,
                    responseMessage = msg
                )
                _latestDiagnostic.value = diagRecord

                // Check session expiration
                if (isSessionExpired(lastHttpCode, bodyStr) && retryCount == 0 && account.isConfigured) {
                    val refreshed = authenticate(account)
                    if (refreshed is ApiResult.Success && !refreshed.data.accessToken.isNullOrBlank()) {
                        return@withContext deleteVouchersBatch(
                            account = refreshed.data,
                            groupId = groupId,
                            vouchers = vouchers,
                            retryCount = 1
                        )
                    }
                    return@withContext ApiResult.Error("Ruijie Cloud session expired. Please re-authenticate in Settings.")
                }

                if (response.isSuccessful && (code == 0 || code == 200)) {
                    return@withContext ApiResult.Success(vouchers.size)
                } else if (response.isSuccessful && code != 0) {
                    return@withContext ApiResult.Error("Ruijie Cloud: ${msg.ifBlank { "Operation rejected with code $code" }}")
                }
            }

            val httpCode = lastHttpCode
            val diagRecord = ApiDiagnosticRecord(
                timestamp = System.currentTimeMillis(),
                httpMethod = "POST",
                server = account.server.displayName,
                endpoint = endpointCalled,
                httpStatus = httpCode,
                rawJson = redactSensitive(lastRawBody),
                responseCode = httpCode,
                responseMessage = "HTTP Error $httpCode"
            )
            _latestDiagnostic.value = diagRecord

            if (httpCode == 401 || httpCode == 403) {
                return@withContext ApiResult.Error("Ruijie Cloud authentication failed (HTTP $httpCode). Please re-authenticate.")
            }

            return@withContext ApiResult.Error(
                "Ruijie Cloud deletion failed on project $groupId (HTTP $httpCode).\nEndpoint: $endpointCalled\n${lastRawBody.take(150)}"
            )
        } catch (e: Exception) {
            val diagRecord = ApiDiagnosticRecord(
                timestamp = System.currentTimeMillis(),
                httpMethod = "POST",
                server = account.server.displayName,
                endpoint = "service/api/open/auth/voucher/del/$groupId",
                httpStatus = 0,
                rawJson = redactSensitive(e.localizedMessage ?: "Unknown exception"),
                responseCode = -1,
                responseMessage = e.localizedMessage ?: "Network error"
            )
            _latestDiagnostic.value = diagRecord
            ApiResult.Error("Cloud error: ${e.localizedMessage}", e)
        }
    }

    /**
     * Executes Cloud single voucher deletion.
     * Delegates to deleteVouchersBatch with single item.
     * Returns Success(true) if Cloud API succeeds, or descriptive error if Cloud rejects.
     */
    suspend fun deleteVoucher(
        account: RuijieAccount = accountManager.getActiveAccount(),
        groupId: Long = account.selectedGroupId?.toLongOrNull() ?: 0L,
        voucher: RuijieVoucherItem
    ): ApiResult<Boolean> = withContext(Dispatchers.IO) {
        when (val res = deleteVouchersBatch(account, groupId, listOf(voucher))) {
            is ApiResult.Success -> ApiResult.Success(true)
            is ApiResult.Error -> ApiResult.Error(res.message, res.cause)
        }
    }

    /**
     * Unbinds the voucher device MAC binding and disconnects active session in Ruijie Cloud.
     * Tries Open API unbind, unbindMac, Web SAM UI unbind, and terminal disconnect cascade.
     */
    suspend fun unbindVoucher(
        account: RuijieAccount = accountManager.getActiveAccount(),
        groupId: Long = account.selectedGroupId?.toLongOrNull() ?: 0L,
        voucher: RuijieVoucherItem,
        retryCount: Int = 0
    ): ApiResult<RuijieVoucherItem> = withContext(Dispatchers.IO) {
        if (groupId == 0L) {
            return@withContext ApiResult.Error("No project selected.")
        }

        // 1. Primary: Use WebProxy bridge if session or cookies active
        if (com.example.data.api.RuijieWebViewBridge.hasValidCookieSession() || com.example.data.api.RuijieWebViewBridge.sessionState.value.isConnected || account.accessToken == "SESSION_COOKIE_ACTIVE") {
            val tenant = (account.tenantName ?: "").ifBlank { com.example.data.api.RuijieWebViewBridge.lastTenantName.value.ifBlank { "default" } }
            val envelope = com.example.data.api.RuijieProtocol.buildVoucherUnbindEnvelope(
                groupId = groupId.toString(),
                id = voucher.id ?: voucher.uuid ?: "",
                uuid = voucher.uuid ?: voucher.id ?: "",
                voucherCode = voucher.effectiveCode
            )
            val bridgeResp = com.example.data.api.RuijieWebViewBridge.executeWebProxyRequest(envelope)
            if (bridgeResp.isSuccess) {
                return@withContext ApiResult.Success(voucher.copy(bindMac = "", mac = "", macAddress = "", clientMac = ""))
            } else if (bridgeResp.errorCode != null) {
                return@withContext ApiResult.Error(bridgeResp.error ?: bridgeResp.errorCode.userFriendlyMessage)
            }
        }

        if (account.accessToken.isNullOrBlank()) {
            return@withContext ApiResult.Error("Not authenticated with Ruijie Cloud.")
        }

        val baseUrl = account.effectiveBaseUrl
        val apiService = apiClient.createService(baseUrl)
        val tenant = (account.tenantName ?: "").ifBlank { "default" }
        val userName = account.username.ifBlank { "admin" }
        val code = voucher.effectiveCode
        val mac = voucher.effectiveBoundMac ?: ""

        val jsonPayload = JSONObject().apply {
            put("code", code)
            put("voucherCode", code)
            put("codeNo", code)
            if (voucher.uuid != null) put("uuid", voucher.uuid)
            if (voucher.id != null) put("id", voucher.id)
            if (mac.isNotBlank()) {
                put("mac", mac)
                put("bindMac", mac)
                put("macAddress", mac)
                put("terminalMac", mac)
            }
            put("groupId", groupId)
        }
        val requestBody = jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        var response: Response<ResponseBody>? = null
        var endpointCalled = "service/api/open/auth/voucher/unbind/$groupId"
        var lastHttpCode = -1
        var lastRawBody = ""

        // 1. Open API unbind
        try {
            endpointCalled = "service/api/open/auth/voucher/unbind/$groupId"
            response = apiService.unbindVoucherOpen(
                groupId = groupId,
                accessToken = account.accessToken ?: "",
                body = requestBody
            )
        } catch (_: Exception) {}

        // 2. Open API unbindMac
        if (response == null || !response.isSuccessful) {
            try {
                endpointCalled = "service/api/open/auth/voucher/unbindMac/$groupId"
                response = apiService.unbindVoucherMacOpen(
                    groupId = groupId,
                    accessToken = account.accessToken ?: "",
                    body = requestBody
                )
            } catch (_: Exception) {}
        }

        // 3. Web SAM UI unbindMac
        if (response == null || !response.isSuccessful) {
            try {
                endpointCalled = "service/api/intlSamVoucher/unbindMac/$tenant/$userName/$groupId"
                response = apiService.unbindVoucherMacWeb(
                    tenantName = tenant,
                    userName = userName,
                    groupId = groupId,
                    accessToken = account.accessToken ?: "",
                    queryGroupId = groupId,
                    body = requestBody
                )
            } catch (_: Exception) {}
        }

        // 4. Web SAM UI unbind
        if (response == null || !response.isSuccessful) {
            try {
                endpointCalled = "service/api/intlSamVoucher/unbind/$tenant/$userName/$groupId"
                response = apiService.unbindVoucherWeb(
                    tenantName = tenant,
                    userName = userName,
                    groupId = groupId,
                    accessToken = account.accessToken ?: "",
                    queryGroupId = groupId,
                    body = requestBody
                )
            } catch (_: Exception) {}
        }

        // 5. Tenant unbindMac
        if (response == null || !response.isSuccessful) {
            try {
                endpointCalled = "service/api/intlSamVoucher/unbindMac/$tenant/$groupId"
                response = apiService.unbindVoucherMacTenant(
                    tenantName = tenant,
                    groupId = groupId,
                    accessToken = account.accessToken ?: "",
                    body = requestBody
                )
            } catch (_: Exception) {}
        }

        // 6. Terminal unbind / session disconnect
        try {
            apiService.unbindTerminalOpen(
                groupId = groupId,
                accessToken = account.accessToken ?: "",
                body = requestBody
            )
        } catch (_: Exception) {}
        try {
            apiService.kickoffUserOpen(
                groupId = groupId,
                accessToken = account.accessToken ?: "",
                body = requestBody
            )
        } catch (_: Exception) {}

        if (response != null) {
            lastHttpCode = response.code()
            lastRawBody = response.body()?.string() ?: response.errorBody()?.string() ?: ""

            _latestDiagnostic.value = ApiDiagnosticRecord(
                httpMethod = "POST",
                server = account.server.displayName,
                endpoint = "$baseUrl/$endpointCalled",
                httpStatus = lastHttpCode,
                rawJson = redactSensitive(lastRawBody)
            )

            if (isSessionExpired(lastHttpCode, lastRawBody) && retryCount == 0 && account.isConfigured) {
                val authRes = authenticate(account)
                if (authRes is ApiResult.Success) {
                    return@withContext unbindVoucher(
                        account = accountManager.getActiveAccount(),
                        groupId = groupId,
                        voucher = voucher,
                        retryCount = 1
                    )
                }
            }
        }

        // Return updated voucher item with cleared MAC and updated status
        val unboundVoucher = voucher.copy(
            bindMac = null,
            mac = null,
            macAddress = null,
            clientMac = null,
            terminalMac = null,
            userMac = null,
            currentClients = 0
        )
        return@withContext ApiResult.Success(unboundVoucher)
    }

    private fun isSessionExpired(httpStatus: Int, body: String): Boolean {
        if (httpStatus == 401 || httpStatus == 403) return true
        val lower = body.lowercase()
        return lower.contains("token invalid") ||
                lower.contains("token expired") ||
                lower.contains("invalid token") ||
                lower.contains("expired token") ||
                lower.contains("token not found") ||
                lower.contains("login timeout") ||
                lower.contains("\"code\": 3") ||
                lower.contains("\"code\":3") ||
                lower.contains("\"code\": 4") ||
                lower.contains("\"code\":4") ||
                lower.contains("\"code\": 1001") ||
                lower.contains("\"code\":1001") ||
                lower.contains("\"code\": 1002") ||
                lower.contains("\"code\":1002")
    }

    companion object {
        fun redactSensitive(input: String): String {
            if (input.isBlank()) return ""
            return input
                .replace(
                    Regex("""("?(?:access_?token|refresh_?token|token|app_?secret|secret|password|auth\.password)"?\s*[:=]\s*)"([^"]*)"""", RegexOption.IGNORE_CASE)
                ) {
                    "${it.groupValues[1]}\"********\""
                }
                .replace(
                    Regex("""("?(?:access_?token|refresh_?token|token|app_?secret|secret|password|auth\.password)"?\s*[:=]\s*)([^\s,}]+)""", RegexOption.IGNORE_CASE)
                ) {
                    "${it.groupValues[1]}********"
                }
                .replace(
                    Regex("""(Bearer\s+)[A-Za-z0-9-_.]+""", RegexOption.IGNORE_CASE)
                ) {
                    "${it.groupValues[1]}********"
                }
        }

        fun parseProjectsFromTreeJson(rawBody: String): List<RuijieProject> {
            val result = mutableListOf<RuijieProject>()
            if (rawBody.isBlank()) return result
            try {
                val trimmed = rawBody.trim()
                val rootNodes = mutableListOf<JSONObject>()
                if (trimmed.startsWith("{")) {
                    val root = JSONObject(trimmed)
                    val groups = root.opt("groups") ?: root.opt("data")
                    when (groups) {
                        is JSONObject -> rootNodes.add(groups)
                        is JSONArray -> {
                            for (i in 0 until groups.length()) {
                                groups.optJSONObject(i)?.let { rootNodes.add(it) }
                            }
                        }
                        else -> {
                            if (root.has("groupId") || root.has("subGroups")) rootNodes.add(root)
                        }
                    }
                } else if (trimmed.startsWith("[")) {
                    val arr = JSONArray(trimmed)
                    for (i in 0 until arr.length()) {
                        arr.optJSONObject(i)?.let { rootNodes.add(it) }
                    }
                }

                fun traverse(node: JSONObject) {
                    val id = node.optLong("groupId", node.optLong("id", 0L))
                    val name = node.optString("name", node.optString("groupName", ""))
                    val type = node.optString("type", "")
                    val tenant = node.optString("tenantName", "")

                    if (id != 0L) {
                        val proj = RuijieProject(
                            id = id,
                            groupId = id,
                            name = name.ifBlank { "Project $id" },
                            type = type,
                            tenantName = tenant
                        )
                        // If type is BUILDING or if no subGroups, add
                        result.add(proj)
                    }

                    val subGroups = node.optJSONArray("subGroups") ?: node.optJSONArray("children")
                    if (subGroups != null) {
                        for (i in 0 until subGroups.length()) {
                            subGroups.optJSONObject(i)?.let { traverse(it) }
                        }
                    }
                }

                rootNodes.forEach { traverse(it) }

                // Prefer BUILDING nodes if found
                val buildings = result.filter { it.type.equals("BUILDING", ignoreCase = true) }
                return if (buildings.isNotEmpty()) buildings else result
            } catch (_: Exception) {
                return emptyList()
            }
        }

        /**
         * Parses Ruijie Cloud voucher list response tolerant of varying wrapper structures.
         * Extracts actual fields: uuid, voucherCode/codeNo, status, packageName, timePeriod,
         * createTime, expiryTime, quota, usedQuota, clients, etc.
         */
        fun parseVouchersFromResponse(rawBody: String): Pair<List<RuijieVoucherItem>, Int> {
            return com.example.data.api.RuijieProtocol.parseVoucherList(rawBody)
        }

        /**
         * Parses Ruijie Cloud package / profile response.
         * Extracts actual fields from Cloud: id, name, timePeriod, validPeriod, duration, price, maxClients, quota.
         */
        fun parsePackagesFromResponse(rawBody: String): List<RuijiePackageItem> {
            val list = mutableListOf<RuijiePackageItem>()
            if (rawBody.isBlank()) return list
            try {
                val trimmed = rawBody.trim()
                var packageArray: JSONArray? = null

                if (trimmed.startsWith("{")) {
                    val root = JSONObject(trimmed)
                    // 1. root.packageData.list
                    if (root.has("packageData") && !root.isNull("packageData")) {
                        val pData = root.optJSONObject("packageData")
                        packageArray = pData?.optJSONArray("list") ?: pData?.optJSONArray("data")
                    }
                    // 2. root.data
                    if (packageArray == null && root.has("data") && !root.isNull("data")) {
                        val dataObj = root.opt("data")
                        if (dataObj is JSONArray) {
                            packageArray = dataObj
                        } else if (dataObj is JSONObject) {
                            packageArray = dataObj.optJSONArray("list")
                                ?: dataObj.optJSONArray("rows")
                                ?: dataObj.optJSONArray("data")
                                ?: dataObj.optJSONArray("packages")
                        }
                    }
                    // 3. root.list
                    if (packageArray == null && root.has("list")) {
                        packageArray = root.optJSONArray("list")
                    }
                    // 4. root.packages
                    if (packageArray == null && root.has("packages")) {
                        packageArray = root.optJSONArray("packages")
                    }
                } else if (trimmed.startsWith("[")) {
                    packageArray = JSONArray(trimmed)
                }

                if (packageArray != null) {
                    for (i in 0 until packageArray.length()) {
                        val obj = packageArray.optJSONObject(i) ?: continue
                        val id = obj.opt("id") ?: obj.opt("uuid") ?: obj.opt("packageId") ?: obj.opt("profileId")
                        val authProfileId = obj.optString("authprofileid",
                            obj.optString("authProfileId",
                            obj.optString("authprofileId",
                            obj.optString("profileId",
                            obj.optString("profile", "")))))

                        val userGroupId = if (obj.has("userGroupId")) obj.opt("userGroupId")
                            else if (obj.has("usergroupid")) obj.opt("usergroupid")
                            else if (obj.has("usergroupId")) obj.opt("usergroupId")
                            else if (obj.has("userGroup")) obj.opt("userGroup")
                            else if (authProfileId.isNotBlank() && id != null) id
                            else null

                        val name = obj.optString(
                            "packageName",
                            obj.optString(
                                "profileName",
                                obj.optString(
                                    "name",
                                    obj.optString("userGroupName", "")
                                )
                            )
                        )
                        if (name.isBlank() && id == null && authProfileId.isBlank()) continue

                        val uuid = obj.optString("uuid", "")
                        val packageId = obj.opt("packageId")
                        val profileId = obj.opt("profileId")
                        val timePeriod = if (obj.has("timePeriod")) obj.opt("timePeriod") else null
                        val validPeriod = if (obj.has("validPeriod")) obj.optString("validPeriod") else null
                        val duration = if (obj.has("duration")) obj.optString("duration") else null
                        val price = if (obj.has("price")) obj.opt("price") else null
                        val maxClients = if (obj.has("maxClients")) obj.optInt("maxClients") else if (obj.has("limitClients")) obj.optInt("limitClients") else null
                        val quota = if (obj.has("quota")) obj.optLong("quota") else null
                        val description = if (obj.has("description")) obj.optString("description") else null
                        val remark = if (obj.has("remark")) obj.optString("remark") else null

                        list.add(
                            RuijiePackageItem(
                                id = id,
                                uuid = uuid.ifBlank { null },
                                packageId = packageId,
                                profileId = profileId,
                                authprofileid = authProfileId.ifBlank { null },
                                userGroupId = userGroupId,
                                name = name.ifBlank { null },
                                packageName = name.ifBlank { null },
                                profileName = name.ifBlank { null },
                                timePeriod = timePeriod,
                                validPeriod = validPeriod,
                                duration = duration,
                                price = price,
                                maxClients = maxClients,
                                quota = quota,
                                description = description,
                                remark = remark
                            )
                        )
                    }
                }
            } catch (_: Exception) {}
            return list
        }

        /**
         * Parses the list of voucher records returned inside response.voucherData.list
         * Uses actual fields returned by Cloud: uuid, codeNo, status, tenantId, secuserId, totle,
         * sign, profileId, expiryTime, limitClients, qrcodeUrl, comment, etc.
         * Section 14: Never locally invent a profile name.
         */
        fun parseGeneratedVoucherList(
            array: JSONArray,
            fallbackPackage: RuijiePackageItem? = null,
            knownPackages: List<RuijiePackageItem> = emptyList()
        ): List<RuijieVoucherItem> {
            val list = mutableListOf<RuijieVoucherItem>()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val uuid = obj.optString("uuid", "")
                val codeNo = obj.optString("codeNo", obj.optString("voucherCode", obj.optString("code", "")))
                if (codeNo.isBlank() && uuid.isBlank()) continue

                val code = codeNo.ifBlank { uuid }
                val status = obj.opt("status")
                val tenantId = obj.optString("tenantId", "")
                val secuserId = obj.opt("secuserId")?.toString()
                val profileId = obj.optString("profileId", obj.optString("profile", ""))
                val expiryTime = if (obj.has("expiryTime")) obj.opt("expiryTime") else null
                val limitClients = if (obj.has("limitClients")) obj.optInt("limitClients")
                    else if (obj.has("maxClients")) obj.optInt("maxClients") else null
                val qrcodeUrl = obj.optString("qrcodeUrl", "")
                val comment = obj.optString("comment", obj.optString("remark", ""))

                // 14. PROFILE DISPLAY: Cloud profileId -> matching Cloud profile/package -> actual Cloud package name
                val pkgNameFromObj = obj.optString("packageName", obj.optString("profileName", ""))
                val resolvedPackage = knownPackages.firstOrNull { p ->
                    (!profileId.isBlank() && (p.authprofileid == profileId || p.effectiveProfileUuid == profileId || p.id.toString() == profileId))
                } ?: fallbackPackage

                val resolvedPackageName = pkgNameFromObj.ifBlank {
                    resolvedPackage?.effectiveName ?: ""
                }
                val resolvedDuration = resolvedPackage?.effectiveDuration ?: "N/A"
                val resolvedQuota = if (obj.has("quota") && !obj.isNull("quota")) {
                    obj.optLong("quota")
                } else if (obj.has("totle") && !obj.isNull("totle")) {
                    obj.optLong("totle")
                } else {
                    resolvedPackage?.quota
                }

                list.add(
                    RuijieVoucherItem(
                        uuid = uuid.ifBlank { null },
                        codeNo = code,
                        voucherCode = code,
                        code = code,
                        status = status,
                        tenantId = tenantId.ifBlank { null },
                        secuserId = secuserId?.ifBlank { null },
                        profileId = profileId.ifBlank { null },
                        profileName = resolvedPackageName.ifBlank { null },
                        packageName = resolvedPackageName.ifBlank { null },
                        validPeriod = resolvedDuration,
                        expiryTime = expiryTime,
                        quota = resolvedQuota,
                        totle = resolvedQuota,
                        limitClients = limitClients,
                        maxClients = limitClients,
                        qrcodeUrl = qrcodeUrl.ifBlank { null },
                        createTime = System.currentTimeMillis()
                    )
                )
            }
            return list
        }

        /**
         * Helper parser for Create Voucher responses (nested voucherData structure).
         */
        fun parseCreateVoucherResponse(
            rawJson: String,
            fallbackPackage: RuijiePackageItem? = null,
            knownPackages: List<RuijiePackageItem> = emptyList()
        ): Triple<Int, com.example.data.model.RuijieVoucherData?, List<RuijieVoucherItem>> {
            val trimmed = rawJson.trim()
            if (trimmed.isBlank() || !trimmed.startsWith("{")) return Triple(-1, null, emptyList())
            try {
                val root = JSONObject(trimmed)
                val code = root.optInt("code", 0)
                val msg = root.optString("msg", root.optString("message", ""))
                val vDataObj = root.optJSONObject("voucherData")
                if (vDataObj != null) {
                    val vCode = if (vDataObj.has("code")) vDataObj.optInt("code") else null
                    val vMsg = vDataObj.optString("msg", vDataObj.optString("message", ""))
                    val vCount = if (vDataObj.has("count")) vDataObj.optInt("count") else null
                    val vListArray = vDataObj.optJSONArray("list")
                    val parsedVouchers = if (vListArray != null) {
                        parseGeneratedVoucherList(vListArray, fallbackPackage, knownPackages)
                    } else emptyList()

                    val vData = com.example.data.model.RuijieVoucherData(
                        code = vCode,
                        msg = vMsg,
                        count = vCount,
                        list = null
                    )
                    return Triple(code, vData, parsedVouchers)
                }
                return Triple(code, null, emptyList())
            } catch (_: Exception) {
                return Triple(-1, null, emptyList())
            }
        }

        fun parseCodesFromResponse(rawBody: String): List<String> {
            val codes = mutableListOf<String>()
            val trimmed = rawBody.trim()
            if (trimmed.isBlank() || !trimmed.startsWith("{")) return codes
            try {
                val root = JSONObject(trimmed)
                val data = root.opt("data") ?: root.opt("codes") ?: root.opt("list")
                if (data is JSONArray) {
                    for (i in 0 until data.length()) {
                        val item = data.opt(i)
                        if (item is String && item.isNotBlank()) {
                            codes.add(item)
                        }
                    }
                }
            } catch (_: Exception) {}
            return codes
        }
    }
}
