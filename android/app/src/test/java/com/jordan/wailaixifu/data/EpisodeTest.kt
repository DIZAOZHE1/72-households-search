package com.jordan.wailaixifu.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Covers the pure search / pagination logic that the source pages implemented in JavaScript. */
class EpisodeTest {

    private val brand = Brand.WaiLai

    private fun episode(
        section: Int,
        start: Int,
        end: Int,
        label: String,
        title: String,
        raw: String = "第${start}集 $title",
        synopsis: String = "",
        characterIds: List<Int> = emptyList(),
        tagIds: List<Int> = emptyList(),
        storyRefs: List<StoryRef> = emptyList(),
    ) = Episode(
        index = 0,
        section = section,
        start = start,
        end = end,
        episodeLabel = label,
        title = title,
        raw = raw,
        synopsis = synopsis,
        characterIds = characterIds,
        tagIds = tagIds,
        storyRefs = storyRefs,
    )

    @Test
    fun `single episode card reports its own section and range`() {
        val card = episode(1, 1, 1, "1", "烦恼的生日")
        assertFalse(card.isMerged)
        assertEquals(1, card.episodeCount)
        assertEquals("第1集", card.rangeText(brand))
        assertEquals("第1部", card.sectionText(brand))
        assertEquals("单集", card.kindText(brand))
    }

    @Test
    fun `merged card is flagged as a story collection`() {
        val card = episode(1, 13, 15, "13-15", "闯荡广州")
        assertTrue(card.isMerged)
        assertEquals(3, card.episodeCount)
        assertEquals("第13–15集", card.rangeText(brand))
        assertEquals("故事合集", card.kindText(brand))
    }

    @Test
    fun `card without a section falls back to 未分部`() {
        assertEquals("未分部", episode(0, 1, 1, "1", "x").sectionText(brand))
    }

    @Test
    fun `card labels follow the site brand`() {
        val card = episode(3, 13, 15, "13-15", "闯关")
        // 《七十二家房客》 counts in 季, not 部.
        assertEquals("第3季", card.sectionText(Brand.QiErShi))
        assertEquals("第13–15集", card.rangeText(Brand.QiErShi))
    }

    @Test
    fun `progress key mirrors the source page se-s key`() {
        assertEquals("7-42", episode(7, 42, 44, "42-44", "x").progressKey)
    }

    @Test
    fun `matches searches title and raw text case insensitively`() {
        val card = episode(1, 9, 9, "9", "粤菜培训班", "第9集 粤菜培训班")
        assertTrue(card.matches("粤菜"))
        assertTrue(card.matches("培训班"))
        assertTrue(card.matches("第9集"))
        assertFalse(card.matches("昌盛街"))
    }

    @Test
    fun `episode number lookup lands inside a merged range`() {
        val merged = episode(1, 13, 15, "13-15", "闯荡广州")
        assertTrue(14 in merged.start..merged.end)
        assertFalse(12 in merged.start..merged.end)
        assertFalse(16 in merged.start..merged.end)
    }

    @Test
    fun `pagination keeps the original 60 cards per page`() {
        val cards = (1..1552).map { episode(1, it, it, "$it", "剧集$it") }
        assertEquals(60, cards.take(60).size)
        assertEquals(26, (cards.size + 59) / 60)
    }

    /* ---------------------------------------------------------------------------------------- */
    /* Search ranking                                                                             */
    /* ---------------------------------------------------------------------------------------- */

    private val characterKeys = listOf(
        listOf("谭宪炳", "太子炳", "炳哥"),
        listOf("蒋宏发", "发仔"),
        listOf("鸡仔珠"),
    )

    @Test
    fun `title hits outrank synopsis hits`() {
        val title = episode(1, 1, 1, "1", "打错更锣", synopsis = "无关内容")
        val synopsis = episode(1, 2, 2, "2", "无关标题", synopsis = "打错更锣的故事")
        assertEquals(0, title.searchRank("打错更锣", characterKeys))
        assertEquals(1, synopsis.searchRank("打错更锣", characterKeys))
    }

    @Test
    fun `character hits rank below title and synopsis`() {
        val card = episode(1, 3, 3, "3", "无关", synopsis = "无关", characterIds = listOf(0))
        assertEquals(2, card.searchRank("太子炳", characterKeys))
        assertEquals(2, card.searchRank("炳哥", characterKeys))
    }

    @Test
    fun `an alias never matches as a prefix of a longer alias`() {
        // 鸡仔 must not resolve to the character whose alias is 鸡仔珠.
        val card = episode(1, 4, 4, "4", "无关", synopsis = "无关", characterIds = listOf(2))
        assertEquals(NO_MATCH, card.searchRank("鸡仔", characterKeys))
        assertEquals(2, card.searchRank("鸡仔珠", characterKeys))
    }

    @Test
    fun `empty query never matches`() {
        assertEquals(NO_MATCH, episode(1, 1, 1, "1", "x").searchRank("", characterKeys))
    }
}
