package com.jordan.wailaixifu.data

/**
 * Per-site copy for the banner, search box and notice card. Baking this into the destination
 * model keeps 《七十二家房客》 and 《外来媳妇本地郎》 fully independent instead of leaking one
 * drama's wording into the other.
 */
data class Brand(
    val title: String,
    val titleAccent: String,
    val deco: String,
    val subtitle: String,
    val searchHint: String,
    val searchHelper: String,
    val noticeTitle: String,
    val noticeBody: String,
    val footer: String,
    val footerAuthor: String,
    val detailLine: String,
    /** Episode-number unit, e.g. "集". */
    val episodeUnit: String = "集",
    /** Badge prefix for the section, e.g. "第…部" or "第…季". */
    val sectionPrefix: String = "第",
    val sectionSuffix: String = "部",
    val sectionFallback: String = "未分部",
    /** Label of the "all sections" chip. */
    val allSectionsLabel: String = "全部",
    val mergedLabel: String = "故事合集",
    val singleLabel: String = "单集",
    /** Search placeholder for the similar-story drawer. */
    val pickerKeywordHint: String = "留空则不按关键词过滤",
) {
    companion object {
        /** 《外来媳妇本地郎》 — 14 部, no characters/tags/storylines. */
        val WaiLai = Brand(
            title = "外来媳妇",
            titleAccent = "本地郎",
            deco = "广东 · 岭南 · 西关 · 情景喜剧",
            subtitle = "剧集查询 · 小红书 @Jordan",
            searchHint = "输入集数（如 4703）或标题关键词（如 康家、阿娇、昌盛街）…",
            searchHelper = "支持集数范围定位、标题模糊搜索；可与“分部”标签组合筛选。",
            noticeTitle = "资料说明：",
            noticeBody = "来源网络。2006年“千集大餐”为特别节目，不计入总集数；" +
                "第十三部网络信息来源缺失，存在缺集。本工具共整理出 4647 条有标题记录、" +
                "2573 个故事卡片。",
            footer = "《外来媳妇本地郎》· 广州西关 · 康家故事",
            footerAuthor = "网站作者：小红书 @Jordan",
            detailLine = "剧集查询 · 共 14 部 · 2573 个故事卡片",
            allSectionsLabel = "全部",
        )

        /** 《七十二家房客》 — 19 季, characters + tags + storylines. Shared by 传统 and 现代. */
        val QiErShi = Brand(
            title = "七十二家",
            titleAccent = "房客",
            deco = "南方电视台 · 情景喜剧 · 岭南市井风情",
            subtitle = "剧集查询 · 第一至十九季 · 共 1552 个故事 · 主线剧情 · 34位角色",
            searchHint = "集数、标题、梗概或角色，如 45、八姑",
            searchHelper = "可配合季份、主题、主线筛选联合过滤；点击卡片上的角色可跳转角色查询",
            noticeTitle = "资料说明：",
            noticeBody = "来源网络整理，按播出顺序编号；如有缺集欢迎补充。",
            footer = "《七十二家房客》· 广州 · 西关大屋 · 岭南情怀",
            footerAuthor = "网站作者：小红书 @Jordan",
            detailLine = "剧集查询 · 第一至十九季 · 共 1552 个故事",
            sectionSuffix = "季",
            allSectionsLabel = "全部季",
        )
    }
}
