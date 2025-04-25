plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.example.cnireader"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.cnireader"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    // 🔥 Ajoute ce bloc PACKAGING 🔥
    packaging {
        resources {
            excludes += setOf(
                "META-INF/versions/9/OSGI-INF/MANIFEST.MF",
                "META-INF/LICENSE",
                "META-INF/NOTICE",
                "META-INF/*.kotlin_module",
                "META-INF/DEPENDENCIES",
                "META-INF/*.version"
            )
        }
    }
}

dependencies {
    // Tests
    testImplementation(libs.mockwebserver)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    // AndroidX + ViewModel
    implementation(libs.androidx.activity.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)

    // Network
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // Core UI
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.relish)
    implementation(libs.androidx.monitor)
    implementation(libs.androidx.junit.ktx)

    // jMRTD & Scuba
    implementation(libs.jmrtd)
    implementation(libs.scuba.sc.android)
}

// 🛠 Correction pour BouncyCastle : force toujours la version bcprov-jdk18on:1.80
configurations.all {
    resolutionStrategy {
        force("org.bouncycastle:bcprov-jdk18on:1.80")
    }
}
