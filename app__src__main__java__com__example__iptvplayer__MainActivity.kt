
package com.example.iptvplayer

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.LocalMovies
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<MainViewModel>()

    private val openPlaylistFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            viewModel.loadPlaylistFromUri(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IPTVPlayerTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppScreen(
                        state = state,
                        onSourceTypeChange = viewModel::updateSourceType,
                        onProfileNameChange = viewModel::updateProfileName,
                        onPlaylistUrlChange = viewModel::updatePlaylistUrl,
                        onXtreamServerChange = viewModel::updateXtreamServer,
                        onXtreamUsernameChange = viewModel::updateXtreamUsername,
                        onXtreamPasswordChange = viewModel::updateXtreamPassword,
                        onLogin = viewModel::loginWithCurrentForm,
                        onOpenFile = { openPlaylistFile.launch(arrayOf("*/*")) },
                        onSelectChannel = viewModel::selectChannel,
                        onSelectMovie = viewModel::selectMovie,
                        onSelectSeries = viewModel::selectSeries,
                        onToggleFavorite = viewModel::toggleFavorite,
                        onToggleFavoritesFilter = viewModel::toggleFavoritesFilter,
                        onSearch = viewModel::updateSearch,
                        onSelectCategory = viewModel::updateSelectedCategory,
                        onSelectVodCategory = viewModel::updateSelectedVodCategory,
                        onSelectSeriesCategory = viewModel::updateSelectedSeriesCategory,
                        onChangeMode = viewModel::updateContentMode,
                        onEnterPip = { enterPip() },
                        onLogout = viewModel::logout,
                        onRefresh = viewModel::refresh
                    )
                }
            }
        }
    }

    private fun enterPip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build()
            enterPictureInPictureMode(params)
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !isInPictureInPictureMode) enterPip()
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
    }
}

@Composable
fun IPTVPlayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFE50914),
            secondary = Color(0xFFB3B3B3),
            surface = Color(0xFF141414),
            background = Color(0xFF0B0B0B)
        ),
        content = content
    )
}

@Composable
private fun AppScreen(
    state: MainUiState,
    onSourceTypeChange: (SourceType) -> Unit,
    onProfileNameChange: (String) -> Unit,
    onPlaylistUrlChange: (String) -> Unit,
    onXtreamServerChange: (String) -> Unit,
    onXtreamUsernameChange: (String) -> Unit,
    onXtreamPasswordChange: (String) -> Unit,
    onLogin: () -> Unit,
    onOpenFile: () -> Unit,
    onSelectChannel: (Channel) -> Unit,
    onSelectMovie: (VodItem) -> Unit,
    onSelectSeries: (VodItem) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onToggleFavoritesFilter: () -> Unit,
    onSearch: (String) -> Unit,
    onSelectCategory: (String) -> Unit,
    onSelectVodCategory: (String) -> Unit,
    onSelectSeriesCategory: (String) -> Unit,
    onChangeMode: (ContentMode) -> Unit,
    onEnterPip: () -> Unit,
    onLogout: () -> Unit,
    onRefresh: () -> Unit
) {
    if (!state.isLoggedIn) {
        LoginScreen(
            state = state,
            onSourceTypeChange = onSourceTypeChange,
            onProfileNameChange = onProfileNameChange,
            onPlaylistUrlChange = onPlaylistUrlChange,
            onXtreamServerChange = onXtreamServerChange,
            onXtreamUsernameChange = onXtreamUsernameChange,
            onXtreamPasswordChange = onXtreamPasswordChange,
            onOpenFile = onOpenFile,
            onLogin = onLogin
        )
    } else {
        HomeScreen(
            state = state,
            onSelectChannel = onSelectChannel,
            onSelectMovie = onSelectMovie,
            onSelectSeries = onSelectSeries,
            onToggleFavorite = onToggleFavorite,
            onToggleFavoritesFilter = onToggleFavoritesFilter,
            onSearch = onSearch,
            onSelectCategory = onSelectCategory,
            onSelectVodCategory = onSelectVodCategory,
            onSelectSeriesCategory = onSelectSeriesCategory,
            onChangeMode = onChangeMode,
            onEnterPip = onEnterPip,
            onLogout = onLogout,
            onRefresh = onRefresh
        )
    }
}

