package com.jordan.wailaixifu.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Palette of the 外来 site: 朱红 / 描金 / 宣纸 / 墨色, taken from the original web page.
 */
object LingnanPalette {
    val Vermilion = Color(0xFFA92D22)
    val Gold = Color(0xFFC79A45)
    val Ink = Color(0xFF24160F)
    val Paper = Color(0xFFF5EBD5)
    val Paper2 = Color(0xFFE9DAB9)
    val Cream = Color(0xFFFFFDF7)
    val Jade = Color(0xFF527A68)
    val Dust = Color(0xFF806C55)
    val Border = Color(0xFFD5B986)

    val BannerTop = Color(0xFF21130C)
    val BannerMid = Color(0xFF452312)
    val BannerText = Color(0xFFF7EEDB)
    val BannerSub = Color(0xFFDCCBA8)

    val VermilionSoft = Color(0xFFC4443A)
    val CreamDim = Color(0xFFEFE3CA)
    val Highlight = Color(0x52C79A45)
}

/**
 * Palette of the 传统 site, taken from the 岭南皮肤 block of v2.20_岭南传统版.html:
 * 麻石底 / 宣纸 / 旧木框 / 满洲窗四色 (朱红、石蓝、青绿、藤黄).
 */
object TraditionalPalette {
    /** 宣纸 */
    val Paper = Color(0xFFFBF5E6)
    val Cream = Color(0xFFF5EBD3)

    /** 旧木 */
    val Wood = Color(0xFF5A3A22)
    val WoodLight = Color(0xFF8A5A35)
    val WoodDark = Color(0xFF3B2414)

    /** 满洲窗四色 */
    val Red = Color(0xFFB8352B)
    val Blue = Color(0xFF2F5F8F)
    val Green = Color(0xFF3E7A5B)
    val Yellow = Color(0xFFD9A621)

    /** 墨色与灰调 */
    val Ink = Color(0xFF2B1B10)
    val Ink2 = Color(0xFF4A3626)
    val Secondary = Color(0xFF7A6450)

    /** 麻石 */
    val Granite = Color(0xFF6F665C)
    val GraniteSoft = Color(0xFF8C8378)

    /** 高亮与描边 */
    val Highlight = Color(0x61D9A621)

    val BannerText = Color(0xFF2B1B10)
    val BannerSub = Color(0xFF7A6450)
    val BannerTop = Color(0xFFFBF5E6)
    val BannerMid = Color(0xFFF5EBD3)
}

/**
 * Palette of the 现代 site. It follows the iOS-flavoured metrics of v2.20_现代简洁版.html
 * (translucent surfaces, hairline separators, blue accent, filled dark pill for the active tab)
 * but uses 青绿 as the accent until the exact hue of that build is pinned down.
 */
object ModernPalette {
    val Ground = Color(0xFFFFFFFF)
    val GroundAlt = Color(0xFFF2F2F7)

    /** System-ish blue used for accents and the active 主线 flag. */
    val Blue = Color(0xFF007AFF)

    /** 青绿 accent, used for primary actions. */
    val Teal = Color(0xFF0F766E)
    val TealLight = Color(0xFF14B8A6)
    val TealWash = Color(0xFFE6F4F2)

    val Text = Color(0xFF1C1C1E)
    val TextSecondary = Color(0xFF8E8E93)
    val Separator = Color(0xFFE5E5EA)
    val Fill = Color(0xFFF2F2F7)

    val Highlight = Color(0x4014B8A6)

    val BannerText = Color(0xFF1C1C1E)
    val BannerSub = Color(0xFF8E8E93)
    val BannerTop = Color(0xFFF2F2F7)
    val BannerMid = Color(0xFFE9E9EE)
}
