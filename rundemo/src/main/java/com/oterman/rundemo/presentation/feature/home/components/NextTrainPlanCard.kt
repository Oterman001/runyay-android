package com.oterman.rundemo.presentation.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.R
import com.oterman.rundemo.domain.model.BlockType
import com.oterman.rundemo.domain.model.TrainBlock
import com.oterman.rundemo.domain.model.TrainPlan
import com.oterman.rundemo.domain.model.TrainPlanSummary
import com.oterman.rundemo.domain.model.TrainWholeType
import com.oterman.rundemo.presentation.feature.trainplan.distanceMeters
import com.oterman.rundemo.presentation.feature.trainplan.estimateDistance
import com.oterman.rundemo.presentation.feature.trainplan.estimateSelfDefine
import com.oterman.rundemo.presentation.feature.trainplan.estimateTime
import com.oterman.rundemo.presentation.feature.trainplan.formatDistance
import com.oterman.rundemo.presentation.feature.trainplan.formatDurationColon
import com.oterman.rundemo.presentation.feature.trainplan.goalText
import com.oterman.rundemo.presentation.feature.trainplan.intensityText
import com.oterman.rundemo.ui.theme.SecondaryTextColor
import com.oterman.rundemo.ui.theme.StepTrainingColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val WarmupBarColor = Color(0xFF90CAF9)
private val CooldownBarColor = Color(0xFF81C784)

@Composable
fun NextTrainPlanCard(
    summary: TrainPlanSummary?,
    detail: TrainPlan?,
    modifier: Modifier = Modifier,
    onNavigateToCalendar: () -> Unit = {},
    onNavigateToEditPlan: (String) -> Unit = {}
) {
    StatisticsCard(modifier = modifier) {
        Column {
            // Header: title + date chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "训练安排",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.weight(1f))
                summary?.scheduledDate?.let { DateChip(it) }
            }

            if (summary == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "日程中无任何训练",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryTextColor
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(10.dp))

                // Plan name + completion / difficulty
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = summary.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (summary.finishFlag == "Y") {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF34C759),
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        summary.hardLevel?.let { HardLevelDots(it) }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Location row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isIndoor = summary.locationType == "indoor"
                    Icon(
                        painter = painterResource(
                            if (isIndoor) R.drawable.figure_run_treadmill
                            else R.drawable.figure_run_square_stack_fill
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = SecondaryTextColor
                    )
                    Text(
                        text = if (isIndoor) "室内" else "室外",
                        fontSize = 12.sp,
                        color = SecondaryTextColor
                    )
                }

                // Metrics row (requires detail)
                detail?.let { plan ->
                    val metrics = buildNextPlanMetrics(summary, plan)
                    if (metrics.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            metrics.forEach { metric ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(metric.iconRes),
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = metric.tint
                                    )
                                    Text(
                                        text = metric.value,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                                    )
                                }
                            }
                        }
                    }

                    // Structure bar
                    val structureBlocks = buildStructureBlocks(plan)
                    if (structureBlocks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        StructureBar(structureBlocks)
                        buildStructureSummaryText(plan)?.let { text ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = text,
                                fontSize = 12.sp,
                                color = SecondaryTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Bottom buttons
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onNavigateToCalendar,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("训练日历", fontSize = 13.sp)
                }

                VerticalDivider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )

                if (summary != null) {
                    TextButton(
                        onClick = { onNavigateToEditPlan(summary.planId) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("查看详情", fontSize = 13.sp)
                    }
                } else {
                    TextButton(
                        onClick = onNavigateToCalendar,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("添加新训练", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DateChip(scheduledDate: String) {
    val today = LocalDate.now()
    val date = parsePlanDate(scheduledDate) ?: return
    val (label, bgColor) = when {
        date == today -> "今天" to MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        date == today.plusDays(1) -> "明天" to MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        date.isBefore(today) -> "${date.monthValue}月${date.dayOfMonth}日" to MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
        else -> "${date.monthValue}月${date.dayOfMonth}日" to MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    }
    val textColor = when {
        date == today -> MaterialTheme.colorScheme.primary
        date.isBefore(today) -> MaterialTheme.colorScheme.error
        else -> SecondaryTextColor
    }
    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (date == today) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor
        )
    }
}

@Composable
private fun HardLevelDots(level: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        val primary = MaterialTheme.colorScheme.primary
        repeat(5) { index ->
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(
                        if (index < level) primary else primary.copy(alpha = 0.18f)
                    )
            )
        }
    }
}

@Composable
private fun StructureBar(blocks: List<StructureSegment>) {
    val total = blocks.fold(0f) { acc, seg -> acc + seg.weight }.coerceAtLeast(1f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
    ) {
        blocks.forEach { seg ->
            Box(
                modifier = Modifier
                    .weight(seg.weight / total)
                    .fillMaxHeight()
                    .background(seg.color)
            )
        }
    }
}

