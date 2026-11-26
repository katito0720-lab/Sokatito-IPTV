package com.streamvault.feature.live.sports

import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale
import kotlin.math.abs

internal data class SportsPlaylistEvent(
    val title: String,
    val schedule: String?,
    val feed: String?,
    val numberedSlot: Boolean = false,
)

private val slot = Regex("""^LIVE EVENTS?\s+(\d+)\s*[-–:]\s*(.+)$""", RegexOption.IGNORE_CASE)
private val startClock = Regex("""^\d{1,2}(?::\d{2})?\s*[ap]m\b""", RegexOption.IGNORE_CASE)
private val datedLocalTime = Regex(
    """([A-Za-z]{3} \d{1,2} [A-Za-z]{3} \d{1,2}:\d{2}) (GMT|UTC|EST|EDT|CST|CDT|MST|MDT|PST|PDT)(?: \((US|CA)\))?""",
    RegexOption.IGNORE_CASE,
)

internal fun isInactiveSportsListing(name: String): Boolean {
    val label = name.trim()
    val prefix = label.substringBefore('|').trim()
    return label.startsWith("###") || prefix.equals("End", ignoreCase = true) ||
        label.startsWith("No event streaming", ignoreCase = true) ||
        slot.matchEntire(label)?.groupValues?.get(2)?.trim()?.equals("No event", ignoreCase = true) == true
}

internal fun parsePlaylistSportsEvent(name: String): SportsPlaylistEvent? {
    val label = name.trim()
    slot.matchEntire(label)?.let {
        val description = it.groupValues[2].trim()
        val time = startClock.find(description)?.value
        val title = if (time == null) description else description.removePrefix(time).trim()
        return title.takeIf(String::isNotBlank)?.let { event ->
            SportsPlaylistEvent(event, time, label.substringBefore(" - ").takeIf { it != label }, numberedSlot = true)
        }
    }
    val fields = label.split('|').map(String::trim)
    if (!fields.first().equals("Live", ignoreCase = true) || fields.size < 3) return null
    val title = fields[1].takeIf(String::isNotBlank) ?: return null
    val schedule = fields[2].takeIf { datedLocalTime.matches(it) }
    return SportsPlaylistEvent(title, schedule, fields.lastOrNull()?.takeIf { fields.size >= 4 && it.isNotBlank() })
}

private val datedClock = Regex("""^(\d{4}-\d{2}-\d{2})\s+(?:·\s+)?(\d{1,2}:\d{2}(?:\s*[AP]M)?)(?:\s+\(([A-Z]+)\))?$""", RegexOption.IGNORE_CASE)
private val monthFirstClock = Regex("""^([A-Z]{3}) (\d{1,2}) (\d{1,2}:\d{2}\s*[AP]M)$""", RegexOption.IGNORE_CASE)
private val providerClock = Regex("""^([A-Z]{3}) (\d{1,2}) ([A-Z]{3}) (\d{1,2}:\d{2}) ([A-Z]+)(?: \((?:US|CA)\))?$""", RegexOption.IGNORE_CASE)
private val monthFormat = DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("MMM").toFormatter(Locale.ENGLISH)
private val clock12 = DateTimeFormatter.ofPattern("h:mma", Locale.ENGLISH)
private val clock24 = DateTimeFormatter.ofPattern("H:mm", Locale.ENGLISH)
private val offsets = mapOf("GMT" to 0, "UTC" to 0, "EST" to -5, "EDT" to -4, "CST" to -6,
    "CDT" to -5, "MST" to -7, "MDT" to -6, "PST" to -8, "PDT" to -7)

/** Only a dated listing can place a playlist channel in Today or Upcoming. */
internal fun listedSportsStart(schedule: String?, now: Instant, zone: ZoneId): Long? {
    if (schedule == null) return null
    try {
        val iso = datedClock.matchEntire(schedule)?.groupValues
        val provider = providerClock.matchEntire(schedule)?.groupValues
        val us = monthFirstClock.matchEntire(schedule)?.groupValues
        val zoneLabel = (iso?.get(3) ?: provider?.get(5)).orEmpty()
        val sourceZone = if (zoneLabel.isEmpty()) zone else {
            ZoneOffset.ofHours(offsets[zoneLabel.uppercase(Locale.ROOT)] ?: return null)
        }
        val date = if (iso != null) LocalDate.parse(iso[1]) else {
            val month = Month.from(monthFormat.parse(provider?.get(3) ?: us?.get(1) ?: return null))
            val day = (provider?.get(2) ?: us?.get(2) ?: return null).toInt()
            val today = now.atZone(sourceZone).toLocalDate()
            val nearest = (today.year - 1..today.year + 1).mapNotNull { year ->
                if (day in 1..month.length(java.time.Year.isLeap(year.toLong()))) LocalDate.of(year, month, day) else null
            }.minByOrNull { abs(it.toEpochDay() - today.toEpochDay()) } ?: return null
            if (provider != null && !nearest.dayOfWeek.name.startsWith(provider[1], ignoreCase = true)) return null
            nearest
        }
        val clock = (iso?.get(2) ?: provider?.get(4) ?: us?.get(3) ?: return null).replace(" ", "").uppercase(Locale.ROOT)
        val time = LocalTime.parse(clock, if (clock.endsWith("M")) clock12 else clock24)
        return date.atTime(time).atZone(sourceZone).toInstant().toEpochMilli()
    } catch (_: DateTimeException) {
        return null
    }
}
