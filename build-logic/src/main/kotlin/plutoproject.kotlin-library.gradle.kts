import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `java-library`
    kotlin("jvm")
}

val baseGroup = "club.plutoproject"
val pathSegments = path.split(':').filter(String::isNotBlank)

group = (listOf(baseGroup) + pathSegments.dropLast(1).map(::normalizeCoordinateSegment))
    .joinToString(".")
version = "1.6.10"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
        javaParameters.set(true)
    }
}

dependencies {
    implementation(libs.bundles.language)
}

fun normalizeCoordinateSegment(segment: String) = segment
    .lowercase()
    .replace(Regex("[^a-z0-9]+"), "-")
    .trim('-')
    .ifBlank { "unnamed" }
