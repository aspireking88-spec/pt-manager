package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RuijieSessionInfo(
    val cookieString: String = "",
    val csrfToken: String? = null,
    val username: String? = null,
    val lastVerified: Long = 0L,
    val isValid: Boolean = false
)
