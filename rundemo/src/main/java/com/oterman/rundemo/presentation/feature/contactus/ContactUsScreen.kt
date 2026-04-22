package com.oterman.rundemo.presentation.feature.contactus

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oterman.rundemo.R
import com.oterman.rundemo.presentation.components.settings.SettingsCard
import com.oterman.rundemo.presentation.components.settings.SettingsItem
import com.oterman.rundemo.ui.theme.RunTheme
import com.oterman.rundemo.util.LogExportHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactUsScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToWeChat: () -> Unit = {},
    onNavigateToUserTerms: () -> Unit = {},
    onNavigateToPrivacyPolicy: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isExportingLogs by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "联系我们",
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                item {
                    SettingsCard {
                        SettingsItem(
                            iconResId = R.drawable.ic_wechat,
                            title = "微信",
                            onClick = onNavigateToWeChat
                        )
                        SettingsItem(
                            iconResId = R.drawable.ic_xiaohongshu,
                            title = "小红书",
                            onClick = {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://www.xiaohongshu.com/user/profile/621b81e5000000001000e9cd")
                                )
                                context.startActivity(intent)
                            }
                        )
                        SettingsItem(
                            icon = Icons.Default.BugReport,
                            title = "发送日志",
                            subtitle = if (isExportingLogs) "日志导出中..." else null,
                            iconTint = RunTheme.colorScheme.blue,
                            showDivider = false,
                            onClick = {
                                if (!isExportingLogs) {
                                    isExportingLogs = true
                                    scope.launch {
                                        val intent = LogExportHelper.exportLogs(context)
                                        isExportingLogs = false
                                        intent?.let {
                                            context.startActivity(Intent.createChooser(it, "分享日志"))
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }

            androidx.compose.foundation.layout.Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onNavigateToUserTerms) {
                        Text(
                            text = "用户协议",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "·",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    TextButton(onClick = onNavigateToPrivacyPolicy) {
                        Text(
                            text = "隐私政策",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    text = "渝ICP备2025052772号-1A",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