private data class StructureSegment(val color: Color, val weight: Float)

private data class NextPlanMetric(val iconRes: Int, val tint: Color, val value: String)

private fun buildStructureBlocks(plan: TrainPlan): List<StructureSegment> {
    val segments = mutableListOf<StructureSegment>()

    fun TrainBlock.durationWeight(): Float {
        val sec = stepList.sumOf { it.timeGoalSeconds ?: 0 } * loopCnt.coerceAtLeast(1)
        return sec.toFloat()
    }

    plan.warmupBlock?.let { block ->
        val w = block.durationWeight()
        if (w > 0f) segments.add(StructureSegment(WarmupBarColor, w))
    }
    plan.blockList.forEach { block ->
        val w = block.durationWeight()
        if (w > 0f) segments.add(StructureSegment(StepTrainingColor, w))
    }
    plan.cooldownBlock?.let { block ->
        val w = block.durationWeight()
        if (w > 0f) segments.add(StructureSegment(CooldownBarColor, w))
    }

    // Fall back to equal-weight segments when no time data
    if (segments.isEmpty()) {
        val allBlocks = buildList {
            plan.warmupBlock?.let { add(it to WarmupBarColor) }
            plan.blockList.forEach { add(it to StepTrainingColor) }
            plan.cooldownBlock?.let { add(it to CooldownBarColor) }
        }
        return allBlocks.map { (_, color) -> StructureSegment(color, 1f) }
    }

    return segments
}

private fun buildStructureSummaryText(plan: TrainPlan): String? {
    val mainBlock = plan.blockList.firstOrNull() ?: return null
    val loopPart = if (mainBlock.loopCnt > 1) "×${mainBlock.loopCnt} " else ""
    val firstStep = mainBlock.stepList.firstOrNull() ?: return null
    val goalPart = firstStep.goalText()
    val intensityPart = firstStep.intensityText()?.let { " @ $it" } ?: ""
    return "${loopPart}${goalPart}${intensityPart}"
}

private fun buildNextPlanMetrics(summary: TrainPlanSummary, detail: TrainPlan): List<NextPlanMetric> {
    return when (summary.trainWholeType) {
        TrainWholeType.DISTANCE -> {
            val distM = detail.distanceGoalStep?.distanceMeters()
                ?: estimateSelfDefine(detail.blockList, vdot = null).distanceMeters ?: 0.0
            val estimate = estimateDistance(distM, vdot = null)
            listOfNotNull(
                distM.takeIf { it > 0 }?.let { distanceMetric(formatDistance(it)) },
                estimate.durationSeconds?.let { timeMetric(formatDurationColon(it)) }
            )
        }
        TrainWholeType.TIME -> {
            val durSec = detail.timeGoalStep?.timeGoalSeconds
                ?: estimateSelfDefine(detail.blockList, vdot = null).durationSeconds ?: 0
            val estimate = estimateTime(durSec, vdot = null)
            listOfNotNull(
                durSec.takeIf { it > 0 }?.let { timeMetric(formatDurationColon(it)) },
                estimate.distanceMeters?.let { distanceMetric(formatDistance(it)) }
            )
        }
        TrainWholeType.CALORIES -> listOfNotNull(
            detail.calGoalStep?.goalText()?.let { caloriesMetric(it) }
        )
        TrainWholeType.PACER -> {
            val step = detail.pacerGoalStep
            listOfNotNull(
                step?.distanceMeters()?.takeIf { it > 0 }?.let { distanceMetric(formatDistance(it)) },
                step?.timeGoalSeconds?.takeIf { it > 0 }?.let { timeMetric(formatDurationColon(it)) }
            )
        }
        TrainWholeType.SELF_DEFINE -> {
            val estimate = estimateSelfDefine(detail.blockList, vdot = null)
            listOfNotNull(
                estimate.distanceMeters?.let { distanceMetric(formatDistance(it)) },
                estimate.durationSeconds?.let { timeMetric(formatDurationColon(it)) }
            )
        }
    }
}

private fun distanceMetric(value: String) =
    NextPlanMetric(iconRes = R.drawable.ic_goal_distance, tint = Color(0xFF1AA9F8), value = value)

private fun timeMetric(value: String) =
    NextPlanMetric(iconRes = R.drawable.ic_goal_time, tint = StepTrainingColor, value = value)

private fun caloriesMetric(value: String) =
    NextPlanMetric(iconRes = R.drawable.ic_goal_calories, tint = Color(0xFFE53935), value = value)

private fun parsePlanDate(value: String): LocalDate? = runCatching {
    if (value.contains("-")) LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE)
    else LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE)
}.getOrNull()
