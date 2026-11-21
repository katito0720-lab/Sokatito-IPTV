package com.streamvault.feature.live.sports

import com.google.common.truth.Truth.assertThat
import com.streamvault.domain.model.Channel
import com.streamvault.domain.model.Program
import com.streamvault.domain.sports.provider.*
import java.time.Instant
import org.junit.Test

class SportsLiveStatusTest {
    private val now = 1_800_000_000_000L
    private val channel = Channel(1, "NBA 01", providerId = 7)
    private val broadcast = SportsBroadcast(channel, null)
    private val tile = SportsFeedTile(broadcast, SportsFeedLabel("Atlanta Hawks", "Charlotte Hornets", "7:00 PM", null))
    private val event = SportsEvent(
        id = "game", title = "Atlanta Hawks vs Charlotte Hornets", league = "NBA", categoryLabel = null,
        description = "", sport = SportType.BASKETBALL, startTime = now - 3600000, endTime = now + 3600000,
        broadcasts = listOf(broadcast), confidence = 1f,
        competitors = listOf(SportsCompetitor("Atlanta Hawks"), SportsCompetitor("Charlotte Hornets")),
        session = null, eventType = SportsEventType.LIVE_EVENT, rawProgrammeRefs = emptyList()
    )
    private fun withStatus(state: ProviderGameState, time: Long) = event.copy(provenance = event.provenance.copy(
        status = SourcedValue(ProviderGameStatus(state, updatedAt = Instant.ofEpochMilli(time)),
            SportsSourceRef.Provider("provider", "custom-json", "game"))
    ))

    @Test fun onlyCurrentMatchingFixturesMarkTheFeedLive() {
        assertThat(tile.isLiveAt(listOf(event), now)).isTrue()
        assertThat(tile.isLiveAt(listOf(event), event.endTime)).isFalse()
        assertThat(tile.isLiveAt(listOf(event), event.startTime - 1)).isFalse()
        assertThat(tile.isLiveAt(listOf(event.copy(broadcasts = listOf(SportsBroadcast(channel.copy(providerId = 8), null)))), now)).isFalse()
        assertThat(tile.isLiveAt(listOf(event.copy(competitors = listOf(SportsCompetitor("Other team")))), now)).isFalse()
    }

    @Test fun replayAndFinalStatusesNeverAppearLive() {
        assertThat(tile.isLiveAt(listOf(event.copy(eventType = SportsEventType.REPLAY)), now)).isFalse()
        for (status in listOf(ProviderGameState.FINAL, ProviderGameState.CANCELLED, ProviderGameState.POSTPONED, ProviderGameState.SCHEDULED)) {
            assertThat(tile.isLiveAt(listOf(withStatus(status, now)), now)).isFalse()
        }
    }

    @Test fun staleLiveScoresExpireButFreshOvertimeRemainsLive() {
        assertThat(withStatus(ProviderGameState.LIVE, now - 600001).isLiveAt(now)).isFalse()
        assertThat(withStatus(ProviderGameState.LIVE, event.endTime).isLiveAt(event.endTime + 60000)).isTrue()
    }

    @Test fun currentGuideCanConfirmMatchButTimeOnlyLabelsCannot() {
        assertThat(tile.isLiveAt(emptyList(), now)).isFalse()
        val program = Program(channelId = "1", title = "Live: Atlanta Hawks vs Charlotte Hornets",
            startTime = now - 1000, endTime = now + 1000)
        fun feed(title: String) = tile.copy(broadcast = broadcast.copy(channel = channel.copy(currentProgram = program.copy(title = title))))
        assertThat(feed(program.title).isLiveAt(emptyList(), now)).isTrue()
        assertThat(feed("Replay: Atlanta Hawks vs Charlotte Hornets").isLiveAt(emptyList(), now)).isFalse()
        assertThat(feed("Live: Chicago Bulls vs Washington Wizards").isLiveAt(emptyList(), now)).isFalse()
        assertThat(feed(program.title).isLiveAt(emptyList(), program.endTime)).isFalse()
    }

    @Test fun recentDatedProviderLivePrefixCanConfirmAnActiveFeed() {
        val label = "LIVE | Atlanta Hawks vs Charlotte Hornets | Fri 20 Nov 18:00 EST (US) | US: NBA PASS PPV 1"
        val listing = parsePlaylistSportsEvent(label)
        val listed = tile.copy(listing = listing)
        val airTime = Instant.parse("2026-11-20T23:30:00Z").toEpochMilli()
        assertThat(listed.isLiveAt(emptyList(), airTime)).isTrue()
        assertThat(listed.isLiveAt(emptyList(), airTime + 86_400_000L)).isFalse()
        assertThat(tile.copy(listing = parsePlaylistSportsEvent("LIVE | Atlanta Hawks vs Charlotte Hornets | all"))
            .isLiveAt(emptyList(), airTime)).isFalse()
    }
}
