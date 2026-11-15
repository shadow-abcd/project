package com.example

import com.example.data.ProjectEntity
import com.example.engine.BuildHealthEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BuildHealthEngineTest {

    @Test
    fun `sanitizePackageId cleans invalid characters and reserved keywords`() {
        val input = "My-App.class.123_game"
        val output = BuildHealthEngine.sanitizePackageId(input)
        assertEquals("com.my_app.app_class.pkg_123_game", output)
    }

    @Test
    fun `auditAndHeal repairs invalid package and clamps legacy storage permissions`() {
        val testProject = ProjectEntity(
            id = "test-1",
            name = "Test Web Game",
            packageId = "Invalid-Package-Name",
            versionName = "1.0",
            versionCode = 1,
            sourceFileName = "game.zip",
            sourceSizeBytes = 2048L,
            framework = "Static",
            buildStatus = "WARNING",
            preflightSummary = "Needs check",
            hasCustomIcon = false,
            permissions = "INTERNET, WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE"
        )

        val (healed, audit) = BuildHealthEngine.auditAndHeal(testProject)

        // Verify package ID auto-healed
        assertEquals("com.invalid_package_name", healed.packageId)

        // Verify permissions policy clamped
        assertTrue(healed.permissions.contains("maxSdk=28"))
        assertTrue(healed.permissions.contains("maxSdk=32"))

        // Verify custom icon fallback assigned
        assertTrue(healed.hasCustomIcon)

        // Verify audit counts
        assertTrue(audit.resolvedCount >= 3)
        assertEquals("AUTO_HEALED", audit.overallStatus)
    }

    @Test
    fun `generateDiagnosticBundle produces valid JSON structure`() {
        val testProject = ProjectEntity(
            id = "test-2",
            name = "Corrupted Project",
            packageId = "com.test.corrupted",
            sourceFileName = "corrupt.zip",
            sourceSizeBytes = 0L
        )

        val (healed, audit) = BuildHealthEngine.auditAndHeal(testProject)
        val bundle = BuildHealthEngine.generateDiagnosticBundle(healed, audit)

        assertNotNull(bundle.bundleId)
        assertTrue(bundle.bundleId.startsWith("DIAG-"))
        assertEquals("BLOCKED", bundle.overallStatus)
        assertTrue(bundle.rawJson.contains("diagnosticBundleId"))
        assertTrue(bundle.rawJson.contains("Corrupted or Zero-Byte Archive"))
    }
}
