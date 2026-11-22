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
        val confirmed = withStatus(ProviderGameState.LIVE, now)
        assertThat(tile.isLiveAt(listOf(confirmed), now)).isTrue()
        assertThat(tile.isLiveAt(listOf(event), event.endTime)).isFalse()
        assertThat(tile.isLiveAt(listOf(event), event.startTime - 1)).isFalse()
        assertThat(tile.isLiveAt(listOf(confirmed.copy(broadcasts = listOf(SportsBroadcast(channel.copy(providerId = 8), null)))), now)).isFalse()
        assertThat(tile.isLiveAt(listOf(confirmed.copy(competitors = listOf(SportsCompetitor("Other team")))), now)).isFalse()
    }

    @Test fun replayAndFinalStatusesNeverAppearLive() {
        val replay = withStatus(ProviderGameState.LIVE, now).copy(eventType = SportsEventType.REPLAY)
        assertThat(tile.isLiveAt(listOf(replay), now)).isFalse()
        for (status in listOf(ProviderGameState.FINAL, ProviderGameState.CANCELLED, ProviderGameState.POSTPONED, ProviderGameState.SCHEDULED)) {
            assertThat(tile.isLiveAt(listOf(withStatus(status, now)), now)).isFalse()
        }
    }

    @Test fun staleLiveScoresExpireButFreshOvertimeRemainsLive() {
        assertThat(withStatus(ProviderGameState.LIVE, now - 600001).isLiveAt(now)).isFalse()
        assertThat(withStatus(ProviderGameState.LIVE, event.endTime).isLiveAt(event.endTime + 60000)).isTrue()
        val cached = withStatus(ProviderGameState.LIVE, now)
        val status = checkNotNull(cached.provenance.status)
        assertThat(cached.copy(provenance = cached.provenance.copy(status = status.copy(isStale = true))).isLiveAt(now)).isFalse()
        assertThat(cached.copy(provenance = cached.provenance.copy(
            status = status.copy(value = status.value.copy(updatedAt = null), updatedAt = null)
        )).isLiveAt(now)).isFalse()
    }

    @Test fun currentGuideCannotConfirmThatTheMatchIsLive() {
        assertThat(event.isLiveAt(now)).isFalse()
        assertThat(withStatus(ProviderGameState.UNKNOWN, now).isLiveAt(now)).isFalse()
        assertThat(tile.isLiveAt(emptyList(), now)).isFalse()
        val program = Program(channelId = "1", title = "Live: Atlanta Hawks vs Charlotte Hornets",
            startTime = now - 1000, endTime = now + 1000)
        val guideTile = tile.copy(broadcast = broadcast.copy(channel = channel.copy(currentProgram = program)))
        assertThat(guideTile.isLiveAt(emptyList(), now)).isFalse()
    }

    @Test fun preferredChannelOnlyTunesWhileItsOwnProgrammeIsAiring() {
        val replayEndingAtKickoff = Program(
            channelId = "1", title = event.title, startTime = now - 2000, endTime = now,
        )
        val preferred = broadcast.copy(guideProgram = replayEndingAtKickoff)
        val live = withStatus(ProviderGameState.LIVE, now - 1000).copy(
            broadcasts = listOf(preferred),
            rememberedChannelKey = preferred.channelIdentityKey(),
        )

        assertThat(live.preferredLiveBroadcast(now - 1)).isEqualTo(preferred)
        assertThat(live.preferredLiveBroadcast(now)).isNull()
        assertThat(event.preferredLiveBroadcast(now)).isNull()
    }

    @Test fun aDatedLiveLabelInThePlaylistDoesNotConfirmTheMatch() {
        val label = "LIVE | Atlanta Hawks vs Charlotte Hornets | Fri 20 Nov 18:00 EST (US) | US: NBA PASS PPV 1"
        val listing = parsePlaylistSportsEvent(label)
        val listed = tile.copy(listing = listing)
        val airTime = Instant.parse("2026-11-20T23:30:00Z").toEpochMilli()
        assertThat(listed.isLiveAt(emptyList(), airTime)).isFalse()
        assertThat(listed.isLiveAt(emptyList(), airTime + 86_400_000L)).isFalse()
        assertThat(tile.copy(listing = parsePlaylistSportsEvent("LIVE | Atlanta Hawks vs Charlotte Hornets | all"))
            .isLiveAt(emptyList(), airTime)).isFalse()
    }
}
