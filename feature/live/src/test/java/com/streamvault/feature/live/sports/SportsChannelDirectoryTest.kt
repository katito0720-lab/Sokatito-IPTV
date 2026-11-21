package com.streamvault.feature.live.sports

import com.google.common.truth.Truth.assertThat
import com.streamvault.domain.model.Channel
import org.junit.Test

class SportsChannelDirectoryTest {
    private val channel = Channel(id = 1, name = "Arena HD", providerId = 7, streamUrl = "https://example.test/live")

    @Test fun `channel names take priority over groups and include numbered feeds`() {
        val feeds = listOf(channel.copy(name = "US | NBA01", groupTitle = "EPL"),
            channel.copy(id = 2, groupTitle = "UK • EPL"), channel.copy(id = 3, categoryName = "Indian Premier League"))
        val result = taggedSportsChannels(feeds, 42L)
        assertThat(result.map { it.first.id }).containsExactly("nba", "premier-league", "indian-premier-league").inOrder()
        assertThat(result.map { it.second.combinedProfileId }.distinct()).containsExactly(42L)
    }

    @Test fun `country-qualified league tags keep their identity`() {
        val feeds = listOf("Next | Premier League - Bermuda | BE", "Premier League - Lebanon",
            "Premier League - England", "Premier League - HD").map { channel.copy(name = it) }
        assertThat(taggedSportsChannels(feeds, null).map { it.first.id }).containsExactly(
            "premier-league-bermuda", "premier-league-lebanon", "premier-league", "premier-league").inOrder()
    }

    @Test fun `restricted empty and unrelated streams are omitted`() {
        val tagged = channel.copy(groupTitle = "NBA")
        assertThat(taggedSportsChannels(listOf(tagged.copy(isAdult = true), tagged.copy(isUserProtected = true),
            tagged.copy(streamUrl = ""), channel.copy(name = "REPLAY"), channel.copy(name = "pinball")), null)).isEmpty()
    }

    @Test fun `structured competition name distinguishes other premier leagues from English feeds`() {
        val feed = channel.copy(name = "Next | 2026-11-21 | 21:00 (GMT) | Rovers vs Prankton | USVISF Premier League | BE")
        assertThat(taggedSportsChannels(listOf(feed), null).single().first.displayName).isEqualTo("USVISF Premier League")
    }
}
