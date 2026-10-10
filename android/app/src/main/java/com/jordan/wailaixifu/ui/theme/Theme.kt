package com.jordan.wailaixifu.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.jordan.wailaixifu.data.SiteTheme

/**
 * Values a screen needs on top of Material's [ColorScheme]: banner colours, the keyword highlight
 * tint, and a few accents the source pages use directly (满洲窗四色, the 主线 star colour, and the
 * "want to watch" gold). Exposed through [LocalBrandColors] so no composable has to branch on the
 * theme to pick a colour.
 */
data class BrandColors(
    val bannerTop: Color,
    val bannerMid: Color,
    val bannerText: Color,
    val bannerSub: Color,
    val highlight: Color,
    /** Colour of the ★ 主线 marker and the active filter chip text. */
    val mark: Color,
    /** Colour of an active "想看" action. */
    val want: Color,
    val wantText: Color,
    /** Colour of the 看过 action when active. */
    val seen: Color,
    val seenText: Color,
    /** 满洲窗 four-colour strip: 朱红 / 石蓝 / 青绿 / 藤黄. */
    val windowStrip: List<Color>,
    /** Decorative frame colour, drawn as rules and card borders. */
    val frame: Color,
    /** The ground the cards sit on. */
    val ground: Color,
) {
    companion object {
        fun of(theme: SiteTheme, dark: Boolean): BrandColors = when (theme) {
            SiteTheme.Lingnan -> BrandColors(
                bannerTop = LingnanPalette.BannerTop,
                bannerMid = LingnanPalette.BannerMid,
                bannerText = LingnanPalette.BannerText,
                bannerSub = LingnanPalette.BannerSub,
                highlight = LingnanPalette.Highlight,
                mark = LingnanPalette.Vermilion,
                want = Color(0x3DC79A45),
                wantText = Color(0xFF7A5A16),
                seen = LingnanPalette.Jade,
                seenText = LingnanPalette.Cream,
                windowStrip = listOf(
                    LingnanPalette.Vermilion,
                    Color(0xFF2F5F8F),
                    LingnanPalette.Jade,
                    LingnanPalette.Gold,
                ),
                frame = LingnanPalette.Gold,
                ground = LingnanPalette.Paper,
            )

            SiteTheme.Traditional -> BrandColors(
                bannerTop = if (dark) Color(0xFF2A1D14) else TraditionalPalette.BannerTop,
                bannerMid = if (dark) Color(0xFF1A120C) else TraditionalPalette.BannerMid,
                bannerText = if (dark) Color(0xFFF3E7CE) else TraditionalPalette.BannerText,
                bannerSub = if (dark) Color(0xFFB8A58A) else TraditionalPalette.BannerSub,
                highlight = TraditionalPalette.Highlight,
                mark = if (dark) Color(0xFFF08A7D) else TraditionalPalette.Red,
                want = if (dark) Color(0x3DD9A621) else Color(0x3DD9A621),
                wantText = if (dark) Color(0xFFF0C766) else Color(0xFF8A5D00),
                seen = TraditionalPalette.Green,
                seenText = Color.White,
                windowStrip = listOf(
                    TraditionalPalette.Red,
                    TraditionalPalette.Blue,
                    TraditionalPalette.Green,
                    TraditionalPalette.Yellow,
                ),
                frame = if (dark) TraditionalPalette.WoodLight else TraditionalPalette.Wood,
                ground = if (dark) Color(0xFF1A120C) else TraditionalPalette.Cream,
            )

            SiteTheme.Modern -> BrandColors(
                bannerTop = if (dark) Color(0xFF1C1C1E) else ModernPalette.BannerTop,
                bannerMid = if (dark) Color(0xFF141416) else ModernPalette.BannerMid,
                bannerText = if (dark) Color(0xFFF5F5F7) else ModernPalette.BannerText,
                bannerSub = if (dark) Color(0xFF98989F) else ModernPalette.BannerSub,
                highlight = ModernPalette.Highlight,
                mark = ModernPalette.Blue,
                want = Color(0x3D0F766E),
                wantText = if (dark) Color(0xFF5EEAD4) else ModernPalette.Teal,
                seen = ModernPalette.Teal,
                seenText = Color.White,
                windowStrip = listOf(
                    ModernPalette.Blue,
                    ModernPalette.Teal,
                    ModernPalette.TealLight,
                    ModernPalette.TextSecondary,
                ),
                frame = if (dark) Color(0xFF3A3A3C) else ModernPalette.Separator,
                ground = if (dark) Color(0xFF000000) else ModernPalette.GroundAlt,
            )
        }
    }
}

