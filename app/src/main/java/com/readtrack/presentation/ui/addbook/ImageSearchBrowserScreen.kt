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

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ImageSearchBrowserScreen(
    onImageSelected: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var webViewError by remember { mutableStateOf<String?>(null) }
    var currentUrl by remember { mutableStateOf("") }
    
    val focusManager = LocalFocusManager.current
    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    
    val searchEngines = remember {
        mapOf(
            "百度" to "https://image.baidu.com/search/index?tn=baiduimage&word=",
            "必应" to "https://cn.bing.com/images/search?q=",
        )
    }
    var selectedEngine by remember { mutableStateOf("百度") }
    var showEngineMenu by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("图片搜索", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { webViewRef.value?.reload() }) {
                        Icon(Icons.Default.Refresh, "刷新")
                    }
                    Box {
                        IconButton(onClick = { showEngineMenu = true }) {
                            Icon(Icons.Default.Language, "搜索引擎")
                        }
                        DropdownMenu(expanded = showEngineMenu, onDismissRequest = { showEngineMenu = false }) {
                            searchEngines.keys.forEach { engine ->
                                DropdownMenuItem(
                                    text = { Text(engine) },
                                    onClick = { selectedEngine = engine; showEngineMenu = false },
                                    leadingIcon = { if (selectedEngine == engine) Icon(Icons.Default.Check, null) }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { showEngineMenu = true }) {
                        Text(selectedEngine, fontWeight = FontWeight.Medium)
                    }
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("搜索图片...") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (searchQuery.isNotBlank()) {
                                    focusManager.clearFocus()
                                    currentUrl = "${searchEngines[selectedEngine]}${java.net.URLEncoder.encode(searchQuery, "UTF-8")}"
                                    isLoading = true
                                }
                            }
                        ),
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent
                        )
                    )
                    IconButton(onClick = {
                        if (searchQuery.isNotBlank()) {
                            focusManager.clearFocus()
                            currentUrl = "${searchEngines[selectedEngine]}${java.net.URLEncoder.encode(searchQuery, "UTF-8")}"
                            isLoading = true
                        }
                    }) {
                        Icon(Icons.Default.Search, "搜索")
                    }
                }
            }
            
            if (currentUrl.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.ImageSearch, null, modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        Text("输入书名搜索封面", style = MaterialTheme.typography.bodyLarge)
                        Text("长按图片选择", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("三体", "活着", "红楼梦").forEach { s ->
                                SuggestionChip(onClick = {
                                    searchQuery = s
                                    focusManager.clearFocus()
                                    currentUrl = "${searchEngines[selectedEngine]}${java.net.URLEncoder.encode(s, "UTF-8")}"
                                    isLoading = true
                                }, label = { Text(s) })
                            }
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (webViewError != null) {
                        Column(modifier = Modifier.fillMaxSize().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(Icons.Default.CloudOff, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("加载失败: ${webViewError}", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { webViewError = null; webViewRef.value?.reload() }) { Text("重试") }
                        }
                    } else {
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    settings.apply {
                                        javaScriptEnabled = true
                                        domStorageEnabled = true
                                        loadWithOverviewMode = true
                                        useWideViewPort = true
                                        builtInZoomControls = true
                                        displayZoomControls = false
                                        setSupportZoom(true)
                                        userAgentString = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0.0.0"
                                    }
                                    webViewClient = object : WebViewClient() {
                                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                            isLoading = true
                                            url?.let { currentUrl = it }
                                        }
                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            isLoading = false
                                            view?.setOnLongClickListener { v ->
                                                val result = (v as WebView).hitTestResult
                                                result?.extra?.let { url ->
                                                    if (url.startsWith("http")) {
                                                        onImageSelected(url)
                                                        true
                                                    } else false
                                                } ?: false
                                            }
                                        }
                                        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                            if (request?.isForMainFrame == true) {
                                                webViewError = error?.description?.toString() ?: "网络错误"
                                            }
                                        }
                                    }
                                    webChromeClient = object : WebChromeClient() {
                                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                            isLoading = newProgress < 100
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                            update = { webView ->
                                webViewRef.value = webView
                                if (webView.url != currentUrl && currentUrl.isNotEmpty()) {
                                    webView.loadUrl(currentUrl)
                                }
                            }
                        )
                        if (isLoading) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }
}
