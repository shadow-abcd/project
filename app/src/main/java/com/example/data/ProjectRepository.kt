package com.example.data

import com.example.engine.BuildHealthEngine
import com.example.engine.ProjectHealthAudit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.UUID

class ProjectRepository(private val dao: ProjectDao) {

    val projects: Flow<List<ProjectEntity>> = dao.getAllProjects()

    suspend fun initializeDefaultsIfNeeded() {
        if (dao.count() == 0) {
            val defaults = listOf(
                ProjectEntity(
                    id = "proj-1",
                    name = "Zip to APK Suite",
                    packageId = "com.aistudio.ziptoapk.apxp",
                    versionName = "1.0.2",
                    versionCode = 3,
                    sourceFileName = "zip-to-apk-rebuilt_8cfcc3f5.zip",
                    sourceSizeBytes = 603920L,
                    framework = "React",
                    buildStatus = "BUILT",
                    preflightSummary = "Verified (6/6 checks passed)",
                    orientation = "sensor",
                    hasCustomIcon = true,
                    permissionsCount = 3,
                    permissions = "INTERNET, ACCESS_NETWORK_STATE, VIBRATE",
                    apkPath = "app/src/zip-to-apk-release.apk",
                    aabPath = "app/src/zip-to-apk-release.aab",
                    createdAt = System.currentTimeMillis() - 3600000L * 4,
                    lastBuiltAt = System.currentTimeMillis() - 3600000L * 2
                ),
                ProjectEntity(
                    id = "proj-2",
                    name = "Neon Canvas Arcade",
                    packageId = "com.arcade.neon.space",
                    versionName = "1.2.4",
                    versionCode = 12,
                    sourceFileName = "neon-space-game-web.zip",
                    sourceSizeBytes = 4320100L,
                    framework = "HTML5",
                    buildStatus = "BUILT",
                    preflightSummary = "Auto-healed (2 fixes: orientation lock & audio permissions)",
                    orientation = "landscape",
                    hasCustomIcon = true,
                    permissionsCount = 2,
                    permissions = "INTERNET, VIBRATE",
                    apkPath = "app/src/zip-to-apk.apk",
                    aabPath = "app/src/zip-to-apk.aab",
                    createdAt = System.currentTimeMillis() - 86400000L * 2,
                    lastBuiltAt = System.currentTimeMillis() - 86400000L * 1
                ),
                ProjectEntity(
                    id = "proj-3",
                    name = "Storefront PWA Commerce",
                    packageId = "com.shop.storefront.app",
                    versionName = "2.0.1",
                    versionCode = 21,
                    sourceFileName = "storefront-pwa-release.zip",
                    sourceSizeBytes = 2840500L,
                    framework = "PWA",
                    buildStatus = "READY",
                    preflightSummary = "Verified (Manifest and service worker detected)",
                    orientation = "portrait",
                    hasCustomIcon = true,
                    permissionsCount = 3,
                    permissions = "INTERNET, ACCESS_NETWORK_STATE, POST_NOTIFICATIONS",
                    apkPath = null,
                    aabPath = null,
                    createdAt = System.currentTimeMillis() - 86400000L * 3,
                    lastBuiltAt = null
                ),
                ProjectEntity(
                    id = "proj-4",
                    name = "Offline Markdown Writer",
                    packageId = "com.writer.markdown.simple",
                    versionName = "0.9.5",
                    versionCode = 9,
                    sourceFileName = "markdown-editor.zip",
                    sourceSizeBytes = 912400L,
                    framework = "Static",
                    buildStatus = "READY",
                    preflightSummary = "Auto-healed (3 fixes: viewport injected & adaptive icon)",
                    orientation = "portrait",
                    hasCustomIcon = true,
                    permissionsCount = 1,
                    permissions = "INTERNET",
                    apkPath = null,
                    aabPath = null,
                    createdAt = System.currentTimeMillis() - 86400000L * 5,
                    lastBuiltAt = null
                )
            )
            dao.insertAll(defaults)
        }
    }

    suspend fun importNewProject(
        fileName: String,
        fileSizeBytes: Long,
        customName: String? = null
    ): Pair<ProjectEntity, ProjectHealthAudit> {
        val baseName = customName?.takeIf { it.isNotBlank() }
            ?: fileName.removeSuffix(".zip").replace(Regex("[-_]"), " ").split(" ")
                .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }

        val cleanPackage = "com.app." + baseName.lowercase().replace(Regex("[^a-z0-9]"), "")

        val frameworkGuess = when {
            fileName.contains("pwa", ignoreCase = true) -> "PWA"
            fileName.contains("game", ignoreCase = true) || fileName.contains("arcade", ignoreCase = true) -> "HTML5"
            fileName.contains("react", ignoreCase = true) || fileName.contains("app", ignoreCase = true) -> "React"
            else -> "Static"
        }

        val rawProject = ProjectEntity(
            id = UUID.randomUUID().toString(),
            name = baseName.ifBlank { "Imported Web App" },
            packageId = cleanPackage.ifBlank { "com.web.app" },
            versionName = "1.0.0",
            versionCode = 1,
            sourceFileName = fileName,
            sourceSizeBytes = fileSizeBytes,
            framework = frameworkGuess,
            buildStatus = "READY",
            preflightSummary = "Inspecting...",
            orientation = "portrait",
            hasCustomIcon = true,
            permissionsCount = 2,
            permissions = "INTERNET, ACCESS_NETWORK_STATE",
            apkPath = null,
            aabPath = null,
            createdAt = System.currentTimeMillis(),
            lastBuiltAt = null
        )

        // Run automated error detection & self-healing
        val (healedProject, audit) = BuildHealthEngine.auditAndHeal(rawProject)
        dao.insertProject(healedProject)
        return Pair(healedProject, audit)
    }

    suspend fun runHealthAudit(project: ProjectEntity): Pair<ProjectEntity, ProjectHealthAudit> {
        val (healed, audit) = BuildHealthEngine.auditAndHeal(project)
        dao.updateProject(healed)
        return Pair(healed, audit)
    }

    suspend fun deleteProject(project: ProjectEntity) {
        dao.deleteProject(project)
    }

    fun startBuild(project: ProjectEntity, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            val (healed, _) = BuildHealthEngine.auditAndHeal(project)
            dao.updateProject(healed.copy(buildStatus = "BUILDING"))
            delay(2500)
            dao.updateProject(
                healed.copy(
                    buildStatus = "BUILT",
                    apkPath = "app/src/zip-to-apk.apk",
                    aabPath = "app/src/zip-to-apk.aab",
                    lastBuiltAt = System.currentTimeMillis()
                )
            )
        }
    }
}
