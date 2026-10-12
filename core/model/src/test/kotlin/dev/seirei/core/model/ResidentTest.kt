package dev.seirei.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant

class ResidentTest {
    @Test
    fun `display name uses nickname when present and motif name otherwise`() {
        assertEquals("わたしの精霊", resident(nickname = "わたしの精霊").displayName)
        assertEquals("スライムの精霊", resident().displayName)
    }

    @Test
    fun `nickname is trimmed and blank nickname becomes null`() {
        assertEquals("名前", resident(nickname = "  名前  ").nickname)
        assertEquals(null, resident(nickname = "   ").nickname)
        assertEquals(null, resident().nickname)
    }

    @Test
    fun `nickname longer than ten characters fails`() {
        assertThrows(IllegalArgumentException::class.java) {
            resident(nickname = "あいうえおかきくけこあ")
        }
    }

    @Test
    fun `special source ids are reserved`() {
        assertEquals(-1L, NAMED_SOURCE)
        assertEquals(-2L, INITIAL_GIFT_SOURCE)
    }

    private fun resident(nickname: String? = null) = Resident(
        id = 1,
        motifId = 1,
        star = 3,
        personalityId = 2,
        colorTier = ColorTier.NORMAL,
        acquiredAt = Instant.parse("2026-10-12T00:00:00Z"),
        sourceSessionId = 42L,
        locked = false,
        nickname = nickname,
    )
}
