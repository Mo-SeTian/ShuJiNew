package com.readtrack.presentation.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.readtrack.domain.model.ProgressType
import com.readtrack.domain.model.YearlyReportData
import com.readtrack.presentation.ui.components.MonthlyTrendChart
import com.readtrack.presentation.ui.share.SharePreviewDialog
import com.readtrack.presentation.viewmodel.YearlyReportViewModel

private val HeroStart = Color(0xFF6366F1)
private val HeroMid = Color(0xFF8B5CF6)
private val HeroEnd = Color(0xFFEC4899)
private val Gold = Color(0xFFFFB400)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearlyReportScreen(
    initialYear: Int,
    onNavigateBack: () -> Unit,
    viewModel: YearlyReportViewModel = hiltViewModel()
) {
    val report by viewModel.uiState.collectAsStateWithLifecycle()
    var showShareDialog by remember { mutableStateOf(false) }

    LaunchedEffect(initialYear) { viewModel.selectYear(initialYear) }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("年度报告", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showShareDialog = true }) {
                        Icon(Icons.Default.Share, contentDescription = "分享")
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        val data = report
        if (data == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 40.dp)
            ) {
                YearSelector(data.year, data.availableYears) { viewModel.selectYear(it) }
                Spacer(Modifier.height(16.dp))
                HeroBanner(data)
                Spacer(Modifier.height(20.dp))
                StatGrid(data)
                Spacer(Modifier.height(16.dp))
                TrendCard(data)
                Spacer(Modifier.height(16.dp))
                RankingSection(data)
                Spacer(Modifier.height(16.dp))
                HabitsSection(data)
            }
        }
    }

    if (showShareDialog && report != null) {
        SharePreviewDialog(
            filename = "yearly_report_${report!!.year}",
            onDismiss = { showShareDialog = false },
            content = { YearlyReportCard(report!!) }
        )
    }
}

// ─── Year Selector ─────────────────────────────────────────────────

@Composable
private fun YearSelector(currentYear: Int, availableYears: List<Int>, onYearSelected: (Int) -> Unit) {
    val idx = availableYears.indexOf(currentYear).coerceAtLeast(0)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { if (idx < availableYears.size - 1) onYearSelected(availableYears[idx + 1]) },
            enabled = idx < availableYears.size - 1
        ) { Icon(Icons.Default.ChevronLeft, "上一年") }
        Text("${currentYear}年", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp))
        IconButton(
            onClick = { if (idx > 0) onYearSelected(availableYears[idx - 1]) },
            enabled = idx > 0
        ) { Icon(Icons.Default.ChevronRight, "下一年") }
    }
}

// ─── Hero ───────────────────────────────────────────────────────────

@Composable
private fun HeroBanner(report: YearlyReportData) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(200.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(HeroStart, HeroMid, HeroEnd)))
    ) {
        // 装饰圆 — 用 BoxScope.align 保证相对于容器定位
        Box(Modifier.size(100.dp).align(Alignment.TopStart).offset(x = (-20).dp, y = (-20).dp)
            .clip(CircleShape).background(Color.White.copy(alpha = 0.08f)))
        Box(Modifier.size(60.dp).align(Alignment.BottomEnd).offset(x = 30.dp, y = 30.dp)
            .clip(CircleShape).background(Color.White.copy(alpha = 0.06f)))

        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("${report.year}", style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.3f))
            Text("年度阅读报告", style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                HeroStat("${report.totalBooksRead}", "本书")
                HeroStat("${report.finishedBooks}", "本读完")
                HeroStat("${report.activeDays}", "天活跃")
            }
        }
    }
}

@Composable
private fun HeroStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold, color = Color.White)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f))
    }
}

// ─── Stat Grid ──────────────────────────────────────────────────────

