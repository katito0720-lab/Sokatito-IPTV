package com.streamvault.feature.live.sports

import com.streamvault.domain.sports.provider.ProviderGameState
import com.streamvault.domain.sports.provider.SportsSourceRef

internal fun SportsEvent.isLiveAt(nowMillis: Long): Boolean {
    if (eventType !in setOf(SportsEventType.LIVE_EVENT, SportsEventType.SCHEDULED_EVENT)) return false
    val status = provenance.status ?: return false
    if (status.source !is SportsSourceRef.Provider || status.isStale || status.value.state != ProviderGameState.LIVE) {
        return false
    }
    val updatedAt = status.value.updatedAt ?: status.updatedAt ?: return false
    return nowMillis - updatedAt.toEpochMilli() in 0..600_000L
}

internal fun SportsFeedTile.isLiveAt(events: List<SportsEvent>, nowMillis: Long): Boolean {
    val teams = fixture?.let { setOf(SportsAliasRegistry.normalizeTeamName(it.firstTeam), SportsAliasRegistry.normalizeTeamName(it.secondTeam)) }
    fun matchesListing(title: String, names: List<String>): Boolean {
        if (teams != null) return names.map(SportsAliasRegistry::normalizeTeamName).toSet() == teams
        val listedTitle = listing?.title?.let(SportsTextNormalizer::normalize) ?: return true
        return listedTitle in SportsTextNormalizer.normalize(title)
    }
    return events.any { event ->
        event.isLiveAt(nowMillis) &&
            matchesListing(event.title, event.competitors.map { it.displayName }) &&
            event.broadcasts.any { it.channel.providerId == broadcast.channel.providerId && it.channel.id == broadcast.channel.id }
    }
}
