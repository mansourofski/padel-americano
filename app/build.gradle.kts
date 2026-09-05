plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.padelamericano.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.padelamericano.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "0.2.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:scheduler"))
    implementation(project(":core:ranking"))

    // Versions compatibles avec compileSdk 36
    implementation(platform("androidx.compose:compose-bom:2025.12.00"))

    implementation("androidx.activity:activity-compose:1.12.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
