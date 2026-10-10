package com.jordan.wailaixifu.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jordan.wailaixifu.data.Character
import com.jordan.wailaixifu.data.Episode
import com.jordan.wailaixifu.data.SiteTheme
import com.jordan.wailaixifu.data.Storyline
import com.jordan.wailaixifu.ui.theme.LocalBrandColors

/**
 * Everything an episode card needs to resolve its tags, characters and 主线 links, plus the
 * reader's 看过 / 想看 state. Passed down instead of reaching into the ViewModel so the cards can
 * also be rendered inside the modal and the share preview.
 */
data class EpisodeContext(
    val theme: SiteTheme,
    val brand: com.jordan.wailaixifu.data.Brand,
    val characters: List<Character>,
    val tags: List<String>,
    val storylines: List<Storyline>,
    val seenKeys: Set<String>,
    val wantKeys: Set<String>,
) {
    fun character(id: Int): Character? = characters.getOrNull(id)
    fun tag(id: Int): String? = tags.getOrNull(id)
    fun storyline(index: Int): Storyline? = storylines.getOrNull(index)
    fun isSeen(episode: Episode) = episode.progressKey in seenKeys
    fun isWant(episode: Episode) = episode.progressKey in wantKeys

    /** Badge label for the section, e.g. "第12季" or "第3部". */
    fun sectionLabel(episode: Episode): String =
        if (episode.section > 0) {
            "${brand.sectionPrefix}${episode.section}${brand.sectionSuffix}"
        } else {
            brand.sectionFallback
        }
}

/** Visual treatment of a card, which is what actually distinguishes the three sites. */
private data class CardStyle(
    val shape: RoundedCornerShape,
    val border: BorderStroke?,
    val elevation: androidx.compose.ui.unit.Dp,
    val stripe: Color?,
    val stripeWidth: androidx.compose.ui.unit.Dp,
    val cornerWindow: Boolean,
    val badgeFilled: Boolean,
)

@Composable
private fun cardStyle(theme: SiteTheme): CardStyle {
    val brand = LocalBrandColors.current
    val scheme = MaterialTheme.colorScheme
    return when (theme) {
        // 岭南: 宣纸卡片 + 描金细边 + 左侧朱红竖条.
        SiteTheme.Lingnan -> CardStyle(
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(1.dp, scheme.outline),
            elevation = 2.dp,
            stripe = scheme.primary,
            stripeWidth = 4.dp,
            cornerWindow = false,
            badgeFilled = false,
        )

        // 传统: 旧木粗框 + 内衬描边 + 右上角满洲窗小格 + 木牌集数章.
        SiteTheme.Traditional -> CardStyle(
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(3.dp, brand.frame),
            elevation = 0.dp,
            stripe = null,
            stripeWidth = 0.dp,
            cornerWindow = true,
            badgeFilled = true,
        )

        // 现代: 白底圆角、无边框、极轻投影.
        SiteTheme.Modern -> CardStyle(
            shape = RoundedCornerShape(14.dp),
            border = null,
            elevation = 1.dp,
            stripe = null,
            stripeWidth = 0.dp,
            cornerWindow = false,
            badgeFilled = false,
        )
    }
}

