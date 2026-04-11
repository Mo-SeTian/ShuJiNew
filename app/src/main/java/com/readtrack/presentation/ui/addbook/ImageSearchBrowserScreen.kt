package com.readtrack.presentation.ui.addbook

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch

// JavaScript接口类
class WebAppInterface(
    private val onImageSelected: (String) -> Unit
) {
    @android.webkit.JavascriptInterface
    fun onImageSelected(src: String) {
        onImageSelected(src)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
@Composable
fun ImageSearchBrowserScreen(
    onImageSelected: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedEngine by remember { mutableIntStateOf(0) }
    var currentUrl by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var webViewError by remember { mutableStateOf<String?>(null) }
    var showEngineDialog by remember { mutableStateOf(false) }
    
    val searchEngines = listOf(
        "百度图片" to "https://image.baidu.com/search/index?tn=baiduimage&word=",
        "必应图片" to "https://cn.bing.com/images/search?q="
    )
    
    val focusManager = LocalFocusManager.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("搜索封面图片") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showEngineDialog = true }) {
                        Icon(Icons.Default.Search, contentDescription = "切换搜索引擎")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 搜索栏
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("搜索图片...") },
                leadingIcon = {
                    Text(searchEngines[selectedEngine].first, style = MaterialTheme.typography.labelMedium)
                },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (searchQuery.isNotBlank()) {
                                focusManager.clearFocus()
                                val encoded = java.net.URLEncoder.encode(searchQuery, "UTF-8")
                                currentUrl = searchEngines[selectedEngine].second + encoded
                                isLoading = true
                            }
                        }
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "搜索")
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (searchQuery.isNotBlank()) {
                            focusManager.clearFocus()
                            val encoded = java.net.URLEncoder.encode(searchQuery, "UTF-8")
                            currentUrl = searchEngines[selectedEngine].second + encoded
                            isLoading = true
                        }
                    }
                ),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            
            // 快捷搜索
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("三体", "活着", "红楼梦", "西游记").forEach { suggestion ->
                    SuggestionChip(
                        onClick = {
                            searchQuery = suggestion
                            focusManager.clearFocus()
                            currentUrl = searchEngines[selectedEngine].second + 
                                java.net.URLEncoder.encode(suggestion, "UTF-8")
                            isLoading = true
                        },
                        label = { Text(suggestion, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 提示文字
            Text(
                "长按图片即可设为封面",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // WebView区域
            Box(modifier = Modifier.weight(1f)) {
                if (currentUrl.isEmpty()) {
                    // 空状态
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Image, null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("输入书名搜索封面", style = MaterialTheme.typography.bodyLarge)
                    }
                } else if (webViewError != null) {
                    // 错误状态
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.CloudOff, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("加载失败: $webViewError", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { isLoading = true; webViewError = null }) {
                            Text("重试")
                        }
                    }
                } else {
                    // 使用coroutine scope来确保回调在正确的上下文执行
                    val coroutineScope = rememberCoroutineScope()
                    
                    // WebView
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                    builtInZoomControls = true
                                    displayZoomControls = false
                                    userAgentString = "Mozilla/5.0 (Linux; Android 11; SM-G991B) AppleWebKit/537.36 Chrome/91.0.4472.120"
                                }
                                
                                // 添加JavaScript接口
                                addJavascriptInterface(
                                    WebAppInterface { imageUrl ->
                                        coroutineScope.launch {
                                            onImageSelected(imageUrl)
                                        }
                                    },
                                    "AndroidPicker"
                                )
                                
                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        isLoading = true
                                        webViewError = null
                                    }
                                    
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        isLoading = false
                                        // 注入JS处理长按
                                        view?.evaluateJavascript("""
                                            (function() {
                                                function selectImage(img) {
                                                    if(img && img.src) {
                                                        window.AndroidPicker.onImageSelected(img.src);
                                                    }
                                                }
                                                document.addEventListener('contextmenu', function(e) {
                                                    if(e.target.tagName === 'IMG') {
                                                        e.preventDefault();
                                                        selectImage(e.target);
                                                        return false;
                                                    }
                                                });
                                            })();
                                        """.trimIndent(), null)
                                    }
                                    
                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        error: WebResourceError?
                                    ) {
                                        if (request?.isForMainFrame == true) {
                                            webViewError = error?.description?.toString() ?: "加载失败"
                                        }
                                    }
                                }
                                
                                setDownloadListener { url, _, _, _, _ ->
                                    coroutineScope.launch {
                                        onImageSelected(url)
                                    }
                                }
                            }
                        },
                        update = { webView ->
                            if (currentUrl.isNotEmpty() && webView.url != currentUrl) {
                                webView.loadUrl(currentUrl)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Loading indicator
                    if (isLoading) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)
                        )
                    }
                }
            }
        }
    }
    
    // 搜索引擎选择对话框
    if (showEngineDialog) {
        AlertDialog(
            onDismissRequest = { showEngineDialog = false },
            title = { Text("选择搜索引擎") },
            text = {
                Column {
                    searchEngines.forEachIndexed { index, (name, _) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedEngine == index,
                                onClick = { selectedEngine = index; showEngineDialog = false }
                            )
                            Text(name, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEngineDialog = false }) {
                    Text("关闭")
                }
            }
        )
    }
}
