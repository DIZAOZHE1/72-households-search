package com.jordan.wailaixifu.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jordan.wailaixifu.R
import com.jordan.wailaixifu.data.Brand
import com.jordan.wailaixifu.data.Catalog
import com.jordan.wailaixifu.data.Episode
import com.jordan.wailaixifu.data.Progress
import com.jordan.wailaixifu.data.Site
import com.jordan.wailaixifu.data.SiteTheme
import com.jordan.wailaixifu.ui.components.EpisodeCard
import com.jordan.wailaixifu.ui.components.EpisodeContext
import com.jordan.wailaixifu.ui.components.Pill
import com.jordan.wailaixifu.ui.components.WindowStrip
import com.jordan.wailaixifu.ui.components.drawLattice
import com.jordan.wailaixifu.ui.components.drawMasonry
import com.jordan.wailaixifu.ui.theme.LocalBrandColors

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val brand = state.brand
    val scheme = MaterialTheme.colorScheme
    val tags = state.catalog?.tags.orEmpty()

    val context = remember(
        state.site,
        brand,
        state.characters,
        tags,
        state.storylines,
        state.progress,
    ) {
        EpisodeContext(
            theme = state.site.theme,
            brand = brand,
            characters = state.characters,
            tags = tags,
            storylines = state.storylines,
            seenKeys = state.progress.seen,
            wantKeys = state.progress.want,
        )
    }

    var overlay by remember { mutableStateOf<Overlay?>(null) }
    val openShare: (Int) -> Unit = { index -> overlay = Overlay.Share(index) }
    val itemLabels = episodeItemLabels()

    // Collapse progress of the cover: 0 while untouched, 1 once scrolled past its own height.
    val listState = rememberLazyListState()
    var coverHeightPx by remember { mutableIntStateOf(0) }
    val rawOffset = listState.firstVisibleItemIndex * 320 + listState.firstVisibleItemScrollOffset
    val collapse by animateFloatAsState(
        targetValue = if (coverHeightPx <= 0) {
            0f
        } else {
            (rawOffset.toFloat() / coverHeightPx).coerceIn(0f, 1f)
        },
        label = "coverCollapse",
    )

    val navBars = WindowInsets.navigationBars.asPaddingValues()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background),
    ) {
        CoverHeader(
            theme = state.site.theme,
            brand = brand,
            collapse = collapse,
            onHeightMeasured = { coverHeightPx = it },
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,
            contentPadding = PaddingValues(
                start = 14.dp,
                end = 14.dp,
                top = 14.dp,
                bottom = 24.dp + contentPadding.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "progress") {
                ProgressCard(progress = state.progress, catalog = state.catalog)
            }

            item(key = "modes") {
                ModeSwitcher(
                    modes = state.availableModes,
                    current = state.mode,
                    onSelect = viewModel::setMode,
                )
            }

            when (state.mode) {
                ViewMode.Episodes -> episodeItems(
                    state = state,
                    context = context,
                    viewModel = viewModel,
                    onShare = openShare,
                    onPick = { title, index -> overlay = Overlay.Pick(title, index) },
                    labels = itemLabels,
                )

                ViewMode.MainLines -> item(key = "storylines") {
                    StorylineSection(
                        state = state,
                        context = context,
                        viewModel = viewModel,
                        onOverlay = openShare,
                    )
                }

                ViewMode.Characters -> item(key = "characters") {
                    CharacterSection(
                        state = state,
                        context = context,
                        viewModel = viewModel,
                        onOverlay = openShare,
                    )
                }

                ViewMode.Picker -> item(key = "picker") {
                    PickerSection(
                        state = state,
                        context = context,
                        viewModel = viewModel,
                        onOverlay = openShare,
                    )
                }
            }

            item(key = "footer") { Footer(brand) }
        }
    }

    when (val current = overlay) {
        null -> Unit
        is Overlay.Share -> ShareModal(
            episode = viewModel.episodeAt(current.episodeIndex),
            context = context,
            onDismiss = { overlay = null },
        )

        is Overlay.Pick -> PickModal(
            title = current.title,
            episode = viewModel.episodeAt(current.episodeIndex),
            context = context,
            onReroll = {
                viewModel.pickRandom()?.let { overlay = Overlay.Pick(current.title, it.index) }
            },
            onShare = { overlay = Overlay.Share(current.episodeIndex) },
            onDismiss = { overlay = null },
        )
    }
}

/** Modal overlays the screen can show. */
private sealed interface Overlay {
    data class Share(val episodeIndex: Int) : Overlay
    data class Pick(val title: String, val episodeIndex: Int) : Overlay
}

