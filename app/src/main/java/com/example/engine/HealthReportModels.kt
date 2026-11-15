package com.example.engine

enum class HealthCategory(val label: String) {
    ARCHIVE_STRUCTURE("Archive Structure & Entrypoint"),
    PACKAGE_IDENTITY("Package ID & App Identity"),
    MANIFEST_SECURITY("Manifest & Play Store Policy"),
    WEB_RUNTIME("Viewport & Mobile Web Runtime"),
    ICON_ASSETS("Launcher Icons & Display Assets")
}

enum class IssueSeverity {
    INFO,
    RESOLVED_AUTO,
    WARNING,
    CRITICAL_BLOCKER
}

data class HealthIssue(
    val id: String,
    val category: HealthCategory,
    val severity: IssueSeverity,
    val title: String,
    val description: String,
    val originalValue: String? = null,
    val resolvedValue: String? = null,
    val remediationAction: String? = null,
    val isAutoResolved: Boolean = false
)

data class ProjectHealthAudit(
    val projectId: String,
    val overallStatus: String, // "VERIFIED", "AUTO_HEALED", "WARNING", "BLOCKED"
    val totalChecks: Int,
    val issues: List<HealthIssue>,
    val resolvedCount: Int,
    val warningCount: Int,
    val blockerCount: Int,
    val timestamp: Long = System.currentTimeMillis()
)

data class DiagnosticBundle(
    val bundleId: String,
    val projectName: String,
    val packageId: String,
    val generatedAt: String,
    val appVersion: String,
    val targetSdk: Int,
    val overallStatus: String,
    val sourceFile: String,
    val sourceSizeBytes: Long,
    val auditSummary: String,
    val issues: List<HealthIssue>,
    val systemEnvironment: Map<String, String>,
    val rawJson: String
)
