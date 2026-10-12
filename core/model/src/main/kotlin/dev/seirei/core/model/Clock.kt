package dev.seirei.core.model

import java.time.Instant

/**
 * 現在時刻の取得口（ADR D-007）。本番実装は :app の AppContainer に1つだけ置く。
 * テストでは時計を進める・戻すフェイクを渡す（設計書01 §7 異常系 #7・#8）。
 */
fun interface Clock {
    fun now(): Instant
}