/* ------------------------------------------------------------------------------------------- */
/* Cover header with the collapsing cover                                                        */
/* ------------------------------------------------------------------------------------------- */

/**
 * The cover of each site. As the list scrolls past its height, the cover folds: the decorative
 * line and subtitle fade away, the title drops to a compact size, and the ornament (gold lattice
 * for 外来, 青砖灰瓦 for 传统, a flat wash for 现代) fades out — so the cover collapses while every
 * site keeps its own look.
 */
@Composable
private fun CoverHeader(
    theme: SiteTheme,
    brand: Brand,
    collapse: Float,
    onHeightMeasured: (Int) -> Unit,
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val palette = coverPalette(theme)
    val scheme = MaterialTheme.colorScheme

    val verticalPadding = (18f * (1f - collapse) + 8f * collapse).dp
    val titleSize = (28f * (1f - collapse) + 17f * collapse).sp
    val decoAlpha = (1f - collapse * 1.6f).coerceAtLeast(0f)
    val subAlpha = (1f - collapse * 1.8f).coerceAtLeast(0f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clipToBounds()
            .onSizeChanged { onHeightMeasured(it.height) }
            .drawBehind {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(palette.top, palette.bottom, palette.top),
                    ),
                )
                val ornamentAlpha = 1f - collapse
                if (ornamentAlpha <= 0.01f) return@drawBehind
                when (palette.ornament) {
                    Ornament.Lattice -> drawLattice(
                        cell = 34.dp.toPx(),
                        color = palette.accent.copy(alpha = 0.10f * ornamentAlpha),
                    )

                    Ornament.Masonry -> drawMasonry(
                        mortar = palette.accent.copy(alpha = 0.16f * ornamentAlpha),
                        rows = 5f,
                    )

                    Ornament.Flat -> Unit
                }
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = statusBarPadding)
                .padding(horizontal = 18.dp)
                .padding(vertical = verticalPadding),
        ) {
            if (decoAlpha > 0.05f) {
                Text(
                    text = brand.deco,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.accent.copy(alpha = 0.9f * decoAlpha),
                    letterSpacing = if (theme == SiteTheme.Traditional) 2.sp else 1.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (theme == SiteTheme.Traditional && collapse < 0.55f) {
                    SealTile(alpha = (1f - collapse * 1.8f).coerceAtLeast(0f))
                    Spacer(Modifier.width(9.dp))
                }
                Text(
                    text = brand.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = titleSize,
                        letterSpacing = if (theme == SiteTheme.Modern) 0.sp else 2.sp,
                    ),
                    color = palette.title,
                )
                Text(
                    text = brand.titleAccent,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = titleSize,
                        letterSpacing = if (theme == SiteTheme.Modern) 0.sp else 2.sp,
                    ),
                    color = palette.titleAccent,
                )
            }

            if (subAlpha > 0.05f) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = brand.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.sub.copy(alpha = subAlpha),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // 满洲窗 four-colour strip along the bottom edge of the cover. It doubles as the cover's
        // bottom rule, so no extra divider is drawn next to it: an added hairline simply painted
        // over the strip and made the cover look doubled when collapsed.
        WindowStrip(
            modifier = Modifier.align(Alignment.BottomCenter),
            height = (6f * (1f - collapse) + 3f * collapse).dp,
        )
    }
}

/** The 花砖 square shown before the 传统 title, echoing `.banner h1::before`. */
@Composable
private fun SealTile(alpha: Float) {
    val strip = LocalBrandColors.current.windowStrip
    val frame = LocalBrandColors.current.frame
    Box(
        Modifier
            .size(26.dp)
            .background(frame.copy(alpha = alpha * 0.9f))
            .padding(2.dp),
    ) {
        Row(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(strip[0].copy(alpha = alpha)),
                )
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(strip[2].copy(alpha = alpha)),
                )
            }
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(strip[3].copy(alpha = alpha)),
                )
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(strip[1].copy(alpha = alpha)),
                )
            }
        }
    }
}

private data class CoverPalette(
    val top: Color,
    val bottom: Color,
    val title: Color,
    val titleAccent: Color,
    val accent: Color,
    val sub: Color,
    val ornament: Ornament,
)

private enum class Ornament { Lattice, Masonry, Flat }