val LocalBrandColors = staticCompositionLocalOf {
    BrandColors.of(SiteTheme.Lingnan, dark = false)
}

private val LingnanLight = lightColorScheme(
    primary = LingnanPalette.Vermilion,
    onPrimary = LingnanPalette.Cream,
    primaryContainer = LingnanPalette.Paper2,
    onPrimaryContainer = LingnanPalette.Ink,
    secondary = LingnanPalette.Gold,
    onSecondary = LingnanPalette.Ink,
    secondaryContainer = LingnanPalette.Paper2,
    onSecondaryContainer = LingnanPalette.Ink,
    tertiary = LingnanPalette.Jade,
    onTertiary = LingnanPalette.Cream,
    background = LingnanPalette.Paper,
    onBackground = LingnanPalette.Ink,
    surface = LingnanPalette.Cream,
    onSurface = LingnanPalette.Ink,
    surfaceVariant = LingnanPalette.Paper2,
    onSurfaceVariant = LingnanPalette.Dust,
    outline = LingnanPalette.Border,
    outlineVariant = LingnanPalette.Border,
    error = LingnanPalette.Vermilion,
    onError = LingnanPalette.Cream,
)

/** Night counterpart: the same lacquer-and-gold scheme on a dark ink ground. */
private val LingnanDark = darkColorScheme(
    primary = LingnanPalette.Gold,
    onPrimary = Color(0xFF2A1A10),
    primaryContainer = Color(0xFF3E2A18),
    onPrimaryContainer = LingnanPalette.BannerText,
    secondary = LingnanPalette.VermilionSoft,
    onSecondary = LingnanPalette.Cream,
    secondaryContainer = Color(0xFF43201A),
    onSecondaryContainer = LingnanPalette.CreamDim,
    tertiary = Color(0xFF8FB6A3),
    onTertiary = Color(0xFF14231C),
    background = Color(0xFF1A120C),
    onBackground = LingnanPalette.CreamDim,
    surface = Color(0xFF241A12),
    onSurface = LingnanPalette.CreamDim,
    surfaceVariant = Color(0xFF33261A),
    onSurfaceVariant = Color(0xFFCBAF8C),
    outline = Color(0xFF6B5333),
    outlineVariant = Color(0xFF4A3823),
    error = LingnanPalette.VermilionSoft,
    onError = LingnanPalette.Cream,
)

/** 宣纸卡片 + 旧木描边，坐在麻石调的地面上。 */
private val TraditionalLight = lightColorScheme(
    primary = TraditionalPalette.Wood,
    onPrimary = TraditionalPalette.Cream,
    primaryContainer = TraditionalPalette.Cream,
    onPrimaryContainer = TraditionalPalette.WoodDark,
    secondary = TraditionalPalette.Red,
    onSecondary = TraditionalPalette.Paper,
    secondaryContainer = Color(0x1FB8352B),
    onSecondaryContainer = TraditionalPalette.Red,
    tertiary = TraditionalPalette.Green,
    onTertiary = Color.White,
    background = TraditionalPalette.Cream,
    onBackground = TraditionalPalette.Ink,
    surface = TraditionalPalette.Paper,
    onSurface = TraditionalPalette.Ink,
    surfaceVariant = Color(0x14FFFFFF),
    onSurfaceVariant = TraditionalPalette.Secondary,
    outline = TraditionalPalette.Wood,
    outlineVariant = Color(0x385A3A22),
    error = TraditionalPalette.Red,
    onError = Color.White,
)

