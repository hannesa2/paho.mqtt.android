import info.git.versionHelper.getGitCommitCount
import info.git.versionHelper.getLatestGitHash
import info.git.versionHelper.getVersionText

plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    namespace = "info.mqtt.java.example"
    defaultConfig {
        applicationId = "info.mqtt.java.example"
        minSdk = 21
        compileSdk = 35
        targetSdk = 35
        versionCode = getGitCommitCount()
        versionName = "${getVersionText()}.$versionCode-${getLatestGitHash()}"

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
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("com.github.AppDevNext.Logcat:LogcatCoreLib:3.3.1")

    implementation("androidx.core:core-ktx:1.16.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.1.20")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("com.github.AppDevNext:Moka:1.7")
    androidTestImplementation("androidx.test.ext:junit-ktx:1.2.1")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestUtil("androidx.test.services:test-services:1.5.0")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.6.1")
}
