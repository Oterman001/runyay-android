package com.oterman.rundemo.presentation.feature.settings.heartrate

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oterman.rundemo.data.fit.AbilityZoneCalculator
import com.oterman.rundemo.domain.model.DataSourcePlatform
import com.oterman.rundemo.presentation.components.BirthdayPickerDialog
import com.oterman.rundemo.ui.theme.RunTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val zoneNames = listOf(
    "恢复/热身", "轻松跑(E)", "马拉松配速(M)",
    "乳酸阈值(T)", "无氧耐力(A)", "最大摄氧(I)", "爆发力训练(R)"
)

private val zonePercentDesc = listOf(
    "<59%", "59%~74%", "74%~84%",
    "84%~88%", "88%~95%", "95%~100%", ">100%"
)

private val zoneColors = listOf(
    Color(0xFF90CAF9), Color(0xFF64B5F6), Color(0xFF4CAF50),
    Color(0xFFFFC107), Color(0xFFFF9800), Color(0xFFFF5722), Color(0xFFF44336)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HearRateZoneScreen(
    viewModel: HearRateZoneViewModel = viewModel(
        factory = HearRateZoneViewModelFactory(LocalContext.current)
    ),
    onNavigateBack: () -> Unit = {}
) {
    val settings by viewModel.settings.collectAsState()
    val zoneRanges by viewModel.zoneRanges.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()
    val serverError by viewModel.serverError.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showHrInfoDialog by remember { mutableStateOf(false) }
    var showAutoSyncInfoDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showPlatformDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            snackbarHostState.showSnackbar("已保存")
            viewModel.clearSaveSuccess()
        }
    }

    LaunchedEffect(serverError) {
        serverError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearServerError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("心率区间设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = viewModel::save) {
                        Text("保存", color = RunTheme.colorScheme.blue)
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            // ① 说明卡片
            item {
                SectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "您可以根据自身情况手动调整最大心率和静息心率，调整后只影响后续数据。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = { showHrInfoDialog = true }) {
                            Icon(
                                Icons.Outlined.Info,
                                contentDescription = "说明",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ② 自动同步心率数据卡片
            item {
                SectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "自动获取心率数据",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { showAutoSyncInfoDialog = true }) {
                            Icon(
                                Icons.Outlined.Info,
                                contentDescription = "说明",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Switch(
                            checked = settings.isAutoSyncEnabled,
                            onCheckedChange = viewModel::onAutoSyncToggled,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = RunTheme.colorScheme.blue,
                                checkedBorderColor = RunTheme.colorScheme.blue
                            )
                        )
                    }

                    if (settings.isAutoSyncEnabled && viewModel.boundPlatforms.size > 1) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "心率数据来源",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            Box {
                                val selectedPlatform = viewModel.boundPlatforms.find {
                                    it.code == settings.preferredPlatform
                                }
                                TextButton(onClick = { showPlatformDropdown = true }) {
                                    Text(selectedPlatform?.displayName ?: "自动最优")
                                }
                                DropdownMenu(
                                    expanded = showPlatformDropdown,
                                    onDismissRequest = { showPlatformDropdown = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("自动最优") },
                                        onClick = {
                                            viewModel.onPreferredPlatformChanged(null)
                                            showPlatformDropdown = false
                                        }
                                    )
                                    viewModel.boundPlatforms.forEach { platform ->
                                        DropdownMenuItem(
                                            text = { Text(platform.displayName) },
                                            onClick = {
                                                viewModel.onPreferredPlatformChanged(platform)
                                                showPlatformDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ③ HR参数设置卡片
            item {
                SectionCard {
                    // 出生日期行
                    val birthdayText = if (settings.birthdayMillis > 0L) {
                        SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA).format(Date(settings.birthdayMillis))
                    } else {
                        "未设置"
                    }
                    SettingRow(
                        label = "出生日期",
                        value = birthdayText,
                        hint = "自动计算最大心率",
                        onClick = { showDatePicker = true }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // 最大心率行
                    HRPickerRow(
                        label = "最大心率",
                        value = settings.maxHeartRate,
                        range = 120..239,
                        onValueChange = viewModel::onMaxHRChanged
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // 静息心率行
                    HRPickerRow(
                        label = "静息心率",
                        value = settings.restingHeartRate,
                        range = 30..120,
                        onValueChange = viewModel::onRestHRChanged
                    )
                }
            }

            // ④ 心率区间结果展示卡片
            item {
                SectionCard {
                    Text(
                        text = "心率区间",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    zoneNames.forEachIndexed { index, name ->
                        val zoneNum = index + 1
                        val range = zoneRanges[zoneNum]
                        val rangeText = when {
                            range == null -> "–"
                            range.minHR < 0 -> "< ${range.maxHR.toInt()} bpm"
                            range.maxHR < 0 -> "> ${range.minHR.toInt()} bpm"
                            else -> "${range.minHR.toInt()} – ${range.maxHR.toInt()} bpm"
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(6.dp)
                                    .height(72.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(zoneColors.getOrElse(index) { Color.Gray })
                            )
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Z$zoneNum ${zoneNames.getOrElse(index) { name }}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = zonePercentDesc.getOrElse(index) { "" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = rangeText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ⑤ 提示文字
            item {
                Text(
                    text = "注意：修改心率区间后，只影响后续数据计算，历史数据不会改变。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    // 日期选择器
    if (showDatePicker) {
        BirthdayPickerDialog(
            currentMillis = settings.birthdayMillis,
            onSelect = { millis -> viewModel.onBirthdayChanged(Date(millis)) },
            onDismiss = { showDatePicker = false }
        )
    }

    // 心率区间说明弹窗
    if (showHrInfoDialog) {
        AlertDialog(
            onDismissRequest = { showHrInfoDialog = false },
            title = { Text("心率区间说明") },
            text = {
                Text(
                    "心率区间基于心率储备（HRR）法计算，公式为：\n\n" +
                    "目标心率 = (最大心率 - 静息心率) × 强度百分比 + 静息心率\n\n" +
                    "最大心率可通过年龄估算（220 - 年龄），也可根据实际测量值手动设置。\n\n" +
                    "静息心率建议在清晨刚醒时测量，值越低通常代表有氧能力越好。"
                )
            },
            confirmButton = {
                TextButton(onClick = { showHrInfoDialog = false }) { Text("了解了") }
            }
        )
    }

    // 自动同步说明弹窗
    if (showAutoSyncInfoDialog) {
        AlertDialog(
            onDismissRequest = { showAutoSyncInfoDialog = false },
            title = { Text("自动获取心率数据") },
            text = {
                Text(
                    "开启后，跑步记录解析时会自动从您绑定的平台（如佳明、高驰）获取该日期的静息心率，" +
                    "用于更精准的心率区间和训练负荷计算。\n\n" +
                    "关闭后，将使用您手动设置的静息心率值。"
                )
            },
            confirmButton = {
                TextButton(onClick = { showAutoSyncInfoDialog = false }) { Text("了解了") }
            }
        )
    }
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(16.dp)
    ) {
        Column { content() }
    }
}

@Composable
private fun SettingRow(
    label: String,
    value: String,
    hint: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (hint != null) {
                Text(hint, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(value, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HRPickerRow(
    label: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(
            "$value bpm",
            style = MaterialTheme.typography.bodyMedium,
            color = RunTheme.colorScheme.blue,
            fontWeight = FontWeight.Medium
        )
    }

    if (expanded) {
        // 数值选择器：简单的+/-按钮行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = { if (value > range.first) onValueChange(value - 1) }
            ) {
                Text("－", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(32.dp))
            Text(
                "$value",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.width(32.dp))
            TextButton(
                onClick = { if (value < range.last) onValueChange(value + 1) }
            ) {
                Text("＋", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
