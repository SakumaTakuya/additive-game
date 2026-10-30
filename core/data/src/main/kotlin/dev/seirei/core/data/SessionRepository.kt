package dev.seirei.core.data

import dev.seirei.core.database.SeireiDatabase
import dev.seirei.core.domain.PartyPresetStore
import dev.seirei.core.domain.SessionStore
import dev.seirei.core.model.ExplorationSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * [SessionStore] の Room 実装（設計書03 §4.2）。置換は SessionWriteDao の
 * compare-and-replace で行い、読み取り後に現在行が変わっていたら書かない。
 * 前回編成の引き継ぎ（[PartyPresetStore]）は同じ単一行の partyIds から読む。
 */
class SessionRepository(
    private val database: SeireiDatabase,
) : SessionStore, PartyPresetStore {

    /** 現在のセッション行の購読（設計書03 §5）。三層サーフェスの導出元。 */
    val sessionFlow: Flow<ExplorationSession?> =
        database.sessionDao().observe().map { row -> row?.toDomain() }

    override suspend fun loadCurrent(): ExplorationSession? =
        database.sessionDao().get()?.toDomain()

    override suspend fun replaceCurrentIfUnchanged(
        expectedCurrent: ExplorationSession?,
        replacement: ExplorationSession,
    ): Boolean = database.sessionWriteDao().replaceIfUnchanged(
        expected = expectedCurrent?.toEntity(),
        replacement = replacement.toEntity(),
    )

    override suspend fun loadPreviousPartyIds(): List<Long> =
        database.sessionDao().get()?.partyIds.orEmpty()

    /**
     * 編成プリセットの即時保存（設計書05 §3.2）。保存先は [loadPreviousPartyIds] と
     * 同じセッション行の partyIds で、編成画面の変更をその場で永続化する。
     * セッション行が存在しないとき（初回点火前）は保存先がないため何もしない
     * （ダミー行を作ると設計書03 §4.6 のガードと衝突する。issue #50 の計画で決定）。
     */
    suspend fun savePartyPreset(partyIds: List<Long>) {
        val current = database.sessionDao().get() ?: return
        if (current.partyIds == partyIds) return
        database.sessionDao().upsert(current.copy(partyIds = partyIds))
    }
}
