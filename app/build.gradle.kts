plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace   = "com.mnmyounus.ypi"
    compileSdk  = 35

    defaultConfig {
        applicationId = "com.mnmyounus.ypi"
        minSdk        = 26          // Android 8.0 Oreo — gives us AudioPlaybackCallback
        targetSdk     = 35
        versionCode   = 1
        versionName   = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled   = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Uses the auto-generated debug keystore so no secrets are needed.
            // Installable via sideload / GitHub Releases. Swap for a real
            // signingConfig if you ever publish to the Play Store.
            signingConfig = signingConfigs.getByName("debug")
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable        = true
        }
    }

    buildFeatures {
        viewBinding = true   // typed View access, zero reflection overhead
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Minimal, privacy-focused set — no networking, no analytics, no tracking
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // AES-256-GCM encrypted file storage, Android Keystore-backed (activity log)
    implementation("androidx.security:security-crypto:1.0.0")
}
