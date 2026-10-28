package dev.seirei.core.database

import dev.seirei.core.model.ColorTier
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 指名覚醒のトランザクション（設計書03 §4.3）。
 * 残高不足では何も変わらず、足りれば住人の追加と残高の減算が1トランザクションで起きる。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NamedAwakenDaoTest {
    @Test
    fun `魔力がコストに足りないと覚醒せず住人も残高も変わらない`() = runBlocking {
        val database = createInMemoryDatabase()
        try {
            seed(database, manaBalance = 399)
            database.residentDao().insert(aResident(motifId = 5))

            val awakened = database.namedAwakenDao().awaken(aResident(motifId = 5), cost = 400)

            assertFalse(awakened)
            assertEquals(1, database.residentDao().count())
            assertEquals(399, database.manaDao().get()?.balance)
        } finally {
            database.close()
        }
    }

    @Test
    fun `魔力が足りていれば住人が増えて残高からコストが引かれる`() = runBlocking {
        val database = createInMemoryDatabase()
        try {
            seed(database, manaBalance = 1_000)
            database.residentDao().insert(aResident(motifId = 5))

            val awakened = database.namedAwakenDao().awaken(aResident(motifId = 5), cost = 400)

            assertTrue(awakened)
            assertEquals(2, database.residentDao().count())
            val mana = database.manaDao().get()
            assertEquals(600, mana?.balance)
            assertEquals(1_000, mana?.lifetimeEarned)
        } finally {
            database.close()
        }
    }

    @Test
    fun `モチーフが未発見だと覚醒せず住人も残高も変わらない`() = runBlocking {
        val database = createInMemoryDatabase()
        try {
            seed(database, manaBalance = 1_000)

            val awakened = database.namedAwakenDao().awaken(aResident(motifId = 5), cost = 400)

            assertFalse(awakened)
            assertEquals(0, database.residentDao().count())
            assertEquals(1_000, database.manaDao().get()?.balance)
        } finally {
            database.close()
        }
    }

    private suspend fun seed(database: SeireiDatabase, manaBalance: Int) {
        database.manaDao().upsert(ManaEntity(balance = manaBalance, lifetimeEarned = manaBalance))
    }

    private fun aResident(motifId: Int) = ResidentEntity(
        motifId = motifId,
        star = 3,
        personalityId = 0,
        colorTier = ColorTier.NORMAL,
        acquiredAt = Instant.parse("2026-10-25T00:00:00Z"),
        sourceSessionId = -1L,
    )
}
