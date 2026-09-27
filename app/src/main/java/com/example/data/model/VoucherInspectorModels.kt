package com.example.data.model

enum class VoucherPackageType(val displayName: String) {
    GB_DATA("GB/DATA"),
    HOURS_TIME("HOURS/TIME"),
    BOTH("BOTH"),
    UNKNOWN("UNKNOWN")
}

data class PackageTypeDetectionResult(
    val packageType: VoucherPackageType,
    val reason: String,
    val profileName: String,
    val profileId: String
)

data class FieldInspection(
    val fieldName: String,
    val sourceKey: String,
    val rawValue: String,
    val normalizedValue: String,
    val unit: String,
    val isPresent: Boolean,
    val isDerived: Boolean
)

data class VoucherInspectionReport(
    val generatedAt: String,
    val selectionIdentity: String,
    val detection: PackageTypeDetectionResult,
    val identities: List<FieldInspection>,
    val packages: List<FieldInspection>,
    val statuses: List<FieldInspection>,
    val dataGbs: List<FieldInspection>,
    val timeHours: List<FieldInspection>,
    val clients: List<FieldInspection>,
    val projects: List<FieldInspection>,
    val requestMeta: InspectorRequestMeta,
    val calculationErrors: List<String> = emptyList(),
    val hasCalculationError: Boolean = false
)

data class InspectorRequestMeta(
    val endpointPath: String,
    val method: String,
    val httpStatus: Int,
    val ruijieCode: Int,
    val safeMessage: String,
    val responseWrapper: String,
    val fieldsPresentCount: Int,
    val durationMs: Long,
    val sourceState: String // "live response", "list snapshot", "cached", "unavailable"
)
