package com.readtrack.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.readtrack.presentation.viewmodel.ReadingPeriod
import com.readtrack.util.getDaysBetween
import com.readtrack.util.toDateString

@Composable
fun ReadingTimelineCard(
    periods: List<ReadingPeriod>,
    isChapterBased: Boolean,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                Text("阅读时间线", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = onShareClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "分享",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (periods.isEmpty()) {
                Text("暂无阅读周期数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                periods.forEachIndexed { index, period ->
                    val days = getDaysBetween(period.startDate, period.endDate) + 1
                    val valuePerDay = if (isChapterBased) period.chaptersPerDay else period.pagesPerDay
                    val unit = if (isChapterBased) "章" else "页"

                    Row(modifier = Modifier.fillMaxWidth()) {
                        // 时间线：圆点 + 竖线，圆点与首行文字垂直居中
                        Box(
                            modifier = Modifier.width(24.dp).height(IntrinsicSize.Min),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            // 竖线：从圆点下方延伸到整行高度
                            if (index < periods.size - 1) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(2.dp)
                                        .background(MaterialTheme.colorScheme.outlineVariant)
                                )
                            }
                            // 圆点
                            Box(
                                modifier = Modifier
                                    .padding(top = 5.dp)
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // 内容
                        Column(modifier = Modifier.weight(1f)) {
                            val endDateText = if (period.isOpenEnded) "至今"
                                else period.endDate.toDateString("yyyy.MM.dd")
                            Text(
                                "${period.startDate.toDateString("yyyy.MM.dd")}(${period.startLabel}) — $endDateText${if (!period.isOpenEnded) "(${period.endLabel})" else ""}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = if (period.isOpenEnded) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text(
                                    "共 $days 天 · 活跃 ${period.activeDays} 天",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "日均 %.1f $unit".format(valuePerDay),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    if (index < periods.size - 1) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}
