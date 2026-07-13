plugins {
    id("plutoproject.paper")
    id("plutoproject.runtime-module")
}

dependencies {
    implementation(projects.kernel.api)
    implementation(projects.kernel.api.paper)
    implementation(projects.foundation.common)
    implementation(projects.foundation.paper)
    implementation(projects.capability.profile.api)
    implementation(projects.capability.legacyCloudCommands.api.paper)
    implementation(libs.adventureKt)
    implementation(libs.aedile)
    implementation(libs.bundles.hoplite)
    implementation(libs.bundles.koin)
    implementation(libs.vaultApi)
}
