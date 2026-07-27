package com.readtrack.presentation.ui.badges

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.readtrack.domain.model.Badge
import com.readtrack.domain.model.BadgeCategory
import com.readtrack.presentation.ui.theme.BadgeGold
import com.readtrack.presentation.ui.theme.BadgePurple
import com.readtrack.presentation.ui.theme.BadgePurpleDark
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
                title = { Text("我的徽章", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                windowInsets = WindowInsets(0, 0, 0, 0)
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item("hero") { BadgeHeroCard(earned = uiState.earnedCount, total = uiState.totalCount) }

            uiState.groups.forEach { (category, entries) ->
                val earnedInCat = entries.count { it.earnedAt != null }
                item("header_${category.name}") {
                    CategoryHeader(category, earned = earnedInCat, total = entries.size)
                }
                item("grid_${category.name}") {
                    BadgeGrid(entries = entries, onClick = { selectedEntry = it })
                }
            }

            item("spacer") { Spacer(Modifier.height(16.dp)) }
        }
    }

    selectedEntry?.let { entry ->
        BadgeDetailDialog(entry = entry, onDismiss = { selectedEntry = null })
    }
}

// ─── Hero ──────────────────────────────────────────────────────────

@Composable
private fun BadgeHeroCard(earned: Int, total: Int) {
    val pct = if (total > 0) earned.toFloat() / total else 0f
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(BadgePurple, BadgePurpleDark)
                    )
                )
                .padding(24.dp)
        ) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                // 大数字 + 标签
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$earned",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = " / $total",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text("已解锁徽章", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))

                Spacer(Modifier.height(16.dp))
                // 进度条
                LinearProgressIndicator(
                    progress = { pct },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = BadgeGold,
                    trackColor = Color.White.copy(alpha = 0.2f),
                    strokeCap = StrokeCap.Round
                )

                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (pct >= 1f) "全部达成！你是真正的阅读之王 👑" else "还差 ${total - earned} 枚，继续加油",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun CategoryHeader(category: BadgeCategory, earned: Int, total: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = category.displayName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        ) {
            Text(
                text = "$earned/$total",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

// ─── Grid ──────────────────────────────────────────────────────────

@Composable
private fun BadgeGrid(entries: List<BadgeUiEntry>, onClick: (BadgeUiEntry) -> Unit) {
    val rows = (entries.size + 2) / 3
    val height = (rows * 130).dp
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

// ─── Tile ──────────────────────────────────────────────────────────

@Composable
private fun BadgeTile(entry: BadgeUiEntry, onClick: () -> Unit) {
    val earned = entry.earnedAt != null
    val tierColor = Color(entry.badge.tier.colorHex)
    // 未获得时 scale down
    val scale = if (earned) 1f else 0.92f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 图标容器 — 根据等级不同尺寸和效果
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .then(
                    if (earned) {
                        Modifier
                            .background(Brush.radialGradient(
                                colors = listOf(tierColor.copy(alpha = 0.35f), tierColor.copy(alpha = 0.08f))
                            ))
                            .border(2.5.dp, tierColor, CircleShape)
                    } else {
                        Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                entry.badge.icon,
                fontSize = 36.sp,
                modifier = Modifier.alpha(if (earned) 1f else 0.3f)
            )
        }
        Spacer(Modifier.height(6.dp))
        // 标题
        Text(
            text = entry.badge.title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (earned) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 1,
            color = if (earned) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
        // 等级标签
        Text(
            text = entry.badge.tier.displayName,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = if (earned) tierColor else tierColor.copy(alpha = 0.4f)
        )
        // 进度条 (未获得时显示)
        if (!earned && entry.badge.threshold > 0) {
            Spacer(Modifier.height(3.dp))
            LinearProgressIndicator(
                progress = { entry.progressPercent },
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = tierColor.copy(alpha = 0.4f),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )
        }
    }
}

// ─── Detail Dialog ─────────────────────────────────────────────────

@Composable
private fun BadgeDetailDialog(entry: BadgeUiEntry, onDismiss: () -> Unit) {
    val earned = entry.earnedAt != null
    val tierColor = Color(entry.badge.tier.colorHex)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(if (earned) "知道了" else "继续努力")
            }
        },
        icon = {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(
                        if (earned) Brush.radialGradient(
                            colors = listOf(tierColor.copy(alpha = 0.6f), tierColor.copy(alpha = 0.1f))
                        ) else SolidColor(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    .border(3.dp, if (earned) tierColor else Color.Transparent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(entry.badge.icon, fontSize = 48.sp)
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                // 等级星星
                Row(horizontalArrangement = Arrangement.Center) {
                    repeat(entry.badge.tier.starCount) {
                        Text("★", color = tierColor, fontSize = 16.sp)
                    }
                    repeat(5 - entry.badge.tier.starCount) {
                        Text("☆", color = tierColor.copy(alpha = 0.3f), fontSize = 16.sp)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(entry.badge.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Text(
                    "${entry.badge.category.displayName} · ${entry.badge.tier.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = tierColor
                )
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(entry.badge.description, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)

                if (earned) {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = tierColor.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "「${entry.badge.quote}」",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = tierColor,
                            textAlign = TextAlign.Center,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "获得于 ${entry.earnedAt!!.toDateTimeString()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Spacer(Modifier.height(12.dp))
                    // 进度条
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "当前进度",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            entry.progressLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = tierColor
                        )
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { entry.progressPercent },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = tierColor,
                            trackColor = tierColor.copy(alpha = 0.1f),
                            strokeCap = StrokeCap.Round
                        )
                    }
                }
            }
        }
    )
}

// ─── Unlock Celebration Dialog ─────────────────────────────────────

@Composable
fun NewBadgeUnlockedDialog(badge: Badge, onDismiss: () -> Unit) {
    val tierColor = Color(badge.tier.colorHex)

    // 入场弹性动画
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val scaleAnim by animateFloatAsState(
        targetValue = if (visible) 1f else 0.3f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "unlock"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = tierColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("太棒了！", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        icon = {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(scaleAnim)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                tierColor.copy(alpha = 0.7f),
                                tierColor.copy(alpha = 0.15f)
                            )
                        )
                    )
                    .border(4.dp, tierColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(badge.icon, fontSize = 52.sp)
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                // 等级星星
                Row(horizontalArrangement = Arrangement.Center) {
                    repeat(5) { i ->
                        Text(
                            if (i < badge.tier.starCount) "★" else "☆",
                            color = tierColor,
                            fontSize = 18.sp
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "🎉 解锁新徽章 🎉",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = tierColor
                )
                Text(
                    badge.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${badge.category.displayName} · ${badge.tier.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = tierColor
                )
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(badge.description, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = tierColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        "「${badge.quote}」",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = tierColor,
                        textAlign = TextAlign.Center,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }
    )
}
