package com.oterman.rundemo.presentation.feature.runningshoes.addedit

import android.net.Uri

data class AddEditShoeUiState(
    val brand: String = "",
    val model: String = "",
    val nickname: String = "",
    val shoeSize: String = "",
    val price: String = "",
    val expectedLifespan: String = "700",
    val initialDistance: String = "0",
    val firstUseDate: Long? = null,
    val notes: String = "",
    val isDefault: Boolean = false,
    val selectedImageUri: Uri? = null,
    val existingImageUrl: String? = null,
    val isEditMode: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
) {
    val isFormValid: Boolean
        get() = brand.isNotBlank() && model.isNotBlank()
}
