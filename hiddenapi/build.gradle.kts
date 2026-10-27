plugins {
    id("com.android.library")
}

// Compile-time stubs for hidden framework APIs (android.os.ServiceManager).
// Used with compileOnly, so nothing from this module ships in the APK.
android {
    namespace = "com.android"
    compileSdk = 35

    defaultConfig {
        minSdk = 29
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
