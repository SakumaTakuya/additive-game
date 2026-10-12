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

    @Test
    fun `party is unchanged when the source list is mutated`() {
        val source = mutableListOf(
            PartyMember(motifId = 1, star = 1, personalityId = 1),
            PartyMember(motifId = 2, star = 1, personalityId = 1),
            PartyMember(motifId = 3, star = 1, personalityId = 1),
        )
        val party = Party(source)

        source += PartyMember(motifId = 4, star = 1, personalityId = 1)

        assertEquals(3, party.members.size)
        @Suppress("UNCHECKED_CAST")
        val exposedMutableMembers = party.members as MutableList<PartyMember>
        assertThrows(UnsupportedOperationException::class.java) {
            exposedMutableMembers += PartyMember(motifId = 5, star = 1, personalityId = 1)
        }
    }
}
