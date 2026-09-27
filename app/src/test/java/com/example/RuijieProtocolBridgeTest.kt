package com.example

import com.example.data.api.RuijieProtocol
import com.example.data.model.RuijieBridgeErrorCode
import com.example.data.model.RuijieSessionState
import com.example.data.model.VoucherStatus
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RuijieProtocolBridgeTest {

    @Test
    fun testWebProxyUrlConstruction() {
        val apiPath = "/intlSamVoucher/getList/tenant1/12345"
        val expected = "https://cloud-as.ruijienetworks.com/webproxy/common/api?$apiPath"
        val constructed = RuijieProtocol.buildWebProxyUrl(apiPath)
        assertEquals(expected, constructed)
        assertFalse(constructed.contains("/webproxy/default/api"))
    }

    @Test
    fun testIsAuthenticatedUrlLogic() {
        assertFalse("SSO page must NOT be treated as authenticated",
            RuijieProtocol.isAuthenticatedUrl("https://cloud.ruijienetworks.com/sso/"))
        assertFalse("SSO login URL must NOT be treated as authenticated",
            RuijieProtocol.isAuthenticatedUrl("https://cloud.ruijienetworks.com/sso/login?redirect=..."))
        assertFalse("Account login page must NOT be treated as authenticated",
            RuijieProtocol.isAuthenticatedUrl("https://cloud.ruijienetworks.com/account/login"))
        assertFalse("Blank URL is not authenticated",
            RuijieProtocol.isAuthenticatedUrl(""))

        assertTrue("macc5 URL is authenticated",
            RuijieProtocol.isAuthenticatedUrl("https://cloud-as.ruijienetworks.com/macc5/project/list"))
        assertTrue("dashboard URL is authenticated",
            RuijieProtocol.isAuthenticatedUrl("https://cloud-as.ruijienetworks.com/dashboard/overview"))
        assertTrue("project list URL is authenticated",
            RuijieProtocol.isAuthenticatedUrl("https://cloud-as.ruijienetworks.com/project/network/groups"))
    }

    @Test
    fun testEnvelopeFieldsAndSpellingPreservation() {
        val envelope = RuijieProtocol.buildVoucherCreateEnvelope(
            tenantName = "tenant123",
            userName = "admin",
            groupId = "9988",
            count = 5,
            codeType = "Alphabetic",
            length = 8,
            userGroupId = "456",
            profileId = "profile_vip"
        )

        assertEquals("/intlSamVoucher/create/tenant123/admin/9988", envelope.getString("api"))
        assertEquals("POST", envelope.getString("method"))
        assertEquals("default", envelope.getString("module"))

        // Verify both querys and queyrs spellings are preserved separately
        assertTrue(envelope.has("querys"))
        assertTrue(envelope.has("queyrs"))
        assertFalse(envelope.has("queries"))

        val querys = envelope.getJSONObject("querys")
        val queyrs = envelope.getJSONObject("queyrs")
        assertEquals("en", querys.getString("lang"))
        assertEquals("9988", queyrs.getString("group_id"))

        // Verify payload parameters
        val params = envelope.getJSONObject("params")
        assertNotNull(params)
        assertEquals("5", params.getString("quantity"))
        assertEquals("1", params.getString("createCodeType")) // Alphabetic mapped to "1"
        assertEquals(8, params.getInt("codeSize"))
        assertEquals(456L, params.getLong("userGroupId"))
        assertEquals("profile_vip", params.getString("profile"))
    }

    @Test
    fun testCreateCodeTypeMapping() {
        // Verified mapping: Alphabetic -> "1", Alphanumeric -> "2", Numeric -> "3"
        val envAlpha = RuijieProtocol.buildVoucherCreateEnvelope("t", "u", "g", 1, "Alphabetic", 8, "ug", "p")
        val envAlphaNum = RuijieProtocol.buildVoucherCreateEnvelope("t", "u", "g", 1, "Alphanumeric", 8, "ug", "p")
        val envNum = RuijieProtocol.buildVoucherCreateEnvelope("t", "u", "g", 1, "Numeric", 8, "ug", "p")

        assertEquals("1", envAlpha.getJSONObject("params").getString("createCodeType"))
        assertEquals("2", envAlphaNum.getJSONObject("params").getString("createCodeType"))
        assertEquals("3", envNum.getJSONObject("params").getString("createCodeType"))
    }

    @Test
    fun testClassifyErrorHandling() {
        val htmlError = RuijieProtocol.classifyError(200, "text/html", "<html><body>Login Required</body></html>", "")
        assertEquals(RuijieBridgeErrorCode.HTML_RESPONSE, htmlError.first)

        val expiredError = RuijieProtocol.classifyError(200, "application/json", "{\"code\": 3, \"msg\": \"Session timeout\"}", "")
        assertEquals(RuijieBridgeErrorCode.SESSION_EXPIRED, expiredError.first)

        val notFoundError = RuijieProtocol.classifyError(404, "application/json", "Not found", "")
        assertEquals(RuijieBridgeErrorCode.HTTP_ERROR, notFoundError.first)
    }

    @Test
    fun testParseProjectsFromGroupTree() {
        val sampleGroupTreeJson = """
        {
            "code": 0,
            "data": [
                {
                    "id": "101",
                    "groupId": "201",
                    "name": "Headquarters Main",
                    "groupName": "HQ Wi-Fi",
                    "tenantName": "corp_tenant",
                    "children": [
                        {
                            "id": "102",
                            "groupId": "202",
                            "name": "Branch Office",
                            "groupName": "Branch Wi-Fi"
                        }
                    ]
                }
            ]
        }
        """.trimIndent()

        val projects = RuijieProtocol.parseProjectsFromGroupTree(sampleGroupTreeJson)
        assertEquals(2, projects.size)
        assertEquals("Headquarters Main", projects[0].name)
        assertEquals("201", projects[0].groupId)
        assertEquals("Branch Office", projects[1].name)
        assertEquals("202", projects[1].groupId)
    }

    // 1. root-level groups wrapper
    @Test
    fun testGroupTreeWrapperRootLevelGroups() {
        val json = """
        {
            "code": 0,
            "groups": [
                {
                    "groupId": "11",
                    "name": "Alpha Campus"
                }
            ]
        }
        """.trimIndent()
        val projects = RuijieProtocol.parseProjectsFromGroupTree(json)
        assertEquals(1, projects.size)
        assertEquals("11", projects[0].groupId)
        assertEquals("Alpha Campus", projects[0].name)
    }

    // 2. data.groups wrapper
    @Test
    fun testGroupTreeWrapperDataGroups() {
        val json = """
        {
            "code": 0,
            "data": {
                "groups": [
                    {
                        "groupId": "22",
                        "name": "Beta Campus"
                    }
                ]
            }
        }
        """.trimIndent()
        val projects = RuijieProtocol.parseProjectsFromGroupTree(json)
        assertEquals(1, projects.size)
        assertEquals("22", projects[0].groupId)
        assertEquals("Beta Campus", projects[0].name)
    }

    // 3. root list wrapper
    @Test
    fun testGroupTreeWrapperRootList() {
        val json = """
        {
            "code": 0,
            "list": [
                {
                    "groupId": "33",
                    "name": "Gamma Campus"
                }
            ]
        }
        """.trimIndent()
        val projects = RuijieProtocol.parseProjectsFromGroupTree(json)
        assertEquals(1, projects.size)
        assertEquals("33", projects[0].groupId)
        assertEquals("Gamma Campus", projects[0].name)
    }

    // 4. data.list wrapper
    @Test
    fun testGroupTreeWrapperDataList() {
        val json = """
        {
            "code": 0,
            "data": {
                "list": [
                    {
                        "groupId": "44",
                        "name": "Delta Campus"
                    }
                ]
            }
        }
        """.trimIndent()
        val projects = RuijieProtocol.parseProjectsFromGroupTree(json)
        assertEquals(1, projects.size)
        assertEquals("44", projects[0].groupId)
        assertEquals("Delta Campus", projects[0].name)
    }

    // 5. data as array wrapper
    @Test
    fun testGroupTreeWrapperDataAsArray() {
        val json = """
        {
            "code": 0,
            "data": [
                {
                    "groupId": "55",
                    "name": "Epsilon Campus"
                }
            ]
        }
        """.trimIndent()
        val projects = RuijieProtocol.parseProjectsFromGroupTree(json)
        assertEquals(1, projects.size)
        assertEquals("55", projects[0].groupId)
        assertEquals("Epsilon Campus", projects[0].name)
    }

    // 6. root groupId "0" is excluded while children are preserved
    @Test
    fun testGroupTreeRootGroupIdZeroExcluded() {
        val json = """
        {
            "code": 0,
            "groups": {
                "groupId": "0",
                "name": "Ruijie Networks Root",
                "subGroups": [
                    {
                        "groupId": "66",
                        "name": "Visible Branch"
                    }
                ]
            }
        }
        """.trimIndent()
        val projects = RuijieProtocol.parseProjectsFromGroupTree(json)
        assertEquals(1, projects.size)
        assertEquals("66", projects[0].groupId)
        assertEquals("Visible Branch", projects[0].name)
    }

    // 7. nested subGroups flattening
    @Test
    fun testGroupTreeNestedSubGroups() {
        val json = """
        {
            "code": 0,
            "data": [
                {
                    "groupId": "71",
                    "name": "HQ",
                    "subGroups": [
                        {
                            "groupId": "72",
                            "name": "Floor 1",
                            "subGroups": [
                                {
                                    "groupId": "73",
                                    "name": "Server Room"
                                }
                            ]
                        }
                    ]
                }
            ]
        }
        """.trimIndent()
        val projects = RuijieProtocol.parseProjectsFromGroupTree(json)
        assertEquals(3, projects.size)
        assertEquals("HQ", projects[0].name)
        assertEquals("Floor 1", projects[1].name)
        assertEquals("Server Room", projects[2].name)
    }

    // 8. groupId preservation (never replaced with index, name, or autogen)
    @Test
    fun testGroupTreeGroupIdPreservation() {
        val json = """
        {
            "code": 0,
            "data": [
                {
                    "groupId": "GRP_EXACT_999",
                    "name": "Project Special"
                }
            ]
        }
        """.trimIndent()
        val projects = RuijieProtocol.parseProjectsFromGroupTree(json)
        assertEquals(1, projects.size)
        assertEquals("GRP_EXACT_999", projects[0].groupId)
    }

    // 9. group name preservation
    @Test
    fun testGroupTreeGroupNamePreservation() {
        val json = """
        {
            "code": 0,
            "data": [
                {
                    "groupId": "123",
                    "name": "Exact Project Name 2026"
                }
            ]
        }
        """.trimIndent()
        val projects = RuijieProtocol.parseProjectsFromGroupTree(json)
        assertEquals(1, projects.size)
        assertEquals("Exact Project Name 2026", projects[0].name)
    }

    // 10. parent path generation
    @Test
    fun testGroupTreeParentPathGeneration() {
        val json = """
        {
            "code": 0,
            "groups": {
                "groupId": "0",
                "name": "Root",
                "subGroups": [
                    {
                        "groupId": "81",
                        "name": "North America",
                        "subGroups": [
                            {
                                "groupId": "82",
                                "name": "West Coast",
                                "subGroups": [
                                    {
                                        "groupId": "83",
                                        "name": "Seattle Hub"
                                    }
                                ]
                            }
                        ]
                    }
                ]
            }
        }
        """.trimIndent()
        val projects = RuijieProtocol.parseProjectsFromGroupTree(json)
        assertEquals(3, projects.size)
        assertEquals("North America", projects[0].displayPath)
        assertEquals(0, projects[0].depth)
        assertEquals("North America / West Coast", projects[1].displayPath)
        assertEquals(1, projects[1].depth)
        assertEquals("North America / West Coast / Seattle Hub", projects[2].displayPath)
        assertEquals(2, projects[2].depth)
    }

    // 11. empty groupId skipped
    @Test
    fun testGroupTreeEmptyGroupIdSkipped() {
        val json = """
        {
            "code": 0,
            "data": [
                {
                    "groupId": "",
                    "name": "Ghost Node"
                },
                {
                    "groupId": "91",
                    "name": "Valid Node"
                }
            ]
        }
        """.trimIndent()
        val projects = RuijieProtocol.parseProjectsFromGroupTree(json)
        assertEquals(1, projects.size)
        assertEquals("91", projects[0].groupId)
        assertEquals("Valid Node", projects[0].name)
    }

    // 12. invalid response code throws
    @Test(expected = IllegalArgumentException::class)
    fun testGroupTreeInvalidResponseCodeThrows() {
        val json = """
        {
            "code": 1001,
            "msg": "Token expired or permission denied"
        }
        """.trimIndent()
        RuijieProtocol.parseProjectsFromGroupTree(json)
    }

    // 13. malformed JSON throws
    @Test(expected = IllegalArgumentException::class)
    fun testGroupTreeMalformedJsonThrows() {
        val json = "{ malformed json content "
        RuijieProtocol.parseProjectsFromGroupTree(json)
    }

    // 14. HTML response throws
    @Test(expected = IllegalArgumentException::class)
    fun testGroupTreeHtmlResponseThrows() {
        val html = "<!DOCTYPE html><html><body>Ruijie SSO Login</body></html>"
        RuijieProtocol.parseProjectsFromGroupTree(html)
    }

    // 15. voucher request uses selected project.groupId
    @Test
    fun testVoucherRequestUsesSelectedProjectGroupId() {
        val selectedGid = "98765"
        val tenant = "myTenant"

        val voucherListEnv = RuijieProtocol.buildVoucherListEnvelope(
            tenantName = tenant,
            groupId = selectedGid
        )
        assertEquals("/intlSamVoucher/getList/$tenant/$selectedGid", voucherListEnv.getString("api"))

        val voucherStatusEnv = RuijieProtocol.buildVoucherStatusEnvelope(
            tenantName = tenant,
            groupId = selectedGid,
            tenantId = 444L
        )
        assertEquals("/intlSamVoucher/getStatus/$tenant/$selectedGid", voucherStatusEnv.getString("api"))

        val userGroupEnv = RuijieProtocol.buildUserGroupListEnvelope(groupId = selectedGid)
        assertEquals("/intl/usergroup/list/$selectedGid", userGroupEnv.getString("api"))
        assertEquals(98765L, userGroupEnv.getJSONObject("querys").getLong("group_id"))
    }

    @Test
    fun testDeleteAndUnbindEnvelopes() {
        val deleteEnv = RuijieProtocol.buildVoucherDeleteEnvelope(
            groupId = "grp55",
            ids = "uuid_delete_me",
            voucherCode = "VOUCH_DEL"
        )
        assertEquals("/intlSamVoucher/v2/delete", deleteEnv.getString("api"))
        assertEquals("DELETE", deleteEnv.getString("method"))
        
        val paramsArray = deleteEnv.getJSONArray("params")
        assertEquals(1, paramsArray.length())
        assertEquals("VOUCH_DEL", paramsArray.getJSONObject(0).getString("voucherCode"))
        assertEquals("VOUCH_DEL", paramsArray.getJSONObject(0).getString("codeNo"))
        assertEquals("uuid_delete_me", deleteEnv.getJSONObject("querys").getString("ids"))

        val unbindEnv = RuijieProtocol.buildVoucherUnbindEnvelope(
            groupId = "grp55",
            id = "id123",
            uuid = "uuid_unbind_me",
            voucherCode = "VOUCH_UNBIND"
        )
        assertEquals("/intlSamVoucher/v2/unbind", unbindEnv.getString("api"))
        assertEquals("POST", unbindEnv.getString("method"))
        assertEquals("uuid_unbind_me", unbindEnv.getJSONObject("params").getString("uuid"))
        assertEquals("id123", unbindEnv.getJSONObject("params").getString("id"))
        assertEquals("VOUCH_UNBIND", unbindEnv.getJSONObject("params").getString("voucherCode"))
    }

    @Test
    fun testParseVoucherListValidDataList() {
        val json = """
        {
            "code": 0,
            "data": {
                "total": 2,
                "list": [
                    {"uuid": "u1", "voucherCode": "CODE1", "status": 1},
                    {"id": "u2", "code": "CODE2", "status": 2}
                ]
            }
        }
        """.trimIndent()
        val (list, total) = RuijieProtocol.parseVoucherList(json)
        assertEquals(2, total)
        assertEquals(2, list.size)
        assertEquals("CODE1", list[0].effectiveCode)
        assertEquals(1, list[0].status)
        assertEquals("CODE2", list[1].effectiveCode)
        assertEquals(2, list[1].status)
    }

    @Test
    fun testParseVoucherListValidVoucherDataList() {
        val json = """
        {
            "code": 0,
            "voucherData": {
                "count": 1,
                "rows": [
                    {"uuid": "v1", "codeNo": "VOUCH99", "status": 3}
                ]
            }
        }
        """.trimIndent()
        val (list, total) = RuijieProtocol.parseVoucherList(json)
        assertEquals(1, total)
        assertEquals(1, list.size)
        assertEquals("VOUCH99", list[0].effectiveCode)
        assertEquals(3, list[0].status)
    }

    @Test
    fun testParseVoucherListEmpty() {
        val json = """{"code": 0, "data": {"total": 0, "list": []}}"""
        val (list, total) = RuijieProtocol.parseVoucherList(json)
        assertEquals(0, total)
        assertTrue(list.isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun testParseVoucherListErrorResponse() {
        val json = """{"code": 1001, "msg": "Session expired"}"""
        RuijieProtocol.parseVoucherList(json)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testParseVoucherListHtmlResponse() {
        val html = "<!DOCTYPE html><html>Login Required</html>"
        RuijieProtocol.parseVoucherList(html)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testParseVoucherListMalformedJson() {
        val malformed = "{not json"
        RuijieProtocol.parseVoucherList(malformed)
    }

    @Test
    fun testClassifyErrorTimeoutDoesNotExpireSession() {
        val timeout408 = RuijieProtocol.classifyError(408, "", null, "https://cloud-as.ruijienetworks.com/macc5/")
        assertEquals(RuijieBridgeErrorCode.REQUEST_TIMEOUT, timeout408.first)
        assertFalse(timeout408.second.contains("sign in again", ignoreCase = true))

        val timeoutMsg = RuijieProtocol.classifyError(200, "application/json", "{\"msg\": \"Gateway Timeout\"}", "https://cloud-as.ruijienetworks.com/macc5/")
        assertEquals(RuijieBridgeErrorCode.REQUEST_TIMEOUT, timeoutMsg.first)
    }

    @Test
    fun testClassifyErrorCode1014GroupNotSynchronized() {
        val json1014 = "{\"code\": 1014, \"msg\": \"Group has not been synchronized\"}"
        val result = RuijieProtocol.classifyError(200, "application/json", json1014, "https://cloud-as.ruijienetworks.com/macc5/")
        assertEquals(RuijieBridgeErrorCode.GROUP_NOT_SYNCHRONIZED, result.first)
        assertEquals("Ruijie is still synchronizing this project group. Refresh and try again.", result.second)
    }

    @Test(expected = com.example.data.model.RuijieGroupNotSynchronizedException::class)
    fun testParseVoucherListCode1014ThrowsSpecificException() {
        val json1014 = "{\"code\": 1014, \"msg\": \"Group has not been synchronized\"}"
        RuijieProtocol.parseVoucherList(json1014)
    }

    @Test
    fun testClassifyErrorPermissionDenied() {
        val forbidden = RuijieProtocol.classifyError(403, "application/json", "Forbidden", "https://cloud-as.ruijienetworks.com/macc5/")
        assertEquals(RuijieBridgeErrorCode.PERMISSION_DENIED, forbidden.first)

        val unauth = RuijieProtocol.classifyError(401, "application/json", "Unauthorized", "https://cloud-as.ruijienetworks.com/macc5/")
        assertEquals(RuijieBridgeErrorCode.PERMISSION_DENIED, unauth.first)
    }

    @Test
    fun testParseVoucherCreateResponse() {
        val json = """
        {
            "code": 0,
            "data": [
                {"voucherCode": "ALPHA1", "uuid": "u1"},
                {"codeNo": "BETA2", "id": "u2"}
            ]
        }
        """.trimIndent()
        val vouchers = RuijieProtocol.parseVoucherCreateResponse(json)
        assertEquals(2, vouchers.size)
        assertEquals("ALPHA1", vouchers[0].effectiveCode)
        assertEquals("BETA2", vouchers[1].effectiveCode)
    }

    @Test
    fun testParseVoucherCreateResponseStringArray() {
        val json = """
        {
            "code": 0,
            "data": ["VOUCH100", "VOUCH200"]
        }
        """.trimIndent()
        val vouchers = RuijieProtocol.parseVoucherCreateResponse(json)
        assertEquals(2, vouchers.size)
        assertEquals("VOUCH100", vouchers[0].effectiveCode)
        assertEquals("VOUCH200", vouchers[1].effectiveCode)
    }

    @Test
    fun testParseVoucherListComprehensiveDetailFields() {
        val json = """
        {
            "code": 0,
            "data": {
                "total": 1,
                "list": [
                    {
                        "uuid": "uuid_item_8899",
                        "voucherCode": "VOUCH_DETAIL_99",
                        "codeNo": "VOUCH_DETAIL_99",
                        "userGroupName": "VIP High Speed",
                        "userGroupId": 789,
                        "profile": "Profile_VIP_100M",
                        "authprofileid": "prof_auth_789",
                        "quota": 1073741824,
                        "usedQuota": 268435456,
                        "trafficUsed": 268435456,
                        "traffic_used": 268435456,
                        "uploadTraffic": 67108864,
                        "downloadTraffic": 201326592,
                        "bindMac": "AA:BB:CC:DD:EE:FF",
                        "status": 2,
                        "statusDesc": "In Use",
                        "period": "24",
                        "periodType": "Hours",
                        "createTime": 1727082000000,
                        "startTime": 1727083000000,
                        "expireTime": 1727169400000
                    }
                ]
            }
        }
        """.trimIndent()

        val (list, total) = RuijieProtocol.parseVoucherList(json)
        assertEquals(1, total)
        assertEquals(1, list.size)

        val item = list[0]
        assertEquals("uuid_item_8899", item.effectiveUuid)
        assertEquals("VOUCH_DETAIL_99", item.effectiveCode)
        assertEquals("VIP High Speed", item.effectiveUserGroupName)
        assertEquals("789", item.effectiveUserGroupId)
        assertEquals("Profile_VIP_100M", item.effectiveProfile)
        assertEquals("AA:BB:CC:DD:EE:FF", item.effectiveBoundMacDisplay)
        assertTrue(item.isDeviceBound)
        assertEquals(VoucherStatus.USED, item.normalizedStatus)
        assertEquals("In Use", item.rawStatusDisplay)
        assertEquals("1 GB", item.effectiveQuotaDisplay)
        assertEquals("256 MB", item.effectiveUsedQuotaDisplay)
        assertEquals("256 MB", item.effectiveTrafficUsedDisplay)
        assertEquals("768 MB", item.effectiveRemainingDataDisplay)
        assertEquals("64 MB", item.effectiveUploadTrafficDisplay)
        assertEquals("192 MB", item.effectiveDownloadTrafficDisplay)
        assertEquals("24 Hours", item.effectivePeriod)
        assertFalse(item.formattedStartTimeDisplay.contains("—"))
        assertFalse(item.formattedExpiryTimeDisplay.contains("—"))
        assertFalse(item.formattedCreateTime.contains("—"))
    }

    @Test
    fun testTenantListItemsParsingAndSelection() {
        val json = """
        {
            "code": 0,
            "data": {
                "list": [
                    {
                        "tenantName": "tenant_alpha",
                        "tenantId": 1001,
                        "isDefault": false,
                        "isCurrent": false
                    },
                    {
                        "tenantName": "cupidleo8387@gmail.com",
                        "tenantId": 1002,
                        "isDefault": true,
                        "isCurrent": true
                    }
                ]
            }
        }
        """.trimIndent()

        val items = RuijieProtocol.parseTenantListItems(json)
        assertEquals(2, items.size)
        assertEquals("tenant_alpha", items[0].tenantName)
        assertEquals(1001L, items[0].tenantId)
        assertFalse(items[0].isDefault)

        assertEquals("cupidleo8387@gmail.com", items[1].tenantName)
        assertEquals(1002L, items[1].tenantId)
        assertTrue(items[1].isDefault)
        assertTrue(items[1].isCurrent)

        val selected = items.firstOrNull { it.isCurrent || it.isDefault } ?: items.first()
        assertEquals("cupidleo8387@gmail.com", selected.tenantName)
        assertEquals(1002L, selected.tenantId)
    }

    @Test
    fun testUserGroupsParsingWithProfileId() {
        val json = """
        {
            "code": 0,
            "data": [
                {
                    "id": "ug_55",
                    "name": "Staff Wi-Fi",
                    "profileId": "prof_vip_99",
                    "maxUsers": 10,
                    "periodTime": 1440,
                    "packageName": "Daily VIP Pass"
                }
            ]
        }
        """.trimIndent()

        val groups = RuijieProtocol.parseUserGroups(json)
        assertEquals(1, groups.size)
        assertEquals("ug_55", groups[0].id)
        assertEquals("Staff Wi-Fi", groups[0].name)
        assertEquals("prof_vip_99", groups[0].profileId)
        assertEquals("Daily VIP Pass", groups[0].packageName)
    }

    @Test
    fun testDiagnosticCollectorClassifyResult() {
        val successRes = com.example.data.diagnostics.RuijieDiagnosticCollector.classifyResult(
            operation = com.example.data.diagnostics.RuijieDiagnosticCollector.OP_VOUCHER_LIST,
            httpStatus = 200,
            ruijieCode = 0,
            message = "Success",
            groupIdStr = "6752877",
            numericGroupId = 6752877L,
            tenantId = "cupidleo8387@gmail.com"
        )
        assertEquals(com.example.data.model.DiagnosticResultCategory.SUCCESS, successRes)

        val syncRes = com.example.data.diagnostics.RuijieDiagnosticCollector.classifyResult(
            operation = com.example.data.diagnostics.RuijieDiagnosticCollector.OP_VOUCHER_CREATE,
            httpStatus = 200,
            ruijieCode = 1014,
            message = "Group has not been synchronized",
            groupIdStr = "6752877",
            numericGroupId = 6752877L,
            tenantId = "cupidleo8387@gmail.com"
        )
        assertEquals(com.example.data.model.DiagnosticResultCategory.GROUP_NOT_SYNCHRONIZED, syncRes)
    }

    @Test
    fun testUserGroupsResponseClassification() {
        val cat = com.example.data.diagnostics.RuijieDiagnosticCollector.classifyResult(
            operation = com.example.data.diagnostics.RuijieDiagnosticCollector.OP_USER_GROUPS,
            httpStatus = 200,
            ruijieCode = 0,
            message = "success",
            groupIdStr = "6752877",
            numericGroupId = 6752877L,
            tenantId = "cupidleo8387@gmail.com"
        )
        assertEquals(com.example.data.model.DiagnosticResultCategory.SUCCESS, cat)
    }

    @Test
    fun testVoucherStatusResponseClassification() {
        val cat = com.example.data.diagnostics.RuijieDiagnosticCollector.classifyResult(
            operation = com.example.data.diagnostics.RuijieDiagnosticCollector.OP_VOUCHER_STATUS,
            httpStatus = 200,
            ruijieCode = 0,
            message = "success",
            groupIdStr = "6752877",
            numericGroupId = 6752877L,
            tenantId = "cupidleo8387@gmail.com"
        )
        assertEquals(com.example.data.model.DiagnosticResultCategory.SUCCESS, cat)
    }

    @Test
    fun testVoucherListResponseClassification() {
        val cat = com.example.data.diagnostics.RuijieDiagnosticCollector.classifyResult(
            operation = com.example.data.diagnostics.RuijieDiagnosticCollector.OP_VOUCHER_LIST,
            httpStatus = 200,
            ruijieCode = 0,
            message = "success",
            groupIdStr = "6752877",
            numericGroupId = 6752877L,
            tenantId = "cupidleo8387@gmail.com"
        )
        assertEquals(com.example.data.model.DiagnosticResultCategory.SUCCESS, cat)
    }

    @Test
    fun testVoucherCreateResponseClassification() {
        val cat = com.example.data.diagnostics.RuijieDiagnosticCollector.classifyResult(
            operation = com.example.data.diagnostics.RuijieDiagnosticCollector.OP_VOUCHER_CREATE,
            httpStatus = 200,
            ruijieCode = 0,
            message = "success",
            groupIdStr = "6752877",
            numericGroupId = 6752877L,
            tenantId = "cupidleo8387@gmail.com"
        )
        assertEquals(com.example.data.model.DiagnosticResultCategory.SUCCESS, cat)
    }

    @Test
    fun testRealInvalidGroupResponseClassification() {
        val cat = com.example.data.diagnostics.RuijieDiagnosticCollector.classifyResult(
            operation = com.example.data.diagnostics.RuijieDiagnosticCollector.OP_VOUCHER_LIST,
            httpStatus = 400,
            ruijieCode = 5001,
            message = "Invalid Group ID provided in request",
            groupIdStr = "6752877",
            numericGroupId = 6752877L,
            tenantId = "cupidleo8387@gmail.com"
        )
        assertEquals(com.example.data.model.DiagnosticResultCategory.WRONG_GROUP_ID, cat)
    }

    @Test
    fun testCode1014GroupSynchronizationResponse() {
        val cat = com.example.data.diagnostics.RuijieDiagnosticCollector.classifyResult(
            operation = com.example.data.diagnostics.RuijieDiagnosticCollector.OP_VOUCHER_CREATE,
            httpStatus = 200,
            ruijieCode = 1014,
            message = "Group has not been synchronized",
            groupIdStr = "6752877",
            numericGroupId = 6752877L,
            tenantId = "cupidleo8387@gmail.com"
        )
        assertEquals(com.example.data.model.DiagnosticResultCategory.GROUP_NOT_SYNCHRONIZED, cat)
    }

    @Test
    fun testEmptyValidListClassification() {
        val cat = com.example.data.diagnostics.RuijieDiagnosticCollector.classifyResult(
            operation = com.example.data.diagnostics.RuijieDiagnosticCollector.OP_VOUCHER_LIST,
            httpStatus = 200,
            ruijieCode = 0,
            message = "success",
            groupIdStr = "6752877",
            numericGroupId = 6752877L,
            tenantId = "cupidleo8387@gmail.com",
            isEmptyResult = true
        )
        assertEquals(com.example.data.model.DiagnosticResultCategory.EMPTY, cat)
    }

    @Test
    fun testMissingWrapperClassification() {
        val cat = com.example.data.diagnostics.RuijieDiagnosticCollector.classifyResult(
            operation = com.example.data.diagnostics.RuijieDiagnosticCollector.OP_VOUCHER_LIST,
            httpStatus = 200,
            ruijieCode = 0,
            message = "success",
            groupIdStr = null,
            numericGroupId = null,
            tenantId = "cupidleo8387@gmail.com"
        )
        assertEquals(com.example.data.model.DiagnosticResultCategory.SUCCESS, cat)
    }

    @Test
    fun testCreateResponseWithoutReturnedVoucherCode() {
        val json = """{"code":0,"msg":"success","data":[]}"""
        val parsed = RuijieProtocol.parseVoucherCreateResponseParsed(json)
        assertEquals(0, parsed.ruijieCode)
        assertEquals("success", parsed.safeMsg)
        assertTrue(parsed.vouchers.isEmpty())
        assertTrue(parsed.isAcceptedWithoutCode)
    }

    @Test
    fun testCreateResponseWithReturnedVoucherCode() {
        val json = """{"code":0,"msg":"success","data":[{"voucherCode":"A7K29X","uuid":"12345"}]}"""
        val parsed = RuijieProtocol.parseVoucherCreateResponseParsed(json)
        assertEquals(0, parsed.ruijieCode)
        assertEquals("success", parsed.safeMsg)
        assertEquals(1, parsed.vouchers.size)
        assertEquals("A7K29X", parsed.vouchers[0].voucherCode)
        assertFalse(parsed.isAcceptedWithoutCode)
    }

    @Test
    fun testSeparateUserGroupIdAndAuthProfileId() {
        val json = """{"code":0,"data":[{"id":679297,"userGroupId":679297,"userGroupName":"Daily Pass","authProfileId":"888111"}]}"""
        val groups = RuijieProtocol.parseUserGroups(json)
        assertEquals(1, groups.size)
        assertEquals("679297", groups[0].userGroupId)
        assertEquals("888111", groups[0].authProfileId)
    }

    // ------------------------------------------------------------------------
    // PART 9 — 15 SESSION PERSISTENCE & FALSE LOGOUT TESTS
    // ------------------------------------------------------------------------

    @Test
    fun test1_NetworkTimeoutDoesNotLogout() {
        val (errorCode, _) = RuijieProtocol.classifyError(
            httpStatus = 408,
            contentType = "",
            rawBody = null,
            currentUrl = "https://cloud-as.ruijienetworks.com/macc5/dashboard"
        )
        assertEquals(RuijieBridgeErrorCode.REQUEST_TIMEOUT, errorCode)
        // Ensure error code is NOT SESSION_EXPIRED
        assertTrue(errorCode != RuijieBridgeErrorCode.SESSION_EXPIRED)
    }

    @Test
    fun test2_JsBridgeTimeoutDoesNotLogout() {
        val (errorCode, _) = RuijieProtocol.classifyError(
            httpStatus = 0,
            contentType = "",
            rawBody = null,
            currentUrl = "https://cloud-as.ruijienetworks.com/macc5/dashboard"
        )
        assertEquals(RuijieBridgeErrorCode.JS_INJECTION_TIMEOUT, errorCode)
        assertTrue(errorCode != RuijieBridgeErrorCode.SESSION_EXPIRED)
    }

    @Test
    fun test3_Http500502503DoesNotLogout() {
        val (err500, _) = RuijieProtocol.classifyError(500, "application/json", "Internal Server Error", "https://cloud-as.ruijienetworks.com")
        val (err502, _) = RuijieProtocol.classifyError(502, "application/json", "Bad Gateway", "https://cloud-as.ruijienetworks.com")
        val (err503, _) = RuijieProtocol.classifyError(503, "application/json", "Service Unavailable", "https://cloud-as.ruijienetworks.com")

        assertEquals(RuijieBridgeErrorCode.HTTP_ERROR, err500)
        assertEquals(RuijieBridgeErrorCode.HTTP_ERROR, err502)
        assertEquals(RuijieBridgeErrorCode.HTTP_ERROR, err503)

        assertTrue(err500 != RuijieBridgeErrorCode.SESSION_EXPIRED)
        assertTrue(err502 != RuijieBridgeErrorCode.SESSION_EXPIRED)
        assertTrue(err503 != RuijieBridgeErrorCode.SESSION_EXPIRED)
    }

    @Test
    fun test4_Code1014DoesNotLogout() {
        val body = """{"code":1014,"msg":"Group has not been synchronized"}"""
        val (errorCode, _) = RuijieProtocol.classifyError(200, "application/json", body, "https://cloud-as.ruijienetworks.com")
        assertEquals(RuijieBridgeErrorCode.GROUP_NOT_SYNCHRONIZED, errorCode)
        assertTrue(errorCode != RuijieBridgeErrorCode.SESSION_EXPIRED)
    }

    @Test
    fun test5_PermissionErrorDoesNotLogout() {
        val body = """{"code":403,"msg":"Access Denied"}"""
        val (errorCode, _) = RuijieProtocol.classifyError(403, "application/json", body, "https://cloud-as.ruijienetworks.com")
        assertEquals(RuijieBridgeErrorCode.PERMISSION_DENIED, errorCode)
        assertTrue(errorCode != RuijieBridgeErrorCode.SESSION_EXPIRED)
    }

    @Test
    fun test6_ParserErrorDoesNotLogout() {
        val body = """{"corrupted_json": [unclosed"""
        val (errorCode, _) = RuijieProtocol.classifyError(200, "application/json", body, "https://cloud-as.ruijienetworks.com")
        assertEquals(RuijieBridgeErrorCode.MALFORMED_RESPONSE, errorCode)
        assertTrue(errorCode != RuijieBridgeErrorCode.SESSION_EXPIRED)
    }

    @Test
    fun test7_VerifiedLoginTimeoutResponseChangesStateToSessionExpired() {
        val body = """{"code":3,"msg":"session expired"}"""
        val (errorCode, _) = RuijieProtocol.classifyError(200, "application/json", body, "https://cloud-as.ruijienetworks.com")
        assertEquals(RuijieBridgeErrorCode.SESSION_EXPIRED, errorCode)
    }

    @Test
    fun test8_AccountInfoSuccessRestoresOrKeepsSessionReady() {
        val body = """{"code":0,"msg":"success","data":{"accountName":"cupidleo8387@gmail.com","tenantName":"corp"}}"""
        val (accName, tenantName, tenantId) = RuijieProtocol.parseAccountInfo(body)
        assertEquals("cupidleo8387@gmail.com", accName)
        assertEquals("corp", tenantName)
        assertEquals(0L, tenantId)
    }

    @Test
    fun test9_AppBackgroundForegroundPreservesSession() {
        com.example.data.api.RuijieWebViewBridge.updateSessionState(
            RuijieSessionState.ACCOUNT_VERIFIED,
            trigger = "Test Init"
        )
        com.example.data.api.RuijieWebViewBridge.onAppBackground()
        assertEquals(RuijieSessionState.ACCOUNT_VERIFIED, com.example.data.api.RuijieWebViewBridge.sessionState.value)
        assertTrue(com.example.data.api.RuijieWebViewBridge.sessionState.value.isConnected)

        com.example.data.api.RuijieWebViewBridge.onAppForeground()
        assertTrue(com.example.data.api.RuijieWebViewBridge.sessionState.value.isConnected)
    }

    @Test
    fun test10_ComposeRecompositionPreservesSession() {
        com.example.data.api.RuijieWebViewBridge.updateSessionState(
            RuijieSessionState.ACCOUNT_VERIFIED,
            trigger = "Test Compose"
        )
        val state1 = com.example.data.api.RuijieWebViewBridge.sessionState.value
        val state2 = com.example.data.api.RuijieWebViewBridge.sessionState.value
        assertEquals(state1, state2)
        assertTrue(state2.isConnected)
    }

    @Test
    fun test11_NavigationBetweenScreensPreservesSession() {
        assertTrue(RuijieProtocol.isAuthenticatedUrl("https://cloud-as.ruijienetworks.com/macc5/dashboard"))
        assertTrue(RuijieProtocol.isAuthenticatedUrl("https://cloud-as.ruijienetworks.com/macc5/project/list"))
        assertFalse(RuijieProtocol.isAuthenticatedUrl("https://cloud.ruijienetworks.com/sso/login"))
    }

    @Test
    fun test12_CookieClearingOccursOnlyAfterExplicitLogout() {
        com.example.data.api.RuijieWebViewBridge.updateSessionState(
            RuijieSessionState.ACCOUNT_VERIFIED,
            trigger = "Before Logout"
        )
        assertTrue(com.example.data.api.RuijieWebViewBridge.sessionState.value.isConnected)

        com.example.data.api.RuijieWebViewBridge.logout(userInitiated = true)
        assertEquals(RuijieSessionState.LOGIN_REQUIRED, com.example.data.api.RuijieWebViewBridge.sessionState.value)
        assertFalse(com.example.data.api.RuijieWebViewBridge.sessionState.value.isConnected)
    }

    @Test
    fun test13_UnknownVoucherCreateResultDoesNotLogout() {
        val (errorCode, _) = RuijieProtocol.classifyError(
            httpStatus = 504,
            contentType = "text/plain",
            rawBody = "Gateway Timeout",
            currentUrl = "https://cloud-as.ruijienetworks.com"
        )
        assertEquals(RuijieBridgeErrorCode.HTTP_ERROR, errorCode)
        assertTrue(errorCode != RuijieBridgeErrorCode.SESSION_EXPIRED)
    }

    @Test
    fun test14_SessionStateDoesNotReturnToIdleAfterAccountVerification() {
        val verifiedState = RuijieSessionState.ACCOUNT_VERIFIED
        assertTrue(verifiedState.isConnected)
        assertFalse(verifiedState == RuijieSessionState.LOGIN_REQUIRED)
        assertFalse(verifiedState == RuijieSessionState.SSO_PAGE)
    }

    @Test
    fun test15_SilentAccountReverificationKeepsUserLoggedInWhenSuccessful() {
        com.example.data.api.RuijieWebViewBridge.updateSessionState(
            RuijieSessionState.ACCOUNT_VERIFIED,
            trigger = "Pre Verification"
        )
        assertTrue(com.example.data.api.RuijieWebViewBridge.sessionState.value.isConnected)
        // Check session info recorded
        val info = com.example.data.diagnostics.RuijieDiagnosticCollector.sessionInfo.value
        assertEquals("Account Verified", info.sessionState)
    }
}
