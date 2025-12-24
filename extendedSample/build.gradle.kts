import info.git.versionHelper.getGitCommitCount
import info.git.versionHelper.getLatestGitHash
import info.git.versionHelper.getVersionText
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "info.mqtt.android.extsample"

    defaultConfig {
        applicationId = "info.mqtt.android.extsample"
        minSdk = 23
        compileSdk = 36
        targetSdk = 36
        versionCode = getGitCommitCount()
        versionName = "${getVersionText()}.$versionCode-${getLatestGitHash()}"

        if (System.getenv("CI") == "true") { // Github action
            resValue("string", "add_connection_server_default", "10.0.2.2")
        } else
            resValue("string", "add_connection_server_default", "broker.hivemq.com")

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments.putAll(
            mapOf(
                "useTestStorageService" to "true",
            ),
        )
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }
}

dependencies {
    implementation("com.github.hannesa2:paho.mqtt.android:4.4.2")

    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.2.20")
    implementation("com.github.AppDevNext.Logcat:LogcatCoreUI:3.4")
    implementation("androidx.room:room-runtime:2.8.3")
    implementation("androidx.test.uiautomator:uiautomator:2.3.0")
    ksp("androidx.room:room-compiler:2.8.3")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("com.github.AppDevNext:Moka:1.7")
    androidTestImplementation("androidx.test.ext:junit-ktx:1.2.1")
    androidTestUtil("androidx.test.services:test-services:1.6.0")
    androidTestImplementation("org.hamcrest:hamcrest:3.0")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.6.1")
}
