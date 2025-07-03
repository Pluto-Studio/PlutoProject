import java.time.Instant

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
}


val gitCommitProvider = providers.exec {
    commandLine("git", "rev-parse", "--short", "HEAD")
}
val gitBranchProvider = providers.exec {
    commandLine("git", "rev-parse", "--abbrev-ref", "HEAD")
}
val buildTimestampProvider = providers.provider {
    Instant.now().toEpochMilli()
}

tasks.jar {
    manifest {
        attributes(
            "PlutoProject-Version" to project.version,
            "PlutoProject-Release-Name" to "${project.version} Development Preview",
            "PlutoProject-Release-Channel" to "development",
            "PlutoProject-Git-Commit" to gitCommitProvider.standardOutput.asText.map { it.trim() },
            "PlutoProject-Git-Branch" to gitBranchProvider.standardOutput.asText.map { it.trim() },
            "PlutoProject-Build-Time" to buildTimestampProvider
        )
    }
}
