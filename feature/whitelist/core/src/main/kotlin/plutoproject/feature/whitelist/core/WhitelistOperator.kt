package plutoproject.feature.whitelist.core

import java.util.UUID

sealed class WhitelistOperator {
    data object Console : WhitelistOperator()

    class Administrator(
        val uniqueId: UUID,
    ) : WhitelistOperator()
}
