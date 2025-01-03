import xyz.jpenilla.gremlin.gradle.WriteDependencySet

plugins {
    id("plutoproject.base-conventions")
    id("xyz.jpenilla.gremlin-gradle")
}

configurations.compileOnly {
    extendsFrom(configurations.getByName("runtimeDownload"))
}

tasks.withType<WriteDependencySet>() {
    outputFileName = when {
        withPaperEnvironment -> "paper-dependencies.txt"
        withVelocityEnvironment -> "velocity-dependencies.txt"
        else -> error("Unexpected")
    }
}