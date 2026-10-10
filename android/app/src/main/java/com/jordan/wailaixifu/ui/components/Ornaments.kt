package com.jordan.wailaixifu.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Banner ornaments drawn with plain canvas primitives, so the app carries no image assets.
 * 外来 uses the gold lattice of its original page, 传统 the 青砖灰瓦 masonry of a 西关大屋,
 * 现代 a flat wash.
 */
internal fun DrawScope.drawLattice(cell: Float, color: Color) {
    val stroke = 1.dp.toPx()
    var x = 0f
    while (x <= size.width) {
        drawLine(color, Offset(x, 0f), Offset(x, size.height), stroke)
        x += cell
    }
    var y = 0f
    while (y <= size.height) {
        drawLine(color, Offset(0f, y), Offset(size.width, y), stroke)
        y += cell
    }
}

/** Staggered masonry courses, as in the 青砖灰瓦 walls of a 西关大屋. */
internal fun DrawScope.drawMasonry(mortar: Color, rows: Float, jointOffsetRatio: Float = 0.5f) {
    val rowHeight = size.height / rows
    val stroke = 1.dp.toPx()

    var y = rowHeight
    while (y < size.height) {
        drawLine(mortar, Offset(0f, y), Offset(size.width, y), stroke)
        y += rowHeight
    }

    val jointSpacing = size.width / 3f
    var index = 0
    var courseY = 0f
    while (courseY < size.height) {
        val offset = if (index % 2 == 0) 0f else jointSpacing * jointOffsetRatio
        var x = offset
        while (x <= size.width) {
            val bottom = (courseY + rowHeight).coerceAtMost(size.height)
            drawLine(mortar, Offset(x, courseY), Offset(x, bottom), stroke)
            x += jointSpacing
        }
        courseY += rowHeight
        index += 1
    }
}

/** The 趟栊 sliding timber grille: evenly spaced bars with two rails. */
internal fun DrawScope.drawTimberGrille(barColor: Color, railColor: Color, barSpacing: Float) {
    val stroke = 2.dp.toPx()
    var x = barSpacing
    while (x < size.width) {
        drawLine(barColor, Offset(x, 0f), Offset(x, size.height), stroke)
        x += barSpacing
    }
    val railStroke = 1.dp.toPx()
    listOf(0.16f, 0.84f).forEach { ratio ->
        val y = size.height * ratio
        drawLine(railColor, Offset(0f, y), Offset(size.width, y), railStroke)
    }
}

/** A ring, used for the 印章 in the share card. */
internal fun DrawScope.strokeRing(color: Color, radius: Float, width: Float, center: Offset) {
    drawCircle(color = color, radius = radius, center = center, style = Stroke(width = width))
}
