package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Normalized Voucher Status from Ruijie Cloud.
 * Ruijie Cloud standard:
 * 1 -> Not Used / Active
 * 2 -> In-use / Used
 * 3 -> Expired
 */
enum class VoucherStatus(val displayName: String) {
    NOT_USED("NOT USED"),
    USED("USED"),
    EXPIRED("EXPIRED")
}

enum class VoucherFilter(val displayName: String) {
    ALL("ALL"),
    NOT_USED("NOT USED"),
    USED("USED"),
    EXPIRED("EXPIRED")
}

enum class VoucherCodeType(
    val label: String,
    val subtext: String,
    val createCodeType: String,
    val apiValue: Int
) {
    ALPHANUMERIC("Alphanumeric", "0-9a-z", "1", 1),
    ALPHABETIC("Alphabetic", "a-z", "2", 2),
    NUMERIC("Numeric", "0-9", "3", 3)
}

/**
 * Ruijie Network Group / Project Model
 */
@JsonClass(generateAdapter = true)
data class RuijieProject(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "groupId") val groupId: Long? = null,
    @Json(name = "group_id") val groupIdAlt: Long? = null,
    @Json(name = "groupIdStr") val groupIdStr: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "groupName") val groupName: String? = null,
    @Json(name = "group_name") val groupNameAlt: String? = null,
    @Json(name = "parentGroupId") val parentGroupId: String? = null,
    @Json(name = "displayPath") val displayPath: String? = null,
    @Json(name = "depth") val depth: Int = 0,
    @Json(name = "type") val type: String? = null,
    @Json(name = "tenantId") val tenantId: Long? = null,
    @Json(name = "tenant_id") val tenantIdAlt: Long? = null,
    @Json(name = "tenantName") val tenantName: String? = null,
    @Json(name = "deviceCount") val deviceCount: Int? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "isCurrent") val isCurrent: Boolean = false,
    @Json(name = "isDefault") val isDefault: Boolean = false
) {
    val effectiveId: Long
        get() = groupId ?: groupIdAlt ?: id ?: groupIdStr?.toLongOrNull() ?: 0L

    val effectiveGroupId: String
        get() = groupIdStr?.ifBlank { null }
            ?: groupId?.toString()
            ?: groupIdAlt?.toString()
            ?: id?.toString()
            ?: ""

    val effectiveGidStr: String
        get() = effectiveGroupId

    val effectiveName: String
        get() = displayPath?.ifBlank { null }
            ?: groupName?.ifBlank { null }
            ?: groupNameAlt?.ifBlank { null }
            ?: name?.ifBlank { null }
            ?: "Project $effectiveGroupId"
}

/**
 * Response model for GET /service/api/group/single/tree?depth=BUILDING
 */
@JsonClass(generateAdapter = true)
data class RuijieGroupTreeResponse(
    @Json(name = "code") val code: Int? = null,
    @Json(name = "msg") val msg: String? = null,
    @Json(name = "groups") val groups: RuijieGroupNode? = null,
    @Json(name = "rootGroupName") val rootGroupName: String? = null,
    @Json(name = "rootGroupId") val rootGroupId: Long? = null
)

@JsonClass(generateAdapter = true)
data class RuijieGroupNode(
    @Json(name = "name") val name: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "timezone") val timezone: String? = null,
    @Json(name = "groupId") val groupId: Long? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "subGroups") val subGroups: List<RuijieGroupNode> = emptyList()
)

/**
 * Exact Ruijie Voucher List response structure:
 * GET /service/api/open/auth/voucher/getList/{groupId}
 * or GET /service/api/intlSamVoucher/getList/{tenantName}/{groupId}
 */
@JsonClass(generateAdapter = true)
data class VoucherListResponse(
    @Json(name = "code") val code: Int = 0,
    @Json(name = "msg") val msg: String? = null,
    @Json(name = "voucherData") val voucherData: VoucherDataWrapper? = null,
    @Json(name = "data") val dataWrapper: VoucherDataWrapper? = null,
    @Json(name = "count") val directCount: Int? = null,
    @Json(name = "list") val directList: List<RuijieVoucherItem>? = null
)

@JsonClass(generateAdapter = true)
data class VoucherDataWrapper(
    @Json(name = "code") val code: Int? = null,
    @Json(name = "msg") val msg: String? = null,
    @Json(name = "count") val count: Int = 0,
    @Json(name = "list") val list: List<RuijieVoucherItem> = emptyList()
)

/**
 * ACTUAL Cloud Voucher Item as returned by Ruijie Cloud API.
 * NO invented data!
 */
