package com.oterman.rundemo.presentation.feature.legal

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.layout.navigationBarsPadding
import com.oterman.rundemo.data.local.PreferencesManager
import com.umeng.commonsdk.UMConfigure
import com.oterman.rundemo.BuildConfig

@Composable
fun PrivacyConsentScreen(
    onAgreed: () -> Unit,
    onNavigateToUserTerms: () -> Unit,
    onNavigateToPrivacyPolicy: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
            Spacer(modifier = Modifier.height(72.dp))

            Text(
                text = "个人信息保护提示",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "欢迎来到跑鸭·RunYay！",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "我们将依据《用户协议》和《隐私政策》向您提供服务并保护您的个人信息。请在同意前仔细阅读并做出选择。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "1、为保障账号安全与统计分析功能，我们会在您使用服务时收集设备标识（Android ID、OAID、SN）、IP 地址及设备基础信息，用于安全风控、崩溃排查与应用统计（由友盟 SDK 处理），仅在您同意后才开始收集。\n\n" +
                        "2、可选权限将仅在您使用对应功能时申请，且可随时关闭：存储（导入 FIT/GPX 运动文件）、位置（记录跑步 GPS 轨迹）、相机（上传头像）、通知（同步进度提醒），拒绝不影响其他基础功能。\n\n" +
                        "3、我们遵循最小必要原则，不会将您的个人信息用于与服务无关的用途，亦不会在未经授权的情况下向第三方发送您的个人信息。\n\n" +
                        "4、剪切板访问：为实现统计分析与功能联动，友盟 SDK 可能读取剪切板相关信息(获取分享信息或者跳转口令)。当您在App内复制客服联系方式时，相关内容会被写入剪切板。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
            )

            Spacer(modifier = Modifier.height(20.dp))

            val linkColor = MaterialTheme.colorScheme.primary
            val annotatedText = buildAnnotatedString {
                append("请您阅读并同意")
                pushStringAnnotation(tag = "USER_TERMS", annotation = "user_terms")
                withStyle(
                    SpanStyle(
                        color = linkColor,
                        fontWeight = FontWeight.Medium,
                        textDecoration = TextDecoration.Underline
                    )
                ) { append("《用户协议》") }
                pop()
                append("和")
                pushStringAnnotation(tag = "PRIVACY_POLICY", annotation = "privacy_policy")
                withStyle(
                    SpanStyle(
                        color = linkColor,
                        fontWeight = FontWeight.Medium,
                        textDecoration = TextDecoration.Underline
                    )
                ) { append("《隐私政策》") }
                pop()
                append("，点击同意并继续即表示您已阅读并同意上述协议。")
            }

            ClickableText(
                text = annotatedText,
                style = TextStyle(
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                onClick = { offset ->
                    annotatedText.getStringAnnotations("USER_TERMS", offset, offset)
                        .firstOrNull()?.let { onNavigateToUserTerms() }
                    annotatedText.getStringAnnotations("PRIVACY_POLICY", offset, offset)
                        .firstOrNull()?.let { onNavigateToPrivacyPolicy() }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))
            } // end scrollable column

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { activity?.finish() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("不同意")
                }

                Button(
                    onClick = {
                        val prefs = PreferencesManager(context)
                        prefs.setPrivacyConsentAccepted(true)
                        UMConfigure.init(
                            context,
                            "69a930fe6f259537c76ddab0",
                            BuildConfig.UMENG_CHANNEL,
                            UMConfigure.DEVICE_TYPE_PHONE,
                            ""
                        )
                        onAgreed()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("同意并继续")
                }
            }
        }
    }
}
