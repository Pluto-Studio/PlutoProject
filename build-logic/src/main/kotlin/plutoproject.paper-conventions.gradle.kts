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
        paperDevBundle("1.21.11-R0.1-SNAPSHOT")
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
    with(extensions.getByType<PlutoDependencyHandlerExtension>()) {
        downloadIfRequired(libs.cloud.paper)
        downloadIfRequired(libs.anvilGui)
        downloadIfRequired(libs.bundles.mccoroutine.paper)
        downloadIfRequired(provider { compose.runtime })
        downloadIfRequired(provider { compose.runtimeSaveable })
        downloadIfRequired(libs.bundles.voyager)
    }
}
