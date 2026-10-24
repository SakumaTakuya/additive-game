package dev.seirei.core.database

import androidx.room.useReaderConnection
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SchemaTest {
    @Test
    fun allFiveTablesExist() = runBlocking {
        val database = createInMemoryDatabase()
        try {
            val tables = database.useReaderConnection { connection ->
                connection.usePrepared(
                    "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'",
                ) { statement ->
                    buildSet {
                        while (statement.step()) add(statement.getText(0))
                    }
                }
            }

            assertTrue(tables.containsAll(setOf("session", "resident", "mana", "collection_receipt", "app_settings")))
            assertEquals(5, tables.count { it in EXPECTED_TABLES })
        } finally {
            database.close()
        }
    }

    @Test
    fun manaRejectsIdOtherThanOne() = runBlocking {
        val database = createInMemoryDatabase()
        try {
            val failure = runCatching { database.manaDao().upsert(ManaEntity(id = 2, balance = 0, lifetimeEarned = 0)) }
                .exceptionOrNull()
            assertTrue("Expected the mana id constraint to reject id=2, got $failure", failure?.message.orEmpty().contains("mana.id must be 1"))
        } finally {
            database.close()
        }
    }

    @Test
    fun appSettingsRejectsIdOtherThanOne() = runBlocking {
        val database = createInMemoryDatabase()
        try {
            val failure = runCatching {
                database.appSettingsDao().upsert(
                    AppSettingsEntity(id = 2, regionCode = null, weatherKind = null, weatherFetchedAt = null),
                )
            }.exceptionOrNull()
            assertTrue(
                "Expected the app_settings id constraint to reject id=2, got $failure",
                failure?.message.orEmpty().contains("app_settings.id must be 1"),
            )
        } finally {
            database.close()
        }
    }

    private companion object {
        val EXPECTED_TABLES = setOf("session", "resident", "mana", "collection_receipt", "app_settings")
    }
}
