plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

android {
    namespace = "com.example.elsuarku"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.elsuarku"
        minSdk = 34
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            // Enable App Check debug token for development
            isDebuggable = true
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

    // ── BuildConfig Fields ──
    defaultConfig {
        // Git hash (short SHA)
        buildConfigField("String", "GIT_HASH", "\"${runCatching {
            ProcessBuilder("git", "rev-parse", "--short", "HEAD")
                .directory(rootProject.projectDir)
                .start().inputStream.bufferedReader().readText().trim()
        }.getOrDefault("unknown")}\"")

        // Build timestamp
        buildConfigField("long", "BUILD_TIMESTAMP", "${System.currentTimeMillis()}L")

        // Branch name
        buildConfigField("String", "GIT_BRANCH", "\"${runCatching {
            ProcessBuilder("git", "rev-parse", "--abbrev-ref", "HEAD")
                .directory(rootProject.projectDir)
                .start().inputStream.bufferedReader().readText().trim()
        }.getOrDefault("main")}\"")
    }
}

dependencies {
    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.appcompat)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Compose
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Firebase BoM — controls all Firebase library versions
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.appcheck)
    implementation(libs.firebase.appcheck.playintegrity)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.crashlytics)
    debugImplementation("com.google.firebase:firebase-appcheck-debug")

    // Google Sign-In
    implementation(libs.play.services.auth)

    // Security
    implementation(libs.androidx.security.crypto)

    // Biometric
    implementation(libs.androidx.biometric)

    // Image Loading — Coil
    implementation(libs.coil.compose)

    // DataStore Preferences
    implementation(libs.androidx.datastore.preferences)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // LeakCanary — memory leak detection in debug builds
    debugImplementation(libs.leakcanary.android)

    // Vico Charts — vote result visualization
    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)

    // WorkManager — background reconciliation
    implementation("androidx.work:work-runtime-ktx:2.9.1")
}
