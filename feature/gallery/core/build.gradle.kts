plugins {
    id("plutoproject.test-conventions")
}

dependencies {
    // Used for hashing; provided at runtime via platform runtimeDownload.
    compileOnly(libs.hash4j)
    testImplementation(libs.hash4j)
}
