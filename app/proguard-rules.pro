# Optimization configuration
-repackageclasses ''
-allowaccessmodification

# Android Activity classes
-keep public class * extends android.app.Activity
-keep public class com.example.MainActivity { *; }

# WebView JavaScript bridge (Ruijie official SSO & session bridge)
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.example.data.api.RuijieWebViewBridge$BridgeJsInterface {
    public *;
}

# Retrofit interfaces & annotations
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keep interface com.example.data.api.RuijieApiService { *; }

# Moshi JSON models and adapters
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keep @com.squareup.moshi.JsonClass class * { *; }
-keep class *JsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
    public <init>(com.squareup.moshi.Moshi, java.lang.reflect.Type[]);
    public <init>();
}
-keep class com.example.data.model.** { *; }

# Room Database entities, DAOs, and database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Entity class * { *; }
-keep class * extends androidx.room.migration.Migration
-keep class com.example.data.db.** { *; }

# Jetpack Compose runtime
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# Bluetooth Thermal Printer and ESC/POS rendering
-keep class com.example.printer.** { *; }

# Diagnostic models & records
-keep class com.example.data.diagnostics.** { *; }

# OkHttp optional security provider warnings
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
-dontwarn javax.annotation.**

# Kotlin metadata warnings on newer Kotlin versions
-dontwarn kotlin.Metadata
