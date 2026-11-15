package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

data class PackageArtifact(
    val name: String,
    val type: String, // "APK" or "AAB"
    val purpose: String,
    val relativePath: String,
    val targetPath: String,
    val sizeBytes: Long,
    val isSigned: Boolean,
    val isRelease: Boolean,
    val file: File?
)

object PackageExportHelper {

    fun getArtifacts(context: Context): List<PackageArtifact> {
        val appSrcDir = File("/app/applet/app/src")
        val fallbackSrcDir = File(context.filesDir, "src")

        val releaseApk = listOf(
            File(appSrcDir, "zip-to-apk-release.apk"),
            File("/app/applet/app/build/outputs/apk/release/app-release.apk"),
            File(appSrcDir, "zip-to-apk.apk"),
            File(fallbackSrcDir, "zip-to-apk-release.apk")
        ).firstOrNull { it.exists() && it.length() > 0 }

        val releaseAab = listOf(
            File(appSrcDir, "zip-to-apk-release.aab"),
            File("/app/applet/app/build/outputs/bundle/release/app-release.aab"),
            File(appSrcDir, "zip-to-apk.aab"),
            File(fallbackSrcDir, "zip-to-apk-release.aab")
        ).firstOrNull { it.exists() && it.length() > 0 }

        val testApk = listOf(
            File(appSrcDir, "zip-to-apk.apk"),
            File(appSrcDir, "app-debug.apk"),
            File("/app/applet/app/build/outputs/apk/debug/app-debug.apk")
        ).firstOrNull { it.exists() && it.length() > 0 }

        val artifacts = mutableListOf<PackageArtifact>()

        releaseApk?.let { file ->
            artifacts.add(
                PackageArtifact(
                    name = "zip-to-apk-release.apk",
                    type = "APK",
                    purpose = "Optimized release APK for testing & sideloading",
                    relativePath = "app/src/zip-to-apk-release.apk",
                    targetPath = file.absolutePath,
                    sizeBytes = file.length(),
                    isSigned = true,
                    isRelease = true,
                    file = file
                )
            )
        }

        releaseAab?.let { file ->
            artifacts.add(
                PackageArtifact(
                    name = "zip-to-apk-release.aab",
                    type = "AAB",
                    purpose = "Mandatory package for Google Play Console upload",
                    relativePath = "app/src/zip-to-apk-release.aab",
                    targetPath = file.absolutePath,
                    sizeBytes = file.length(),
                    isSigned = true,
                    isRelease = true,
                    file = file
                )
            )
        }

        if (artifacts.isEmpty()) {
            testApk?.let { file ->
                artifacts.add(
                    PackageArtifact(
                        name = "zip-to-apk.apk",
                        type = "APK",
                        purpose = "Signed APK for mobile testing",
                        relativePath = "app/src/zip-to-apk.apk",
                        targetPath = file.absolutePath,
                        sizeBytes = file.length(),
                        isSigned = true,
                        isRelease = false,
                        file = file
                    )
                )
            }
        }

        return artifacts
    }

    fun installApk(context: Context, file: File) {
        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open installer: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun shareFile(context: Context, file: File, mimeType: String) {
        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, contentUri)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(Intent.createChooser(intent, "Export Package"))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not share file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied: $text", Toast.LENGTH_SHORT).show()
    }
}
