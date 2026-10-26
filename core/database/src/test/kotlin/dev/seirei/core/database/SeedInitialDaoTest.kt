package dev.seirei.core.database

import dev.seirei.core.model.ColorTier
import dev.seirei.core.model.TimeBand
import dev.seirei.core.model.WeatherKind
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 初期付与の冪等性（設計書03 §4.6）。2回呼んでも1体だけが付与される。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SeedInitialDaoTest {
    @Test
    fun callingTwiceGrantsOnlyOneResident() = runBlocking {
        val database = createInMemoryDatabase()
        try {
            val dao = database.seedInitialDao()

            assertTrue(dao.insertInitial(aResident()))
            assertFalse(dao.insertInitial(aResident()))

            assertEquals(1, database.residentDao().count())
        } finally {
            database.close()
        }
    }

    @Test
    fun existingSessionRowBlocksTheGrant() = runBlocking {
        val database = createInMemoryDatabase()
        try {
            database.sessionDao().upsert(
                SessionEntity(
                    id = 1L,
                    startedAt = Instant.parse("2026-10-25T00:00:00Z"),
                    endAt = Instant.parse("2026-10-25T02:00:00Z"),
                    seed = 42L,
                    snapshotDate = LocalDate.of(2026, 10, 25),
                    snapshotTimeBand = TimeBand.DAY,
                    snapshotWeather = WeatherKind.UNKNOWN,
                    partyIds = emptyList(),
                    collected = true,
                ),
            )

            assertFalse(database.seedInitialDao().insertInitial(aResident()))
            assertEquals(0, database.residentDao().count())
        } finally {
            database.close()
        }
    }

    private fun aResident() = ResidentEntity(
        motifId = 1,
        star = 3,
        personalityId = 0,
        colorTier = ColorTier.NORMAL,
        acquiredAt = Instant.parse("2026-10-25T00:00:00Z"),
        sourceSessionId = -2L,
    )
}
