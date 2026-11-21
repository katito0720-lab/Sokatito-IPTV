package com.streamvault.feature.live.sports

import com.streamvault.domain.model.Channel
import java.util.Locale

private val numberedLeagueFeed = Regex("""\b(nba|wnba|epl|nfl|nhl|mlb)(?=\d)""", RegexOption.IGNORE_CASE)
private val premierLeagueRegion = Regex("""\bpremier\s+league\s*[-–—]\s*([^|:;()\[\]]+)""", RegexOption.IGNORE_CASE)
private val feedDate = Regex("""\d{4}-\d{2}-\d{2}""")
private val regions = (Locale.getISOCountries().map { Locale("", it).getDisplayCountry(Locale.ENGLISH) } +
    listOf("Scotland", "Wales", "Northern Ireland")).associateBy(SportsTextNormalizer::normalize)

private fun channelLeague(label: String): SportsLeague? {
    val league = SportsLeagueRegistry.detectChannelTag(label.replace(numberedLeagueFeed, "$1 ")) ?: return null
    if (league.id != "premier-league") return league
    // Some playlists give the competition its own field after the date, time and fixture.
    val fields = label.split('|', limit = 6)
    if (fields.size >= 5 && feedDate.matches(fields[1].trim()) && ':' in fields[2]) {
        val competition = fields[4].trim()
        val normalized = SportsTextNormalizer.normalize(competition)
        if (normalized.endsWith(" premier league") && normalized !in league.aliases) {
            return league.copy(id = normalized.replace(' ', '-'), displayName = competition, aliases = setOf(normalized))
        }
    }
    val qualifier = premierLeagueRegion.find(label)?.groupValues?.get(1) ?: return league
    val regionKey = SportsTextNormalizer.normalize(qualifier)
    val region = regions[regionKey] ?: return league
    if (regionKey == "united kingdom") return league
    val name = "${league.displayName} - $region"
    return league.copy(id = "premier-league-${regionKey.replace(' ', '-')}", displayName = name,
        aliases = setOf(SportsTextNormalizer.normalize(name)))
}

internal fun taggedSportsChannels(channels: List<Channel>, combinedProfileId: Long?): List<Pair<SportsLeague, SportsBroadcast>> {
    val groups = mutableMapOf<String, SportsLeague?>()
    fun groupLeague(label: String?): SportsLeague? {
        if (label.isNullOrBlank()) return null
        if (label !in groups) groups[label] = channelLeague(label)
        return groups[label]
    }
    return channels.mapNotNull { channel ->
        if (channel.streamUrl.isBlank() || channel.isAdult || channel.isUserProtected) return@mapNotNull null
        val league = channelLeague(channel.name)
            ?: groupLeague(channel.groupTitle)
            ?: groupLeague(channel.categoryName)
            ?: return@mapNotNull null
        league to SportsBroadcast(channel, combinedProfileId)
    }
}