private val TraditionalDark = darkColorScheme(
    primary = TraditionalPalette.WoodLight,
    onPrimary = Color(0xFF2A1D10),
    primaryContainer = Color(0xFF4A3826),
    onPrimaryContainer = TraditionalPalette.Cream,
    secondary = Color(0xFFF08A7D),
    onSecondary = Color(0xFF3B1512),
    secondaryContainer = Color(0xFF43201A),
    onSecondaryContainer = Color(0xFFF3E7CE),
    tertiary = Color(0xFF8FB6A3),
    onTertiary = Color(0xFF14231C),
    background = Color(0xFF1A120C),
    onBackground = Color(0xFFF3E7CE),
    surface = Color(0xFF2A1D14),
    onSurface = Color(0xFFF3E7CE),
    surfaceVariant = Color(0x14FFFFFF),
    onSurfaceVariant = Color(0xFFB8A58A),
    outline = TraditionalPalette.WoodLight,
    outlineVariant = Color(0xFF6B5238),
    error = Color(0xFFF08A7D),
    onError = Color(0xFF3B1512),
)

/** White ground, translucent grey fills, hairline separators, 青绿 accent. */
private val ModernLight = lightColorScheme(
    primary = ModernPalette.Teal,
    onPrimary = ModernPalette.Ground,
    primaryContainer = ModernPalette.TealWash,
    onPrimaryContainer = ModernPalette.Teal,
    secondary = ModernPalette.Blue,
    onSecondary = ModernPalette.Ground,
    secondaryContainer = Color(0x1F007AFF),
    onSecondaryContainer = ModernPalette.Blue,
    tertiary = ModernPalette.TextSecondary,
    onTertiary = ModernPalette.Ground,
    background = ModernPalette.GroundAlt,
    onBackground = ModernPalette.Text,
    surface = ModernPalette.Ground,
    onSurface = ModernPalette.Text,
    surfaceVariant = ModernPalette.Fill,
    onSurfaceVariant = ModernPalette.TextSecondary,
    outline = ModernPalette.Separator,
    outlineVariant = ModernPalette.Separator,
    error = Color(0xFFDC2626),
    onError = ModernPalette.Ground,
)

private val ModernDark = darkColorScheme(
    primary = ModernPalette.TealLight,
    onPrimary = Color(0xFF052E2B),
    primaryContainer = Color(0xFF11403C),
    onPrimaryContainer = Color(0xFFCCFBF1),
    secondary = Color(0xFF64D2FF),
    onSecondary = Color(0xFF052E2B),
    secondaryContainer = Color(0xFF134E4A),
    onSecondaryContainer = Color(0xFFCCFBF1),
    tertiary = Color(0xFF94A3B8),
    onTertiary = Color(0xFF0B1220),
    background = Color(0xFF000000),
    onBackground = Color(0xFFF5F5F7),
    surface = Color(0xFF1C1C1E),
    onSurface = Color(0xFFF5F5F7),
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFF98989F),
    outline = Color(0xFF3A3A3C),
    outlineVariant = Color(0xFF2C2C2E),
    error = Color(0xFFFF453A),
    onError = Color(0xFF2A0A08),
)

internal fun colorSchemeFor(theme: SiteTheme, dark: Boolean): ColorScheme = when (theme) {
    SiteTheme.Lingnan -> if (dark) LingnanDark else LingnanLight
    SiteTheme.Traditional -> if (dark) TraditionalDark else TraditionalLight
    SiteTheme.Modern -> if (dark) ModernDark else ModernLight
}

internal fun typographyFor(theme: SiteTheme): Typography = when (theme) {
    SiteTheme.Modern -> SansTypography
    SiteTheme.Lingnan, SiteTheme.Traditional -> SerifTypography
}

/** Applies one site's palette and typography to everything inside [content]. */
@Composable
fun SiteThemeProvider(
    theme: SiteTheme,
    dark: Boolean,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalBrandColors provides BrandColors.of(theme, dark)) {
        MaterialTheme(
            colorScheme = colorSchemeFor(theme, dark),
            typography = typographyFor(theme),
            content = content,
        )
    }
}