@Composable
private fun StatGrid(report: YearlyReportData) {
    val total = (report.totalPages + report.totalChapters).toInt()
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatCard(Icons.Default.MenuBook, "总阅读量", "${if (total >= 10000) "${total / 10000}万" else "$total"}", Color(0xFF6366F1), Modifier.weight(1f))
        StatCard(Icons.Default.AutoStories, "新增书籍", "${report.newBooksCount} 本", Color(0xFF8B5CF6), Modifier.weight(1f))
        StatCard(Icons.Default.Star, "平均评分", "%.1f".format(report.averageRating), Gold, Modifier.weight(1f))
    }
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatCard(Icons.Default.EditNote, "记录次数", "${report.totalRecords} 次", Color(0xFFEC4899), Modifier.weight(1f))
        StatCard(Icons.Default.LocalFireDepartment, "最长连续", "${report.maxStreakDays} 天", Color(0xFFF97316), Modifier.weight(1f))
        StatCard(Icons.Default.TrendingUp, "完读率", "${if (report.totalBooksRead > 0) report.finishedBooks * 100 / report.totalBooksRead else 0}%", Color(0xFF22C55E), Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(icon: ImageVector, label: String, value: String, color: Color, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f))) {
        Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(34.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ─── Trend ──────────────────────────────────────────────────────────

@Composable
private fun TrendCard(report: YearlyReportData) {
    val merged = remember(report) { report.monthlyPages.zip(report.monthlyChapters) { p, c -> p + c } }
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("月度趋势", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("页/章", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
            MonthlyTrendChart(values = merged)
        }
    }
}

// ─── Rankings ───────────────────────────────────────────────────────

@Composable
private fun RankingSection(report: YearlyReportData) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(20.dp)) {
            Text("年度之最", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))

            report.favoriteBook?.let { book ->
                RankRow("🥇", "年度最爱", book.title,
                    "${book.rating ?: 0f} ★", Gold)
            }
            report.thickestBook?.let { book ->
                val detail = if (book.progressType == ProgressType.CHAPTER)
                    "${book.totalChapters ?: 0} 章" else "${book.totalPages.toInt()} 页"
                RankRow("📚", "最厚之书", book.title, detail, Color(0xFF6366F1))
            }
            report.fastestBook?.let { book ->
                RankRow("⚡", "读得最快", book.title, "高效阅读", Color(0xFF22C55E))
            }
            report.longestBook?.let { book ->
                val dateStr = YearlyReportViewModel.formatTimestamp(book.createdAt)
                RankRow("⏳", "陪伴最久", book.title, "从 $dateStr 至今", Color(0xFF8B5CF6))
            }
            report.favoriteAuthor?.let { author ->
                RankRow("✍️", "最爱作者", author, "阅读最多", Color(0xFFEC4899))
            }
        }
    }
}

@Composable
private fun RankRow(emoji: String, label: String, title: String, detail: String, color: Color) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 20.sp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.12f)) {
            Text(detail, Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ─── Habits ─────────────────────────────────────────────────────────

@Composable
private fun HabitsSection(report: YearlyReportData) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(20.dp)) {
            Text("阅读习惯", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HabitPill(Icons.Default.Category, "最爱类型", report.topGenre, Color(0xFF6366F1), Modifier.weight(1f))
                val dowNames = listOf("", "周日", "周一", "周二", "周三", "周四", "周五", "周六")
                HabitPill(Icons.Default.CalendarToday, "常读星期",
                    dowNames.getOrElse(report.favoriteDayOfWeek) { "-" }, Color(0xFF8B5CF6), Modifier.weight(1f))
                HabitPill(Icons.Default.Schedule, "最爱月份", "${report.favoriteMonth + 1}月", Color(0xFFF97316), Modifier.weight(1f))
            }

            // 状态分布
            if (report.statusDistribution.any { it.value > 0 }) {
                Spacer(Modifier.height(18.dp))
                Text("书籍状态", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                report.statusDistribution.forEach { (status, count) ->
                    if (count == 0) return@forEach
                    val pct = count.toFloat() / report.totalBooksRead.coerceAtLeast(1)
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(status, Modifier.width(48.dp), style = MaterialTheme.typography.bodySmall)
                        Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)) {
                            Box(Modifier.fillMaxWidth(pct).fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("$count 本", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun HabitPill(icon: ImageVector, label: String, value: String, color: Color, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f))) {
        Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ─── Share Card ─────────────────────────────────────────────────────

@Composable
fun YearlyReportCard(report: YearlyReportData, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Hero
        Box(
            Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(HeroStart, HeroMid, HeroEnd))),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${report.year} 年度阅读报告", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(4.dp))
                Text("共读 ${report.totalBooksRead} 本 · ${(report.totalPages + report.totalChapters).toInt()} 页/章",
                    style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
            }
        }

        Spacer(Modifier.height(12.dp))

        // 3 stats in a row
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ShareStat("读完", "${report.finishedBooks} 本", Modifier.weight(1f))
            ShareStat("记录", "${report.totalRecords} 次", Modifier.weight(1f))
            ShareStat("活跃", "${report.activeDays} 天", Modifier.weight(1f))
        }

        Spacer(Modifier.height(12.dp))

        // 最爱之书
        report.favoriteBook?.let { book ->
            Card(shape = RoundedCornerShape(12.dp)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("年度最爱", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(book.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        if (book.author != null) Text(book.author, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Surface(shape = RoundedCornerShape(8.dp), color = Gold.copy(alpha = 0.15f)) {
                        Text("${book.rating ?: 0f} ★", Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = Gold, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        report.favoriteAuthor?.let { author ->
            Spacer(Modifier.height(8.dp))
            Card(shape = RoundedCornerShape(12.dp)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("✍️", fontSize = 20.sp)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("最爱作者", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(author, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.padding(14.dp)) {
                Text("月度趋势", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                val merged = report.monthlyPages.zip(report.monthlyChapters) { p, c -> p + c }
                MonthlyTrendChart(values = merged, modifier = Modifier.height(130.dp))
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("—— 书迹 App ——", Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
    }
}

@Composable
private fun ShareStat(label: String, value: String, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(10.dp)) {
        Column(Modifier.fillMaxWidth().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
