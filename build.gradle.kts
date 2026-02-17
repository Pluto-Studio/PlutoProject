import java.time.Instant

plugins {
    id("plutoproject.build-logic")
    id("plutoproject.legacy-base-conventions")
    id("plutoproject.dokka-conventions")
    alias(libs.plugins.shadow)
}

dependencies {
    api(projects.platformPaper)
    api(projects.platformVelocity)
}

tasks.shadowJar {
    archiveClassifier.set(null as String?)
    mergeServiceFiles()
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
            "PlutoProject-Release-Name" to "${project.version}",
            "PlutoProject-Release-Channel" to "stable",
            "PlutoProject-Git-Commit" to gitCommitProvider.standardOutput.asText.map { it.trim() },
            "PlutoProject-Git-Branch" to gitBranchProvider.standardOutput.asText.map { it.trim() },
            "PlutoProject-Build-Time" to buildTimestampProvider
        )
    }
}