@Composable
fun EpisodeCard(
    episode: Episode,
    context: EpisodeContext,
    query: String = "",
    onCharacterClick: (Int) -> Unit = {},
    onTagClick: (Int) -> Unit = {},
    onStorylineClick: (Int) -> Unit = {},
    onToggleSeen: () -> Unit = {},
    onToggleWant: () -> Unit = {},
    onShare: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val style = cardStyle(context.theme)
    val brand = LocalBrandColors.current
    val scheme = MaterialTheme.colorScheme
    val seen = context.isSeen(episode)
    val want = context.isWant(episode)

    // A pure episode number is a range lookup, not a text match, so it is never highlighted.
    val highlight = if (query.toIntOrNull() != null) "" else query

    val windowStrip = remember(brand.windowStrip) { brand.windowStrip }

    Surface(
        shape = style.shape,
        color = if (seen) scheme.surface.copy(alpha = 0.72f) else scheme.surface,
        border = style.border,
        shadowElevation = style.elevation,
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    if (style.cornerWindow) {
                        // 右上角满洲窗小格: 2×2 four-colour pane inside a frame.
                        val pane = 8.dp.toPx()
                        val pad = 8.dp.toPx()
                        val origin = Offset(size.width - pad - pane * 2, pad)
                        drawRect(
                            color = brand.frame,
                            topLeft = origin - Offset(2.dp.toPx(), 2.dp.toPx()),
                            size = Size(pane * 2 + 4.dp.toPx(), pane * 2 + 4.dp.toPx()),
                        )
                        drawRect(windowStrip[0], origin, Size(pane, pane))
                        drawRect(windowStrip[1], origin + Offset(pane, 0f), Size(pane, pane))
                        drawRect(windowStrip[2], origin + Offset(0f, pane), Size(pane, pane))
                        drawRect(windowStrip[3], origin + Offset(pane, pane), Size(pane, pane))
                    }
                    // 宣纸卡片左侧的朱红竖条 for the 外来 site. Drawn here rather than as a sibling
                    // Row child: that needed IntrinsicSize.Min, which recursed into the chips and
                    // blew up with "Size(2147483647 x 47) is out of range".
                    style.stripe?.let { stripe ->
                        drawRect(
                            color = stripe,
                            topLeft = Offset.Zero,
                            size = Size(style.stripeWidth.toPx(), size.height),
                        )
                    }
                },
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.padding(
                    start = 12.dp + style.stripeWidth,
                    end = 14.dp,
                    top = 12.dp,
                    bottom = 12.dp,
                ),
            ) {
                Badge(episode, context, style)

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = highlightText(episode.title, highlight, brand.highlight),
                                style = MaterialTheme.typography.titleMedium,
                                color = if (seen) {
                                    scheme.onSurfaceVariant
                                } else {
                                    scheme.onSurface
                                },
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            if (episode.storyRefs.isNotEmpty()) {
                                Spacer(Modifier.width(6.dp))
                                MainFlag()
                            }
                        }

                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = episode.rangeText(context.brand),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )

                        if (episode.tagIds.isNotEmpty() && context.tags.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                episode.tagIds.forEach { tagId ->
                                    val label = context.tag(tagId) ?: return@forEach
                                    TagDot(
                                        text = label,
                                        index = tagId,
                                        onClick = { onTagClick(tagId) },
                                    )
                                }
                            }
                        }

                        if (episode.synopsis.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = highlightText(episode.synopsis, highlight, brand.highlight),
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        if (episode.storyRefs.isNotEmpty() && context.storylines.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = buildAnnotatedString {
                                    append("🧵 ")
                                    episode.storyRefs.forEachIndexed { i, ref ->
                                        val line = context.storyline(ref.lineIndex) ?: return@forEachIndexed
                                        if (i > 0) append(" · ")
                                        withStyle(
                                            SpanStyle(
                                                color = brand.mark,
                                                fontWeight = FontWeight.Medium,
                                            ),
                                        ) {
                                            append(line.name)
                                        }
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                                modifier = Modifier.clickable {
                                    episode.storyRefs.firstOrNull()?.let { onStorylineClick(it.lineIndex) }
                                },
                            )
                        }

                        if (episode.characterIds.isNotEmpty() && context.characters.isNotEmpty()) {
                            Spacer(Modifier.height(7.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                episode.characterIds.forEach { id ->
                                    val character = context.character(id) ?: return@forEach
                                    Pill(
                                        text = character.name,
                                        active = false,
                                        onClick = { onCharacterClick(id) },
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ActionButton(text = "🖼 分享", active = false, onClick = onShare)
                            ActionButton(
                                text = if (want) "★" else "☆",
                                active = want,
                                activeColor = brand.want,
                                activeTextColor = brand.wantText,
                                onClick = onToggleWant,
                            )
                            ActionButton(
                                text = "✓",
                                active = seen,
                                activeColor = brand.seen,
                                activeTextColor = brand.seenText,
                                onClick = onToggleSeen,
                            )
                        }
                    }
                }
            }
        }
    }

/** The episode badge: season label, episode range and single/merged marker. */
@Composable
private fun Badge(episode: Episode, context: EpisodeContext, style: CardStyle) {
    val brand = LocalBrandColors.current
    val scheme = MaterialTheme.colorScheme

    Surface(
        shape = RoundedCornerShape(if (style.badgeFilled) 12.dp else 4.dp),
        color = if (style.badgeFilled) brand.frame else scheme.surfaceVariant,
        border = if (style.badgeFilled) {
            BorderStroke(2.dp, brand.frame)
        } else {
            BorderStroke(1.dp, scheme.outline)
        },
        modifier = Modifier.width(72.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
        ) {
            Text(
                text = context.sectionLabel(episode),
                style = MaterialTheme.typography.labelSmall,
                color = if (style.badgeFilled) brand.bannerSub.copy(alpha = 0.95f) else scheme.onSurfaceVariant,
                maxLines = 1,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = episode.episodeLabel,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                ),
                color = if (style.badgeFilled) Color(0xFFF5EBD3) else scheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (episode.isMerged) "合集" else "单集",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = if (style.badgeFilled) brand.bannerSub.copy(alpha = 0.8f) else scheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

/** "第12季" for 《七十二家房客》, "第3部" for 《外来媳妇本地郎》 — see [EpisodeContext.sectionLabel]. */

@Composable
private fun MainFlag() {
    val brand = LocalBrandColors.current
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = brand.mark.copy(alpha = 0.14f),
    ) {
        Text(
            text = "★ 主线",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = brand.mark,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
        )
    }
}

/** A tag with the 满洲窗 four-colour dot, matching `.tag::before` on the source page. */
@Composable
fun TagDot(text: String, index: Int, onClick: (() -> Unit)? = null) {
    val scheme = MaterialTheme.colorScheme
    val strip = LocalBrandColors.current.windowStrip
    val dotColor = strip[index % strip.size]
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 2.dp),
    ) {
        Box(
            Modifier
                .size(7.dp)
                .background(dotColor),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
        )
    }
}

/** A selectable pill, used for characters, tags and seasons. */
@Composable
fun Pill(
    text: String,
    active: Boolean,
    onClick: () -> Unit,
    trailing: String? = null,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (active) scheme.primary else scheme.surfaceVariant,
        border = BorderStroke(
            1.dp,
            if (active) scheme.primary else scheme.outlineVariant,
        ),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = if (active) scheme.onPrimary else scheme.onSurfaceVariant,
            )
            if (trailing != null) {
                Spacer(Modifier.width(4.dp))
                Text(
                    text = trailing,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = if (active) {
                        scheme.onPrimary.copy(alpha = 0.7f)
                    } else {
                        scheme.onSurfaceVariant.copy(alpha = 0.6f)
                    },
                )
            }
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    active: Boolean,
    onClick: () -> Unit,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    activeTextColor: Color = MaterialTheme.colorScheme.onPrimary,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (active) activeColor else scheme.surfaceVariant,
        border = BorderStroke(1.dp, if (active) activeColor else scheme.outlineVariant),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = if (active) activeTextColor else scheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
        )
    }
}

