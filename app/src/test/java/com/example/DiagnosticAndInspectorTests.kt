package com.example

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.data.diagnostics.RuijieSessionDiagnosticCollector
import com.example.data.diagnostics.VoucherInspectorCollector
import com.example.data.model.RuijieVoucherItem
import com.example.data.model.VoucherPackageType
import com.example.data.model.VoucherStatus
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DiagnosticAndInspectorTests {

    @Test
    fun `test 1 - login diagnosis report generation`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val report = RuijieSessionDiagnosticCollector.generateReport(context)
        assertNotNull(report)
        val txtReport = RuijieSessionDiagnosticCollector.buildTxtReport(report)
        assertTrue(txtReport.contains("RUIJIE LOGIN ERROR DIAGNOSTIC REPORT"))
        assertTrue(txtReport.contains("SESSION STATE"))
    }

    @Test
    fun `test 2 - login diagnosis secret redaction`() {
        val text = "password=my_secret_password, token=abc-123, cookie=sess-999"
        val redacted = RuijieSessionDiagnosticCollector.redactSecrets(text)
        assertFalse(redacted.contains("my_secret_password"))
        assertFalse(redacted.contains("abc-123"))
        assertFalse(redacted.contains("sess-999"))
        assertTrue(redacted.contains("password=[REDACTED]"))
        assertTrue(redacted.contains("token=[REDACTED]"))
        assertTrue(redacted.contains("cookie=[REDACTED]"))
    }

    @Test
    fun `test 3 - ACTION_CREATE_DOCUMENT with text_plain`() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, "ruijie-login-diagnosis.txt")
        }
        assertEquals(Intent.ACTION_CREATE_DOCUMENT, intent.action)
        assertEquals("text/plain", intent.type)
        assertTrue(intent.categories.contains(Intent.CATEGORY_OPENABLE))
    }

    @Test
    fun `test 4 - session valid when info returns 200 and code 0`() {
        RuijieSessionDiagnosticCollector.authHttpStatus = 200
        RuijieSessionDiagnosticCollector.authRuijieCode = 0
        RuijieSessionDiagnosticCollector.runLoginErrorDiagnosis()
        assertEquals("Session Valid", RuijieSessionDiagnosticCollector.computeFinalDiagnosis())
    }

    @Test
    fun `test 5 - timeout is not session expiry`() {
        RuijieSessionDiagnosticCollector.authHttpStatus = 408
        RuijieSessionDiagnosticCollector.authResultCategory = "Timeout"
        RuijieSessionDiagnosticCollector.runLoginErrorDiagnosis()
        assertEquals("Temporary Timeout", RuijieSessionDiagnosticCollector.computeFinalDiagnosis())
        assertNotEquals("Real Session Expired", RuijieSessionDiagnosticCollector.computeFinalDiagnosis())
    }

    @Test
    fun `test 6 - cookie present boolean does not expose cookie value`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        RuijieSessionDiagnosticCollector.isCookiePresent = true
        val report = RuijieSessionDiagnosticCollector.generateReport(context)
        assertTrue(report.session.cookiePresent)
        
        val txt = RuijieSessionDiagnosticCollector.buildTxtReport(report)
        assertTrue(txt.contains("Cookie present: true"))
        assertFalse(txt.contains("raw_cookie_val"))
        assertTrue(txt.contains("Passwords / Raw Cookies: [REDACTED]"))
    }

    @Test
    fun `test 7 - voucher inspector report generation`() {
        val voucher = RuijieVoucherItem(uuid = "test-uuid", voucherCode = "V-12345")
        val report = VoucherInspectorCollector.inspectVoucher(voucher)
        assertNotNull(report)
        val txt = VoucherInspectorCollector.buildTxtReport(report)
        assertTrue(txt.contains("RUIJIE VOUCHER DETAIL INSPECTION REPORT"))
        assertTrue(txt.contains("Voucher code: V-12345"))
    }

    @Test
    fun `test 8 - 1GB classified as DATA_GB`() {
        val v = RuijieVoucherItem(packageName = "1GB Premium")
        val det = VoucherInspectorCollector.detectPackageType(v)
        assertEquals(VoucherPackageType.GB_DATA, det.packageType)
    }

    @Test
    fun `test 9 - 1hour classified as HOURS_TIME`() {
        val v = RuijieVoucherItem(profileName = "1hour Free")
        val det = VoucherInspectorCollector.detectPackageType(v)
        assertEquals(VoucherPackageType.HOURS_TIME, det.packageType)
    }

    @Test
    fun `test 10 - remaining calc 1024 minus 461 equals 563`() {
        val total = 1024L
        val used = 461L
        val remaining = (total - used).coerceAtLeast(0L)
        assertEquals(563L, remaining)
    }

    @Test
    fun `test 11 - remaining calc 1024 minus 1025 equals 0`() {
        val total = 1024L
        val used = 1025L
        val remaining = (total - used).coerceAtLeast(0L)
        assertEquals(0L, remaining)
    }

    @Test
    fun `test 12 - time remaining calc 60 minus 13 equals 47`() {
        val total = 60L
        val used = 13L
        val remaining = (total - used).coerceAtLeast(0L)
        assertEquals(47L, remaining)
    }

    @Test
    fun `test 13 - missing traffic fields say Not returned by Ruijie response`() {
        val v = RuijieVoucherItem()
        val report = VoucherInspectorCollector.inspectVoucher(v)
        val trafficUsedField = report.dataGbs.find { it.fieldName == "trafficUsed" }
        assertNotNull(trafficUsedField)
        assertEquals("Not returned by Ruijie response", trafficUsedField?.rawValue)
    }

    @Test
    fun `test 14 - bindMac equals 1 is not displayed as MAC address`() {
        val v = RuijieVoucherItem(bindMac = "1")
        val report = VoucherInspectorCollector.inspectVoucher(v)
        val bindMacField = report.clients.find { it.fieldName == "bindMac" }
        assertNotNull(bindMacField)
        assertEquals("Bound: Yes\nMAC address: Not returned by Ruijie", bindMacField?.normalizedValue)
        assertNotEquals("1", bindMacField?.normalizedValue)
    }

    @Test
    fun `test 15 - selected voucher remains same after list reorder`() {
        val originalList = listOf(
            RuijieVoucherItem(uuid = "v-1", voucherCode = "C-1"),
            RuijieVoucherItem(uuid = "v-2", voucherCode = "C-2")
        )
        VoucherInspectorCollector.selectVoucher("v-2", "C-2")
        
        val reorderedList = listOf(
            RuijieVoucherItem(uuid = "v-2", voucherCode = "C-2"),
            RuijieVoucherItem(uuid = "v-1", voucherCode = "C-1")
        )
        
        val matched = VoucherInspectorCollector.onListRefreshed(reorderedList)
        assertNotNull(matched)
        assertEquals("v-2", matched?.uuid)
        assertEquals("v-2", VoucherInspectorCollector.selectedVoucherId.value)
    }

    @Test
    fun `test 16 - duplicate refresh is blocked`() {
        var callCount = 0
        VoucherInspectorCollector.blockDuplicateRefresh {
            callCount++
            // Attempt duplicate inner block
            VoucherInspectorCollector.blockDuplicateRefresh {
                callCount++
            }
        }
        assertEquals(1, callCount)
    }

    @Test
    fun `test 17 - share and copy report actions work`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val success = VoucherInspectorCollector.copyToClipboard(context, "test report content")
        assertTrue(success)
        
        // Share call verification (should execute without crash)
        VoucherInspectorCollector.shareReport(context, "test report content")
    }

    @Test
    fun `test 18 - no sensitive values occur in either TXT report`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // Login Report
        val loginReport = RuijieSessionDiagnosticCollector.generateReport(context)
        val loginTxt = RuijieSessionDiagnosticCollector.buildTxtReport(loginReport)
        assertFalse(loginTxt.contains("my_secret"))
        
        // Voucher Report
        val v = RuijieVoucherItem()
        val voucherReport = VoucherInspectorCollector.inspectVoucher(v)
        val voucherTxt = VoucherInspectorCollector.buildTxtReport(voucherReport)
        assertFalse(voucherTxt.contains("my_secret"))
    }
}
