package com.streamvault.feature.catalog.presentation.dashboard

import com.streamvault.domain.model.AppHomeDashboardShelf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

@OptIn(ExperimentalCoroutinesApi::class)
internal fun <T> Flow<List<AppHomeDashboardShelf>>.observeWhenHomeShelfEnabled(
    shelf: AppHomeDashboardShelf,
    emptyValue: T,
    observe: () -> Flow<T>,
): Flow<T> = map { shelf in it }
    .distinctUntilChanged()
    .flatMapLatest { enabled -> if (enabled) observe() else flowOf(emptyValue) }
    .onStart { emit(emptyValue) }
    .distinctUntilChanged()

internal fun resolveVisibleDashboardShelves(
    uiState: DashboardUiState
): List<AppHomeDashboardShelf> = AppHomeDashboardShelf
    .normalizeForStorage(uiState.homeDashboardShelves)
    .filter(uiState::hasContentFor)

internal fun DashboardUiState.hasContentFor(shelf: AppHomeDashboardShelf): Boolean = when (shelf) {
    AppHomeDashboardShelf.FAVORITE_CHANNELS -> favoriteChannels.isNotEmpty()
    AppHomeDashboardShelf.RECENT_CHANNELS -> recentChannels.isNotEmpty()
    AppHomeDashboardShelf.LIVE_SHORTCUTS -> liveShortcuts.isNotEmpty()
    AppHomeDashboardShelf.CONTINUE_WATCHING -> continueWatching.isNotEmpty()
    AppHomeDashboardShelf.PINNED_CATEGORIES -> pinnedMovieCategories.isNotEmpty() || pinnedSeriesCategories.isNotEmpty()
    AppHomeDashboardShelf.RECENT_MOVIES -> recentMovies.isNotEmpty()
    AppHomeDashboardShelf.RECENT_SERIES -> recentSeries.isNotEmpty()
    AppHomeDashboardShelf.FAVORITE_MOVIES -> favoriteMovies.isNotEmpty()
    AppHomeDashboardShelf.FAVORITE_SERIES -> favoriteSeries.isNotEmpty()
    AppHomeDashboardShelf.TRAKT_WATCHLIST_MOVIES -> traktWatchlistMovies.isNotEmpty()
    AppHomeDashboardShelf.TRAKT_WATCHLIST_SERIES -> traktWatchlistSeries.isNotEmpty()
    AppHomeDashboardShelf.CONTINUE_WATCHING_MOVIES -> continueWatchingMovies.isNotEmpty()
    AppHomeDashboardShelf.CONTINUE_WATCHING_SERIES -> continueWatchingSeriesItems.isNotEmpty()
    AppHomeDashboardShelf.TOP_RATED_MOVIES -> topRatedMovies.isNotEmpty()
    AppHomeDashboardShelf.RECOMMENDED_MOVIES -> recommendedMovies.isNotEmpty()
}
