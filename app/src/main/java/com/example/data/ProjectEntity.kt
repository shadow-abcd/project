package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val packageId: String,
    val versionName: String = "1.0.0",
    val versionCode: Int = 1,
    val sourceFileName: String,
    val sourceSizeBytes: Long,
    val framework: String = "React", // React, HTML5, PWA, Vue, Static
    val buildStatus: String = "BUILT", // BUILT, BUILDING, READY, WARNING
    val preflightSummary: String = "Preflight passed (6/6 checks)",
    val orientation: String = "portrait",
    val hasCustomIcon: Boolean = true,
    val permissionsCount: Int = 2,
    val permissions: String = "INTERNET, ACCESS_NETWORK_STATE",
    val apkPath: String? = "app/src/zip-to-apk.apk",
    val aabPath: String? = "app/src/zip-to-apk.aab",
    val createdAt: Long = System.currentTimeMillis(),
    val lastBuiltAt: Long? = System.currentTimeMillis()
)
