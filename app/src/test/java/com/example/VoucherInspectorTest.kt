package com.example

import com.example.data.diagnostics.VoucherInspectorCollector
import com.example.data.model.RuijieAccount
import com.example.data.model.RuijieProject
import com.example.data.model.RuijieVoucherItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VoucherInspectorTest {

    @Test
    fun testRawQuota1024AndUsedQuota975() {
        val voucher = RuijieVoucherItem(quota = 1024.0, usedQuota = 975.0)
        val report = VoucherInspectorCollector.inspectVoucher(voucher)
        val txt = VoucherInspectorCollector.buildTxtReport(report)

        assertTrue("Report should contain 'Normalized total: 1024'", txt.contains("Normalized total: 1024"))
        assertTrue("Report should contain 'Normalized used: 975'", txt.contains("Normalized used: 975"))
        assertTrue("Report should contain 'Normalized remaining: 49'", txt.contains("Normalized remaining: 49"))
        assertTrue("Report should contain 'Unit: MB'", txt.contains("Unit: MB"))
    }

    @Test
    fun testRawQuota1024AndUsedQuota1025() {
        val voucher = RuijieVoucherItem(quota = 1024.0, usedQuota = 1025.0)
        val report = VoucherInspectorCollector.inspectVoucher(voucher)
        val txt = VoucherInspectorCollector.buildTxtReport(report)

        assertTrue("Report should contain 'Normalized total: 1024'", txt.contains("Normalized total: 1024"))
        assertTrue("Report should contain 'Normalized used: 1025'", txt.contains("Normalized used: 1025"))
        assertTrue("Report should contain 'Normalized remaining: 0'", txt.contains("Normalized remaining: 0"))
        assertTrue("Report should contain 'Unit: MB'", txt.contains("Unit: MB"))
    }

    @Test
    fun testRawQuota1024AndUsedQuota824() {
        val voucher = RuijieVoucherItem(quota = 1024.0, usedQuota = 824.0)
        val report = VoucherInspectorCollector.inspectVoucher(voucher)
        val txt = VoucherInspectorCollector.buildTxtReport(report)

        assertTrue("Report should contain 'Normalized remaining: 200'", txt.contains("Normalized remaining: 200"))
    }

    @Test
    fun testBindMacEquals1WithNoRealMac() {
        val voucher = RuijieVoucherItem(bindMac = "1")
        val report = VoucherInspectorCollector.inspectVoucher(voucher)
        val txt = VoucherInspectorCollector.buildTxtReport(report)

        assertTrue("Report should contain 'Bind flag: Yes'", txt.contains("Bind flag: Yes"))
        assertTrue("Report should contain 'MAC address: Not returned by Ruijie response'", txt.contains("MAC address: Not returned by Ruijie response"))
        assertFalse("Report should NOT state MAC address is 1", txt.contains("MAC address: 1"))
    }

    @Test
    fun testBindMacEquals0WithNoRealMac() {
        val voucher = RuijieVoucherItem(bindMac = "0")
        val report = VoucherInspectorCollector.inspectVoucher(voucher)
        val txt = VoucherInspectorCollector.buildTxtReport(report)

        assertTrue("Report should contain 'Bind flag: No'", txt.contains("Bind flag: No"))
        assertTrue("Report should contain 'MAC address: Not bound'", txt.contains("MAC address: Not bound"))
    }

    @Test
    fun testSelectedProjectGroupIdStrWhenVoucherProjectIdMissing() {
        val voucher = RuijieVoucherItem()
        val selectedProject = RuijieProject(groupIdStr = "6752877")
        val activeAccount = RuijieAccount()

        val effGid = voucher.getEffectiveProjectGroupId(selectedProject, activeAccount)
        assertEquals("6752877", effGid)

        val report = VoucherInspectorCollector.inspectVoucher(voucher, selectedProject, activeAccount)
        val txt = VoucherInspectorCollector.buildTxtReport(report)

        assertTrue("Report should contain 'Project GID: 6752877'", txt.contains("Project GID: 6752877") || txt.contains("Project Group ID: 6752877"))
    }

    @Test
    fun testSelectedProjectNameDisplayedWhenVoucherProjectNameMissing() {
        val voucher = RuijieVoucherItem(projectName = null)
        val selectedProject = RuijieProject(name = "Main Headquarters HQ")

        val effName = voucher.getEffectiveProjectName(selectedProject)
        assertEquals("Main Headquarters HQ", effName)

        val report = VoucherInspectorCollector.inspectVoucher(voucher, selectedProject)
        val txt = VoucherInspectorCollector.buildTxtReport(report)

        assertTrue("Report should contain project name", txt.contains("Main Headquarters HQ"))
    }
}
