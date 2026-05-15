package com.oterman.rundemo.presentation.feature.trainplan

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.local.dao.OverallVdotDao
import com.oterman.rundemo.data.repository.TrainPlanRepository
import com.oterman.rundemo.domain.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class TrainPlanEditViewModel(
    private val repository: TrainPlanRepository,
    private val overallVdotDao: OverallVdotDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrainPlanEditUiState())
    val uiState: StateFlow<TrainPlanEditUiState> = _uiState.asStateFlow()

    fun init(planId: String?, date: String?) {
        if (planId != null) {
            _uiState.update { it.copy(isNewPlan = false, planId = planId, isEditMode = false) }
            loadPlan(planId)
        } else {
            _uiState.update {
                it.copy(
                    isNewPlan = true,
                    isEditMode = true,
                    planId = UUID.randomUUID().toString(),
                    scheduledDate = date,
                    name = generateDefaultName(date),
                    warmupBlock = createBlock(BlockType.WARMUP, 0, "热身", "WARMUP", TrainGoalType.TIME),
                    cooldownBlock = createBlock(BlockType.COOLDOWN, 99, "放松", "COOLDOWN", TrainGoalType.TIME)
                )
            }
        }
        loadUserVdot()
    }

    private fun loadUserVdot() {
        viewModelScope.launch {
            val vdot = overallVdotDao.getLatestVdot()?.value?.takeIf { it > 0.0 }
            _uiState.update { it.copy(userVdot = vdot) }
        }
    }

    private fun generateDefaultName(date: String?): String {
        val localDate = runCatching {
            LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        }.getOrElse { LocalDate.now() }
        return "${localDate.monthValue}.${localDate.dayOfMonth}日训练"
    }

    // ==================== Basic Info ====================

    fun onNameChange(name: String) {
        if (!_uiState.value.isEditMode) return
        _uiState.update { it.copy(name = name) }
    }

    fun onDateChange(date: String) {
        if (!_uiState.value.isEditMode) return
        _uiState.update { it.copy(scheduledDate = date, showDatePicker = false) }
    }

    fun onLocationTypeChange(type: LocationType) {
        if (!_uiState.value.isEditMode) return
        _uiState.update { it.copy(locationType = type) }
    }

    fun onTrainWholeTypeChange(type: TrainWholeType) {
        if (!_uiState.value.isEditMode) return
        _uiState.update { it.copy(trainWholeType = type) }
    }

    fun onDescriptionChange(desc: String) {
        if (!_uiState.value.isEditMode) return
        _uiState.update { it.copy(description = desc) }
    }

    fun toggleDatePicker(show: Boolean) {
        if (show && !_uiState.value.isEditMode) return
        _uiState.update { it.copy(showDatePicker = show) }
    }

    fun enterEditMode() {
        if (canEditCurrentPlan()) {
            _uiState.update { it.copy(isEditMode = true) }
        }
    }

    fun canEditCurrentPlan(): Boolean {
        val state = _uiState.value
        if (state.isNewPlan) return true
        if (state.finishFlag != null && state.finishFlag != "N") return false
        val date = state.scheduledDate ?: return true
        return runCatching {
            java.time.LocalDate.parse(date).isBefore(java.time.LocalDate.now()).not()
        }.getOrDefault(true)
    }

    // ==================== Block Operations ====================

    fun addWarmupBlock() {
        if (!_uiState.value.isEditMode) return
        if (_uiState.value.warmupBlock != null) return
        _uiState.update {
            it.copy(warmupBlock = TrainBlock(
                blockId = UUID.randomUUID().toString(),
                blockType = BlockType.WARMUP,
                seq = 0,
                loopCnt = 1,
                stepList = listOf(createDefaultStep(TrainGoalType.TIME, "热身", "WARMUP"))
            ))
        }
    }

    private fun createBlock(
        blockType: BlockType,
        seq: Int,
        descName: String,
        purpose: String,
        goalType: TrainGoalType,
        loopCnt: Int = 1
    ): TrainBlock = TrainBlock(
        blockId = UUID.randomUUID().toString(),
        blockType = blockType,
        seq = seq,
        loopCnt = loopCnt,
        stepList = listOf(createDefaultStep(goalType, descName, purpose))
    )

    fun addMainBlock() {
        if (!_uiState.value.isEditMode) return
        _uiState.update {
            val blocks = it.mainBlocks.toMutableList()
            blocks.add(TrainBlock(
                blockId = UUID.randomUUID().toString(),
                blockType = BlockType.MAIN,
                seq = blocks.size + 1,
                loopCnt = 1,
                stepList = listOf(createDefaultStep(TrainGoalType.DISTANCE, "训练", "WORK"))
            ))
            it.copy(mainBlocks = blocks)
        }
    }

    fun addRecoveryBlock() {
        if (!_uiState.value.isEditMode) return
        _uiState.update {
            val blocks = it.mainBlocks.toMutableList()
            blocks.add(TrainBlock(
                blockId = UUID.randomUUID().toString(),
                blockType = BlockType.MAIN,
                seq = blocks.size + 1,
                loopCnt = 1,
                stepList = listOf(createDefaultStep(TrainGoalType.TIME, "恢复", "RECOVERY"))
            ))
            it.copy(mainBlocks = blocks)
        }
    }

    fun addIntervalBlock() {
        if (!_uiState.value.isEditMode) return
        _uiState.update {
            val blocks = it.mainBlocks.toMutableList()
            blocks.add(TrainBlock(
                blockId = UUID.randomUUID().toString(),
                blockType = BlockType.MAIN,
                seq = blocks.size + 1,
                loopCnt = 2,
                stepList = listOf(
                    createDefaultStep(TrainGoalType.DISTANCE, "训练", "WORK"),
                    createDefaultStep(TrainGoalType.TIME, "恢复", "RECOVERY")
                )
            ))
            it.copy(mainBlocks = blocks)
        }
    }

    fun addCooldownBlock() {
        if (!_uiState.value.isEditMode) return
        if (_uiState.value.cooldownBlock != null) return
        _uiState.update {
            it.copy(cooldownBlock = TrainBlock(
                blockId = UUID.randomUUID().toString(),
                blockType = BlockType.COOLDOWN,
                seq = 99,
                loopCnt = 1,
                stepList = listOf(createDefaultStep(TrainGoalType.TIME, "放松", "COOLDOWN"))
            ))
        }
    }

    fun removeBlock(blockType: BlockType, blockIndex: Int) {
        if (!_uiState.value.isEditMode) return
        _uiState.update {
            when (blockType) {
                BlockType.WARMUP -> it.copy(warmupBlock = null)
                BlockType.COOLDOWN -> it.copy(cooldownBlock = null)
                BlockType.MAIN -> {
                    val blocks = it.mainBlocks.toMutableList()
                    if (blockIndex in blocks.indices) blocks.removeAt(blockIndex)
                    it.copy(mainBlocks = blocks)
                }
            }
        }
    }

    fun updateLoopCount(blockIndex: Int, delta: Int) {
        if (!_uiState.value.isEditMode) return
        _uiState.update {
            val blocks = it.mainBlocks.toMutableList()
            if (blockIndex in blocks.indices) {
                val block = blocks[blockIndex]
                val newCount = (block.loopCnt + delta).coerceIn(1, 99)
                blocks[blockIndex] = block.copy(loopCnt = newCount)
            }
            it.copy(mainBlocks = blocks)
        }
    }

    // ==================== Step Operations ====================

    fun addStepToBlock(blockType: BlockType, blockIndex: Int) {
        if (!_uiState.value.isEditMode) return
        _uiState.update {
            when (blockType) {
                BlockType.WARMUP -> {
                    val block = it.warmupBlock ?: return@update it
                    it.copy(warmupBlock = block.copy(
                        stepList = block.stepList + createDefaultStep(TrainGoalType.TIME, "热身", "WARMUP")
                    ))
                }
                BlockType.COOLDOWN -> {
                    val block = it.cooldownBlock ?: return@update it
                    it.copy(cooldownBlock = block.copy(
                        stepList = block.stepList + createDefaultStep(TrainGoalType.TIME, "放松", "COOLDOWN")
                    ))
                }
                BlockType.MAIN -> {
                    val blocks = it.mainBlocks.toMutableList()
                    if (blockIndex in blocks.indices) {
                        val block = blocks[blockIndex]
                        blocks[blockIndex] = block.copy(
                            stepList = block.stepList + createDefaultStep(TrainGoalType.DISTANCE, "训练", "WORK")
                        )
                    }
                    it.copy(mainBlocks = blocks)
                }
            }
        }
    }

    fun removeStep(blockType: BlockType, blockIndex: Int, stepIndex: Int) {
        if (!_uiState.value.isEditMode) return
        _uiState.update {
            when (blockType) {
                BlockType.WARMUP -> {
                    val block = it.warmupBlock ?: return@update it
                    val steps = block.stepList.toMutableList()
                    if (stepIndex in steps.indices) steps.removeAt(stepIndex)
                    if (steps.isEmpty()) it.copy(warmupBlock = null)
                    else it.copy(warmupBlock = block.copy(stepList = steps))
                }
                BlockType.COOLDOWN -> {
                    val block = it.cooldownBlock ?: return@update it
                    val steps = block.stepList.toMutableList()
                    if (stepIndex in steps.indices) steps.removeAt(stepIndex)
                    if (steps.isEmpty()) it.copy(cooldownBlock = null)
                    else it.copy(cooldownBlock = block.copy(stepList = steps))
                }
                BlockType.MAIN -> {
                    val blocks = it.mainBlocks.toMutableList()
                    if (blockIndex in blocks.indices) {
                        val block = blocks[blockIndex]
                        val steps = block.stepList.toMutableList()
                        if (stepIndex in steps.indices) steps.removeAt(stepIndex)
                        if (steps.isEmpty()) blocks.removeAt(blockIndex)
                        else blocks[blockIndex] = block.copy(stepList = steps)
                    }
                    it.copy(mainBlocks = blocks)
                }
            }
        }
    }

    fun openStepEditor(blockType: BlockType, blockIndex: Int, stepIndex: Int) {
        if (!_uiState.value.isEditMode) return
        val state = _uiState.value
        val step = when (blockType) {
            BlockType.WARMUP -> state.warmupBlock?.stepList?.getOrNull(stepIndex)
            BlockType.COOLDOWN -> state.cooldownBlock?.stepList?.getOrNull(stepIndex)
            BlockType.MAIN -> state.mainBlocks.getOrNull(blockIndex)?.stepList?.getOrNull(stepIndex)
        } ?: return
        _uiState.update {
            it.copy(editingStep = EditingStepInfo(blockIndex, stepIndex, blockType, step))
        }
    }

    fun saveStepEdit(updatedStep: TrainStep) {
        if (!_uiState.value.isEditMode) return
        val editing = _uiState.value.editingStep ?: return
        _uiState.update {
            val result = when (editing.blockType) {
                BlockType.WARMUP -> {
                    val block = it.warmupBlock ?: return@update it
                    val steps = block.stepList.toMutableList()
                    if (editing.stepIndex != null && editing.stepIndex in steps.indices) {
                        steps[editing.stepIndex] = updatedStep
                    }
                    it.copy(warmupBlock = block.copy(stepList = steps))
                }
                BlockType.COOLDOWN -> {
                    val block = it.cooldownBlock ?: return@update it
                    val steps = block.stepList.toMutableList()
                    if (editing.stepIndex != null && editing.stepIndex in steps.indices) {
                        steps[editing.stepIndex] = updatedStep
                    }
                    it.copy(cooldownBlock = block.copy(stepList = steps))
                }
                BlockType.MAIN -> {
                    val blocks = it.mainBlocks.toMutableList()
                    if (editing.blockIndex in blocks.indices) {
                        val block = blocks[editing.blockIndex]
                        val steps = block.stepList.toMutableList()
                        if (editing.stepIndex != null && editing.stepIndex in steps.indices) {
                            steps[editing.stepIndex] = updatedStep
                        }
                        blocks[editing.blockIndex] = block.copy(stepList = steps)
                    }
                    it.copy(mainBlocks = blocks)
                }
            }
            result.copy(editingStep = null)
        }
    }

    fun dismissStepEditor() {
        _uiState.update { it.copy(editingStep = null) }
    }

    fun moveMainBlock(fromIndex: Int, toIndex: Int) {
        _uiState.update {
            if (!it.isEditMode) return@update it
            val blocks = it.mainBlocks.toMutableList()
            if (fromIndex !in blocks.indices || toIndex !in blocks.indices || fromIndex == toIndex) {
                return@update it
            }
            val item = blocks.removeAt(fromIndex)
            blocks.add(toIndex, item)
            it.copy(mainBlocks = blocks.resequenceBlocks())
        }
    }

    fun moveStep(blockType: BlockType, blockIndex: Int, fromIndex: Int, toIndex: Int) {
        if (!_uiState.value.isEditMode) return
        _uiState.update {
            when (blockType) {
                BlockType.MAIN -> {
                    val blocks = it.mainBlocks.toMutableList()
                    if (blockIndex in blocks.indices) {
                        val block = blocks[blockIndex]
                        val steps = block.stepList.toMutableList()
                        if (fromIndex in steps.indices && toIndex in steps.indices && fromIndex != toIndex) {
                            val item = steps.removeAt(fromIndex)
                            steps.add(toIndex, item)
                            blocks[blockIndex] = block.copy(stepList = steps)
                        }
                    }
                    it.copy(mainBlocks = blocks)
                }
                BlockType.WARMUP, BlockType.COOLDOWN -> it
            }
        }
    }

    // ==================== Single Goal ====================

    fun updateDistanceGoal(value: Double?, unit: String = "KM") {
        if (!_uiState.value.isEditMode) return
        _uiState.update {
            it.copy(distanceGoalStep = TrainStep(
                stepId = it.distanceGoalStep?.stepId ?: UUID.randomUUID().toString(),
                goalType = TrainGoalType.DISTANCE,
                distanceUnit = unit,
                distanceValue = value
            ))
        }
    }

    fun updateTimeGoal(seconds: Int?) {
        if (!_uiState.value.isEditMode) return
        _uiState.update {
            it.copy(timeGoalStep = TrainStep(
                stepId = it.timeGoalStep?.stepId ?: UUID.randomUUID().toString(),
                goalType = TrainGoalType.TIME,
                timeGoalSeconds = seconds
            ))
        }
    }

    fun updateCaloriesGoal(value: Int?, unit: String = "KCAL") {
        if (!_uiState.value.isEditMode) return
        _uiState.update {
            it.copy(calGoalStep = TrainStep(
                stepId = it.calGoalStep?.stepId ?: UUID.randomUUID().toString(),
                goalType = TrainGoalType.CALORIES,
                caloriesUnit = unit,
                caloriesValue = value
            ))
        }
    }

    fun updatePacerFull(
        distanceMeters: Double?,
        distanceUnit: String,
        timeSeconds: Int?,
        paceSecondsPerKm: Int?
    ) {
        if (!_uiState.value.isEditMode) return
        _uiState.update {
            val distVal = when {
                distanceMeters == null -> null
                distanceUnit == "M" -> distanceMeters
                else -> distanceMeters / 1000.0
            }
            it.copy(pacerGoalStep = TrainStep(
                stepId = it.pacerGoalStep?.stepId ?: UUID.randomUUID().toString(),
                goalType = TrainGoalType.PACER,
                distanceUnit = distanceUnit,
                distanceValue = distVal,
                timeGoalSeconds = timeSeconds,
                minPace = paceSecondsPerKm,
                maxPace = paceSecondsPerKm
            ))
        }
    }

    // ==================== Save ====================

    fun savePlan() {
        val state = _uiState.value
        if (!state.isEditMode) return
        if (state.name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "请输入训练名称") }
            return
        }
        if (state.trainWholeType == TrainWholeType.SELF_DEFINE && state.mainBlocks.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "还没添加训练内容呢！") }
            return
        }
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            val plan = buildTrainPlan()
            val result = repository.savePlan(plan)
            result.onSuccess {
                _uiState.update {
                    if (state.isNewPlan) {
                        it.copy(isSaving = false, saveSuccess = true)
                    } else {
                        it.copy(isSaving = false, isEditMode = false)
                    }
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isSaving = false, errorMessage = e.message ?: "保存失败") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun deletePlan() {
        val planId = _uiState.value.planId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true) }
            repository.deletePlans(listOf(planId))
                .onSuccess { _uiState.update { it.copy(isDeleting = false, deleteSuccess = true) } }
                .onFailure { e -> _uiState.update { it.copy(isDeleting = false, errorMessage = e.message ?: "删除失败") } }
        }
    }

    fun pushPlan(platformCode: String) {
        val planId = _uiState.value.planId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isPushing = true) }
            repository.pushPlan(planId, platformCode)
                .onSuccess { result -> _uiState.update { it.copy(isPushing = false, workoutId = result.extWorkoutId) } }
                .onFailure { e -> _uiState.update { it.copy(isPushing = false, errorMessage = e.message ?: "推送失败") } }
        }
    }

    fun deletePushedPlan(platformCode: String) {
        val planId = _uiState.value.planId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingPush = true) }
            repository.deletePushedPlan(planId, platformCode)
                .onSuccess { _uiState.update { it.copy(isDeletingPush = false, workoutId = null) } }
                .onFailure { e -> _uiState.update { it.copy(isDeletingPush = false, errorMessage = e.message ?: "从手表删除失败") } }
        }
    }

    // ==================== Load (edit mode) ====================

    private fun loadPlan(planId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = repository.getPlanDetail(planId)
            result.onSuccess { plan ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        planId = plan.planId,
                        name = plan.name,
                        scheduledDate = plan.scheduledDate,
                        finishFlag = plan.finishFlag,
                        locationType = plan.locationType,
                        trainWholeType = plan.trainWholeType,
                        description = plan.description ?: "",
                        hardLevel = plan.hardLevel,
                        warmupBlock = plan.warmupBlock,
                        mainBlocks = plan.blockList,
                        cooldownBlock = plan.cooldownBlock,
                        distanceGoalStep = plan.distanceGoalStep,
                        timeGoalStep = plan.timeGoalStep,
                        calGoalStep = plan.calGoalStep,
                        pacerGoalStep = plan.pacerGoalStep,
                        workoutId = plan.workoutId
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    private fun buildTrainPlan(): TrainPlan {
        val state = _uiState.value
        return TrainPlan(
            planId = state.planId,
            name = state.name,
            description = state.description.takeIf { it.isNotBlank() },
            trainWholeType = state.trainWholeType,
            scheduledDate = state.scheduledDate,
            hardLevel = state.hardLevel,
            finishFlag = state.finishFlag ?: "N",
            locationType = state.locationType,
            warmupBlock = if (state.trainWholeType == TrainWholeType.SELF_DEFINE) state.warmupBlock else null,
            blockList = if (state.trainWholeType == TrainWholeType.SELF_DEFINE) state.mainBlocks else emptyList(),
            cooldownBlock = if (state.trainWholeType == TrainWholeType.SELF_DEFINE) state.cooldownBlock else null,
            calGoalStep = if (state.trainWholeType == TrainWholeType.CALORIES) state.calGoalStep else null,
            distanceGoalStep = if (state.trainWholeType == TrainWholeType.DISTANCE) state.distanceGoalStep else null,
            timeGoalStep = if (state.trainWholeType == TrainWholeType.TIME) state.timeGoalStep else null,
            pacerGoalStep = if (state.trainWholeType == TrainWholeType.PACER) state.pacerGoalStep else null
        )
    }

    private fun createDefaultStep(
        goalType: TrainGoalType,
        descName: String = when (goalType) {
            TrainGoalType.TIME -> "热身"
            else -> "训练"
        },
        purpose: String? = null
    ) = TrainStep(
        stepId = UUID.randomUUID().toString(),
        seq = 0,
        descName = descName,
        purpose = purpose,
        warmupFlag = if (purpose == "WARMUP") "Y" else "N",
        cooldownFlag = if (purpose == "COOLDOWN") "Y" else "N",
        goalType = goalType,
        distanceValue = if (goalType == TrainGoalType.DISTANCE) 1.0 else null,
        timeGoalSeconds = if (goalType == TrainGoalType.TIME) 300 else null
    )
}

private fun List<TrainBlock>.resequenceBlocks(): List<TrainBlock> =
    mapIndexed { index, block ->
        block.copy(
            seq = index + 1,
            stepList = block.stepList.mapIndexed { stepIndex, step -> step.copy(seq = stepIndex) }
        )
    }

class TrainPlanEditViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TrainPlanEditViewModel::class.java)) {
            val prefs = PreferencesManager(context)
            val db = com.oterman.rundemo.data.local.database.RunDatabase.getInstance(context)
            val repository = TrainPlanRepository(prefs, localDao = db.trainPlanDao())
            return TrainPlanEditViewModel(repository, db.overallVdotDao()) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
