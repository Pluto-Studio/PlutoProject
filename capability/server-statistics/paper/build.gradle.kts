plugins {
    id("plutoproject.paper")
    id("plutoproject.runtime-module")
}

dependencies {
    implementation(projects.kernel.api)
    implementation(projects.capability.serverStatistics.api)
    implementation(libs.bundles.koin)
    compileOnly(libs.sparkApi)
}
