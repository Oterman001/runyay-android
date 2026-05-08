package com.oterman.rundemo.presentation.feature.trainplan.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.oterman.rundemo.domain.model.TrainGoalType
import com.oterman.rundemo.domain.model.IntensityType
import com.oterman.rundemo.domain.model.TrainStep

@Composable
fun TrainStepRow(
    step: TrainStep,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Goal icon
        val goalIcon = when (step.goalType) {
            TrainGoalType.DISTANCE -> Icons.Outlined.DirectionsRun
            TrainGoalType.TIME -> Icons.Outlined.Timer
            TrainGoalType.CALORIES -> Icons.Outlined.FavoriteBorder
            TrainGoalType.PACER -> Icons.Outlined.Speed
        }
        Icon(
            goalIcon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Goal value
            Text(
                text = formatGoalText(step),
                style = MaterialTheme.typography.bodyMedium
            )

            // Intensity
            val intensityText = formatIntensityText(step)
            if (intensityText != null) {
                Text(
                    text = intensityText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
            Icon(
                Icons.Default.Close,
                contentDescription = "删除",
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

private fun formatGoalText(step: TrainStep): String {
    return when (step.goalType) {
        TrainGoalType.DISTANCE -> {
            val value = step.distanceValue ?: 0.0
            val unit = if (step.distanceUnit == "M") "m" else "km"
            "${formatNumber(value)} $unit"
        }
        TrainGoalType.TIME -> {
            val seconds = step.timeGoalSeconds ?: 0
            val min = seconds / 60
            val sec = seconds % 60
            if (sec > 0) "${min}分${sec}秒" else "${min}分钟"
        }
        TrainGoalType.CALORIES -> {
            "${step.caloriesValue ?: 0} kcal"
        }
        TrainGoalType.PACER -> {
            val min = step.minPace?.let { formatPaceValue(it) } ?: "--"
            val max = step.maxPace?.let { formatPaceValue(it) } ?: "--"
            "$min ~ $max /km"
        }
    }
}

private fun formatIntensityText(step: TrainStep): String? {
    val type = step.intensityType ?: return null
    return when (type) {
        IntensityType.HEART_RATE -> {
            val zone = step.heartZoneType
            val range = if (step.minHeartRate != null && step.maxHeartRate != null) {
                "${step.minHeartRate}-${step.maxHeartRate} bpm"
            } else null
            listOfNotNull(zone, range).joinToString(" ").takeIf { it.isNotBlank() }
        }
        IntensityType.SPEED -> {
            val min = step.minPace?.let { formatPaceValue(it) }
            val max = step.maxPace?.let { formatPaceValue(it) }
            if (min != null && max != null) "$min ~ $max /km" else null
        }
    }
}

private fun formatPaceValue(seconds: Int): String {
    val min = seconds / 60
    val sec = seconds % 60
    return "$min:${sec.toString().padStart(2, '0')}"
}

private fun formatNumber(value: Double): String {
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        "%.1f".format(value)
    }
}
