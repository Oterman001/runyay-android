package com.oterman.rundemo.presentation.components

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * 阿里云图形验证码对话框
 * 通过 WebView 加载 assets/aliyun_captcha.html，并与 JS 通信
 */
@Composable
fun AliyunCaptchaDialog(
    onSuccess: (String) -> Unit,
    onFailure: (String) -> Unit,
    onCancel: () -> Unit
) {
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // Surface 自动处理圆角、阴影和居中布局
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            Box {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            webViewClient = WebViewClient()
                            addJavascriptInterface(
                                CaptchaBridge(onSuccess, onFailure),
                                "AndroidBridge"
                            )
                            loadUrl("file:///android_asset/aliyun_captcha.html")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                )

                // 关闭按钮（右上角）
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * JS Bridge，供 WebView 回调 Compose 层
 * @JavascriptInterface 方法在后台线程调用，需切换回主线程
 */
private class CaptchaBridge(
    private val onSuccess: (String) -> Unit,
    private val onFailure: (String) -> Unit
) {
    @JavascriptInterface
    fun onCaptchaSuccess(param: String) {
        Handler(Looper.getMainLooper()).post { onSuccess(param) }
    }

    @JavascriptInterface
    fun onCaptchaFailure(msg: String) {
        Handler(Looper.getMainLooper()).post { onFailure(msg) }
    }
}
