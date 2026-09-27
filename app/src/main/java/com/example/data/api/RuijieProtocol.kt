package com.example.data.api

import com.example.data.model.GroupTreeDetails
import com.example.data.model.RuijieBridgeErrorCode
import com.example.data.model.RuijieProject
import com.example.data.model.RuijieProjectItem
import com.example.data.model.RuijieUserGroupItem
import com.example.data.model.RuijieVoucherItem
import com.example.data.model.VoucherCreateResult
import org.json.JSONArray
import org.json.JSONObject

object RuijieProtocol {
    const val SSO_LOGIN_PATH = "/login"
    const val SSO_LOGIN_URL = "https://cloud.ruijienetworks.com/sso/"
    const val DEFAULT_USER_AGENT = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36 RuijieCloudApp/1.0"

    fun buildLoginUrl(baseUrl: String): String {
        val root = baseUrl.trimEnd('/')
        return "$root/login"
    }

    fun buildPortalUrl(baseUrl: String, tenantId: String? = null): String {
        val root = baseUrl.trimEnd('/')
        return if (tenantId.isNullOrBlank()) "$root/m/dashboard" else "$root/m/dashboard?tenantId=$tenantId"
    }

    fun isLoginPage(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return url.contains("/sso/login") || url.contains("/login") || url.endsWith("/sso")
    }

