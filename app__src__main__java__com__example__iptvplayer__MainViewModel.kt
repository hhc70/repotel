
package com.example.iptvplayer

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val loginForm: LoginFormState = LoginFormState(),
    val session: UserSession? = null,
    val channels: List<Channel> = emptyList(),
    val categories: List<ChannelCategory> = emptyList(),
    val movieItems: List<VodItem> = emptyList(),
    val movieCategories: List<VodCategory> = emptyList(),
    val seriesItems: List<VodItem> = emptyList(),
    val seriesCategories: List<VodCategory> = emptyList(),
    val selectedChannel: Channel? = null,
    val selectedMovie: VodItem? = null,
    val selectedSeries: VodItem? = null,
    val selectedCategoryId: String = "all",
    val selectedVodCategoryId: String = "all",
    val selectedSeriesCategoryId: String = "all",
    val favoriteIds: Set<String> = emptySet(),
    val searchQuery: String = "",
    val onlyFavorites: Boolean = false,
    val contentMode: ContentMode = ContentMode.LIVE,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val isLoggedIn: Boolean get() = session != null

    val filteredChannels: List<Channel>
        get() {
            var source = channels
            if (selectedCategoryId != "all" && selectedCategoryId != "favorites") {
                source = source.filter { it.categoryId == selectedCategoryId }
            }
            if (onlyFavorites || selectedCategoryId == "favorites") {
                source = source.filter { it.id in favoriteIds }
            }
            if (searchQuery.isNotBlank()) {
                source = source.filter {
                    it.name.contains(searchQuery, true) || it.group.orEmpty().contains(searchQuery, true)
                }
            }
            return source
        }

    val filteredMovies: List<VodItem>
        get() = movieItems.filter { item ->
            (selectedVodCategoryId == "all" || item.categoryId == selectedVodCategoryId) &&
                (searchQuery.isBlank() || item.title.contains(searchQuery, true) || item.categoryName.contains(searchQuery, true))
        }

    val filteredSeries: List<VodItem>
        get() = seriesItems.filter { item ->
            (selectedSeriesCategoryId == "all" || item.categoryId == selectedSeriesCategoryId) &&
                (searchQuery.isBlank() || item.title.contains(searchQuery, true) || item.categoryName.contains(searchQuery, true))
        }

    val featuredChannel: Channel?
        get() = selectedChannel ?: filteredChannels.firstOrNull() ?: channels.firstOrNull()

    val featuredVod: VodItem?
        get() = when (contentMode) {
            ContentMode.MOVIES -> selectedMovie ?: filteredMovies.firstOrNull()
            ContentMode.SERIES -> selectedSeries ?: filteredSeries.firstOrNull()
            else -> null
        }

    val channelsByCategory: List<Pair<ChannelCategory, List<Channel>>>
        get() {
            val actualCategories = categories.filterNot { it.id == "favorites" || it.id == "all" }
            val base = mutableListOf<Pair<ChannelCategory, List<Channel>>>()
            val favoriteChannels = channels.filter { it.id in favoriteIds }.take(20)
            if (favoriteChannels.isNotEmpty()) base += ChannelCategory("favorites", "My List") to favoriteChannels
            if (filteredChannels.isNotEmpty()) base += ChannelCategory("all", "Live Now") to filteredChannels.take(20)
            actualCategories.forEach { category ->
                val row = channels.filter { it.categoryId == category.id }
                    .filter { searchQuery.isBlank() || it.name.contains(searchQuery, true) }
                    .take(20)
                if (row.isNotEmpty()) base += category to row
            }
            return base.distinctBy { it.first.id }
        }

    val moviesByCategory: List<Pair<VodCategory, List<VodItem>>>
        get() = movieCategories.mapNotNull { category ->
            val items = filteredMovies.filter { category.id == "all" || it.categoryId == category.id }.take(20)
            if (items.isEmpty()) null else category to items
        }

    val seriesByCategory: List<Pair<VodCategory, List<VodItem>>>
        get() = seriesCategories.mapNotNull { category ->
            val items = filteredSeries.filter { category.id == "all" || it.categoryId == category.id }.take(20)
            if (items.isEmpty()) null else category to items
        }
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = PlaylistRepository(application)
    private val favoritesStore = FavoritesStore(application)
    private val appStore = AppStore(application)

    private val mutableState = MutableStateFlow(MainUiState())

    val uiState: StateFlow<MainUiState> = combine(
        mutableState,
        appStore.session,
        appStore.session.flatMapLatest { session ->
            favoritesStore.favorites(session?.key.orEmpty())
        }
    ) { state, session, favorites ->
        state.copy(session = session, favoriteIds = favorites)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())

    init {
        viewModelScope.launch {
            appStore.session.collect { session ->
                if (session != null && mutableState.value.channels.isEmpty() && mutableState.value.movieItems.isEmpty() && mutableState.value.seriesItems.isEmpty()) {
                    reloadSession(session)
                }
            }
        }
    }

    fun updateSearch(query: String) { mutableState.value = mutableState.value.copy(searchQuery = query) }
    fun updateSelectedCategory(categoryId: String) { mutableState.value = mutableState.value.copy(selectedCategoryId = categoryId) }
    fun updateSelectedVodCategory(categoryId: String) { mutableState.value = mutableState.value.copy(selectedVodCategoryId = categoryId) }
    fun updateSelectedSeriesCategory(categoryId: String) { mutableState.value = mutableState.value.copy(selectedSeriesCategoryId = categoryId) }
    fun updateContentMode(mode: ContentMode) { mutableState.value = mutableState.value.copy(contentMode = mode) }
    fun toggleFavoritesFilter() { mutableState.value = mutableState.value.copy(onlyFavorites = !mutableState.value.onlyFavorites) }
    fun selectChannel(channel: Channel) { mutableState.value = mutableState.value.copy(selectedChannel = channel, contentMode = ContentMode.LIVE) }
    fun selectMovie(item: VodItem) { mutableState.value = mutableState.value.copy(selectedMovie = item, contentMode = ContentMode.MOVIES) }
    fun selectSeries(item: VodItem) { mutableState.value = mutableState.value.copy(selectedSeries = item, contentMode = ContentMode.SERIES) }

    fun toggleFavorite(channelId: String) {
        val session = uiState.value.session ?: return
        viewModelScope.launch { favoritesStore.toggle(session.key, channelId) }
    }

    fun updateProfileName(value: String) { mutableState.value = mutableState.value.copy(loginForm = mutableState.value.loginForm.copy(profileName = value)) }
    fun updatePlaylistUrl(value: String) { mutableState.value = mutableState.value.copy(loginForm = mutableState.value.loginForm.copy(playlistUrl = value)) }
    fun updateXtreamServer(value: String) { mutableState.value = mutableState.value.copy(loginForm = mutableState.value.loginForm.copy(xtreamServer = value)) }
    fun updateXtreamUsername(value: String) { mutableState.value = mutableState.value.copy(loginForm = mutableState.value.loginForm.copy(xtreamUsername = value)) }
    fun updateXtreamPassword(value: String) { mutableState.value = mutableState.value.copy(loginForm = mutableState.value.loginForm.copy(xtreamPassword = value)) }
    fun updateSourceType(value: SourceType) { mutableState.value = mutableState.value.copy(loginForm = mutableState.value.loginForm.copy(sourceType = value)) }

    fun loginWithCurrentForm() {
        val form = mutableState.value.loginForm
        val profile = form.profileName.ifBlank { "Guest" }

        when (form.sourceType) {
            SourceType.M3U -> {
                val session = UserSession(profileName = profile, sourceType = SourceType.M3U, playlistUrl = form.playlistUrl.trim())
                if (session.playlistUrl.isNullOrBlank()) {
                    mutableState.value = mutableState.value.copy(error = "M3U playlist URL is required.")
                    return
                }
                viewModelScope.launch { loadAndPersist(session) { repo.loadM3uFromUrl(session.playlistUrl!!) } }
            }
            SourceType.XTREAM -> {
                val session = UserSession(
                    profileName = profile,
                    sourceType = SourceType.XTREAM,
                    xtreamServer = form.xtreamServer.trim(),
                    xtreamUsername = form.xtreamUsername.trim(),
                    xtreamPassword = form.xtreamPassword
                )
                if (session.xtreamServer.isNullOrBlank() || session.xtreamUsername.isNullOrBlank() || session.xtreamPassword.isNullOrBlank()) {
                    mutableState.value = mutableState.value.copy(error = "Xtream server, username and password are required.")
                    return
                }
                viewModelScope.launch {
                    mutableState.value = mutableState.value.copy(isLoading = true, error = null)
                    val result = repo.loginXtream(session.xtreamServer!!, session.xtreamUsername!!, session.xtreamPassword!!)
                        .fold(
                            onSuccess = { repo.loadXtream(session.xtreamServer, session.xtreamUsername, session.xtreamPassword) },
                            onFailure = { Result.failure(it) }
                        )
                    applyLoadResult(session, result)
                }
            }
        }
    }

    fun loadPlaylistFromUri(uri: Uri) {
        val session = UserSession(
            profileName = mutableState.value.loginForm.profileName.ifBlank { "Guest" },
            sourceType = SourceType.M3U,
            playlistUrl = "local_file"
        )
        viewModelScope.launch { loadAndPersist(session) { repo.loadM3uFromUri(uri) } }
    }

    fun refresh() { uiState.value.session?.let { reloadSession(it) } }

    fun logout() {
        viewModelScope.launch {
            appStore.clearSession()
            mutableState.value = MainUiState(loginForm = mutableState.value.loginForm.copy(profileName = ""))
        }
    }

    private fun reloadSession(session: UserSession) {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(isLoading = true, error = null)
            val result = when (session.sourceType) {
                SourceType.M3U -> repo.loadM3uFromUrl(session.playlistUrl.orEmpty())
                SourceType.XTREAM -> repo.loadXtream(
                    session.xtreamServer.orEmpty(),
                    session.xtreamUsername.orEmpty(),
                    session.xtreamPassword.orEmpty()
                )
            }
            applyLoadResult(session, result, persist = false)
        }
    }

    private suspend fun loadAndPersist(session: UserSession, block: suspend () -> Result<LoadedContent>) {
        mutableState.value = mutableState.value.copy(isLoading = true, error = null)
        applyLoadResult(session, block())
    }

    private suspend fun applyLoadResult(session: UserSession, result: Result<LoadedContent>, persist: Boolean = true) {
        mutableState.value = result.fold(
            onSuccess = { loaded ->
                if (persist) appStore.saveSession(session)
                mutableState.value.copy(
                    isLoading = false,
                    channels = loaded.channels,
                    categories = loaded.categories,
                    movieItems = loaded.movieItems,
                    movieCategories = loaded.movieCategories,
                    seriesItems = loaded.seriesItems,
                    seriesCategories = loaded.seriesCategories,
                    selectedChannel = loaded.channels.firstOrNull(),
                    selectedMovie = loaded.movieItems.firstOrNull(),
                    selectedSeries = loaded.seriesItems.firstOrNull(),
                    selectedCategoryId = "all",
                    selectedVodCategoryId = "all",
                    selectedSeriesCategoryId = "all",
                    error = if (loaded.channels.isEmpty() && loaded.movieItems.isEmpty() && loaded.seriesItems.isEmpty()) "No content found." else null
                )
            },
            onFailure = { error ->
                mutableState.value.copy(isLoading = false, error = error.message ?: "Load failed.")
            }
        )
    }
}