@JsonClass(generateAdapter = true)
data class RuijieVoucherItem(
    @Json(name = "uuid") val uuid: String? = null,
    @Json(name = "id") val id: String? = null,
    @Json(name = "voucherCode") val voucherCode: String? = null,
    @Json(name = "codeNo") val codeNo: String? = null,
    @Json(name = "code") val code: String? = null,
    @Json(name = "status") val status: Any? = null,
    @Json(name = "statusDesc") val statusDesc: String? = null,
    @Json(name = "statusName") val statusName: String? = null,
    @Json(name = "voucherStatus") val voucherStatus: Any? = null,
    @Json(name = "state") val state: Any? = null,
    @Json(name = "disableStatus") val disableStatus: Any? = null,
    @Json(name = "tenantId") val tenantId: String? = null,
    @Json(name = "secuserId") val secuserId: String? = null,
    @Json(name = "totle") val totle: Any? = null,
    @Json(name = "sign") val sign: String? = null,
    @Json(name = "profileId") val profileId: String? = null,
    @Json(name = "authprofileid") val authprofileid: String? = null,
    @Json(name = "profile") val profile: String? = null,
    @Json(name = "packageName") val packageName: String? = null,
    @Json(name = "profileName") val profileName: String? = null,
    @Json(name = "authProfileName") val authProfileName: String? = null,
    @Json(name = "userGroupName") val userGroupName: String? = null,
    @Json(name = "groupName") val groupName: String? = null,
    @Json(name = "userGroupId") val userGroupId: Any? = null,
    @Json(name = "groupId") val groupId: Any? = null,
    @Json(name = "projectName") val projectName: String? = null,
    @Json(name = "comment") val comment: String? = null,
    @Json(name = "remark") val remark: String? = null,
    @Json(name = "firstName") val firstName: String? = null,
    @Json(name = "lastName") val lastName: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "timePeriod") val timePeriod: Any? = null,
    @Json(name = "validPeriod") val validPeriod: String? = null,
    @Json(name = "duration") val duration: String? = null,
    @Json(name = "period") val period: String? = null,
    @Json(name = "periodType") val periodType: String? = null,
    @Json(name = "createTime") val createTime: Any? = null,
    @Json(name = "createdTime") val createdTime: Any? = null,
    @Json(name = "usedTime") val usedTime: Any? = null,
    @Json(name = "startTime") val startTime: Any? = null,
    @Json(name = "firstLoginTime") val firstLoginTime: Any? = null,
    @Json(name = "activeTime") val activeTime: Any? = null,
    @Json(name = "activateTime") val activateTime: Any? = null,
    @Json(name = "loginTime") val loginTime: Any? = null,
    @Json(name = "endTime") val endTime: Any? = null,
    @Json(name = "expiryTime") val expiryTime: Any? = null,
    @Json(name = "expiredTime") val expiredTime: Any? = null,
    @Json(name = "expireTime") val expireTime: Any? = null,
    @Json(name = "expireDate") val expireDate: Any? = null,
    @Json(name = "quotalimit") val quotalimit: Any? = null,
    @Json(name = "remainingQuota") val remainingQuota: Any? = null,
    @Json(name = "remainTime") val remainTime: Any? = null,
    @Json(name = "remainingTime") val remainingTime: Any? = null,
    @Json(name = "tenantName") val tenantName: String? = null,
    @Json(name = "projectGroupId") val projectGroupId: String? = null,
    @Json(name = "quota") val quota: Any? = null,
    @Json(name = "dataQuota") val dataQuota: Any? = null,
    @Json(name = "totalTraffic") val totalTraffic: Any? = null,
    @Json(name = "usedQuota") val usedQuota: Any? = null,
    @Json(name = "usedData") val usedData: Any? = null,
    @Json(name = "usedTraffic") val usedTraffic: Any? = null,
    @Json(name = "trafficUsed") val trafficUsed: Any? = null,
    @Json(name = "usedFlow") val usedFlow: Any? = null,
    @Json(name = "remainQuota") val remainQuota: Any? = null,
    @Json(name = "remainFlow") val remainFlow: Any? = null,
    @Json(name = "uploadTraffic") val uploadTraffic: Any? = null,
    @Json(name = "upTraffic") val upTraffic: Any? = null,
    @Json(name = "uploadFlow") val uploadFlow: Any? = null,
    @Json(name = "downloadTraffic") val downloadTraffic: Any? = null,
    @Json(name = "downTraffic") val downTraffic: Any? = null,
    @Json(name = "downloadFlow") val downloadFlow: Any? = null,
    @Json(name = "maxClients") val maxClients: Int? = null,
    @Json(name = "limitClients") val limitClients: Int? = null,
    @Json(name = "currentClients") val currentClients: Int? = null,
    @Json(name = "qrcodeUrl") val qrcodeUrl: String? = null,
    @Json(name = "downloadRateLimit") val downloadRateLimit: Long? = null,
    @Json(name = "uploadRateLimit") val uploadRateLimit: Long? = null,
    @Json(name = "packagePrice") val packagePrice: Any? = null,
    @Json(name = "bindMac") val bindMac: Any? = null,
    @Json(name = "bindingMac") val bindingMac: Any? = null,
    @Json(name = "mac") val mac: Any? = null,
    @Json(name = "macAddress") val macAddress: Any? = null,
    @Json(name = "clientMac") val clientMac: Any? = null,
    @Json(name = "terminalMac") val terminalMac: Any? = null,
    @Json(name = "userMac") val userMac: Any? = null
) {
    /**
     * Primary voucher code from Ruijie Cloud.
     * Per API Reference Manual V2.0.3 the voucher code field is `codeNo`
     * (create response); list responses use `voucherCode`. Prefer `codeNo`.
     */
    val effectiveCode: String
        get() = codeNo?.ifBlank { null }
            ?: voucherCode?.ifBlank { null }
            ?: code?.ifBlank { null }
            ?: ""

    /**
     * Cloud UUID for voucher operations (distinct from voucher code)
     */
    val effectiveUuid: String
        get() = uuid?.ifBlank { null }
            ?: id?.ifBlank { null }
            ?: "—"

    /**
     * Profile / Package name as returned by Ruijie Cloud
     */
    val effectiveProfile: String
        get() = packageName?.ifBlank { null }
            ?: profileName?.ifBlank { null }
            ?: profile?.ifBlank { null }
            ?: authProfileName?.ifBlank { null }
            ?: userGroupName?.ifBlank { null }
            ?: "—"

    val effectiveUserGroupName: String
        get() = userGroupName?.ifBlank { null }
            ?: groupName?.ifBlank { null }
            ?: "—"

    val effectiveUserGroupId: String
        get() = when (val ug = userGroupId) {
            is Number -> ug.toString()
            is String -> ug.ifBlank { "—" }
            else -> "—"
        }

    val effectiveGroupId: String
        get() = when (val g = groupId) {
            is Number -> g.toString()
            is String -> g.ifBlank { "—" }
            else -> "—"
        }

    val rawStatusDisplay: String
        get() = statusDesc?.ifBlank { null }
            ?: statusName?.ifBlank { null }
            ?: when (val st = status ?: voucherStatus ?: state) {
                is Number -> st.toString()
                is String -> st.ifBlank { "—" }
                else -> "—"
            }

    /**
     * Period string formatted from actual Cloud timePeriod or validPeriod
     */
    val effectivePeriod: String
        get() {
            if (!validPeriod.isNullOrBlank() && validPeriod != "null" && validPeriod != "0") {
                return validPeriod.trim()
            }
            if (!duration.isNullOrBlank() && duration != "null" && duration != "0") {
                return duration.trim()
            }
            if (!period.isNullOrBlank() && period != "null" && period != "0") {
                return if (!periodType.isNullOrBlank()) "${period.trim()} ${periodType.trim()}" else period.trim()
            }
            val minutes = when (val tp = timePeriod) {
                is Number -> tp.toInt()
                is String -> tp.toIntOrNull()
                else -> null
            }
            if (minutes != null) {
                return formatCloudMinutes(minutes)
            }
            return "—"
        }

    /**
     * Total Quota parsed in raw bytes (0 if unavailable, -1 if unlimited)
     */
    val effectiveTotalBytes: Long
        get() {
            // Using quota or dataQuota as total
            val raw = quota ?: dataQuota 
            return parseRawQuotaToBytes(raw)
        }

    /**
     * Used Quota / Data parsed in raw bytes
     */
    val effectiveUsedBytes: Long
        get() {
            // Using usedQuota or usedData as used
            val raw = usedQuota ?: usedData
            return parseRawUsageToBytes(raw)
        }

    val effectiveQuotaDisplay: String
        get() {
            val bytes = effectiveTotalBytes
            if (bytes == -1L) return "Unlimited"
            val raw = quota ?: dataQuota ?: totalTraffic ?: totle ?: quotalimit
            if (raw == null) return "—"
            val rawStr = raw.toString().trim()
            if (rawStr == "0" || rawStr.equals("unlimited", ignoreCase = true) || rawStr == "-1") return "Unlimited"
            if (bytes <= 0L) return "—"
            return formatDataQuota(bytes)
        }

    val effectiveQuota: String
        get() = effectiveQuotaDisplay

    val effectiveUsedDataDisplay: String
        get() {
            val raw = usedQuota ?: usedData ?: usedTraffic ?: trafficUsed ?: usedFlow
            if (raw == null) {
                return if (normalizedStatus == VoucherStatus.NOT_USED) "0 MB" else "—"
            }
            val bytes = effectiveUsedBytes
            return if (bytes >= 0L) formatDataQuota(bytes) else "—"
        }

    val effectiveUsedQuotaDisplay: String
        get() = effectiveUsedDataDisplay

    val effectiveTrafficUsedDisplay: String
        get() = effectiveUsedDataDisplay

    val effectiveUploadTrafficDisplay: String
        get() {
            val raw = uploadTraffic ?: upTraffic ?: uploadFlow
            if (raw == null) return "—"
            val bytes = parseRawUsageToBytes(raw)
            return if (bytes >= 0L) formatDataQuota(bytes) else "—"
        }

    val effectiveDownloadTrafficDisplay: String
        get() {
            val raw = downloadTraffic ?: downTraffic ?: downloadFlow
            if (raw == null) return "—"
            val bytes = parseRawUsageToBytes(raw)
            return if (bytes >= 0L) formatDataQuota(bytes) else "—"
        }

    val formattedExpiryTimeDisplay: String
        get() {
            val ts = expiryTimestampMillis
            if (ts > 0L) {
                return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(ts))
            }
            val raw = expiryTime ?: expiredTime ?: expireTime ?: expireDate ?: endTime
            if (raw is String && raw.isNotBlank() && raw != "0" && !raw.equals("null", ignoreCase = true)) {
                return raw.trim()
            }
            return "—"
        }

    val effectiveRemainingDataDisplay: String
        get() {
            val total = effectiveTotalBytes
            if (total == -1L) return "Unlimited"
            val rawQuota = quota ?: dataQuota ?: totalTraffic ?: totle ?: quotalimit
            val rawTotalStr = rawQuota?.toString()?.trim()
            if (rawTotalStr == "0" || rawTotalStr.equals("unlimited", ignoreCase = true) || rawTotalStr == "-1") {
                return "Unlimited"
            }
            val directRemain = remainQuota ?: remainFlow ?: remainingQuota
            if (directRemain != null) {
                val remainBytes = parseRawUsageToBytes(directRemain)
                if (remainBytes >= 0L) return formatDataQuota(remainBytes)
            }
            if (total > 0L) {
                return formatDataQuota(effectiveRemainingBytes)
            }
            return "—"
        }

    val effectiveRemainingBytes: Long
        get() {
            val total = effectiveTotalBytes
            val used = effectiveUsedBytes
            
            // If total/used are absent, check if remainQuota/remainFlow is present
            val directRemain = parseRawUsageToBytes(remainQuota ?: remainFlow)
            
            if (total == -1L) return -1L
            
            // Only calculate if total and used are actually present/valid
            if (quota != null || dataQuota != null) {
                return (total - used).coerceAtLeast(0L)
            }
            
            // Fallback to direct field if available
            return directRemain
        }

    /**
     * Data usage progress (0.0 to 1.0)
     */
    val effectiveDataUsageProgress: Float
        get() {
            val total = effectiveTotalBytes
            if (total <= 0L) return 0f
            return (effectiveUsedBytes.toFloat() / total.toFloat()).coerceIn(0f, 1f)
        }

    /**
     * Formatted Bound MAC Address (e.g. "AA:BB:CC:DD:EE:FF") or null if none
     */
    val effectiveBoundMac: String?
        get() {
            val rawMac = bindMac ?: bindingMac ?: mac ?: macAddress ?: clientMac ?: terminalMac ?: userMac
            return formatMacAddress(rawMac)
        }

    val effectiveBoundMacDisplay: String
        get() {
            val macStr = effectiveBoundMac
            if (!macStr.isNullOrBlank()) return macStr
            return "Not bound"
        }

    val isDeviceBound: Boolean
        get() = !effectiveBoundMac.isNullOrBlank()

    /**
     * Activation / Start time
     */
    val effectiveStartTimeMillis: Long
        get() {
            val raw = startTime ?: firstLoginTime ?: activeTime ?: loginTime ?: usedTime
            return parseTimestampToMillis(raw)
        }

    val formattedStartTimeDisplay: String
        get() {
            val ts = effectiveStartTimeMillis
            if (ts > 0L) {
                return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(ts))
            }
            val raw = startTime ?: firstLoginTime ?: activeTime ?: loginTime ?: usedTime
            if (raw is String && raw.isNotBlank() && raw != "0" && !raw.equals("null", ignoreCase = true)) {
                return raw.trim()
            }
            return "—"
        }

    /**
     * Normalized status (NOT_USED, USED, EXPIRED)
     */
    val normalizedStatus: VoucherStatus
        get() {
            // 1. Explicit Ruijie API EXPIRED status
            val isExplicitlyDisabled = when (val d = disableStatus) {
                is Number -> d.toInt() == 1 || d.toInt() == 3
                is String -> d.trim() == "1" || d.trim() == "3" || d.uppercase().trim() in listOf("DISABLE", "DISABLED", "EXPIRED", "OUT_OF_DATE")
                is Boolean -> d
                else -> false
            }
            if (isExplicitlyDisabled) {
                return VoucherStatus.EXPIRED
            }

            // 2. Explicit status / state enum from API
            val rawStatus = status ?: voucherStatus ?: state
            val rawStatusStr = when (rawStatus) {
                is Number -> rawStatus.toInt().toString()
                is String -> rawStatus.uppercase().trim()
                else -> ""
            }

            when (rawStatusStr) {
                "3", "4", "5", "-1", "EXPIRED", "OUT_OF_DATE", "DISABLE", "DISABLED", "INVALID" -> return VoucherStatus.EXPIRED
                "2", "USED", "IN_USE", "INUSE", "ONLINE", "ACTIVATED", "CONSUM", "CONSUMED" -> return VoucherStatus.USED
            }

            // 3. Valid expiry timestamp that has passed (only when expiryTime / expiredTime is explicitly provided and > 0)
            val expTs = expiryTimestampMillis
            if (expTs > 0L && expTs <= System.currentTimeMillis()) {
                return VoucherStatus.EXPIRED
            }

            when (rawStatusStr) {
                "1", "0", "NOT_USED", "NOTUSED", "NOT_USE", "NOT USE", "UNUSED", "ACTIVE", "NORMAL" -> return VoucherStatus.NOT_USED
            }

            // 4. Default: NOT_USED
            return VoucherStatus.NOT_USED
        }

    val effectiveStatus: String
        get() = normalizedStatus.displayName

    val formattedCreateTime: String
        get() {
            val ts = createTimestampMillis
            return if (ts > 0L) {
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(ts))
            } else if (createTime is String && (createTime as String).isNotBlank()) {
                createTime as String
            } else if (createdTime is String && (createdTime as String).isNotBlank()) {
                createdTime as String
            } else {
                "—"
            }
        }

    val formattedExpiryTime: String?
        get() {
            val ts = expiryTimestampMillis
            return if (ts > 0L) {
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(ts))
            } else {
                null
            }
        }

    val createTimestampMillis: Long
        get() = parseTimestampToMillis(createTime ?: createdTime)

    val expiryTimestampMillis: Long
        get() = parseTimestampToMillis(expiryTime ?: expiredTime ?: expireTime ?: expireDate ?: endTime)

    fun isExpiredOnOrBefore(targetDateEndOfDayMillis: Long): Boolean {
        if (normalizedStatus == VoucherStatus.EXPIRED) {
            val ts = expiryTimestampMillis
            return if (ts > 0L) {
                ts <= targetDateEndOfDayMillis
            } else {
                true
            }
        }
        val ts = expiryTimestampMillis
        return ts > 0L && ts <= targetDateEndOfDayMillis
    }

    /**
     * Checks if this voucher is EXPIRED and was created BEFORE the target date (start of day millis).
     */
    fun isExpiredCreatedBefore(targetStartOfDayMillis: Long): Boolean {
        if (normalizedStatus != VoucherStatus.EXPIRED) return false
        val ts = createTimestampMillis
        return if (ts > 0L) {
            ts < targetStartOfDayMillis
        } else {
            val expTs = expiryTimestampMillis
            if (expTs > 0L) expTs < targetStartOfDayMillis else false
        }
    }

    /**
     * Effective actual MAC address from any MAC-related field.
     */
    val effectiveActualMac: String?
        get() {
            val candidates = listOf(macAddress, clientMac, terminalMac, userMac, mac, bindMac, bindingMac)
            for (c in candidates) {
                val formatted = formatMacAddress(c)
                if (!formatted.isNullOrBlank() && formatted !in listOf("0", "1", "true", "false", "yes", "no", "Not returned by Ruijie response")) {
                    return formatted
                }
            }
            return null
        }

    /**
     * Whether the voucher is bound to a client MAC.
     */
    val effectiveIsBound: Boolean
        get() {
            val bindVal = bindMac?.toString()?.trim()?.lowercase() ?: bindingMac?.toString()?.trim()?.lowercase()
            if (bindVal == "1" || bindVal == "true" || bindVal == "yes") return true
            return effectiveActualMac != null
        }

    /**
     * Effective project group ID, falling back to voucher fields then selected project/account.
     */
    fun getEffectiveProjectGroupId(selectedProject: RuijieProject?, activeAccount: RuijieAccount?): String {
        projectGroupId?.ifBlank { null }?.let { return it }
        effectiveGroupId.takeIf { it != "—" }?.let { return it }
        selectedProject?.effectiveGroupId?.takeIf { it != "—" && it != "0" && it.isNotBlank() }?.let { return it }
        activeAccount?.selectedGroupId?.ifBlank { null }?.let { return it }
        return "—"
    }

    /**
     * Effective project name, falling back to voucher fields then selected project/account.
     */
    fun getEffectiveProjectName(selectedProject: RuijieProject?): String {
        projectName?.ifBlank { null }?.let { return it }
        groupName?.ifBlank { null }?.let { return it }
        selectedProject?.effectiveName?.ifBlank { null }?.let { return it }
        return "—"
    }

    companion object {
        fun formatCloudMinutes(minutes: Int): String {
            return when {
                minutes <= 0 -> "Unlimited"
                minutes % 1440 == 0 -> {
                    val days = minutes / 1440
                    "$days Day${if (days > 1) "s" else ""}"
                }
                minutes > 1440 -> {
                    val days = minutes / 1440
                    val rem = minutes % 1440
                    val hrs = rem / 60
                    if (hrs > 0) "$days Day${if (days > 1) "s" else ""} $hrs Hr${if (hrs > 1) "s" else ""}"
                    else "$days Day${if (days > 1) "s" else ""}"
                }
                minutes % 60 == 0 -> {
                    val hrs = minutes / 60
                    "$hrs Hour${if (hrs > 1) "s" else ""}"
                }
                else -> {
                    val hrs = minutes / 60
                    val remMin = minutes % 60
                    if (hrs > 0) "$hrs Hour${if (hrs > 1) "s" else ""} $remMin Mins"
                    else "$minutes Minutes"
                }
            }
        }

        fun formatDataQuota(bytes: Long): String {
            return when {
                bytes >= 1073741824L -> {
                    if (bytes % 1073741824L == 0L) "${bytes / 1073741824L} GB"
                    else String.format(Locale.US, "%.1f GB", bytes / 1073741824.0)
                }
                bytes >= 1048576L -> {
                    if (bytes % 1048576L == 0L) "${bytes / 1048576L} MB"
                    else String.format(Locale.US, "%.1f MB", bytes / 1048576.0)
                }
                bytes >= 1024L -> {
                    if (bytes % 1024L == 0L) "${bytes / 1024L} KB"
                    else String.format(Locale.US, "%.1f KB", bytes / 1024.0)
                }
                else -> "$bytes Bytes"
            }
        }

        fun parseRawQuotaToBytes(value: Any?): Long {
            if (value == null) return 0L
            val num: Long = when (value) {
                is Number -> {
                    val l = value.toLong()
                    if (l == -1L) return -1L
                    l
                }
                is String -> {
                    val s = value.trim()
                    if (s.equals("unlimited", ignoreCase = true) || s == "-1") {
                        return -1L
                    }
                    if (s.endsWith("GB", ignoreCase = true)) {
                        val v = s.replace("GB", "", ignoreCase = true).trim().toDoubleOrNull() ?: 0.0
                        return (v * 1073741824.0).toLong()
                    }
                    if (s.endsWith("MB", ignoreCase = true)) {
                        val v = s.replace("MB", "", ignoreCase = true).trim().toDoubleOrNull() ?: 0.0
                        return (v * 1048576.0).toLong()
                    }
                    if (s.endsWith("KB", ignoreCase = true)) {
                        val v = s.replace("KB", "", ignoreCase = true).trim().toDoubleOrNull() ?: 0.0
                        return (v * 1024.0).toLong()
                    }
                    if (s.endsWith("B", ignoreCase = true)) {
                        return s.replace("B", "", ignoreCase = true).trim().toLongOrNull() ?: 0L
                    }
                    s.toLongOrNull() ?: 0L
                }
                else -> 0L
            }
            if (num <= 0L) return 0L
            return when {
                num >= 1_000_000L -> num // Already in bytes (>= 1MB)
                num in 1L..99_999L -> num * 1048576L // in MB (e.g. 1024 -> 1GB, 500 -> 500MB)
                else -> num
            }
        }

        fun parseRawUsageToBytes(value: Any?): Long {
            if (value == null) return 0L
            val num: Long = when (value) {
                is Number -> value.toLong()
                is String -> {
                    val s = value.trim()
                    if (s.endsWith("GB", ignoreCase = true)) {
                        val v = s.replace("GB", "", ignoreCase = true).trim().toDoubleOrNull() ?: 0.0
                        return (v * 1073741824.0).toLong()
                    }
                    if (s.endsWith("MB", ignoreCase = true)) {
                        val v = s.replace("MB", "", ignoreCase = true).trim().toDoubleOrNull() ?: 0.0
                        return (v * 1048576.0).toLong()
                    }
                    if (s.endsWith("KB", ignoreCase = true)) {
                        val v = s.replace("KB", "", ignoreCase = true).trim().toDoubleOrNull() ?: 0.0
                        return (v * 1024.0).toLong()
                    }
                    if (s.endsWith("B", ignoreCase = true)) {
                        return s.replace("B", "", ignoreCase = true).trim().toLongOrNull() ?: 0L
                    }
                    s.toLongOrNull() ?: 0L
                }
                else -> 0L
            }
            if (num <= 0L) return 0L
            return when {
                num >= 1_000_000L -> num
                num in 1L..99_999L -> num * 1048576L
                else -> num
            }
        }

        fun formatMacAddress(value: Any?): String? {
            if (value == null) return null
            val raw = when (value) {
                is String -> value.trim()
                is Collection<*> -> value.firstOrNull()?.toString()?.trim() ?: ""
                is Array<*> -> value.firstOrNull()?.toString()?.trim() ?: ""
                else -> value.toString().trim()
            }
            if (raw.isBlank() || raw == "0" || raw.equals("null", ignoreCase = true) || raw == "[]" || raw == "{}") {
                return null
            }
            val clean = raw.replace(":", "").replace("-", "").replace(".", "").uppercase()
            return if (clean.length == 12 && clean.all { it in "0123456789ABCDEF" }) {
                clean.chunked(2).joinToString(":")
            } else {
                raw.uppercase()
            }
        }

        fun parseTimestampToMillis(value: Any?): Long {
            if (value == null) return 0L
            var ts = when (value) {
                is Number -> value.toLong()
                is String -> value.toLongOrNull() ?: parseDateStringToTimestamp(value)
                else -> 0L
            }
            if (ts in 1..9999999999L) {
                ts *= 1000L
            }
            return ts
        }

        fun parseTimestamp(value: Any?): Long = parseTimestampToMillis(value)

        private fun parseDateStringToTimestamp(dateStr: String): Long {
            val patterns = listOf(
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd HH:mm",
                "MMM d, yyyy, h:mm:ss a"
            )
            for (p in patterns) {
                try {
                    val sdf = SimpleDateFormat(p, Locale.ENGLISH)
                    val d = sdf.parse(dateStr)
                    if (d != null) return d.time
                } catch (_: Exception) {}
            }
            return 0L
        }
    }
}

