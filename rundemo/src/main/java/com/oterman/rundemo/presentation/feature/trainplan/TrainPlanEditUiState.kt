package com.oterman.rundemo.presentation.feature.trainplan

import com.oterman.rundemo.domain.model.*

data class EditingStepInfo(
    val blockIndex: Int,
    val stepIndex: Int?,
    val blockType: BlockType,
    val step: TrainStep
)

data class TrainPlanEditUiState(
    val planId: String? = null,
    val name: String = "",
    val scheduledDate: String? = null,
    val finishFlag: String? = "N",
    val locationType: LocationType = LocationType.OUTDOOR,
    val trainWholeType: TrainWholeType = TrainWholeType.SELF_DEFINE,
    val description: String = "",
    val hardLevel: Int? = 2,
    // Structured training blocks
    val warmupBlock: TrainBlock? = null,
    val mainBlocks: List<TrainBlock> = emptyList(),
    val cooldownBlock: TrainBlock? = null,
    // Single goal steps
    val distanceGoalStep: TrainStep? = null,
    val timeGoalStep: TrainStep? = null,
    val calGoalStep: TrainStep? = null,
    val pacerGoalStep: TrainStep? = null,
    // UI state
    val isNewPlan: Boolean = true,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null,
    val editingStep: EditingStepInfo? = null,
    val showDatePicker: Boolean = false,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isEditMode: Boolean = true,
    val successMessage: String? = null,
    // Plan actions
    val workoutId: String? = null,
    val isDeleting: Boolean = false,
    val deleteSuccess: Boolean = false,
    val isPushing: Boolean = false,
    val isDeletingPush: Boolean = false,
    val userVdot: Double? = null
)
