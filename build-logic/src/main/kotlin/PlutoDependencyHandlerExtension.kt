import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.provider.Provider

open class PlutoDependencyHandlerExtension(private val project: Project) {
    fun downloadIfRequired(
        dependencyNotation: Provider<*>,
        dependencyConfiguration: Action<ExternalModuleDependency> = Action {}
    ) = project.afterEvaluate {
        if (project.isPlatformModule) {
            dependencies.addProvider("runtimeDownload", dependencyNotation, dependencyConfiguration)
        } else {
            dependencies.addProvider("compileOnly", dependencyNotation, dependencyConfiguration)
        }
    }
}