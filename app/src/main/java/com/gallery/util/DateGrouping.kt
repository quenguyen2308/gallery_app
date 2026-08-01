package com.gallery.util

import android.content.Context
import com.gallery.R
import com.gallery.domain.model.MediaItem
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

data class MediaDateGroup(
    val date: LocalDate,
    val label: String,
    val items: List<MediaItem>,
)

fun groupMediaByDate(items: List<MediaItem>, context: Context, zoneId: ZoneId = ZoneId.systemDefault()): List<MediaDateGroup> {
    val today = LocalDate.now(zoneId)
    val yesterday = today.minusDays(1)
    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(Locale.getDefault())
    return items
        .groupBy { Instant.ofEpochMilli(it.dateTakenMillis).atZone(zoneId).toLocalDate() }
        .toSortedMap(compareByDescending { it })
        .map { (date, group) ->
            val label = when (date) {
                today -> context.getString(R.string.date_today)
                yesterday -> context.getString(R.string.date_yesterday)
                else -> date.format(formatter)
            }
            MediaDateGroup(date = date, label = label, items = group)
        }
}
