package plutoproject.kernel.common

import plutoproject.kernel.api.ModuleOperationResult

fun interface ModuleOperationReporter {
    fun report(result: ModuleOperationResult)

    companion object {
        val NONE = ModuleOperationReporter {}
    }
}
