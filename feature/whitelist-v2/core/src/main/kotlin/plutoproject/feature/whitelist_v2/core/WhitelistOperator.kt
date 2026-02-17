package plutoproject.feature.whitelist_v2.core

import java.util.UUID

sealed class WhitelistOperator {
    data object Console : WhitelistOperator()

    class Administrator(
        val uniqueId: UUID,
    ) : WhitelistOperator()
}
