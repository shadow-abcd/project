package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.PackageExportHelper
import com.example.data.ProjectEntity
import com.example.data.ProjectRepository
import com.example.engine.BuildHealthEngine
import com.example.engine.DiagnosticBundle
import com.example.engine.HealthCategory
import com.example.engine.HealthIssue
import com.example.engine.IssueSeverity
import com.example.engine.ProjectHealthAudit
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDashboardScreen(
    repository: ProjectRepository,
    onOpenBuilderWizard: () -> Unit,
    onOpenExportModal: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val projects by repository.projects.collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var selectedProjectForDetails by remember { mutableStateOf<ProjectEntity?>(null) }
    var activeHealthAudit by remember { mutableStateOf<Pair<ProjectEntity, ProjectHealthAudit>?>(null) }
    var exportDiagnosticBundle by remember { mutableStateOf<DiagnosticBundle?>(null) }
    var projectToDelete by remember { mutableStateOf<ProjectEntity?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val (name, size) = queryFileDetails(context, uri)
            scope.launch {
                val (created, audit) = repository.importNewProject(
                    fileName = name,
                    fileSizeBytes = size
                )
                if (audit.resolvedCount > 0) {
                    Toast.makeText(
                        context,
                        "Imported '${created.name}' & auto-resolved ${audit.resolvedCount} issues!",
                        Toast.LENGTH_LONG
                    ).show()
                    activeHealthAudit = Pair(created, audit)
                } else {
                    Toast.makeText(context, "Imported '${created.name}' successfully", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val filteredProjects = projects.filter { proj ->
        val matchesSearch = proj.name.contains(searchQuery, ignoreCase = true) ||
                proj.packageId.contains(searchQuery, ignoreCase = true) ||
                proj.sourceFileName.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (selectedFilter) {
            "BUILT" -> proj.buildStatus == "BUILT"
            "READY" -> proj.buildStatus == "READY"
            "HEALED" -> proj.preflightSummary.contains("healed", ignoreCase = true)
            else -> true
        }
        matchesSearch && matchesFilter
    }

    val builtCount = projects.count { it.buildStatus == "BUILT" }
    val readyCount = projects.count { it.buildStatus == "READY" }
    val healedCount = projects.count { it.preflightSummary.contains("healed", ignoreCase = true) }
    val totalSizeMb = projects.sumOf { it.sourceSizeBytes } / (1024.0 * 1024.0)

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("dashboard_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary Banner
            item {
                DashboardHeaderCard(
                    totalProjects = projects.size,
                    builtCount = builtCount,
                    healedCount = healedCount,
                    totalSizeMb = totalSizeMb,
                    onOpenExportModal = onOpenExportModal
                )
            }

            // Search and Filter Bar
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_projects_input"),
                        placeholder = { Text("Search imported projects or package ID...") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedFilter == "ALL",
                            onClick = { selectedFilter = "ALL" },
                            label = { Text("All (${projects.size})") }
                        )
                        FilterChip(
                            selected = selectedFilter == "BUILT",
                            onClick = { selectedFilter = "BUILT" },
                            label = { Text("Built ($builtCount)") }
                        )
                        FilterChip(
                            selected = selectedFilter == "HEALED",
                            onClick = { selectedFilter = "HEALED" },
                            label = { Text("Auto-Healed ($healedCount)") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }
            }

            // Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Recently Imported Projects",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${filteredProjects.size} items",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Projects List
            if (filteredProjects.isEmpty()) {
                item {
                    EmptyProjectsCard(
                        hasQuery = searchQuery.isNotEmpty(),
                        onImportClick = {
                            filePickerLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "*/*"))
                        }
                    )
                }
            } else {
                items(filteredProjects, key = { it.id }) { project ->
                    ProjectItemCard(
                        project = project,
                        onBuildClick = {
                            repository.startBuild(project, scope)
                            Toast.makeText(context, "Scanning & compiling '${project.name}' APK & AAB...", Toast.LENGTH_SHORT).show()
                        },
                        onInstallClick = {
                            val apk = File("/app/applet/app/src/zip-to-apk.apk")
                            if (apk.exists()) {
                                PackageExportHelper.installApk(context, apk)
                            } else {
                                Toast.makeText(context, "APK ready in app/src/zip-to-apk.apk", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onShareClick = {
                            val apk = File("/app/applet/app/src/zip-to-apk.apk")
                            if (apk.exists()) {
                                PackageExportHelper.shareFile(context, apk, "application/vnd.android.package-archive")
                            } else {
                                PackageExportHelper.copyToClipboard(context, project.name, "app/src/zip-to-apk.apk")
                            }
                        },
                        onHealthAuditClick = {
                            scope.launch {
                                val pair = repository.runHealthAudit(project)
                                activeHealthAudit = pair
                            }
                        },
                        onDetailsClick = {
                            selectedProjectForDetails = project
                        },
                        onDeleteClick = {
                            projectToDelete = project
                        }
                    )
                }
            }
        }

        // FAB to import new ZIP
        FloatingActionButton(
            onClick = {
                filePickerLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "*/*"))
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("fab_import_zip"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Import ZIP")
                Text(text = "Import ZIP", fontWeight = FontWeight.SemiBold)
            }
        }
    }

    // Health & Resolution Report Modal
    activeHealthAudit?.let { (project, audit) ->
        BuildHealthReportDialog(
            project = project,
            audit = audit,
            onDismiss = { activeHealthAudit = null },
            onExportDiagnostic = {
                val bundle = BuildHealthEngine.generateDiagnosticBundle(project, audit)
                exportDiagnosticBundle = bundle
            },
            onReScan = {
                scope.launch {
                    val pair = repository.runHealthAudit(project)
                    activeHealthAudit = pair
                    Toast.makeText(context, "Diagnostics updated", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // Diagnostic Bundle Export Modal
    exportDiagnosticBundle?.let { bundle ->
        DiagnosticBundleDialog(
            bundle = bundle,
            onDismiss = { exportDiagnosticBundle = null }
        )
    }

    // Project Details Dialog
    selectedProjectForDetails?.let { proj ->
        ProjectDetailsDialog(
            project = proj,
            onDismiss = { selectedProjectForDetails = null },
            onStartBuild = {
                repository.startBuild(proj, scope)
                selectedProjectForDetails = null
                Toast.makeText(context, "Building packages...", Toast.LENGTH_SHORT).show()
            },
            onRunHealthCheck = {
                scope.launch {
                    val pair = repository.runHealthAudit(proj)
                    selectedProjectForDetails = null
                    activeHealthAudit = pair
                }
            }
        )
    }

    // Delete Confirmation Dialog
    projectToDelete?.let { proj ->
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            title = { Text("Delete Project?") },
            text = { Text("Are you sure you want to remove '${proj.name}' from your dashboard? The original file won't be deleted.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch { repository.deleteProject(proj) }
                        projectToDelete = null
                        Toast.makeText(context, "Project removed", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { projectToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DashboardHeaderCard(
    totalProjects: Int,
    builtCount: Int,
    healedCount: Int,
    totalSizeMb: Double,
    onOpenExportModal: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Build Pipeline & Diagnostics",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Automated preflight self-healing & package integrity",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.clickable { onOpenExportModal() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Exports",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "APK & AAB",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem(label = "Total Projects", value = "$totalProjects")
                MetricItem(label = "Auto-Healed", value = "$healedCount", color = Color(0xFF3B82F6))
                MetricItem(label = "Signed Builds", value = "$builtCount", color = Color(0xFF10B981))
                MetricItem(label = "Total Storage", value = String.format(Locale.US, "%.1f MB", totalSizeMb))
            }

            HorizontalDivider()

            val context = LocalContext.current
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.GITHUB_REPO_URL)).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Unable to open link: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(vertical = 4.dp)
                            .testTag("dashboard_github_repo_row"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = "GitHub Repository",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Column {
                                Text(
                                    text = "GitHub Source Repository",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = BuildConfig.GITHUB_REPO_URL.removePrefix("https://github.com/"),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    PackageExportHelper.copyToClipboard(context, "GitHub Repository", BuildConfig.GITHUB_REPO_URL)
                                },
                                modifier = Modifier.size(48.dp).testTag("copy_github_url_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy GitHub URL",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Open GitHub Repository",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                PackageExportHelper.copyToClipboard(context, "Worker URL", BuildConfig.WORKER_URL)
                            }
                            .padding(vertical = 4.dp)
                            .testTag("dashboard_worker_url_row"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = "Worker URL",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(16.dp)
                            )
                            Column {
                                Text(
                                    text = "Cloudflare Build Worker",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = BuildConfig.WORKER_URL,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                PackageExportHelper.copyToClipboard(context, "Worker URL", BuildConfig.WORKER_URL)
                            },
                            modifier = Modifier.size(48.dp).testTag("copy_worker_url_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Worker URL",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricItem(label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ProjectItemCard(
    project: ProjectEntity,
    onBuildClick: () -> Unit,
    onInstallClick: () -> Unit,
    onShareClick: () -> Unit,
    onHealthAuditClick: () -> Unit,
    onDetailsClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val frameworkColor = when (project.framework) {
        "React" -> Color(0xFF0284C7)
        "HTML5" -> Color(0xFF8B5CF6)
        "PWA" -> Color(0xFF4F46E5)
        else -> Color(0xFFD97706)
    }

    val isHealed = project.preflightSummary.contains("healed", ignoreCase = true)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDetailsClick() },
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Framework Badge + Title + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = frameworkColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (project.framework == "HTML5") Icons.Default.FolderZip else Icons.Default.Android,
                                contentDescription = project.framework,
                                tint = frameworkColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = project.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = project.packageId,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                StatusPill(status = project.buildStatus)
            }

            // Preflight / File Info Row
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Source: ${project.sourceFileName} (${formatBytes(project.sourceSizeBytes)})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "v${project.versionName} (${project.versionCode})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Health Status & Self-Healing Badge Row
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isHealed) Color(0xFF3B82F6).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onHealthAuditClick() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (isHealed) Icons.Default.AutoFixHigh else Icons.Default.CheckCircle,
                            contentDescription = "Health Status",
                            tint = if (isHealed) Color(0xFF3B82F6) else Color(0xFF10B981),
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = project.preflightSummary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isHealed) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isHealed) Color(0xFF1D4ED8) else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Report",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Open Report",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            HorizontalDivider()

            // Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDate(project.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (project.buildStatus == "BUILT") {
                        FilledTonalButton(
                            onClick = onInstallClick,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(imageVector = Icons.Default.InstallMobile, contentDescription = "Install", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Install APK", fontSize = 12.sp)
                        }
                        IconButton(onClick = onShareClick, modifier = Modifier.size(34.dp)) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                        }
                    } else if (project.buildStatus == "BUILDING") {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Button(
                            onClick = onBuildClick,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Build", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Build Packages", fontSize = 12.sp)
                        }
                    }

                    IconButton(onClick = onHealthAuditClick, modifier = Modifier.size(34.dp)) {
                        Icon(imageVector = Icons.Default.Healing, contentDescription = "Diagnostics", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(34.dp)) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun StatusPill(status: String) {
    val (bgColor, textColor, text, icon) = when (status) {
        "BUILT" -> Quad(Color(0xFF10B981).copy(alpha = 0.15f), Color(0xFF10B981), "Built (APK/AAB)", Icons.Default.CheckCircle)
        "BUILDING" -> Quad(Color(0xFF3B82F6).copy(alpha = 0.15f), Color(0xFF3B82F6), "Compiling...", Icons.Default.HourglassTop)
        "WARNING" -> Quad(Color(0xFFF59E0B).copy(alpha = 0.15f), Color(0xFFF59E0B), "Warning", Icons.Default.Warning)
        else -> Quad(Color(0xFF8B5CF6).copy(alpha = 0.15f), Color(0xFF8B5CF6), "Ready to Build", Icons.Default.PlayArrow)
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = textColor, modifier = Modifier.size(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun BuildHealthReportDialog(
    project: ProjectEntity,
    audit: ProjectHealthAudit,
    onDismiss: () -> Unit,
    onExportDiagnostic: () -> Unit,
    onReScan: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (audit.resolvedCount > 0) Color(0xFF3B82F6).copy(alpha = 0.2f) else Color(0xFF10B981).copy(alpha = 0.2f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (audit.resolvedCount > 0) Icons.Default.AutoFixHigh else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (audit.resolvedCount > 0) Color(0xFF3B82F6) else Color(0xFF10B981),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Build Health & Resolution",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = project.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Summary Status Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Engine Status: ${audit.overallStatus}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (audit.resolvedCount > 0) Color(0xFF1D4ED8) else Color(0xFF047857)
                            )
                            Text(
                                text = "${audit.resolvedCount} issues auto-healed • ${audit.totalChecks} checks verified",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Text(
                    text = "Resolution Audit Log:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                // List of Issues & Resolutions
                audit.issues.forEach { issue ->
                    HealthIssueItem(issue = issue)
                }
            }
        },
        confirmButton = {
            Button(onClick = onExportDiagnostic) {
                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Diagnostic Bundle")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onReScan) {
                    Text("Re-Scan")
                }
                OutlinedButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}

@Composable
fun HealthIssueItem(issue: HealthIssue) {
    val (statusColor, statusLabel) = when (issue.severity) {
        IssueSeverity.RESOLVED_AUTO -> Pair(Color(0xFF3B82F6), "Auto-Resolved ✓")
        IssueSeverity.CRITICAL_BLOCKER -> Pair(Color(0xFFEF4444), "Critical Blocker")
        IssueSeverity.WARNING -> Pair(Color(0xFFF59E0B), "Warning")
        IssueSeverity.INFO -> Pair(Color(0xFF10B981), "Verified ✓")
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = issue.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = issue.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (issue.isAutoResolved && issue.originalValue != null && issue.resolvedValue != null) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Original: ${issue.originalValue}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFEF4444)
                        )
                        Text(
                            text = "Auto-Fixed: ${issue.resolvedValue}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }
                }
            }

            issue.remediationAction?.let { action ->
                Text(
                    text = "Safety Action: $action",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun DiagnosticBundleDialog(
    bundle: DiagnosticBundle,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Diagnostic Error Bundle")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Bundle ID: ${bundle.bundleId}",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Contains full diagnostic telemetry, archive structure dump, and automated audit logs for developer debugging.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = bundle.rawJson,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    PackageExportHelper.copyToClipboard(context, bundle.bundleId, bundle.rawJson)
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Diagnostic Bundle ${bundle.bundleId}")
                        putExtra(Intent.EXTRA_TEXT, bundle.rawJson)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Export Diagnostic Bundle"))
                }
            ) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copy & Share")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun EmptyProjectsCard(hasQuery: Boolean, onImportClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.FolderZip,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Text(
                text = if (hasQuery) "No matching web projects found" else "No imported projects yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = if (hasQuery) "Try searching by another title or package ID" else "Import any zipped HTML5 website, game, or PWA archive to inspect and generate signed Android APK & AAB packages.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Button(
                onClick = onImportClick,
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Import")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Import Web ZIP")
            }
        }
    }
}

@Composable
fun ProjectDetailsDialog(
    project: ProjectEntity,
    onDismiss: () -> Unit,
    onStartBuild: () -> Unit,
    onRunHealthCheck: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Android, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(text = project.name, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DetailRow("Package ID", project.packageId)
                DetailRow("Version", "${project.versionName} (${project.versionCode})")
                DetailRow("Framework", project.framework)
                DetailRow("Source File", project.sourceFileName)
                DetailRow("Source Size", formatBytes(project.sourceSizeBytes))
                DetailRow("Permissions", project.permissions)
                DetailRow("Orientation", project.orientation.replaceFirstChar { it.uppercase() })
                DetailRow("Health Status", project.preflightSummary)

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text(
                    text = "Build Outputs:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "• Signed APK: ${project.apkPath ?: "Pending build"}\n• Signed AAB: ${project.aabPath ?: "Pending build"}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRunHealthCheck) {
                    Icon(imageVector = Icons.Default.Healing, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Health Check")
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
        bytes >= 1024 -> String.format(Locale.US, "%.0f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}

fun formatDate(timeMillis: Long): String {
    val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return formatter.format(Date(timeMillis))
}

fun queryFileDetails(context: Context, uri: Uri): Pair<String, Long> {
    var name = "imported-web-app.zip"
    var size = 1024L * 500L
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex != -1) name = cursor.getString(nameIndex) ?: name
                if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
            }
        }
    } catch (_: Exception) {}
    return Pair(name, size)
}
