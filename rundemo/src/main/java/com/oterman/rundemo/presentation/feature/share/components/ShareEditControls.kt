package com.oterman.rundemo.presentation.feature.share.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.ui.theme.RunBlue

/**
 * 共用编辑控件：设备名编辑、日期开关、品牌文案编辑
 * 短图和长图编辑面板都使用
 */
@Composable
fun ShareEditControls(
    deviceName: String,
    showDate: Boolean,
    showNickname: Boolean,
    brandText: String,
    onDeviceNameEdit: () -> Unit,
    onDeviceNameReset: () -> Unit,
    onShowDateToggle: (Boolean) -> Unit,
    onShowNicknameToggle: (Boolean) -> Unit,
    onBrandTextEdit: () -> Unit,
    onBrandTextRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // 手表信息区域
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable(onClick = onDeviceNameEdit),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 小标题行
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "手表信息",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "(点击编辑)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // 内容块
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(RunBlue.copy(alpha = 0.08f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = deviceName.ifBlank { "未设置" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (deviceName.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // 重置按钮：图标上方，文字下方
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(50.dp)
                        .clickable(onClick = onDeviceNameReset)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "重置",
                        tint = RunBlue,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    Text(
                        text = "重置",
                        fontSize = 11.sp,
                        color = RunBlue
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // 底部文案区域
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable(onClick = onBrandTextEdit),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 小标题行
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "底部文案",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "(点击编辑)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // 内容块
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(RunBlue.copy(alpha = 0.08f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = brandText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 2
                )
                // 刷新按钮：图标上方，文字下方
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(50.dp)
                        .clickable(onClick = onBrandTextRefresh)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "刷新",
                        tint = RunBlue,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    Text(
                        text = "刷新",
                        fontSize = 11.sp,
                        color = RunBlue
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // 显示日期时间开关
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "显示日期时间",
                style = MaterialTheme.typography.bodyMedium
            )
            Switch(
                checked = showDate,
                onCheckedChange = onShowDateToggle
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // 显示昵称开关
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "显示昵称",
                style = MaterialTheme.typography.bodyMedium
            )
            Switch(
                checked = showNickname,
                onCheckedChange = onShowNicknameToggle
            )
        }

        Spacer(modifier = Modifier.height(0.dp))
    }
}
