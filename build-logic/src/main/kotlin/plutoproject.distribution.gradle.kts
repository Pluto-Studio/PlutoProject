import xyz.jpenilla.gremlin.gradle.WriteDependencySet

plugins {
    id("plutoproject.kotlin-library")
    id("xyz.jpenilla.gremlin-gradle")
    id("com.gradleup.shadow")
}

val paperRuntimeDownload by configurations.creating
val velocityRuntimeDownload by configurations.creating

tasks.withType<WriteDependencySet>().configureEach {
    when (name) {
        "writePaperRuntimeDownloadDependencies" -> outputFileName = "paper-dependencies.txt"
        "writeVelocityRuntimeDownloadDependencies" -> outputFileName = "velocity-dependencies.txt"
    }
}

tasks.shadowJar {
    archiveClassifier.set(null as String?)
    mergeServiceFiles()
}
