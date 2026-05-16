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

/** Calendar.DAY_OF_WEEK (1=Sun) → 周一=0 … 周日=6 */
private fun monDow(dow: Int) = (dow + 5) % 7

private val WEEK_LABELS = listOf("一", "二", "三", "四", "五", "六", "日")

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
            // 标题 + 月份切换
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
                        ) { Icon(Icons.Default.ChevronLeft, "上月", modifier = Modifier.size(20.dp)) }
                        Text(
                            months.getOrNull(currentMonthIndex)?.label ?: "",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        IconButton(
                            onClick = { if (currentMonthIndex < months.size - 1) currentMonthIndex++ },
                            modifier = Modifier.size(28.dp),
                            enabled = currentMonthIndex < months.size - 1
                        ) { Icon(Icons.Default.ChevronRight, "下月", modifier = Modifier.size(20.dp)) }
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
            val sorted = nonZero.sorted()
            val p25 = if (sorted.size >= 4) sorted[(sorted.size * 0.25).toInt()] else maxVal * 0.25
            val p50 = if (sorted.size >= 4) sorted[(sorted.size * 0.5).toInt()] else maxVal * 0.5
            val p75 = if (sorted.size >= 4) sorted[(sorted.size * 0.75).toInt()] else maxVal * 0.75

            val emptyColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            fun heatColor(v: Double) = when {
                v <= 0 -> emptyColor
                v <= p25 -> Color(0xFFC8E6C9)
                v <= p50 -> Color(0xFF81C784)
                v <= p75 -> Color(0xFF4CAF50)
                else -> Color(0xFF2E7D32)
            }

            val cell = 14.dp
            val gap = 3.dp

            // 按周分组：每周 7 天（周一~周日）
            val startDow = monDow(days.first().dayOfWeek) // 本月1号是周几
            val weeks = mutableListOf<List<HeatmapDay?>>()
            var week = mutableListOf<HeatmapDay?>()
            repeat(startDow) { week.add(null) } // 第一周前补空白
            for (day in days) {
                week.add(day)
                if (week.size == 7) { weeks.add(week.toList()); week = mutableListOf() }
            }
            if (week.isNotEmpty()) {
                repeat(7 - week.size) { week.add(null) } // 最后一周后补空白
                weeks.add(week.toList())
            }

            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                Column {
                    // 顶部星期标签
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        WEEK_LABELS.forEachIndexed { i, label ->
                            Box(modifier = Modifier.size(cell), contentAlignment = Alignment.Center) {
                                Text(label, fontSize = 9.sp,
                                    color = if (i == 6) Color(0xFFE57373)
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center)
                            }
                            if (i < 6) Spacer(modifier = Modifier.width(gap))
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))

                    // 每周一行
                    weeks.forEach { weekDays ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            weekDays.forEachIndexed { i, day ->
                                if (day != null) {
                                    val v = if (isChapterBased) day.chaptersRead else day.pagesRead
                                    Box(modifier = Modifier.size(cell).padding(1.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(heatColor(v))
                                        .clickable { selectedDay = day })
                                } else {
                                    Spacer(modifier = Modifier.size(cell))
                                }
                                if (i < 6) Spacer(modifier = Modifier.width(gap))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 图例
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically) {
                Text("少", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(4.dp))
                listOf(emptyColor, Color(0xFFC8E6C9), Color(0xFF81C784), Color(0xFF4CAF50), Color(0xFF2E7D32)).forEach {
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(it))
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
            text = { Text(if (value > 0) "阅读了 $value $unit" else "当天没有阅读记录") },
            confirmButton = { TextButton(onClick = { selectedDay = null }) { Text("关闭") } }
        )
    }
}
