plugins {
    id("plutoproject.paper-devbundle")
    id("plutoproject.runtime-module")
    id("plutoproject.compose")
}

dependencies {
    implementation(projects.kernel.api)
    implementation(projects.kernel.api.paper)
    implementation(projects.foundation.common)
    implementation(projects.foundation.paper)
    implementation(projects.capability.databasePersist.api)
    implementation(projects.capability.interactive.api)
    implementation(projects.capability.legacyCloudCommands.api.paper)
    implementation(projects.feature.menu.api.paper)
    implementation(projects.feature.sit.api.paper)
    implementation(libs.bundles.koin)
    implementation(libs.bundles.mccoroutine.paper)
}