/**
 * ACTUAL Cloud Package/Profile Item from Ruijie Cloud API.
 * NO invented or hardcoded profiles!
 */
@JsonClass(generateAdapter = true)
data class RuijiePackageItem(
    @Json(name = "id") val id: Any? = null,
    @Json(name = "uuid") val uuid: String? = null,
    @Json(name = "packageId") val packageId: Any? = null,
    @Json(name = "profileId") val profileId: Any? = null,
    @Json(name = "authprofileid") val authprofileid: String? = null,
    @Json(name = "userGroupId") val userGroupId: Any? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "packageName") val packageName: String? = null,
    @Json(name = "profileName") val profileName: String? = null,
    @Json(name = "userGroupName") val userGroupName: String? = null,
    @Json(name = "timePeriod") val timePeriod: Any? = null,
    @Json(name = "validPeriod") val validPeriod: String? = null,
    @Json(name = "duration") val duration: String? = null,
    @Json(name = "price") val price: Any? = null,
    @Json(name = "maxClients") val maxClients: Int? = null,
    @Json(name = "limitClients") val limitClients: Int? = null,
    @Json(name = "quota") val quota: Long? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "remark") val remark: String? = null
) {
    val effectiveProfileUuid: String
        get() = authprofileid?.ifBlank { null }
            ?: uuid?.ifBlank { null }
            ?: profileId?.toString()?.ifBlank { null }
            ?: packageId?.toString()?.ifBlank { null }
            ?: id?.toString()?.ifBlank { null }
            ?: ""

    val effectiveUserGroupId: String
        get() = userGroupId?.toString()?.ifBlank { null }
            ?: id?.toString()?.ifBlank { null }
            ?: ""

    val effectiveId: String
        get() = effectiveProfileUuid.ifBlank { null }
            ?: effectiveName

    val effectiveName: String
        get() = packageName?.ifBlank { null }
            ?: profileName?.ifBlank { null }
            ?: userGroupName?.ifBlank { null }
            ?: name?.ifBlank { null }
            ?: "Package $effectiveId"

    val effectiveDuration: String
        get() {
            if (!validPeriod.isNullOrBlank() && validPeriod != "null" && validPeriod != "0") {
                return validPeriod.trim()
            }
            if (!duration.isNullOrBlank() && duration != "null" && duration != "0") {
                return duration.trim()
            }
            val minutes = when (val tp = timePeriod) {
                is Number -> tp.toInt()
                is String -> tp.toIntOrNull()
                else -> null
            }
            if (minutes != null) {
                return RuijieVoucherItem.formatCloudMinutes(minutes)
            }
            return "N/A"
        }

    val effectivePriceFormatted: String?
        get() {
            val p = when (val pr = price) {
                is Number -> pr.toDouble()
                is String -> pr.toDoubleOrNull()
                else -> null
            }
            return if (p != null && p > 0.0) {
                String.format(Locale.US, "$%.2f", p)
            } else null
        }

    val effectiveClientsFormatted: String?
        get() {
            val c = maxClients ?: limitClients
            return if (c != null && c > 0) {
                "$c device${if (c > 1) "s" else ""}"
            } else null
        }

    val effectiveQuotaFormatted: String?
        get() {
            val q = quota ?: return null
            if (q <= 0L) return null
            return RuijieVoucherItem.formatDataQuota(q)
        }
}

