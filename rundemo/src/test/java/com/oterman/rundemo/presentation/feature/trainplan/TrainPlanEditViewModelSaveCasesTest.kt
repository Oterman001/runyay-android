package com.oterman.rundemo.presentation.feature.trainplan

import com.oterman.rundemo.domain.model.BlockType
import com.oterman.rundemo.domain.model.IntensityType
import com.oterman.rundemo.domain.model.LocationType
import com.oterman.rundemo.domain.model.TrainBlock
import com.oterman.rundemo.domain.model.TrainGoalType
import com.oterman.rundemo.domain.model.TrainStep
import com.oterman.rundemo.domain.model.TrainWholeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class TrainPlanEditViewModelSaveCasesTest {

    @Test
    fun `buildTrainPlanForSave keeps required blocks for all course types`() {
        TrainWholeType.entries.forEach { type ->
            val plan = buildTrainPlanForSave(baseState(type))

            assertNotNull("warmupBlock should exist for $type", plan.warmupBlock)
            assertTrue("main block should exist for $type", plan.blockList.isNotEmpty())
            assertNotNull("cooldownBlock should exist for $type", plan.cooldownBlock)
            assertTrue("all steps should have an intensity for $type", plan.allSteps().all { it.intensityType != null })
        }
    }

    @Test
    fun `buildTrainPlanForSave creates single goal defaults by course type`() {
        val distance = buildTrainPlanForSave(baseState(TrainWholeType.DISTANCE))
        assertEquals(TrainGoalType.DISTANCE, distance.distanceGoalStep?.goalType)
        assertEquals(5.0, distance.distanceGoalStep?.distanceValue ?: 0.0, 0.001)

        val time = buildTrainPlanForSave(baseState(TrainWholeType.TIME))
        assertEquals(TrainGoalType.TIME, time.timeGoalStep?.goalType)
        assertEquals(1800, time.timeGoalStep?.timeGoalSeconds)

        val calories = buildTrainPlanForSave(baseState(TrainWholeType.CALORIES))
        assertEquals(TrainGoalType.CALORIES, calories.calGoalStep?.goalType)
        assertEquals(300, calories.calGoalStep?.caloriesValue)

        val pacer = buildTrainPlanForSave(baseState(TrainWholeType.PACER))
        assertEquals(TrainGoalType.PACER, pacer.pacerGoalStep?.goalType)
        assertEquals(5.0, pacer.pacerGoalStep?.distanceValue ?: 0.0, 0.001)
        assertEquals(1800, pacer.pacerGoalStep?.timeGoalSeconds)
    }

    @Test
    fun `buildTrainPlanForSave preserves step goal and intensity combinations`() {
        val state = baseState(TrainWholeType.SELF_DEFINE).copy(
            mainBlocks = listOf(
                block(
                    seq = 4,
                    steps = listOf(
                        step(0, TrainGoalType.DISTANCE, IntensityType.HEART_RATE),
                        step(1, TrainGoalType.TIME, IntensityType.SPEED),
                        step(2, TrainGoalType.OPEN, IntensityType.NONE)
                    )
                )
            )
        )

        val plan = buildTrainPlanForSave(state)
        val mainSteps = plan.blockList.first().stepList

        assertEquals(listOf(0, 1, 2), mainSteps.map { it.seq })
        assertEquals(
            listOf(TrainGoalType.DISTANCE, TrainGoalType.TIME, TrainGoalType.OPEN),
            mainSteps.map { it.goalType }
        )
        assertEquals(
            listOf(IntensityType.HEART_RATE, IntensityType.SPEED, IntensityType.NONE),
            mainSteps.map { it.intensityType }
        )
    }

    private fun baseState(type: TrainWholeType): TrainPlanEditUiState =
        TrainPlanEditUiState(
            planId = UUID.randomUUID().toString(),
            name = "$type 新增课程",
            scheduledDate = "2026-05-20",
            locationType = LocationType.OUTDOOR,
            trainWholeType = type,
            hardLevel = 2
        )

    private fun block(seq: Int, steps: List<TrainStep>): TrainBlock =
        TrainBlock(
            blockId = UUID.randomUUID().toString(),
            blockType = BlockType.MAIN,
            seq = seq,
            loopCnt = 2,
            stepList = steps
        )

    private fun step(seq: Int, goalType: TrainGoalType, intensityType: IntensityType): TrainStep =
        TrainStep(
            stepId = UUID.randomUUID().toString(),
            seq = seq,
            descName = goalType.name,
            purpose = "training",
            goalType = goalType,
            distanceValue = if (goalType == TrainGoalType.DISTANCE) 1.2 else null,
            timeGoalSeconds = if (goalType == TrainGoalType.TIME) 600 else null,
            intensityType = intensityType,
            minHeartRate = if (intensityType == IntensityType.HEART_RATE) 130 else null,
            maxHeartRate = if (intensityType == IntensityType.HEART_RATE) 160 else null,
            minPace = if (intensityType == IntensityType.SPEED) 300 else null,
            maxPace = if (intensityType == IntensityType.SPEED) 390 else null
        )
}

private fun com.oterman.rundemo.domain.model.TrainPlan.allSteps(): List<TrainStep> =
    listOfNotNull(warmupBlock, cooldownBlock).flatMap { it.stepList } +
        blockList.flatMap { it.stepList } +
        listOfNotNull(calGoalStep, distanceGoalStep, timeGoalStep, pacerGoalStep)
