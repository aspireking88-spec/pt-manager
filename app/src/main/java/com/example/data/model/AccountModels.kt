package com.example.data.model

import com.squareup.moshi.JsonClass

enum class AuthMode(val displayName: String) {
    OPEN_API("Ruijie Cloud Open API (App ID / Secret)"),
    USER_ACCOUNT("Ruijie Cloud Account (Email / Password)")
}

enum class RuijieServer(val displayName: String, val baseUrl: String) {
    GLOBAL("Global Cloud (cloud-as.ruijienetworks.com)", "https://cloud-as.ruijienetworks.com"),
    ASIA("Asia Cloud (cloud-as.ruijienetworks.com)", "https://cloud-as.ruijienetworks.com"),
    EU("Europe Cloud (cloud-eu.ruijienetworks.com)", "https://cloud-eu.ruijienetworks.com"),
    RUIJIE_CLOUD_US("Americas Cloud (cloud-us.ruijienetworks.com)", "https://cloud-us.ruijienetworks.com"),
    CUSTOM("Custom Server URL", "")
}

val defaultBaseUrl: String = "https://cloud-as.ruijienetworks.com"

@JsonClass(generateAdapter = true)
data class RuijieAccount(
    val id: String = java.util.UUID.randomUUID().toString(),
    val accountName: String = "Default Account",
    val appId: String = "",
    val secret: String = "",
    val username: String = "",
    val email: String? = null,
    val tenantName: String? = null,
    val server: RuijieServer = RuijieServer.GLOBAL,
    val customUrl: String = "",
    val customServerUrl: String = "",
    val authMode: AuthMode = AuthMode.OPEN_API,
    val token: String? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val tokenExpiresAt: Long = 0L,
    val userRole: String? = null,
    val nickname: String? = null,
    val defaultTenant: String? = null,
    val selectedTenantId: String? = null,
    val selectedTenantName: String? = null,
    val selectedGroupId: String? = null,
    val selectedGroupName: String? = null
) {
    val activeBaseUrl: String
        get() {
            val u = if (customServerUrl.isNotBlank()) customServerUrl else customUrl
            return if (server == RuijieServer.CUSTOM && u.isNotBlank()) u.trimEnd('/') else server.baseUrl
        }

    val selectedGroupIdStr: String
        get() = selectedGroupId?.ifBlank { null } ?: ""

    val tenantId: Long
        get() = selectedTenantId?.toLongOrNull() ?: 0L
}