@Composable
private fun LoginScreen(
    state: MainUiState,
    onSourceTypeChange: (SourceType) -> Unit,
    onProfileNameChange: (String) -> Unit,
    onPlaylistUrlChange: (String) -> Unit,
    onXtreamServerChange: (String) -> Unit,
    onXtreamUsernameChange: (String) -> Unit,
    onXtreamPasswordChange: (String) -> Unit,
    onOpenFile: () -> Unit,
    onLogin: () -> Unit
) {
    val form = state.loginForm
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0B0B0B))) {
        Box(modifier = Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color(0xFF1A1A1A), Color(0xFF0B0B0B)))))
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp).imePadding(),
            verticalArrangement = Arrangement.Center
        ) {
            Text("IPTV Stream+", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Canlı TV, film/dizi VOD, EPG, favoriler ve Android TV deneyimi.", color = Color.LightGray)
            Spacer(Modifier.height(24.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xCC171717)), shape = RoundedCornerShape(24.dp)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedTextField(value = form.profileName, onValueChange = onProfileNameChange, modifier = Modifier.fillMaxWidth(), label = { Text("Profil adı") }, leadingIcon = { Icon(Icons.Default.Person, null) })
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(selected = form.sourceType == SourceType.M3U, onClick = { onSourceTypeChange(SourceType.M3U) }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text("M3U") }
                        SegmentedButton(selected = form.sourceType == SourceType.XTREAM, onClick = { onSourceTypeChange(SourceType.XTREAM) }, shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text("Xtream") }
                    }
                    if (form.sourceType == SourceType.M3U) {
                        OutlinedTextField(value = form.playlistUrl, onValueChange = onPlaylistUrlChange, modifier = Modifier.fillMaxWidth(), label = { Text("M3U URL") }, leadingIcon = { Icon(Icons.Default.LiveTv, null) })
                        Button(onClick = onOpenFile) { Icon(Icons.Default.FolderOpen, null); Spacer(Modifier.width(8.dp)); Text("Dosyadan aç") }
                    } else {
                        OutlinedTextField(value = form.xtreamServer, onValueChange = onXtreamServerChange, modifier = Modifier.fillMaxWidth(), label = { Text("Sunucu") }, leadingIcon = { Icon(Icons.Default.Tv, null) })
                        OutlinedTextField(value = form.xtreamUsername, onValueChange = onXtreamUsernameChange, modifier = Modifier.fillMaxWidth(), label = { Text("Kullanıcı adı") }, leadingIcon = { Icon(Icons.Default.Person, null) })
                        OutlinedTextField(value = form.xtreamPassword, onValueChange = onXtreamPasswordChange, modifier = Modifier.fillMaxWidth(), label = { Text("Şifre") }, leadingIcon = { Icon(Icons.Default.Lock, null) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                    }
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                    Button(onClick = onLogin, modifier = Modifier.fillMaxWidth()) { Text(if (state.isLoading) "Yükleniyor..." else "Devam Et") }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeScreen(
    state: MainUiState,
    onSelectChannel: (Channel) -> Unit,
    onSelectMovie: (VodItem) -> Unit,
    onSelectSeries: (VodItem) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onToggleFavoritesFilter: () -> Unit,
    onSearch: (String) -> Unit,
    onSelectCategory: (String) -> Unit,
    onSelectVodCategory: (String) -> Unit,
    onSelectSeriesCategory: (String) -> Unit,
    onChangeMode: (ContentMode) -> Unit,
    onEnterPip: () -> Unit,
    onLogout: () -> Unit,
    onRefresh: () -> Unit
) {
    val firstMenuRequester = remember { FocusRequester() }
    BackHandler(enabled = state.contentMode != ContentMode.LIVE) { onChangeMode(ContentMode.LIVE) }

    Box(Modifier.fillMaxSize().background(Color(0xFF0B0B0B))) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 48.dp)
        ) {
            item {
                HeroPanel(
                    state = state,
                    onEnterPip = onEnterPip,
                    onToggleFavorite = onToggleFavorite,
                    onPrimaryAction = {
                        when (state.contentMode) {
                            ContentMode.LIVE -> state.featuredChannel?.let(onSelectChannel)
                            ContentMode.MOVIES -> state.featuredVod?.let(onSelectMovie)
                            ContentMode.SERIES -> state.featuredVod?.let(onSelectSeries)
                        }
                    }
                )
            }

            stickyHeader {
                TopControlBar(
                    state = state,
                    firstMenuRequester = firstMenuRequester,
                    onSearch = onSearch,
                    onChangeMode = onChangeMode,
                    onToggleFavoritesFilter = onToggleFavoritesFilter,
                    onRefresh = onRefresh,
                    onLogout = onLogout
                )
            }

            item {
                when (state.contentMode) {
                    ContentMode.LIVE -> CategoryChips(
                        ids = state.categories.map { it.id to it.name },
                        selectedId = state.selectedCategoryId,
                        onSelected = onSelectCategory
                    )
                    ContentMode.MOVIES -> CategoryChips(
                        ids = state.movieCategories.map { it.id to it.name },
                        selectedId = state.selectedVodCategoryId,
                        onSelected = onSelectVodCategory
                    )
                    ContentMode.SERIES -> CategoryChips(
                        ids = state.seriesCategories.map { it.id to it.name },
                        selectedId = state.selectedSeriesCategoryId,
                        onSelected = onSelectSeriesCategory
                    )
                }
            }

            when (state.contentMode) {
                ContentMode.LIVE -> {
                    items(state.channelsByCategory) { (category, channels) ->
                        PosterRow(
                            title = category.name,
                            items = channels,
                            isFavorite = { it.id in state.favoriteIds },
                            onSelectChannel = onSelectChannel,
                            onToggleFavorite = onToggleFavorite
                        )
                    }
                }
                ContentMode.MOVIES -> {
                    items(state.moviesByCategory) { (category, items) ->
                        VodRow(title = category.name, items = items, onSelect = onSelectMovie)
                    }
                }
                ContentMode.SERIES -> {
                    items(state.seriesByCategory) { (category, items) ->
                        VodRow(title = category.name, items = items, onSelect = onSelectSeries)
                    }
                }
            }

            if (state.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }

            state.error?.let { message ->
                item {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        runCatching { firstMenuRequester.requestFocus() }
    }
}

@Composable
private fun HeroPanel(
    state: MainUiState,
    onEnterPip: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onPrimaryAction: () -> Unit
) {
    val channel = state.featuredChannel
    val vod = state.featuredVod
    Box(
        modifier = Modifier.fillMaxWidth().height(380.dp).background(
            Brush.verticalGradient(listOf(Color(0xFF1A1A1A), Color(0xFF0B0B0B)))
        )
    ) {
        when (state.contentMode) {
            ContentMode.LIVE -> {
                channel?.let {
                    PlayerSurface(
                        url = it.url,
                        modifier = Modifier.matchParentSize().alpha(0.82f)
                    )
                }
            }
            else -> {
                AsyncImage(
                    model = vod?.posterUrl,
                    contentDescription = vod?.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize().alpha(0.48f)
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = when (state.contentMode) {
                    ContentMode.LIVE -> channel?.name ?: "Canlı TV"
                    ContentMode.MOVIES, ContentMode.SERIES -> vod?.title ?: "VOD"
                },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = when (state.contentMode) {
                    ContentMode.LIVE -> buildString {
                        append(channel?.group.orEmpty())
                        channel?.currentProgram?.title?.takeIf { it.isNotBlank() }?.let { append(" • Şimdi: $it") }
                        channel?.nextProgram?.title?.takeIf { it.isNotBlank() }?.let { append(" • Sıradaki: $it") }
                    }.ifBlank { "EPG bilgisi yok" }
                    ContentMode.MOVIES, ContentMode.SERIES -> buildString {
                        vod?.year?.let { append(it) }
                        vod?.rating?.let {
                            if (isNotBlank()) append(" • ")
                            append("Puan: ").append(it)
                        }
                        vod?.plot?.takeIf { it.isNotBlank() }?.let {
                            if (isNotBlank()) append(" • ")
                            append(it)
                        }
                    }.ifBlank { "Detay bilgisi yok" }
                },
                color = Color.LightGray,
                maxLines = 3
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TvButton(text = if (state.contentMode == ContentMode.LIVE) "İzlemeye Devam Et" else "Aç", onClick = onPrimaryAction)
                if (state.contentMode == ContentMode.LIVE && channel != null) {
                    TvButton(text = "PiP", icon = Icons.Default.PictureInPictureAlt, onClick = onEnterPip)
                    TvButton(
                        text = if (channel.id in state.favoriteIds) "Listemde" else "Listeye Ekle",
                        icon = if (channel.id in state.favoriteIds) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        onClick = { onToggleFavorite(channel.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TopControlBar(
    state: MainUiState,
    firstMenuRequester: FocusRequester,
    onSearch: (String) -> Unit,
    onChangeMode: (ContentMode) -> Unit,
    onToggleFavoritesFilter: () -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().background(Color(0xEE0B0B0B)).padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().focusGroup(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ModeChip("Canlı", state.contentMode == ContentMode.LIVE, Icons.Default.LiveTv, Modifier.focusRequester(firstMenuRequester)) { onChangeMode(ContentMode.LIVE) }
            ModeChip("Filmler", state.contentMode == ContentMode.MOVIES, Icons.Default.LocalMovies) { onChangeMode(ContentMode.MOVIES) }
            ModeChip("Diziler", state.contentMode == ContentMode.SERIES, Icons.Default.VideoLibrary) { onChangeMode(ContentMode.SERIES) }
            ModeChip("Favoriler", state.onlyFavorites, Icons.Default.Favorite) { onToggleFavoritesFilter() }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, null) }
            IconButton(onClick = onLogout) { Icon(Icons.Default.ExitToApp, null) }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearch,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Ara") },
            leadingIcon = { Icon(Icons.Default.Search, null) }
        )
    }
}

@Composable
private fun ModeChip(
    text: String,
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    FocusableCard(
        modifier = modifier,
        selected = selected,
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Icon(icon, null)
            Spacer(Modifier.width(6.dp))
            Text(text, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CategoryChips(ids: List<Pair<String, String>>, selectedId: String, onSelected: (String) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(ids) { (id, title) ->
            FilterChip(selected = selectedId == id, onClick = { onSelected(id) }, label = { Text(title) })
        }
    }
}

@Composable
private fun PosterRow(
    title: String,
    items: List<Channel>,
    isFavorite: (Channel) -> Boolean,
    onSelectChannel: (Channel) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(items, key = { it.id }) { channel ->
                ChannelCard(channel = channel, isFavorite = isFavorite(channel), onClick = { onSelectChannel(channel) }, onToggleFavorite = { onToggleFavorite(channel.id) })
            }
        }
    }
}

@Composable
private fun VodRow(title: String, items: List<VodItem>, onSelect: (VodItem) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(items, key = { it.id }) { item ->
                VodCard(item = item, onClick = { onSelect(item) })
            }
        }
    }
}

@Composable
private fun ChannelCard(channel: Channel, isFavorite: Boolean, onClick: () -> Unit, onToggleFavorite: () -> Unit) {
    FocusableCard(selected = false, onClick = onClick) {
        Column(modifier = Modifier.width(230.dp).padding(12.dp)) {
            AsyncImage(model = channel.logo, contentDescription = channel.name, modifier = Modifier.fillMaxWidth().height(120.dp), contentScale = ContentScale.Crop)
            Spacer(Modifier.height(10.dp))
            Text(channel.name, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(channel.group.orEmpty(), color = Color.LightGray, maxLines = 1)
            channel.currentProgram?.title?.let {
                Spacer(Modifier.height(6.dp))
                AssistChip(onClick = {}, enabled = false, label = { Text("Şimdi: $it") }, colors = AssistChipDefaults.assistChipColors(disabledContainerColor = Color(0xFF1D1D1D), disabledLabelColor = Color.White))
            }
            channel.nextProgram?.title?.let { Text("Sıradaki: $it", color = Color(0xFFB8B8B8), maxLines = 1) }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onToggleFavorite) {
                Icon(if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null)
                Spacer(Modifier.width(6.dp))
                Text(if (isFavorite) "Favoriden çıkar" else "Favoriye ekle")
            }
        }
    }
}

@Composable
private fun VodCard(item: VodItem, onClick: () -> Unit) {
    FocusableCard(selected = false, onClick = onClick) {
        Column(modifier = Modifier.width(220.dp).padding(10.dp)) {
            AsyncImage(model = item.posterUrl, contentDescription = item.title, modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f), contentScale = ContentScale.Crop)
            Spacer(Modifier.height(10.dp))
            Text(item.title, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(item.categoryName, color = Color.LightGray, maxLines = 1)
            item.rating?.let { Text("Puan: $it", color = Color(0xFFB8B8B8)) }
        }
    }
}

@Composable
private fun FocusableCard(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Card(
        modifier = modifier
            .border(
                BorderStroke(
                    width = if (focused || selected) 2.dp else 1.dp,
                    color = if (focused || selected) MaterialTheme.colorScheme.primary else Color(0x33FFFFFF)
                ),
                RoundedCornerShape(18.dp)
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        colors = CardDefaults.cardColors(containerColor = if (focused) Color(0xFF1C1C1C) else Color(0xFF131313)),
        shape = RoundedCornerShape(18.dp)
    ) { content() }
}

@Composable
private fun TvButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    FocusableCard(selected = false, onClick = onClick) {
        Row(modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text)
        }
    }
}

@Composable
private fun PlayerSurface(url: String, modifier: Modifier = Modifier) {
    if (LocalInspectionMode.current) {
        Box(modifier.background(Color.DarkGray))
        return
    }
    val context = androidx.compose.ui.platform.LocalContext.current
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    AndroidView(
        modifier = modifier,
        factory = { ctx -> PlayerView(ctx).apply { useController = false; this.player = player } }
    )
}
