# Project specific ProGuard & R8 optimization rules for production builds

# --- Android WebKit & WebView Integration ---
# Preserve JavaScript interface annotations and methods called dynamically from WebView
-keepattributes *Annotation*,JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Preserve the native JavaScript interface bridge
-keep class com.example.AndroidAppBridge {
    public <methods>;
}

# Preserve AndroidX WebKit AssetLoader and internal handlers
-keep class androidx.webkit.** { *; }
-dontwarn androidx.webkit.**

# Preserve WebViewClient and WebChromeClient subclasses
-keepclassmembers class * extends android.webkit.WebViewClient {
    public <methods>;
}
-keepclassmembers class * extends android.webkit.WebChromeClient {
    public <methods>;
}

# --- Room Database & Persistence ---
# Preserve Room Database, DAOs, and Entities
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * {
    @androidx.room.Query *;
    @androidx.room.Insert *;
    @androidx.room.Update *;
    @androidx.room.Delete *;
}
# Keep generated Room implementations
-keep class * extends androidx.room.RoomDatabase_Impl { *; }
-keep class * extends androidx.room.EntityInsertionAdapter { *; }
-keep class * extends androidx.room.EntityDeletionOrUpdateAdapter { *; }

# --- Data Models & Diagnostic Engine ---
-keep class com.example.data.** { *; }
-keep class com.example.engine.** { *; }

# --- Retain line numbers and source file names for crash reports ---
-keepattributes SourceFile,LineNumberTable
-dontwarn okio.**
-dontwarn com.squareup.moshi.**
