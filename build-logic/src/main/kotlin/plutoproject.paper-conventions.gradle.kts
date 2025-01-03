import io.papermc.paperweight.userdev.PaperweightUserDependenciesExtension

plugins {
    id("plutoproject.common-conventions")
    id("plutoproject.compose-conventions")
    id("io.papermc.paperweight.userdev")
}

dependencies {
    with(extensions.getByType<PaperweightUserDependenciesExtension>()) {
        paperDevBundle("1.21.1-R0.1-SNAPSHOT")
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
        downloadIfRequired(libs.bundles.mccoroutine.paper)
        downloadIfRequired(provider { compose.runtime })
        downloadIfRequired(provider { compose.runtimeSaveable })
        downloadIfRequired(libs.bundles.voyager)
    }
}
