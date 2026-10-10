package com.jordan.wailaixifu.data

/**
 * The three "sites" of the app.
 *
 * 传统 and 现代 are two presentations of the same drama (《七十二家房客》) with different visual
 * themes; 外来 is a different drama (《外来媳妇本地郎》) and carries its own palette.
 */
enum class Site(
    val dataset: String,
    val theme: SiteTheme,
) {
    Traditional("qiershi", SiteTheme.Traditional),
    Modern("qiershi", SiteTheme.Modern),
    WaiLai("wailai", SiteTheme.Lingnan),
    ;

    companion object {
        val default: Site = WaiLai
        fun fromName(name: String?): Site =
            entries.firstOrNull { it.name == name } ?: default
    }
}

/** Which palette + typography a site renders with. */
enum class SiteTheme { Lingnan, Traditional, Modern }