    fun isAuthenticatedUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        if (isLoginPage(url)) return false
        if (url.contains("/sso/")) return false
        return url.contains("/macc5/") || url.contains("/dashboard") || url.contains("/project") || url.contains("/intlSamVoucher") || url.contains("/common/api")
    }

    fun buildWebProxyUrl(apiPath: String): String {
        val cleanPath = apiPath.trimStart('/')
        return "https://cloud-as.ruijienetworks.com/webproxy/common/api?$cleanPath"
    }

    fun classifyError(httpStatus: Int, contentType: String?, body: String?, url: String? = ""): Pair<RuijieBridgeErrorCode, String> {
        if (httpStatus == 401) return Pair(RuijieBridgeErrorCode.SESSION_EXPIRED, "Session expired (401)")
        if (httpStatus == 403) return Pair(RuijieBridgeErrorCode.PERMISSION_DENIED, "Permission denied (403)")
        if (httpStatus == 408) return Pair(RuijieBridgeErrorCode.REQUEST_TIMEOUT, "Request timeout (408)")
        if (httpStatus in 500..599) return Pair(RuijieBridgeErrorCode.HTTP_ERROR, "Server error ($httpStatus)")
        if (!contentType.isNullOrEmpty() && contentType.contains("text/html", ignoreCase = true)) {
            return Pair(RuijieBridgeErrorCode.HTML_RESPONSE, "Received HTML instead of JSON")
        }
        if (!body.isNullOrEmpty()) {
            try {
                if (body.trim().startsWith("<")) {
                    return Pair(RuijieBridgeErrorCode.HTML_RESPONSE, "HTML payload received")
                }
                val json = JSONObject(body)
                val code = json.optInt("code", 0)
                val msg = json.optString("msg", json.optString("message", ""))
                if (code == 3 || msg.contains("timeout", true) || msg.contains("expired", true) || code == 401) {
                    return Pair(RuijieBridgeErrorCode.SESSION_EXPIRED, msg.ifBlank { "Session expired" })
                }
                if (code == 403 || msg.contains("forbidden", true) || msg.contains("permission", true)) {
                    return Pair(RuijieBridgeErrorCode.PERMISSION_DENIED, msg.ifBlank { "Permission denied" })
                }
                if (code == 404 || msg.contains("not found", true) || msg.contains("group", true)) {
                    return Pair(RuijieBridgeErrorCode.GROUP_NOT_SYNCHRONIZED, msg.ifBlank { "Group not found" })
                }
                if (code != 0 && code != 200) {
                    return Pair(RuijieBridgeErrorCode.MALFORMED_RESPONSE, msg.ifBlank { "API error code $code" })
                }
            } catch (e: Exception) {
                // Not JSON
            }
        }
        return Pair(RuijieBridgeErrorCode.UNKNOWN, "Unknown error")
    }

    fun parseProjectsFromGroupTree(json: String?): List<RuijieProject> {
        val list = mutableListOf<RuijieProject>()
        if (json.isNullOrBlank()) return list
        try {
            val root = JSONObject(json)
            val groups = root.optJSONObject("groups") ?: root.optJSONArray("list")
            if (groups is JSONObject) {
                parseGroupNodeRecursive(groups, list)
            } else if (groups is JSONArray) {
                for (i in 0 until groups.length()) {
                    val obj = groups.optJSONObject(i)
                    if (obj != null) parseGroupNodeRecursive(obj, list)
                }
            }
        } catch (e: Exception) {
            // parse error
        }
        return list
    }

    private fun parseGroupNodeRecursive(node: JSONObject, outList: MutableList<RuijieProject>) {
        val id = node.optLong("groupId", node.optLong("id", 0L))
        val name = node.optString("name", node.optString("groupName", ""))
        val type = node.optString("type", "")
        outList.add(RuijieProject(id = id, groupId = id, name = name, type = type))
        val subGroups = node.optJSONArray("subGroups") ?: node.optJSONArray("children")
        if (subGroups != null) {
            for (i in 0 until subGroups.length()) {
                val sub = subGroups.optJSONObject(i)
                if (sub != null) parseGroupNodeRecursive(sub, outList)
            }
        }
    }

    fun buildAccountInfoEnvelope(): String = "{}"

    fun buildTenantListEnvelope(): String = "{}"

    fun buildVoucherListEnvelope(groupId: Any? = null, tenantId: Any? = null): String {
        return JSONObject().apply {
            put("groupId", groupId ?: "")
            put("tenantId", tenantId ?: "")
        }.toString()
    }

    fun buildVoucherStatusEnvelope(voucherId: Any? = null, status: Any? = null): String {
        return JSONObject().apply {
            put("id", voucherId ?: "")
            put("status", status ?: "")
        }.toString()
    }

    fun buildUserGroupListEnvelope(groupId: Any? = null): String {
        return JSONObject().apply {
            put("groupId", groupId ?: "")
        }.toString()
    }

    fun buildVoucherDeleteEnvelope(
        voucherId: Any? = null,
        groupId: Any? = null,
        ids: Any? = null,
        voucherCode: String? = null
    ): String {
        return JSONObject().apply {
            val effectiveId = voucherId ?: ids
            put("id", effectiveId ?: "")
            if (groupId != null) put("groupId", groupId.toString())
            if (ids != null && voucherId == null) put("ids", ids.toString())
            if (!voucherCode.isNullOrBlank()) put("voucherCode", voucherCode)
        }.toString()
    }

    fun buildVoucherUnbindEnvelope(
        voucherId: Any? = null,
        groupId: Any? = null,
        id: Any? = null,
        uuid: String? = null,
        voucherCode: String? = null
    ): String {
        return JSONObject().apply {
            val effectiveId = voucherId ?: id ?: uuid
            put("id", effectiveId ?: "")
            if (groupId != null) put("groupId", groupId.toString())
            if (!uuid.isNullOrBlank() && voucherId == null && id == null) put("uuid", uuid)
            if (!voucherCode.isNullOrBlank()) put("voucherCode", voucherCode)
        }.toString()
    }

    fun parseVoucherList(json: String?): Pair<List<RuijieVoucherItem>, Int> {
        val list = mutableListOf<RuijieVoucherItem>()
        if (json.isNullOrBlank()) return Pair(list, 0)
        try {
            val root = JSONObject(json)
            val voucherData = root.optJSONObject("voucherData") ?: root.optJSONObject("data") ?: root
            val jsonArray = voucherData.optJSONArray("list") ?: root.optJSONArray("list")
            val total = voucherData.optInt("count", voucherData.optInt("total", jsonArray?.length() ?: 0))
            if (jsonArray != null) {
                for (i in 0 until jsonArray.length()) {
                    val itemObj = jsonArray.optJSONObject(i)
                    if (itemObj != null) {
                        list.add(
                            RuijieVoucherItem(
                                uuid = itemObj.optString("uuid", itemObj.optString("id")),
                                voucherCode = itemObj.optString("voucherCode", itemObj.optString("code")),
                                codeNo = itemObj.optString("codeNo").ifBlank { null },
                                code = itemObj.optString("code", itemObj.optString("voucherCode")),
                                status = itemObj.opt("status")
                            )
                        )
                    }
                }
            }
            return Pair(list, total)
        } catch (e: Exception) {
            return Pair(list, 0)
        }
    }

    fun parseVoucherCreateResponse(json: String?): List<RuijieVoucherItem> {
        val list = mutableListOf<RuijieVoucherItem>()
        if (json.isNullOrBlank()) return list
        try {
            val root = JSONObject(json)
            // Create response nests items under "voucherData" (real API) or "data"
            val data = root.optJSONObject("data")
                ?: root.optJSONObject("voucherData")
                ?: root
            val arr = data.optJSONArray("list") ?: root.optJSONArray("list")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i)
                    if (obj != null) {
                        list.add(
                            RuijieVoucherItem(
                                voucherCode = obj.optString("voucherCode", obj.optString("code")),
                                // Create response uses `codeNo` per API Reference Manual V2.0.3
                                codeNo = obj.optString("codeNo").ifBlank { null },
                                status = obj.opt("status"),
                                uuid = obj.optString("uuid").ifBlank { null },
                                profileId = obj.optString("profileId").ifBlank { null },
                                tenantId = obj.optString("tenantId").ifBlank { null },
                                groupId = obj.opt("groupId")
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // ignore
        }
        return list
    }

    fun parseVoucherCreateResponseParsed(json: String?, httpStatus: Int = 200): VoucherCreateResult {
        val vouchers = parseVoucherCreateResponse(json)
        var ruijieCode: Int? = null
        var safeMsg = ""
        if (!json.isNullOrBlank()) {
            try {
                val root = JSONObject(json)
                // Nested voucherData carries the real operation result; prefer a
                // non-zero nested code (error) over the outer envelope code.
                val nestedCode = (root.optJSONObject("data") ?: root.optJSONObject("voucherData"))
                    ?.optInt("code", Int.MIN_VALUE)?.takeIf { it != Int.MIN_VALUE }
                val outerCode = root.optInt("code", Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE }
                ruijieCode = if (nestedCode != null && nestedCode != 0) nestedCode else outerCode ?: nestedCode
                val nestedMsg = (root.optJSONObject("data") ?: root.optJSONObject("voucherData"))
                    ?.optString("msg", "")?.ifBlank { null }
                safeMsg = if (ruijieCode != null && ruijieCode != 0 && nestedMsg != null) nestedMsg
                    else root.optString("msg", root.optString("message", ""))
            } catch (e: Exception) {
                // ignore
            }
        }
        val firstCode = vouchers.firstOrNull()?.effectiveCode
        return VoucherCreateResult(
            ruijieCode = ruijieCode,
            safeMsg = safeMsg,
            vouchers = vouchers,
            voucherCode = firstCode,
            isAcceptedWithoutCode = vouchers.isEmpty() && httpStatus in 200..299
        )
    }

    fun parseTenantListItems(json: String?): List<RuijieProject> {
        val list = mutableListOf<RuijieProject>()
        if (json.isNullOrBlank()) return list
        try {
            val root = JSONObject(json)
            val data = root.optJSONArray("data") ?: root.optJSONArray("list") ?: root.optJSONArray("tenants")
            if (data != null) {
                for (i in 0 until data.length()) {
                    val obj = data.optJSONObject(i)
                    if (obj != null) {
                        list.add(
                            RuijieProject(
                                tenantId = obj.optLong("tenantId", obj.optLong("id", 0L)),
                                tenantName = obj.optString("tenantName", obj.optString("name", ""))
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // ignore
        }
        return list
    }

    fun parseUserGroups(json: String?): List<RuijieUserGroupItem> {
        val list = mutableListOf<RuijieUserGroupItem>()
        if (json.isNullOrBlank()) return list
        try {
            val root = JSONObject(json)
            val data = root.optJSONArray("data") ?: root.optJSONArray("list") ?: root.optJSONArray("userGroups")
            if (data != null) {
                for (i in 0 until data.length()) {
                    val obj = data.optJSONObject(i) ?: continue
                    list.add(
                        RuijieUserGroupItem(
                            id = obj.optString("id", obj.optString("userGroupId", null)),
                            name = obj.optString("name", obj.optString("userGroupName", "")),
                            packageName = obj.optString("packageName", obj.optString("name", "")),
                            periodTime = obj.optLong("periodTime", obj.optLong("timePeriod", 0L)),
                            maxUsers = obj.optInt("maxUsers", obj.optInt("maxClients", -1)).takeIf { it >= 0 },
                            profileId = obj.optString("profileId", obj.optString("authprofileid", ""))
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // ignore
        }
        return list
    }

    fun buildGroupTreeEnvelope(): String {
        return JSONObject().apply {
            put("api", "/group/all/tree?1=1&group_id=0")
            put("method", "GET")
        }.toString()
    }

    fun parseProjectsFromGroupTreeWithDetails(json: String?): GroupTreeDetails {
        val items = mutableListOf<RuijieProjectItem>()
        var wrapperDetected = "none"
        var rootGroupId: Long? = null
        var rootGroupCount: Int? = null
        if (!json.isNullOrBlank()) {
            try {
                val root = JSONObject(json)
                // Detect wrapper: Ruijie sometimes wraps in {code, msg, data: {...}}
                if (root.has("code") || root.has("data")) {
                    wrapperDetected = "code-msg-data"
                }
                val dataObj = root.optJSONObject("data") ?: root
                rootGroupId = dataObj.optLong("rootGroupId", 0L).takeIf { it != 0L }
                    ?: root.optLong("rootGroupId", 0L).takeIf { it != 0L }
                val groups = dataObj.optJSONObject("groups") ?: dataObj.optJSONArray("list") ?: root.optJSONArray("list")
                if (groups is JSONObject) {
                    if (rootGroupId == null) rootGroupId = groups.optLong("groupId", 0L).takeIf { it != 0L }
                    parseGroupNodeDetailed(groups, items, parentId = null, depth = 0, path = "")
                    rootGroupCount = 1
                } else if (groups is JSONArray) {
                    rootGroupCount = groups.length()
                    for (i in 0 until groups.length()) {
                        val obj = groups.optJSONObject(i) ?: continue
                        parseGroupNodeDetailed(obj, items, parentId = null, depth = 0, path = "")
                    }
                }
            } catch (e: Exception) {
                // parse error
            }
        }
        return GroupTreeDetails(
            projects = items,
            wrapperDetected = wrapperDetected,
            rootGroupId = rootGroupId,
            rootGroupCount = rootGroupCount
        )
    }

    private fun parseGroupNodeDetailed(
        node: JSONObject,
        outList: MutableList<RuijieProjectItem>,
        parentId: Long?,
        depth: Int,
        path: String
    ) {
        val id = node.optLong("groupId", node.optLong("id", 0L))
        val name = node.optString("name", node.optString("groupName", ""))
        val groupName = node.optString("groupName", name)
        val tenantName = node.optString("tenantName", null)
        val currentPath = if (path.isBlank()) name else "$path / $name"
        outList.add(
            RuijieProjectItem(
                numericGroupId = id,
                groupId = id.toString(),
                name = name.ifBlank { null },
                groupName = groupName.ifBlank { null },
                parentGroupId = parentId,
                depth = depth,
                displayPath = currentPath.ifBlank { null },
                tenantName = tenantName?.ifBlank { null }
            )
        )
        val subGroups = node.optJSONArray("subGroups") ?: node.optJSONArray("children")
        if (subGroups != null) {
            for (i in 0 until subGroups.length()) {
                val sub = subGroups.optJSONObject(i) ?: continue
                parseGroupNodeDetailed(sub, outList, parentId = id, depth = depth + 1, path = currentPath)
            }
        }
    }

    fun buildVoucherCreateEnvelope(
        groupId: Any? = null,
        quantity: Int = 1,
        profile: String? = null,
        userGroupId: Any? = null,
        tenantName: String? = null,
        userName: String? = null,
        count: Int? = null,
        codeType: String? = null,
        length: Int? = null,
        profileId: String? = null
    ): String {
        return JSONObject().apply {
            put("groupId", groupId ?: "")
            put("quantity", count ?: quantity)
            val effectiveProfile = profileId?.ifBlank { null } ?: profile
            if (!effectiveProfile.isNullOrBlank()) put("profile", effectiveProfile)
            if (userGroupId != null) put("userGroupId", userGroupId.toString())
            if (!tenantName.isNullOrBlank()) put("tenantName", tenantName)
            if (!userName.isNullOrBlank()) put("userName", userName)
            if (!codeType.isNullOrBlank()) put("codeType", codeType)
            if (length != null && length > 0) put("length", length)
        }.toString()
    }

    fun parseAccountInfo(json: String?): Triple<String?, String?, String?> {
        if (json.isNullOrBlank()) return Triple(null, null, null)
        try {
            val root = JSONObject(json)
            val data = root.optJSONObject("data") ?: root
            val accName = data.optString("username", data.optString("accountName", null))
            val tenantName = data.optString("tenantName", null)
            val tenantId = data.optString("tenantId", data.optLong("tenantId", 0L).let { if (it == 0L) null else it.toString() })
            return Triple(accName, tenantName, tenantId)
        } catch (e: Exception) {
            return Triple(null, null, null)
        }
    }
}
