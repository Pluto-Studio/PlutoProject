plugins {
    id("plutoproject.legacy-base-conventions")
}

dependencies {
    compileOnly(libs.luckperms.api)
    implementation(libs.adventureKt)
    implementation(libs.bundles.cloud)
}
