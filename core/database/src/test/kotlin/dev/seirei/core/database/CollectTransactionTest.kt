package dev.seirei.core.database

import dev.seirei.core.model.ColorTier
import dev.seirei.core.model.TimeBand
import dev.seirei.core.model.WeatherKind
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 回収トランザクション（設計書03 §4.1）。
 * レシート→住人→魔力→回収済みの4書込が1トランザクションで、レシートの主キー衝突で
 * 全体がロールバックし、二重加算が起きないことを確かめる。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CollectTransactionTest {
    private val sessionId = 7L

    @Test
    fun `回収するとレシート・住人・魔力が記録され回収済みになる`() = runBlocking {
        val database = createInMemoryDatabase()
        try {
            seed(database, manaBalance = 100)

            database.collectDao().collect(
                sessionId = sessionId,
                awakened = listOf(awakenedEntity(motifId = 1), awakenedEntity(motifId = 2)),
                manaGained = 60,
            )

            assertTrue(database.collectionReceiptDao().exists(sessionId))
            assertEquals(2, database.residentDao().count())
            val mana = database.manaDao().get()
            assertEquals(160, mana?.balance)
            assertEquals(160, mana?.lifetimeEarned)
            assertTrue(database.sessionDao().get()?.collected == true)
        } finally {
            database.close()
        }
    }

    @Test
    fun `同じセッションを二度回収すると失敗し二重に加算されない`() = runBlocking {
        val database = createInMemoryDatabase()
        try {
            seed(database, manaBalance = 100)
            val dao = database.collectDao()
            dao.collect(sessionId, listOf(awakenedEntity(motifId = 1)), manaGained = 60)

            val failure = runCatching {
                dao.collect(sessionId, listOf(awakenedEntity(motifId = 3)), manaGained = 60)
            }.exceptionOrNull()

            assertNotNull(failure)
            assertEquals(1, database.residentDao().count())
            assertEquals(160, database.manaDao().get()?.balance)
        } finally {
            database.close()
        }
    }

    @Test
    fun `住人の挿入に失敗すると全体がロールバックされる`() = runBlocking {
        val database = createInMemoryDatabase()
        try {
            seed(database, manaBalance = 100)
            val duplicateIds = listOf(
                awakenedEntity(motifId = 1).copy(id = 900L),
                awakenedEntity(motifId = 2).copy(id = 900L),
            )

            val failure = runCatching {
                database.collectDao().collect(sessionId, duplicateIds, manaGained = 60)
            }.exceptionOrNull()

            assertNotNull(failure)
            assertFalse(database.collectionReceiptDao().exists(sessionId))
            assertEquals(0, database.residentDao().count())
            assertEquals(100, database.manaDao().get()?.balance)
            assertTrue(database.sessionDao().get()?.collected == false)
        } finally {
            database.close()
        }
    }

    @Test
    fun `魔力の行がないときは回収で新しい行が作られる`() = runBlocking {
        val database = createInMemoryDatabase()
        try {
            seed(database, manaBalance = null)

            database.collectDao().collect(sessionId, emptyList(), manaGained = 45)

            val mana = database.manaDao().get()
            assertEquals(45, mana?.balance)
            assertEquals(45, mana?.lifetimeEarned)
        } finally {
            database.close()
        }
    }

    private suspend fun seed(database: SeireiDatabase, manaBalance: Int?) {
        database.sessionDao().upsert(
            SessionEntity(
                id = sessionId,
                startedAt = Instant.parse("2026-10-25T00:00:00Z"),
                endAt = Instant.parse("2026-10-25T02:00:00Z"),
                seed = 42L,
                snapshotDate = LocalDate.of(2026, 10, 25),
                snapshotTimeBand = TimeBand.DAY,
                snapshotWeather = WeatherKind.UNKNOWN,
                partyIds = emptyList(),
                collected = false,
            ),
        )
        if (manaBalance != null) {
            database.manaDao().upsert(
                ManaEntity(balance = manaBalance, lifetimeEarned = manaBalance),
            )
        }
    }

    private fun awakenedEntity(motifId: Int): ResidentEntity = ResidentEntity(
        motifId = motifId,
        star = 3,
        personalityId = 0,
        colorTier = ColorTier.NORMAL,
        acquiredAt = Instant.parse("2026-10-25T02:00:00Z"),
        sourceSessionId = sessionId,
    )
}
