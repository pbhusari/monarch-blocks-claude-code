/*
 * Copyright (c) 2026 American Printing House for the Blind
 * Use of this source code is governed by an MIT-style license that can be found in the LICENSE file.
 */

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

//Needed for Monarch Development
val jenkinsBuildCode: String = System.getenv("J_VERCODE") ?: "1"
val jenkinsBuildName: String = System.getenv("J_VERNAME") ?: "1.1"

//When testing debug builds on a Monarch that already has the app loaded on it from HumanWare.
//val jenkinsBuildCode = System.getenv("J_VERCODE") ?: "99999999"
//val jenkinsBuildName = System.getenv("J_VERNAME") ?: "Debug [99999999]"

android {
    namespace = "org.aph.monarchblocks"
    compileSdk = 34

    defaultConfig {
        applicationId = "org.aph.monarchblocks"
        minSdk = 33
        targetSdk = 33
        versionCode = jenkinsBuildCode.toInt()
        versionName = jenkinsBuildName

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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("com.humanware:KeySoftSDK:latest.release") //Needed for Monarch Development
    implementation("com.google.dagger:hilt-android:2.45")//Needed for Monarch Development
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
