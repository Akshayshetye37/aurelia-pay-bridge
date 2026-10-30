plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.aurelia.paybridge"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aurelia.paybridge"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Set to the PMS APK signing certificate SHA-256 before enabling IPC.
        // Blank value intentionally rejects all PMS callers.
        buildConfigField("String", "PMS_SIGNING_CERT_SHA256", "\"\\\"\\\"")
    }

    buildFeatures { buildConfig = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    packaging {
        resources.excludes += setOf("META-INF/DEPENDENCIES", "META-INF/LICENSE*", "META-INF/NOTICE*")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")

    // Provider SDKs pinned to versions referenced by official integration documentation.
    implementation("com.razorpay:checkout:1.6.41")
    implementation("com.cashfree.pg:api:2.6.1")
    implementation("in.payu:payu-sdk:7.12.3")
    implementation("in.payu:payu-custom-browser:7.16.6")
    implementation("in.payu:upisdk:1.8.8")
    implementation("in.payu:phonepe-intent:1.8.9")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
