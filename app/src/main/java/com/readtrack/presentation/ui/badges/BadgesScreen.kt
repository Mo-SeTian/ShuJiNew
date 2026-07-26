package com.readtrack.presentation.ui.badges

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.readtrack.domain.model.Badge
import com.readtrack.domain.model.BadgeCategory
import com.readtrack.presentation.viewmodel.BadgeUiEntry
import com.readtrack.presentation.viewmodel.BadgesViewModel
import com.readtrack.util.toDateTimeString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadgesScreen(
    onNavigateBack: () -> Unit,
    viewModel: BadgesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedEntry by remember { mutableStateOf<BadgeUiEntry?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的徽章") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item("summary") {
                BadgeSummaryCard(earned = uiState.earnedCount, total = uiState.totalCount)
            }
            uiState.groups.forEach { (category, entries) ->
                item("header_${category.name}") {
                    Text(
                        text = "${category.displayName}  ${entries.count { it.earnedAt != null }}/${entries.size}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                item("grid_${category.name}") {
                    BadgeGrid(entries = entries, onClick = { selectedEntry = it })
                }
            }
        }
    }

    selectedEntry?.let { entry ->
        BadgeDetailDialog(entry = entry, onDismiss = { selectedEntry = null })
    }
}

@Composable
private fun BadgeSummaryCard(earned: Int, total: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "已解锁 $earned / $total",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { if (total > 0) earned.toFloat() / total else 0f },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
            )
        }
    }
}

@Composable
private fun BadgeGrid(entries: List<BadgeUiEntry>, onClick: (BadgeUiEntry) -> Unit) {
    // 使用固定高度让内部 Grid 与外层 LazyColumn 共存
    val rows = (entries.size + 2) / 3
    val height = (rows * 118).dp
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxWidth().height(height),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false
    ) {
        items(entries, key = { it.badge.id }) { entry ->
            BadgeTile(entry = entry, onClick = { onClick(entry) })
        }
    }
}

@Composable
private fun BadgeTile(entry: BadgeUiEntry, onClick: () -> Unit) {
    val earned = entry.earnedAt != null
    val tierColor = Color(entry.badge.tier.colorHex)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(if (earned) tierColor.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant)
                .border(2.dp, if (earned) tierColor else Color.Transparent, CircleShape)
                .alpha(if (earned) 1f else 0.35f),
            contentAlignment = Alignment.Center
        ) {
            Text(entry.badge.emoji, fontSize = 32.sp)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = entry.badge.title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            color = if (earned) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BadgeDetailDialog(entry: BadgeUiEntry, onDismiss: () -> Unit) {
    val earned = entry.earnedAt != null
    val tierColor = Color(entry.badge.tier.colorHex)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
        icon = {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(if (earned) tierColor.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant)
                    .border(3.dp, if (earned) tierColor else Color.Transparent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(entry.badge.emoji, fontSize = 44.sp)
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(entry.badge.title, fontWeight = FontWeight.Bold)
                Text(entry.badge.tier.displayName, style = MaterialTheme.typography.bodySmall, color = tierColor)
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(entry.badge.description, textAlign = TextAlign.Center)
                if (earned) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "获得时间：${entry.earnedAt!!.toDateTimeString()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "尚未解锁",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    )
}

/** 新徽章解锁弹窗，MainActivity 用。 */
@Composable
fun NewBadgeUnlockedDialog(badge: Badge, onDismiss: () -> Unit) {
    val tierColor = Color(badge.tier.colorHex)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("太棒了！") }
        },
        icon = {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(tierColor.copy(alpha = 0.35f))
                    .border(3.dp, tierColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(badge.emoji, fontSize = 48.sp)
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("解锁新徽章", fontWeight = FontWeight.Bold)
                Text(badge.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            }
        },
        text = {
            Text(badge.description, textAlign = TextAlign.Center)
        }
    )
}
