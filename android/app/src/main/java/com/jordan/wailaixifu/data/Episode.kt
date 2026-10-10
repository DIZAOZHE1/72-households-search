package com.jordan.wailaixifu.data

/**
 * One "story card" of a drama. A card can cover a single episode or a merged multi-episode story
 * arc (which the source pages label as 合集).
 *
 * [start] / [end] are the inclusive numeric episode range used by the "jump to episode N" search.
 * [episodeLabel] is what the badge shows, e.g. "13-15".
 */
data class Episode(
    /** Index into the catalogue's episode list. Stable identity for seen/want tracking. */
    val index: Int,
    /** 1-based season for 《七十二家房客》, or the 部 number for 《外来媳妇本地郎》. */
    val section: Int,
    val start: Int,
    val end: Int,
    val episodeLabel: String,
    val title: String,
    val raw: String,
    /** Story synopsis; empty for datasets that do not carry one. */
    val synopsis: String,
    /** Character ids appearing in this story. */
    val characterIds: List<Int>,
    /** Tag ids attached to this story. */
    val tagIds: List<Int>,
    /** Storylines this story belongs to, as (lineIndex, nodeIndex). */
    val storyRefs: List<StoryRef>,
) {
    /** True when this card merges several episodes into one story. */
    val isMerged: Boolean get() = start != end

    /** The key used to persist 看过 / 想看 state, mirroring the web page's `se-s` key. */
    val progressKey: String get() = "$section-$start"

    /** Number of episodes this card covers. */
    val episodeCount: Int get() = end - start + 1

    /** Human readable episode range, e.g. "第13–15集". */
    fun rangeText(brand: Brand): String =
        if (isMerged) {
            "第$start–$end${brand.episodeUnit}"
        } else {
            "第$start${brand.episodeUnit}"
        }

    /** Badge label for the section, e.g. "第3部" / "第3季". */
    fun sectionText(brand: Brand): String =
        if (section > 0) {
            "${brand.sectionPrefix}$section${brand.sectionSuffix}"
        } else {
            brand.sectionFallback
        }

    /** Badge label describing whether this card merges episodes. */
    fun kindText(brand: Brand): String = if (isMerged) brand.mergedLabel else brand.singleLabel

    /** Case-insensitive match against the title and the original raw text. */
    fun matches(text: String): Boolean =
        title.contains(text, ignoreCase = true) || raw.contains(text, ignoreCase = true)
}

/** A reference from a story card to one node of a storyline. */
data class StoryRef(val lineIndex: Int, val nodeIndex: Int)

/**
 * Search ranking of a card for a keyword, mirroring `searchRank()` on the source pages:
 * 0 = title hit, 1 = synopsis hit, 2 = exact character name/alias hit, 3 = no match.
 */
fun Episode.searchRank(query: String, characterKeys: List<List<String>>): Int {
    if (query.isEmpty()) return NO_MATCH
    if (title.contains(query, ignoreCase = true)) return 0
    if (synopsis.contains(query, ignoreCase = true)) return 1
    // Only complete names or aliases count, so an alias like 鸡仔 never matches 鸡仔珠.
    if (characterIds.any { id -> characterKeys.getOrNull(id)?.any { it == query } == true }) return 2
    return NO_MATCH
}

const val NO_MATCH = 3

/** One character of 《七十二家房客》. */
data class Character(
    val id: Int,
    val name: String,
    val category: Int,
    val aliases: List<String>,
    val description: String,
) {
    val aliasText: String get() = if (aliases.isEmpty()) "无" else aliases.joinToString("、")
}

/** One node (story beat) inside a storyline. */
data class StoryNode(
    val group: String,
    val text: String,
    /** Indices into the catalogue's episode list. */
    val episodeIndices: List<Int>,
)

/** One storyline of 《七十二家房客》. */
data class Storyline(
    val index: Int,
    val section: String,
    val name: String,
    val summary: String,
    val nodes: List<StoryNode>,
)

/** The whole offline dataset of one site. */
data class Catalog(
    val dataset: String,
    val episodes: List<Episode>,
    /** Section headings: 部 for 《外来媳妇本地郎》, 季 for 《七十二家房客》. */
    val sections: List<String>,
    val tags: List<String> = emptyList(),
    val characters: List<Character> = emptyList(),
    val storylines: List<Storyline> = emptyList(),
) {
    /** Lower-cased name + alias keys per character, precomputed for search ranking. */
    val characterSearchKeys: List<List<String>> = characters.map { character ->
        buildList {
            add(character.name.lowercase())
            character.aliases.forEach { add(it.lowercase()) }
        }
    }

    /** How many stories each character appears in. */
    val characterStoryCounts: List<Int> = characters.map { character ->
        episodes.count { character.id in it.characterIds }
    }

    /** Total episodes covered, used by the progress card. */
    val totalEpisodes: Int = episodes.sumOf { it.episodeCount }

    /** Episodes covered per section, used by the progress card. */
    val episodesPerSection: Map<Int, Int> = buildMap {
        episodes.forEach { put(it.section, (get(it.section) ?: 0) + it.episodeCount) }
    }

    val hasCharacters: Boolean get() = characters.isNotEmpty()
    val hasStorylines: Boolean get() = storylines.isNotEmpty()
    val hasTags: Boolean get() = tags.isNotEmpty()
    val hasSynopses: Boolean get() = episodes.any { it.synopsis.isNotEmpty() }
}
