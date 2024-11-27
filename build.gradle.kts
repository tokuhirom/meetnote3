plugins {
    kotlin("multiplatform") version "2.1.0" apply false
    id("io.gitlab.arturbosch.detekt") version("1.23.7")
    id("org.jlleitschuh.gradle.ktlint") version "12.1.2"
}

group = "org.example"
version = "1.0-SNAPSHOT"

allprojects {
    repositories {
        mavenCentral()
    }
}
