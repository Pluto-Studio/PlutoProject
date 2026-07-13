plugins {
    id("plutoproject.paper")
    id("plutoproject.runtime-module")
}

dependencies {
    implementation(projects.kernel.api)
    implementation(projects.kernel.api.paper)
    implementation(projects.capability.legacyCloudCommands.api.paper)
    implementation(libs.bundles.cloud)
    implementation(libs.cloud.paper)
}
