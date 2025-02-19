plugins {
    id("plutoproject.build-logic")
    id("plutoproject.base-conventions")
    alias(libs.plugins.shadow)
    alias(libs.plugins.protobuf)
}

dependencies {
    api(projects.platformPaper)
    api(projects.platformVelocity)
}

tasks.shadowJar {
    archiveClassifier.set(null as String?)
    mergeServiceFiles()
    relocate("com.google.protobuf", "libs.com.google.protobuf")
    relocate("com.github.retrooper.packetevents", "libs.com.github.retrooper.packetevents")
}