@Composable
private fun coverPalette(theme: SiteTheme): CoverPalette {
    val brand = LocalBrandColors.current
    val scheme = MaterialTheme.colorScheme
    return when (theme) {
        SiteTheme.Lingnan -> CoverPalette(
            top = brand.bannerTop,
            bottom = brand.bannerMid,
            title = brand.bannerText,
            titleAccent = brand.bannerText,
            accent = scheme.secondary,
            sub = brand.bannerSub,
            ornament = Ornament.Lattice,
        )

        SiteTheme.Traditional -> CoverPalette(
            top = brand.bannerTop,
            bottom = brand.bannerMid,
            title = brand.bannerText,
            titleAccent = brand.mark,
            accent = scheme.onSurfaceVariant,
            sub = brand.bannerSub,
            ornament = Ornament.Masonry,
        )

        SiteTheme.Modern -> CoverPalette(
            top = brand.bannerTop,
            bottom = brand.bannerMid,
            title = brand.bannerText,
            titleAccent = scheme.primary,
            accent = scheme.onSurfaceVariant,
            sub = brand.bannerSub,
            ornament = Ornament.Flat,
        )
    }
}

/* ------------------------------------------------------------------------------------------- */
/* Progress card                                                                                 */
/* ------------------------------------------------------------------------------------------- */

@Composable
private fun ProgressCard(progress: Progress, catalog: Catalog?) {
    val scheme = MaterialTheme.colorScheme

    val summary = remember(progress, catalog) {
        catalog?.let { summaryOf(it, progress) }
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = scheme.surface,
        border = BorderStroke(1.dp, scheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            if (summary == null || summary.frontier == 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.progress_title),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.progress_not_started),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = scheme.primary,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = stringResource(R.string.progress_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(9.dp))
                ProgressBar(fraction = 0f)
            } else {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.progress_title),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(
                            R.string.progress_frontier,
                            summary.frontier,
                            summary.frontierSeen,
                            summary.frontierTotal,
                        ),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = scheme.primary,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = buildString {
                            append(
                                stringResource(
                                    R.string.progress_total,
                                    summary.totalSeen,
                                    summary.total,
                                ),
                            )
                            if (summary.wantCount > 0) {
                                append(stringResource(R.string.progress_want, summary.wantCount))
                            }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(9.dp))
                val fraction = if (summary.frontierTotal == 0) {
                    0f
                } else {
                    summary.frontierSeen.toFloat() / summary.frontierTotal
                }
                ProgressBar(fraction = fraction.coerceIn(0f, 1f))
            }
        }
    }
}

private data class ProgressSummary(
    val frontier: Int,
    val frontierSeen: Int,
    val frontierTotal: Int,
    val totalSeen: Int,
    val total: Int,
    val wantCount: Int,
)

private fun summaryOf(catalog: Catalog, progress: Progress): ProgressSummary {
    var seenEpisodes = 0
    var wantCount = 0
    var frontier = 0
    catalog.episodes.forEach { episode ->
        if (progress.isSeen(episode.progressKey)) {
            seenEpisodes += episode.episodeCount
            if (episode.section > frontier) frontier = episode.section
        }
        if (progress.isWant(episode.progressKey)) wantCount++
    }
    val frontierSeen = if (frontier == 0) {
        0
    } else {
        catalog.episodes
            .filter { it.section == frontier && progress.isSeen(it.progressKey) }
            .sumOf { it.episodeCount }
    }
    return ProgressSummary(
        frontier = frontier,
        frontierSeen = frontierSeen,
        frontierTotal = catalog.episodesPerSection[frontier] ?: 0,
        totalSeen = seenEpisodes,
        total = catalog.totalEpisodes,
        wantCount = wantCount,
    )
}

@Composable
private fun ProgressBar(fraction: Float) {
    val strip = LocalBrandColors.current.windowStrip
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxWidth()
            .height(7.dp)
            .background(scheme.surfaceVariant, RoundedCornerShape(999.dp)),
    ) {
        if (fraction > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(
                        brush = Brush.horizontalGradient(
                            List(8) { strip[it % strip.size] },
                        ),
                        shape = RoundedCornerShape(999.dp),
                    ),
            )
        }
    }
}

/* ------------------------------------------------------------------------------------------- */
/* Mode switcher                                                                                 */
/* ------------------------------------------------------------------------------------------- */

