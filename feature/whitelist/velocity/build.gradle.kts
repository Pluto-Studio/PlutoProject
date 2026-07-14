plugins {
    id("plutoproject.velocity")
    id("plutoproject.runtime-module")
}

dependencies {
    implementation(projects.kernel.api)
    implementation(projects.kernel.api.velocity)
    implementation(projects.foundation.common)
    implementation(projects.foundation.velocity)
    implementation(projects.capability.mongo.api)
    implementation(projects.capability.charonflow.api)
    implementation(projects.capability.geoip.api)
    implementation(projects.capability.profile.api)
    implementation(projects.capability.legacyCloudCommands.api.velocity)
    implementation(projects.feature.whitelist.common)
    implementation(projects.feature.whitelist.core)
    implementation(libs.koin.core)
    implementation(libs.bundles.hoplite)
    implementation(libs.bundles.cloud)
    implementation(libs.bundles.mccoroutine.velocity)
    implementation(libs.geoip2)
    compileOnly(libs.luckperms.api)
}
