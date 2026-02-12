package com.oterman.rundemo.presentation.feature.datasource

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import com.oterman.rundemo.domain.model.DataSourcePlatform
import com.oterman.rundemo.util.RLog

private const val TAG = "OAuthWebView"

/**
 * OAuth回调参数
 * 区分OAuth 1.0a（佳明）和OAuth 2.0（高驰）
 */
sealed class OAuthCallbackParams {
    /**
     * OAuth 1.0a回调参数（佳明）
     */
    data class OAuth1(
        val oauthToken: String,
        val oauthVerifier: String
    ) : OAuthCallbackParams()

    /**
     * OAuth 2.0回调参数（高驰）
     */
    data class OAuth2(
        val code: String,
        val state: String
    ) : OAuthCallbackParams()
}

/**
 * OAuth授权WebView页面
 * 用于加载第三方平台的授权页面并处理回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OAuthWebViewScreen(
    authUrl: String,
    platform: DataSourcePlatform,
    onAuthCallback: (OAuthCallbackParams) -> Unit,
    onDismiss: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var loadingProgress by remember { mutableFloatStateOf(0f) }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "${platform.displayName}授权",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 加载进度条
            if (isLoading) {
                LinearProgressIndicator(
                    progress = { loadingProgress },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            
            // WebView
            Box(modifier = Modifier.fillMaxSize()) {
                OAuthWebView(
                    authUrl = authUrl,
                    platform = platform,
                    onLoadingChanged = { loading -> isLoading = loading },
                    onProgressChanged = { progress -> loadingProgress = progress / 100f },
                    onAuthCallback = onAuthCallback,
                    onDismiss = onDismiss
                )
            }
        }
    }
}

/**
 * OAuth WebView组件
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun OAuthWebView(
    authUrl: String,
    platform: DataSourcePlatform,
    onLoadingChanged: (Boolean) -> Unit,
    onProgressChanged: (Int) -> Unit,
    onAuthCallback: (OAuthCallbackParams) -> Unit,
    onDismiss: () -> Unit
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    builtInZoomControls = true
                    displayZoomControls = false
                    setSupportZoom(true)
                }
                
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val url = request?.url?.toString() ?: return false
                        RLog.d(TAG, "WebView导航到: $url")

                        // 检查是否是回调URL
                        if (isCallbackUrl(url, platform)) {
                            handleCallbackUrl(url, platform, onAuthCallback, onDismiss)
                            return true
                        }

                        return false
                    }
                    
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        RLog.d(TAG, "页面开始加载: $url")
                        onLoadingChanged(true)
                    }
                    
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        RLog.d(TAG, "页面加载完成: $url")
                        onLoadingChanged(false)
                    }
                }
                
                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        onProgressChanged(newProgress)
                    }
                }
                
                loadUrl(authUrl)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * 检查是否是回调URL
 * 佳明: https://yayarun.cn/oauth/garmin/callback
 * 高驰: https://yayarun.cn/oauth/coros/callback
 */
private fun isCallbackUrl(url: String, platform: DataSourcePlatform): Boolean {
    val isCallback = when (platform) {
        DataSourcePlatform.GARMIN_CHINA -> {
            url.contains("yayarun.cn/oauth/garmin/callback")
        }

       DataSourcePlatform.GARMIN_GLOBAL -> {
            url.contains("yayarun.cn/oauth/garmin/global/callback")
        }

        DataSourcePlatform.COROS -> {
            url.contains("yayarun.cn/oauth/coros/callback") ||
            url.contains("/coros/callback")
        }
        else -> false
    }
    
    if (isCallback) {
        RLog.i(TAG, "检测到回调URL: $url")
    }
    
    return isCallback
}

/**
 * 处理回调URL
 * 佳明使用OAuth 1.0a，参数为oauth_token和oauth_verifier
 * 高驰使用OAuth 2.0，参数为code和state
 */
private fun handleCallbackUrl(
    url: String,
    platform: DataSourcePlatform,
    onAuthCallback: (OAuthCallbackParams) -> Unit,
    onDismiss: () -> Unit
) {
    try {
        val uri = Uri.parse(url)
        RLog.d(TAG, "开始解析回调URL: $url")
        RLog.d(TAG, "平台类型: ${platform.code}")

        when (platform) {
            DataSourcePlatform.GARMIN_CHINA, DataSourcePlatform.GARMIN_GLOBAL -> {
                // 佳明OAuth 1.0a参数
                val oauthToken = uri.getQueryParameter("oauth_token")
                val oauthVerifier = uri.getQueryParameter("oauth_verifier")

                RLog.i(TAG, "佳明回调参数: oauth_token=$oauthToken, oauth_verifier=$oauthVerifier")

                if (oauthToken != null && oauthVerifier != null) {
                    if (oauthVerifier == "null" || oauthVerifier.isEmpty()) {
                        RLog.w(TAG, "用户拒绝佳明授权或授权被取消")
                        onDismiss()
                    } else {
                        RLog.i(TAG, "佳明授权成功，回调处理")
                        onAuthCallback(OAuthCallbackParams.OAuth1(oauthToken, oauthVerifier))
                    }
                } else {
                    RLog.w(TAG, "佳明授权失败 - 回调参数不完整")
                    onDismiss()
                }
            }

            DataSourcePlatform.COROS -> {
                // 高驰OAuth 2.0参数
                // 使用正则表达式直接从URL字符串提取原始编码参数，避免+被解码为空格
                // 与iOS实现保持一致（见ZhiRun1App.swift:283-299）
                val codeRegex = Regex("code=([^&]*)")
                val stateRegex = Regex("state=([^&]*)")

                val code = codeRegex.find(url)?.groupValues?.get(1)
                // 提取state后，将+替换为%2B以匹配服务端期望的编码格式
                // 原因：WebView加载authUrl时自动将%2B解码为+，COROS回调时保持+原样
                // 服务端期望收到原始的%2B格式，因此需要还原编码
                val state = stateRegex.find(url)?.groupValues?.get(1)?.replace("+", "%2B")

                RLog.i(TAG, "高驰回调参数(编码修正): code=$code, state=$state")

                if (code != null && state != null) {
                    if (code.isEmpty()) {
                        RLog.w(TAG, "用户拒绝高驰授权或授权被取消")
                        onDismiss()
                    } else {
                        RLog.i(TAG, "高驰授权成功，回调处理")
                        onAuthCallback(OAuthCallbackParams.OAuth2(code, state))
                    }
                } else {
                    // 检查是否有error参数（OAuth 2.0错误响应）
                    val error = uri.getQueryParameter("error")
                    if (error != null) {
                        RLog.w(TAG, "高驰授权失败: error=$error")
                    } else {
                        RLog.w(TAG, "高驰授权失败 - 回调参数不完整")
                    }
                    onDismiss()
                }
            }

            else -> {
                RLog.w(TAG, "不支持的平台类型: ${platform.code}")
                onDismiss()
            }
        }
    } catch (e: Exception) {
        RLog.e(TAG, "解析回调URL异常", e)
        onDismiss()
    }
}

