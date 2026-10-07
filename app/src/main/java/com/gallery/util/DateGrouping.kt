package com.gallery.util

import android.content.Context
import com.gallery.R
import com.gallery.domain.model.MediaItem
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

enum class DensityViewMode(val labelRes: Int, val columns: Int) {
    YEARS(R.string.density_years, 5),
    MONTHS(R.string.density_months, 4),
    DAYS(R.string.density_days, 3),
    ALL(R.string.density_all, 4),
}

data class MediaDateGroup(
    val key: String,
    val label: String,
    val items: List<MediaItem>,
    val subLabel: String? = null,
    val date: LocalDate = LocalDate.now(),
)

fun groupMediaByDensity(
    items: List<MediaItem>,
    mode: DensityViewMode,
    context: Context,
    zoneId: ZoneId = ZoneId.systemDefault(),
): List<MediaDateGroup> {
    if (items.isEmpty()) return emptyList()

    return when (mode) {
        DensityViewMode.YEARS -> {
            items
                .groupBy {
                    Instant.ofEpochMilli(it.dateTakenMillis).atZone(zoneId).year
                }
                .toSortedMap(compareByDescending { it })
                .map { (year, group) ->
                    MediaDateGroup(
                        key = "year_$year",
                        label = year.toString(),
                        subLabel = context.getString(R.string.photo_count, group.size),
                        items = group,
                        date = LocalDate.of(year, 1, 1),
                    )
                }
        }
        DensityViewMode.MONTHS -> {
            val monthFormatter = DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault())
            items
                .groupBy {
                    val dt = Instant.ofEpochMilli(it.dateTakenMillis).atZone(zoneId).toLocalDate()
                    YearMonth.from(dt)
                }
                .toSortedMap(compareByDescending { it })
                .map { (yearMonth, group) ->
                    val rawLabel = yearMonth.format(monthFormatter)
                    val label = rawLabel.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                    MediaDateGroup(
                        key = "month_${yearMonth.year}_${yearMonth.monthValue}",
                        label = label,
                        subLabel = context.getString(R.string.photo_count, group.size),
                        items = group,
                        date = yearMonth.atDay(1),
                    )
                }
        }
        DensityViewMode.DAYS -> {
            val today = LocalDate.now(zoneId)
            val yesterday = today.minusDays(1)
            val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(Locale.getDefault())
            items
                .groupBy { Instant.ofEpochMilli(it.dateTakenMillis).atZone(zoneId).toLocalDate() }
                .toSortedMap(compareByDescending { it })
                .map { (date, group) ->
                    val label = when (date) {
                        today -> context.getString(R.string.date_today)
                        yesterday -> context.getString(R.string.date_yesterday)
                        else -> date.format(formatter)
                    }
                    MediaDateGroup(
                        key = "day_$date",
                        label = label,
                        subLabel = context.getString(R.string.photo_count, group.size),
                        items = group,
                        date = date,
                    )
                }
        }
        DensityViewMode.ALL -> {
            val today = LocalDate.now(zoneId)
            val yesterday = today.minusDays(1)
            val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())
            items
                .groupBy { Instant.ofEpochMilli(it.dateTakenMillis).atZone(zoneId).toLocalDate() }
                .toSortedMap(compareByDescending { it })
                .map { (date, group) ->
                    val label = when (date) {
                        today -> context.getString(R.string.date_today)
                        yesterday -> context.getString(R.string.date_yesterday)
                        else -> date.format(formatter)
                    }
                    MediaDateGroup(
                        key = "all_$date",
                        label = label,
                        subLabel = context.getString(R.string.photo_count, group.size),
                        items = group,
                        date = date,
                    )
                }
        }
    }
}

fun groupMediaByDate(
    items: List<MediaItem>,
    context: Context,
    zoneId: ZoneId = ZoneId.systemDefault(),
): List<MediaDateGroup> {
    return groupMediaByDensity(items, DensityViewMode.DAYS, context, zoneId)
}
