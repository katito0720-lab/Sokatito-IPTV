package com.streamvault.feature.live.sports

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
