package com.oterman.rundemo.presentation.feature.contactus

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.R
import com.oterman.rundemo.ui.theme.RunTheme
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeChatQrCodeScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    var showCopiedToast by remember { mutableStateOf(false) }

    // Auto-dismiss toast after 1.5 seconds
    LaunchedEffect(showCopiedToast) {
        if (showCopiedToast) {
            delay(1500L)
            showCopiedToast = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "微信公众号",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // QR code image
                Image(
                    painter = painterResource(id = R.drawable.img_runyay_gzh_qrcode),
                    contentDescription = "跑鸭RunYay公众号二维码",
                    modifier = Modifier.size(280.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Description text with highlighted keyword
                Text(
                    text = buildAnnotatedString {
                        append("公众号搜索")
                        withStyle(
                            SpanStyle(
                                color = RunTheme.colorScheme.destructive,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        ) {
                            append("跑鸭RunYay")
                        }
                        append("关注后，点击底部客服微信获取二维码，添加客服可以吐槽、建议、或者加入跑友群哦。")
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Copy & open WeChat button
                TextButton(
                    onClick = {
                        // Copy "跑鸭RunYay" to clipboard
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("公众号名称", "跑鸭RunYay")
                        clipboard.setPrimaryClip(clip)

                        showCopiedToast = true

                        // Open WeChat
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("weixin://"))
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            // WeChat not installed, ignore
                        }
                    }
                ) {
                    Text(
                        text = "一键复制 & 打开微信",
                        style = MaterialTheme.typography.titleMedium,
                        color = RunTheme.colorScheme.blue
                    )
                }
            }

            // Toast overlay
            AnimatedVisibility(
                visible = showCopiedToast,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp)
            ) {
                Text(
                    text = "复制成功！",
                    color = Color.White,
                    modifier = Modifier
                        .background(
                            color = Color.Black.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}
