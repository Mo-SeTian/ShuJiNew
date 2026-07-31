package com.readtrack

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.readtrack.data.local.PreferencesManager
import com.readtrack.domain.model.Badge
import com.readtrack.domain.repository.BadgeRepository
import com.readtrack.presentation.ui.MainNavigation
import com.readtrack.presentation.ui.badges.NewBadgeUnlockedDialog
import com.readtrack.presentation.ui.settings.UpdateDialog
import com.readtrack.presentation.ui.theme.ReadTrackTheme
import com.readtrack.presentation.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    @Inject
    lateinit var badgeRepository: BadgeRepository

    companion object {
        const val ACTION_OPEN_BOOK_DETAIL = "com.readtrack.ACTION_OPEN_BOOK_DETAIL"
        const val ACTION_OPEN_WIDGET_SETTINGS = "com.readtrack.ACTION_OPEN_WIDGET_SETTINGS"
        const val EXTRA_BOOK_ID = "extra_book_id"
    }

    /** 待打开的书籍 id，供小组件等外部入口触发导航（避免 onNewIntent 重复 startActivity） */
    private val pendingBookId = MutableStateFlow<Long?>(null)

    /** 待打开的小组件设置页标记 */
    private val pendingWidgetSettings = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        consumeLaunchIntent(intent)

        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val themeMode by preferencesManager.themeMode.collectAsState(initial = com.readtrack.data.local.ThemeMode.SYSTEM)
            val uiState by settingsViewModel.uiState.collectAsState()
            var showUpdateDialog by remember { mutableStateOf(false) }
            val pendingBookId by this.pendingBookId.collectAsState()
            val pendingWidgetSettings by this.pendingWidgetSettings.collectAsState()
            var pendingBadge by remember { mutableStateOf<Badge?>(null) }

            LaunchedEffect(Unit) {
                settingsViewModel.checkForUpdate()
                // 升级后首次启动，根据历史数据一次性核算徽章
                runCatching { badgeRepository.checkAndAward() }
            }

            LaunchedEffect(Unit) {
                badgeRepository.newBadgeEvents.collect { badge ->
                    pendingBadge = badge
                }
            }

            LaunchedEffect(uiState.updateResult) {
                if (uiState.updateResult != null && uiState.updateResult!!.hasUpdate) {
                    showUpdateDialog = true
                }
            }

            if (showUpdateDialog && uiState.updateResult != null) {
                val result = uiState.updateResult!!
                UpdateDialog(
                    result = result,
                    onDismiss = {
                        showUpdateDialog = false
                        settingsViewModel.clearUpdateResult()
                    },
                    onOpenDownload = {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(result.downloadUrl))
                        startActivity(intent)
                        showUpdateDialog = false
                        settingsViewModel.clearUpdateResult()
                    }
                )
            }

            pendingBadge?.let { badge ->
                NewBadgeUnlockedDialog(badge = badge, onDismiss = { pendingBadge = null })
            }

            ReadTrackTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation(
                        pendingBookId = pendingBookId,
                        onPendingBookIdConsumed = { this.pendingBookId.value = null },
                        pendingWidgetSettings = pendingWidgetSettings,
                        onPendingWidgetSettingsConsumed = { this.pendingWidgetSettings.value = false }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeLaunchIntent(intent)
    }

    /**
     * 解析外部入口 intent（小组件等），写入待导航状态。
     * 只更新状态、不再 startActivity，避免栈顶实例重复进入 onNewIntent 造成死循环。
     */
    private fun consumeLaunchIntent(intent: Intent) {
        when (intent.action) {
            ACTION_OPEN_BOOK_DETAIL -> {
                val bookId = intent.getLongExtra(EXTRA_BOOK_ID, -1L)
                if (bookId != -1L) {
                    pendingBookId.value = bookId
                }
            }
            ACTION_OPEN_WIDGET_SETTINGS -> pendingWidgetSettings.value = true
        }
    }
}
