plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.vergipro.mobile"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.vergipro.mobile"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("String", "API_BASE_URL", "\"https://api.vergi.pro\"")
        buildConfigField("String", "APP_ENVIRONMENT", "\"development\"")
    }

    buildTypes {
        debug {
            buildConfigField("String", "APP_ENVIRONMENT", "\"development\"")
        }
        release {
            isMinifyEnabled = true
            buildConfigField("String", "APP_ENVIRONMENT", "\"production\"")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.08.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.3")
    implementation("androidx.work:work-runtime-ktx:2.10.3")
    implementation("androidx.camera:camera-core:1.5.0")
    implementation("androidx.camera:camera-camera2:1.5.0")
    implementation("androidx.camera:camera-lifecycle:1.5.0")
    implementation("androidx.camera:camera-view:1.5.0")
    implementation("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
