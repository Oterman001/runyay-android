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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import com.oterman.rundemo.presentation.feature.share.ShareCardType
import com.oterman.rundemo.ui.theme.RunBlue

/**
 * 长图编辑面板
 * 切换各卡片显示/隐藏 + 设备名/日期/文案 编辑
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LongShareEditSheet(
    enabledCards: Map<ShareCardType, Boolean>,
    availableCards: List<ShareCardType>,
    deviceName: String,
    showDate: Boolean,
    brandText: String,
    onCardToggle: (ShareCardType, Boolean) -> Unit,
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
                text = "编辑卡片",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // 提示
            Text(
                text = "选择要在分享图中显示的卡片",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 卡片列表（单列）
            availableCards.forEach { cardType ->
                val isHeader = cardType == ShareCardType.HEADER
                val isEnabled = enabledCards[cardType] != false
                CardToggleItem(
                    label = cardType.displayName,
                    isEnabled = isEnabled,
                    isRequired = isHeader,
                    onToggle = { enabled -> onCardToggle(cardType, enabled) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 共用编辑控件
            ShareEditControls(
                deviceName = deviceName,
                showDate = showDate,
                brandText = brandText,
                onDeviceNameEdit = { showDeviceDialog = true },
                onShowDateToggle = onShowDateChanged,
                onBrandTextEdit = { showBrandDialog = true },
                onBrandTextRefresh = { onBrandTextChanged("") }
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showDeviceDialog) {
        EditTextDialog(
            title = "编辑手表信息",
            currentValue = deviceName,
            placeholder = "输入手表型号",
            onConfirm = { onDeviceNameChanged(it); showDeviceDialog = false },
            onDismiss = { showDeviceDialog = false }
        )
    }

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

@Composable
private fun CardToggleItem(
    label: String,
    isEnabled: Boolean,
    isRequired: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isEnabled) RunBlue.copy(alpha = 0.08f)
    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (isRequired) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "必选",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(RunBlue)
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
        }
        Switch(
            checked = isEnabled,
            onCheckedChange = { if (!isRequired) onToggle(it) },
            enabled = !isRequired
        )
    }
}
