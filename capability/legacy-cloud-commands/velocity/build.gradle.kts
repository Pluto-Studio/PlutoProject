plugins {
    id("plutoproject.velocity")
    id("plutoproject.runtime-module")
}

dependencies {
    implementation(projects.kernel.api)
    implementation(projects.kernel.api.velocity)
    implementation(projects.capability.legacyCloudCommands.api.velocity)
    implementation(libs.bundles.cloud)
    implementation(libs.cloud.velocity)
}
