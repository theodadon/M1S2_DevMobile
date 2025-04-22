plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
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
    compileOptions { sourceCompatibility = JavaVersion.VERSION_11; targetCompatibility = JavaVersion.VERSION_11 }
    kotlinOptions   { jvmTarget = "11" }
}

dependencies {
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

