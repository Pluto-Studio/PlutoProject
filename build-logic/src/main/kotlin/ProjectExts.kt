import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Project
import org.gradle.kotlin.dsl.the

val Project.isPlatformModule: Boolean
    get() = name.lowercase().contains("platform")

val Project.isFeatureModule: Boolean
    get() = name.lowercase().contains("feature")

val Project.isFrameworkModule: Boolean
    get() = name.lowercase().contains("framework")

val Project.withCommonEnvironment
    get() = name.lowercase().contains("common")

val Project.withPaperEnvironment
    get() = name.lowercase().contains("paper")

val Project.withVelocityEnvironment
    get() = name.lowercase().contains("velocity")

val Project.libs: LibrariesForLibs
    get() = the()