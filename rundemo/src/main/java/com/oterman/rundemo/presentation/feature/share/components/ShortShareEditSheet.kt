package com.oterman.rundemo.presentation.feature.share.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.presentation.feature.share.ShareMetricType
import com.oterman.rundemo.ui.theme.RunBlue

/**
 * 短图编辑面板
 * 选择 3-9 个指标 + 设备名/日期/文案 编辑
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ShortShareEditSheet(
    selectedMetrics: List<ShareMetricType>,
    availableMetrics: List<ShareMetricType>,
    deviceName: String,
    showDate: Boolean,
    brandText: String,
    onMetricsChanged: (List<ShareMetricType>) -> Unit,
    onDeviceNameChanged: (String) -> Unit,
    onShowDateChanged: (Boolean) -> Unit,
    onBrandTextChanged: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDeviceDialog by remember { mutableStateOf(false) }
    var showBrandDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // 标题
            Text(
                text = "编辑指标",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // 提示
            Text(
                text = "选择 3-12 个指标显示在分享图中",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 指标选择网格（2列）
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 2
            ) {
                availableMetrics.forEach { metric ->
                    val isSelected = selectedMetrics.contains(metric)
                    val canSelect = isSelected || selectedMetrics.size < 12

                    MetricSelectChip(
                        label = metric.displayName,
                        isSelected = isSelected,
                        enabled = canSelect || isSelected,
                        onClick = {
                            val newList = if (isSelected) {
                                if (selectedMetrics.size > 3) {
                                    selectedMetrics - metric
                                } else selectedMetrics
                            } else {
                                if (selectedMetrics.size < 12) {
                                    selectedMetrics + metric
                                } else selectedMetrics
                            }
                            onMetricsChanged(newList)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                // 奇数个时补空
                if (availableMetrics.size % 2 != 0) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 共用编辑控件
            ShareEditControls(
                deviceName = deviceName,
                showDate = showDate,
                brandText = brandText,
                onDeviceNameEdit = { showDeviceDialog = true },
                onDeviceNameReset = { onDeviceNameChanged("") },
                onShowDateToggle = onShowDateChanged,
                onBrandTextEdit = { showBrandDialog = true },
                onBrandTextRefresh = { onBrandTextChanged("") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text("确定")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // 设备名编辑弹窗
    if (showDeviceDialog) {
        EditTextDialog(
            title = "编辑手表信息",
            currentValue = deviceName,
            placeholder = "输入手表型号",
            onConfirm = { onDeviceNameChanged(it); showDeviceDialog = false },
            onDismiss = { showDeviceDialog = false }
        )
    }

    // 品牌文案编辑弹窗
    if (showBrandDialog) {
        EditTextDialog(
            title = "编辑底部文案",
            currentValue = brandText,
            placeholder = "输入自定义文案（3-100字）",
            onConfirm = { onBrandTextChanged(it); showBrandDialog = false },
            onDismiss = { showBrandDialog = false }
        )
    }
}

/**
 * 指标选择芯片
 */
@Composable
private fun MetricSelectChip(
    label: String,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isSelected) RunBlue.copy(alpha = 0.1f)
    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val borderColor = if (isSelected) RunBlue else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val textColor = if (isSelected) RunBlue
    else if (enabled) MaterialTheme.colorScheme.onSurface
    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(RunBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(12.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = label,
                fontSize = 14.sp,
                color = textColor,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}

/**
 * 文字编辑弹窗
 */
@Composable
fun EditTextDialog(
    title: String,
    currentValue: String,
    placeholder: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(currentValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            TextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(placeholder) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
