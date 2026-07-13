import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java")
    id("java-library")
    kotlin("jvm")
    kotlin("plugin.serialization")
}

val baseGroup = "club.plutoproject"
val pathSegments = path.split(':').filter(String::isNotBlank)

// 将所有父级路径加入 group，避免同名末级模块共享同一 GAV。
group = (listOf(baseGroup) + pathSegments.dropLast(1).map(::normalizeCoordinateSegment))
    .joinToString(".")
version = "1.6.10"

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
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

kotlin {
    jvmToolchain(25)
}

tasks.compileKotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget("25"))
        javaParameters = true
    }
}

dependencies {
    compileOnly(libs.bundles.language)
}

fun normalizeCoordinateSegment(segment: String) = segment
    .lowercase()
    .replace(Regex("[^a-z0-9]+"), "-")
    .trim('-')
    .ifBlank { "unnamed" }
