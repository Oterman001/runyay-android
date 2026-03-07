package com.oterman.rundemo.presentation.feature.onboarding.physio

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.ui.theme.RunTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhysioSetupScreen(
    viewModel: PhysioSetupViewModel,
    onComplete: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val serverError by viewModel.serverError.collectAsState()
    val setupComplete by viewModel.setupComplete.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    var showExplanation by remember { mutableStateOf(true) }
    var showSkipDialog by remember { mutableStateOf(false) }
    var showBirthdayRequiredDialog by remember { mutableStateOf(false) }

    LaunchedEffect(setupComplete) {
        if (setupComplete) onComplete()
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, end = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                TextButton(onClick = { showSkipDialog = true }) {
                    Text("跳过", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            // 图标 + 标题
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = RunTheme.colorScheme.blue
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "完善您的生理参数",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 说明卡片
            item {
                SectionCard {
                    Text(
                        text = "心率区间、训练负荷等指标基于以下参数计算。准确的参数让您的跑步分析更有意义。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 参数设置卡片
            item {
                SectionCard {
                    // 性别
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "性别",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = settings.isMale,
                                onClick = { viewModel.onGenderChanged(true) },
                                label = { Text("男") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RunTheme.colorScheme.blue,
                                    selectedLabelColor = androidx.compose.ui.graphics.Color.White
                                )
                            )
                            FilterChip(
                                selected = !settings.isMale,
                                onClick = { viewModel.onGenderChanged(false) },
                                label = { Text("女") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RunTheme.colorScheme.blue,
                                    selectedLabelColor = androidx.compose.ui.graphics.Color.White
                                )
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // 出生年月
                    val birthdayText = if (settings.birthdayMillis > 0L) {
                        SimpleDateFormat("yyyy年M月", Locale.CHINA).format(Date(settings.birthdayMillis))
                    } else {
                        "未设置"
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "出生年月",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = birthdayText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // 最大心率
                    HRPickerRow(
                        label = "最大心率",
                        value = settings.maxHeartRate,
                        range = 120..239,
                        onValueChange = viewModel::onMaxHRChanged
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // 静息心率
                    HRPickerRow(
                        label = "静息心率",
                        value = settings.restingHeartRate,
                        range = 30..120,
                        onValueChange = viewModel::onRestHRChanged
                    )
                }
            }

            // 参数说明折叠卡片
            item {
                SectionCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showExplanation = !showExplanation },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "参数说明",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (showExplanation) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    AnimatedVisibility(visible = showExplanation) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            ExplanationItem(
                                title = "最大心率",
                                desc = "心脏每分钟最大跳动次数，通常 = 220 - 年龄。"
                            )
                            Spacer(Modifier.height(6.dp))
                            ExplanationItem(
                                title = "静息心率",
                                desc = "清晨安静时的心率，越低代表有氧能力越好。"
                            )
                            Spacer(Modifier.height(6.dp))
                            ExplanationItem(
                                title = "心率区间",
                                desc = "基于 HRR 法（心率储备）划分 7 个训练区间。"
                            )
                        }
                    }
                }
            }

            // 完成按钮
            item {
                Button(
                    onClick = {
                        if (settings.birthdayMillis <= 0L) {
                            showBirthdayRequiredDialog = true
                        } else {
                            viewModel.completeSetup()
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RunTheme.colorScheme.blue
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = androidx.compose.ui.graphics.Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("完成", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    // 生日必填校验弹窗
    if (showBirthdayRequiredDialog) {
        AlertDialog(
            onDismissRequest = { showBirthdayRequiredDialog = false },
            title = { Text("请设置出生年月") },
            text = { Text("出生年月用于计算最大心率，请先完成设置。") },
            confirmButton = {
                TextButton(onClick = { showBirthdayRequiredDialog = false }) {
                    Text("好的")
                }
            }
        )
    }

    // 服务端失败弹窗
    if (serverError != null) {
        AlertDialog(
            onDismissRequest = { viewModel.skipServerSync() },
            title = { Text("同步失败") },
            text = { Text("生理参数已保存到本地，但同步到服务器失败，可稍后重试。") },
            confirmButton = {
                TextButton(onClick = { viewModel.retryServerSync() }) { Text("重试") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.skipServerSync() }) { Text("跳过，继续") }
            }
        )
    }

    // 跳过挽留弹窗
    if (showSkipDialog) {
        AlertDialog(
            onDismissRequest = { showSkipDialog = false },
            title = { Text("确定跳过？") },
            text = { Text("准确的生理参数让心率区间和训练分析更有意义，建议完成设置。跳过后也可在「我的」→「心率区间设置」中随时修改。") },
            confirmButton = {
                TextButton(onClick = { showSkipDialog = false }) {
                    Text("继续设置", color = RunTheme.colorScheme.blue)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSkipDialog = false
                    viewModel.skipSetup()
                    onComplete()
                }) {
                    Text("跳过")
                }
            }
        )
    }

    // 日期选择器
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = if (settings.birthdayMillis > 0L) settings.birthdayMillis else null
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        viewModel.onBirthdayChanged(Date(millis))
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { if (value > range.first) onValueChange(value - 1) }) {
                Text("－", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(32.dp))
            Text(
                "$value",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.width(32.dp))
            TextButton(onClick = { if (value < range.last) onValueChange(value + 1) }) {
                Text("＋", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ExplanationItem(title: String, desc: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = desc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
