package com.jordan.wailaixifu.ui

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jordan.wailaixifu.data.Episode
import com.jordan.wailaixifu.ui.components.EpisodeContext
import com.jordan.wailaixifu.ui.components.strokeRing
import com.jordan.wailaixifu.ui.theme.LocalBrandColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

/**
 * 分享卡片: the 1080×1520 poster of the source page, redrawn on a Compose canvas.
 *
 * The web version builds this with 2D canvas calls; here the same layout is expressed in design
 * units (1080 wide) and scaled to whatever the preview is, then rendered to a bitmap for saving
 * or sharing.
 */
private const val CARD_WIDTH = 1080f
private const val CARD_HEIGHT = 1520f

@Composable
fun ShareModal(
    episode: Episode?,
    context: EpisodeContext,
    onDismiss: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val applicationContext = LocalContext.current.applicationContext
    val density = LocalDensity.current
    val brandColors = LocalBrandColors.current
    var status by remember { mutableStateOf<String?>(null) }

    // The accent is the site's own colour, so the poster matches the theme the reader is in.
    val cardStyle = remember(context.brand, brandColors.mark, brandColors.frame) {
        ShareCardStyle(
            accent = brandColors.mark,
            frame = brandColors.frame,
            title = context.brand.title + context.brand.titleAccent,
            subtitle = context.brand.deco,
            footer = context.brand.footer,
            footerAuthor = context.brand.footerAuthor,
        )
    }

    // The preview is rendered once and reused for both display and export.
    val bitmap = remember(episode?.index, context.characters.size, density.density, cardStyle) {
        episode?.let { renderShareBitmap(it, context, density, cardStyle) }
    }

    ModalShell(onDismiss = onDismiss) {
        if (episode == null || bitmap == null) {
            Text(
                text = "无法生成分享卡片",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.error,
            )
            PickButton(text = "关闭", primary = false, modifier = Modifier.fillMaxWidth(), onClick = onDismiss)
            return@ModalShell
        }

        Text(
            text = "分享卡片 · ${context.sectionLabel(episode)} ${episode.title}",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = scheme.onSurface,
        )

        Surface(
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, scheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(CARD_WIDTH / CARD_HEIGHT),
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "分享卡片预览",
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Text(
            text = status ?: "可保存到相册，或通过其他应用分享",
            style = MaterialTheme.typography.labelSmall,
            color = if (status == null) scheme.onSurfaceVariant else scheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PickButton(
                text = "⬇ 保存图片",
                primary = true,
                modifier = Modifier.weight(1f),
                onClick = {
                    scope.launch {
                        val fileName = shareFileName(episode)
                        val ok = withContext(Dispatchers.IO) {
                            saveToGallery(applicationContext, bitmap, fileName)
                        }
                        status = if (ok) "已保存到相册：$fileName" else "保存失败，请检查存储权限"
                    }
                },
            )
            PickButton(
                text = "📤 分享",
                primary = false,
                modifier = Modifier.weight(1f),
                onClick = {
                    shareBitmap(applicationContext, bitmap, shareFileName(episode), episode.title)
                },
            )
        }

        Spacer(Modifier.height(2.dp))
        PickButton(
            text = "关闭",
            primary = false,
            modifier = Modifier.fillMaxWidth(),
            onClick = onDismiss,
        )
    }
}

private fun shareFileName(episode: Episode): String {
    val range = if (episode.isMerged) "${episode.start}-${episode.end}" else "${episode.start}"
    return "七十二家房客_第${episode.section}季_第${range}集.png"
}

/* ------------------------------------------------------------------------------------------- */
/* Bitmap rendering                                                                              */
/* ------------------------------------------------------------------------------------------- */

private fun renderShareBitmap(
    episode: Episode,
    context: EpisodeContext,
    density: androidx.compose.ui.unit.Density,
    style: ShareCardStyle,
): Bitmap {
    val bitmap = Bitmap.createBitmap(CARD_WIDTH.toInt(), CARD_HEIGHT.toInt(), Bitmap.Config.ARGB_8888)
    val canvas = androidx.compose.ui.graphics.Canvas(bitmap.asImageBitmap())
    // The poster is authored in the source page's canvas units and rendered once at density 1; the
    // surrounding preview scales the bitmap. The device density is passed separately so 1 dp
    // strokes inside the drawing land the same relative width as they do on screen.
    androidx.compose.ui.graphics.drawscope.CanvasDrawScope().draw(
        density = androidx.compose.ui.unit.Density(1f),
        layoutDirection = androidx.compose.ui.unit.LayoutDirection.Ltr,
        canvas = canvas,
        size = Size(CARD_WIDTH, CARD_HEIGHT),
    ) {
        drawShareCard(episode, context, density, style)
    }
    return bitmap
}

/**
 * The parts of the poster that come from the active site rather than from the story itself.
 *
 * The source page picked the accent from `THEME[first tag id]`, which produced an arbitrary hue
 * unrelated to the surrounding palette. Here the accent is the site's own 主线 colour, so the
 * poster always matches the theme the reader is looking at.
 */
private data class ShareCardStyle(
    val accent: Color,
    val frame: Color,
    val title: String,
    val subtitle: String,
    val footer: String,
    val footerAuthor: String,
)

/** Draws the whole poster. Coordinates are the source page's own canvas units. */
private fun DrawScope.drawShareCard(
    episode: Episode,
    context: EpisodeContext,
    density: androidx.compose.ui.unit.Density,
    style: ShareCardStyle,
) {
    val accent = style.accent
    val cream = Color(0xFFF5EBD3)
    val gold = Color(0xFFC9963A)
    val inkPlate = Color(0xFF2A1608)

    // 宣纸底
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFFFBF3DE), Color(0xFFF0E2BF)),
            start = Offset(0f, 0f),
            end = Offset(0f, CARD_HEIGHT),
        ),
        size = Size(CARD_WIDTH, CARD_HEIGHT),
    )

    // 描金格纹
    val grid = gold.copy(alpha = 0.14f)
    var gx = 0f
    while (gx < CARD_WIDTH) {
        drawLine(grid, Offset(gx, 0f), Offset(gx, CARD_HEIGHT), 1f)
        gx += 60f
    }
    var gy = 0f
    while (gy < CARD_HEIGHT) {
        drawLine(grid, Offset(0f, gy), Offset(CARD_WIDTH, gy), 1f)
        gy += 60f
    }

    // 双线外框
    drawRect(
        color = accent,
        topLeft = Offset(30f, 30f),
        size = Size(CARD_WIDTH - 60f, CARD_HEIGHT - 60f),
        style = Stroke(width = 8f),
    )
    drawRect(
        color = gold,
        topLeft = Offset(46f, 46f),
        size = Size(CARD_WIDTH - 92f, CARD_HEIGHT - 92f),
        style = Stroke(width = 2f),
    )

    // 木框 + 满洲窗
    drawRect(inkPlate, Offset(70f, 70f), Size(CARD_WIDTH - 140f, 246f))
    val glassColors = listOf(
        Color(0xFFB5311A),
        Color(0xFFC9963A),
        Color(0xFF2F7A74),
        Color(0xFF2F4A6B),
        Color(0xFF8E3B7A),
    )
    val wx = 88f
    val wy = 88f
    val ww = CARD_WIDTH - 176f
    val wh = 210f
    val cols = 12
    val rows = 3
    val cw = ww / cols
    val ch = wh / rows
    for (r in 0 until rows) {
        for (c in 0 until cols) {
            val cellX = wx + c * cw
            val cellY = wy + r * ch
            drawRect(glassColors[(r * 2 + c * 3) % 5], Offset(cellX, cellY), Size(cw, ch))
            // 玻璃菱形高光
            val diamond = Path().apply {
                moveTo(cellX + cw / 2f, cellY + 8f)
                lineTo(cellX + cw - 8f, cellY + ch / 2f)
                lineTo(cellX + cw / 2f, cellY + ch - 8f)
                lineTo(cellX + 8f, cellY + ch / 2f)
                close()
            }
            drawPath(diamond, Color.White.copy(alpha = 0.28f))
            drawCircle(
                glassColors[(r * 2 + c * 3 + 2) % 5],
                radius = 6f * density.density,
                center = Offset(cellX + cw / 2f, cellY + ch / 2f),
            )
        }
    }
    for (r in 0..rows) {
        val y = wy + r * ch
        drawLine(inkPlate, Offset(wx, y), Offset(wx + ww, y), 7f)
    }
    for (c in 0..cols) {
        val x = wx + c * cw
        drawLine(inkPlate, Offset(x, wy), Offset(x, wy + wh), 7f)
    }

    val canvas = drawContext.canvas.nativeCanvas
    // Typeface.create with a null family falls back to the system face, which renders CJK.
    val serif = Typeface.create(Typeface.SERIF, Typeface.NORMAL) ?: Typeface.DEFAULT
    val serifBold = Typeface.create(Typeface.SERIF, Typeface.BOLD) ?: Typeface.DEFAULT_BOLD
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = serif
        textAlign = Paint.Align.CENTER
    }
    fun textPaint(size: Float, color: Color, bold: Boolean = false) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = if (bold) serifBold else serif
        textSize = size
        this.color = color.toArgb()
        textAlign = Paint.Align.CENTER
    }

    // 牌匾
    drawRoundRect(
        color = Color(0xFF1C1008),
        topLeft = Offset(270f, 116f),
        size = Size(540f, 154f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f),
    )
    drawRoundRect(
        color = gold,
        topLeft = Offset(278f, 124f),
        size = Size(524f, 138f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f),
        style = Stroke(width = 4f),
    )
    canvas.drawText(style.title, CARD_WIDTH / 2f, 200f, textPaint(66f, Color(0xFFE8C26A), bold = true))
    canvas.drawText(style.subtitle, CARD_WIDTH / 2f, 246f, textPaint(24f, gold))

    // 印章圆 + 集数
    val sealCenter = Offset(215f, 420f)
    drawCircle(accent, radius = 92f, center = sealCenter)
    strokeRing(cream, radius = 80f, width = 3f, center = sealCenter)
    canvas.drawText("${episode.section}", sealCenter.x, sealCenter.y + 22f, textPaint(84f, cream, bold = true))
    canvas.drawText("季", sealCenter.x, sealCenter.y + 62f, textPaint(28f, cream))

    val merged = episode.isMerged
    val episodeText = if (merged) "${episode.start}–${episode.end}" else "${episode.start}"
    val left = textPaint(30f, Color(0xFF8B7355)).apply { textAlign = Paint.Align.LEFT }
    canvas.drawText(if (merged) "合集" else "单集", 350f, 372f, left)
    val epPaint = textPaint(if (episodeText.length > 5) 74f else 88f, Color(0xFF1C1008), bold = true)
        .apply { textAlign = Paint.Align.LEFT }
    canvas.drawText("第${episodeText}集", 350f, 462f, epPaint)

    // 分隔线 + 花饰
    drawLine(gold, Offset(90f, 552f), Offset(470f, 552f), 2f)
    drawLine(gold, Offset(610f, 552f), Offset(990f, 552f), 2f)
    canvas.drawText("◆ ❧ ◆", CARD_WIDTH / 2f, 564f, textPaint(30f, gold))

    // 标题（最多两行，自动缩字号）
    var titleSize = 92f
    var lines: List<String>
    do {
        paint.textSize = titleSize
        paint.typeface = serifBold
        lines = wrapText(paint, episode.title, 880f)
        titleSize -= 4f
    } while (lines.size > 2 && titleSize > 52f)
    titleSize += 4f
    paint.textSize = titleSize
    val titleY = 640f + if (lines.size == 1) 20f else 0f
    lines.take(2).forEachIndexed { index, line ->
        canvas.drawText(
            line,
            CARD_WIDTH / 2f,
            titleY + index * (titleSize + 16f),
            textPaint(titleSize, accent, bold = true),
        )
    }
    var belowTitle = titleY + (minOf(lines.size, 2) - 1) * (titleSize + 16f) + 44f

    // 主题标签
    val tagLabels = episode.tagIds.mapNotNull { context.tag(it) }
    if (tagLabels.isNotEmpty()) {
        val tagText = textPaint(28f, Color.White)
        tagText.textAlign = Paint.Align.LEFT
        val widths = tagLabels.map { tagText.measureText(it) + 40f }
        var tagX = CARD_WIDTH / 2f - (widths.sum() + (tagLabels.size - 1) * 14f) / 2f
        tagLabels.forEachIndexed { index, label ->
            drawRoundRect(
                color = accent,
                topLeft = Offset(tagX, belowTitle),
                size = Size(widths[index], 46f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(23f, 23f),
            )
            // Re-measure for centring inside the pill.
            val labelPaint = textPaint(28f, Color.White).apply { textAlign = Paint.Align.LEFT }
            canvas.drawText(label, tagX + 20f, belowTitle + 32f, labelPaint)
            tagX += widths[index] + 14f
        }
        belowTitle += 46f + 20f
    }

    // 梗概
    val maxBodyHeight = 1262f - (belowTitle + 86f)
    var synopsisSize = 50f
    var synopsisLines: List<String>
    var lineHeight: Float
    do {
        paint.typeface = serif
        paint.textSize = synopsisSize
        lineHeight = synopsisSize * 1.62f
        synopsisLines = wrapText(paint, episode.synopsis, CARD_WIDTH - 270f)
        synopsisSize -= 2f
    } while (synopsisLines.size * lineHeight > maxBodyHeight - 50f && synopsisSize > 26f)
    val maxLines = max(1, ((maxBodyHeight - 50f) / lineHeight).toInt())
    if (synopsisLines.size > maxLines) {
        synopsisLines = synopsisLines.take(maxLines).toMutableList().also { kept ->
            var last = kept.last()
            val bodyPaint = textPaint(synopsisSize, Color(0xFF3A2A1A))
            while (bodyPaint.measureText("$last…") > CARD_WIDTH - 270f && last.length > 1) {
                last = last.dropLast(1)
            }
            kept[kept.size - 1] = "$last…"
        }
    }
    val bodyHeight = minOf(maxBodyHeight, max(230f, synopsisLines.size * lineHeight + 64f))
    val bodyTop = belowTitle + 90f
    drawRect(Color(0xE6FFFDF5), Offset(90f, bodyTop), Size(CARD_WIDTH - 180f, bodyHeight))
    drawRect(
        color = Color(0xFFD4B483),
        topLeft = Offset(90f, bodyTop),
        size = Size(CARD_WIDTH - 180f, bodyHeight),
        style = Stroke(width = 2f),
    )
    drawRect(accent, Offset(90f, bodyTop), Size(10f, bodyHeight))
    val bodyPaint = textPaint(synopsisSize - 4f, Color(0xFF3A2A1A)).apply { textAlign = Paint.Align.LEFT }
    val bodyTextTop = bodyTop + (bodyHeight - synopsisLines.size * lineHeight) / 2f + synopsisSize
    synopsisLines.forEachIndexed { index, line ->
        canvas.drawText(line, 128f, bodyTextTop + index * lineHeight, bodyPaint)
    }

    // 出场角色
    var cast = "出场：" + episode.characterIds.mapNotNull { context.character(it)?.name }.joinToString("、")
    val castPaint = textPaint(28f, Color(0xFF8B7355)).apply { textAlign = Paint.Align.LEFT }
    while (castPaint.measureText(cast) > CARD_WIDTH - 180f && cast.length > 6) {
        cast = cast.dropLast(2)
    }
    canvas.drawText(cast, 90f, minOf(bodyTop + bodyHeight + 44f, 1306f), castPaint)

    // 瓦片檐
    val tileTop = 1300f
    for (row in 0 until 2) {
        for (col in -1 until 26) {
            val px = 70f + col * 40f + (if (row == 1) 20f else 0f)
            val py = 1330f + row * 20f
            val arc = Path().apply {
                addArc(
                    androidx.compose.ui.geometry.Rect(px, py - 22f, px + 44f, py + 22f),
                    180f, 180f,
                )
            }
            drawPath(arc, if (row == 1) Color(0xFF3A2A20) else Color(0xFF2A1608))
            drawArc(
                color = Color(0xFF6B5238),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(px, py - 22f),
                size = Size(44f, 44f),
                style = Stroke(width = 2f),
            )
        }
    }

    // 页脚
    drawRect(Color(0xFF1C1008), Offset(70f, 1392f), Size(CARD_WIDTH - 140f, 78f))
    canvas.drawText(style.footer, CARD_WIDTH / 2f, 1428f, textPaint(28f, Color(0xFFE8C26A)))
    canvas.drawText(style.footerAuthor, CARD_WIDTH / 2f, 1457f, textPaint(22f, gold))
}

