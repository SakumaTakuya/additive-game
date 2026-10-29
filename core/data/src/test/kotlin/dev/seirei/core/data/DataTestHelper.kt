package dev.seirei.core.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dev.seirei.core.database.SeireiDatabase
import org.robolectric.RuntimeEnvironment

/** :core:data のテスト用インメモリ DB（:core:database の DatabaseTestHelper と同じ構成）。 */
internal fun createInMemoryDatabase(): SeireiDatabase =
    Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), SeireiDatabase::class.java)
        .setDriver(BundledSQLiteDriver())
        .addCallback(SeireiDatabase.SINGLETON_ID_CONSTRAINTS)
        .build()
