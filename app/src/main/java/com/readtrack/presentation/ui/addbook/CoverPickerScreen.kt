package com.readtrack.presentation.ui.addbook

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.readtrack.presentation.viewmodel.AddBookViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoverPickerScreen(
    initialCoverUri: String?,
    viewModel: AddBookViewModel,
    onNavigateBack: () -> Unit
) {
    var urlInput by remember { mutableStateOf("") }
    var urlError by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("网络导入封面", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    // 如果已经有URL封面，显示清除按钮
                    if (!initialCoverUri.isNullOrEmpty()) {
                        TextButton(
                            onClick = {
                                viewModel.updateCoverUri(null)
                                onNavigateBack()
                            }
                        ) {
                            Text("清除封面")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "输入图片网址",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "输入图片的网络链接地址，应用将自动下载并设为封面",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            OutlinedTextField(
                value = urlInput,
                onValueChange = { 
                    urlInput = it
                    urlError = null
                },
                label = { Text("图片URL") },
                placeholder = { Text("https://example.com/book-cover.jpg") },
                singleLine = true,
                isError = urlError != null,
                supportingText = urlError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    when {
                        urlInput.isBlank() -> urlError = "请输入图片地址"
                        !urlInput.startsWith("http://") && !urlInput.startsWith("https://") -> urlError = "请输入以 http:// 或 https:// 开头的地址"
                        else -> {
                            // 直接更新ViewModel的封面URI
                            viewModel.updateCoverUri(urlInput.trim())
                            onNavigateBack()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("应用封面", fontWeight = FontWeight.Bold)
            }
            
            if (!initialCoverUri.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "当前封面：",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = initialCoverUri,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}
