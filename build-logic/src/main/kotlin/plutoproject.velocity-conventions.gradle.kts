plugins {
    id("plutoproject.common-conventions")
}

dependencies {
    compileOnly(libs.velocity.api)
    implementation(libs.cloud.velocity)
    implementation(libs.bundles.mccoroutine.velocity)
}
