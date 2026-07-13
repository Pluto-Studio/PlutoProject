plugins {
    id("plutoproject.kotlin-library")
    id("com.gradleup.shadow")
}

tasks.shadowJar {
    archiveClassifier.set(null as String?)
    mergeServiceFiles()
}

tasks.jar {
    enabled = false
}
