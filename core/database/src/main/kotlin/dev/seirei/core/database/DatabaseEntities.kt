package dev.seirei.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.seirei.core.model.ColorTier
import dev.seirei.core.model.TimeBand
import dev.seirei.core.model.WeatherKind
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "session",
    indices = [Index(value = ["id"], unique = true)],
)
data class SessionEntity(
    @PrimaryKey val slot: Int = SINGLETON_SLOT,
    val id: Long,
    val startedAt: Instant,
    val endAt: Instant,
    val seed: Long,
    val snapshotDate: LocalDate,
    val snapshotTimeBand: TimeBand,
    val snapshotWeather: WeatherKind,
    val partyIds: List<Long>,
    val collected: Boolean,
) {
    companion object {
        const val SINGLETON_SLOT = 1
    }
}

@Entity(tableName = "resident")
data class ResidentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val motifId: Int,
    val star: Int,
    val personalityId: Int,
    val colorTier: ColorTier,
    val acquiredAt: Instant,
    val sourceSessionId: Long,
    @ColumnInfo(defaultValue = "0") val locked: Boolean = false,
    val nickname: String? = null,
)

@Entity(tableName = "mana")
data class ManaEntity(
    @PrimaryKey val id: Int = SINGLETON_ROW_ID,
    val balance: Int,
    val lifetimeEarned: Int,
) {
    companion object {
        const val SINGLETON_ROW_ID = 1
    }
}

@Entity(tableName = "collection_receipt")
data class CollectionReceiptEntity(
    @PrimaryKey val sessionId: Long,
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = SINGLETON_ROW_ID,
    val regionCode: String? = null,
    val weatherKind: WeatherKind? = null,
    val weatherFetchedAt: Instant? = null,
) {
    companion object {
        const val SINGLETON_ROW_ID = 1
    }
}
