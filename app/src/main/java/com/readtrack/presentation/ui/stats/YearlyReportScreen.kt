package com.readtrack.presentation.ui.stats

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.readtrack.domain.model.YearlyReportData
import com.readtrack.presentation.ui.components.MonthlyTrendChart
import com.readtrack.presentation.ui.share.SharePreviewDialog
import com.readtrack.presentation.viewmodel.YearlyReportViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearlyReportScreen(
    initialYear: Int,
    onNavigateBack: () -> Unit,
    viewModel: YearlyReportViewModel = hiltViewModel()
) {
    val report by viewModel.uiState.collectAsStateWithLifecycle()
    var showShareDialog by remember { mutableStateOf(false) }

    LaunchedEffect(initialYear) {
        viewModel.selectYear(initialYear)
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("年度阅读报告", fontWeight = FontWeight.Bold) },
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
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        val reportData = report
        if (reportData == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // 年份切换
                YearSelector(
                    currentYear = reportData.year,
                    availableYears = reportData.availableYears,
                    onYearSelected = { viewModel.selectYear(it) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Hero 区域
                HeroSection(reportData)

                Spacer(modifier = Modifier.height(20.dp))

                // 卡片1: 年度数据总结
                SummaryCard(reportData)
                Spacer(modifier = Modifier.height(12.dp))

                // 卡片2: 月度趋势图
                TrendCard(reportData)
                Spacer(modifier = Modifier.height(12.dp))

                // 卡片3: 趣味排行榜
                RankingCard(reportData)
                Spacer(modifier = Modifier.height(12.dp))

                // 卡片4: 阅读习惯画像
                HabitsCard(reportData)

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // 分享弹窗
    if (showShareDialog && report != null) {
        SharePreviewDialog(
            filename = "yearly_report_${report!!.year}",
            onDismiss = { showShareDialog = false },
            content = { YearlyReportCard(report!!) }
        )
    }
}

@Composable
private fun YearSelector(
    currentYear: Int,
    availableYears: List<Int>,
    onYearSelected: (Int) -> Unit
) {
    val currentIndex = availableYears.indexOf(currentYear).coerceAtLeast(0)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = {
                if (currentIndex < availableYears.size - 1) {
                    onYearSelected(availableYears[currentIndex + 1])
                }
            },
            enabled = currentIndex < availableYears.size - 1
        ) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "上一年")
        }

        Text(
            text = "${currentYear}年",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        IconButton(
            onClick = {
                if (currentIndex > 0) {
                    onYearSelected(availableYears[currentIndex - 1])
                }
            },
            enabled = currentIndex > 0
        ) {
            Icon(Icons.Default.ChevronRight, contentDescription = "下一年")
        }
    }
}

@Composable
private fun HeroSection(report: YearlyReportData) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(160.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF6366F1),
                        Color(0xFF8B5CF6),
                        Color(0xFFA78BFA)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${report.year} 年度阅读报告",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "你的阅读，有迹可循",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun SummaryCard(report: YearlyReportData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("年度数据总结", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                StatItem("共读书籍", "${report.totalBooksRead} 本", Modifier.weight(1f))
                StatItem("总阅读量", "${(report.totalPages + report.totalChapters).toInt()} 页/章", Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                StatItem("读完书籍", "${report.finishedBooks} 本", Modifier.weight(1f))
                StatItem("平均评分", "%.1f 星".format(report.averageRating), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TrendCard(report: YearlyReportData) {
    val merged = remember(report) {
        report.monthlyPages.zip(report.monthlyChapters) { p, c -> p + c }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("月度阅读趋势", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            MonthlyTrendChart(values = merged)
        }
    }
}

@Composable
private fun RankingCard(report: YearlyReportData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("趣味排行榜", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            report.favoriteBook?.let { book ->
                RankItem("最爱之书", book.title, "${book.rating ?: 0f} 星")
                Spacer(modifier = Modifier.height(8.dp))
            }
            report.thickestBook?.let { book ->
                val detail = if (book.progressType == com.readtrack.domain.model.ProgressType.CHAPTER)
                    "${book.totalChapters ?: 0} 章" else "${book.totalPages.toInt()} 页"
                RankItem("最厚之书", book.title, detail)
                Spacer(modifier = Modifier.height(8.dp))
            }
            report.longestBook?.let { book ->
                val dateStr = remember(book.createdAt) { YearlyReportViewModel.formatTimestamp(book.createdAt) }
                RankItem("陪伴最久之书", book.title, "从 $dateStr 至今")
            }
        }
    }
}

@Composable
private fun RankItem(label: String, title: String, detail: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
            Text(
                detail,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun HabitsCard(report: YearlyReportData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("阅读习惯画像", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                HabitItem("最爱类型", report.topGenre, Modifier.weight(1f))
                HabitItem("最长连续", "${report.maxStreakDays} 天", Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                HabitItem("活跃天数", "${report.activeDays} 天", Modifier.weight(1f))
                HabitItem("最爱月份", "${report.favoriteMonth + 1}月", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HabitItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ==================== 分享用卡片（纯Composable，不依赖ViewModel） ====================

@Composable
fun YearlyReportCard(report: YearlyReportData, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Hero
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${report.year} 年度阅读报告",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "共读 ${report.totalBooksRead} 本 · ${(report.totalPages + report.totalChapters).toInt()} 页/章",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 摘要
        Card(shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    StatItem("读完", "${report.finishedBooks} 本", Modifier.weight(1f))
                    StatItem("平均", "%.1f 星".format(report.averageRating), Modifier.weight(1f))
                    StatItem("活跃", "${report.activeDays} 天", Modifier.weight(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 最爱之书
        report.favoriteBook?.let { book ->
            Card(shape = RoundedCornerShape(12.dp)) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("年度最爱", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(book.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        if (book.author != null) {
                            Text(book.author, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFFFB400).copy(alpha = 0.2f)) {
                        Text(
                            "${book.rating ?: 0f} ★",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = Color(0xFFFFB400),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 月度趋势图
        Card(shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("月度趋势", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                val merged = report.monthlyPages.zip(report.monthlyChapters) { p, c -> p + c }
                MonthlyTrendChart(values = merged, modifier = Modifier.height(150.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 水印
        Text(
            "—— 书迹 App ——",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}
