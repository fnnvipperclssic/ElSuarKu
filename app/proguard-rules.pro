# ElSuarKu ProGuard/R8 Rules — Maximum Security Configuration
# Cloud-Based Secure E-Voting Platform

# ================================================================
# OBFUSCATION — Maximum level for production
# ================================================================
-repackageclasses 'elsuarku.internal'
-allowaccessmodification
-mergeinterfacesaggressively
-overloadaggressively
-useuniqueclassmembernames
-adaptclassstrings
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,SourceFile,LineNumberTable,*Annotation*,EnclosingMethod

# ================================================================
# Firebase — Keep all
# ================================================================
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ================================================================
# Kotlin Serialization — Data models for Firestore
# ================================================================
-keepattributes SerialDescriptor,Serializable
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class com.example.elsuarku.data.model.** {
    *** Companion;
    <fields>;
    <init>(...);
}

# ================================================================
# SECURITY — Keep encryption & integrity classes
# ================================================================
-keep class com.example.elsuarku.security.** {
    public <methods>;
    protected <methods>;
}

# Obfuscate encryption keys/constants in this package
-keep,allowobfuscation class com.example.elsuarku.security.EncryptionManager
-keep,allowobfuscation class com.example.elsuarku.security.IntegrityVerifier
-keep,allowobfuscation class com.example.elsuarku.security.AntiTampering
-keep,allowobfuscation class com.example.elsuarku.security.InputSanitizer

# String encryption for security constants
-keepclassmembers class com.example.elsuarku.security.* {
    private static final *** *KEY*;
    private static final *** *SECRET*;
    private static final *** *ALIAS*;
}

# ================================================================
# AndroidX & Framework
# ================================================================
-keep class androidx.biometric.** { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn javax.annotation.**

# ================================================================
# Compose — Keep composable functions
# ================================================================
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ================================================================
# Coil (Image Loading)
# ================================================================
-keep class coil.** { *; }
-dontwarn coil.**

# ================================================================
# Remove logging in release builds
# ================================================================
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
