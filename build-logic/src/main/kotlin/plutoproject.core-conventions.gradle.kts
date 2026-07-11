import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java")
    id("java-library")
    kotlin("jvm")
    kotlin("plugin.serialization")
}

val baseGroup = "club.plutoproject"

// 注意：`:feature:<featureId>:<module>` 这些子项目在不同 feature 之间会共享相同的末级名称（core/api/adapter-*），
// 如果继续使用默认 group，Gradle 依赖解析会把它们当成同一个 GAV，从而发生模块冲突（只会保留其中一个）。
// 因此这里为每个 feature 生成独立的 group，避免不同 feature 的同名模块互相替换（例如不同 feature 的 adapter-paper）。
group = run {
    val segments = path.split(':').filter { it.isNotBlank() }
    if (segments.firstOrNull() == "feature" && segments.size >= 2) {
        val featureId = segments[1]
        val normalized = featureId
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .ifBlank { "unknown" }
        "$baseGroup.feature.$normalized"
    } else {
        baseGroup
    }
}
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
