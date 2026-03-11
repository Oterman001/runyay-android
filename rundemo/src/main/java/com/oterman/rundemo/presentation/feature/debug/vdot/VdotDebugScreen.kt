package com.oterman.rundemo.presentation.feature.debug.vdot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.oterman.rundemo.data.fit.VdotCalculator
import com.oterman.rundemo.presentation.components.settings.SettingsCard

private data class VdotDebugResult(
    val finalVdot: Double,
    val rawVdot: Double,
    val apparentTemperature: Double?,
    val environmentalAdjustmentMinutes: Double,
    val heartRateZone: Int,
    val confidence: Double,
    val confidenceDetail: ConfidenceDetail
)

private data class ConfidenceDetail(
    val distanceScore: Double,
    val durationScore: Double,
    val hrZoneScore: Double,
    val envScore: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VdotDebugScreen(
    onNavigateBack: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    // Input fields
    var distanceKm by remember { mutableStateOf("") }
    var durationMin by remember { mutableStateOf("") }
    var avgHeartRate by remember { mutableStateOf("") }
    var maxHeartRate by remember { mutableStateOf("190") }
    var restHeartRate by remember { mutableStateOf("60") }
    var temperature by remember { mutableStateOf("") }
    var humidity by remember { mutableStateOf("") }

    var result by remember { mutableStateOf<VdotDebugResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun calculate() {
        val dist = distanceKm.toDoubleOrNull()
        val dur = durationMin.toDoubleOrNull()
        val hr = avgHeartRate.toDoubleOrNull()
        val maxHR = maxHeartRate.toDoubleOrNull() ?: 190.0
        val restHR = restHeartRate.toDoubleOrNull() ?: 60.0
        val temp = temperature.toDoubleOrNull()
        val hum = humidity.toDoubleOrNull()

        if (dist == null || dist <= 0) {
            errorMessage = "请输入有效的距离"
            result = null
            return
        }
        if (dur == null || dur <= 0) {
            errorMessage = "请输入有效的时长"
            result = null
            return
        }
        if (hr == null || hr <= 0) {
            errorMessage = "请输入有效的心率"
            result = null
            return
        }

        errorMessage = null

        val distanceMeters = dist * 1000.0
        val vdotResult = VdotCalculator.calculateWithResult(
            distanceMeters = distanceMeters,
            timeMinute = dur,
            heartRate = hr,
            temperature = temp,
            humidity = hum,
            maxHR = maxHR,
            restHR = restHR
        )

        if (vdotResult == null) {
            errorMessage = "计算失败，请检查输入参数"
            result = null
            return
        }

        // Calculate apparent temperature
        val apparentTemp = if (temp != null && temp != 0.0) {
            VdotCalculator.getApparentTemperature(temp, hum)
        } else null

        // Calculate heart rate zone using AbilityZoneCalculator
        val heartRateZone = getHeartRateZoneForDebug(hr, maxHR, restHR)

        // Calculate confidence detail
        val hasTemp = temp != null && temp != 0.0
        val confidenceDetail = calculateConfidenceDetail(distanceMeters, dur, heartRateZone, hasTemp)

        result = VdotDebugResult(
            finalVdot = vdotResult.vdot,
            rawVdot = vdotResult.rawVdot,
            apparentTemperature = apparentTemp,
            environmentalAdjustmentMinutes = vdotResult.environmentalAdjustmentMinutes,
            heartRateZone = heartRateZone,
            confidence = vdotResult.confidence,
            confidenceDetail = confidenceDetail
        )
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("VDOT 计算调试") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            state = rememberLazyListState(),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // 基础跑步数据
            item {
                Text(
                    text = "基础跑步数据",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
            }

            item {
                SettingsCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        InputField(
                            label = "距离（km）",
                            value = distanceKm,
                            onValueChange = { distanceKm = it }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        InputField(
                            label = "时长（分钟）",
                            value = durationMin,
                            onValueChange = { durationMin = it }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        InputField(
                            label = "平均心率（bpm）",
                            value = avgHeartRate,
                            onValueChange = { avgHeartRate = it }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // 运动员参数
            item {
                Text(
                    text = "运动员参数",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
            }

            item {
                SettingsCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        InputField(
                            label = "最大心率（默认 190）",
                            value = maxHeartRate,
                            onValueChange = { maxHeartRate = it }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        InputField(
                            label = "静息心率（默认 60）",
                            value = restHeartRate,
                            onValueChange = { restHeartRate = it }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // 环境参数
            item {
                Text(
                    text = "环境参数（可选）",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
            }

            item {
                SettingsCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        InputField(
                            label = "温度（℃）",
                            value = temperature,
                            onValueChange = { temperature = it }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        InputField(
                            label = "湿度（%）",
                            value = humidity,
                            onValueChange = { humidity = it }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // 计算按钮
            item {
                Button(
                    onClick = { calculate() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("计算 VDOT")
                }
            }

            // 错误信息
            errorMessage?.let { msg ->
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            // 计算结果
            result?.let { res ->
                item { Spacer(modifier = Modifier.height(16.dp)) }

                item {
                    Text(
                        text = "计算结果",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )
                }

                item {
                    SettingsCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            ResultRow("最终 VDOT（含调整）", "%.2f".format(res.finalVdot))
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            ResultRow("原始 VDOT（无环境修正）", "%.2f".format(res.rawVdot))
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            if (res.apparentTemperature != null) {
                                ResultRow("体感温度", "%.1f ℃".format(res.apparentTemperature))
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            ResultRow(
                                "环境时间修正量",
                                "%.2f 分钟".format(res.environmentalAdjustmentMinutes)
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            ResultRow("心率区间", "Zone ${res.heartRateZone}")
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            ResultRow("置信度", "%.2f".format(res.confidence))
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                item {
                    Text(
                        text = "置信度明细",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )
                }

                item {
                    SettingsCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            ResultRow(
                                "距离可靠性（0~0.4）",
                                "%.2f".format(res.confidenceDetail.distanceScore)
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            ResultRow(
                                "时长可靠性（0~0.3）",
                                "%.2f".format(res.confidenceDetail.durationScore)
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            ResultRow(
                                "心率区间适配（0~0.2）",
                                "%.2f".format(res.confidenceDetail.hrZoneScore)
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            ResultRow(
                                "环境数据完整（0~0.1）",
                                "%.2f".format(res.confidenceDetail.envScore)
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun InputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * 计算心率区间（复用 AbilityZoneCalculator 逻辑）
 */
private fun getHeartRateZoneForDebug(heartRate: Double, maxHR: Double, restHR: Double): Int {
    if (heartRate <= 0) return 0
    val heartRateRanges =
        com.oterman.rundemo.data.fit.AbilityZoneCalculator.calculateHeartRate7Ranges(restHR, maxHR)
    return com.oterman.rundemo.data.fit.AbilityZoneCalculator.getZoneByHeartRate(
        heartRate,
        heartRateRanges
    )
}

/**
 * 计算置信度明细（四项子分）
 */
private fun calculateConfidenceDetail(
    distanceMeters: Double,
    timeMinute: Double,
    heartRateZone: Int,
    hasTemperature: Boolean
): ConfidenceDetail {
    val distanceKm = distanceMeters / 1000.0

    val distanceScore = when {
        distanceKm >= 5 && distanceKm <= 42 -> 0.4
        distanceKm >= 3 -> 0.3
        distanceKm >= 2 -> 0.2
        distanceKm >= 1 -> 0.1
        else -> 0.05
    }

    val durationScore = when {
        timeMinute >= 25 && timeMinute <= 120 -> 0.3
        timeMinute >= 15 -> 0.2
        timeMinute >= 10 -> 0.1
        else -> 0.05
    }

    val hrZoneScore = when (heartRateZone) {
        4, 5 -> 0.2
        3 -> 0.15
        2 -> 0.10
        1 -> 0.05
        else -> 0.15
    }

    val envScore = if (hasTemperature) 0.1 else 0.05

    return ConfidenceDetail(
        distanceScore = distanceScore,
        durationScore = durationScore,
        hrZoneScore = hrZoneScore,
        envScore = envScore
    )
}