/** Wraps every case-insensitive occurrence of [query] in the site's highlight tint. */
fun highlightText(text: String, query: String, highlight: Color): AnnotatedString {
    if (query.isEmpty()) return AnnotatedString(text)
    return buildAnnotatedString {
        var cursor = 0
        while (cursor < text.length) {
            val hit = text.indexOf(query, cursor, ignoreCase = true)
            if (hit < 0) {
                append(text.substring(cursor))
                break
            }
            append(text.substring(cursor, hit))
            withStyle(SpanStyle(background = highlight)) {
                append(text.substring(hit, hit + query.length))
            }
            cursor = hit + query.length
        }
    }
}

/** The 满洲窗 four-colour divider used between major blocks of the 传统 skin. */
@Composable
fun WindowStrip(modifier: Modifier = Modifier, height: androidx.compose.ui.unit.Dp = 5.dp) {
    val strip = LocalBrandColors.current.windowStrip
    Row(modifier = modifier.fillMaxWidth().height(height)) {
        strip.forEach { color ->
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(color),
            )
        }
    }
}

/** Full-width 主线/detail divider rule. */
@Composable
fun RuleLine(color: Color = LocalBrandColors.current.frame) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.5.dp)
            .background(color.copy(alpha = 0.5f)),
    )
}
