import io.papermc.paperweight.userdev.PaperweightUserDependenciesExtension
import org.jetbrains.kotlin.gradle.internal.KaptGenerateStubsTask

plugins {
    id("plutoproject.common-conventions")
    id("plutoproject.compose-conventions")
    id("io.papermc.paperweight.userdev")
}

// 2026/1/29 - 更新到 Kotlin 2.3.0 后出现 kapt 问题：property 'compilerOptions.freeCompilerArgs' doesn't have a configured value.
// 怀疑是 paperweight 导致的，临时 hack 一下。
afterEvaluate {
    tasks.withType<KaptGenerateStubsTask>().configureEach {
        compilerOptions {
            freeCompilerArgs.set(listOf())
        }
    }
}

dependencies {
    with(extensions.getByType<PaperweightUserDependenciesExtension>()) {
        paperDevBundle("26.2.build.38-alpha")
    }
    compileOnly(libs.sparkApi) {
        isTransitive = false
    }
    compileOnly(libs.vaultApi) {
        isTransitive = false
    }
    compileOnly(libs.coreprotect) {
        isTransitive = false
    }
    implementation(libs.cloud.paper)
    implementation(libs.bundles.mccoroutine.paper)
    implementation(libs.jetbrains.compose.runtime)
    implementation(libs.jetbrains.compose.runtime.saveable)
    implementation(libs.bundles.voyager)
}
