plugins {
    kotlin("multiplatform") version "2.0.0"
    kotlin("plugin.power-assert") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.0"
    id("io.gitlab.arturbosch.detekt")
    id("org.jlleitschuh.gradle.ktlint")
}

kotlin {
    macosArm64("native") {
        binaries {
            executable {
            }
        }
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
