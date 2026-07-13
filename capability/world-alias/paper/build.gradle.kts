plugins {
    id("plutoproject.paper-conventions")
    id("plutoproject.runtime-module")
}

dependencies {
    implementation(projects.kernel.api)
    implementation(projects.capability.worldAlias.api)
    implementation(libs.hoplite.core)
    implementation(libs.hoplite.hocon)
}
