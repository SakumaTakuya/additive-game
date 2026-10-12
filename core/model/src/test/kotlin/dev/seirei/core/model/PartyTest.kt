package dev.seirei.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PartyTest {
    @Test
    fun `party accepts at most three members`() {
        val members = (1..3).map { PartyMember(motifId = it, star = 1, personalityId = 1) }

        assertThrows(IllegalArgumentException::class.java) {
            Party(members + PartyMember(motifId = 4, star = 1, personalityId = 1))
        }
    }

    @Test
    fun `leader is the first member`() {
        val leader = PartyMember(motifId = 1, star = 3, personalityId = 2)
        val party = Party(listOf(leader, PartyMember(motifId = 2, star = 4, personalityId = 3)))

        assertEquals(leader, party.leader)
    }

    @Test
    fun `empty party is allowed`() {
        assertEquals(null, Party(emptyList()).leader)
    }
}
