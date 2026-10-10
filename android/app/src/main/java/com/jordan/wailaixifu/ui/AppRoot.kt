package com.jordan.wailaixifu.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jordan.wailaixifu.R
import com.jordan.wailaixifu.data.Site
import com.jordan.wailaixifu.ui.theme.SiteThemeProvider
import com.jordan.wailaixifu.ui.theme.colorSchemeFor
import com.jordan.wailaixifu.ui.theme.typographyFor

/**
 * The shell of the app: one screen per site behind a three-entry bottom bar.
 *
 * 传统 and 现代 are two presentations of 《七十二家房客》 (different palettes and typography),
 * 外来 is 《外来媳妇本地郎》 with its own dataset and 岭南 palette.
 */
@Composable
fun AppRoot(viewModel: MainViewModel) {
    val site by viewModel.selectedSite.collectAsStateWithLifecycle()
    val dark = isSystemInDarkTheme()

    // The bottom bar uses the active site's palette, so switching tabs re-skins immediately.
    val scheme = colorSchemeFor(site.theme, dark)
    val typography = typographyFor(site.theme)

    Scaffold(
        containerColor = scheme.background,
        bottomBar = {
            NavigationBar(containerColor = scheme.surface) {
                Site.entries.forEach { destination ->
                    val label = stringResource(labelRes(destination))
                    NavigationBarItem(
                        selected = destination == site,
                        onClick = { viewModel.selectSite(destination) },
                        icon = {
                            Icon(
                                imageVector = siteIcon(destination),
                                contentDescription = label,
                                // Let NavigationBarItem's colors drive the tint.
                                tint = Color.Unspecified,
                            )
                        },
                        label = {
                            Text(text = label, style = typography.labelMedium)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = scheme.primary,
                            selectedTextColor = scheme.primary,
                            indicatorColor = scheme.primaryContainer,
                            unselectedIconColor = scheme.onSurfaceVariant,
                            unselectedTextColor = scheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        Crossfade(
            targetState = site,
            animationSpec = tween(durationMillis = 220),
            label = "site",
        ) { activeSite ->
            Box(modifier = Modifier.fillMaxSize()) {
                SiteThemeProvider(theme = activeSite.theme, dark = dark) {
                    // MainScreen owns its own scrolling and applies innerPadding itself.
                    MainScreen(
                        viewModel = viewModel,
                        contentPadding = innerPadding,
                    )
                }
            }
        }
    }
}

private fun labelRes(site: Site): Int = when (site) {
    Site.Traditional -> R.string.site_traditional
    Site.Modern -> R.string.site_modern
    Site.WaiLai -> R.string.site_wailai
}

/* ------------------------------------------------------------------------------------------- */
/* Tab icons: drawn as vectors so no icon dependency is needed.                                 */
/* ------------------------------------------------------------------------------------------- */

private val TraditionalIcon: ImageVector by lazy {
    Builder(
        name = "Traditional",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        // A 西关大屋 gable roof above two 趟栊门 bays.
        path(fill = SolidColor(Color.Black)) {
            moveTo(2f, 9f); lineTo(12f, 3f); lineTo(22f, 9f); lineTo(22f, 11f); lineTo(2f, 11f); close()
            moveTo(5f, 13f); lineTo(10.5f, 13f); lineTo(10.5f, 21f); lineTo(5f, 21f); close()
            moveTo(13.5f, 13f); lineTo(19f, 13f); lineTo(19f, 21f); lineTo(13.5f, 21f); close()
        }
    }.build()
}

private val ModernIcon: ImageVector by lazy {
    Builder(
        name = "Modern",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        // A flat card grid, the contemporary counterpart.
        path(fill = SolidColor(Color.Black)) {
            moveTo(3f, 4f); lineTo(21f, 4f); lineTo(21f, 9f); lineTo(3f, 9f); close()
            moveTo(3f, 12f); lineTo(11f, 12f); lineTo(11f, 20f); lineTo(3f, 20f); close()
            moveTo(13f, 12f); lineTo(21f, 12f); lineTo(21f, 20f); lineTo(13f, 20f); close()
        }
    }.build()
}

private val WaiLaiIcon: ImageVector by lazy {
    Builder(
        name = "WaiLai",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        // A 镬耳屋 silhouette: the curved gable walls of a Lingnan house.
        // PathBuilder uses SVG-style names, so this is curveTo rather than Path.cubicTo.
        path(fill = SolidColor(Color.Black)) {
            moveTo(4f, 20f)
            lineTo(4f, 10f)
            curveTo(4f, 4f, 8f, 3f, 12f, 3f)
            curveTo(16f, 3f, 20f, 4f, 20f, 10f)
            lineTo(20f, 20f)
            close()
        }
    }.build()
}

private fun siteIcon(site: Site): ImageVector = when (site) {
    Site.Traditional -> TraditionalIcon
    Site.Modern -> ModernIcon
    Site.WaiLai -> WaiLaiIcon
}
