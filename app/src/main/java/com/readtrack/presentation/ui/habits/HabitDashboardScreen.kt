package com.readtrack.presentation.ui.habits

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.readtrack.domain.model.DayOfWeekActivity
import com.readtrack.domain.model.HabitDashboardData
import com.readtrack.domain.model.ReadingSpeed
import com.readtrack.domain.model.TimeSlotDistribution
import com.readtrack.domain.model.TypePreference
import com.readtrack.presentation.ui.components.EmptyStateView
import com.readtrack.presentation.viewmodel.HabitDashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDashboardScreen(
    onNavigateBack: () -> Unit,
    viewModel: HabitDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("阅读习惯", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.data == null) {
            EmptyStateView(
                icon = Icons.Default.TrendingUp,
                title = "暂无阅读数据",
                description = "累积一定阅读记录后，这里会展示你的阅读习惯画像",
                modifier = Modifier.padding(padding)
            )
        } else {
            val data = uiState.data!!
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item("hero") { HeroSummaryCard(data) }
                item("time") { TimeDistributionRingChart(data.timeDistribution) }
                item("week") { WeeklyActivityBarChart(data.weeklyActivity) }
                item("type") { TypePreferenceBars(data.typePreference) }
                item("speed") { ReadingSpeedCards(data.readingSpeed) }
            }
        }
    }
}

// ─── Hero Summary ────────────────────────────────────────────────

@Composable
private fun HeroSummaryCard(data: HabitDashboardData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF8B5CF6).copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF8B5CF6).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = Color(0xFF8B5CF6),
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("阅读习惯画像", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    StatPill("${data.totalActiveDays}", "活跃天数")
                    StatPill("${data.streakDays}", "连续打卡")
                    StatPill(data.favoriteTimeLabel, "阅读节奏")
                }
            }
        }
    }
}

@Composable
private fun StatPill(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold, color = Color(0xFF7C3AED))
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ─── Ring Chart ───────────────────────────────────────────────────

@Composable
private fun TimeDistributionRingChart(distribution: List<TimeSlotDistribution>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("阅读时段分布", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            val dominant = distribution.maxByOrNull { it.recordCount }

            // 环形图 + 中心文字叠加
            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val ringThickness = 24.dp.toPx()
                    val canvasSize = minOf(size.width, size.height)
                    // 环外缘留 ringThickness 边距防止裁切
                    val safeDiameter = canvasSize - ringThickness
                    val topLeft = Offset(
                        (size.width - safeDiameter) / 2f,
                        (size.height - safeDiameter) / 2f
                    )
                    val arcSize = Size(safeDiameter, safeDiameter)
                    var startAngle = -90f

                    distribution.forEach { item ->
                        if (item.recordCount == 0) return@forEach
                        val sweep = item.percentage * 360f
                        if (sweep <= 0f) return@forEach
                        drawArc(
                            color = Color(item.slot.colorHex),
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = ringThickness, cap = StrokeCap.Butt)
                        )
                        startAngle += sweep
                    }
                }
                // 用 Compose Text 叠在 Canvas 上方，避免 nativeCanvas 基线偏移问题
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${dominant?.recordCount ?: 0}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7C3AED)
                    )
                    Text(
                        text = "次阅读",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B7280)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            // Legend — 两行各 3 个，避免单行溢出，0% 的隐藏
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val visible = distribution.filter { it.recordCount > 0 }
                val rows = visible.chunked(3)
                rows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { item ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(8.dp).clip(CircleShape)
                                        .background(Color(item.slot.colorHex))
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "${item.slot.label} ${(item.percentage * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Weekly Bar Chart ─────────────────────────────────────────────

@Composable
private fun WeeklyActivityBarChart(weekly: List<DayOfWeekActivity>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("每周活跃度", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            val maxDays = weekly.maxOfOrNull { it.activeDays }?.coerceAtLeast(1) ?: 1

            // 固定高度确保柱状图基线一致（参照 WeeklyChartModern）
            Row(
                modifier = Modifier.fillMaxWidth().height(160.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                weekly.forEachIndexed { index, day ->
                    // 目标高度 0~100，直当 dp 用
                    val targetHeight = ((day.activeDays.toFloat() / maxDays) * 100f).coerceIn(4f, 100f)

                    var animated by remember(day.dayIndex) { mutableFloatStateOf(0f) }
                    LaunchedEffect(targetHeight) { animated = targetHeight }
                    val animatedHeight by animateFloatAsState(
                        targetValue = animated,
                        animationSpec = tween(400, delayMillis = index * 40),
                        label = "bar"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (day.activeDays > 0) {
                            Text(
                                "${day.activeDays}天",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (day.isMostActive) Color(0xFF7C3AED)
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(animatedHeight.dp)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    if (day.isMostActive) Color(0xFF7C3AED)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            day.dayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (day.isMostActive) FontWeight.Bold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ─── Type Preference ──────────────────────────────────────────────

@Composable
private fun TypePreferenceBars(types: List<TypePreference>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("类型偏好", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            val typeColor = mapOf(
                "NOVEL" to Color(0xFF2196F3),   // FinishedBlue
                "COMIC" to Color(0xFFFF9800),    // ReadingOrange
                "AUDIOBOOK" to Color(0xFF4CAF50) // WantToReadGreen
            )

            types.forEach { type ->
                val color = typeColor[type.bookType] ?: Color(0xFF9E9E9E)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        type.typeLabel, style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.width(56.dp)
                    )
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        LinearProgressIndicator(
                            progress = { type.percentage.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = color,
                            trackColor = color.copy(alpha = 0.15f),
                            strokeCap = StrokeCap.Round
                        )
                    }
                    Text(
                        "${(type.percentage * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(36.dp)
                    )
                    Text(
                        "${type.bookCount}本",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(32.dp)
                    )
                }
            }
        }
    }
}

// ─── Reading Speed Cards ──────────────────────────────────────────

@Composable
private fun ReadingSpeedCards(speed: ReadingSpeed) {
    Column {
        Text(
            "阅读特征", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SpeedCard(
                Icons.Default.MenuBook, "平均每次",
                String.format("%.1f 页", speed.avgPagesPerSession),
                modifier = Modifier.weight(1f)
            )
            SpeedCard(
                Icons.Default.NightsStay, "阅读节奏",
                speed.favoriteTimeLabel,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SpeedCard(
                Icons.Default.CalendarToday, "最活跃",
                speed.favoriteDayLabel,
                modifier = Modifier.weight(1f)
            )
            SpeedCard(
                Icons.Default.LocalFireDepartment, "章节/次",
                String.format("%.1f 章", speed.avgChaptersPerSession),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SpeedCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(2.dp))
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
