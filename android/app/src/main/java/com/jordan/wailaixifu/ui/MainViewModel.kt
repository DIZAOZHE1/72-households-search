package com.jordan.wailaixifu.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jordan.wailaixifu.data.Brand
import com.jordan.wailaixifu.data.Catalog
import com.jordan.wailaixifu.data.CatalogRepository
import com.jordan.wailaixifu.data.Character
import com.jordan.wailaixifu.data.Episode
import com.jordan.wailaixifu.data.NO_MATCH
import com.jordan.wailaixifu.data.Progress
import com.jordan.wailaixifu.data.ProgressStore
import com.jordan.wailaixifu.data.Site
import com.jordan.wailaixifu.data.Storyline
import com.jordan.wailaixifu.data.searchRank
import java.util.Calendar
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The four modes of the query page, mirroring the web page's segmented control. */
enum class ViewMode(val label: String, val icon: String) {
    Episodes("剧集查询", "📺"),
    MainLines("主线剧情", "🧵"),
    Characters("角色查询", "🎭"),
    Picker("相似抽取", "🎯"),
    ;

    companion object {
        /** Modes offered by a dataset: 外来 has no characters or storylines. */
        fun of(catalog: Catalog?): List<ViewMode> {
            if (catalog == null) return listOf(Episodes)
            return buildList {
                add(Episodes)
                if (catalog.hasStorylines) add(MainLines)
                if (catalog.hasCharacters) add(Characters)
                if (catalog.hasCharacters || catalog.hasTags) add(Picker)
            }
        }
    }
}

/** Load state of the selected site's dataset. */
sealed interface CatalogState {
    data object Loading : CatalogState
    data object Failed : CatalogState
    data class Ready(val catalog: Catalog) : CatalogState
}

/** Episode-list filters, mirroring `st = {season, q, tag, main, lim, …}` on the source pages. */
data class EpisodeFilters(
    val section: Int = ALL_SECTIONS,
    val query: String = "",
    val tag: Int = NO_TAG,
    val mainOnly: Boolean = false,
    val onlyWant: Boolean = false,
    val hideSeen: Boolean = false,
    val limit: Int = INITIAL_LIMIT,
) {
    companion object {
        const val ALL_SECTIONS = 0
        const val NO_TAG = -1
        const val INITIAL_LIMIT = 60
        const val MORE_STEP = 120
    }
}

/** State of the 相似抽取 drawer. */
data class PickerState(
    val characterIds: Set<Int> = emptySet(),
    val tagIds: Set<Int> = emptySet(),
    val keyword: String = "",
    val count: Int = 3,
    val unseenOnly: Boolean = false,
    val drawn: List<Int> = emptyList(),
    val poolSize: Int = 0,
    val hasDrawn: Boolean = false,
)

/** Everything the query page needs in order to render itself. */
data class MainUiState(
    val site: Site = Site.default,
    val catalogState: CatalogState = CatalogState.Loading,
    val mode: ViewMode = ViewMode.Episodes,
    val filters: EpisodeFilters = EpisodeFilters(),
    val matches: List<Episode> = emptyList(),
    val loaded: List<Episode> = emptyList(),
    val characters: List<Character> = emptyList(),
    val storylines: List<Storyline> = emptyList(),
    val progress: Progress = Progress(),
    val picker: PickerState = PickerState(),
    val selectedCharacter: Int? = null,
    val selectedCharacterSection: Int = 0,
    val selectedStoryline: Int = 0,
) {
    val catalog: Catalog? get() = (catalogState as? CatalogState.Ready)?.catalog
    val brand: Brand get() = brandOf(site)
    val loading: Boolean get() = catalogState is CatalogState.Loading
    val availableModes: List<ViewMode> get() = ViewMode.of(catalog)
    val hasMore: Boolean get() = loaded.size < matches.size
}

fun brandOf(site: Site): Brand = when (site) {
    Site.WaiLai -> Brand.WaiLai
    Site.Traditional, Site.Modern -> Brand.QiErShi
}

