package com.streamvault.feature.live.sports

import com.streamvault.core.navigation.AppDestination
import com.streamvault.domain.model.Program

internal fun SportsEvent.guideDestinationFor(
    broadcast: SportsBroadcast,
    nowMillis: Long,
): AppDestination.Guide? {
    val programmeStart = broadcast.guideProgram?.startTime ?: provenance.timing.eventStart?.toEpochMilli() ?: startTime
    if (programmeStart <= nowMillis) return null
    return AppDestination.Guide(
        categoryId = broadcast.channel.categoryId.takeIf { broadcast.combinedProfileId == null },
        anchorTimeMs = programmeStart,
        channelId = broadcast.channel.id.takeIf { broadcast.guideProgram != null },
    )
}

internal fun SportsEvent.preferredLiveBroadcast(nowMillis: Long): SportsBroadcast? {
    if (!isLiveAt(nowMillis)) return null
    return rememberedBroadcast()?.takeIf { broadcast ->
        val program = broadcast.guideProgram
        if (program != null) nowMillis >= program.startTime && nowMillis < program.endTime
        else guideDestinationFor(broadcast, nowMillis) == null
    }
}

internal data class SportsReminderTarget(
    val providerId: Long,
    val channelName: String,
    val program: Program,
)

internal fun SportsBroadcast.reminderTarget(nowMillis: Long): SportsReminderTarget? {
    val program = guideProgram ?: return null
    val providerId = program.providerId.takeIf { it > 0L } ?: channel.providerId
    return if (providerId > 0L && program.channelId.isNotBlank() && program.startTime > nowMillis) {
        SportsReminderTarget(providerId, channel.name, program)
    } else null
}
