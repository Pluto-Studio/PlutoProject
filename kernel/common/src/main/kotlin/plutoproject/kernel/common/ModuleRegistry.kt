package plutoproject.kernel.common

import java.util.concurrent.ConcurrentHashMap
import plutoproject.kernel.api.FeatureRegistry
import plutoproject.kernel.api.ModuleDescriptor
import plutoproject.kernel.api.ModuleOperation
import plutoproject.kernel.api.ModuleOperationResult
import plutoproject.kernel.api.ModuleState

data class ModuleSnapshot(
    val descriptor: ModuleDescriptor,
    val state: ModuleState,
    val runningOperation: ModuleOperation? = null,
    val latestResult: ModuleOperationResult? = null,
    val dependencyPath: List<String> = emptyList(),
    val failure: Throwable? = null,
)

class ModuleRegistry internal constructor(descriptors: Collection<ModuleDescriptor>) : FeatureRegistry {
    private val snapshots = ConcurrentHashMap(
        descriptors.associate { descriptor ->
            descriptor.id to ModuleSnapshot(descriptor, ModuleState.DISCOVERED)
        },
    )

    override fun state(id: String): ModuleState? = snapshots[id]?.state

    override fun isEnabled(id: String): Boolean = state(id) == ModuleState.ENABLED

    fun descriptor(id: String): ModuleDescriptor? = snapshots[id]?.descriptor

    fun snapshot(id: String): ModuleSnapshot? = snapshots[id]

    fun snapshots(): List<ModuleSnapshot> = snapshots.values.sortedBy { it.descriptor.id }

    internal fun begin(id: String, operation: ModuleOperation) {
        snapshots.compute(id) { _, snapshot -> snapshot?.copy(runningOperation = operation) }
    }

    internal fun complete(
        id: String,
        state: ModuleState,
        result: ModuleOperationResult,
        dependencyPath: List<String> = emptyList(),
        failure: Throwable? = null,
    ) {
        snapshots.compute(id) { _, snapshot ->
            snapshot?.copy(
                state = state,
                runningOperation = null,
                latestResult = result,
                dependencyPath = dependencyPath,
                failure = failure,
            )
        }
    }

    internal fun terminate(id: String, state: ModuleState) {
        snapshots.compute(id) { _, snapshot ->
            snapshot?.copy(
                state = state,
                runningOperation = null,
            )
        }
    }
}
