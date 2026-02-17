package plutoproject.feature.whitelist_v2.core

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import plutoproject.feature.whitelist_v2.core.usecase.CreateVisitorRecordUseCase
import java.net.InetAddress
import java.net.InetSocketAddress
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

class CreateVisitorRecordUseCaseTest {
    @Test
    fun `should persist record and return it`() = runTest {
        val clock = fixedClock("2026-02-07T00:00:00Z")
        val visitorRepo = InMemoryVisitorRecordRepository()
        val useCase = CreateVisitorRecordUseCase(visitorRepo, clock)

        val uid = dummyUuid(4)
        val record = useCase.execute(
            uniqueId = uid,
            params = VisitorRecordParams(
                ipAddress = InetAddress.getByName("127.0.0.1"),
                virtualHost = InetSocketAddress("localhost", 25565),
                visitedAt = Instant.parse("2026-02-07T00:00:00Z"),
                duration = 5.seconds,
                visitedServers = listOf("lobby"),
            )
        )

        assertEquals(uid, record.uniqueId)
        assertEquals(1, visitorRepo.records.size)
        assertEquals(clock.instant(), visitorRepo.records.single().createdAt)
    }
}
