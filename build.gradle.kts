import org.gradle.internal.jvm.Jvm

buildscript {
    repositories {
        mavenCentral()
        google()
        maven { url = uri("https://plugins.gradle.org/m2/") }
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.9.2")
        classpath("com.github.dcendents:android-maven-gradle-plugin:2.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.10")
        classpath("org.jlleitschuh.gradle:ktlint-gradle:12.2.0")
    }
}

plugins {
    id("com.google.devtools.ksp") version "2.1.20-2.0.1" apply false
}

println("Gradle uses Java ${Jvm.current()}")

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    // Optionally configure plugin
    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        debug.set(false)
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }

}
