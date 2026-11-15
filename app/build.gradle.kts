import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.io.File

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.ziptoapk.apxp"
    minSdk = 24
    targetSdk = 36
    versionCode = 3
    versionName = "1.0.2"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    buildConfigField("String", "WORKER_URL", "\"https://pixel.shadow012399.workers.dev\"")
    buildConfigField("String", "GITHUB_REPO_URL", "\"https://github.com/shadow-abcd/Zip-html-to-apk-converter\"")
  }

  // Do not remove or modify these signingConfigs. They are necessary for building, installing,
  // and updating Android apps in AI Studio.
  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      // Do not remove or modify this signingConfig assignment. It is necessary for Android apps in
      // AI Studio.
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      // Do not remove or modify this signingConfig assignment. It is necessary for Android apps in
      // AI Studio.
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.webkit)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  // implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  // Uncomment to use Firestore:
  // implementation(libs.firebase.firestore)

  // Uncomment ALL FOUR of the following dependencies together to use Firebase Auth and Google
  // Sign-In via Credential Manager:
  // implementation(libs.firebase.auth)
  // implementation(libs.androidx.credentials)
  // implementation(libs.androidx.credentials.play.services)
  // implementation(libs.googleid)
  implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.firebase.appcheck.debug)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}

abstract class EnsureReleaseKeystoreTask : DefaultTask() {
  @get:Input
  abstract val keystorePath: Property<String>

  @TaskAction
  fun execute() {
    val path = keystorePath.get()
    val keystoreFile = File(path)
    if (!keystoreFile.exists()) {
      println("Release keystore not found at ${keystoreFile.absolutePath}. Generating 2048-bit RSA JKS...")
      keystoreFile.parentFile?.mkdirs()
      val cmd = listOf(
        "keytool", "-genkeypair", "-v",
        "-keystore", keystoreFile.absolutePath,
        "-alias", "upload",
        "-keyalg", "RSA",
        "-keysize", "2048",
        "-validity", "10000",
        "-storepass", "android",
        "-keypass", "android",
        "-dname", "CN=ZipToAPK, OU=Engineering, O=ZipToAPK, L=MountainView, ST=California, C=US"
      )
      val process = ProcessBuilder(cmd).redirectErrorStream(true).start()
      val output = process.inputStream.bufferedReader().readText()
      val exitCode = process.waitFor()
      if (exitCode != 0) {
        throw GradleException("Failed to generate release keystore (exit code $exitCode): $output")
      }
      println("Release keystore generated successfully at ${keystoreFile.absolutePath}")
    } else {
      println("Release keystore verified at ${keystoreFile.absolutePath}")
    }
  }
}

tasks.register<EnsureReleaseKeystoreTask>("ensureReleaseKeystore") {
  description = "Checks for the existence of the release keystore and generates a 2048-bit RSA JKS file if missing."
  group = "signing"
  keystorePath.set(System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks")
}

abstract class ExportReleaseArtifactsTask : DefaultTask() {
  @get:Input
  abstract val apkSourcePath: Property<String>

  @get:Input
  abstract val aabSourcePath: Property<String>

  @get:Input
  abstract val destinationDirs: ListProperty<String>

  @TaskAction
  fun execute() {
    val dests = destinationDirs.get().map { File(it) }
    val artifacts = listOf(
      File(apkSourcePath.get()) to listOf("zip-to-apk-release.apk", "app-release.apk"),
      File(aabSourcePath.get()) to listOf("zip-to-apk-release.aab", "app-release.aab")
    )

    var copiedCount = 0
    for ((sourceFile, targetNames) in artifacts) {
      if (sourceFile.exists() && sourceFile.length() > 0L) {
        for (dir in dests) {
          if (!dir.exists()) dir.mkdirs()
          for (targetName in targetNames) {
            val targetFile = File(dir, targetName)
            sourceFile.copyTo(targetFile, overwrite = true)
            println("Exported ${targetFile.absolutePath} (${targetFile.length()} bytes)")
            copiedCount++
          }
        }
      }
    }

    if (copiedCount == 0) {
      println("No release APK or AAB outputs found to export yet.")
    }
  }
}

tasks.register<ExportReleaseArtifactsTask>("exportReleaseArtifacts") {
  description = "Copies signed release APK and AAB files (zip-to-apk-release and app-release) to the project root, src, and app/src directories."
  group = "distribution"
  apkSourcePath.set(layout.buildDirectory.file("outputs/apk/release/app-release.apk").map { it.asFile.absolutePath })
  aabSourcePath.set(layout.buildDirectory.file("outputs/bundle/release/app-release.aab").map { it.asFile.absolutePath })
  destinationDirs.set(
    listOf(
      rootProject.layout.projectDirectory.asFile.absolutePath,
      rootProject.layout.projectDirectory.dir("src").asFile.absolutePath,
      layout.projectDirectory.dir("src").asFile.absolutePath
    )
  )
  outputs.upToDateWhen { false }
}

tasks.register("copyReleaseOutputs") {
  description = "Convenience alias that triggers exportReleaseArtifacts to copy release APK and AAB outputs."
  group = "distribution"
  dependsOn("exportReleaseArtifacts")
}

tasks.matching { it.name in listOf("preReleaseBuild", "validateSigningRelease") }.configureEach {
  dependsOn("ensureReleaseKeystore")
}

tasks.matching { it.name in listOf("assembleRelease", "bundleRelease") }.configureEach {
  finalizedBy("exportReleaseArtifacts")
}

