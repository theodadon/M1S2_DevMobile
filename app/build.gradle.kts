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
        //noinspection OldTargetApi
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
    compileOptions { sourceCompatibility = JavaVersion.VERSION_11; targetCompatibility = JavaVersion.VERSION_11 }
    kotlinOptions   { jvmTarget = "11" }
}

dependencies {
    testImplementation(libs.mockwebserver)
// JUnit
    testImplementation(libs.junit)
// (Optionnel) Coroutines-test si tu veux tester en runBlockingTest
    testImplementation(libs.kotlinx.coroutines.test)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.okhttp.logging)
    implementation(libs.okhttp)
    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)

// ViewModel Scope
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation("com.google.dagger:hilt-android:2.56.2")
    kapt("com.google.dagger:hilt-compiler:2.56.2")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    implementation("org.jmrtd:jmrtd:0.8.1") {
        exclude(group = "org.bouncycastle")           // ← BC out
    }
    implementation("net.sf.scuba:scuba-sc-android:0.0.26") {
        exclude(group = "org.bouncycastle")           // ← BC out
    }

    // 👉  option A : pas de provider tiers (Android ≥ 8 sait faire ECDSA/ECDH)
    // 👉  option B : remettre SpongyCastle si besoin cryptographique spécial :
    // implementation("com.madgag.spongycastle:core:1.58.0.0")
    // implementation("com.madgag.spongycastle:prov:1.58.0.0")

    implementation(libs.relish)
    implementation(libs.androidx.monitor)
    implementation(libs.androidx.junit.ktx)
}

