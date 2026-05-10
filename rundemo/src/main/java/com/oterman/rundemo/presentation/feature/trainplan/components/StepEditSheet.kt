package com.oterman.rundemo.presentation.feature.trainplan.components

import android.widget.NumberPicker
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.oterman.rundemo.domain.model.IntensityType
import com.oterman.rundemo.domain.model.TrainGoalType
import com.oterman.rundemo.domain.model.TrainStep
import com.oterman.rundemo.presentation.feature.trainplan.canMoveLikeIos
import com.oterman.rundemo.presentation.feature.trainplan.formatDuration
import com.oterman.rundemo.presentation.feature.trainplan.formatPace
import com.oterman.rundemo.presentation.feature.trainplan.formatPaceInput
import com.oterman.rundemo.presentation.feature.trainplan.goalText
import com.oterman.rundemo.presentation.feature.trainplan.intensityText
import com.oterman.rundemo.presentation.feature.trainplan.parsePaceInput
import com.oterman.rundemo.ui.theme.RunTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepEditSheet(
    step: TrainStep,
    onSave: (TrainStep) -> Unit,
    onDismiss: () -> Unit
) {
    var purpose by remember { mutableStateOf(step.purpose ?: defaultPurpose(step)) }
    var descName by remember { mutableStateOf(step.descName ?: "") }
    var skipStatus by remember { mutableIntStateOf(step.skipStatus) }
    var goalType by remember { mutableStateOf(step.goalType) }
    var distanceUnit by remember { mutableStateOf(step.distanceUnit ?: "KM") }
    var distanceKmMajor by remember { mutableIntStateOf(((step.distanceValue ?: 1.0).toInt()).coerceIn(0, 100)) }
    var distanceKmMinor by remember {
        mutableIntStateOf((((step.distanceValue ?: 1.0) - (step.distanceValue ?: 1.0).toInt()) * 100).toInt().coerceIn(0, 99))
    }
    var distanceMeters by remember { mutableIntStateOf(((step.distanceValue ?: 400.0).toInt()).coerceIn(0, 10000)) }
    val initialSeconds = step.timeGoalSeconds ?: 300
    var hours by remember { mutableIntStateOf((initialSeconds / 3600).coerceIn(0, 23)) }
    var minutes by remember { mutableIntStateOf(((initialSeconds % 3600) / 60).coerceIn(0, 59)) }
    var seconds by remember { mutableIntStateOf((initialSeconds % 60).coerceIn(0, 59)) }
    var intensityType by remember { mutableStateOf(step.intensityType) }
    var minHeartRate by remember { mutableIntStateOf((step.minHeartRate ?: 120).coerceIn(40, 240)) }
    var maxHeartRate by remember { mutableIntStateOf((step.maxHeartRate ?: 150).coerceIn(41, 241)) }
    var minPace by remember { mutableStateOf(step.minPace?.let { formatPaceInput(it) } ?: "5:00") }
    var maxPace by remember { mutableStateOf(step.maxPace?.let { formatPaceInput(it) } ?: "6:00") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp)
        ) {
            SheetHeader(onDismiss)
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(max = 620.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ActionCard(
                    canModifyPurpose = step.canMoveLikeIos(),
                    purpose = purpose,
                    onPurposeChange = { purpose = it },
                    skipStatus = skipStatus,
                    onSkipStatusChange = { skipStatus = it },
                    descName = descName,
                    onDescNameChange = { descName = it }
                )

                if (skipStatus != 1) {
                    GoalCard(
                        goalType = goalType,
                        onGoalTypeChange = { goalType = it },
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
                        onIntensityTypeChange = { intensityType = it },
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
                        minPace = minPace,
                        onMinPaceChange = { minPace = it },
                        maxPace = maxPace,
                        onMaxPaceChange = { maxPace = it }
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Button(
                onClick = {
                    val distanceValue = if (distanceUnit == "M") {
                        distanceMeters.toDouble()
                    } else {
                        distanceKmMajor + distanceKmMinor / 100.0
                    }
                    val timeSeconds = hours * 3600 + minutes * 60 + seconds
                    onSave(
                        step.copy(
                            descName = descName.takeIf { it.isNotBlank() },
                            purpose = purpose,
                            skipStatus = skipStatus,
                            goalType = goalType,
                            distanceUnit = if (goalType == TrainGoalType.DISTANCE) distanceUnit else step.distanceUnit,
                            distanceValue = if (goalType == TrainGoalType.DISTANCE) distanceValue else step.distanceValue,
                            timeGoalSeconds = if (goalType == TrainGoalType.TIME) timeSeconds.takeIf { it > 0 } else step.timeGoalSeconds,
                            intensityType = intensityType,
                            minHeartRate = if (intensityType == IntensityType.HEART_RATE) minHeartRate else null,
                            maxHeartRate = if (intensityType == IntensityType.HEART_RATE) maxHeartRate else null,
                            minPace = if (intensityType == IntensityType.SPEED) parsePaceInput(minPace) else null,
                            maxPace = if (intensityType == IntensityType.SPEED) parsePaceInput(maxPace) else null
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
    canModifyPurpose: Boolean,
    purpose: String,
    onPurposeChange: (String) -> Unit,
    skipStatus: Int,
    onSkipStatusChange: (Int) -> Unit,
    descName: String,
    onDescNameChange: (String) -> Unit
) {
    SectionCard {
        Text("动作", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        if (canModifyPurpose) {
            Segmented(
                items = listOf("WORK" to "训练", "RECOVERY" to "恢复"),
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
        HorizontalDivider(Modifier.padding(vertical = 10.dp), color = RunTheme.colorScheme.divider)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("是否跳过")
            Spacer(Modifier.weight(1f))
            Segmented(
                items = listOf(0 to "否", 1 to "是"),
                selected = skipStatus,
                onSelected = onSkipStatusChange,
                modifier = Modifier.width(160.dp)
            )
        }
        HorizontalDivider(Modifier.padding(vertical = 10.dp), color = RunTheme.colorScheme.divider)
        OutlinedTextField(
            value = descName,
            onValueChange = onDescNameChange,
            label = { Text("单段名字") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun GoalCard(
    goalType: TrainGoalType,
    onGoalTypeChange: (TrainGoalType) -> Unit,
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
            Segmented(
                items = listOf(TrainGoalType.DISTANCE to "距离", TrainGoalType.TIME to "时间"),
                selected = goalType,
                onSelected = onGoalTypeChange,
                modifier = Modifier.width(180.dp)
            )
        }
        HorizontalDivider(Modifier.padding(vertical = 10.dp), color = RunTheme.colorScheme.divider)
        Text(
            text = if (goalType == TrainGoalType.DISTANCE) {
                val value = if (distanceUnit == "M") "${distanceMeters}m" else "${distanceKmMajor}.${distanceKmMinor.toString().padStart(2, '0')}km"
                "目标值 $value"
            } else {
                "目标值 ${formatDuration(hours * 3600 + minutes * 60 + seconds)}"
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        if (goalType == TrainGoalType.DISTANCE) {
            Segmented(
                items = listOf("KM" to "公里", "M" to "米"),
                selected = distanceUnit,
                onSelected = onDistanceUnitChange,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            if (distanceUnit == "KM") {
                Row {
                    WheelPicker(distanceKmMajor, 0..100, onDistanceKmMajorChange, Modifier.weight(1f)) { it.toString() }
                    WheelPicker(distanceKmMinor, 0..99, onDistanceKmMinorChange, Modifier.weight(1f)) { ".${it.toString().padStart(2, '0')}" }
                }
            } else {
                WheelPicker(distanceMeters, 0..10000, onDistanceMetersChange, Modifier.fillMaxWidth()) { "${it}m" }
            }
        } else {
            Row {
                WheelPicker(hours, 0..23, onHoursChange, Modifier.weight(1f)) { "${it}时" }
                WheelPicker(minutes, 0..59, onMinutesChange, Modifier.weight(1f)) { "${it}分" }
                WheelPicker(seconds, 0..59, onSecondsChange, Modifier.weight(1f)) { "${it}秒" }
            }
        }
    }
}

@Composable
private fun IntensityCard(
    intensityType: IntensityType?,
    onIntensityTypeChange: (IntensityType?) -> Unit,
    minHeartRate: Int,
    onMinHeartRateChange: (Int) -> Unit,
    maxHeartRate: Int,
    onMaxHeartRateChange: (Int) -> Unit,
    minPace: String,
    onMinPaceChange: (String) -> Unit,
    maxPace: String,
    onMaxPaceChange: (String) -> Unit
) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("强度类型")
            Spacer(Modifier.weight(1f))
            Segmented(
                items = listOf(null to "无", IntensityType.HEART_RATE to "心率", IntensityType.SPEED to "配速"),
                selected = intensityType,
                onSelected = onIntensityTypeChange,
                modifier = Modifier.width(220.dp)
            )
        }
        HorizontalDivider(Modifier.padding(vertical = 10.dp), color = RunTheme.colorScheme.divider)
        when (intensityType) {
            IntensityType.HEART_RATE -> {
                Text("范围 $minHeartRate-$maxHeartRate bpm", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row {
                    WheelPicker(minHeartRate, 40..240, onMinHeartRateChange, Modifier.weight(1f)) { it.toString() }
                    WheelPicker(maxHeartRate, 41..241, onMaxHeartRateChange, Modifier.weight(1f)) { it.toString() }
                }
            }
            IntensityType.SPEED -> {
                Text("范围 ${parsePaceInput(minPace)?.let(::formatPace) ?: "--"} - ${parsePaceInput(maxPace)?.let(::formatPace) ?: "--"} /km", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = minPace,
                        onValueChange = onMinPaceChange,
                        label = { Text("最快") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("-")
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = maxPace,
                        onValueChange = onMaxPaceChange,
                        label = { Text("最慢") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            null -> Text("自由练", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
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

@Composable
private fun <T> Segmented(
    items: List<Pair<T, String>>,
    selected: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        items.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = selected == value,
                onClick = { onSelected(value) },
                shape = SegmentedButtonDefaults.itemShape(index, items.size)
            ) { Text(label, maxLines = 1) }
        }
    }
}

@Composable
private fun WheelPicker(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    label: (Int) -> String
) {
    val values = remember(range.first, range.last) { range.toList() }
    AndroidView(
        modifier = modifier.height(150.dp),
        factory = { context ->
            NumberPicker(context).apply {
                minValue = 0
                maxValue = values.lastIndex
                displayedValues = values.map(label).toTypedArray()
                wrapSelectorWheel = true
                this.value = values.indexOf(value).coerceAtLeast(0)
                setOnValueChangedListener { _, _, newVal -> onValueChange(values[newVal]) }
            }
        },
        update = { picker ->
            val index = values.indexOf(value).coerceAtLeast(0)
            if (picker.value != index) picker.value = index
        }
    )
}

private fun defaultPurpose(step: TrainStep): String = when {
    step.purpose != null -> step.purpose
    step.warmupFlag == "Y" -> "WARMUP"
    step.cooldownFlag == "Y" -> "COOLDOWN"
    else -> "WORK"
}
