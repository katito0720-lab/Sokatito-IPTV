package com.streamvault.feature.live.sports

internal data class SportsFeedLabel(val firstTeam: String, val secondTeam: String, val schedule: String?, val feed: String?)

private val versus = Regex("""\s+(?:vs\.?|v\.?)\s+""", RegexOption.IGNORE_CASE)
private val clockField = Regex("""\d{1,2}:\d{2}(?:\s*(?:AM|PM|\([^)]*\)))?""", RegexOption.IGNORE_CASE)
private val dateField = Regex("""\d{4}-\d{2}-\d{2}""")
private val feedSuffix = Regex("""\s+:\s*[^:]+$""")
private val trailingDateTime = Regex("""\s*\((\d{4}-\d{2}-\d{2} \d{2}:\d{2}):\d{2}\)\s*$""")
private val leagueMatchLabel = Regex("""^\s*(?:\[4K]\s*)?(?:Premier League|La ?Liga(?: 2)?)\s+-\s+(.+)""", RegexOption.IGNORE_CASE)
private val redundantMatchup = Regex("""\s*\([^()]*(?:vs\.?|v\.?)\s+[^()]*\)\s*$""", RegexOption.IGNORE_CASE)
private val matchday = Regex("""\s+Matchday\s+#?\d+\s*$""", RegexOption.IGNORE_CASE)

/** Decode explicit fixture labels. A league station without a matchup keeps its channel name. */
internal fun parseSportsFeedLabel(name: String, listing: SportsPlaylistEvent? = null): SportsFeedLabel? {
    val fields = name.split('|').map(String::trim)
    val clock = fields.indexOfFirst { clockField.matches(it) }
    val scheduledField = fields.getOrNull(clock + 1).takeIf { clock in 1..2 }
    val leagueField = fields.firstNotNullOfOrNull { leagueMatchLabel.matchEntire(it)?.groupValues?.get(1) }
    val fixture = listing?.title ?: scheduledField ?: fields.firstOrNull { versus.containsMatchIn(it) }
        ?: leagueField ?: return null
    val stamp = trailingDateTime.find(fixture)?.groupValues?.get(1)
        ?: trailingDateTime.find(name)?.groupValues?.get(1)
    val opponents = fixture.substringBefore(" @ ").substringAfterLast(": ")
        .replace(trailingDateTime, "").replace(redundantMatchup, "").replace(matchday, "").trim()
    val teams = if ((scheduledField != null || leagueField != null) && !versus.containsMatchIn(opponents)) opponents.split(" - ")
        else versus.split(opponents)
    if (teams.size != 2 || teams.any { it.isBlank() }) return null
    val schedule = if (listing != null) listing.schedule else if (clock >= 0) {
        listOfNotNull(fields.getOrNull(clock - 1)?.takeIf(dateField::matches), fields[clock]).joinToString(" · ")
    } else stamp ?: fixture.substringAfter(" @ ", "").replace(feedSuffix, "").trim().ifEmpty { null }
    val feed = if (listing != null) listing.feed else if (scheduledField != null) {
        val hasLanguageField = fields.getOrNull(clock + 2) == ""
        val source = fields.lastOrNull()?.takeIf { fields.lastIndex >= clock + 3 && it.isNotBlank() }
        listOfNotNull(fields.getOrNull(clock + 3)?.takeIf { hasLanguageField && it.isNotBlank() }, source)
            .distinct().joinToString(" · ").ifBlank { null }
    } else {
        feedSuffix.find(fixture.substringAfter(" @ ", ""))?.value?.trim()?.removePrefix(":")?.trim()
            ?: fields.firstOrNull()?.takeIf { stamp != null && fields.size > 1 && it != fixture &&
                !it.equals("Next", ignoreCase = true) }
    }
    return SportsFeedLabel(teams[0].trim(), teams[1].trim(), schedule, feed)
}
