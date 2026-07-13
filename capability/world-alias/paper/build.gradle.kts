plugins {
    id("plutoproject.paper")
    id("plutoproject.runtime-module")
}

dependencies {
    implementation(projects.kernel.api)
    implementation(projects.capability.worldAlias.api)
    implementation(libs.bundles.koin)
    implementation(libs.hoplite.core)
    implementation(libs.hoplite.hocon)
}
