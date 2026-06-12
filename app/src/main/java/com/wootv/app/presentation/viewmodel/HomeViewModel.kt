package com.wootv.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wootv.app.domain.model.Channel
import com.wootv.app.domain.model.Playlist
import com.wootv.app.domain.repository.ChannelRepository
import com.wootv.app.domain.usecase.playlist.AddPlaylistUseCase
import com.wootv.app.domain.usecase.playlist.DeletePlaylistUseCase
import com.wootv.app.domain.usecase.playlist.GetPlaylistsUseCase
import com.wootv.app.domain.usecase.playlist.RefreshPlaylistUseCase
import com.wootv.app.data.config.DefaultPlaylistsConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class MainCategory {
    TV,
    MOVIES,
    SERIES,
    ANIME
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getPlaylistsUseCase: GetPlaylistsUseCase,
    private val addPlaylistUseCase: AddPlaylistUseCase,
    private val refreshPlaylistUseCase: RefreshPlaylistUseCase,
    private val deletePlaylistUseCase: DeletePlaylistUseCase,
    private val channelRepository: ChannelRepository
) : ViewModel() {

    val playlists: StateFlow<List<Playlist>> = getPlaylistsUseCase()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val playlistNameMap: StateFlow<Map<Long, String>> = playlists
        .map { list -> list.associate { it.id to it.name } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    private val _selectedPlaylistId = MutableStateFlow<Long?>(null)
    val selectedPlaylistId: StateFlow<Long?> = _selectedPlaylistId

    private val _selectedCategory = MutableStateFlow(MainCategory.TV)
    val selectedCategory: StateFlow<MainCategory> = _selectedCategory

    private val _selectedGroup = MutableStateFlow<String?>(null)
    val selectedGroup: StateFlow<String?> = _selectedGroup

    private val _showOnlyFavorites = MutableStateFlow(false)
    val showOnlyFavorites: StateFlow<Boolean> = _showOnlyFavorites

    private val _allChannels = MutableStateFlow<List<Channel>>(emptyList())
    val allChannels: StateFlow<List<Channel>> = _allChannels

    companion object {
        private val NoticiasKeywords = listOf(
            "ntn24", "cablenoticias", "caracol internacional", "noticias rcn", "rcn", "citytv",
            "ecuavisa", "teleamazonas", "tc televisión", "tc television", "rtu", "rts",
            "foro tv", "milenio", "adn 40", "n+", "excelsior", "canal n", "rpp",
            "tv perú noticias", "tv peru noticias", "latina noticias", "atv+"
        )

        private val DeportesKeywords = listOf(
            "el canal del fútbol", "el canal del futbol", "ecdf", "goltv", "espn", "win sports",
            "dsports", "directv sports", "claro sports", "fox sports", "tyc sports"
        )

        private val PeliculasKeywords = listOf(
            "hbo", "tnt", "space", "cinecanal", "star channel", "fx", "universal tv", "universal",
            "studio universal", "paramount network", "paramount", "amc", "axn", "golden", "golden edge",
            "cine latino", "de película", "de pelicula"
        )

        private val InfantilKeywords = listOf(
            "cartoon network", "nickelodeon", "disney channel", "disney", "discovery kids",
            "dreamworks", "tooncast", "cartoonito", "nick jr", "baby tv"
        )
    }

    private fun cleanCategoryName(name: String): String {
        // 1. Strip country prefixes like "CO | ", "MX - ", etc.
        val prefixRegex = Regex("^(?i)(co|mx|es|ec|pe|ar|cl|us|it|fr|pt|br|col|mex|ecu|per|colombia)\\s*[\\-_|:\\s]\\s*")
        val withoutPrefix = name.trim().replace(prefixRegex, "")
        
        // 2. Remove emojis and miscellaneous symbols
        val emojiPattern = Regex("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF\\u2600-\\u27BF\\u2300-\\u23FF\\u2B50\\u2B06\\u2190-\\u21FF]|\\p{So}")
        val noEmojis = withoutPrefix.replace(emojiPattern, "")
        
        // 3. Replace special characters with space
        val cleanChars = noEmojis.replace(Regex("[\\-_|:\\[\\]()➔♦🔥💥⚡⭐📍✨📌,/+.\\\\&]"), " ")
        
        // 4. Collapse spaces and trim
        val cleaned = cleanChars.replace(Regex("\\s+"), " ").trim()
        
        // 5. Convert to Title Case
        return cleaned.split(" ").filter { it.isNotBlank() }.joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }

    val availableGroups: StateFlow<List<String>> = _allChannels
        .map { channels ->
            val fixedCategories = listOf("Noticias", "Deportes", "Películas", "Infantil")
            channels.mapNotNull { it.groupTitle }
                .map { cleanCategoryName(it) }
                .filter { it.isNotBlank() && it !in fixedCategories }
                .distinct()
                .sortedWith(String.CASE_INSENSITIVE_ORDER)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val filteredChannels: StateFlow<List<Channel>> = combine(
        _allChannels,
        _selectedCategory,
        _selectedGroup,
        _showOnlyFavorites
    ) { channels, category, selectedGrp, showOnlyFavs ->
        val categoryChannels = if (selectedGrp != null) {
            val fixedCategories = listOf("Noticias", "Deportes", "Películas", "Infantil")
            if (selectedGrp in fixedCategories) {
                channels.filter { channel ->
                    val cleanName = channel.name.lowercase()
                    val group = (channel.groupTitle ?: "").lowercase()
                    val cleanGroup = cleanCategoryName(group).lowercase()
                    
                    when (selectedGrp) {
                        "Noticias" -> {
                            // Explicitly exclude sports, movies, and kids channels to prevent overlap/substring bugs
                            val isSports = cleanName.contains("sports") || cleanName.contains("sport") || cleanName.contains("deportes") || cleanName.contains("deporte") || cleanName.contains("futbol") || cleanName.contains("fútbol") || cleanName.contains("espn") || cleanName.contains("win") || cleanName.contains("fox") || cleanName.contains("goltv") || cleanName.contains("tyc") || cleanGroup.contains("deportes") || cleanGroup.contains("sports")
                            val isMovies = cleanName.contains("hbo") || cleanName.contains("tnt") || cleanName.contains("space") || cleanName.contains("cine") || cleanName.contains("movie") || cleanGroup.contains("película") || cleanGroup.contains("pelicula") || cleanGroup.contains("movies") || cleanGroup.contains("cine")
                            val isInfantil = cleanName.contains("cartoon") || cleanName.contains("nickelodeon") || cleanName.contains("disney") || cleanName.contains("discovery kids") || cleanName.contains("nick") || cleanGroup.contains("infantil") || cleanGroup.contains("kids")
                            
                            if (isSports || isMovies || isInfantil) {
                                false
                            } else {
                                val nameMatch = NoticiasKeywords.any { keyword ->
                                    if (keyword == "rts") {
                                        cleanName.contains("rts") && !cleanName.contains("sports") && !cleanName.contains("sport")
                                    } else {
                                        cleanName.contains(keyword)
                                    }
                                }
                                val groupMatch = cleanGroup.contains("noticias") || cleanGroup.contains("news") || cleanGroup.contains("informacion") || cleanGroup.contains("información")
                                nameMatch || groupMatch
                            }
                        }
                        "Deportes" -> {
                            val nameMatch = DeportesKeywords.any { cleanName.contains(it) }
                            val groupMatch = cleanGroup.contains("deportes") || cleanGroup.contains("sports")
                            nameMatch || groupMatch
                        }
                        "Películas" -> {
                            val nameMatch = PeliculasKeywords.any { cleanName.contains(it) }
                            val groupMatch = cleanGroup.contains("película") || cleanGroup.contains("pelicula") || cleanGroup.contains("movies") || cleanGroup.contains("cine") || cleanGroup.contains("cinema")
                            nameMatch || groupMatch
                        }
                        "Infantil" -> {
                            val nameMatch = InfantilKeywords.any { cleanName.contains(it) }
                            val groupMatch = cleanGroup.contains("infantil") || cleanGroup.contains("kids") || cleanGroup.contains("niños") || cleanGroup.contains("ninos")
                            nameMatch || groupMatch
                        }
                        else -> false
                    }
                }
            } else {
                channels.filter { channel ->
                    val group = channel.groupTitle ?: ""
                    cleanCategoryName(group).equals(selectedGrp, ignoreCase = true)
                }
            }
        } else {
            channels.filter { channel ->
                val group = channel.groupTitle ?: ""
                when (category) {
                    MainCategory.ANIME -> {
                        group.contains("anime", ignoreCase = true)
                    }
                    MainCategory.MOVIES -> {
                        (group.contains("cine", ignoreCase = true) || 
                         group.contains("movie", ignoreCase = true) || 
                         group.contains("cinema", ignoreCase = true) || 
                         group.contains("pelicula", ignoreCase = true) || 
                         group.contains("película", ignoreCase = true) || 
                         group.contains("estrenos", ignoreCase = true) || 
                         group.contains("sagas", ignoreCase = true)) && 
                        !group.contains("anime", ignoreCase = true) && 
                        !group.contains("series", ignoreCase = true) && 
                        !group.contains("season", ignoreCase = true) && 
                        !group.contains("temporada", ignoreCase = true)
                    }
                    MainCategory.SERIES -> {
                        (group.contains("series", ignoreCase = true) || 
                         group.contains("vix", ignoreCase = true) || 
                         group.contains("season", ignoreCase = true) || 
                         group.contains("temporada", ignoreCase = true) || 
                         group.contains("serie", ignoreCase = true) || 
                         group.contains("reality", ignoreCase = true)) && 
                        !group.contains("anime", ignoreCase = true)
                    }
                    MainCategory.TV -> {
                        val isAnime = group.contains("anime", ignoreCase = true)
                        val isMovie = (group.contains("cine", ignoreCase = true) || 
                                       group.contains("movie", ignoreCase = true) || 
                                       group.contains("cinema", ignoreCase = true) || 
                                       group.contains("pelicula", ignoreCase = true) || 
                                       group.contains("película", ignoreCase = true) || 
                                       group.contains("estrenos", ignoreCase = true) || 
                                       group.contains("sagas", ignoreCase = true)) && 
                                      !group.contains("anime", ignoreCase = true) && 
                                      !group.contains("series", ignoreCase = true) && 
                                      !group.contains("season", ignoreCase = true) && 
                                      !group.contains("temporada", ignoreCase = true)
                        val isSeries = (group.contains("series", ignoreCase = true) || 
                                        group.contains("vix", ignoreCase = true) || 
                                        group.contains("season", ignoreCase = true) || 
                                        group.contains("temporada", ignoreCase = true) || 
                                        group.contains("serie", ignoreCase = true) || 
                                        group.contains("reality", ignoreCase = true)) && 
                                       !group.contains("anime", ignoreCase = true)
                        
                        !isAnime && !isMovie && !isSeries
                    }
                }
            }
        }
        val filtered = if (showOnlyFavs) {
            categoryChannels.filter { it.isFavorite }
        } else {
            categoryChannels
        }
        
        // Show Colombia channels first, then the rest, preserving original order
        val (colombia, rest) = filtered.partition { channel ->
            val name = channel.name
            val group = channel.groupTitle ?: ""
            group.contains("colombia", ignoreCase = true) ||
            group.contains("co |", ignoreCase = true) ||
            group.contains("co:", ignoreCase = true) ||
            name.contains("colombia", ignoreCase = true) ||
            name.contains("co |", ignoreCase = true) ||
            name.startsWith("co:", ignoreCase = true) ||
            name.contains("(co)", ignoreCase = true) ||
            name.contains("[co]", ignoreCase = true)
        }
        colombia + rest
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                android.util.Log.d("HomeViewModel", "=== APP STARTING ===")
                
                // 1. Get current playlists from DB
                val currentPlaylists = getPlaylistsUseCase().first()
                android.util.Log.d("HomeViewModel", "Found ${currentPlaylists.size} playlists in DB")
                
                val currentUrls = currentPlaylists.map { it.url }.toSet()
                val defaultUrls = DefaultPlaylistsConfig.PLAYLISTS.map { it.second }.toSet()

                // Delete playlists from DB that are no longer in our defaults
                currentPlaylists.forEach { playlist ->
                    if (playlist.url !in defaultUrls) {
                        try {
                            deletePlaylistUseCase(playlist)
                            android.util.Log.d("HomeViewModel", "Deleted old playlist: ${playlist.name}")
                        } catch (e: Exception) {
                            android.util.Log.e("HomeViewModel", "Error deleting playlist: ${e.message}")
                        }
                    }
                }

                // 2. Add missing default playlists
                val updatedCurrentPlaylists = getPlaylistsUseCase().first()
                val updatedUrls = updatedCurrentPlaylists.map { it.url }.toSet()
                val updatedNames = updatedCurrentPlaylists.map { it.name }.toSet()

                val missingPlaylists = DefaultPlaylistsConfig.PLAYLISTS.filter { 
                    it.second !in updatedUrls && it.first !in updatedNames 
                }

                android.util.Log.d("HomeViewModel", "Adding ${missingPlaylists.size} missing playlists")
                
                missingPlaylists.forEach { (name, url) ->
                    try {
                        addPlaylistUseCase(name, url, null)
                        android.util.Log.d("HomeViewModel", "Added playlist: $name")
                    } catch (e: Exception) {
                        android.util.Log.e("HomeViewModel", "Error adding playlist $name: ${e.message}")
                    }
                }

                // 3. AUTO-REFRESH first playlist only (to avoid crashes)
                val finalPlaylists = getPlaylistsUseCase().first()
                if (finalPlaylists.isNotEmpty()) {
                    val firstPlaylist = finalPlaylists.first()
                    android.util.Log.d("HomeViewModel", "Auto-refreshing first playlist: ${firstPlaylist.name}")
                    try {
                        val count = refreshPlaylistUseCase(firstPlaylist)
                        android.util.Log.d("HomeViewModel", "Auto-refreshed $count channels from ${firstPlaylist.name}")
                    } catch (e: Exception) {
                        android.util.Log.e("HomeViewModel", "Error auto-refreshing: ${e.message}")
                    }
                }

                // 4. Load channels from first playlist
                loadChannelsFromFirstPlaylist()

            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "FATAL ERROR in init: ${e.message}")
                e.printStackTrace()
            } finally {
                _isLoading.value = false
                android.util.Log.d("HomeViewModel", "=== APP STARTED ===")
            }
        }
    }

    private var channelsJob: kotlinx.coroutines.Job? = null

    fun selectPlaylist(playlistId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _selectedPlaylistId.value = playlistId
            _selectedGroup.value = null
            channelsJob?.cancel()
            channelsJob = viewModelScope.launch {
                try {
                    channelRepository.getChannelsByPlaylist(playlistId).collect { channels ->
                        android.util.Log.d("HomeViewModel", "Loaded ${channels.size} channels for playlist: $playlistId")
                        _allChannels.value = channels
                        _isLoading.value = false
                    }
                } catch (e: Exception) {
                    android.util.Log.e("HomeViewModel", "Error loading playlist channels: ${e.message}")
                    _isLoading.value = false
                }
            }
        }
    }

    fun selectCategory(category: MainCategory) {
        _selectedGroup.value = null
        _selectedCategory.value = category
    }

    fun selectGroup(group: String?) {
        _selectedGroup.value = group
    }

    private suspend fun loadChannelsFromFirstPlaylist() {
        try {
            val finalPlaylists = getPlaylistsUseCase().first()
            if (finalPlaylists.isNotEmpty()) {
                val firstPlaylist = finalPlaylists.first()
                selectPlaylist(firstPlaylist.id)
            } else {
                android.util.Log.w("HomeViewModel", "No playlists to load channels from")
                loadAllChannels()
            }
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "Error in loadChannelsFromFirstPlaylist: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun loadAllChannels() {
        viewModelScope.launch {
            channelRepository.getAllChannels().collect { channels ->
                _allChannels.value = channels
            }
        }
    }

    fun setShowOnlyFavorites(show: Boolean) {
        _showOnlyFavorites.value = show
    }

    fun toggleFavorite(channelId: Long, isFavorite: Boolean) {
        viewModelScope.launch {
            channelRepository.toggleFavorite(channelId, isFavorite)
        }
    }

    fun refreshAllPlaylists() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                android.util.Log.d("HomeViewModel", "=== MANUAL REFRESH STARTED ===")
                
                val playlists = getPlaylistsUseCase().first()
                android.util.Log.d("HomeViewModel", "Refreshing ${playlists.size} playlists")
                
                playlists.forEach { playlist ->
                    try {
                        android.util.Log.d("HomeViewModel", "Refreshing: ${playlist.name}")
                        val count = refreshPlaylistUseCase(playlist)
                        android.util.Log.d("HomeViewModel", "Refreshed ${count} channels from ${playlist.name}")
                    } catch (e: Exception) {
                        android.util.Log.e("HomeViewModel", "Error refreshing ${playlist.name}: ${e.message}")
                    }
                }
                
                android.util.Log.d("HomeViewModel", "=== MANUAL REFRESH COMPLETED ===")
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "FATAL ERROR in refreshAllPlaylists: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
