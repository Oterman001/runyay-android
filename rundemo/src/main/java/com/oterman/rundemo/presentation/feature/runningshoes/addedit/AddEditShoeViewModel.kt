package com.oterman.rundemo.presentation.feature.runningshoes.addedit

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.repository.RunningShoeRepository
import com.oterman.rundemo.domain.model.RunningShoe
import com.oterman.rundemo.util.RLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AddEditShoeViewModel(
    private val context: Context,
    private val shoeId: String? = null,
    private val repository: RunningShoeRepository = RunningShoeRepository(context),
    private val preferencesManager: PreferencesManager = PreferencesManager(context)
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditShoeUiState(isEditMode = shoeId != null))
    val uiState: StateFlow<AddEditShoeUiState> = _uiState.asStateFlow()

    init {
        if (shoeId != null) {
            loadShoe(shoeId)
        }
    }

    private fun loadShoe(id: String) {
        viewModelScope.launch {
            val shoe = repository.getShoe(id) ?: return@launch
            _uiState.update {
                it.copy(
                    brand = shoe.brand ?: "",
                    model = shoe.model ?: "",
                    nickname = shoe.nickname ?: "",
                    shoeSize = shoe.shoeSize ?: "",
                    price = shoe.price?.toString() ?: "",
                    expectedLifespan = shoe.expectedLifespan.toInt().toString(),
                    initialDistance = shoe.initialDistance.toInt().toString(),
                    firstUseDate = shoe.firstUseDate,
                    notes = shoe.notes ?: "",
                    isDefault = shoe.isDefault,
                    existingLocalImagePath = shoe.localImagePath,
                    existingImageUrl = shoe.imageUrl,
                    existingImagePath = shoe.imagePath,
                    isEditMode = true
                )
            }
        }
    }

    fun onBrandChange(value: String) { _uiState.update { it.copy(brand = value) } }
    fun onModelChange(value: String) { _uiState.update { it.copy(model = value) } }
    fun onNicknameChange(value: String) { _uiState.update { it.copy(nickname = value) } }
    fun onShoeSizeChange(value: String) { _uiState.update { it.copy(shoeSize = value) } }
    fun onPriceChange(value: String) { _uiState.update { it.copy(price = value) } }
    fun onExpectedLifespanChange(value: String) { _uiState.update { it.copy(expectedLifespan = value) } }
    fun onInitialDistanceChange(value: String) { _uiState.update { it.copy(initialDistance = value) } }
    fun onFirstUseDateChange(value: Long?) { _uiState.update { it.copy(firstUseDate = value) } }
    fun onNotesChange(value: String) { _uiState.update { it.copy(notes = value) } }
    fun onIsDefaultChange(value: Boolean) { _uiState.update { it.copy(isDefault = value) } }
    fun onImageSelected(uri: Uri?) { _uiState.update { it.copy(selectedImageUri = uri) } }

    fun saveShoe() {
        val state = _uiState.value
        if (!state.isFormValid) {
            _uiState.update { it.copy(errorMessage = "品牌和型号为必填项") }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val userId = preferencesManager.getUserId() ?: ""
                val id = shoeId ?: repository.generateShoeId()
                val shoe = RunningShoe(
                    id = id,
                    userId = userId,
                    brand = state.brand.trim(),
                    model = state.model.trim(),
                    nickname = state.nickname.trim().takeIf { it.isNotEmpty() },
                    shoeSize = state.shoeSize.trim().takeIf { it.isNotEmpty() },
                    price = state.price.toDoubleOrNull(),
                    expectedLifespan = state.expectedLifespan.toDoubleOrNull() ?: 700.0,
                    initialDistance = state.initialDistance.toDoubleOrNull() ?: 0.0,
                    firstUseDate = state.firstUseDate,
                    notes = state.notes.trim().takeIf { it.isNotEmpty() },
                    isDefault = state.isDefault,
                    imageUrl = if (state.selectedImageUri == null) state.existingImageUrl else null,
                    imagePath = if (state.selectedImageUri == null) state.existingImagePath else null,
                    syncStatus = "localOnly"
                )

                if (state.isEditMode) {
                    repository.updateShoe(shoe)
                } else {
                    repository.createShoe(shoe)
                }

                if (state.isDefault) {
                    repository.setDefaultShoe(id)
                }

                // Upload image if selected
                state.selectedImageUri?.let { uri ->
                    repository.uploadImage(id, uri)
                }

                // Sync to server
                repository.syncToServer()

                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            } catch (e: Exception) {
                RLog.e("AddEditShoeVM", "saveShoe failed", e)
                _uiState.update { it.copy(isSaving = false, errorMessage = "保存失败: ${e.message}") }
            }
        }
    }

    fun consumeError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

class AddEditShoeViewModelFactory(
    private val context: Context,
    private val shoeId: String? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddEditShoeViewModel::class.java)) {
            return AddEditShoeViewModel(context, shoeId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
