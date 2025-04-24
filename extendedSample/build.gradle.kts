import info.git.versionHelper.getGitCommitCount
import info.git.versionHelper.getLatestGitHash
import info.git.versionHelper.getVersionText

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "info.mqtt.android.extsample"

    defaultConfig {
        applicationId = "info.mqtt.android.extsample"
        minSdk = 21
        compileSdk = 35
        targetSdk = 35
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
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":serviceLibrary"))

    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.1.20")
    implementation("com.github.AppDevNext.Logcat:LogcatCoreUI:3.3.1")
    implementation("androidx.room:room-runtime:2.7.1")
    implementation("androidx.test.uiautomator:uiautomator:2.3.0")
    ksp("androidx.room:room-compiler:2.7.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("com.github.AppDevNext:Moka:1.7")
    androidTestImplementation("androidx.test.ext:junit-ktx:1.2.1")
    androidTestUtil("androidx.test.services:test-services:1.5.0")
    androidTestImplementation("org.hamcrest:hamcrest:3.0")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.6.1")
}
