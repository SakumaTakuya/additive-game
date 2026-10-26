package dev.seirei.core.database

import dev.seirei.core.model.ColorTier
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * ニックネーム更新（設計書03 §4.5）。空文字は NULL に正規化される。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NicknameDaoTest {
    @Test
    fun emptyNicknameIsStoredAsNull() = runBlocking {
        val database = createInMemoryDatabase()
        try {
            val dao = database.residentDao()
            val id = dao.insert(aResident(nickname = "もち"))

            dao.updateNickname(id, "")

            assertNull(dao.getById(id)?.nickname)
        } finally {
            database.close()
        }
    }

    @Test
    fun nicknameIsTrimmedAndControlCharactersAreRemoved() = runBlocking {
        val database = createInMemoryDatabase()
        try {
            val dao = database.residentDao()
            val id = dao.insert(aResident())

            dao.updateNickname(id, "  もち\nまる  ")

            assertEquals("もちまる", dao.getById(id)?.nickname)
        } finally {
            database.close()
        }
    }

    private fun aResident(nickname: String? = null) = ResidentEntity(
        motifId = 1,
        star = 3,
        personalityId = 0,
        colorTier = ColorTier.NORMAL,
        acquiredAt = Instant.parse("2026-10-25T00:00:00Z"),
        sourceSessionId = 1L,
        nickname = nickname,
    )
}
