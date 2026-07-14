plugins {
    id("plutoproject.paper-devbundle")
}

dependencies {
    implementation(projects.foundation.common)
    implementation(libs.bundles.mccoroutine.paper)
    compileOnlyApi(libs.vaultApi)
    api(libs.cloud.annotations)
}
