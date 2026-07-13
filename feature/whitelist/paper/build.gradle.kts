plugins {
    id("plutoproject.paper")
    id("plutoproject.runtime-module")
}

dependencies {
    implementation(projects.kernel.api)
    implementation(projects.kernel.api.paper)
    implementation(projects.foundation.common)
    implementation(projects.foundation.paper)
    implementation(projects.capability.mongo.api)
    implementation(projects.capability.charonflow.api)
    implementation(projects.feature.whitelist.common)
    implementation(libs.koin.core)
    implementation(libs.bundles.hoplite)
}
