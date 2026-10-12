package dev.seirei.core.model

import java.time.Instant
import java.util.Collections

const val NAMED_SOURCE: Long = -1L
const val INITIAL_GIFT_SOURCE: Long = -2L

private const val MAX_NICKNAME_LENGTH = 10 // TODO(DEC-10): 叩き台

/** 永続化する住人個体の事実（設計書03 §3）。 */
class Resident(
    val id: Long,
    val motifId: Int,
    val star: Int,
    val personalityId: Int,
    val colorTier: ColorTier,
    val acquiredAt: Instant,
    val sourceSessionId: Long,
    val locked: Boolean,
    nickname: String? = null,
) {
    val nickname: String? = normalizeNickname(nickname)

    val displayName: String
        get() = nickname ?: Motif.ALL.single { it.id == motifId }.displayName
}

/** エンジンへ渡す住人の不変属性だけを持つ（設計書02 §2）。 */
data class PartyMember(
    val motifId: Int,
    val star: Int,
    val personalityId: Int,
)

/** 順序付き編成。[0] が隊長で、空編成も有効（設計書02 §2・§5.2）。 */
class Party(members: List<PartyMember>) {
    val members: List<PartyMember> = Collections.unmodifiableList(ArrayList(members))

    init {
        require(this.members.size <= MAX_MEMBERS) { "A party can contain at most $MAX_MEMBERS members" }
    }

    val leader: PartyMember?
        get() = members.firstOrNull()

    override fun equals(other: Any?): Boolean = other is Party && members == other.members

    override fun hashCode(): Int = members.hashCode()

    override fun toString(): String = "Party(members=$members)"

    private companion object {
        const val MAX_MEMBERS = 3
    }
}

/** 前後の空白を除き、空文字は null とする。上限は DEC-10 の推奨値。 */
fun normalizeNickname(nickname: String?): String? {
    val normalized = nickname?.trim()?.takeIf(String::isNotEmpty) ?: return null
    require(normalized.length <= MAX_NICKNAME_LENGTH) {
        "Nickname must be at most $MAX_NICKNAME_LENGTH characters"
    }
    return normalized
}
