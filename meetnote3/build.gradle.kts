plugins {
    kotlin("multiplatform") version "2.1.0"
    kotlin("plugin.power-assert") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    id("io.gitlab.arturbosch.detekt")
    id("org.jlleitschuh.gradle.ktlint")
}

kotlin {
    macosArm64("native") {
        binaries {
            executable {
                // show GC logs
//                freeCompilerArgs += listOf("-Xruntime-logs=gc=info")
            }
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":model"))

                implementation(project.dependencies.platform("io.ktor:ktor-bom:2.3.13"))
                implementation("io.ktor:ktor-client-core")
                implementation("io.ktor:ktor-client-darwin")
                implementation("io.ktor:ktor-serialization-kotlinx-json")

                implementation("com.squareup.okio:okio:3.10.2")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
            }
        }

        val nativeMain by getting {
            kotlin.srcDir("src/nativeMain/kotlin")
            kotlin.srcDir("build/generated/kotlin") // Add this line
        }
    }
}

tasks.register("generateGpt2Kt") {
    doLast {
        val sourceFile = file(rootDir.resolve("scripts/summarize.py"))
        val targetFile = projectDir.resolve("build/generated/kotlin/meetnote3/python/Gpt2.kt")
        val content = sourceFile.readText()

        val kotlinFileContent = listOf(
            "// This file was auto generated by build.gradle.kts",
            "package meetnote3.python\n",
            "const val SUMMARIZE_GPT2 = \"\"\"",
            content,
            "\"\"\"\n",
        ).joinToString("\n")

        targetFile.parentFile.mkdirs()
        targetFile.writeText(kotlinFileContent)
    }
}

tasks.named("compileKotlinNative") {
    dependsOn("generateGpt2Kt")
}
