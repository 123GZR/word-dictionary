package com.galaxy.worddictionary.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme

/**
 * Canvas 矢量图标：替代 emoji 实现（手表上渲染彩色 emoji 会触发字体回退扫描，
 * 列表滚动时卡顿）。所有图标在 24x24 设计空间内绘制，等比缩放到目标尺寸。
 * 使用 drawWithCache 缓存绘制指令，滚动/重组时直接复用。
 */
@Composable
internal fun DictionaryIcon(
    sizeDp: Dp = 24.dp,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    content: DrawScope.() -> Unit
) {
    Spacer(
        modifier = modifier
            .size(sizeDp)
            .drawWithCache {
                onDrawBehind {
                    val unit = size.minDimension / 24f
                    val offsetX = (size.width - size.minDimension) / 2f
                    val offsetY = (size.height - size.minDimension) / 2f
                    withTransform({
                        translate(offsetX, offsetY)
                        scale(scaleX = unit, scaleY = unit, pivot = Offset.Zero)
                    }) { content() }
                }
            }
    )
}

private val IconStroke = 2.2f

/** 返回（左箭头） */
@Composable
fun BackIcon(
    sizeDp: Dp = 24.dp,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    DictionaryIcon(sizeDp = sizeDp, modifier = modifier, color = color) {
        drawLine(color, Offset(15f, 5f), Offset(9f, 12f), IconStroke, StrokeCap.Round)
        drawLine(color, Offset(9f, 12f), Offset(15f, 19f), IconStroke, StrokeCap.Round)
    }
}

/** 搜索（放大镜） */
@Composable
fun SearchIcon(
    sizeDp: Dp = 24.dp,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    DictionaryIcon(sizeDp = sizeDp, modifier = modifier, color = color) {
        drawCircle(color, radius = 4.5f, center = Offset(10f, 10f), style = Stroke(IconStroke))
        drawLine(color, Offset(13.5f, 13.5f), Offset(19f, 19f), IconStroke, StrokeCap.Round)
    }
}

/** 词库（翻开的书） */
@Composable
fun BookIcon(
    sizeDp: Dp = 24.dp,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    DictionaryIcon(sizeDp = sizeDp, modifier = modifier, color = color) {
        val book = Path().apply {
            moveTo(12f, 5f)
            cubicTo(9f, 2.5f, 5.5f, 2f, 2f, 2f)
            lineTo(2f, 16f)
            cubicTo(5.5f, 16f, 9f, 17f, 12f, 19f)
            cubicTo(15f, 17f, 18.5f, 16f, 22f, 16f)
            lineTo(22f, 2f)
            cubicTo(18.5f, 2f, 15f, 2.5f, 12f, 5f)
            close()
        }
        drawPath(book, color)
        drawLine(color, Offset(12f, 5f), Offset(12f, 19f), IconStroke, StrokeCap.Round)
    }
}

/** 翻面/转表圈（环形箭头） */
@Composable
fun FlipIcon(
    sizeDp: Dp = 24.dp,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    DictionaryIcon(sizeDp = sizeDp, modifier = modifier, color = color) {
        val arc = Rect(Offset(3.5f, 3.5f), Size(17f, 17f))
        drawArc(
            color = color,
            startAngle = -45f,
            sweepAngle = 265f,
            useCenter = false,
            topLeft = arc.topLeft,
            size = arc.size,
            style = Stroke(IconStroke, cap = StrokeCap.Round)
        )
        // 箭头（弧线末端）
        val tip = Offset(20.5f, 3.5f)
        val head = Path().apply {
            moveTo(tip.x - 3f, tip.y + 1.5f)
            lineTo(tip.x, tip.y)
            lineTo(tip.x - 1.5f, tip.y + 3f)
            close()
        }
        drawPath(head, color)
    }
}

/** 认识（对勾） */
@Composable
fun CheckIcon(
    sizeDp: Dp = 24.dp,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.secondary
) {
    DictionaryIcon(sizeDp = sizeDp, modifier = modifier, color = color) {
        drawLine(color, Offset(4.5f, 12.5f), Offset(10f, 18f), IconStroke, StrokeCap.Round)
        drawLine(color, Offset(10f, 18f), Offset(19.5f, 6f), IconStroke, StrokeCap.Round)
    }
}

/** 不认识（叉号） */
@Composable
fun CrossIcon(
    sizeDp: Dp = 24.dp,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.tertiary
) {
    DictionaryIcon(sizeDp = sizeDp, modifier = modifier, color = color) {
        drawLine(color, Offset(6.5f, 6.5f), Offset(17.5f, 17.5f), IconStroke, StrokeCap.Round)
        drawLine(color, Offset(17.5f, 6.5f), Offset(6.5f, 17.5f), IconStroke, StrokeCap.Round)
    }
}

/** 梗概（三行列表） */
@Composable
fun SummaryIcon(
    sizeDp: Dp = 24.dp,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    DictionaryIcon(sizeDp = sizeDp, modifier = modifier, color = color) {
        drawLine(color, Offset(5f, 7f), Offset(19f, 7f), IconStroke, StrokeCap.Round)
        drawLine(color, Offset(5f, 12f), Offset(19f, 12f), IconStroke, StrokeCap.Round)
        drawLine(color, Offset(5f, 17f), Offset(19f, 17f), IconStroke, StrokeCap.Round)
    }
}
