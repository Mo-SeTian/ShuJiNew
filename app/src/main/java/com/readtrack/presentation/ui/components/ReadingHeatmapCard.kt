package com.readtrack.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.readtrack.presentation.viewmodel.HeatmapDay
import com.readtrack.presentation.viewmodel.HeatmapMonth
import com.readtrack.util.toDateString

/**
 * dayOfWeek 从 Calendar.DAY_OF_WEEK (1=Sun..7=Sat) 转为周一=0..周日=6
 */
private fun HeatmapDay.mondayBasedDow(): Int = (dayOfWeek + 5) % 7

@Composable
fun ReadingHeatmapCard(
    months: List<HeatmapMonth>,
    isChapterBased: Boolean,
    modifier: Modifier = Modifier
) {
    var selectedDay by remember { mutableStateOf<HeatmapDay?>(null) }
    var currentMonthIndex by remember(months) { mutableIntStateOf(if (months.isNotEmpty()) months.size - 1 else 0) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("阅读热力图", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (months.size > 1) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { if (currentMonthIndex > 0) currentMonthIndex-- },
                            modifier = Modifier.size(28.dp),
                            enabled = currentMonthIndex > 0
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "上月", modifier = Modifier.size(20.dp))
                        }
                        Text(
                            months.getOrNull(currentMonthIndex)?.label ?: "",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        IconButton(
                            onClick = { if (currentMonthIndex < months.size - 1) currentMonthIndex++ },
                            modifier = Modifier.size(28.dp),
                            enabled = currentMonthIndex < months.size - 1
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "下月", modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val month = months.getOrNull(currentMonthIndex) ?: return@Column
            val days = month.days
            if (days.isEmpty()) return@Column

            // 颜色等级
            val nonZero = days.map { it.pagesRead + it.chaptersRead }.filter { it > 0 }
            val maxVal = nonZero.maxOrNull() ?: 1.0
            val p25 = if (nonZero.size >= 4) nonZero.sorted()[(nonZero.size * 0.25).toInt()] else maxVal * 0.25
            val p50 = if (nonZero.size >= 4) nonZero.sorted()[(nonZero.size * 0.5).toInt()] else maxVal * 0.5
            val p75 = if (nonZero.size >= 4) nonZero.sorted()[(nonZero.size * 0.75).toInt()] else maxVal * 0.75

            val baseColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            fun heatColor(value: Double): Color = when {
                value <= 0 -> baseColor
                value <= p25 -> Color(0xFFC8E6C9)
                value <= p50 -> Color(0xFF81C784)
                value <= p75 -> Color(0xFF4CAF50)
                else -> Color(0xFF2E7D32)
            }

            // 按周一=0..周日=6 构建网格：7列 × N行
            val cellSize = 14
            val gap = 2
            val weekLabels = listOf("一", "二", "三", "四", "五", "六", "日")
            val firstDay = days.first()
            val lastDay = days.last()
            val startDow = firstDay.mondayBasedDow() // 本月第一天是周几（周一=0）
            val endDow = lastDay.mondayBasedDow()

            // 补齐第一周前面空白天
            val paddedDays = mutableListOf<HeatmapDay?>()
            repeat(startDow) { paddedDays.add(null) }
            paddedDays.addAll(days)
            // 补齐最后一周后面空白天
            val trailing = (7 - (paddedDays.size % 7)) % 7
            repeat(trailing) { paddedDays.add(null) }

            val rows = paddedDays.chunked(7)

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                Column {
                    // 星期头
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // 左侧星期标签列占位
                        Spacer(modifier = Modifier.width(16.dp))
                        // 月份中的周序列（显示星期几在一个占位列中）
                        // 这里是7列，不需要单独标注星期
                    }
                    Spacer(modifier = Modifier.height(4.dp))

                    rows.forEach { weekDays ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // 不需要左侧星期标签——统一放顶部
                            weekDays.forEach { day ->
                                val value = if (day != null) {
                                    if (isChapterBased) day.chaptersRead else day.pagesRead
                                } else -1.0

                                if (day != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(cellSize.dp)
                                            .padding(1.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(heatColor(value))
                                            .clickable { selectedDay = day }
                                    )
                                } else {
                                    Spacer(modifier = Modifier.size(cellSize.dp))
                                }
                                Spacer(modifier = Modifier.width(gap.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 星期标签行（放在网格下方）
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        weekLabels.forEachIndexed { index, label ->
                            Box(
                                modifier = Modifier.size(cellSize.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    fontSize = 9.sp,
                                    color = if (index == 6) Color(0xFFE57373) // 周日红色
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center
                                )
                            }
                            if (index < 6) Spacer(modifier = Modifier.width(gap.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 图例
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("少", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(4.dp))
                listOf(baseColor, Color(0xFFC8E6C9), Color(0xFF81C784), Color(0xFF4CAF50), Color(0xFF2E7D32)).forEach { c ->
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(c))
                    Spacer(modifier = Modifier.width(2.dp))
                }
                Text("多", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    // 点击弹窗
    selectedDay?.let { day ->
        val unit = if (isChapterBased) "章" else "页"
        val value = if (isChapterBased) day.chaptersRead.toInt() else day.pagesRead.toInt()
        AlertDialog(
            onDismissRequest = { selectedDay = null },
            title = { Text(day.dateMs.toDateString("yyyy年M月d日"), fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    if (value > 0) "阅读了 $value $unit"
                    else "当天没有阅读记录"
                )
            },
            confirmButton = {
                TextButton(onClick = { selectedDay = null }) { Text("关闭") }
            }
        )
    }
}
