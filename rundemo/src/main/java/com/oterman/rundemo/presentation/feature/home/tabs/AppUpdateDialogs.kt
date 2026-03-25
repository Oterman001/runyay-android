package com.oterman.rundemo.presentation.feature.home.tabs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.oterman.rundemo.data.network.dto.response.GetLatestVersionResponse

/**
 * 发现新版本弹窗
 */
@Composable
fun UpdateAvailableDialog(
    info: GetLatestVersionResponse,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    val isForce = info.forceUpgrade == true
    AlertDialog(
        onDismissRequest = { if (!isForce) onDismiss() },
        title = { Text("发现新版本 ${info.versionName ?: ""}") },
        text = {
            Column {
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
                Text("立即更新")
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

/**
 * 下载进度弹窗（不可取消）
 */
@Composable
fun DownloadProgressDialog(progress: Float) {
    val percent = (progress * 100).toInt()
    AlertDialog(
        onDismissRequest = {},
        title = { Text("正在下载新版本") },
        text = {
            Column {
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {}
    )
}
