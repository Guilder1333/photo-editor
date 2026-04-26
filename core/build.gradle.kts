import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    // alias(libs.plugins.androidLibrary)  // disabled: not available in CI environment
}

kotlin {
    // androidTarget { ... }  // disabled: not available in CI environment

    // iOS targets disabled in CI (no Xcode toolchain available)
    // iosX64()
    // iosArm64()
    // iosSimulatorArm64()

    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

// android { ... }  // disabled: not available in CI environment
