package com.streamvault.feature.live.sports

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.ZoneId
import org.junit.Test

class SportsFeedLabelTest {
    @Test fun nbaFeedKeepsTeamsScheduleAndChannelNumber() {
        val label = "Atlanta Hawks vs Charlotte Hornets @ Nov 20 7:00 PM :NBA  01"
        assertThat(parseSportsFeedLabel(label)).isEqualTo(
            SportsFeedLabel("Atlanta Hawks", "Charlotte Hornets", "Nov 20 7:00 PM", "NBA  01"))
    }

    @Test fun footballFeedKeepsLanguageAndOriginalTimezone() {
        val label = "Next | 21:11 (GMT) | Manchester City vs Fulham || English | Premier League | TOD"
        assertThat(parseSportsFeedLabel(label)).isEqualTo(
            SportsFeedLabel("Manchester City", "Fulham", "21:11 (GMT)", "English · TOD"))
    }

    @Test fun datedFixtureSupportsDashSeparatedTeams() {
        val label = "Next | 2026-11-21 | 11:00 (GMT) | Frankfurt - Hamburg | Frauen Bundesliga | BE"
        assertThat(parseSportsFeedLabel(label)).isEqualTo(
            SportsFeedLabel("Frankfurt", "Hamburg", "2026-11-21 · 11:00 (GMT)", "BE"))
    }

    @Test fun ordinaryChannelsStayChannels() {
        assertThat(parseSportsFeedLabel("|US| NBA Network (US)")).isNull()
        assertThat(parseSportsFeedLabel("Sky - Premier League")).isNull()
    }

    @Test fun playlistSchedulesKeepExplicitZonesAndResolveYearlessDatesAcrossNewYear() {
        val now = Instant.parse("2026-12-31T14:00:00Z")
        val eastern = ZoneId.of("America/New_York")
        fun start(label: String) = listedSportsStart(label, now, eastern)?.let(Instant::ofEpochMilli)

        assertThat(start("2027-01-01 · 01:30 (GMT)")).isEqualTo(Instant.parse("2027-01-01T01:30:00Z"))
        assertThat(start("Fri 1 Jan 19:00 EST (US)")).isEqualTo(Instant.parse("2027-01-02T00:00:00Z"))
        assertThat(start("Jan 1 7:00 PM")).isEqualTo(Instant.parse("2027-01-02T00:00:00Z"))
        assertThat(start("2027-01-01 19:00")).isEqualTo(Instant.parse("2027-01-02T00:00:00Z"))
        assertThat(start("Sun 1 Jan 19:00 EST (US)")).isNull()
        assertThat(start("21:30 (GMT)")).isNull()
        assertThat(start("2027-02-30 19:00")).isNull()
        assertThat(start("2027-01-01 · 01:30 (XYZ)")).isNull()
    }

    @Test fun ukPremierLeagueAndLaLigaSourceFormatsPreserveFixtureTitles() {
        assertThat(parseSportsFeedLabel("HBO UK 27| Premier League - Manchester City - Fulham (2026-11-21 11:00:00)"))
            .isEqualTo(SportsFeedLabel("Manchester City", "Fulham", "2026-11-21 11:00", "HBO UK 27"))
        assertThat(parseSportsFeedLabel("CA (TSN+ 037) | LaLiga: Real Madrid vs. Celta (2026-11-21 14:30:00)"))
            .isEqualTo(SportsFeedLabel("Real Madrid", "Celta", "2026-11-21 14:30", "CA (TSN+ 037)"))
        val slot = parsePlaylistSportsEvent("LIVE EVENT 03 - 1:30pm Price v Kotaskova")
        assertThat(parseSportsFeedLabel("", slot))
            .isEqualTo(SportsFeedLabel("Price", "Kotaskova", "1:30pm", "LIVE EVENT 03"))
    }
}
