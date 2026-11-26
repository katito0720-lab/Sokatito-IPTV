package com.streamvault.feature.live.sports

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.streamvault.domain.model.Channel
import com.streamvault.feature.live.R
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

private val numberedLeagueFeed = Regex("""\b(nba|wnba|epl|nfl|nhl|mlb)(?=\d)""", RegexOption.IGNORE_CASE)
private val premierLeagueRegion = Regex("""\bpremier\s+league\s*[-–—]\s*([^|:;()\[\]]+)""", RegexOption.IGNORE_CASE)
private val feedDate = Regex("""\d{4}-\d{2}-\d{2}""")
private val regions = (Locale.getISOCountries().map { Locale("", it).getDisplayCountry(Locale.ENGLISH) } +
    listOf("Scotland", "Wales", "Northern Ireland")).associateBy(SportsTextNormalizer::normalize)

@Composable
internal fun SportsLeague.displayLabel(): String = labelResource?.let { stringResource(it) } ?: displayName

private val liveEvents = SportsLeague(
    id = "playlist:live-events", displayName = "", sport = SportType.OTHER,
    aliases = setOf("live events", "live event", "ppv"), labelResource = R.string.live_sports_playlist_events,
)

private val sportChannels = SportType.entries.associateWith { sport ->
    SportsLeague("playlist:${sport.name.lowercase(Locale.ROOT)}", "", sport, setOf(
        sport.name.lowercase(Locale.ROOT).replace('_', ' '),
    ), if (sport == SportType.UNKNOWN) R.string.nav_sports else sport.labelResource())
}

internal val Channel.sportsProviderLabel: String
    get() = currentVariant?.originalName?.takeIf(String::isNotBlank) ?: name

internal data class SportsDirectoryChannel(
    val tile: SportsFeedTile,
    val searchText: String,
) {
    constructor(league: SportsLeague, broadcast: SportsBroadcast) : this(
        tile = broadcast.toDirectoryTile(),
        searchText = SportsTextNormalizer.normalize(
            listOfNotNull(
                broadcast.channel.name, broadcast.channel.sportsProviderLabel, broadcast.channel.groupTitle,
                broadcast.channel.categoryName, league.displayName,
            ).joinToString(" "),
        ),
    )
}

private fun SportsBroadcast.toDirectoryTile(): SportsFeedTile {
    val label = channel.sportsProviderLabel
    val listing = parsePlaylistSportsEvent(label)
    val fixture = parseSportsFeedLabel(label, listing)
    val start = (fixture?.schedule ?: listing?.schedule)?.let { listedSportsStart(it, Instant.now(), ZoneId.systemDefault()) }
    return SportsFeedTile(this, fixture, listing = listing, listedStartMillis = start)
}

internal fun Map<SportsLeague, List<SportsDirectoryChannel>>.inTimeRange(
    range: SportsTimeRange,
    nowMillis: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): Map<SportsLeague, List<SportsDirectoryChannel>> {
    if (range == SportsTimeRange.ON_NOW) return this
    val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
    val tomorrow = today.plusDays(1)
    val firstDay = if (range == SportsTimeRange.TODAY) today else tomorrow
    val untilDay = if (range == SportsTimeRange.TODAY) tomorrow else tomorrow.plusDays(7)
    val from = firstDay.atStartOfDay(zone).toInstant().toEpochMilli()
    val until = untilDay.atStartOfDay(zone).toInstant().toEpochMilli()
    return mapValues { (_, rows) ->
        rows.filter { it.tile.listedStartMillis?.let { start -> start >= from && start < until } == true }
            .sortedBy { it.tile.listedStartMillis }
    }.filterValues { it.isNotEmpty() }
}

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
        val label = channel.sportsProviderLabel
        if (isInactiveSportsListing(label)) return@mapNotNull null
        val league = channelLeague(label)
            ?: groupLeague(channel.groupTitle)
            ?: groupLeague(channel.categoryName)
            ?: run {
                val context = SportsProgrammeClassifier.prepareChannel(channel, providerLabel = label)
                val listing = parsePlaylistSportsEvent(label)
                when {
                    listing != null && (listing.numberedSlot || context.isSportsChannel ||
                        context.detectedSport != SportType.UNKNOWN || SportsEventParser.extractMatchup(listing.title) != null) -> liveEvents
                    context.isSportsChannel || context.detectedSport != SportType.UNKNOWN -> sportChannels[context.detectedSport]
                    else -> null
                }
            } ?: return@mapNotNull null
        league to SportsBroadcast(channel, combinedProfileId)
    }
}