/**
 * Documented Voucher creation request payload for Ruijie Cloud API
 * POST /service/api/open/auth/voucher/create/{groupId}
 */
@JsonClass(generateAdapter = true)
data class RuijieCreateVoucherRequestBody(
    @Json(name = "quantity") val quantity: Int,
    @Json(name = "profile") val profile: String,
    @Json(name = "userGroupId") val userGroupId: Any,
    @Json(name = "createCodeType") val createCodeType: String? = "1",
    @Json(name = "codeSize") val codeSize: Int? = 6,
    @Json(name = "firstName") val firstName: String? = null,
    @Json(name = "lastName") val lastName: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "comment") val comment: String? = null
)

/**
 * Legacy request model kept for compatibility
 */
@JsonClass(generateAdapter = true)
data class RuijieCreateVoucherRequest(
    @Json(name = "profile") val profile: String? = null,
    @Json(name = "packageId") val packageId: String? = null,
    @Json(name = "packageName") val packageName: String? = null,
    @Json(name = "amount") val amount: Int = 1,
    @Json(name = "quantity") val quantity: Int = 1,
    @Json(name = "userGroupId") val userGroupId: Any? = null,
    @Json(name = "createCodeType") val createCodeType: String? = "1",
    @Json(name = "codeSize") val codeSize: Int? = 6,
    @Json(name = "remark") val remark: String? = null,
    @Json(name = "comment") val comment: String? = null,
    @Json(name = "description") val description: String? = null
)

