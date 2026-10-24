package dev.seirei.core.database

import dev.seirei.core.model.TimeBand
import dev.seirei.core.model.WeatherKind
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SessionDaoTest {
    @Test
    fun upsertReplacesTheSingleSlotForANewSessionId() = runBlocking {
        val database = createInMemoryDatabase()
        try {
            val dao = database.sessionDao()
            dao.upsert(session(id = 41, seed = 100))
            dao.upsert(session(id = 42, seed = 200))

            assertEquals(1, dao.count())
            assertEquals(42L, dao.get()?.id)
            assertEquals(200L, dao.get()?.seed)
            assertEquals(listOf(7L, 9L), dao.get()?.partyIds)
        } finally {
            database.close()
        }
    }

    private fun session(id: Long, seed: Long) = SessionEntity(
        slot = 1,
        id = id,
        startedAt = Instant.parse("2026-10-24T00:00:00Z"),
        endAt = Instant.parse("2026-10-24T02:00:00Z"),
        seed = seed,
        snapshotDate = LocalDate.parse("2026-10-24"),
        snapshotTimeBand = TimeBand.DAY,
        snapshotWeather = WeatherKind.UNKNOWN,
        partyIds = listOf(7, 9),
        collected = false,
    )
}
