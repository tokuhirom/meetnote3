plugins {
    kotlin("multiplatform") version "2.0.10"
    kotlin("plugin.power-assert") version "2.0.20"
    kotlin("plugin.serialization") version "2.0.10"
    id("io.gitlab.arturbosch.detekt")
    id("org.jlleitschuh.gradle.ktlint")
}

kotlin {
    macosArm64("native") {
    }
    js {
        browser()
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
            }
        }
    }
}
