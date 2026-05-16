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

            // 计算颜色等级
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

            // 星期头
            val weekLabels = listOf("日", "一", "二", "三", "四", "五", "六")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.width(24.dp))
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState())
                ) {
                    days.forEach { day ->
                        Box(modifier = Modifier.size(16.dp), contentAlignment = Alignment.Center) {
                            Text(
                                weekLabels.getOrElse(day.dayOfWeek - 1) { "" },
                                fontSize = 8.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(modifier = Modifier.width(2.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))

            // 热力图网格
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                days.forEach { day ->
                    val value = if (isChapterBased) day.chaptersRead else day.pagesRead
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .padding(1.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(heatColor(value))
                            .clickable { selectedDay = day }
                    )
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
