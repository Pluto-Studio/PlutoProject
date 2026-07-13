plugins {
    id("plutoproject.paper")
    id("plutoproject.runtime-module")
}

dependencies {
    implementation(projects.kernel.api)
    implementation(projects.capability.profile.common)
}
