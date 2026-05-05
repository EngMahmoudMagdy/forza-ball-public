plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvm()
    val iosX64 = iosX64()
    val iosArm64 = iosArm64()
    val iosSimulatorArm64 = iosSimulatorArm64()
    listOf(iosX64, iosArm64, iosSimulatorArm64).forEach { target ->
        target.binaries.framework {
            baseName = "CoreAuthShared"
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(project(":shared:platform"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
