package plutoproject.feature.whitelist_v2.core

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import plutoproject.feature.whitelist_v2.core.usecase.RevokeWhitelistUseCase
import java.time.Instant

class RevokeWhitelistUseCaseTest {
    @Test
    fun `should set revocation fields when record is active`() = runTest {
        val clock = fixedClock("2026-02-07T12:00:00Z")
        val uid = dummyUuid(3)
        val whitelistRepo = InMemoryWhitelistRecordRepository(
            records = mutableMapOf(
                uid to WhitelistRecord(
                    uniqueId = uid,
                    username = "abc",
                    granter = WhitelistOperator.Console,
                    createdAt = Instant.parse("2026-02-01T00:00:00Z"),
                    joinedAsVisitorBefore = false,
                    isMigrated = false,
                    isRevoked = false,
                    revoker = null,
                    revokeReason = null,
                    revokeAt = null,
                )
            )
        )
        val useCase = RevokeWhitelistUseCase(whitelistRepo, clock)

        val result = useCase.execute(uid, WhitelistOperator.Console, WhitelistRevokeReason.REQUESTED)
        assertEquals(result, RevokeWhitelistUseCase.Result.Ok)

        val updated = whitelistRepo.records.getValue(uid)
        assertTrue(updated.isRevoked)
        assertEquals(WhitelistOperator.Console, updated.revoker)
        assertEquals(WhitelistRevokeReason.REQUESTED, updated.revokeReason)
        assertEquals(Instant.parse("2026-02-07T12:00:00Z"), updated.revokeAt)
    }
}
