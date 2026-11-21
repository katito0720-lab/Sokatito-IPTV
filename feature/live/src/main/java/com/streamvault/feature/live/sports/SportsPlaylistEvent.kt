package com.streamvault.feature.live.sports

import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

internal data class SportsPlaylistEvent(
    val title: String,
    val schedule: String?,
    val feed: String?,
    val numberedSlot: Boolean = false,
    val reportedLive: Boolean = false,
) {
    fun isReportedLiveAt(nowMillis: Long): Boolean {
        if (!reportedLive || schedule == null) return false
        val match = datedLocalTime.matchEntire(schedule) ?: return false
        val offset = zones[match.groupValues[2].uppercase(Locale.ROOT)] ?: return false
        val now = Instant.ofEpochMilli(nowMillis).atOffset(offset)
        val dateTime = runCatching {
            // A weekday without a year is only meaningful around the device's current date.
            LocalDateTime.parse("${now.year} ${match.groupValues[1]}", datedLocalTimeFormat)
        }.getOrNull() ?: return false
        val age = Duration.between(dateTime.atOffset(offset), now)
        return !age.isNegative && age <= Duration.ofHours(4)
    }
}

private val slot = Regex("""^LIVE EVENTS?\s+(\d+)\s*[-–:]\s*(.+)$""", RegexOption.IGNORE_CASE)
private val startClock = Regex("""^\d{1,2}(?::\d{2})?\s*[ap]m\b""", RegexOption.IGNORE_CASE)
private val datedLocalTime = Regex(
    """([A-Za-z]{3} \d{1,2} [A-Za-z]{3} \d{1,2}:\d{2}) (GMT|UTC|EST|EDT|CST|CDT|MST|MDT|PST|PDT)(?: \((US|CA)\))?""",
    RegexOption.IGNORE_CASE,
)
private val datedLocalTimeFormat = DateTimeFormatter.ofPattern("uuuu EEE d MMM H:mm", Locale.ENGLISH)
private val zones = mapOf(
    "GMT" to ZoneOffset.UTC, "UTC" to ZoneOffset.UTC,
    "EST" to ZoneOffset.ofHours(-5), "EDT" to ZoneOffset.ofHours(-4),
    "CST" to ZoneOffset.ofHours(-6), "CDT" to ZoneOffset.ofHours(-5),
    "MST" to ZoneOffset.ofHours(-7), "MDT" to ZoneOffset.ofHours(-6),
    "PST" to ZoneOffset.ofHours(-8), "PDT" to ZoneOffset.ofHours(-7),
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
    return SportsPlaylistEvent(title, schedule, fields.lastOrNull()?.takeIf { fields.size >= 4 && it.isNotBlank() }, reportedLive = true)
}