/**
 * Root Ruijie Cloud Create Voucher API response
 * POST /service/api/open/auth/voucher/create/{groupId}
 */
@JsonClass(generateAdapter = true)
data class RuijieCreateVoucherResponse(
    @Json(name = "code") val code: Int? = null,
    @Json(name = "msg") val msg: String? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "voucherData") val voucherData: RuijieVoucherData? = null
)

/**
 * Nested VoucherData object inside Ruijie Cloud Create Voucher API response
 */
@JsonClass(generateAdapter = true)
data class RuijieVoucherData(
    @Json(name = "code") val code: Int? = null,
    @Json(name = "msg") val msg: String? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "count") val count: Int? = null,
    @Json(name = "list") val list: List<RuijieGeneratedVoucherRecord>? = null
)

/**
 * Voucher record returned inside voucherData.list from Ruijie Cloud
 */
@JsonClass(generateAdapter = true)
data class RuijieGeneratedVoucherRecord(
    @Json(name = "uuid") val uuid: String? = null,
    @Json(name = "codeNo") val codeNo: String? = null,
    @Json(name = "voucherCode") val voucherCode: String? = null,
    @Json(name = "code") val code: String? = null,
    @Json(name = "status") val status: Any? = null,
    @Json(name = "tenantId") val tenantId: String? = null,
    @Json(name = "secuserId") val secuserId: Any? = null,
    @Json(name = "totle") val totle: Any? = null,
    @Json(name = "sign") val sign: String? = null,
    @Json(name = "profileId") val profileId: Any? = null,
    @Json(name = "profile") val profile: Any? = null,
    @Json(name = "expiryTime") val expiryTime: Any? = null,
    @Json(name = "limitClients") val limitClients: Any? = null,
    @Json(name = "maxClients") val maxClients: Any? = null,
    @Json(name = "qrcodeUrl") val qrcodeUrl: String? = null,
    @Json(name = "groupId") val groupId: Any? = null,
    @Json(name = "firstName") val firstName: String? = null,
    @Json(name = "lastName") val lastName: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "comment") val comment: String? = null
) {
    val effectiveCode: String
        get() = codeNo?.ifBlank { null }
            ?: voucherCode?.ifBlank { null }
            ?: code?.ifBlank { null }
            ?: uuid ?: ""
}

