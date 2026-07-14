plugins {
    id("plutoproject.core")
    id("plutoproject.test")
}

dependencies {
    implementation(projects.kernel.api)
    implementation(projects.capability.serverIdentifier.api)
    implementation(libs.hoplite.core)
    implementation(libs.hoplite.hocon)
}
