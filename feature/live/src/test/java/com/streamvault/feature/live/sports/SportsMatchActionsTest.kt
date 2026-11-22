package com.streamvault.feature.live.sports

import com.google.common.truth.Truth.assertThat
import com.streamvault.core.navigation.AppDestination
import com.streamvault.domain.model.Channel
import com.streamvault.domain.model.Program
import org.junit.Test

class SportsMatchActionsTest {
    private val channel = Channel(
        id = 42L, providerId = 7L, streamId = 12L, name = "Sports One",
        categoryId = 11L, streamUrl = "https://example.test/live",
    )
    private val program = Program(
        channelId = "sports-one", title = "Arsenal vs Liverpool",
        startTime = 200L, endTime = 400L, providerId = 9L,
    )
    private val event = SportsEvent(
        id = "fixture", title = program.title, league = "Premier League", categoryLabel = "Football",
        description = "", sport = SportType.FOOTBALL, startTime = 100L, endTime = 300L,
        broadcasts = emptyList(), confidence = 1f, competitors = emptyList(), session = null,
        eventType = SportsEventType.SCHEDULED_EVENT, rawProgrammeRefs = listOf(program),
    )

    @Test
    fun lateBroadcastUsesItsOwnProgrammeForGuideAndReminder() {
        val broadcast = SportsBroadcast(channel, combinedProfileId = null, guideProgram = program)

        assertThat(event.guideDestinationFor(broadcast, 150L))
            .isEqualTo(AppDestination.Guide(11L, 200L, channelId = 42L))
        assertThat(broadcast.reminderTarget(150L))
            .isEqualTo(SportsReminderTarget(9L, channel.name, program))
        assertThat(event.guideDestinationFor(broadcast, 200L)).isNull()
        assertThat(broadcast.reminderTarget(200L)).isNull()
    }

    @Test
    fun combinedGuideUsesTheExactChannelWithoutItsProviderCategory() {
        val broadcast = SportsBroadcast(channel, combinedProfileId = 8L, guideProgram = program)

        assertThat(event.guideDestinationFor(broadcast, 150L))
            .isEqualTo(AppDestination.Guide(anchorTimeMs = 200L, channelId = 42L))
    }

    @Test
    fun inferredChannelCanOpenGuideAtKickoffWithoutCreatingAReminder() {
        val broadcast = SportsBroadcast(channel, combinedProfileId = null)

        assertThat(event.guideDestinationFor(broadcast, 90L))
            .isEqualTo(AppDestination.Guide(11L, 100L))
        assertThat(event.guideDestinationFor(broadcast, 100L)).isNull()
        assertThat(broadcast.reminderTarget(90L)).isNull()
    }
}
