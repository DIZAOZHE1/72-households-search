package com.jordan.wailaixifu.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Covers the three-site wiring and the aggregate numbers the progress card and badges rely on. */
class SiteTest {

    @Test
    fun `traditional and modern share one dataset`() {
        assertEquals(Site.Traditional.dataset, Site.Modern.dataset)
        assertEquals("qiershi", Site.Modern.dataset)
    }

    @Test
    fun `wailai uses its own dataset and theme`() {
        assertEquals("wailai", Site.WaiLai.dataset)
        assertEquals(SiteTheme.Lingnan, Site.WaiLai.theme)
    }

    @Test
    fun `the two qiershi sites differ only by theme`() {
        assertEquals(SiteTheme.Traditional, Site.Traditional.theme)
        assertEquals(SiteTheme.Modern, Site.Modern.theme)
    }

    @Test
    fun `three navigation destinations exist`() {
        assertEquals(3, Site.entries.size)
    }

    @Test
    fun `brands describe their own drama`() {
        assertEquals("第", Brand.QiErShi.sectionPrefix)
        assertEquals("季", Brand.QiErShi.sectionSuffix)
        assertEquals("部", Brand.WaiLai.sectionSuffix)
        assertEquals("全部季", Brand.QiErShi.allSectionsLabel)
        assertFalse(Brand.QiErShi.noticeBody.contains("大杂院"))
        assertFalse(Brand.QiErShi.footer.contains("大杂院"))
        assertFalse(Brand.QiErShi.deco.contains("大杂院"))
    }

    /* ---------------------------------------------------------------------------------------- */
    /* Catalogue aggregates                                                                       */
    /* ---------------------------------------------------------------------------------------- */

    private fun episode(index: Int, section: Int, start: Int, end: Int, characters: List<Int>) =
        Episode(
            index = index,
            section = section,
            start = start,
            end = end,
            episodeLabel = if (end > start) "$start-$end" else "$start",
            title = "标题$index",
            raw = "",
            synopsis = "",
            characterIds = characters,
            tagIds = emptyList(),
            storyRefs = emptyList(),
        )

    private fun catalog(): Catalog {
        val episodes = listOf(
            episode(0, 1, 1, 2, listOf(0)),
            episode(1, 1, 3, 3, listOf(0, 1)),
            episode(2, 2, 4, 6, listOf(1)),
        )
        return Catalog(
            dataset = "test",
            episodes = episodes,
            sections = listOf("第1季：a", "第2季：b"),
            tags = listOf("大院日常"),
            characters = listOf(
                Character(0, "谭宪炳", 0, listOf("太子炳"), "包租公"),
                Character(1, "刘丽琼", 0, listOf("八姑"), "包租婆"),
            ),
            storylines = emptyList(),
        )
    }

    @Test
    fun `total episodes counts the covered span, not the card count`() {
        assertEquals(6, catalog().totalEpisodes)
    }

    @Test
    fun `episodes per section feeds the progress frontier`() {
        val perSection = catalog().episodesPerSection
        assertEquals(3, perSection[1])
        assertEquals(3, perSection[2])
    }

    @Test
    fun `character story counts are per character`() {
        val counts = catalog().characterStoryCounts
        assertEquals(2, counts[0])
        assertEquals(2, counts[1])
    }

    @Test
    fun `character search keys include names and aliases, lowercased`() {
        val keys = catalog().characterSearchKeys
        assertEquals(listOf("谭宪炳", "太子炳"), keys[0])
        assertEquals(listOf("刘丽琼", "八姑"), keys[1])
    }

    @Test
    fun `capability flags follow the dataset contents`() {
        val full = catalog()
        assertTrue(full.hasCharacters)
        assertTrue(full.hasTags)
        assertFalse(full.hasStorylines)
        assertFalse(full.hasSynopses)
    }
}