@Composable
private fun ModeSwitcher(
    modes: List<ViewMode>,
    current: ViewMode,
    onSelect: (ViewMode) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = scheme.surfaceVariant,
        border = BorderStroke(1.dp, scheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            modes.forEach { mode ->
                val active = mode == current
                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = if (active) scheme.primary else Color.Transparent,
                    shadowElevation = if (active) 2.dp else 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(mode) },
                ) {
                    Text(
                        text = "${mode.icon}${mode.label}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        color = if (active) scheme.onPrimary else scheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun Footer(brand: Brand) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp),
    ) {
        Text(
            text = brand.footer,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = brand.footerAuthor,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
        )
    }
}

/* ------------------------------------------------------------------------------------------- */
/* Episode list                                                                                  */
/* ------------------------------------------------------------------------------------------- */

/**
 * Episode list rows. Declared inside [MainScreen] on purpose: `item {}` blocks are not composable,
 * so the label resources have to be resolved in the enclosing composable scope first.
 */
private fun LazyListScope.episodeItems(
    state: MainUiState,
    context: EpisodeContext,
    viewModel: MainViewModel,
    onShare: (Int) -> Unit,
    onPick: (String, Int) -> Unit,
    labels: EpisodeItemLabels,
) {
    val brand = state.brand

    item(key = "picks") {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PickButton(
                text = labels.pickRandom,
                primary = true,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.pickRandom()?.let { onPick(labels.pickRandom, it.index) } },
            )
            PickButton(
                text = labels.pickToday,
                primary = false,
                modifier = Modifier.weight(1f),
                onClick = {
                    viewModel.pickToday()?.let {
                        onPick(labels.pickTodayTitle.format(todayLabel()), it.index)
                    }
                },
            )
        }
    }

    item(key = "search") {
        SearchField(
            value = state.filters.query,
            hint = brand.searchHint,
            helper = brand.searchHelper,
            onValueChange = viewModel::setQuery,
            onClear = viewModel::clearQuery,
        )
    }

    item(key = "sections") {
        SectionTabs(
            sections = state.catalog?.sections.orEmpty(),
            selected = state.filters.section,
            allLabel = brand.allSectionsLabel,
            onSelect = viewModel::setSection,
        )
    }

    item(key = "filters") { FilterChips(state = state, viewModel = viewModel, labels = labels) }

    item(key = "info") {
        ResultInfo(
            shown = state.loaded.size,
            total = state.matches.size,
            query = state.filters.query,
            sectionLabel = state.filters.section
                .takeIf { it > 0 }
                ?.let { state.catalog?.sections?.getOrNull(it - 1) },
        )
    }

    if (state.loaded.isEmpty() && !state.loading) {
        item(key = "empty") { EmptyState(title = labels.emptyTitle, body = labels.emptyBody) }
    } else {
        items(
            count = state.loaded.size,
            key = { index -> "${state.site.name}-${state.loaded[index].index}" },
        ) { index ->
            val episode = state.loaded[index]
            EpisodeCard(
                episode = episode,
                context = context,
                query = state.filters.query,
                onCharacterClick = {
                    viewModel.selectCharacter(it)
                    viewModel.setMode(ViewMode.Characters)
                },
                onTagClick = {
                    viewModel.setMode(ViewMode.Episodes)
                    viewModel.toggleTag(it)
                },
                onStorylineClick = {
                    viewModel.selectStoryline(it)
                    viewModel.setMode(ViewMode.MainLines)
                },
                onToggleSeen = { viewModel.toggleSeen(episode.progressKey) },
                onToggleWant = { viewModel.toggleWant(episode.progressKey) },
                onShare = { onShare(episode.index) },
            )
        }

        if (state.hasMore) {
            item(key = "more") {
                PickButton(
                    text = labels.showMore.format(state.matches.size - state.loaded.size),
                    primary = false,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = viewModel::showMore,
                )
            }
        }
    }
}

/** Resource strings that LazyListScope item bodies cannot resolve themselves. */
private data class EpisodeItemLabels(
    val pickRandom: String,
    val pickToday: String,
    val pickTodayTitle: String,
    val showMore: String,
    val filterMainOnly: String,
    val filterOnlyWant: String,
    val filterHideSeen: String,
    val emptyTitle: String,
    val emptyBody: String,
)

@Composable
private fun episodeItemLabels(): EpisodeItemLabels = EpisodeItemLabels(
    pickRandom = stringResource(R.string.pick_random),
    pickToday = stringResource(R.string.pick_today),
    pickTodayTitle = stringResource(R.string.pick_today_title),
    showMore = stringResource(R.string.show_more),
    filterMainOnly = stringResource(R.string.filter_main_only),
    filterOnlyWant = stringResource(R.string.filter_only_want),
    filterHideSeen = stringResource(R.string.filter_hide_seen),
    emptyTitle = stringResource(R.string.empty_title),
    emptyBody = stringResource(R.string.empty_body),
)

