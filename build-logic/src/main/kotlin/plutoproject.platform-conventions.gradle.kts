import xyz.jpenilla.gremlin.gradle.WriteDependencySet

plugins {
    id("plutoproject.base-conventions")
    id("xyz.jpenilla.gremlin-gradle")
}

configurations.compileOnly {
    extendsFrom(configurations.getByName("runtimeDownload"))
}

tasks.withType<WriteDependencySet> {
    relocate("com.google.protobuf", "libs.com.google.protobuf")
    relocate("com.github.retrooper.packetevents", "libs.com.github.retrooper.packetevents")
    outputFileName = when {
        withPaperEnvironment -> "paper-dependencies.txt"
        withVelocityEnvironment -> "velocity-dependencies.txt"
        else -> error("Unexpected")
    }
}
