package com.gallery.util

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

private val fullDateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(Locale("vi", "VN"))

fun groupMediaByDate(items: List<MediaItem>, zoneId: ZoneId = ZoneId.systemDefault()): List<MediaDateGroup> {
    val today = LocalDate.now(zoneId)
    val yesterday = today.minusDays(1)
    return items
        .groupBy { Instant.ofEpochMilli(it.dateTakenMillis).atZone(zoneId).toLocalDate() }
        .toSortedMap(compareByDescending { it })
        .map { (date, group) ->
            val label = when (date) {
                today -> "Hôm nay"
                yesterday -> "Hôm qua"
                else -> date.format(fullDateFormatter)
            }
            MediaDateGroup(date = date, label = label, items = group)
        }
}
