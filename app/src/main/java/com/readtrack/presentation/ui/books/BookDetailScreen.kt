package com.readtrack.presentation.ui.books

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.readtrack.presentation.viewmodel.TrendPoint
import com.readtrack.presentation.ui.components.BookCover
import com.readtrack.presentation.ui.components.BookCoverQuality
import com.readtrack.data.local.entity.BookEntity
import com.readtrack.data.local.entity.ReadingRecordEntity
import com.readtrack.data.local.entity.RecordType
import com.readtrack.domain.model.BookStatus
import com.readtrack.presentation.ui.components.statusColorOf
import com.readtrack.presentation.ui.components.statusLabelOf
import com.readtrack.presentation.viewmodel.BookDetailViewModel
import com.readtrack.domain.model.ProgressType
import com.readtrack.presentation.ui.booklist.AddToBookListDialog
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    bookId: Long,
    onNavigateBack: () -> Unit,
    onEditBook: () -> Unit,
    viewModel: BookDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAddRecordDialog by remember { mutableStateOf(false) }
    var showEditRecordDialog by remember { mutableStateOf(false) }
    var showAddToBookListDialog by remember { mutableStateOf(false) }
    var recordToEdit by remember { mutableStateOf<ReadingRecordEntity?>(null) }
    var recordToDelete by remember { mutableStateOf<ReadingRecordEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("书籍详情", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onEditBook) {
                        Icon(Icons.Default.Edit, contentDescription = "编辑")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                    }
                    IconButton(onClick = { showAddToBookListDialog = true }) {
                        Icon(Icons.Default.PlaylistAdd, contentDescription = "加入书单")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            uiState.book?.let { book ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(4.dp)) }
                    
                    // 书籍头部 - 简洁风格
                    item {
                        BookInfoCard(book = book)
                    }
                    
                    // 进度卡片
                    item {
                        ProgressInfoCard(book = book)
                    }

                    // 阅读趋势图（至少2个数据点才显示）
                    if (uiState.trendData.size >= 2) {
                        item {
                            ReadingTrendCard(
                                trendData = uiState.trendData,
                                isChapterBased = book.progressType == ProgressType.CHAPTER
                            )
                        }
                    }

                    // 状态选择
                    item {
                        StatusCard(book = book, onStatusChange = { viewModel.updateStatus(it) })
                    }

                    // 评分
                    item {
                        RatingCard(
                            rating = book.rating,
                            onRatingChange = { viewModel.updateRating(it) }
                        )
                    }
                    
                    // 更新进度按钮
                    item {
                        Button(
                            onClick = { showAddRecordDialog = true },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            enabled = book.status == BookStatus.READING,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("更新阅读进度", fontWeight = FontWeight.Bold)
                        }
                        if (book.status != BookStatus.READING) {
                            Text(
                                "💡 将状态改为在读后可记录阅读进度",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                    
                    // 阅读记录标题
                    item {
                        Text(
                            "阅读记录",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    
                    // 阅读记录列表
                    if (uiState.readingRecords.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("📖", style = MaterialTheme.typography.displaySmall)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("暂无阅读记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    } else {
                        val isChapterBased = book.progressType == ProgressType.CHAPTER
                        items(
                            items = uiState.readingRecords,
                            key = { it.id }
                        ) { record ->
                            ReadingRecordRow(
                                record = record,
                                isChapterBased = isChapterBased,
                                onEdit = { recordToEdit = record; showEditRecordDialog = true },
                                onDelete = { recordToDelete = record }
                            )
                        }
                    }
                    
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }

    // 加入书单
    if (showAddToBookListDialog) {
        uiState.book?.let { book ->
            AddToBookListDialog(
                bookIds = listOf(book.id),
                onDismiss = { showAddToBookListDialog = false }
            )
        }
    }

    // 删除书籍确认
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除书籍", fontWeight = FontWeight.Bold) },
            text = { Text("确定要删除这本书吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteBook(); showDeleteDialog = false; onNavigateBack() }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("取消") } }
        )
    }

    // 删除阅读记录确认
    recordToDelete?.let { record ->
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            title = { Text("删除记录", fontWeight = FontWeight.Bold) },
            text = { Text("确定要删除这条阅读记录吗？书籍进度将重新计算。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteReadingRecord(record)
                    recordToDelete = null
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { recordToDelete = null }) { Text("取消") } }
        )
    }

    // 编辑阅读记录对话框
    if (showEditRecordDialog && recordToEdit != null && uiState.book != null) {
        val book = uiState.book!!
        val record = recordToEdit!!
        val isChapterBased = book.progressType == ProgressType.CHAPTER
        var inputText by remember { mutableStateOf("") }
        var noteText by remember { mutableStateOf(record.note ?: "") }
        var isIncrement by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showEditRecordDialog = false; recordToEdit = null },
            title = { Text("编辑阅读记录", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Text(
                            "原始记录：${if (isChapterBased) record.chaptersRead ?: 0 else record.pagesRead.toInt()} ${if (isChapterBased) "章" else "页"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = isIncrement,
                            onClick = { isIncrement = true },
                            label = { Text("本次读了") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = !isIncrement,
                            onClick = { isIncrement = false },
                            label = { Text("本次到") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = if (isChapterBased) it.filter { c -> c.isDigit() } else it.filter { c -> c.isDigit() || c == '.' } },
                        label = {
                            Text(
                                if (isChapterBased)
                                    if (isIncrement) "阅读章节数" else "章节号"
                                else
                                    if (isIncrement) "阅读页数" else "页码"
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = if (isChapterBased) KeyboardType.Number else KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("备注（可选）") },
                        placeholder = { Text("添加备注...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 2
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val updatedRecord = if (isChapterBased) {
                        val chapters = inputText.toIntOrNull() ?: 0
                        val toChapter = if (isIncrement) (record.fromPage.toInt() + chapters).coerceAtMost(book.totalChapters ?: 0) else inputText.toIntOrNull()?.coerceIn(0, book.totalChapters ?: 0) ?: record.toPage.toInt()
                        val pagesActuallyRead = if (isIncrement) chapters.toDouble() else (toChapter - record.fromPage.toInt()).coerceAtLeast(0).toDouble()
                        record.copy(
                            pagesRead = pagesActuallyRead,
                            toPage = toChapter.toDouble(),
                            note = noteText.takeIf { it.isNotBlank() }
                        )
                    } else {
                        val pages = inputText.toDoubleOrNull() ?: 0.0
                        val toPage = if (isIncrement) (record.fromPage + pages).coerceAtMost(book.totalPages) else inputText.toDoubleOrNull()?.coerceIn(0.0, book.totalPages) ?: record.toPage
                        val pagesActuallyRead = if (isIncrement) pages else (toPage - record.fromPage).coerceAtLeast(0.0)
                        record.copy(
                            pagesRead = pagesActuallyRead,
                            toPage = toPage,
                            note = noteText.takeIf { it.isNotBlank() }
                        )
                    }
                    viewModel.updateReadingRecord(updatedRecord)
                    showEditRecordDialog = false
                    recordToEdit = null
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showEditRecordDialog = false; recordToEdit = null }) { Text("取消") } }
        )
    }

    // 更新进度对话框
    if (showAddRecordDialog && uiState.book != null) {
        val book = uiState.book!!
        var inputText by remember { mutableStateOf("") }
        var isIncrement by remember { mutableStateOf(true) } // true=增量模式，false=直接模式
        val isChapterBased = book.progressType == ProgressType.CHAPTER
        
        AlertDialog(
            onDismissRequest = { showAddRecordDialog = false },
            title = { Text("更新阅读进度", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    // 模式切换
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = isIncrement,
                            onClick = { isIncrement = true },
                            label = { Text("本次读了") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = !isIncrement,
                            onClick = { isIncrement = false },
                            label = { Text("本次到") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = if (isChapterBased) it.filter { c -> c.isDigit() } else it.filter { c -> c.isDigit() || c == '.' } },
                        label = { 
                            Text(
                                if (isChapterBased) 
                                    if (isIncrement) "阅读章节数" else "章节号" 
                                else 
                                    if (isIncrement) "阅读页数" else "页码"
                            ) 
                        },
                        placeholder = { 
                            Text(
                                if (isChapterBased)
                                    if (isIncrement) "输入本次阅读章节数" else "输入目标章节号"
                                else
                                    if (isIncrement) "输入本次阅读页数" else "输入目标页码"
                            ) 
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = if (isChapterBased) KeyboardType.Number else KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (isChapterBased) 
                            if (isIncrement) "当前：第 ${book.currentChapter}/${book.totalChapters ?: 0} 章"
                            else "将更新到第 X 章"
                        else 
                            if (isIncrement) "当前：第 ${book.currentPage.toInt()} / ${book.totalPages.toInt()} 页"
                            else "将更新到第 X 页",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (isChapterBased) {
                            val chapters = inputText.toIntOrNull() ?: 0
                            if (chapters > 0) { viewModel.addChapterProgress(chapters, isIncrement); showAddRecordDialog = false }
                        } else {
                            val pages = inputText.toDoubleOrNull() ?: 0.0
                            if (pages > 0) { viewModel.addReadingRecord(pages, isIncrement); showAddRecordDialog = false }
                        }
                    }
                ) { Text("确认") }
            },
            dismissButton = { TextButton(onClick = { showAddRecordDialog = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun BookInfoCard(book: BookEntity) {
    val statusColor = remember(book.status) { statusColorOf(book.status) }
    val primaryColor = MaterialTheme.colorScheme.primary
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 封面
            BookCover(
                coverPath = book.coverPath,
                contentDescription = book.title,
                modifier = Modifier
                    .width(100.dp)
                    .height(150.dp),
                requestSize = DpSize(200.dp, 300.dp),
                quality = BookCoverQuality.PREVIEW
            )
            
            // 信息
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    book.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
                
                if (!book.author.isNullOrBlank()) {
                    Text(
                        "作者：${book.author}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (book.totalPages > 0 || book.totalChapters != null) {
                    val info = if (book.progressType == ProgressType.CHAPTER) {
                        "共 ${book.totalChapters ?: 0} 章"
                    } else {
                        "共 ${book.totalPages.toInt()} 页"
                    }
                    Text(
                        info,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // 状态标签
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        statusLabelOf(book.status),
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressInfoCard(book: BookEntity) {
    val isChapterBased = book.progressType == ProgressType.CHAPTER
    val statusColor = remember(book.status) { statusColorOf(book.status) }

    val progressData = remember(isChapterBased, book.totalChapters, book.totalPages, book.currentChapter, book.currentPage) {
        val total = if (isChapterBased) (book.totalChapters ?: 0).toDouble() else book.totalPages
        val current = if (isChapterBased) book.currentChapter.toDouble() else book.currentPage
        val progress = if (total > 0) (current / total).coerceIn(0.0, 1.0).toFloat() else 0f
        val progressPercent = (progress * 100).toInt()
        ProgressData(total = total, current = current, progress = progress, progressPercent = progressPercent)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "阅读进度",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
                Text(
                    "${progressData.progressPercent}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { progressData.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = statusColor,
                trackColor = statusColor.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    if (isChapterBased) "第 ${progressData.current.toInt()} 章" else "第 ${progressData.current.toInt()} 页",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    if (isChapterBased) "共 ${progressData.total.toInt()} 章" else "共 ${progressData.total.toInt()} 页",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ReadingTrendCard(
    trendData: List<TrendPoint>,
    isChapterBased: Boolean
) {
    if (trendData.size < 2) {
        // 数据点太少时不显示趋势图
        return
    }

    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val unit = if (isChapterBased) "章" else "页"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "阅读趋势",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                val lastPoint = trendData.last()
                Text(
                    "累计 ${lastPoint.cumulative.toInt()} $unit",
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 折线图
            ReadingLineChart(
                trendData = trendData,
                lineColor = lineColor,
                gridColor = gridColor,
                textColor = textColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 底部日期标签
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    trendData.first().dateLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor
                )
                Text(
                    trendData.last().dateLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor
                )
            }
        }
    }
}

@Composable
private fun ReadingLineChart(
    trendData: List<TrendPoint>,
    lineColor: androidx.compose.ui.graphics.Color,
    gridColor: androidx.compose.ui.graphics.Color,
    textColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    if (trendData.size < 2) return

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val paddingLeft = 0f
        val paddingRight = 0f
        val paddingTop = 8f
        val paddingBottom = 20f

        val chartWidth = size.width - paddingLeft - paddingRight
        val chartHeight = size.height - paddingTop - paddingBottom

        val maxValue = trendData.maxOf { it.cumulative }.coerceAtLeast(1.0)
        val minValue = 0.0

        val xStep = chartWidth / (trendData.size - 1)

        // 绘制水平网格线（3条）
        val gridCount = 3
        for (i in 0..gridCount) {
            val y = paddingTop + chartHeight * i / gridCount
            drawLine(
                color = gridColor,
                start = Offset(paddingLeft, y),
                end = Offset(paddingLeft + chartWidth, y),
                strokeWidth = 1f
            )
        }

        // 构建折线路径和填充路径
        val linePath = Path()
        val fillPath = Path()
        val points = trendData.mapIndexed { index, point ->
            val x = paddingLeft + index * xStep
            val normalizedY = ((point.cumulative - minValue) / (maxValue - minValue)).toFloat()
            val y = paddingTop + chartHeight * (1 - normalizedY)
            Offset(x, y)
        }

        points.forEachIndexed { index, point ->
            if (index == 0) {
                linePath.moveTo(point.x, point.y)
                fillPath.moveTo(point.x, paddingTop + chartHeight)
                fillPath.lineTo(point.x, point.y)
            } else {
                // 平滑曲线：用二次贝塞尔连接
                val prev = points[index - 1]
                val midX = (prev.x + point.x) / 2
                linePath.quadraticBezierTo(prev.x, prev.y, midX, (prev.y + point.y) / 2)
                fillPath.quadraticBezierTo(prev.x, prev.y, midX, (prev.y + point.y) / 2)
                if (index == points.size - 1) {
                    linePath.quadraticBezierTo(point.x, point.y, point.x, point.y)
                    fillPath.quadraticBezierTo(point.x, point.y, point.x, point.y)
                }
            }
        }
        fillPath.lineTo(points.last().x, paddingTop + chartHeight)
        fillPath.close()

        // 填充区域渐变
        drawPath(
            path = fillPath,
            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(
                    lineColor.copy(alpha = 0.3f),
                    lineColor.copy(alpha = 0.05f)
                ),
                startY = paddingTop,
                endY = paddingTop + chartHeight
            )
        )

        // 绘制折线
        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(width = 2.5f)
        )

        // 绘制数据点
        points.forEach { point ->
            drawCircle(
                color = lineColor,
                radius = 4f,
                center = point
            )
            drawCircle(
                color = androidx.compose.ui.graphics.Color.White,
                radius = 2f,
                center = point
            )
        }
    }
}

private data class ProgressData(
    val total: Double,
    val current: Double,
    val progress: Float,
    val progressPercent: Int
)

@Composable
private fun StatusCard(book: BookEntity, onStatusChange: (BookStatus) -> Unit) {
    val currentStatus = book.status
    val statusColor = remember(currentStatus) { statusColorOf(currentStatus) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "书籍状态",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BookStatus.entries.forEach { status ->
                    val isSelected = status == currentStatus
                    val color = statusColorOf(status)
                    
                    FilterChip(
                        selected = isSelected,
                        onClick = { onStatusChange(status) },
                        label = { Text(statusLabelOf(status), fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = color.copy(alpha = 0.2f),
                            selectedLabelColor = color
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = color.copy(alpha = 0.3f),
                            selectedBorderColor = color,
                            enabled = true,
                            selected = isSelected
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun RatingCard(
    rating: Float?,
    onRatingChange: (Float?) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "我的评分",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 星级评分
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (star in 1..5) {
                    val filled = rating != null && star <= rating
                    val halfFilled = rating != null && star - 0.5f <= rating && star > rating

                    IconButton(
                        onClick = {
                            // 点击同一颗星则清除评分
                            onRatingChange(if (rating == star.toFloat()) null else star.toFloat())
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (filled || halfFilled) Icons.Filled.Star else Icons.Filled.Star,
                            contentDescription = "$star 星",
                            tint = if (filled || halfFilled) Color(0xFFFFB400) else MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                if (rating != null) {
                    Text(
                        text = String.format("%.1f", rating),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFB400),
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
            }

            if (rating == null) {
                Text(
                    "点击星星打分",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                Text(
                    "点击已选星级可清除评分",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ReadingRecordRow(
    record: ReadingRecordEntity,
    isChapterBased: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }
    val isStatusRecord = record.recordType != RecordType.NORMAL

    val (statusColor, statusIcon) = when (record.recordType) {
        RecordType.STATUS_ADDED -> Color(0xFF4CAF50) to Icons.Default.Add
        RecordType.STATUS_READING -> Color(0xFFFF9800) to Icons.Default.PlayArrow
        RecordType.STATUS_FINISHED -> Color(0xFF2196F3) to Icons.Default.CheckCircle
        RecordType.STATUS_DROPPED -> Color(0xFFF44336) to Icons.Default.Delete
        else -> MaterialTheme.colorScheme.primary to Icons.Default.Add
    }
    val statusLabel: String = when (record.recordType) {
        RecordType.STATUS_ADDED -> "添加"
        RecordType.STATUS_READING -> "在读"
        RecordType.STATUS_FINISHED -> "已读"
        RecordType.STATUS_DROPPED -> "放弃"
        else -> ""
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    dateFormat.format(Date(record.date)),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (isStatusRecord) {
                    // 状态记录：显示备注
                    val noteText = record.note?.takeIf { it.isNotBlank() } ?: ""
                    if (noteText.isNotBlank()) {
                        Text(
                            noteText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        record.note?.takeIf { it.isNotBlank() } ?: "阅读了 ${if (isChapterBased) record.chaptersRead ?: 0 else record.pagesRead.toInt()} ${if (isChapterBased) "章" else "页"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (isStatusRecord) {
                    // 状态记录：彩色徽章
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = statusColor.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = statusIcon,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            "+${if (isChapterBased) record.chaptersRead ?: 0 else record.pagesRead.toInt()} ${if (isChapterBased) "章" else "页"}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "编辑",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}