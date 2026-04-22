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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(72.dp))

            Text(
                text = "服务协议及隐私政策",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "欢迎使用 跑鸭·RunYay！",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "在您开始使用前，请仔细阅读并同意以下协议。我们依据相关规定保护您的隐私权益：",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "• 我们收集您的设备信息和运动数据，用于提供跑步记录、数据分析等核心功能；\n" +
                        "• 我们使用友盟统计 SDK 进行应用分析，仅在您同意后才收集相关信息；\n" +
                        "• 我们不会在未经您授权的情况下向第三方发送您的个人信息。\n",
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

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
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

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
