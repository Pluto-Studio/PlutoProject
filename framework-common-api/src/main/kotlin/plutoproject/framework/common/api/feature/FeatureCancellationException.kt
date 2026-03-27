package plutoproject.framework.common.api.feature

import kotlinx.coroutines.CancellationException

class FeatureCancellationException(val featureId: String) : CancellationException()
