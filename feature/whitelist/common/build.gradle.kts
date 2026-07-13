plugins {
    id("plutoproject.common-conventions")
}

dependencies {
    implementation(projects.foundation.common)
    implementation(projects.feature.whitelist.api)
    implementation(projects.feature.whitelist.mongo)
}
