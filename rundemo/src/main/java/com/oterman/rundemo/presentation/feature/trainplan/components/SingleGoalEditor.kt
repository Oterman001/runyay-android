package com.oterman.rundemo.presentation.feature.trainplan.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.oterman.rundemo.domain.model.TrainStep
import com.oterman.rundemo.domain.model.TrainWholeType
import com.oterman.rundemo.ui.theme.RunTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SingleGoalEditor(
    trainWholeType: TrainWholeType,
    distanceGoalStep: TrainStep?,
    timeGoalStep: TrainStep?,
    calGoalStep: TrainStep?,
    pacerGoalStep: TrainStep?,
    onDistanceChange: (Double?) -> Unit,
    onTimeChange: (Int?) -> Unit,
    onCaloriesChange: (Int?) -> Unit,
    onPacerFullChange: (distanceMeters: Double?, distanceUnit: String, timeSeconds: Int?, paceSecondsPerKm: Int?) -> Unit,
    isEditMode: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (trainWholeType) {
            TrainWholeType.DISTANCE -> DistanceGoal(distanceGoalStep, onDistanceChange, isEditMode)
            TrainWholeType.TIME -> TimeGoal(timeGoalStep, onTimeChange, isEditMode)
            TrainWholeType.CALORIES -> CaloriesGoal(calGoalStep, onCaloriesChange, isEditMode)
            TrainWholeType.PACER -> PacerGoal(pacerGoalStep, onPacerFullChange, isEditMode)
            TrainWholeType.SELF_DEFINE -> Unit
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DistanceGoal(step: TrainStep?, onDistanceChange: (Double?) -> Unit, isEditMode: Boolean) {
    var distanceUnit by remember { mutableStateOf(step?.distanceUnit ?: "KM") }
    var distanceKmMajor by remember(step?.distanceValue) {
        mutableIntStateOf(((step?.distanceValue ?: 1.0).toInt()).coerceIn(0, 100))
    }
    var distanceKmMinor by remember(step?.distanceValue) {
        mutableIntStateOf((((step?.distanceValue ?: 1.0) - (step?.distanceValue ?: 1.0).toInt()) * 100).toInt().coerceIn(0, 99))
    }
    var distanceMetersVal by remember(step?.distanceValue) {
        mutableIntStateOf(((step?.distanceValue ?: 400.0).toInt() / 10 * 10).coerceIn(10, 1000))
    }
    var expanded by remember { mutableStateOf(false) }

    val displayText = if (distanceUnit == "M") "${distanceMetersVal} m"
    else "${distanceKmMajor}.${distanceKmMinor.toString().padStart(2, '0')} km"

    SectionCard {
        ExpandableResultRow(
            title = "目标距离",
            value = displayText,
            expandable = isEditMode,
            expanded = expanded && isEditMode,
            onClick = { if (isEditMode) expanded = !expanded }
        )
        AnimatedVisibility(visible = expanded && isEditMode) {
            Column {
                HorizontalDivider(Modifier.padding(vertical = 10.dp), color = RunTheme.colorScheme.divider.copy(alpha = 0.6f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("单位选择")
                    Spacer(Modifier.weight(1f))
                    SegmentedChoiceGroup(
                        options = listOf("KM" to "公里", "M" to "米"),
                        selected = distanceUnit,
                        onSelected = { distanceUnit = it }
                    )
                }
                HorizontalDivider(Modifier.padding(vertical = 10.dp), color = RunTheme.colorScheme.divider.copy(alpha = 0.6f))
                if (distanceUnit == "KM") {
                    WheelPickerGroup {
                        Row {
                            WheelPicker(distanceKmMajor, 0..100, {
                                distanceKmMajor = it
                                onDistanceChange(it + distanceKmMinor / 100.0)
                            }, Modifier.weight(1f)) { it.toString() }
                            WheelPicker(distanceKmMinor, 0..99, {
                                distanceKmMinor = it
                                onDistanceChange(distanceKmMajor + it / 100.0)
                            }, Modifier.weight(1f)) { ".${it.toString().padStart(2, '0')}" }
                        }
                    }
                } else {
                    WheelPickerGroup {
                        WheelPicker(distanceMetersVal, 10..1000, {
                            distanceMetersVal = it
                            onDistanceChange(it.toDouble())
                        }, Modifier.fillMaxWidth(), step = 10) { "${it}m" }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TimeGoal(step: TrainStep?, onTimeChange: (Int?) -> Unit, isEditMode: Boolean) {
    val totalSeconds = step?.timeGoalSeconds ?: 0
    var hours by remember(totalSeconds) { mutableIntStateOf((totalSeconds / 3600).coerceIn(0, 23)) }
    var minutes by remember(totalSeconds) { mutableIntStateOf(((totalSeconds % 3600) / 60).coerceIn(0, 59)) }
    var seconds by remember(totalSeconds) { mutableIntStateOf((totalSeconds % 60).coerceIn(0, 59)) }
    var expanded by remember { mutableStateOf(false) }

    val displayText = "%02d:%02d:%02d".format(hours, minutes, seconds)

    SectionCard {
        ExpandableResultRow(
            title = "目标时长",
            value = displayText,
            expandable = isEditMode,
            expanded = expanded && isEditMode,
            onClick = { if (isEditMode) expanded = !expanded }
        )
        AnimatedVisibility(visible = expanded && isEditMode) {
            Column {
                HorizontalDivider(Modifier.padding(vertical = 10.dp), color = RunTheme.colorScheme.divider.copy(alpha = 0.6f))
                WheelPickerGroup {
                    Row {
                        WheelPicker(hours, 0..23, {
                            hours = it
                            onTimeChange((it * 3600 + minutes * 60 + seconds).takeIf { t -> t > 0 })
                        }, Modifier.weight(1f)) { it.toString().padStart(2, '0') }
                        WheelPicker(minutes, 0..59, {
                            minutes = it
                            onTimeChange((hours * 3600 + it * 60 + seconds).takeIf { t -> t > 0 })
                        }, Modifier.weight(1f)) { it.toString().padStart(2, '0') }
                        WheelPicker(seconds, 0..59, {
                            seconds = it
                            onTimeChange((hours * 3600 + minutes * 60 + it).takeIf { t -> t > 0 })
                        }, Modifier.weight(1f)) { it.toString().padStart(2, '0') }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CaloriesGoal(step: TrainStep?, onCaloriesChange: (Int?) -> Unit, isEditMode: Boolean) {
    var calories by remember(step?.caloriesValue) {
        mutableIntStateOf(((step?.caloriesValue ?: 0) / 50 * 50).coerceIn(0, 2000))
    }
    var expanded by remember { mutableStateOf(false) }
    val displayText = if (calories > 0) "$calories kcal" else "未设置"

    SectionCard {
        ExpandableResultRow(
            title = "目标热量",
            value = displayText,
            expandable = isEditMode,
            expanded = expanded && isEditMode,
            onClick = { if (isEditMode) expanded = !expanded }
        )
        AnimatedVisibility(visible = expanded && isEditMode) {
            Column {
                HorizontalDivider(Modifier.padding(vertical = 10.dp), color = RunTheme.colorScheme.divider.copy(alpha = 0.6f))
                WheelPickerGroup {
                    WheelPicker(calories, 0..2000, {
                        calories = it
                        onCaloriesChange(it.takeIf { v -> v > 0 })
                    }, Modifier.fillMaxWidth(), step = 50) { "$it kcal" }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PacerGoal(
    step: TrainStep?,
    onPacerFullChange: (distanceMeters: Double?, distanceUnit: String, timeSeconds: Int?, paceSecondsPerKm: Int?) -> Unit,
    isEditMode: Boolean
) {
    val initDistUnit = step?.distanceUnit ?: "KM"
    val initDistVal = step?.distanceValue ?: 5.0
    val initDistMajor = initDistVal.toInt().coerceIn(0, 100)
    val initDistMinor = ((initDistVal - initDistVal.toInt()) * 100).toInt().coerceIn(0, 99)
    val initTimeTotal = step?.timeGoalSeconds ?: 1800
    val initPaceSec = step?.minPace ?: 360

    var distanceUnit by remember { mutableStateOf(initDistUnit) }
    var distanceKmMajor by remember { mutableIntStateOf(initDistMajor) }
    var distanceKmMinor by remember { mutableIntStateOf(initDistMinor) }
    var distanceMetersVal by remember { mutableIntStateOf(((initDistVal * 1000).toInt() / 10 * 10).coerceIn(10, 1000)) }
    var hours by remember { mutableIntStateOf((initTimeTotal / 3600).coerceIn(0, 23)) }
    var minutes by remember { mutableIntStateOf(((initTimeTotal % 3600) / 60).coerceIn(0, 59)) }
    var seconds by remember { mutableIntStateOf((initTimeTotal % 60).coerceIn(0, 59)) }
    var paceMinute by remember { mutableIntStateOf((initPaceSec / 60).coerceIn(2, 23)) }
    var paceSecond by remember { mutableIntStateOf((initPaceSec % 60).coerceIn(0, 59)) }
    var expandedSection by remember { mutableStateOf<String?>(null) }

    fun currentDistKm(): Double = if (distanceUnit == "M") distanceMetersVal / 1000.0
    else distanceKmMajor + distanceKmMinor / 100.0

    fun currentDistMeters(): Double = if (distanceUnit == "M") distanceMetersVal.toDouble()
    else currentDistKm() * 1000.0

    fun currentTimeSec(): Int = hours * 3600 + minutes * 60 + seconds

    fun recalcPaceFromDistTime() {
        val distKm = currentDistKm()
        val totalSec = currentTimeSec()
        if (distKm > 0 && totalSec > 0) {
            val newPaceSec = (totalSec / distKm).toInt()
            paceMinute = (newPaceSec / 60).coerceIn(2, 23)
            paceSecond = (newPaceSec % 60).coerceIn(0, 59)
        }
    }

    fun recalcTimeFromDistPace() {
        val distKm = currentDistKm()
        val newPaceSec = paceMinute * 60 + paceSecond
        if (distKm > 0 && newPaceSec > 0) {
            val newTotalSec = (distKm * newPaceSec).toInt()
            hours = (newTotalSec / 3600).coerceIn(0, 23)
            minutes = ((newTotalSec % 3600) / 60).coerceIn(0, 59)
            seconds = (newTotalSec % 60).coerceIn(0, 59)
        }
    }

    fun notifyChange() {
        onPacerFullChange(
            currentDistMeters(),
            distanceUnit,
            currentTimeSec().takeIf { it > 0 },
            (paceMinute * 60 + paceSecond).takeIf { it > 0 }
        )
    }

    val distDisplayText = if (distanceUnit == "M") "${distanceMetersVal} m"
    else "${distanceKmMajor}.${distanceKmMinor.toString().padStart(2, '0')} km"
    val timeDisplayText = "%02d:%02d:%02d".format(hours, minutes, seconds)
    val paceDisplayText = "${paceMinute}'${paceSecond.toString().padStart(2, '0')}\"/km"

    // 距离卡片
    SectionCard {
        ExpandableResultRow(
            title = "距离",
            value = distDisplayText,
            expandable = isEditMode,
            expanded = expandedSection == "distance" && isEditMode,
            onClick = { if (isEditMode) expandedSection = if (expandedSection == "distance") null else "distance" }
        )
        AnimatedVisibility(visible = expandedSection == "distance" && isEditMode) {
            Column {
                HorizontalDivider(Modifier.padding(vertical = 10.dp), color = RunTheme.colorScheme.divider.copy(alpha = 0.6f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("单位选择")
                    Spacer(Modifier.weight(1f))
                    SegmentedChoiceGroup(
                        options = listOf("KM" to "公里", "M" to "米"),
                        selected = distanceUnit,
                        onSelected = { distanceUnit = it; recalcPaceFromDistTime(); notifyChange() }
                    )
                }
                HorizontalDivider(Modifier.padding(vertical = 10.dp), color = RunTheme.colorScheme.divider.copy(alpha = 0.6f))
                if (distanceUnit == "KM") {
                    WheelPickerGroup {
                        Row {
                            WheelPicker(distanceKmMajor, 0..100, {
                                distanceKmMajor = it; recalcPaceFromDistTime(); notifyChange()
                            }, Modifier.weight(1f)) { it.toString() }
                            WheelPicker(distanceKmMinor, 0..99, {
                                distanceKmMinor = it; recalcPaceFromDistTime(); notifyChange()
                            }, Modifier.weight(1f)) { ".${it.toString().padStart(2, '0')}" }
                        }
                    }
                } else {
                    WheelPickerGroup {
                        WheelPicker(distanceMetersVal, 10..1000, {
                            distanceMetersVal = it; recalcPaceFromDistTime(); notifyChange()
                        }, Modifier.fillMaxWidth(), step = 10) { "${it}m" }
                    }
                }
            }
        }
    }

    // 时间卡片
    SectionCard {
        ExpandableResultRow(
            title = "时间",
            value = timeDisplayText,
            expandable = isEditMode,
            expanded = expandedSection == "time" && isEditMode,
            onClick = { if (isEditMode) expandedSection = if (expandedSection == "time") null else "time" }
        )
        AnimatedVisibility(visible = expandedSection == "time" && isEditMode) {
            Column {
                HorizontalDivider(Modifier.padding(vertical = 10.dp), color = RunTheme.colorScheme.divider.copy(alpha = 0.6f))
                WheelPickerGroup {
                    Row {
                        WheelPicker(hours, 0..23, {
                            hours = it; recalcPaceFromDistTime(); notifyChange()
                        }, Modifier.weight(1f)) { it.toString().padStart(2, '0') }
                        WheelPicker(minutes, 0..59, {
                            minutes = it; recalcPaceFromDistTime(); notifyChange()
                        }, Modifier.weight(1f)) { it.toString().padStart(2, '0') }
                        WheelPicker(seconds, 0..59, {
                            seconds = it; recalcPaceFromDistTime(); notifyChange()
                        }, Modifier.weight(1f)) { it.toString().padStart(2, '0') }
                    }
                }
            }
        }
    }

    // 配速卡片
    SectionCard {
        ExpandableResultRow(
            title = "配速",
            value = paceDisplayText,
            expandable = isEditMode,
            expanded = expandedSection == "pace" && isEditMode,
            onClick = { if (isEditMode) expandedSection = if (expandedSection == "pace") null else "pace" }
        )
        AnimatedVisibility(visible = expandedSection == "pace" && isEditMode) {
            Column {
                HorizontalDivider(Modifier.padding(vertical = 10.dp), color = RunTheme.colorScheme.divider.copy(alpha = 0.6f))
                WheelPickerGroup {
                    Row {
                        WheelPicker(paceMinute, 2..23, {
                            paceMinute = it; recalcTimeFromDistPace(); notifyChange()
                        }, Modifier.weight(1f)) { "$it'" }
                        WheelPicker(paceSecond, 0..59, {
                            paceSecond = it; recalcTimeFromDistPace(); notifyChange()
                        }, Modifier.weight(1f)) { "${it.toString().padStart(2, '0')}\"" }
                    }
                }
            }
        }
    }
}
