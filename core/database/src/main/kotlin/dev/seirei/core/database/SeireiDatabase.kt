package dev.seirei.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

@Database(
    entities = [
        SessionEntity::class,
        ResidentEntity::class,
        ManaEntity::class,
        CollectionReceiptEntity::class,
        AppSettingsEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(RoomConverters::class)
abstract class SeireiDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun collectDao(): CollectDao
    abstract fun residentDao(): ResidentDao
    abstract fun manaDao(): ManaDao
    abstract fun collectionReceiptDao(): CollectionReceiptDao
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        internal val SINGLETON_ID_CONSTRAINTS = object : Callback() {
            override fun onCreate(connection: SQLiteConnection) {
                // Room's @Entity API cannot declare CHECK constraints. These triggers enforce
                // the same persisted invariant and run for both the Android and bundled drivers.
                connection.execSQL(
                    """CREATE TRIGGER mana_id_must_be_one_insert BEFORE INSERT ON mana
                       WHEN NEW.id != 1 BEGIN SELECT RAISE(ABORT, 'mana.id must be 1'); END""".trimIndent(),
                )
                connection.execSQL(
                    """CREATE TRIGGER mana_id_must_be_one_update BEFORE UPDATE OF id ON mana
                       WHEN NEW.id != 1 BEGIN SELECT RAISE(ABORT, 'mana.id must be 1'); END""".trimIndent(),
                )
                connection.execSQL(
                    """CREATE TRIGGER app_settings_id_must_be_one_insert BEFORE INSERT ON app_settings
                       WHEN NEW.id != 1 BEGIN SELECT RAISE(ABORT, 'app_settings.id must be 1'); END""".trimIndent(),
                )
                connection.execSQL(
                    """CREATE TRIGGER app_settings_id_must_be_one_update BEFORE UPDATE OF id ON app_settings
                       WHEN NEW.id != 1 BEGIN SELECT RAISE(ABORT, 'app_settings.id must be 1'); END""".trimIndent(),
                )
                connection.execSQL(
                    """CREATE TRIGGER session_slot_must_be_one_insert BEFORE INSERT ON `session`
                       WHEN NEW.slot != 1 BEGIN SELECT RAISE(ABORT, 'session.slot must be 1'); END""".trimIndent(),
                )
                connection.execSQL(
                    """CREATE TRIGGER session_slot_must_be_one_update BEFORE UPDATE OF slot ON `session`
                       WHEN NEW.slot != 1 BEGIN SELECT RAISE(ABORT, 'session.slot must be 1'); END""".trimIndent(),
                )
            }
        }

        fun open(context: Context, databaseName: String): SeireiDatabase =
            Room.databaseBuilder(context.applicationContext, SeireiDatabase::class.java, databaseName)
                .setDriver(BundledSQLiteDriver())
                .fallbackToDestructiveMigration(dropAllTables = true)
                .addCallback(SINGLETON_ID_CONSTRAINTS)
                .build()
    }
}