private fun todayLabel(): String {
    val calendar = java.util.Calendar.getInstance()
    return "${calendar.get(java.util.Calendar.MONTH) + 1}月${calendar.get(java.util.Calendar.DAY_OF_MONTH)}日"
}

@Composable
fun PickButton(
    text: String,
    primary: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (primary) scheme.primary else scheme.surface,
        border = BorderStroke(1.5.dp, if (primary) scheme.primary else scheme.outline),
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (primary) scheme.onPrimary else scheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun SearchField(
    value: String,
    hint: String,
    helper: String,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = scheme.surfaceVariant,
            border = BorderStroke(1.dp, scheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp),
            ) {
                Box(
                    Modifier
                        .size(16.dp)
                        .drawBehind {
                            val stroke = size.minDimension * 0.14f
                            val radius = size.minDimension * 0.34f
                            val center = Offset(size.width * 0.42f, size.height * 0.42f)
                            drawCircle(
                                color = scheme.onSurfaceVariant,
                                radius = radius,
                                center = center,
                                style = Stroke(width = stroke),
                            )
                            drawLine(
                                color = scheme.onSurfaceVariant,
                                start = Offset(center.x + radius * 0.72f, center.y + radius * 0.72f),
                                end = Offset(size.width * 0.94f, size.height * 0.94f),
                                strokeWidth = stroke,
                            )
                        },
                )
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = hint,
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant.copy(alpha = 0.75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(vertical = 13.dp),
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        singleLine = true,
                        textStyle = LocalTextStyle.current.merge(
                            MaterialTheme.typography.bodyMedium.copy(color = scheme.onSurface),
                        ),
                        cursorBrush = SolidColor(scheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 13.dp),
                    )
                }
                if (value.isNotEmpty()) {
                    Text(
                        text = "✕",
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.primary,
                        modifier = Modifier
                            .clickable(onClick = onClear)
                            .padding(start = 8.dp, end = 2.dp, top = 8.dp, bottom = 8.dp),
                    )
                }
            }
        }
        if (helper.isNotEmpty()) {
            Spacer(Modifier.height(7.dp))
            Text(
                text = helper,
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

@Composable
private fun SectionTabs(
    sections: List<String>,
    selected: Int,
    allLabel: String,
    onSelect: (Int) -> Unit,
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Pill(text = allLabel, active = selected == 0, onClick = { onSelect(0) })
        sections.forEachIndexed { index, label ->
            val number = index + 1
            Pill(
                text = shortSectionName(label),
                active = selected == number,
                onClick = { onSelect(number) },
            )
        }
    }
}

@Composable
private fun FilterChips(state: MainUiState, viewModel: MainViewModel, labels: EpisodeItemLabels) {
    val filters = state.filters
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Pill(
            text = labels.filterMainOnly,
            active = filters.mainOnly,
            onClick = viewModel::toggleMainOnly,
        )
        Pill(
            text = labels.filterOnlyWant,
            active = filters.onlyWant,
            onClick = viewModel::toggleOnlyWant,
        )
        Pill(
            text = labels.filterHideSeen,
            active = filters.hideSeen,
            onClick = viewModel::toggleHideSeen,
        )
        state.catalog?.tags?.forEachIndexed { index, tag ->
            Pill(
                text = tag,
                active = filters.tag == index,
                onClick = { viewModel.toggleTag(index) },
            )
        }
    }
}

@Composable
private fun ResultInfo(shown: Int, total: Int, query: String, sectionLabel: String?) {
    val brand = LocalBrandColors.current
    Text(
        text = buildAnnotatedString {
            append("共找到 ")
            withStyle(SpanStyle(color = brand.mark, fontWeight = FontWeight.Bold)) {
                append("$total")
            }
            append(" 个故事")
            if (sectionLabel != null) append("，${sectionHeading(sectionLabel)}")
            if (query.isNotEmpty()) append("，关键词：$query")
            if (shown < total) append("（已显示 $shown 个）")
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp),
    )
}

@Composable
private fun EmptyState(title: String, body: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 44.dp),
        ) {
            Text("📜", fontSize = 32.sp)
            Spacer(Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/* ------------------------------------------------------------------------------------------- */
/* Section label helpers                                                                        */
/* ------------------------------------------------------------------------------------------- */

/** "第十二部：第3559集—第4650集（…）" -> "第十二部" */
internal fun shortSectionName(label: String): String =
    label.substringBefore(':').substringBefore('：').trim()

/** "第十二部：第3559集—第4650集（…）" -> "第十二部：第3559集—第4650集" */
internal fun sectionHeading(label: String): String =
    label.substringBefore('（').trim()
