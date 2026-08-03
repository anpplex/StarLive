plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.starlive.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.starlive.app"
        minSdk = 28
        targetSdk = 35
        versionCode = 7
        versionName = "0.1.5-redeem"
        // Override at build: -PREDEEM_API_BASE=https://host
        buildConfigField(
            "String",
            "REDEEM_API_BASE",
            "\"${project.findProperty("REDEEM_API_BASE") ?: "https://buy.998618.xyz"}\"",
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures { buildConfig = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
}
