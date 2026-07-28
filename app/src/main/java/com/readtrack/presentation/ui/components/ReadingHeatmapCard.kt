package com.readtrack.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

private fun monDow(dow: Int) = (dow + 5) % 7 // Calendar.DAY_OF_WEEK(1=Sun) → Mon=0..Sun=6
private val WEEK_LABELS = listOf("一", "二", "三", "四", "五", "六", "日")

@Composable
fun ReadingHeatmapCard(
    months: List<HeatmapMonth>,
    isChapterBased: Boolean,
    modifier: Modifier = Modifier
) {
    var selectedDay by remember { mutableStateOf<HeatmapDay?>(null) }
    // 默认显示最后有阅读记录的月份，而非当前月
    val defaultMonthIndex = remember(months) {
        if (months.isEmpty()) 0
        else months.indexOfLast { it.totalValue > 0 }.coerceAtLeast(0)
    }
    var currentMonthIndex by remember(months) { mutableIntStateOf(defaultMonthIndex) }

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
                            modifier = Modifier.size(32.dp),
                            enabled = currentMonthIndex > 0
                        ) { Icon(Icons.Default.ChevronLeft, "上月", modifier = Modifier.size(22.dp)) }
                        Text(
                            months.getOrNull(currentMonthIndex)?.label ?: "",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        IconButton(
                            onClick = { if (currentMonthIndex < months.size - 1) currentMonthIndex++ },
                            modifier = Modifier.size(32.dp),
                            enabled = currentMonthIndex < months.size - 1
                        ) { Icon(Icons.Default.ChevronRight, "下月", modifier = Modifier.size(22.dp)) }
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

            val borderColor = MaterialTheme.colorScheme.outlineVariant
            val emptyFill = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            fun cellColor(v: Double) = when {
                v <= 0 -> emptyFill
                v <= p25 -> Color(0xFFC8E6C9)
                v <= p50 -> Color(0xFF81C784)
                v <= p75 -> Color(0xFF4CAF50)
                else -> Color(0xFF2E7D32)
            }

            // 固定网格：7列（周一~周日），N行（周数）
            val startDow = monDow(days.first().dayOfWeek)
            val weeks = mutableListOf<List<HeatmapDay?>>()
            var week = mutableListOf<HeatmapDay?>()
            repeat(startDow) { week.add(null) }
            for (day in days) {
                week.add(day)
                if (week.size == 7) { weeks.add(week.toList()); week = mutableListOf() }
            }
            if (week.isNotEmpty()) {
                repeat(7 - week.size) { week.add(null) }
                weeks.add(week.toList())
            }

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    Column {
                        // 表头：星期标签
                        Row {
                            WEEK_LABELS.forEachIndexed { i, label ->
                                Box(
                                    modifier = Modifier.width(34.dp).height(20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(label, fontSize = 10.sp,
                                        color = if (i == 6) MaterialTheme.colorScheme.error
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center)
                                }
                                if (i < 6) Spacer(modifier = Modifier.width(2.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))

                        // 数据行
                        weeks.forEach { weekDays ->
                            Row {
                                weekDays.forEachIndexed { i, day ->
                                    if (day != null) {
                                        val v = if (isChapterBased) day.chaptersRead else day.pagesRead
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .padding(1.dp)
                                                .border(1.dp, borderColor, RoundedCornerShape(3.dp))
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(cellColor(v))
                                                .clickable { selectedDay = day },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "${day.dayOfMonth}",
                                                fontSize = 9.sp,
                                                color = if (v > 0 && v >= p50) Color.White
                                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.width(34.dp))
                                    }
                                    if (i < 6) Spacer(modifier = Modifier.width(2.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 图例
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically) {
                Text("少", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(4.dp))
                listOf(emptyFill, Color(0xFFC8E6C9), Color(0xFF81C784), Color(0xFF4CAF50), Color(0xFF2E7D32)).forEach {
                    Box(modifier = Modifier.size(12.dp).border(1.dp, borderColor, RoundedCornerShape(2.dp))
                        .clip(RoundedCornerShape(2.dp)).background(it))
                    Spacer(modifier = Modifier.width(2.dp))
                }
                Text("多", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // 点击日期后在下方展示当日书籍明细
            selectedDay?.let { day ->
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(10.dp))
                Text(
                    text = day.dateMs.toDateString("yyyy年M月d日") + " 阅读明细",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))

                if (day.bookBreakdowns.isEmpty()) {
                    Text(
                        "当天没有阅读记录",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    // 合计行
                    val totalParts = mutableListOf<String>()
                    if (day.pagesRead > 0) totalParts += "${day.pagesRead.toInt()} 页"
                    if (day.chaptersRead > 0) totalParts += "${day.chaptersRead.toInt()} 章"
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "合计",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            totalParts.joinToString(" + "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                    day.bookBreakdowns.forEach { book ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = book.bookTitle,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                                maxLines = 1
                            )
                            val parts = mutableListOf<String>()
                            if (book.pagesRead > 0) parts += "${book.pagesRead.toInt()} 页"
                            if (book.chaptersRead > 0) parts += "${book.chaptersRead.toInt()} 章"
                            Text(
                                text = parts.joinToString(" + "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
