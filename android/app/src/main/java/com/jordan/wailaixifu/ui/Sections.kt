package com.jordan.wailaixifu.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jordan.wailaixifu.data.Catalog
import com.jordan.wailaixifu.ui.components.EpisodeCard
import com.jordan.wailaixifu.ui.components.EpisodeContext
import com.jordan.wailaixifu.ui.components.Pill
import com.jordan.wailaixifu.ui.theme.LocalBrandColors

/* ------------------------------------------------------------------------------------------- */
/* 主线剧情                                                                                       */
/* ------------------------------------------------------------------------------------------- */

@Composable
fun StorylineSection(
    state: MainUiState,
    context: EpisodeContext,
    viewModel: MainViewModel,
    onOverlay: (Int) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val brand = LocalBrandColors.current
    val current = state.storylines.getOrNull(state.selectedStoryline)
    val sections = remember(state.storylines) { state.storylines.map { it.section }.distinct() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "共 ${state.storylines.size} 条主线 · 点击选择：",
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
        )

        sections.forEach { section ->
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .width(8.dp)
                            .height(8.dp)
                            .background(brand.windowStrip[0]),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = section,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = brand.frame,
                    )
                }
                Spacer(Modifier.height(7.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    state.storylines.forEachIndexed { index, line ->
                        if (line.section == section) {
                            Pill(
                                text = line.name,
                                active = index == state.selectedStoryline,
                                onClick = { viewModel.selectStoryline(index) },
                            )
                        }
                    }
                }
            }
        }

        if (current != null) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = scheme.surface,
                border = BorderStroke(1.dp, scheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = current.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = scheme.onSurface,
                    )
                    if (current.summary.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = current.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
            }

            current.nodes.forEachIndexed { nodeIndex, node ->
                StoryNodeBlock(
                    node = node,
                    context = context,
                    state = state,
                    viewModel = viewModel,
                    onOverlay = onOverlay,
                )
            }
        }
    }
}

