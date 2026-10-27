# Network Switch ProGuard Rules

# Aggressive optimization settings
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 10
-allowaccessmodification
-dontpreverify
-overloadaggressively
-repackageclasses ''

# Strip verbose logging from release builds. Warnings and errors are kept so
# field issues (e.g. a rejected network mode) can still be diagnosed.
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int d(...);
}

# Keep line numbers for readable stack traces
-keepattributes LineNumberTable

# AIDL interfaces shared with the Shizuku / root processes
-keep class com.markvoronin.networkswitch.IRootController { *; }
-keep class com.markvoronin.networkswitch.IShizukuController { *; }

# Instantiated by class name from the Shizuku / libsu service processes
-keep class com.markvoronin.networkswitch.service.ShizukuControllerService { *; }
-keep class com.markvoronin.networkswitch.service.RootNetworkControllerService { *; }

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.modules.ApplicationContextModule
-keep class **_HiltModules$*
-keep class **_MembersInjector

# Keep libsu classes
-keep class com.topjohnwu.superuser.** { *; }

# Keep Shizuku classes
-keep class rikka.shizuku.** { *; }

# Compose
-dontwarn androidx.compose.**
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.platform.** { *; }
-keep class androidx.compose.ui.graphics.** { *; }
