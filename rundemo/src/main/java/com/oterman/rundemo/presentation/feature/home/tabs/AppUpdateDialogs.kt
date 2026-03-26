package com.oterman.rundemo.presentation.feature.home.tabs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.oterman.rundemo.data.network.dto.response.GetLatestVersionResponse
import com.oterman.rundemo.util.MarketUtils

/**
 * 发现新版本弹窗
 * @param isAlreadyDownloaded 是否已在本地缓存，true 时显示"直接安装"文案
 */
@Composable
fun UpdateAvailableDialog(
    info: GetLatestVersionResponse,
    isAlreadyDownloaded: Boolean = false,
    resolvedMarket: MarketUtils.ResolvedMarket? = null,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    val isForce = info.forceUpgrade == true
    val title = if (isAlreadyDownloaded) "新版本已就绪 ${info.versionName ?: ""}"
                else "发现新版本 ${info.versionName ?: ""}"
    val confirmText = when {
        resolvedMarket != null -> "前往 ${resolvedMarket.label}"
        isAlreadyDownloaded    -> "立即安装"
        else                   -> "立即更新"
    }

    AlertDialog(
        onDismissRequest = { if (!isForce) onDismiss() },
        title = { Text(title) },
        text = {
            Column {
                if (isAlreadyDownloaded) {
                    Text(
                        text = "安装包已下载，可直接安装",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (!info.changelog.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                if (!info.changelog.isNullOrBlank()) {
                    Text(
                        text = info.changelog,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onUpdate) {
                Text(confirmText)
            }
        },
        dismissButton = if (!isForce) {
            { TextButton(onClick = onDismiss) { Text("暂不更新") } }
        } else null
    )
}

/**
 * 非 WiFi 流量警告弹窗
 */
@Composable
fun WifiWarningDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("流量提醒") },
        text = { Text("当前未连接 WiFi，继续下载将消耗移动流量，是否继续？") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("继续下载") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