/** Greedy line breaking that avoids starting a line with closing punctuation. */
private fun wrapText(paint: Paint, text: String, maxWidth: Float): List<String> {
    if (text.isEmpty()) return emptyList()
    val noLeading = "。，、！？；：）》”’…」"
    val out = mutableListOf<String>()
    var line = StringBuilder()
    for (ch in text) {
        val candidate = line.toString() + ch
        if (paint.measureText(candidate) > maxWidth && line.isNotEmpty() && ch !in noLeading) {
            out += line.toString()
            line = StringBuilder().append(ch)
        } else {
            line.append(ch)
        }
    }
    if (line.isNotEmpty()) out += line.toString()
    return out
}

/* ------------------------------------------------------------------------------------------- */
/* Saving and sharing                                                                            */
/* ------------------------------------------------------------------------------------------- */

private fun saveToGallery(context: Context, bitmap: Bitmap, fileName: String): Boolean = runCatching {
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/七十二家房客")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        ?: return@runCatching false

    resolver.openOutputStream(uri).use { stream ->
        if (stream == null) return@runCatching false
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
    }
    true
}.getOrDefault(false)

private fun shareBitmap(context: Context, bitmap: Bitmap, fileName: String, title: String) {
    runCatching {
        val dir = java.io.File(context.cacheDir, "share").apply { mkdirs() }
        val file = java.io.File(dir, fileName)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri as android.os.Parcelable)
            putExtra(Intent.EXTRA_TITLE, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "分享").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
