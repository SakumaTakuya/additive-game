package dev.seirei.core.data

import dev.seirei.core.database.SeireiDatabase
import dev.seirei.core.domain.PartyPresetStore
import dev.seirei.core.domain.SessionStore
import dev.seirei.core.model.ExplorationSession

/**
 * [SessionStore] の Room 実装（設計書03 §4.2）。置換は SessionWriteDao の
 * compare-and-replace で行い、読み取り後に現在行が変わっていたら書かない。
 * 前回編成の引き継ぎ（[PartyPresetStore]）は同じ単一行の partyIds から読む。
 */
class SessionRepository(
    private val database: SeireiDatabase,
) : SessionStore, PartyPresetStore {
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
}
