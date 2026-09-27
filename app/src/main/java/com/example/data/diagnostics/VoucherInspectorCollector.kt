package com.example.data.diagnostics

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.model.FieldInspection
import com.example.data.model.InspectorRequestMeta
import com.example.data.model.PackageTypeDetectionResult
import com.example.data.model.RuijieVoucherItem
import com.example.data.model.VoucherInspectionReport
import com.example.data.model.VoucherPackageType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object VoucherInspectorCollector {

    private val _selectedVoucherId = MutableStateFlow<String?>(null)
    val selectedVoucherId: StateFlow<String?> = _selectedVoucherId.asStateFlow()

    private val _selectedVoucherCode = MutableStateFlow<String?>(null)
    val selectedVoucherCode: StateFlow<String?> = _selectedVoucherCode.asStateFlow()

    private val _lastInspectionReport = MutableStateFlow<VoucherInspectionReport?>(null)
    val lastInspectionReport: StateFlow<VoucherInspectionReport?> = _lastInspectionReport.asStateFlow()

    private var isRefreshInProgress = false

    fun selectVoucher(uuid: String?, code: String?) {
        _selectedVoucherId.value = uuid
        _selectedVoucherCode.value = code
    }

    fun clearSelection() {
        _selectedVoucherId.value = null
        _selectedVoucherCode.value = null
        _lastInspectionReport.value = null
    }

    fun isDuplicateRefresh(): Boolean {
        return isRefreshInProgress
    }

    fun blockDuplicateRefresh(block: () -> Unit) {
        if (isRefreshInProgress) return
        isRefreshInProgress = true
        try {
            block()
        } finally {
            isRefreshInProgress = false
        }
    }

    fun onListRefreshed(vouchers: List<RuijieVoucherItem>, sourceState: String = "list snapshot"): RuijieVoucherItem? {
        val targetId = _selectedVoucherId.value
        val targetCode = _selectedVoucherCode.value

        val matched = vouchers.find { item ->
            val idMatch = !targetId.isNullOrBlank() && (item.uuid == targetId || item.id == targetId)
            val codeMatch = !targetCode.isNullOrBlank() && (item.voucherCode == targetCode || item.codeNo == targetCode || item.code == targetCode)
            idMatch || codeMatch
        }

        if (matched != null) {
            _selectedVoucherId.value = matched.uuid ?: matched.id
            _selectedVoucherCode.value = matched.voucherCode ?: matched.codeNo ?: matched.code
            inspectVoucher(matched, sourceState = sourceState)
        } else {
            _lastInspectionReport.value = null
        }
        return matched
    }

    fun detectPackageType(voucher: RuijieVoucherItem): PackageTypeDetectionResult {
        val name = voucher.packageName ?: voucher.profileName ?: voucher.profile ?: ""
        val id = voucher.profileId ?: voucher.authprofileid ?: "—"

        val hasGbLabel = name.contains("GB", ignoreCase = true) || 
                         name.contains("MB", ignoreCase = true) || 
                         name.contains("DATA", ignoreCase = true)

        val hasTimeLabel = name.contains("Hour", ignoreCase = true) || 
                           name.contains("Hours", ignoreCase = true) || 
                           name.contains("TIME", ignoreCase = true)

        val hasExplicitBothFlag = name.contains("BOTH", ignoreCase = true)

        return when {
            hasExplicitBothFlag -> PackageTypeDetectionResult(
                packageType = VoucherPackageType.BOTH,
                reason = "Explicit BOTH flag verified in profile/package name.",
                profileName = name,
                profileId = id
            )
            hasGbLabel -> PackageTypeDetectionResult(
                packageType = VoucherPackageType.GB_DATA,
                reason = "Profile/package name hint suggests DATA_GB limits ($name).",
                profileName = name,
                profileId = id
            )
            hasTimeLabel -> PackageTypeDetectionResult(
                packageType = VoucherPackageType.HOURS_TIME,
                reason = "Profile/package name hint suggests HOURS_TIME limits ($name).",
                profileName = name,
                profileId = id
            )
            else -> PackageTypeDetectionResult(
                packageType = VoucherPackageType.UNKNOWN,
                reason = "Unable to classify package type safely. Defaulting to UNKNOWN.",
                profileName = name,
                profileId = id
            )
        }
    }

    fun parseRawNumericValue(v: Any?): Double? {
        if (v == null) return null
        return when (v) {
            is Number -> v.toDouble()
            is String -> {
                val s = v.trim()
                if (s.isBlank() || s.equals("null", ignoreCase = true) || s == "—" || s.startsWith("Not returned")) {
                    null
                } else if (s.endsWith("GB", ignoreCase = true)) {
                    val num = s.replace("GB", "", ignoreCase = true).trim().toDoubleOrNull()
                    if (num != null) num * 1024.0 else null
                } else if (s.endsWith("MB", ignoreCase = true)) {
                    s.replace("MB", "", ignoreCase = true).trim().toDoubleOrNull()
                } else if (s.endsWith("KB", ignoreCase = true)) {
                    val num = s.replace("KB", "", ignoreCase = true).trim().toDoubleOrNull()
                    if (num != null) num / 1024.0 else null
                } else if (s.endsWith("B", ignoreCase = true)) {
                    val num = s.replace("B", "", ignoreCase = true).trim().toDoubleOrNull()
                    if (num != null) num / 1048576.0 else null
                } else {
                    s.toDoubleOrNull()
                }
            }
            else -> null
        }
    }

    fun formatNumericValue(valD: Double): String {
        return if (valD % 1.0 == 0.0) {
            valD.toLong().toString()
        } else {
            String.format(Locale.US, "%.1f", valD)
        }
    }

    fun isNonZeroRawValue(v: Any?): Boolean {
        if (v == null) return false
        val d = parseRawNumericValue(v)
        if (d != null) return d > 0.0
        val s = v.toString().trim().lowercase()
        return s.isNotBlank() && s != "0" && s != "0.0" && s != "false" && s != "null" && s != "—"
    }

    fun inspectVoucher(
        voucher: RuijieVoucherItem,
        endpoint: String = "/intlSamVoucher/getList",
        httpStatus: Int = 200,
        ruijieCode: Int = 0,
        durationMs: Long = 0,
        sourceState: String = "live response",
        selectedProject: com.example.data.model.RuijieProject? = null,
        activeAccount: com.example.data.model.RuijieAccount? = null
    ): VoucherInspectionReport {
        val detection = detectPackageType(voucher)

        val identities = listOf(
            FieldInspection("uuid", "uuid", voucher.uuid ?: "—", "UUID", "N/A", voucher.uuid != null, false),
            FieldInspection("voucherCode", "voucherCode", voucher.voucherCode ?: "—", "Voucher code", "N/A", voucher.voucherCode != null, false),
            FieldInspection("codeNo", "codeNo", voucher.codeNo ?: "—", "codeNo", "N/A", voucher.codeNo != null, false)
        )

        val packages = listOf(
            FieldInspection("profileName", "profileName", voucher.profileName ?: "—", voucher.profileName ?: "—", "N/A", voucher.profileName != null, false),
            FieldInspection("packageName", "packageName", voucher.packageName ?: "—", voucher.packageName ?: "—", "N/A", voucher.packageName != null, false),
            FieldInspection("profileId", "profileId", voucher.profileId ?: "—", voucher.profileId ?: "—", "N/A", voucher.profileId != null, false),
            FieldInspection("userGroupId", "userGroupId", voucher.effectiveUserGroupId, voucher.effectiveUserGroupId, "N/A", voucher.userGroupId != null, false),
            FieldInspection("userGroupName", "userGroupName", voucher.effectiveUserGroupName, voucher.effectiveUserGroupName, "N/A", voucher.userGroupName != null, false)
        )

        val statuses = listOf(
            FieldInspection("status", "status", voucher.status?.toString() ?: "—", voucher.status?.toString() ?: "—", "N/A", voucher.status != null, false),
            FieldInspection("normalizedStatus", "status", voucher.status?.toString() ?: "—", voucher.normalizedStatus.displayName, "N/A", voucher.status != null, true),
            FieldInspection("disableStatus", "disableStatus", voucher.disableStatus?.toString() ?: "—", voucher.disableStatus?.toString() ?: "—", "N/A", voucher.disableStatus != null, false),
            FieldInspection("statusDesc", "statusDesc", voucher.statusDesc ?: "—", voucher.statusDesc ?: "—", "N/A", voucher.statusDesc != null, false)
        )

        val calculationErrors = mutableListOf<String>()

        val dataGbs = mutableListOf<FieldInspection>()
        val dataFields = mapOf(
            "quota" to (voucher.quota),
            "dataQuota" to (voucher.dataQuota),
            "quotalimit" to (voucher.quotalimit),
            "usedQuota" to (voucher.usedQuota),
            "usedData" to (voucher.usedData),
            "remainQuota" to (voucher.remainQuota),
            "remainingQuota" to (voucher.remainingQuota),
            "trafficUsed" to (voucher.trafficUsed),
            "usedTraffic" to (voucher.usedTraffic),
            "usedFlow" to (voucher.usedFlow),
            "totalTraffic" to (voucher.totalTraffic),
            "uploadTraffic" to (voucher.uploadTraffic),
            "downloadTraffic" to (voucher.downloadTraffic)
        )

        dataFields.forEach { (key, value) ->
            val present = value != null
            val raw = value?.toString() ?: "Not returned by Ruijie response"
            val dVal = parseRawNumericValue(value)
            val norm = if (present) {
                if (dVal != null) formatNumericValue(dVal) else "Unit not confirmed from Ruijie response"
            } else {
                "Not returned by Ruijie response"
            }
            dataGbs.add(FieldInspection(key, key, raw, norm, if (present && dVal != null) "MB" else "N/A", present, false))
        }

        // Diagnostic Checks for DATA/GB
        val totalD = parseRawNumericValue(voucher.quota ?: voucher.dataQuota)
        val usedD = parseRawNumericValue(voucher.usedQuota ?: voucher.usedData)
        val rawQuotaStr = (voucher.quota ?: voucher.dataQuota)?.toString() ?: ""
        val rawUsedStr = (voucher.usedQuota ?: voucher.usedData)?.toString() ?: ""

        if (rawQuotaStr.isNotBlank() && isNonZeroRawValue(rawQuotaStr) && (totalD == null || totalD == 0.0)) {
            calculationErrors.add("Normalization error: raw quota is non-zero ($rawQuotaStr) but normalized total is 0.")
        }
        if (rawUsedStr.isNotBlank() && isNonZeroRawValue(rawUsedStr) && (usedD == null || usedD == 0.0)) {
            calculationErrors.add("Normalization error: raw usedQuota is non-zero ($rawUsedStr) but normalized used is 0.")
        }

        if (totalD != null && usedD != null) {
            val expectedRemaining = maxOf(totalD - usedD, 0.0)
            val displayedRemainRaw = voucher.remainQuota ?: voucher.remainFlow
            val displayedRemainD = parseRawNumericValue(displayedRemainRaw)
            if (displayedRemainRaw != null && displayedRemainD != null && displayedRemainD != expectedRemaining) {
                calculationErrors.add("Remaining calculation error: Expected remaining ${formatNumericValue(expectedRemaining)}, displayed ${formatNumericValue(displayedRemainD)}.")
            }
        }

        // Raw & Normalized Time Fields
        val timeHours = mutableListOf<FieldInspection>()
        val timeFields = mapOf(
            "timePeriod" to (voucher.timePeriod),
            "period" to (voucher.period),
            "periodType" to (voucher.periodType),
            "usedTime" to (voucher.usedTime),
            "remainTime" to (voucher.remainTime),
            "remainingTime" to (voucher.remainingTime),
            "startTime" to (voucher.startTime),
            "activateTime" to (voucher.activateTime),
            "expiryTime" to (voucher.expiryTime),
            "expireTime" to (voucher.expireTime),
            "expiredTime" to (voucher.expiredTime),
            "expiryDate" to (voucher.expireDate),
            "endTime" to (voucher.endTime)
        )

        timeFields.forEach { (key, value) ->
            val present = value != null
            val raw = value?.toString() ?: "Not returned by Ruijie response"
            val norm = if (present) {
                val mins = parseToLong(value)
                if (mins >= 0) mins.toString() else "Unit not confirmed from Ruijie response"
            } else {
                "Not returned by Ruijie response"
            }
            timeHours.add(FieldInspection(key, key, raw, norm, if (present) "Minutes" else "N/A", present, false))
        }

        // Client/MAC Fields
        val clients = mutableListOf<FieldInspection>()
        val macFields = mapOf(
            "bindMac" to (voucher.bindMac),
            "bindingMac" to (voucher.bindingMac),
            "mac" to (voucher.mac),
            "macAddress" to (voucher.macAddress),
            "userMac" to (voucher.userMac)
        )

        macFields.forEach { (key, value) ->
            val present = value != null && value.toString().isNotBlank() && value.toString().trim() != "0" && value.toString().trim().lowercase() != "false"
            val raw = value?.toString() ?: "Not returned by Ruijie response"
            val norm = if (present) {
                val act = voucher.effectiveActualMac
                if (act != null) {
                    act
                } else if (raw == "1" || raw.lowercase() == "true" || voucher.effectiveIsBound) {
                    "Bound: Yes\nMAC address: Not returned by Ruijie response"
                } else {
                    raw
                }
            } else {
                "Not returned by Ruijie response"
            }
            clients.add(FieldInspection(key, key, raw, norm, "MAC/String", present, false))
        }

        clients.add(FieldInspection("currentClients", "currentClients", voucher.currentClients?.toString() ?: "—", voucher.currentClients?.toString() ?: "—", "Count", voucher.currentClients != null, false))
        clients.add(FieldInspection("maxClients", "maxClients", voucher.maxClients?.toString() ?: "—", voucher.maxClients?.toString() ?: "—", "Count", voucher.maxClients != null, false))
        clients.add(FieldInspection("limitClients", "limitClients", voucher.limitClients?.toString() ?: "—", voucher.limitClients?.toString() ?: "—", "Count", voucher.limitClients != null, false))

        val effectiveGid = voucher.getEffectiveProjectGroupId(selectedProject, activeAccount)
        val effectiveProjName = voucher.getEffectiveProjectName(selectedProject)

        val projects = listOf(
            FieldInspection("tenantName", "tenantName", voucher.tenantName ?: "—", voucher.tenantName ?: "—", "String", voucher.tenantName != null, false),
            FieldInspection("projectName", "projectName", effectiveProjName, effectiveProjName, "String", effectiveProjName != "—", false),
            FieldInspection("projectGroupId", "projectGroupId", effectiveGid, effectiveGid, "String", effectiveGid != "—", false)
        )

        // Diagnostic Check for Project GID
        val availableGid = selectedProject?.effectiveGidStr?.ifBlank { null }
            ?: if ((selectedProject?.effectiveId ?: 0L) > 0L) selectedProject?.effectiveId?.toString() else null
            ?: activeAccount?.selectedGroupId?.ifBlank { null }
        if (availableGid != null && availableGid != "0" && availableGid != "—" && effectiveGid == "—") {
            calculationErrors.add("Project GID mapping error: Project GID is available ($availableGid) from selected project state but displayed as —.")
        }

        val meta = InspectorRequestMeta(
            endpointPath = endpoint,
            method = "POST",
            httpStatus = httpStatus,
            ruijieCode = if (ruijieCode == 0) -1 else ruijieCode,
            safeMessage = if (ruijieCode == 0) "Success" else "Ruijie Error code $ruijieCode",
            responseWrapper = "voucherData",
            fieldsPresentCount = countPresentFields(voucher),
            durationMs = durationMs,
            sourceState = sourceState
        )

        val report = VoucherInspectionReport(
            generatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
            selectionIdentity = "UUID: ${voucher.uuid ?: voucher.id ?: "—"}, Code: ${voucher.voucherCode ?: voucher.codeNo ?: "—"}",
            detection = detection,
            identities = identities,
            packages = packages,
            statuses = statuses,
            dataGbs = dataGbs,
            timeHours = timeHours,
            clients = clients,
            projects = projects,
            requestMeta = meta,
            calculationErrors = calculationErrors,
            hasCalculationError = calculationErrors.isNotEmpty()
        )

        _lastInspectionReport.value = report
        return report
    }

    private fun parseToLong(v: Any?): Long {
        if (v == null) return -1L
        if (v is Number) return v.toLong()
        return v.toString().toLongOrNull() ?: -1L
    }

    private fun countPresentFields(v: RuijieVoucherItem): Int {
        var count = 0
        val items = listOf(
            v.uuid, v.id, v.voucherCode, v.codeNo, v.status, v.statusDesc, v.statusName, v.voucherStatus,
            v.state, v.disableStatus, v.tenantId, v.profileId, v.authprofileid, v.profile, v.packageName,
            v.profileName, v.authProfileName, v.userGroupName, v.groupName, v.userGroupId, v.groupId,
            v.projectName, v.timePeriod, v.validPeriod, v.duration, v.period, v.periodType, v.createTime,
            v.usedTime, v.startTime, v.activeTime, v.endTime, v.expiryTime, v.quota, v.dataQuota,
            v.totalTraffic, v.usedQuota, v.usedData, v.remainQuota, v.remainFlow, v.maxClients, v.limitClients,
            v.currentClients, v.bindMac, v.bindingMac, v.mac, v.macAddress, v.clientMac, v.terminalMac, v.userMac
        )
        for (it in items) {
            if (it != null && it != "null" && it != "") {
                count++
            }
        }
        return count
    }

    fun buildTxtReport(
        report: VoucherInspectionReport,
        voucher: RuijieVoucherItem? = null,
        selectedProject: com.example.data.model.RuijieProject? = null,
        activeAccount: com.example.data.model.RuijieAccount? = null
    ): String {
        val sb = StringBuilder()
        sb.appendLine("==================================================")
        sb.appendLine("RUIJIE VOUCHER DETAIL INSPECTION REPORT")
        sb.appendLine("Generated time: ${report.generatedAt}")
        sb.appendLine("App version: 1.6 (Printer V1)")
        sb.appendLine("Android version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        sb.appendLine("==================================================")
        sb.appendLine()

        val selectionMethod = "UUID & Code Match"

        val effectiveProjName = voucher?.getEffectiveProjectName(selectedProject)
            ?: report.projects.find { it.fieldName == "projectName" }?.rawValue
            ?: "—"
        val effectiveProjGid = voucher?.getEffectiveProjectGroupId(selectedProject, activeAccount)
            ?: report.projects.find { it.fieldName == "projectGroupId" }?.rawValue
            ?: "—"

        sb.appendLine("SELECTED VOUCHER")
        sb.appendLine("UUID: ${report.identities.find { it.fieldName == "uuid" }?.rawValue ?: "—"}")
        sb.appendLine("Voucher code: ${report.identities.find { it.fieldName == "voucherCode" }?.rawValue ?: "—"}")
        sb.appendLine("codeNo: ${report.identities.find { it.fieldName == "codeNo" }?.rawValue ?: "—"}")
        sb.appendLine("Selection method: $selectionMethod")
        sb.appendLine("Selected project: $effectiveProjName")
        sb.appendLine("Selected group ID: $effectiveProjGid")
        sb.appendLine()

        sb.appendLine("PACKAGE")
        sb.appendLine("Profile name: ${report.packages.find { it.fieldName == "profileName" }?.rawValue ?: "—"}")
        sb.appendLine("Package name: ${report.packages.find { it.fieldName == "packageName" }?.rawValue ?: "—"}")
        sb.appendLine("Profile ID: ${report.packages.find { it.fieldName == "profileId" }?.rawValue ?: "—"}")
        sb.appendLine("User group ID: ${report.packages.find { it.fieldName == "userGroupId" }?.rawValue ?: "—"}")
        sb.appendLine("User group name: ${report.packages.find { it.fieldName == "userGroupName" }?.rawValue ?: "—"}")
        sb.appendLine("Detected package type: ${report.detection.packageType.name}")
        sb.appendLine("Detection reason: ${report.detection.reason}")
        sb.appendLine()

        sb.appendLine("STATUS")
        sb.appendLine("Raw status: ${report.statuses.find { it.fieldName == "status" }?.rawValue ?: "—"}")
        sb.appendLine("Normalized status: ${report.statuses.find { it.fieldName == "normalizedStatus" }?.normalizedValue ?: "—"}")
        sb.appendLine("Disable status: ${report.statuses.find { it.fieldName == "disableStatus" }?.rawValue ?: "—"}")
        sb.appendLine("Status description: ${report.statuses.find { it.fieldName == "statusDesc" }?.rawValue ?: "—"}")
        sb.appendLine()

        // Data fields for DATA_GB
        val quotaRawStr = report.dataGbs.find { it.fieldName == "quota" }?.rawValue ?: report.dataGbs.find { it.fieldName == "dataQuota" }?.rawValue
        val usedRawStr = report.dataGbs.find { it.fieldName == "usedQuota" }?.rawValue ?: report.dataGbs.find { it.fieldName == "usedData" }?.rawValue

        val quotaObj = voucher?.quota ?: voucher?.dataQuota ?: quotaRawStr
        val usedObj = voucher?.usedQuota ?: voucher?.usedData ?: usedRawStr

        val totalD = parseRawNumericValue(quotaObj)
        val usedD = parseRawNumericValue(usedObj)

        val hasDataFields = totalD != null || usedD != null ||
                            (quotaRawStr != null && quotaRawStr != "Not returned by Ruijie response") ||
                            (usedRawStr != null && usedRawStr != "Not returned by Ruijie response")

        sb.appendLine("DATA/GB")
        sb.appendLine("Raw quota: ${report.dataGbs.find { it.fieldName == "quota" }?.rawValue ?: "Not returned by Ruijie response"}")
        sb.appendLine("Raw dataQuota: ${report.dataGbs.find { it.fieldName == "dataQuota" }?.rawValue ?: "Not returned by Ruijie response"}")
        sb.appendLine("Raw usedQuota: ${report.dataGbs.find { it.fieldName == "usedQuota" }?.rawValue ?: "Not returned by Ruijie response"}")
        sb.appendLine("Raw usedData: ${report.dataGbs.find { it.fieldName == "usedData" }?.rawValue ?: "Not returned by Ruijie response"}")
        if (totalD != null || usedD != null) {
            val normTotal = if (totalD != null) formatNumericValue(totalD) else "Not returned by Ruijie response"
            val normUsed = if (usedD != null) formatNumericValue(usedD) else "Not returned by Ruijie response"
            val remainVal = if (totalD != null && usedD != null) maxOf(totalD - usedD, 0.0) else totalD
            val normRemain = if (remainVal != null) formatNumericValue(remainVal) else "Not returned by Ruijie response"

            sb.appendLine("Normalized total: $normTotal")
            sb.appendLine("Normalized used: $normUsed")
            sb.appendLine("Normalized remaining: $normRemain")
            sb.appendLine("Unit: MB")
        } else if (hasDataFields) {
            sb.appendLine("Normalized total: Unit not confirmed")
            sb.appendLine("Normalized used: Unit not confirmed")
            sb.appendLine("Normalized remaining: Unit not confirmed")
            sb.appendLine("Unit: Unit not confirmed")
        } else {
            sb.appendLine("Normalized total: Not returned by Ruijie response")
            sb.appendLine("Normalized used: Not returned by Ruijie response")
            sb.appendLine("Normalized remaining: Not returned by Ruijie response")
            sb.appendLine("Unit: Unit not confirmed")
        }
        sb.appendLine("Direct or derived: Derived")

        val trafficUsedVal = report.dataGbs.find { it.fieldName == "trafficUsed" }?.rawValue 
            ?: report.dataGbs.find { it.fieldName == "usedTraffic" }?.rawValue 
            ?: report.dataGbs.find { it.fieldName == "usedFlow" }?.rawValue 
            ?: "Not returned by Ruijie response"
        sb.appendLine("Traffic used: $trafficUsedVal")

        val uploadVal = report.dataGbs.find { it.fieldName == "uploadTraffic" }?.rawValue 
            ?: report.dataGbs.find { it.fieldName == "upTraffic" }?.rawValue 
            ?: report.dataGbs.find { it.fieldName == "uploadFlow" }?.rawValue 
            ?: "Not returned by Ruijie response"
        sb.appendLine("Upload traffic: $uploadVal")

        val downloadVal = report.dataGbs.find { it.fieldName == "downloadTraffic" }?.rawValue 
            ?: report.dataGbs.find { it.fieldName == "downTraffic" }?.rawValue 
            ?: report.dataGbs.find { it.fieldName == "downloadFlow" }?.rawValue 
            ?: "Not returned by Ruijie response"
        sb.appendLine("Download traffic: $downloadVal")
        sb.appendLine()

        // Maths for HOURS_TIME
        val timePeriodVal = parseToLong(report.timeHours.find { it.fieldName == "timePeriod" }?.rawValue)
        val usedTimeVal = parseToLong(report.timeHours.find { it.fieldName == "usedTime" }?.rawValue)

        val totalTime = if (timePeriodVal >= 0) timePeriodVal else 0L
        val usedTime = if (usedTimeVal >= 0) usedTimeVal else 0L
        val remainingTime = (totalTime - usedTime).coerceAtLeast(0L)
        
        val timePeriodPresent = report.timeHours.find { it.fieldName == "timePeriod" }?.isPresent == true
        val usedTimePresent = report.timeHours.find { it.fieldName == "usedTime" }?.isPresent == true

        sb.appendLine("TIME/HOURS")
        sb.appendLine("Raw timePeriod: ${report.timeHours.find { it.fieldName == "timePeriod" }?.rawValue ?: "Not returned by Ruijie response"}")
        sb.appendLine("Raw usedTime: ${report.timeHours.find { it.fieldName == "usedTime" }?.rawValue ?: "Not returned by Ruijie response"}")
        if (timePeriodPresent || usedTimePresent) {
            sb.appendLine("Raw remaining: $remainingTime")
            sb.appendLine("Normalized total: $totalTime")
            sb.appendLine("Normalized used: $usedTime")
            sb.appendLine("Normalized remaining: $remainingTime")
        }
        sb.appendLine("Time unit: Minutes")
        sb.appendLine("Direct or derived: Derived")
        sb.appendLine()

        sb.appendLine("DATES")
        sb.appendLine("Start time: ${report.timeHours.find { it.fieldName == "startTime" }?.rawValue ?: "Not returned by Ruijie response"}")
        sb.appendLine("Activation time: ${report.timeHours.find { it.fieldName == "activateTime" }?.rawValue ?: "Not returned by Ruijie response"}")
        sb.appendLine("Expiry time: ${report.timeHours.find { it.fieldName == "expiryTime" }?.rawValue ?: "Not returned by Ruijie response"}")
        sb.appendLine()

        sb.appendLine("CLIENT")
        val isBound = voucher?.effectiveIsBound ?: run {
            val bindVal = report.clients.find { it.fieldName == "bindMac" }?.rawValue ?: report.clients.find { it.fieldName == "bindingMac" }?.rawValue
            bindVal == "1" || bindVal?.lowercase() == "true"
        }
        val bindFlag = if (isBound) "Yes" else "No"
        val actualMac = voucher?.effectiveActualMac ?: run {
            val candidate = report.clients.find { it.fieldName in listOf("mac", "macAddress", "clientMac", "terminalMac", "userMac") && it.isPresent }?.rawValue
            if (candidate != null && candidate !in listOf("0", "1", "true", "false", "yes", "no", "Not returned by Ruijie response")) {
                RuijieVoucherItem.formatMacAddress(candidate)
            } else null
        }

        val macAddressValue = if (actualMac != null) {
            actualMac
        } else if (isBound) {
            "Not returned by Ruijie response"
        } else {
            "Not bound"
        }

        sb.appendLine("Bind flag: $bindFlag")
        sb.appendLine("MAC address: $macAddressValue")
        sb.appendLine("Current clients: ${report.clients.find { it.fieldName == "currentClients" }?.rawValue ?: "—"}")
        sb.appendLine("Maximum clients: ${report.clients.find { it.fieldName == "maxClients" }?.rawValue ?: "—"}")
        sb.appendLine("Limit clients: ${report.clients.find { it.fieldName == "limitClients" }?.rawValue ?: "—"}")
        sb.appendLine()

        sb.appendLine("PROJECTS")
        sb.appendLine("Tenant name: ${report.projects.find { it.fieldName == "tenantName" }?.rawValue ?: "—"}")
        sb.appendLine("Project name: $effectiveProjName")
        sb.appendLine("Project GID: $effectiveProjGid")
        sb.appendLine("Project Group ID: $effectiveProjGid")
        sb.appendLine()

        sb.appendLine("REQUEST")
        sb.appendLine("Endpoint: ${report.requestMeta.endpointPath}")
        sb.appendLine("Method: ${report.requestMeta.method}")
        sb.appendLine("HTTP status: ${report.requestMeta.httpStatus}")
        sb.appendLine("Ruijie code: ${report.requestMeta.ruijieCode}")
        sb.appendLine("Safe message: ${report.requestMeta.safeMessage}")
        sb.appendLine("Response wrapper: ${report.requestMeta.responseWrapper}")
        sb.appendLine("Duration: ${if (report.requestMeta.durationMs > 0) "${report.requestMeta.durationMs} ms" else "Not recorded"}")
        sb.appendLine("Live/snapshot/cached: ${report.requestMeta.sourceState}")
        sb.appendLine("Refresh timestamp: ${report.generatedAt}")
        sb.appendLine()

        sb.appendLine("ERROR CHECK")
        if (report.calculationErrors.isEmpty()) {
            sb.appendLine("No errors detected.")
        } else {
            report.calculationErrors.forEach { sb.appendLine("- $it") }
        }
        sb.appendLine()

        sb.appendLine("FINAL ASSESSMENT")
        if (report.hasCalculationError) {
            sb.appendLine("Voucher detail error detected in this inspection.")
        } else {
            sb.appendLine("No voucher detail error detected in this inspection.")
        }
        sb.appendLine("==================================================")

        return sb.toString()
    }

    fun copyToClipboard(context: Context, text: String): Boolean {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Ruijie Voucher Inspection Report", text)
            clipboard.setPrimaryClip(clip)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun shareReport(context: Context, text: String) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Ruijie Voucher Inspection Report")
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(Intent.createChooser(intent, "Share Voucher Detail"))
        } catch (_: Exception) {}
    }

    fun exportReportToTxtFile(context: Context, text: String): File? {
        return try {
            val sdf = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date())
            val filename = "ruijie-voucher-detail-$sdf.txt"
            val downloadsDir = context.getExternalFilesDir("Downloads")
            val file = File(downloadsDir, filename)
            file.writeText(text)
            file
        } catch (_: Exception) {
            null
        }
    }
}
