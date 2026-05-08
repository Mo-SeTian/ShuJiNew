package com.readtrack

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
import com.readtrack.data.local.PreferencesManager
import com.readtrack.presentation.ui.MainNavigation
import com.readtrack.presentation.ui.settings.UpdateDialog
import com.readtrack.presentation.theme.ReadTrackTheme
import com.readtrack.presentation.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    @Inject
    lateinit var settingsViewModel: SettingsViewModel

    private var hasCheckedUpdateOnResume = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val themeMode by preferencesManager.themeMode.collectAsState(initial = com.readtrack.data.local.ThemeMode.SYSTEM)
            val uiState by settingsViewModel.uiState.collectAsState()
            var showUpdateDialog by remember { mutableStateOf(false) }

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

            ReadTrackTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!hasCheckedUpdateOnResume) {
            settingsViewModel.checkForUpdate()
            hasCheckedUpdateOnResume = true
        }
    }
}
