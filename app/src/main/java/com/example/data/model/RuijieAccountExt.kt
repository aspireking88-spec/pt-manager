package com.example.data.model

/**
 * PT Manager: helpers restored for the documented Ruijie Cloud API flow.
 */

/** Resolve the effective base URL for a server selection (supports custom URL override). */
fun RuijieServer.resolveBaseUrl(customUrl: String): String {
    if (this == RuijieServer.CUSTOM && customUrl.isNotBlank()) {
        return customUrl.trim().trimEnd('/')
    }
    return baseUrl
}

/** Whether this account has enough credentials configured to attempt authentication. */
val RuijieAccount.isConfigured: Boolean
    get() = when (authMode) {
        AuthMode.OPEN_API -> appId.isNotBlank() && secret.isNotBlank()
        AuthMode.USER_ACCOUNT -> username.isNotBlank()
    }

/** Alias for activeBaseUrl used by repository code. */
val RuijieAccount.effectiveBaseUrl: String
    get() = activeBaseUrl

/** Masked token for safe logging (never expose full token). */
val RuijieAccount.maskedToken: String
    get() {
        val t = accessToken?.ifBlank { null } ?: token?.ifBlank { null } ?: return "none"
        return if (t.length <= 8) "****" else t.take(4) + "****" + t.takeLast(4)
    }
