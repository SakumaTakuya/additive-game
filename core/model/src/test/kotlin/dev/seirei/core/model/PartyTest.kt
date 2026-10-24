package dev.seirei.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/** 編成（設計書02 §2・§5.2）。順序付きで先頭が隊長、0〜3体。 */
class PartyTest {
    private fun member(motifId: Int) = PartyMember(motifId = motifId, star = 1, personalityId = 0)

    // ---- 代表例 ----

    @Test fun `先頭の住人が隊長`() {
        val leader = PartyMember(motifId = 1, star = 3, personalityId = 2)
        val party = Party(listOf(leader, PartyMember(motifId = 2, star = 4, personalityId = 3)))

        assertEquals(leader, party.leader)
    }

    // ---- 境界 ----

    @Test fun `空編成も有効で、隊長はいない`() {
        assertEquals(null, Party(emptyList()).leader)
    }

    @Test fun `3体までは組める`() {
        assertEquals(3, Party((1..3).map(::member)).members.size)
    }

    // ---- 異常系 ----

    @Test fun `4体以上は例外`() {
        assertThrows(IllegalArgumentException::class.java) { Party((1..4).map(::member)) }
    }

    // ---- 保証 ----

    @Test fun `元のリストを変更しても編成は変わらず、編成からも変更できない`() {
        val source = (1..3).map(::member).toMutableList()
        val party = Party(source)

        source += member(4)

        assertEquals(3, party.members.size)
        @Suppress("UNCHECKED_CAST")
        val exposedMutableMembers = party.members as MutableList<PartyMember>
        assertThrows(UnsupportedOperationException::class.java) { exposedMutableMembers += member(5) }
    }
}
