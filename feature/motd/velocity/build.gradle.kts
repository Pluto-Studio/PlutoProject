plugins {
    id("plutoproject.velocity")
    id("plutoproject.runtime-module")
}

dependencies {
    implementation(projects.kernel.api)
    implementation(projects.kernel.api.velocity)
    implementation(projects.foundation.common)
    implementation(projects.foundation.velocity)
    implementation(projects.capability.legacyCloudCommands.api.velocity)
    implementation(libs.bundles.hoplite)
    implementation(libs.bundles.mccoroutine.velocity)
}
