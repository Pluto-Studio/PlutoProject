import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java")
    id("java-library")
    kotlin("jvm")
    kotlin("plugin.serialization")
}

group = "club.plutoproject"
version = "1.6.5"

repositories {
    mavenCentral()
    google()
    maven("https://jitpack.io")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.nostal.ink/repository/maven-public")
    maven("https://repo.lucko.me/")
    maven("https://maven.playpro.com/")
    maven("https://repo.opencollab.dev/main/")
    maven("https://repo.codemc.org/repository/maven-public")
}

configurations.all {
    resolutionStrategy {
        force(libs.kotlin.stdlib)
        force(libs.kotlin.reflect)
        force(libs.kotlin.serialization)
        force(libs.kotlinx.coroutine.core)
        force(libs.guava)
        force(libs.okio)
        force(libs.adventure)
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

kotlin {
    jvmToolchain(21)
}

tasks.compileKotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        javaParameters = true
    }
}

dependencies {
    compileOnly(libs.bundles.language)
}
