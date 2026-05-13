package com.oterman.rundemo.presentation.feature.trainplan.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.oterman.rundemo.domain.model.IntensityType
import com.oterman.rundemo.domain.model.TrainGoalType
import com.oterman.rundemo.domain.model.TrainStep
import com.oterman.rundemo.presentation.feature.trainplan.canMoveLikeIos
import com.oterman.rundemo.presentation.feature.trainplan.formatDuration
import com.oterman.rundemo.presentation.feature.trainplan.formatPace
import com.oterman.rundemo.ui.theme.RunTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepEditSheet(
    step: TrainStep,
    onSave: (TrainStep) -> Unit,
    onDismiss: () -> Unit
) {
    val isWarmupOrCooldown = step.isWarmupOrCooldownStep()
    var purpose by remember { mutableStateOf(step.purpose ?: defaultPurpose(step)) }
    var descName by remember { mutableStateOf(step.descName ?: "") }
    var skipStatus by remember { mutableIntStateOf(if (isWarmupOrCooldown) step.skipStatus else 0) }
    var goalType by remember { mutableStateOf(step.goalType) }
    var distanceUnit by remember { mutableStateOf(step.distanceUnit ?: "KM") }
    var distanceKmMajor by remember { mutableIntStateOf(((step.distanceValue ?: 1.0).toInt()).coerceIn(0, 100)) }
    var distanceKmMinor by remember {
        mutableIntStateOf((((step.distanceValue ?: 1.0) - (step.distanceValue ?: 1.0).toInt()) * 100).toInt().coerceIn(0, 99))
    }
    var distanceMeters by remember { mutableIntStateOf(((step.distanceValue ?: 400.0).toInt()).coerceIn(10, 1000)) }
    val initialSeconds = step.timeGoalSeconds ?: 300
    var hours by remember { mutableIntStateOf((initialSeconds / 3600).coerceIn(0, 23)) }
    var minutes by remember { mutableIntStateOf(((initialSeconds % 3600) / 60).coerceIn(0, 59)) }
    var seconds by remember { mutableIntStateOf((initialSeconds % 60).coerceIn(0, 59)) }
    var intensityType by remember { mutableStateOf(step.intensityType) }
    var minHeartRate by remember { mutableIntStateOf((step.minHeartRate ?: 120).coerceIn(40, 240)) }
    var maxHeartRate by remember { mutableIntStateOf((step.maxHeartRate ?: 150).coerceIn(41, 241)) }
    var minPaceMinute by remember { mutableIntStateOf(paceMinute(step.minPace ?: 300)) }
    var minPaceSecond by remember { mutableIntStateOf(paceSecond(step.minPace ?: 300)) }
    var maxPaceMinute by remember { mutableIntStateOf(paceMinute(step.maxPace ?: 360)) }
    var maxPaceSecond by remember { mutableIntStateOf(paceSecond(step.maxPace ?: 360)) }
    var isGoalPickerExpanded by remember { mutableStateOf(false) }
    var isIntensityPickerExpanded by remember { mutableStateOf(false) }
    var validationMessage by remember { mutableStateOf<String?>(null) }
    val effectiveSkipStatus = if (isWarmupOrCooldown) skipStatus else 0
    val shouldShowGoalAndIntensity = !isWarmupOrCooldown || effectiveSkipStatus != 1

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(720.dp)
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp)
        ) {
            SheetHeader(onDismiss)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ActionCard(
                    isWarmupOrCooldown = isWarmupOrCooldown,
                    canModifyPurpose = step.canMoveLikeIos(),
                    purpose = purpose,
                    onPurposeChange = { purpose = it },
                    skipStatus = effectiveSkipStatus,
                    onSkipStatusChange = {
                        skipStatus = it
                        if (it == 1) {
                            isGoalPickerExpanded = false
                            isIntensityPickerExpanded = false
                        }
                    },
                    descName = descName,
                    onDescNameChange = { descName = it }
                )

                if (shouldShowGoalAndIntensity) {
                    GoalCard(
                        goalType = goalType,
                        onGoalTypeChange = {
                            goalType = it
                            isGoalPickerExpanded = it != TrainGoalType.OPEN
                            isIntensityPickerExpanded = false
                        },
                        isExpanded = isGoalPickerExpanded,
                        onExpandedChange = {
                            if (goalType != TrainGoalType.OPEN) {
                                isGoalPickerExpanded = it
                                if (it) isIntensityPickerExpanded = false
                            }
                        },
                        distanceUnit = distanceUnit,
                        onDistanceUnitChange = { distanceUnit = it },
                        distanceKmMajor = distanceKmMajor,
                        onDistanceKmMajorChange = { distanceKmMajor = it },
                        distanceKmMinor = distanceKmMinor,
                        onDistanceKmMinorChange = { distanceKmMinor = it },
                        distanceMeters = distanceMeters,
                        onDistanceMetersChange = { distanceMeters = it },
                        hours = hours,
                        onHoursChange = { hours = it },
                        minutes = minutes,
                        onMinutesChange = { minutes = it },
                        seconds = seconds,
                        onSecondsChange = { seconds = it }
                    )

                    IntensityCard(
                        intensityType = intensityType,
                        onIntensityTypeChange = {
                            intensityType = it
                            isIntensityPickerExpanded = it != null
                            isGoalPickerExpanded = false
                        },
                        isExpanded = isIntensityPickerExpanded,
                        onExpandedChange = {
                            if (intensityType != null) {
                                isIntensityPickerExpanded = it
                                if (it) isGoalPickerExpanded = false
                            }
                        },
                        minHeartRate = minHeartRate,
                        onMinHeartRateChange = {
                            minHeartRate = it
                            if (minHeartRate >= maxHeartRate) maxHeartRate = (minHeartRate + 1).coerceAtMost(241)
                        },
                        maxHeartRate = maxHeartRate,
                        onMaxHeartRateChange = {
                            maxHeartRate = it
                            if (maxHeartRate <= minHeartRate) minHeartRate = (maxHeartRate - 1).coerceAtLeast(40)
                        },
                        minPaceMinute = minPaceMinute,
                        onMinPaceMinuteChange = { minPaceMinute = it },
                        minPaceSecond = minPaceSecond,
                        onMinPaceSecondChange = { minPaceSecond = it },
                        maxPaceMinute = maxPaceMinute,
                        onMaxPaceMinuteChange = { maxPaceMinute = it },
                        maxPaceSecond = maxPaceSecond,
                        onMaxPaceSecondChange = { maxPaceSecond = it }
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            validationMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Button(
                onClick = {
                    val distanceValue = if (distanceUnit == "M") {
                        distanceMeters.toDouble()
                    } else {
                        distanceKmMajor + distanceKmMinor / 100.0
                    }
                    val timeSeconds = hours * 3600 + minutes * 60 + seconds
                    val minPaceSeconds = paceSeconds(minPaceMinute, minPaceSecond)
                    val maxPaceSeconds = paceSeconds(maxPaceMinute, maxPaceSecond)
                    validationMessage = validateStepEdit(
                        skipStatus = effectiveSkipStatus,
                        goalType = goalType,
                        distanceValue = distanceValue,
                        timeSeconds = timeSeconds,
                        intensityType = intensityType,
                        minHeartRate = minHeartRate,
                        maxHeartRate = maxHeartRate,
                        minPaceSeconds = minPaceSeconds,
                        maxPaceSeconds = maxPaceSeconds
                    )
                    if (validationMessage != null) return@Button
                    onSave(
                        step.copy(
                            descName = descName.takeIf { it.isNotBlank() },
                            purpose = purpose,
                            skipStatus = effectiveSkipStatus,
                            goalType = goalType,
                            distanceUnit = if (goalType == TrainGoalType.DISTANCE) distanceUnit else step.distanceUnit,
                            distanceValue = if (goalType == TrainGoalType.DISTANCE) distanceValue else step.distanceValue,
                            timeGoalSeconds = if (goalType == TrainGoalType.TIME) timeSeconds.takeIf { it > 0 } else step.timeGoalSeconds,
                            intensityType = intensityType,
                            minHeartRate = if (intensityType == IntensityType.HEART_RATE) minHeartRate else null,
                            maxHeartRate = if (intensityType == IntensityType.HEART_RATE) maxHeartRate else null,
                            minPace = if (intensityType == IntensityType.SPEED) minPaceSeconds else null,
                            maxPace = if (intensityType == IntensityType.SPEED) maxPaceSeconds else null
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RunTheme.colorScheme.blue)
            ) {
                Text("保存", modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }
}

@Composable
private fun SheetHeader(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onDismiss) { Text("取消") }
        Spacer(Modifier.weight(1f))
        Text("编辑步骤", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.width(52.dp))
    }
}

@Composable
private fun ActionCard(
    isWarmupOrCooldown: Boolean,
    canModifyPurpose: Boolean,
    purpose: String,
    onPurposeChange: (String) -> Unit,
    skipStatus: Int,
    onSkipStatusChange: (Int) -> Unit,
    descName: String,
    onDescNameChange: (String) -> Unit
) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("动作类型", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.weight(1f))
            if (canModifyPurpose) {
                SegmentedChoiceGroup(
                    options = listOf("WORK" to "训练", "RECOVERY" to "恢复"),
                    selected = purpose,
                    onSelected = onPurposeChange
                )
            } else {
                Text(
                    text = when (purpose) {
                        "WARMUP" -> "热身"
                        "COOLDOWN" -> "放松"
                        else -> purpose
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 10.dp), color = RunTheme.colorScheme.divider.copy(alpha = 0.6f))
        if (isWarmupOrCooldown) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("是否跳过")
                Spacer(Modifier.weight(1f))
                SegmentedChoiceGroup(
                    options = listOf(0 to "否", 1 to "是"),
                    selected = skipStatus,
                    onSelected = onSkipStatusChange
                )
            }
        }
        if (!isWarmupOrCooldown || skipStatus != 1) {
            if (isWarmupOrCooldown) {
                HorizontalDivider(Modifier.padding(vertical = 10.dp), color = RunTheme.colorScheme.divider.copy(alpha = 0.6f))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("单段名字")
                Spacer(Modifier.width(18.dp))
                BasicTextField(
                    value = descName,
                    onValueChange = onDescNameChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterEnd) {
                            if (descName.isEmpty()) {
                                Text(
                                    text = "输入单段名称",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun GoalCard(
    goalType: TrainGoalType,
    onGoalTypeChange: (TrainGoalType) -> Unit,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    distanceUnit: String,
    onDistanceUnitChange: (String) -> Unit,
    distanceKmMajor: Int,
    onDistanceKmMajorChange: (Int) -> Unit,
    distanceKmMinor: Int,
    onDistanceKmMinorChange: (Int) -> Unit,
    distanceMeters: Int,
    onDistanceMetersChange: (Int) -> Unit,
    hours: Int,
    onHoursChange: (Int) -> Unit,
    minutes: Int,
    onMinutesChange: (Int) -> Unit,
    seconds: Int,
    onSecondsChange: (Int) -> Unit
) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("目标类型")
            Spacer(Modifier.weight(1f))
            SegmentedChoiceGroup(
                options = listOf(
                    TrainGoalType.DISTANCE to "距离",
                    TrainGoalType.TIME to "时间",
                    TrainGoalType.OPEN to "开放"
                ),
                selected = goalType,
                onSelected = onGoalTypeChange
            )
        }
        HorizontalDivider(Modifier.padding(vertical = 10.dp), color = RunTheme.colorScheme.divider.copy(alpha = 0.6f))
        ExpandableResultRow(
            title = "目标值",
            value = goalResultText(goalType, distanceUnit, distanceKmMajor, distanceKmMinor, distanceMeters, hours, minutes, seconds),
            expandable = goalType != TrainGoalType.OPEN,
            expanded = isExpanded,
            onClick = { onExpandedChange(!isExpanded) }
        )
        AnimatedVisibility(visible = isExpanded && goalType != TrainGoalType.OPEN) {
            Column {
                HorizontalDivider(Modifier.padding(vertical = 10.dp), color = RunTheme.colorScheme.divider.copy(alpha = 0.6f))
                if (goalType == TrainGoalType.DISTANCE) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("单位选择")
                        Spacer(Modifier.weight(1f))
                        SegmentedChoiceGroup(
                            options = listOf("KM" to "公里", "M" to "米"),
                            selected = distanceUnit,
                            onSelected = onDistanceUnitChange
                        )
                    }
                    HorizontalDivider(Modifier.padding(vertical = 10.dp), color = RunTheme.colorScheme.divider.copy(alpha = 0.6f))
                    if (distanceUnit == "KM") {
                        WheelPickerGroup {
                            Row {
                                WheelPicker(distanceKmMajor, 0..100, onDistanceKmMajorChange, Modifier.weight(1f)) { it.toString() }
                                WheelPicker(distanceKmMinor, 0..99, onDistanceKmMinorChange, Modifier.weight(1f)) { ".${it.toString().padStart(2, '0')}" }
                            }
                        }
                    } else {
                        WheelPickerGroup {
                            WheelPicker(distanceMeters, 10..1000, onDistanceMetersChange, Modifier.fillMaxWidth()) { "${it}m" }
                        }
                    }
                } else if (goalType == TrainGoalType.TIME) {
                    WheelPickerGroup {
                        Row {
                            WheelPicker(hours, 0..23, onHoursChange, Modifier.weight(1f)) { it.toString().padStart(2, '0') }
                            WheelPicker(minutes, 0..59, onMinutesChange, Modifier.weight(1f)) { it.toString().padStart(2, '0') }
                            WheelPicker(seconds, 0..59, onSecondsChange, Modifier.weight(1f)) { it.toString().padStart(2, '0') }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpandableResultRow(
    title: String,
    value: String,
    expandable: Boolean,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = expandable) { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title)
        Spacer(Modifier.weight(1f))
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (expandable) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(if (expanded) 180f else 0f)
            )
        }
    }
}

private fun goalResultText(
    goalType: TrainGoalType,
    distanceUnit: String,
    distanceKmMajor: Int,
    distanceKmMinor: Int,
    distanceMeters: Int,
    hours: Int,
    minutes: Int,
    seconds: Int
): String = when (goalType) {
    TrainGoalType.DISTANCE -> {
        if (distanceUnit == "M") "${distanceMeters} m" else "${distanceKmMajor}.${distanceKmMinor.toString().padStart(2, '0')} km"
    }
    TrainGoalType.TIME -> "%02d:%02d:%02d".format(hours, minutes, seconds)
    TrainGoalType.OPEN -> "自由训练"
    TrainGoalType.CALORIES -> "0 kcal"
    TrainGoalType.PACER -> {
        val duration = hours * 3600 + minutes * 60 + seconds
        formatDuration(duration)
    }
}

@Composable
private fun IntensityCard(
    intensityType: IntensityType?,
    onIntensityTypeChange: (IntensityType?) -> Unit,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    minHeartRate: Int,
    onMinHeartRateChange: (Int) -> Unit,
    maxHeartRate: Int,
    onMaxHeartRateChange: (Int) -> Unit,
    minPaceMinute: Int,
    onMinPaceMinuteChange: (Int) -> Unit,
    minPaceSecond: Int,
    onMinPaceSecondChange: (Int) -> Unit,
    maxPaceMinute: Int,
    onMaxPaceMinuteChange: (Int) -> Unit,
    maxPaceSecond: Int,
    onMaxPaceSecondChange: (Int) -> Unit
) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("强度类型")
            Spacer(Modifier.weight(1f))
            SegmentedChoiceGroup(
                options = listOf<Pair<IntensityType?, String>>(
                    IntensityType.HEART_RATE to "心率",
                    IntensityType.SPEED to "配速",
                    null to "无"
                ),
                selected = intensityType,
                onSelected = onIntensityTypeChange
            )
        }
        HorizontalDivider(Modifier.padding(vertical = 10.dp), color = RunTheme.colorScheme.divider.copy(alpha = 0.6f))
        ExpandableResultRow(
            title = "范围",
            value = intensityResultText(intensityType, minHeartRate, maxHeartRate, minPaceMinute, minPaceSecond, maxPaceMinute, maxPaceSecond),
            expandable = intensityType != null,
            expanded = isExpanded,
            onClick = { onExpandedChange(!isExpanded) }
        )
        AnimatedVisibility(visible = isExpanded && intensityType != null) {
            Column {
                HorizontalDivider(Modifier.padding(vertical = 10.dp), color = RunTheme.colorScheme.divider.copy(alpha = 0.6f))
                when (intensityType) {
                    IntensityType.HEART_RATE -> {
                        WheelPickerGroup {
                            Row {
                                WheelPicker(minHeartRate, 40..240, onMinHeartRateChange, Modifier.weight(1f)) { it.toString() }
                                WheelPicker(maxHeartRate, 41..241, onMaxHeartRateChange, Modifier.weight(1f)) { it.toString() }
                            }
                        }
                    }
                    IntensityType.SPEED -> {
                        WheelPickerGroup {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                WheelPicker(minPaceMinute, 2..23, onMinPaceMinuteChange, Modifier.weight(1f)) { "$it'" }
                                WheelPicker(minPaceSecond, 0..59, onMinPaceSecondChange, Modifier.weight(1f)) { "${it.toString().padStart(2, '0')}\"" }
                                Text("-", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                WheelPicker(maxPaceMinute, 2..23, onMaxPaceMinuteChange, Modifier.weight(1f)) { "$it'" }
                                WheelPicker(maxPaceSecond, 0..59, onMaxPaceSecondChange, Modifier.weight(1f)) { "${it.toString().padStart(2, '0')}\"" }
                            }
                        }
                    }
                    null -> Unit
                }
            }
        }
    }
}

private fun intensityResultText(
    intensityType: IntensityType?,
    minHeartRate: Int,
    maxHeartRate: Int,
    minPaceMinute: Int,
    minPaceSecond: Int,
    maxPaceMinute: Int,
    maxPaceSecond: Int
): String = when (intensityType) {
    IntensityType.HEART_RATE -> "$minHeartRate-$maxHeartRate bpm"
    IntensityType.SPEED -> {
        "${formatPace(paceSeconds(minPaceMinute, minPaceSecond))}-${formatPace(paceSeconds(maxPaceMinute, maxPaceSecond))} /km"
    }
    null -> "无强度要求"
}

private fun validateStepEdit(
    skipStatus: Int,
    goalType: TrainGoalType,
    distanceValue: Double,
    timeSeconds: Int,
    intensityType: IntensityType?,
    minHeartRate: Int,
    maxHeartRate: Int,
    minPaceSeconds: Int,
    maxPaceSeconds: Int
): String? {
    if (skipStatus == 1) return null

    when (goalType) {
        TrainGoalType.DISTANCE -> if (distanceValue <= 0.0) return "目标距离必须大于 0"
        TrainGoalType.TIME -> if (timeSeconds <= 0) return "目标时间不能为 00:00:00"
        TrainGoalType.OPEN -> Unit
        TrainGoalType.CALORIES -> Unit
        TrainGoalType.PACER -> Unit
    }

    return when (intensityType) {
        IntensityType.HEART_RATE -> {
            if (minHeartRate >= maxHeartRate) "心率范围应从低到高，例如 150-160 bpm" else null
        }
        IntensityType.SPEED -> {
            if (minPaceSeconds >= maxPaceSeconds) {
                "配速范围应从快到慢，例如 4'00\"-6'00\" /km"
            } else null
        }
        null -> null
    }
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(RunTheme.colorScheme.cardBg, RoundedCornerShape(12.dp))
            .padding(14.dp),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SegmentedChoiceGroup(
    options: List<Pair<T, String>>,
    selected: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(
        modifier = modifier.horizontalScroll(rememberScrollState())
    ) {
        options.forEachIndexed { index, (value, label) ->
            val isSelected = selected == value
            SegmentedButton(
                selected = isSelected,
                onClick = { onSelected(value) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = RunTheme.colorScheme.blue.copy(alpha = 0.15f),
                    activeContentColor = RunTheme.colorScheme.blue,
                    activeBorderColor = RunTheme.colorScheme.blue
                ),
                icon = {},
                label = {
                    Text(
                        text = label,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WheelPickerGroup(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val itemHeight = 44.dp
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(itemHeight * 3),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .background(RunTheme.colorScheme.blue.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
        )
        content()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WheelPicker(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    label: (Int) -> String
) {
    val items = remember(range.first, range.last) { range.toList() }
    val itemHeight = 44.dp
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = items.indexOf(value).coerceAtLeast(0)
    )
    val snapBehavior = rememberSnapFlingBehavior(listState)
    val centeredIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }
    val consumeAllScroll = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource) = available
            override suspend fun onPostFling(consumed: Velocity, available: Velocity) = available
        }
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val idx = listState.firstVisibleItemIndex.coerceIn(items.indices)
            onValueChange(items[idx])
        }
    }

    Box(
        modifier = modifier.height(itemHeight * 3).nestedScroll(consumeAllScroll),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = snapBehavior,
            contentPadding = PaddingValues(vertical = itemHeight),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(items, key = { _, v -> v }) { index, item ->
                val isSelected = index == centeredIndex
                Box(
                    modifier = Modifier.fillMaxWidth().height(itemHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label(item),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) RunTheme.colorScheme.blue
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.30f)
                    )
                }
            }
        }
    }
}

private fun defaultPurpose(step: TrainStep): String = when {
    step.purpose != null -> step.purpose
    step.warmupFlag == "Y" -> "WARMUP"
    step.cooldownFlag == "Y" -> "COOLDOWN"
    else -> "WORK"
}

private fun TrainStep.isWarmupOrCooldownStep(): Boolean =
    warmupFlag == "Y" || cooldownFlag == "Y"

private fun paceMinute(seconds: Int): Int =
    (seconds / 60).coerceIn(2, 23)

private fun paceSecond(seconds: Int): Int =
    (seconds % 60).coerceIn(0, 59)

private fun paceSeconds(minutes: Int, seconds: Int): Int =
    minutes.coerceIn(2, 23) * 60 + seconds.coerceIn(0, 59)