@OptIn(FlowPreview::class)
class MainViewModel(
    private val repository: CatalogRepository,
    private val progressStore: ProgressStore,
) : ViewModel() {

    private val site = MutableStateFlow(Site.default)
    private val mode = MutableStateFlow(ViewMode.Episodes)
    private val filters = MutableStateFlow(EpisodeFilters())
    private val catalogState = MutableStateFlow<CatalogState>(CatalogState.Loading)
    private val picker = MutableStateFlow(PickerState())
    private val selectedCharacter = MutableStateFlow<Int?>(null)
    private val selectedCharacterSection = MutableStateFlow(0)
    private val selectedStoryline = MutableStateFlow(0)

    // Each site keeps its own input state so switching tabs never leaks one drama into the other.
    private val inputBySite = mutableMapOf<Site, Pair<ViewMode, EpisodeFilters>>()

    // Typing stays responsive: the light state updates immediately, the heavy filter is debounced.
    private val debouncedQuery = filters.map { it.query }
        .debounce { if (it.isEmpty()) 0L else SEARCH_DEBOUNCE_MS }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    /**
     * The filtered result set for the active site and filters, before the "show more" limit.
     * The reader's progress is part of the input because 隐藏已看 / 只看想看 depend on it.
     */
    private val matches: StateFlow<List<Episode>> =
        combine(catalogState, filters, debouncedQuery, progressStore.state) { state, f, debounced, progress ->
            val catalog = (state as? CatalogState.Ready)?.catalog ?: return@combine emptyList()
            filterEpisodes(catalog, f.copy(query = debounced), progress)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val loaded: StateFlow<List<Episode>> =
        combine(matches, filters) { all, f ->
            if (all.size <= f.limit) all else all.subList(0, f.limit)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val characters: StateFlow<List<Character>> =
        catalogState.map { (it as? CatalogState.Ready)?.catalog?.characters.orEmpty() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val storylines: StateFlow<List<Storyline>> =
        catalogState.map { (it as? CatalogState.Ready)?.catalog?.storylines.orEmpty() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Everything except the catalogue, the reader's progress and the current selection. */
    private val browsing: StateFlow<Browsing> =
        combine(mode, filters, loaded, matches, picker) { mode, filters, loaded, matches, picker ->
            Browsing(mode, filters, loaded, matches, picker)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, Browsing())

    /** The current screen selection: character, character season, storyline. */
    private val selection: StateFlow<Selection> =
        combine(
            selectedCharacter,
            selectedCharacterSection,
            selectedStoryline,
            progressStore.state,
        ) { character, characterSection, storyline, progress ->
            Selection(character, characterSection, storyline, progress)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, Selection())

    /** Recomputed whenever the site changes, so filters adapted to the dataset stay valid. */
    private fun filterEpisodes(catalog: Catalog, f: EpisodeFilters, progress: Progress): List<Episode> {
        val query = f.query.lowercase()
        val numeric = query.isNotEmpty() && query.all { it.isDigit() }
        val episodeNumber = if (numeric) query.toIntOrNull() else null

        val result = catalog.episodes.filter { episode ->
            if (f.section != EpisodeFilters.ALL_SECTIONS && episode.section != f.section) return@filter false
            if (f.tag != EpisodeFilters.NO_TAG && f.tag !in episode.tagIds) return@filter false
            if (f.mainOnly && episode.storyRefs.isEmpty()) return@filter false
            if (f.hideSeen && progress.isSeen(episode.progressKey)) return@filter false
            if (f.onlyWant && !progress.isWant(episode.progressKey)) return@filter false
            if (query.isEmpty()) return@filter true
            val rank = episode.searchRank(query, catalog.characterSearchKeys)
            rank < NO_MATCH || (episodeNumber != null && episodeNumber in episode.start..episode.end)
        }

        // Keyword hits are ordered title-first, synopsis second, character third.
        return if (query.isNotEmpty() && !numeric) {
            result.sortedBy { it.searchRank(query, catalog.characterSearchKeys) }
        } else {
            result
        }
    }

    /**
     * Built from a vararg [combine]: the stdlib only provides typed overloads up to five flows,
     * and this state genuinely needs six.
     */
    val state: StateFlow<MainUiState> =
        combine(
            site,
            catalogState,
            characters,
            storylines,
            browsing,
            selection,
        ) { values ->
            val activeSite = values[0] as Site
            val catalog = values[1] as CatalogState
            val characters = values[2] as List<Character>
            val storylines = values[3] as List<Storyline>
            val browsing = values[4] as Browsing
            val selection = values[5] as Selection
            MainUiState(
                site = activeSite,
                catalogState = catalog,
                mode = browsing.mode,
                filters = browsing.filters,
                loaded = browsing.loaded,
                matches = browsing.matches,
                picker = browsing.picker,
                characters = characters,
                storylines = storylines,
                selectedCharacter = selection.character,
                selectedCharacterSection = selection.characterSection,
                selectedStoryline = selection.storyline,
                progress = selection.progress,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = MainUiState(),
        )

    private val _selectedSite = MutableStateFlow(Site.default)
    val selectedSite: StateFlow<Site> = _selectedSite.asStateFlow()

    init {
        selectSite(Site.default)
    }

    /** Switches the visible site, restoring the search state that site was left in. */
    fun selectSite(target: Site) {
        if (_selectedSite.value == target && catalogState.value is CatalogState.Ready) return
        rememberCurrentInput()
        _selectedSite.value = target
        site.value = target
        val (savedMode, savedFilters) = inputBySite[target]
            ?: (ViewMode.Episodes to EpisodeFilters())
        mode.value = savedMode
        filters.value = savedFilters
        selectedCharacter.value = null
        selectedCharacterSection.value = 0
        selectedStoryline.value = 0
        picker.value = PickerState()
        load(target)
    }

    fun retry() = load(_selectedSite.value)

    private fun load(target: Site) {
        viewModelScope.launch {
            catalogState.value = CatalogState.Loading
            val catalog = repository.load(target.dataset)
            catalogState.value = catalog?.let { CatalogState.Ready(it) } ?: CatalogState.Failed
        }
    }

    private fun rememberCurrentInput() {
        inputBySite[_selectedSite.value] = mode.value to filters.value
    }

    /* ------------------------------------------------------------------------------------- */
    /* Episode list                                                                          */
    /* ------------------------------------------------------------------------------------- */

    fun setMode(target: ViewMode) {
        if (mode.value == target) return
        // A dataset without characters/storylines never offers those modes.
        if (target !in state.value.availableModes) return
        mode.value = target
    }

    fun setSection(section: Int) = filters.update {
        if (it.section == section) it else it.copy(section = section, limit = EpisodeFilters.INITIAL_LIMIT)
    }

    fun setQuery(text: String) = filters.update {
        if (it.query == text) it else it.copy(query = text, limit = EpisodeFilters.INITIAL_LIMIT)
    }

    fun clearQuery() = setQuery("")

    fun toggleTag(tag: Int) = filters.update {
        val next = if (it.tag == tag) EpisodeFilters.NO_TAG else tag
        it.copy(tag = next, limit = EpisodeFilters.INITIAL_LIMIT)
    }

    fun toggleMainOnly() = filters.update {
        it.copy(mainOnly = !it.mainOnly, limit = EpisodeFilters.INITIAL_LIMIT)
    }

    fun toggleOnlyWant() = filters.update {
        it.copy(onlyWant = !it.onlyWant, limit = EpisodeFilters.INITIAL_LIMIT)
    }

    fun toggleHideSeen() = filters.update {
        it.copy(hideSeen = !it.hideSeen, limit = EpisodeFilters.INITIAL_LIMIT)
    }

    fun showMore() = filters.update { it.copy(limit = it.limit + EpisodeFilters.MORE_STEP) }

    /* ------------------------------------------------------------------------------------- */
    /* Characters                                                                            */
    /* ------------------------------------------------------------------------------------- */

    fun selectCharacter(id: Int) {
        selectedCharacter.value = id
        selectedCharacterSection.value = 0
    }

    fun setCharacterSection(section: Int) {
        selectedCharacterSection.value = section
    }

    /** Stories featuring [characterId], optionally narrowed to one season. */
    fun characterEpisodes(characterId: Int, section: Int): List<Episode> {
        val catalog = state.value.catalog ?: return emptyList()
        return catalog.episodes.filter { characterId in it.characterIds }
            .filter { section == 0 || it.section == section }
    }

    fun characterSeasonCounts(characterId: Int): Map<Int, Int> {
        val catalog = state.value.catalog ?: return emptyMap()
        return catalog.episodes.filter { characterId in it.characterIds }
            .groupingBy { it.section }
            .eachCount()
            .toSortedMap()
    }

    /* ------------------------------------------------------------------------------------- */
    /* Storylines                                                                            */
    /* ------------------------------------------------------------------------------------- */

    fun selectStoryline(index: Int) {
        selectedStoryline.value = index
    }

    /* ------------------------------------------------------------------------------------- */
    /* 相似抽取                                                                               */
    /* ------------------------------------------------------------------------------------- */

    fun togglePickerCharacter(id: Int) = picker.update {
        it.copy(characterIds = it.characterIds.toggle(id))
    }

    fun togglePickerTag(id: Int) = picker.update {
        it.copy(tagIds = it.tagIds.toggle(id))
    }

    fun setPickerKeyword(text: String) = picker.update { it.copy(keyword = text) }

    fun setPickerCount(count: Int) = picker.update { it.copy(count = count) }

    fun togglePickerUnseen() = picker.update { it.copy(unseenOnly = !it.unseenOnly) }

    /** Randomly draws [PickerState.count] stories from the pool that satisfies every condition. */
    fun drawPicker() {
        val catalog = state.value.catalog ?: return
        val settings = picker.value
        val keywords = settings.keyword.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }

        val pool = catalog.episodes.filter { episode ->
            settings.characterIds.all { it in episode.characterIds } &&
                settings.tagIds.all { it in episode.tagIds } &&
                (keywords.isEmpty() || keywords.any { episode.searchRank(it, catalog.characterSearchKeys) < NO_MATCH }) &&
                (!settings.unseenOnly || !progressStore.state.value.isSeen(episode.progressKey))
        }

        picker.value = settings.copy(
            drawn = pool.shuffled().take(settings.count).map { it.index },
            poolSize = pool.size,
            hasDrawn = true,
        )
    }

    /** 🎲 随便看一集: any unread story, or any story once everything has been read. */
    fun pickRandom(): Episode? {
        val catalog = state.value.catalog ?: return null
        val unread = catalog.episodes.filter { !progressStore.state.value.isSeen(it.progressKey) }
        return (if (unread.isNotEmpty()) unread else catalog.episodes).randomOrNull()
    }

    /**
     * 📅 今日推荐: hashed from the date so the same day always suggests the same story, skipping
     * stories already marked as seen — the same rule as `pickToday()` on the source page.
     */
    fun pickToday(): Episode? {
        val catalog = state.value.catalog ?: return null
        if (catalog.episodes.isEmpty()) return null

        val today = Calendar.getInstance()
        val date = "${today.get(Calendar.YEAR)}-${today.get(Calendar.MONTH) + 1}-${today.get(Calendar.DAY_OF_MONTH)}"

        val remembered = progressStore.state.value
            .takeIf { it.todayDate == date }
            ?.todayKey
            ?.let { key -> catalog.episodes.firstOrNull { it.progressKey == key } }
        if (remembered != null) return remembered

        var hash = 0
        for (ch in date) hash = (hash * 31 + ch.code) and 0xFFFFFFFF.toInt()
        var index = (hash.toLong() and 0xFFFFFFFFL).mod(catalog.episodes.size.toLong()).toInt()
        var steps = 0
        while (progressStore.state.value.isSeen(catalog.episodes[index].progressKey) && steps < catalog.episodes.size) {
            index = (index + 1) % catalog.episodes.size
            steps++
        }
        val picked = catalog.episodes[index]
        progressStore.rememberToday(date, picked.progressKey)
        return picked
    }

    fun episodeAt(index: Int): Episode? = state.value.catalog?.episodes?.getOrNull(index)

    /* ------------------------------------------------------------------------------------- */
    /* Progress                                                                              */
    /* ------------------------------------------------------------------------------------- */

    fun toggleSeen(key: String) = progressStore.toggle(key, ProgressStore.Kind.Seen)

    fun toggleWant(key: String) = progressStore.toggle(key, ProgressStore.Kind.Want)

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 200L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                MainViewModel(CatalogRepository(app), ProgressStore(app))
            }
        }
    }
}

private fun Set<Int>.toggle(value: Int): Set<Int> =
    if (value in this) this - value else this + value

/** Episode-list state, grouped so the UI state can be assembled from six flows instead of thirteen. */
private data class Browsing(
    val mode: ViewMode = ViewMode.Episodes,
    val filters: EpisodeFilters = EpisodeFilters(),
    val loaded: List<Episode> = emptyList(),
    val matches: List<Episode> = emptyList(),
    val picker: PickerState = PickerState(),
)

/** Current drill-down selection plus the reader's progress. */
private data class Selection(
    val character: Int? = null,
    val characterSection: Int = 0,
    val storyline: Int = 0,
    val progress: Progress = Progress(),
)
