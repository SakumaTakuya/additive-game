package dev.seirei.core.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import org.robolectric.RuntimeEnvironment

internal fun createInMemoryDatabase(): SeireiDatabase =
    Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), SeireiDatabase::class.java)
        .setDriver(BundledSQLiteDriver())
        .addCallback(SeireiDatabase.SINGLETON_ID_CONSTRAINTS)
        .build()
