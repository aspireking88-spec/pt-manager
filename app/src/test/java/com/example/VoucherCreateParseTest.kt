package com.example

import com.example.data.api.RuijieProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifies voucher create/list parsing against the real Ruijie Cloud API
 * response shapes (captured 2026-09-27).
 *
 * Create response: {"code":0,"msg":"Success.",
 *   "voucherData":{"code":0,"msg":"OK.","count":1,
 *     "list":[{"uuid":"...","codeNo":"55xjmi","status":"1", ...}]}}
 * List response: {"code":0,"msg":"OK.",
 *   "voucherData":{"code":0,"msg":"OK.","count":1193,
 *     "list":[{"uuid":"...","voucherCode":"55255407","status":"1", ...}]}}
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VoucherCreateParseTest {

    private val createResponse = """
        {"code":0,"msg":"Success.","voucherData":{"code":0,"msg":"OK.","count":1,
        "list":[{"uuid":"83cfe95d3290426687a8e26ac991ae8d","codeNo":"55xjmi",
        "status":"1","tenantId":"gHdrJkhRUsiQGwgbtJccRZblXXjoMxuM","secuserId":"",
        "totle":0,"sign":"","profileId":"95739597329441832215270949202782",
        "expiryTime":"2125-09-27 12:06:19","limitClients":1,
        "groupId":"c7c556c0106547e59ba2a82a93b05ade","comment":"PT Manager test"}]}}
    """.trimIndent()

    private val listResponse = """
        {"code":0,"msg":"OK.","voucherData":{"code":0,"msg":"OK.","count":1193,
        "list":[{"uuid":"fd80b87e78354aa4967a00eedac0dda8",
        "tenantId":"gHdrJkhRUsiQGwgbtJccRZblXXjoMxuM","voucherCode":"55255407",
        "nameRef":"","timePeriod":1440,"usedTime":0,"createTime":1790473007000,
        "maxClients":1,"currentClients":0,"quota":1024,"status":"1"}]}}
    """.trimIndent()

    @Test
    fun testCreateResponseParsesCodeNo() {
        val parsed = RuijieProtocol.parseVoucherCreateResponseParsed(createResponse, 200)
        assertEquals(0, parsed.ruijieCode)
        assertTrue(parsed.vouchers.isNotEmpty())
        val v = parsed.vouchers[0]
        // PDF mandates codeNo for preview/printing
        assertEquals("55xjmi", v.codeNo)
        assertEquals("55xjmi", v.effectiveCode)
        assertEquals("55xjmi", parsed.voucherCode)
        assertEquals("1", v.status.toString())
        assertEquals("95739597329441832215270949202782", v.profileId)
    }

    @Test
    fun testListResponseParsesVoucherCode() {
        val (list, total) = RuijieProtocol.parseVoucherList(listResponse)
        assertEquals(1193, total)
        assertTrue(list.isNotEmpty())
        val v = list[0]
        assertEquals("55255407", v.voucherCode)
        // effectiveCode falls back to voucherCode when codeNo absent
        assertEquals("55255407", v.effectiveCode)
        assertEquals("1", v.status.toString())
    }

    @Test
    fun testEffectiveCodePrefersCodeNo() {
        val parsed = RuijieProtocol.parseVoucherCreateResponseParsed(createResponse, 200)
        val v = parsed.vouchers[0]
        // Even if both present, codeNo wins per API Reference Manual
        assertEquals("55xjmi", v.effectiveCode)
    }
}
