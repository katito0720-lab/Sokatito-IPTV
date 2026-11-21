package com.streamvault.feature.live.sports

internal data class SportsFeedLabel(val firstTeam: String, val secondTeam: String, val schedule: String?, val feed: String?)

private val versus = Regex("""\s+(?:vs\.?|v\.?)\s+""", RegexOption.IGNORE_CASE)
private val clockField = Regex("""\d{1,2}:\d{2}(?:\s*(?:AM|PM|\([^)]*\)))?""", RegexOption.IGNORE_CASE)
private val dateField = Regex("""\d{4}-\d{2}-\d{2}""")
private val feedSuffix = Regex("""\s+:\s*[^:]+$""")

/** Decode explicit fixture labels. A league station without a matchup keeps its channel name. */
internal fun parseSportsFeedLabel(name: String): SportsFeedLabel? {
    val fields = name.split('|').map(String::trim)
    val clock = fields.indexOfFirst { clockField.matches(it) }
    val scheduledField = if (clock >= 0) fields.getOrNull(clock + 1) else null
    val fixture = scheduledField ?: fields.firstOrNull { versus.containsMatchIn(it) } ?: return null
    val opponents = fixture.substringBefore(" @ ").substringAfter(": ").trim()
    val teams = if (scheduledField != null && !versus.containsMatchIn(opponents)) opponents.split(" - ")
        else versus.split(opponents)
    if (teams.size != 2 || teams.any { it.isBlank() }) return null
    val schedule = if (scheduledField != null) {
        listOfNotNull(fields.getOrNull(clock - 1)?.takeIf(dateField::matches), fields[clock]).joinToString(" · ")
    } else fixture.substringAfter(" @ ", "").replace(feedSuffix, "").trim().ifEmpty { null }
    val feed = if (scheduledField != null) {
        val hasLanguageField = fields.getOrNull(clock + 2) == ""
        val source = fields.lastOrNull()?.takeIf { fields.lastIndex >= clock + 3 && it.isNotBlank() }
        listOfNotNull(fields.getOrNull(clock + 3)?.takeIf { hasLanguageField && it.isNotBlank() }, source)
            .distinct().joinToString(" · ").ifBlank { null }
    } else feedSuffix.find(fixture.substringAfter(" @ ", ""))?.value?.trim()?.removePrefix(":")?.trim()
    return SportsFeedLabel(teams[0].trim(), teams[1].trim(), schedule, feed)
}