@Composable
private fun StoryNodeBlock(
    node: com.jordan.wailaixifu.data.StoryNode,
    context: EpisodeContext,
    state: MainUiState,
    viewModel: MainViewModel,
    onOverlay: (Int) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val brand = LocalBrandColors.current
    var expanded by remember(node.text) { mutableStateOf(false) }

    val ranges = remember(node) {
        node.episodeIndices.mapNotNull { state.catalog?.episodes?.getOrNull(it) }
        // The range label mirrors the source page: deduplicated "第X季 第a-b集" strings.
        val episode = state.catalog?.episodes
        node.episodeIndices
            .mapNotNull { episode?.getOrNull(it) }
            .map { it.section to it }
            .distinctBy { (section, ep) -> "$section-${ep.start}-${ep.end}" }
            .joinToString(" / ") { (section, ep) ->
                if (ep.isMerged) "第${section}季 第${ep.start}-${ep.end}集" else "第${section}季 第${ep.start}集"
            }
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        // The vertical rail with a rotated ruby marker, echoing `.node` on the source page.
        Box(
            Modifier
                .width(3.dp)
                .height(intrinsicNodeHeight())
                .background(brand.frame.copy(alpha = 0.8f)),
        )
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            val head = listOfNotNull(node.group.takeIf { it.isNotBlank() }, ranges.takeIf { it.isNotBlank() })
                .joinToString(" · ")
            if (head.isNotEmpty()) {
                Text(
                    text = head,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = brand.mark,
                )
                Spacer(Modifier.height(3.dp))
            }
            Text(
                text = node.text,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
            if (node.episodeIndices.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Pill(
                    text = if (expanded) {
                        "收起相关剧集（${node.episodeIndices.size}）"
                    } else {
                        "查看相关剧集（${node.episodeIndices.size}）"
                    },
                    active = false,
                    onClick = { expanded = !expanded },
                )
                if (expanded) {
                    Spacer(Modifier.height(8.dp))
                    node.episodeIndices.forEach { index ->
                        state.catalog?.episodes?.getOrNull(index)?.let { episode ->
                            EpisodeCard(
                                episode = episode,
                                context = context,
                                onToggleSeen = { viewModel.toggleSeen(episode.progressKey) },
                                onToggleWant = { viewModel.toggleWant(episode.progressKey) },
                                onShare = { onOverlay(episode.index) },
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** A tall enough rail so the marker reads as a continuous spine next to the node body. */
private fun intrinsicNodeHeight() = 96.dp

/* ------------------------------------------------------------------------------------------- */
/* 角色查询                                                                                       */
/* ------------------------------------------------------------------------------------------- */

/** Category labels of the source page's 角色查询 view. */
private val CHARACTER_CATEGORIES = listOf(
    "🏛️ 房东与当铺",
    "🏠 大院房客及家属",
    "🕵️ 警界与特务",
    "⚔️ 军方与江湖势力",
    "👤 其他",
)

@Composable
fun CharacterSection(
    state: MainUiState,
    context: EpisodeContext,
    viewModel: MainViewModel,
    onOverlay: (Int) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val catalog = state.catalog
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CHARACTER_CATEGORIES.forEachIndexed { categoryIndex, label ->
            val members = state.characters.filter { it.category == categoryIndex }
            if (members.isEmpty()) return@forEachIndexed
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = LocalBrandColors.current.frame,
                )
                Spacer(Modifier.height(7.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    members.forEach { character ->
                        Pill(
                            text = character.name,
                            active = character.id == state.selectedCharacter,
                            trailing = catalog?.characterStoryCounts?.getOrNull(character.id)?.toString(),
                            onClick = { viewModel.selectCharacter(character.id) },
                        )
                    }
                }
            }
        }

        val selectedId = state.selectedCharacter
        if (selectedId != null) {
            val character = state.characters.getOrNull(selectedId)
            if (character != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = scheme.surface,
                    border = BorderStroke(1.dp, scheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = character.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = scheme.onSurface,
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = CHARACTER_CATEGORIES.getOrElse(character.category) { "👤 其他" },
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "别称：${character.aliasText}",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurface,
                        )
                        if (character.description.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = character.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "共出场 ${catalog?.characterStoryCounts?.getOrNull(selectedId) ?: 0} 个故事",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = scheme.primary,
                        )
                    }
                }

                val seasonCounts = viewModel.characterSeasonCounts(selectedId)
                if (seasonCounts.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Pill(
                            text = "全部季",
                            active = state.selectedCharacterSection == 0,
                            onClick = { viewModel.setCharacterSection(0) },
                        )
                        seasonCounts.forEach { (section, count) ->
                            Pill(
                                text = "第${section}季",
                                active = state.selectedCharacterSection == section,
                                trailing = count.toString(),
                                onClick = { viewModel.setCharacterSection(section) },
                            )
                        }
                    }
                }

                val episodes = viewModel.characterEpisodes(selectedId, state.selectedCharacterSection)
                Text(
                    text = "${episodes.size} 个故事" + if (episodes.size > 150) "（仅显示前 150 个）" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
                episodes.take(150).forEach { episode ->
                    EpisodeCard(
                        episode = episode,
                        context = context,
                        onCharacterClick = { viewModel.selectCharacter(it) },
                        onToggleSeen = { viewModel.toggleSeen(episode.progressKey) },
                        onToggleWant = { viewModel.toggleWant(episode.progressKey) },
                        onShare = { onOverlay(episode.index) },
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
            }
        }
    }
}

/* ------------------------------------------------------------------------------------------- */
/* 相似抽取                                                                                       */
/* ------------------------------------------------------------------------------------------- */

@Composable
fun PickerSection(
    state: MainUiState,
    context: EpisodeContext,
    viewModel: MainViewModel,
    onOverlay: (Int) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    // Named brandColors on purpose: `brand` here is the palette, while the copy lives in state.brand.
    val brandColors = LocalBrandColors.current
    val picker = state.picker
    val catalog = state.catalog

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = scheme.surface,
        border = BorderStroke(1.dp, scheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "🎯 相似剧情随机抽取器",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = brandColors.frame,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = "挑角色、选主题、填关键词，随机抽几集同类型剧情慢慢看。" +
                    "例如：选「蒋宏发（发仔）」+ 关键词「打」，就能抽到发仔跟别人拼命的集数。",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(14.dp))
            Text(
                text = "👤 出场角色（可多选，须全部出场）",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = scheme.onSurface,
            )
            Spacer(Modifier.height(7.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                state.characters.forEach { character ->
                    Pill(
                        text = character.name,
                        active = character.id in picker.characterIds,
                        trailing = catalog?.characterStoryCounts?.getOrNull(character.id)?.toString(),
                        onClick = { viewModel.togglePickerCharacter(character.id) },
                    )
                }
            }

            if (catalog != null && catalog.tags.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "🏷 主题类型（可多选，须全部命中）",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = scheme.onSurface,
                )
                Spacer(Modifier.height(7.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    catalog.tags.forEachIndexed { index, tag ->
                        Pill(
                            text = tag,
                            active = index in picker.tagIds,
                            onClick = { viewModel.togglePickerTag(index) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                text = "🔍 关键词（可填多个，用空格分隔，命中其一即可，如：发仔 打）",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = scheme.onSurface,
            )
            Spacer(Modifier.height(7.dp))
            PickerKeywordField(
                value = picker.keyword,
                hint = state.brand.pickerKeywordHint,
                onValueChange = viewModel::setPickerKeyword,
                onSubmit = viewModel::drawPicker,
            )

            Spacer(Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "抽取",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
                listOf(1, 3, 5, 10).forEach { count ->
                    Pill(
                        text = count.toString(),
                        active = picker.count == count,
                        onClick = { viewModel.setPickerCount(count) },
                    )
                }
                Text(
                    text = "集",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Pill(
                    text = "只抽没看过的",
                    active = picker.unseenOnly,
                    onClick = viewModel::togglePickerUnseen,
                )
                Spacer(Modifier.weight(1f))
                PickButton(
                    text = "抽！",
                    primary = true,
                    onClick = viewModel::drawPicker,
                )
            }

            val drawn = picker.drawn.mapNotNull { index -> state.catalog?.episodes?.getOrNull(index) }
            if (picker.hasDrawn) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = if (picker.poolSize == 0) {
                        "没有符合条件的剧集，放宽点条件再试试"
                    } else {
                        "符合条件的共 ${picker.poolSize} 个故事，本次抽取 ${drawn.size} 个"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (picker.poolSize == 0) scheme.error else scheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                drawn.forEach { episode ->
                    EpisodeCard(
                        episode = episode,
                        context = context,
                        onToggleSeen = { viewModel.toggleSeen(episode.progressKey) },
                        onToggleWant = { viewModel.toggleWant(episode.progressKey) },
                        onShare = { onOverlay(episode.index) },
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                if (picker.poolSize > 0) {
                    Spacer(Modifier.height(4.dp))
                    PickButton(
                        text = "🔄 换一批",
                        primary = false,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = viewModel::drawPicker,
                    )
                }
            }
        }
    }
}

@Composable
private fun PickerKeywordField(
    value: String,
    hint: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = scheme.surfaceVariant,
        border = BorderStroke(1.dp, scheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(modifier = Modifier.padding(horizontal = 14.dp)) {
            if (value.isEmpty()) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant.copy(alpha = 0.75f),
                    maxLines = 1,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
            androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = androidx.compose.material3.LocalTextStyle.current.merge(
                    MaterialTheme.typography.bodySmall.copy(color = scheme.onSurface),
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(scheme.primary),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Search,
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSearch = { onSubmit() },
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
            )
        }
    }
}

/* ------------------------------------------------------------------------------------------- */
/* Modal shell                                                                                   */
/* ------------------------------------------------------------------------------------------- */

/** Full-screen backdrop matching the `.modal` / `.modal-box` treatment of the source pages. */
@Composable
fun ModalShell(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(modifier = Modifier.fillMaxSize()) {
        // Tapping the backdrop dismisses; the card itself sits above it and swallows clicks.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(onClick = onDismiss),
        )
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = scheme.surface,
            border = BorderStroke(1.dp, scheme.outlineVariant),
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .align(Alignment.Center),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                content()
            }
        }
    }
}

/** 🎲 随便看一集 / 📅 今日推荐 result. */
@Composable
fun PickModal(
    title: String,
    episode: com.jordan.wailaixifu.data.Episode?,
    context: EpisodeContext,
    onReroll: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalShell(onDismiss = onDismiss) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (episode != null) {
            EpisodeCard(episode = episode, context = context)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PickButton(
                    text = "🎲 再抽一集",
                    primary = false,
                    modifier = Modifier.weight(1f),
                    onClick = onReroll,
                )
                PickButton(
                    text = "🖼 生成分享卡",
                    primary = true,
                    modifier = Modifier.weight(1f),
                    onClick = onShare,
                )
            }
        }
        PickButton(
            text = "关闭",
            primary = false,
            modifier = Modifier.fillMaxWidth(),
            onClick = onDismiss,
        )
    }
}

/** Small helper so the modals can show a catalogue-independent unit label. */
@Composable
internal fun UnitHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}
