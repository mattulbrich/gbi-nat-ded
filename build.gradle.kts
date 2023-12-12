plugins {
    kotlin("multiplatform") version "1.9.21"
    distribution
}

group = "edu.kit.kastel.formal"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
}

kotlin {
    js {
        browser {
            binaries.executable()
        }
    }
    jvm {}
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("com.github.h0tk3y.betterParse:better-parse:0.4.4")
            }
        }
        val jsMain by getting {
            dependencies {
                // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-html-js
                implementation("org.jetbrains.kotlinx:kotlinx-html-js:0.9.1")
            }
        }
    }
}