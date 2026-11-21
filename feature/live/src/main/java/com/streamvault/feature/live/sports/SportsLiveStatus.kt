package com.streamvault.feature.live.sports

import com.streamvault.domain.sports.provider.ProviderGameState
import com.streamvault.domain.model.Program

internal fun SportsEvent.isLiveAt(nowMillis: Long): Boolean {
    if (eventType !in setOf(SportsEventType.LIVE_EVENT, SportsEventType.SCHEDULED_EVENT)) return false
    val status = provenance.status
    val updatedAt = status?.value?.updatedAt ?: status?.updatedAt
    val age = updatedAt?.let { nowMillis - it.toEpochMilli() }
    val fresh = age != null && age in 0..600_000L
    return when (status?.value?.state) {
        ProviderGameState.LIVE -> fresh || (updatedAt == null && broadcastStatusAt(nowMillis) == SportsBroadcastStatus.ON_AIR)
        null, ProviderGameState.UNKNOWN -> broadcastStatusAt(nowMillis) == SportsBroadcastStatus.ON_AIR
        else -> false
    }
}

internal fun SportsFeedTile.isLiveAt(events: List<SportsEvent>, nowMillis: Long): Boolean {
    val teams = fixture?.let { setOf(SportsAliasRegistry.normalizeTeamName(it.firstTeam), SportsAliasRegistry.normalizeTeamName(it.secondTeam)) }
    fun matchesListing(title: String, names: List<String>): Boolean {
        if (teams != null) return names.map(SportsAliasRegistry::normalizeTeamName).toSet() == teams
        val listedTitle = listing?.title?.let(SportsTextNormalizer::normalize) ?: return true
        return listedTitle in SportsTextNormalizer.normalize(title)
    }
    val matchingEvents = events.filter { event ->
        matchesListing(event.title, event.competitors.map { it.displayName }) &&
            event.broadcasts.any { it.channel.providerId == broadcast.channel.providerId && it.channel.id == broadcast.channel.id }
    }
    if (matchingEvents.isNotEmpty()) return matchingEvents.any { it.isLiveAt(nowMillis) }
    val program = broadcast.channel.currentProgram
    if (program != null && nowMillis in program.startTime until program.endTime) {
        val classification = SportsProgrammeClassifier.classify(program, broadcast.channel)
        return classification.isCompetitiveEvent && matchesListing(program.title, classification.competitors) &&
            classification.eventType in setOf(SportsEventType.LIVE_EVENT, SportsEventType.SCHEDULED_EVENT)
    }
    if (listing == null || !listing.isReportedLiveAt(nowMillis)) return false
    val listedProgram = Program(channelId = "", title = listing.title)
    return SportsEventParser.classifyEventType(listedProgram, hasCompetitiveSignal = true) in
        setOf(SportsEventType.LIVE_EVENT, SportsEventType.SCHEDULED_EVENT)
}
