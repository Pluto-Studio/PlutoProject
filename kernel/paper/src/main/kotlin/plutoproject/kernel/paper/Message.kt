package plutoproject.kernel.paper

import net.kyori.adventure.text.Component
import plutoproject.kernel.api.ModuleOperationResult
import plutoproject.kernel.common.ModuleInspection
import plutoproject.kernel.common.ModuleSnapshot

internal const val FEATURE_LIST_PERMISSION = "plutoproject.command.feature.list"
internal const val FEATURE_INFO_PERMISSION = "plutoproject.command.feature.info"
internal const val FEATURE_DISABLE_PERMISSION = "plutoproject.command.feature.disable"
internal const val CAPABILITY_LIST_PERMISSION = "plutoproject.command.capability.list"
internal const val CAPABILITY_INFO_PERMISSION = "plutoproject.command.capability.info"
internal const val MODULE_GRAPH_PERMISSION = "plutoproject.command.module.graph"

internal fun getModuleListMessage(type: String, modules: List<ModuleSnapshot>): Component {
    val values = modules.joinToString { "${it.descriptor.id}=${it.state}" }.ifEmpty { "none" }
    return Component.text("$type modules: $values")
}

internal fun getModuleInfoMessage(inspection: ModuleInspection): Component {
    val snapshot = inspection.snapshot
    val descriptor = snapshot.descriptor
    val operation = snapshot.runningOperation?.name ?: "none"
    val optional = inspection.activeOptionalDependencies.joinToString().ifEmpty { "none" }
    val dependents = inspection.enabledDirectDependents.joinToString().ifEmpty { "none" }
    val latest = when (val result = snapshot.latestResult) {
        is ModuleOperationResult.Success -> "${result.operation}: success (${result.state})"
        is ModuleOperationResult.Rejected -> "${result.operation}: rejected (${result.reason})"
        is ModuleOperationResult.Failed -> "${result.operation}: failed (${result.phase})"
        null -> "none"
    }
    val failure = snapshot.failure?.message ?: "none"
    return Component.text(
        "${descriptor.id}: type=${descriptor.type}, platform=${descriptor.platform}, state=${snapshot.state}, " +
            "operation=$operation, requiredFeatures=${descriptor.requiredFeatures}, " +
            "optionalFeatures=$optional, requiredCapabilities=${descriptor.requiredCapabilities}, " +
            "enabledDependents=$dependents, latest=$latest, failure=$failure",
    )
}

internal fun getUnknownModuleMessage(id: String): Component = Component.text("Unknown runtime module '$id'")

internal fun getModuleGraphMessage(paths: List<List<String>>): Component = Component.text(
    "Dependency paths: ${paths.joinToString("; ") { it.joinToString(" -> ") }}",
)

internal fun getDisableResultMessage(result: ModuleOperationResult): Component = when (result) {
    is ModuleOperationResult.Success -> Component.text("Feature '${result.id}' disabled")
    is ModuleOperationResult.Rejected -> {
        val paths = result.blockerPaths.joinToString("; ") { it.joinToString(" -> ") }
        Component.text("Cannot disable '${result.id}': ${result.reason}${if (paths.isEmpty()) "" else " ($paths)"}")
    }
    is ModuleOperationResult.Failed -> Component.text(
        "Failed to disable '${result.id}' during ${result.phase}: ${result.cause.message}",
    )
}
