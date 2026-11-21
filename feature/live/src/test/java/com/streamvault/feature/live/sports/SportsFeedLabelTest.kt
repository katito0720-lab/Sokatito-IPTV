package com.streamvault.feature.live.sports

import com.google.common.truth.Truth.assertThat
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
}
