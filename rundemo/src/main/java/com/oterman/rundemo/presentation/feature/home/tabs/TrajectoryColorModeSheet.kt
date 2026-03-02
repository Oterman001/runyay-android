package com.oterman.rundemo.presentation.feature.home.tabs

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.oterman.rundemo.ui.theme.RunTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.oterman.rundemo.presentation.components.trajectory.DistanceTier
import com.oterman.rundemo.presentation.components.trajectory.FixedTrackColors
import com.oterman.rundemo.presentation.components.trajectory.TrajectoryColorMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrajectoryColorModeSheet(
    currentMode: TrajectoryColorMode,
    onModeSelected: (TrajectoryColorMode) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Title
            Text(
                text = "轨迹配色",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // Mode cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModeCard(
                    title = "固定配色",
                    subtitle = "单一颜色轨迹",
                    isSelected = currentMode == TrajectoryColorMode.FIXED,
                    onClick = { onModeSelected(TrajectoryColorMode.FIXED) },
                    modifier = Modifier.weight(1f)
                )
                ModeCard(
                    title = "距离分色",
                    subtitle = "按距离自动配色",
                    isSelected = currentMode == TrajectoryColorMode.DISTANCE_BASED,
                    onClick = { onModeSelected(TrajectoryColorMode.DISTANCE_BASED) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Color rules section with animated content
            Text(
                text = "配色规则",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            AnimatedContent(
                targetState = currentMode,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "color_rules"
            ) { mode ->
                when (mode) {
                    TrajectoryColorMode.FIXED -> FixedColorRules()
                    TrajectoryColorMode.DISTANCE_BASED -> DistanceColorRules()
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "设置会自动保存",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ModeCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.03f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "mode_card_scale"
    )

    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent

    Column(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = isSelected,
                onClick = null,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 28.dp, top = 2.dp)
        )
    }
}

@Composable
private fun ColorSwatchHeader() {
    val isDark = RunTheme.isDark
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 6.dp)
    ) {
        Text(
            text = "亮色",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (!isDark) FontWeight.SemiBold else FontWeight.Normal,
            color = if (!isDark) activeColor else inactiveColor,
            modifier = Modifier.width(20.dp),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "暗色",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isDark) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isDark) activeColor else inactiveColor,
            modifier = Modifier.width(20.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FixedColorRules() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ColorSwatchHeader()
        ColorSwatchRow(
            lightColor = FixedTrackColors.LIGHT_TRACK,
            darkColor = FixedTrackColors.DARK_TRACK,
            label = "轨迹线"
        )
        ColorSwatchRow(
            lightColor = FixedTrackColors.LIGHT_START,
            darkColor = FixedTrackColors.DARK_START,
            label = "起点"
        )
        ColorSwatchRow(
            lightColor = FixedTrackColors.LIGHT_END,
            darkColor = FixedTrackColors.DARK_END,
            label = "终点"
        )
    }
}

@Composable
private fun DistanceColorRules() {
    val tiers = listOf(
        Triple(DistanceTier.T1_RECOVERY, "恢复跑", "\u22643km"),
        Triple(DistanceTier.T2_DAILY,    "日常跑", "3-5km"),
        Triple(DistanceTier.T3_LONG,     "长距离", "5-10km"),
        Triple(DistanceTier.T4_HALF_MARATHON, "半马", "10-21km"),
        Triple(DistanceTier.T5_MARATHON, "全马", "\u226521km")
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ColorSwatchHeader()
        tiers.forEach { (tier, label, range) ->
            ColorSwatchRow(
                lightColor = Color(tier.lightTrackColor),
                darkColor = Color(tier.darkTrackColor),
                label = label,
                rangeText = range
            )
        }
    }
}

@Composable
private fun ColorSwatchRow(
    lightColor: Color,
    darkColor: Color,
    label: String,
    rangeText: String? = null
) {
    val isDark = RunTheme.isDark

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Light swatch
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(lightColor)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Dark swatch
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(darkColor)
                    .then(
                        if (!isDark) Modifier.border(
                            0.5.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(4.dp)
                        ) else Modifier
                    )
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )

        if (rangeText != null) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = rangeText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
