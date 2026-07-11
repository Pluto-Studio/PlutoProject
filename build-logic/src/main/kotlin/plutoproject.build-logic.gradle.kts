plugins {
    id("base")
}

tasks.register("verifyArchitecture") {
    group = "verification"
    description = "Verifies project paths, coordinates, plugins, and high-level dependency directions."

    doLast {
        val errors = mutableListOf<String>()
        val featureLeafProjects = setOf(
            "core", "api", "mongo", "messaging", "common", "paper", "velocity", "frontend",
            "adapter-common", "adapter-paper", "adapter-velocity", "infra-mongo",
        )
        rootProject.allprojects.forEach { candidate ->
            val segments = candidate.path.split(':').filter(String::isNotBlank)
            val knownPath = when (segments.firstOrNull()) {
                null -> true
                "foundation" -> segments.size == 1 ||
                    (segments.size == 2 && segments.last() in setOf("common", "paper", "velocity"))
                "kernel" -> segments.size == 1 ||
                    (segments.size == 2 && segments.last() in setOf("api", "common", "paper", "velocity")) ||
                    (segments.size == 3 && segments[1] == "api" && segments.last() in setOf("paper", "velocity"))
                "capability" -> segments.size <= 2 ||
                    (segments.size == 3 && segments.last() in setOf("api", "common", "paper", "velocity")) ||
                    (segments.size == 4 && segments[2] == "api" && segments.last() in setOf("paper", "velocity"))
                "feature" -> segments.size <= 2 ||
                    (segments.size == 3 && segments.last() in featureLeafProjects) ||
                    (segments.size == 4 && segments[2] == "api" && segments.last() in setOf("paper", "velocity"))
                "platform" -> segments.size == 1 ||
                    (segments.size == 2 && segments.last() in setOf("paper", "velocity"))
                "build-support" -> segments.size == 1 ||
                    (segments.size == 2 && segments.last() == "module-processor")
                else -> true // Flat legacy and ancillary projects remain valid during migration.
            }
            if (!knownPath) {
                errors += "${candidate.path} does not match a known runtime architecture project path"
            }
        }

        val migratedProjects = rootProject.allprojects.filter {
            it.plugins.hasPlugin("plutoproject.kotlin-library")
        }

        migratedProjects.forEach { candidate ->
            val segments = candidate.path.split(':').filter(String::isNotBlank)
            val expectedGroup = (listOf("club.plutoproject") + segments.dropLast(1).map { segment ->
                segment.lowercase()
                    .replace(Regex("[^a-z0-9]+"), "-")
                    .trim('-')
                    .ifBlank { "unnamed" }
            }).joinToString(".")
            if (candidate.group.toString() != expectedGroup) {
                errors += "${candidate.path} has group ${candidate.group}; expected $expectedGroup"
            }
        }

        val firstMigrationProjects = listOf(
            ":feature:gallery:api",
            ":feature:gallery:core",
            ":feature:whitelist-v2:api",
            ":feature:whitelist-v2:core",
        ).map(rootProject::project)
        val unrelatedPluginIds = listOf(
            "com.google.devtools.ksp",
            "org.jetbrains.kotlin.kapt",
            "org.jetbrains.kotlin.plugin.serialization",
            "io.papermc.paperweight.userdev",
        )
        firstMigrationProjects.forEach { candidate ->
            unrelatedPluginIds.filter(candidate.plugins::hasPlugin).forEach { pluginId ->
                errors += "${candidate.path} unexpectedly inherits $pluginId"
            }
        }

        rootProject.allprojects.forEach { source ->
            source.configurations.flatMap { it.dependencies }
                .filterIsInstance<org.gradle.api.artifacts.ProjectDependency>()
                .map { it.path }
                .distinct()
                .forEach { targetPath ->
                    when {
                        source.path.startsWith(":foundation:") &&
                            listOf(":kernel:", ":capability:", ":feature:").any(targetPath::startsWith) ->
                            errors += "${source.path} must not depend on $targetPath"

                        source.path.startsWith(":capability:") && targetPath.startsWith(":feature:") ->
                            errors += "${source.path} must not depend on feature $targetPath"

                        source.path.matches(Regex("^:feature:[^:]+:core$")) &&
                            targetPath.startsWith(":feature:") &&
                            !targetPath.startsWith(source.path.substringBeforeLast(':') + ":") ->
                            errors += "${source.path} must not depend on another feature: $targetPath"
                    }
                }
        }

        if (errors.isNotEmpty()) {
            throw GradleException(errors.joinToString("\n", "Architecture verification failed:\n"))
        }
    }
}
