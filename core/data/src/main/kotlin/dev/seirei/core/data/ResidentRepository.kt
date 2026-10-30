package dev.seirei.core.data

import dev.seirei.core.database.SeireiDatabase
import dev.seirei.core.model.Resident
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * 住人の単一列更新（設計書03 §4.5）と購読クエリ（設計書03 §5）。
 * ニックネームの正規化は ResidentDao 側で行う。UI 上の呼称は「保護」（DEC-10）。
 */
class ResidentRepository(
    private val database: SeireiDatabase,
) {
    /** 全住人の購読。Widget 描画の入力（設計書03 §5）。 */
    val residentsFlow: Flow<List<Resident>> =
        database.residentDao().observeAll().map { rows -> rows.map { row -> row.toDomain() } }

    /**
     * 留守番組の購読（設計書01 §6.2）：現在のセッション行の編成（partyIds）に
     * 入っていない住人。編成が変わると即座に流れる値も変わる。
     */
    val caretakersFlow: Flow<List<Resident>> =
        combine(residentsFlow, database.sessionDao().observe()) { residents, session ->
            val partyIds = session?.partyIds?.toSet().orEmpty()
            residents.filter { resident -> resident.id !in partyIds }
        }.distinctUntilChanged()

    /**
     * 編成の購読：セッション行の partyIds を順序どおりに住人へ展開する（[0] が隊長）。
     * プリセットに残っている削除済みの住人は落とす。
     */
    val partyFlow: Flow<List<Resident>> =
        combine(residentsFlow, database.sessionDao().observe()) { residents, session ->
            val byId = residents.associateBy { resident -> resident.id }
            session?.partyIds.orEmpty().mapNotNull { id -> byId[id] }
        }.distinctUntilChanged()

    /** 発見済みモチーフの購読（図鑑・指名候補。設計書03 §3）：住人のモチーフの重複なし集合。 */
    val discoveredMotifsFlow: Flow<Set<Int>> =
        residentsFlow.map { residents -> residents.map { resident -> resident.motifId }.toSet() }
            .distinctUntilChanged()

    suspend fun updateNickname(residentId: Long, nickname: String?) {
        database.residentDao().updateNickname(residentId, nickname)
    }

    suspend fun setLocked(residentId: Long, locked: Boolean) {
        database.residentDao().updateLocked(residentId, locked)
    }
}
