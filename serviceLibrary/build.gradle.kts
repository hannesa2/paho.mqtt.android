import info.git.versionHelper.getVersionText
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("maven-publish")
    id("com.google.devtools.ksp")
}

android {
    namespace = "info.mqtt.android.service"
    testNamespace = "info.mqtt.android.service.test"
    compileSdk = 36
    defaultConfig {
        minSdk = 23

        // Android Studio 4.1 doesn"t generate versionName in libraries any more
        // https://developer.android.com/studio/releases/gradle-plugin#version_properties_removed_from_buildconfig_class_in_library_projects
        buildConfigField("String", "VERSION_NAME", "\"${getVersionText()}\"")

        testApplicationId = "info.mgtt.android.service.test"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments.putAll(
            mapOf(
                "useTestStorageService" to "true",
            ),
        )

        packaging {
            resources {
                pickFirsts += setOf("META-INF/serviceLibrary_debug.kotlin_module")
            }
        }

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }

        buildTypes {
            release {
                isMinifyEnabled = false
                consumerProguardFile("proguard-sdk.pro")
            }
        }

        buildFeatures {
            buildConfig = true
        }
    }

    testOptions {
        targetSdk = 36
    }

    testFixtures {
        enable = true
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
    publishing {
        singleVariant("release") {}
    }
}

dependencies {
    api("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.2.20")
    implementation("androidx.work:work-runtime-ktx:2.10.5")
    implementation("com.github.AppDevNext.Logcat:LogcatCoreLib:3.4")

    implementation("androidx.room:room-runtime:2.8.0")
    ksp("androidx.room:room-compiler:2.8.0")

    androidTestImplementation("androidx.test.ext:junit-ktx:1.2.1")
    androidTestUtil("androidx.test.services:test-services:1.6.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.test:rules:1.7.0")
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["release"])
                pom {
                    licenses {
                        license {
                            name = "Apache License Version 2.0"
                            url = "https://github.com/hannesa2/paho.mqtt.android/blob/master/LICENSE"
                        }
                    }
                }
            }
        }
    }
}
