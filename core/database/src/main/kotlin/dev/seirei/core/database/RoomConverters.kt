package dev.seirei.core.database

import androidx.room.TypeConverter
import dev.seirei.core.model.ColorTier
import dev.seirei.core.model.TimeBand
import dev.seirei.core.model.WeatherKind
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate

class RoomConverters {
    @TypeConverter
    fun instantToEpochMillis(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun epochMillisToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun localDateToIsoString(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun isoStringToLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun timeBandToName(value: TimeBand?): String? = value?.name

    @TypeConverter
    fun nameToTimeBand(value: String?): TimeBand? = value?.let(TimeBand::valueOf)

    @TypeConverter
    fun weatherKindToName(value: WeatherKind?): String? = value?.name

    @TypeConverter
    fun nameToWeatherKind(value: String?): WeatherKind? = value?.let(WeatherKind::valueOf)

    @TypeConverter
    fun colorTierToName(value: ColorTier?): String? = value?.name

    @TypeConverter
    fun nameToColorTier(value: String?): ColorTier? = value?.let(ColorTier::valueOf)

    @TypeConverter
    fun partyIdsToJson(value: List<Long>?): String? = value?.let(Json::encodeToString)

    @TypeConverter
    fun jsonToPartyIds(value: String?): List<Long>? = value?.let { Json.decodeFromString<List<Long>>(it) }
}
