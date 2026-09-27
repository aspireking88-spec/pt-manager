package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class DiagnosisVerdict(val displayName: String) {
    PASS("PASS"),
    FAIL("FAIL"),
    UNSUPPORTED("UNSUPPORTED")
}

data class VoucherGenerationDiagnosticRecord(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val selectedCodeType: VoucherCodeType = VoucherCodeType.ALPHANUMERIC,
    val uiValue: String = "Alphanumeric",
    val mappedApiValue: String = "createCodeType=1, codeSize=6",
    val endpoint: String = "",
    val httpMethod: String = "POST",
    val requestQuery: String = "",
    val sanitizedRequestBody: String = "",
    val responseHttpStatus: Int = 0,
    val sanitizedResponseBody: String = "",
    val returnedVoucherField: String = "voucherData.list[0].codeNo",
    val returnedVoucherCode: String = "",
    val codeTypeResult: DiagnosisVerdict = DiagnosisVerdict.PASS,
    val reason: String = ""
) {
    val formattedTime: String
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(timestamp))

    fun formatForChatGPT(): String {
        return buildString {
            appendLine("PRINTER V1 — VOUCHER GENERATION DIAGNOSIS")
            appendLine("Timestamp: $formattedTime")
            appendLine("----------------------------------------")
            appendLine("Selected Type: ${selectedCodeType.label}")
            appendLine()
            appendLine("UI Value:")
            appendLine(uiValue)
            appendLine()
            appendLine("Mapped API Value:")
            appendLine(mappedApiValue)
            appendLine()
            appendLine("Endpoint:")
            appendLine(endpoint)
            appendLine()
            appendLine("Method:")
            appendLine(httpMethod)
            appendLine()
            appendLine("Request Query:")
            appendLine(requestQuery.ifBlank { "(none or access_token redacted)" })
            appendLine()
            appendLine("Request Body:")
            appendLine(sanitizedRequestBody.ifBlank { "{}" })
            appendLine()
            appendLine("HTTP Status:")
            appendLine("$responseHttpStatus")
            appendLine()
            appendLine("Response Body:")
            appendLine(sanitizedResponseBody.ifBlank { "{}" })
            appendLine()
            appendLine("Voucher Field:")
            appendLine(returnedVoucherField)
            appendLine()
            appendLine("Returned Code:")
            appendLine(returnedVoucherCode.ifBlank { "(none returned)" })
            appendLine()
            appendLine("Result:")
            appendLine(codeTypeResult.displayName)
            appendLine()
            appendLine("Diagnosis:")
            appendLine(reason)
            appendLine("----------------------------------------")
            appendLine("Note: All credentials, access tokens, and secrets are masked (********).")
        }
    }

    fun formatSanitizedRequest(): String {
        return buildString {
            appendLine("ENDPOINT: $endpoint")
            appendLine("METHOD: $httpMethod")
            appendLine("QUERY: $requestQuery")
            appendLine("BODY:")
            appendLine(sanitizedRequestBody)
        }
    }

    fun formatSanitizedResponse(): String {
        return buildString {
            appendLine("HTTP STATUS: $responseHttpStatus")
            appendLine("BODY:")
            appendLine(sanitizedResponseBody)
        }
    }
}
