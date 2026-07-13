plugins {
    id("plutoproject.velocity-conventions")
}

dependencies {
    implementation(projects.foundation.velocity)
    implementation(projects.feature.whitelist.common)
}
