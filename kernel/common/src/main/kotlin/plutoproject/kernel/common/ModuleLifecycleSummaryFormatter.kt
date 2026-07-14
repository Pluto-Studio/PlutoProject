package plutoproject.kernel.common

import kotlin.time.Duration
import plutoproject.kernel.api.ModuleOperation
import plutoproject.kernel.api.ModuleOperationResult

private const val MAX_SUMMARY_LINE_LENGTH = 100

fun formatModuleLifecycleSummary(
    operation: ModuleOperation,
    results: Collection<ModuleOperationResult>,
    elapsed: Duration,
): List<String> {
    val succeeded = results.filterIsInstance<ModuleOperationResult.Success>()
    val failed = results.filterIsInstance<ModuleOperationResult.Failed>()
    val rejected = results.filterIsInstance<ModuleOperationResult.Rejected>()
    val blocked = rejected.filter(ModuleOperationResult.Rejected::isDependencyBlocked)
    val otherRejected = rejected - blocked.toSet()

    return buildList {
        add(
            "Runtime module ${operation.name.lowercase()} completed in ${elapsed.inWholeMilliseconds} ms: " +
                "${succeeded.size} succeeded, ${failed.size} failed, ${blocked.size} blocked.",
        )
        addResultLines("Succeeded", succeeded.map(ModuleOperationResult.Success::id))
        addResultLines(
            "Failed",
            failed.map { result ->
                val suffix = if (failed.size == 1) "stack trace" else "stack traces"
                "${result.id} (see $suffix above)"
            },
        )
        addResultLines("Blocked", blocked.map(ModuleOperationResult.Rejected::formatBlockedModule))
        if (otherRejected.isNotEmpty()) {
            addResultLines("Rejected", otherRejected.map(ModuleOperationResult.Rejected::id))
        }
    }
}

fun ModuleOperationResult.Rejected.isDependencyBlocked(): Boolean = blockerPaths.isNotEmpty()

private fun ModuleOperationResult.Rejected.formatBlockedModule(): String {
    val paths = blockerPaths.joinToString("; ") { it.joinToString(" -> ") }
    return "$id [$paths]"
}

private fun MutableList<String>.addResultLines(label: String, modules: List<String>) {
    val firstPrefix = "|- $label (${modules.size}): "
    if (modules.isEmpty()) {
        add("${firstPrefix}None.")
        return
    }

    val continuationPrefix = "|  "
    var line = firstPrefix
    var hasModule = false
    modules.forEach { module ->
        val separator = if (hasModule) ", " else ""
        if (hasModule && line.length + separator.length + module.length + 1 > MAX_SUMMARY_LINE_LENGTH) {
            add("$line,")
            line = continuationPrefix + module
            hasModule = true
        } else {
            line += separator + module
            hasModule = true
        }
    }
    add("$line.")
}
