package com.readtrack.presentation.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp

@Composable
fun MonthlyTrendChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0xFF6366F1),
    fillColor: Color = Color(0x336366F1),
    dotColor: Color = Color(0xFF6366F1),
    gridColor: Color = Color(0xFFE0E0E0),
    labelColor: Color = Color(0xFF9E9E9E)
) {
    val months = remember { listOf("1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月") }
    val maxValue = remember(values) { values.maxOrNull()?.coerceAtLeast(1f) ?: 1f }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        val leftPadding = 40f
        val bottomPadding = 30f
        val topPadding = 20f
        val rightPadding = 16f

        val chartWidth = size.width - leftPadding - rightPadding
        val chartHeight = size.height - topPadding - bottomPadding

        // 水平网格线
        val gridLines = 4
        for (i in 0..gridLines) {
            val y = topPadding + chartHeight * (1f - i.toFloat() / gridLines)
            drawLine(
                color = gridColor,
                start = Offset(leftPadding, y),
                end = Offset(size.width - rightPadding, y),
                strokeWidth = 1f
            )
            // 左侧刻度标签
            val labelValue = (maxValue * i / gridLines).toInt()
            drawContext.canvas.nativeCanvas.drawText(
                "$labelValue",
                leftPadding - 8f,
                y + 4f,
                android.graphics.Paint().apply {
                    color = labelColor.hashCode()
                    textSize = 24f
                    textAlign = android.graphics.Paint.Align.RIGHT
                    isAntiAlias = true
                }
            )
        }

        if (values.isEmpty() || maxValue <= 0f) return@Canvas

        // 数据点坐标
        val points = values.mapIndexed { index, value ->
            val x = leftPadding + chartWidth * index / 11f
            val y = topPadding + chartHeight * (1f - value / maxValue)
            Offset(x, y)
        }

        // 填充区域路径
        val fillPath = Path().apply {
            moveTo(points.first().x, topPadding + chartHeight)
            points.forEach { lineTo(it.x, it.y) }
            lineTo(points.last().x, topPadding + chartHeight)
            close()
        }
        drawPath(fillPath, fillColor)

        // 折线
        val linePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                lineTo(points[i].x, points[i].y)
            }
        }
        drawPath(
            linePath,
            color = lineColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // 数据点圆点
        points.forEach { point ->
            drawCircle(color = dotColor, radius = 5.dp.toPx(), center = point)
            drawCircle(color = Color.White, radius = 2.5.dp.toPx(), center = point)
        }

        // X轴标签
        val monthStep = if (values.size <= 12) 1 else 2
        months.forEachIndexed { index, month ->
            if (index % monthStep == 0) {
                val x = leftPadding + chartWidth * index / 11f
                drawContext.canvas.nativeCanvas.drawText(
                    month,
                    x,
                    size.height - 4f,
                    android.graphics.Paint().apply {
                        color = labelColor.hashCode()
                        textSize = 22f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                )
            }
        }
    }
}
