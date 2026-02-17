package plutoproject.feature.whitelist_v2.core

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import plutoproject.feature.whitelist_v2.api.WhitelistOperator
import plutoproject.feature.whitelist_v2.api.WhitelistRevokeReason
import plutoproject.feature.whitelist_v2.core.usecase.GrantWhitelistUseCase
import java.time.Instant

class GrantWhitelistUseCaseTest {
    @Test
    fun `should return false when already active`() = runBlocking {
        val clock = fixedClock("2026-02-07T00:00:00Z")
        val uid = dummyUuid(1)
        val whitelistRepo = InMemoryWhitelistRecordRepository(
            records = mutableMapOf(
                uid to WhitelistRecordData(
                    uniqueId = uid,
                    username = "old",
                    granter = WhitelistOperator.Console,
                    createdAt = Instant.parse("2026-02-06T00:00:00Z"),
                    joinedAsVisitorBefore = false,
                    isMigrated = false,
                    isRevoked = false,
                    revoker = null,
                    revokeReason = null,
                    revokeAt = null,
                )
            )
        )
        val visitorRepo = InMemoryVisitorRecordRepository()
        val useCase = GrantWhitelistUseCase(whitelistRepo, visitorRepo, clock)

        val ok = useCase.execute(
            uniqueId = uid,
            username = "new",
            operator = WhitelistOperator.Administrator(dummyUuid(0xAA)),
        )

        assertFalse(ok)
        assertEquals(
            "old",
            whitelistRepo.records.getValue(uid).username
        )
    }

    @Test
    fun `should reactivate revoked record and keep createdAt`() = runBlocking {
        val clock = fixedClock("2026-02-07T00:00:00Z")
        val uid = dummyUuid(2)
        val whitelistRepo = InMemoryWhitelistRecordRepository(
            records = mutableMapOf(
                uid to WhitelistRecordData(
                    uniqueId = uid,
                    username = "old",
                    granter = WhitelistOperator.Console,
                    createdAt = Instant.parse("2026-01-01T00:00:00Z"),
                    joinedAsVisitorBefore = false,
                    isMigrated = true,
                    isRevoked = true,
                    revoker = WhitelistOperator.Console,
                    revokeReason = WhitelistRevokeReason.VIOLATION,
                    revokeAt = Instant.parse("2026-02-01T00:00:00Z"),
                )
            )
        )
        val visitorRepo = InMemoryVisitorRecordRepository(hasByUniqueId = setOf(uid))
        val useCase = GrantWhitelistUseCase(whitelistRepo, visitorRepo, clock)

        val ok = useCase.execute(uid, "newname", WhitelistOperator.Console)
        assertTrue(ok)

        val updated = whitelistRepo.records.getValue(uid)
        assertEquals("newname", updated.username)
        assertEquals(Instant.parse("2026-01-01T00:00:00Z"), updated.createdAt)
        assertTrue(updated.joinedAsVisitorBefore)
        assertFalse(updated.isRevoked)
        assertNull(updated.revoker)
        assertNull(updated.revokeReason)
        assertNull(updated.revokeAt)
        assertTrue(updated.isMigrated)
    }
}
