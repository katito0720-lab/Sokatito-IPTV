package com.streamvault.feature.live.sports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamvault.domain.manager.ProgramReminderManager
import com.streamvault.domain.model.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async

@HiltViewModel
internal class SportsHubViewModel @Inject constructor(
    private val repository: SportsHubRepository,
    private val reminders: ProgramReminderManager,
) : ViewModel() {
    init {
        repository.refreshPresentationNow()
    }

    val uiState = repository.uiState

    fun setSearchQuery(query: String) = repository.setSearchQuery(query)
    fun setSportIncluded(sport: SportType, included: Boolean) = repository.setSportIncluded(sport, included)
    fun setLeagueIncluded(league: String, included: Boolean) = repository.setLeagueIncluded(league, included)
    fun resetInclusions() = repository.resetInclusions()
    fun retry() = repository.retry()
    fun selectTimeRange(range: SportsTimeRange) = repository.selectTimeRange(range)
    fun selectSportFilter(filter: SportsSportFilter) = repository.selectSportFilter(filter)
    fun selectLeagueFilter(league: String?) = repository.selectLeagueFilter(league)
    suspend fun searchChannels(event: SportsEvent, query: String) = repository.searchChannels(event, query)
    suspend fun rememberChannelForEvent(event: SportsEvent, broadcast: SportsBroadcast) =
        repository.rememberChannelForEvent(event, broadcast)
    suspend fun forgetChannelForEvent(event: SportsEvent) = repository.forgetChannelForEvent(event)
    suspend fun loadStandings(event: SportsEvent) = repository.loadStandings(event)
    suspend fun loadExtraDetails(event: SportsEvent) = repository.loadExtraDetails(event)

    suspend fun isReminderScheduled(target: SportsReminderTarget): Boolean = with(target) {
        reminders.isReminderScheduled(providerId, program.channelId, program.title, program.startTime)
    }

    suspend fun setReminder(target: SportsReminderTarget, enabled: Boolean): Result<Unit> =
        viewModelScope.async {
            with(target) {
                if (enabled) {
                    reminders.scheduleReminder(providerId, program.channelId, channelName, program, leadTimeMinutes = 0)
                } else {
                    reminders.cancelReminder(providerId, program.channelId, program.title, program.startTime)
                }
            }
        }.await()
}