/**
 * OAuth Token Request Body matching Ruijie Cloud API Section 2.1.1 "Get an Access Token":
 * Body (JSON): {"appid": "...", "secret": "..."}
 */
@JsonClass(generateAdapter = true)
data class RuijieTokenRequestBody(
    @Json(name = "appid") val appid: String,
    @Json(name = "secret") val secret: String
)

/**
 * Diagnostic payload record for Ruijie API
 */
data class ApiDiagnosticRecord(
    val httpMethod: String = "GET",
    val server: String = "",
    val endpoint: String = "",
    val httpStatus: Int = 0,
    val responseCode: Int? = null,
    val responseMessage: String = "",
    val voucherDataCode: Int? = null,
    val voucherDataMessage: String? = null,
    val voucherCount: Int? = null,
    val returnedListCount: Int? = null,
    val rawJson: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Detailed project item parsed from group tree (with all metadata).
 */
data class RuijieProjectItem(
    val numericGroupId: Long = 0L,
    val groupId: String = "",
    val name: String? = null,
    val groupName: String? = null,
    val parentGroupId: Long? = null,
    val depth: Int = 0,
    val displayPath: String? = null,
    val tenantName: String? = null
)

/**
 * User group / package item for voucher generation.
 */
data class RuijieUserGroupItem(
    val id: String? = null,
    val name: String = "",
    val packageName: String = "",
    val periodTime: Long = 0L,
    val maxUsers: Int? = null,
    val profileId: String = ""
)

/**
 * Detailed result of parsing a group tree response.
 */
data class GroupTreeDetails(
    val projects: List<RuijieProjectItem> = emptyList(),
    val wrapperDetected: String = "none",
    val rootGroupId: Long? = null,
    val rootGroupCount: Int? = null
)

/**
 * Diagnostic info for project/group-tree fetching.
 */
data class ProjectDiagnosticInfo(
    val endpointPath: String = "",
    val httpMethod: String = "GET",
    val httpStatus: Int = 0,
    val responseCode: Int? = null,
    val responseMsg: String? = null,
    val responseWrapperDetected: String = "none",
    val rootGroupId: Long? = null,
    val rootGroupCount: Int? = null,
    val flattenedProjectCount: Int = 0,
    val visibleProjects: List<Pair<String?, String?>> = emptyList(),
    val selectedProjectName: String? = null,
    val selectedGroupId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Diagnostic info for voucher list fetching.
 */
data class VoucherDiagnosticInfo(
    val sessionState: String = "",
    val webViewAlive: Boolean = false,
    val cookiePresent: Boolean = false,
    val selectedTenantName: String = "",
    val selectedProjectName: String? = null,
    val selectedGroupIdStr: String = "",
    val selectedGroupIdNumeric: Long = 0L,
    val voucherEndpointPath: String = "",
    val httpStatus: Int = 0,
    val responseMsg: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Result of parsing a voucher create response.
 */
data class VoucherCreateResult(
    val ruijieCode: Int? = null,
    val safeMsg: String = "",
    val vouchers: List<RuijieVoucherItem> = emptyList(),
    val voucherCode: String? = null,
    val isAcceptedWithoutCode: Boolean = false
)


/**
 * Extension properties for Voucher (RuijieVoucherItem) printing compatibility.
 */
val RuijieVoucherItem.createdAt: Long
    get() = createTime?.let {
        when (it) {
            is Number -> it.toLong()
            is String -> it.toLongOrNull() ?: 0L
            else -> 0L
        }
    } ?: System.currentTimeMillis()

val RuijieVoucherItem.maxDevices: Int?
    get() = maxClients ?: limitClients

val RuijieVoucherItem.price: Double?
    get() = when (val p = packagePrice) {
        is Number -> p.toDouble()
        is String -> p.toDoubleOrNull()
        else -> null
    }

val RuijieVoucherItem.password: String?
    get() = null
