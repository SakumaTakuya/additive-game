package dev.seirei.core.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dev.seirei.core.database.SeireiDatabase
import org.robolectric.RuntimeEnvironment

/**
 * :core:data のテスト用インメモリ DB（:core:database の DatabaseTestHelper と同じ構成）。
 * シングルトン制約トリガーのコールバックは :core:database の internal で参照できないため付けない。
 * 制約自体は :core:database のテストで担保されており、リポジトリのテストには不要。
 */
internal fun createInMemoryDatabase(): SeireiDatabase =
    Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), SeireiDatabase::class.java)
        .setDriver(BundledSQLiteDriver())
        .build()
