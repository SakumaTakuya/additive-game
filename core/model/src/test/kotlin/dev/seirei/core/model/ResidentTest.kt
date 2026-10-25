package dev.seirei.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant

/** 住人個体（設計書03 §3）。表示名とニックネームの規則。 */
class ResidentTest {

    // ---- 代表例 ----

    @Test fun `表示名はニックネームがあればそれ、なければモチーフ名`() {
        assertEquals("わたしの精霊", resident(nickname = "わたしの精霊").displayName)
        assertEquals("スライムの精霊", resident().displayName)
    }

    // ---- バリエーション ----

    @Test fun `ニックネームは前後の空白を除き、空白だけなら未設定扱い`() {
        assertEquals("名前", resident(nickname = "  名前  ").nickname)
        assertEquals(null, resident(nickname = "   ").nickname)
        assertEquals(null, resident().nickname)
    }

    // ---- 異常系 ----

    @Test fun `ニックネームが10文字を超えると例外（DEC-10）`() {
        assertThrows(IllegalArgumentException::class.java) { resident(nickname = "あいうえおかきくけこあ") }
    }

    // ---- 保証 ----

    @Test fun `指名覚醒と初期ギフトの入手元IDは予約済み`() {
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
