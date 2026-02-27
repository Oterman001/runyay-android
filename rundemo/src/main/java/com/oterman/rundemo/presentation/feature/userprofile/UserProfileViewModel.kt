package com.oterman.rundemo.presentation.feature.userprofile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.local.database.RunDatabase
import com.oterman.rundemo.data.repository.RunDataRepositoryImpl
import com.oterman.rundemo.data.repository.UserRepository
import com.oterman.rundemo.util.RLog
import com.oterman.rundemo.util.ValidationUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

/**
 * 用户信息页面ViewModel
 * 对应iOS的UserProfileView功能
 */
class UserProfileViewModel(
    private val context: Context,
    private val preferencesManager: PreferencesManager = PreferencesManager(context),
    private val userRepository: UserRepository = UserRepository(context)
) : ViewModel() {

    companion object {
        private const val TAG = "UserProfileViewModel"
        private const val MAX_IMAGE_SIZE = 300 * 1024 // 300KB (裁剪后压缩目标)
        private const val MAX_DIMENSION = 512 // 最大边长
        private const val JPEG_QUALITY = 85
    }

    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserInfo()
    }

    /**
     * 加载用户信息
     */
    private fun loadUserInfo() {
        val userId = preferencesManager.getUserId() ?: ""
        val userName = preferencesManager.getUserName() ?: ""
        val phoneNumber = preferencesManager.getPhoneNumber() ?: ""

        RLog.d(TAG, "加载用户信息: userId=$userId, userName=$userName")

        _uiState.update { state ->
            state.copy(
                userId = userId,
                userName = userName,
                phoneNumber = phoneNumber
            )
        }

        // 异步加载头像
        if (userId.isNotEmpty()) {
            loadAvatarUrl(userId)
        }
    }

    /**
     * 加载头像URL
     */
    private fun loadAvatarUrl(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAvatar = true) }

            RLog.d(TAG, "开始加载头像URL: userId=$userId")

            val result = userRepository.getAvatarUrl(userId)

            result.onSuccess { url ->
                RLog.d(TAG, "头像URL加载成功: $url")
                _uiState.update { it.copy(avatarUrl = url, isLoadingAvatar = false) }
            }.onFailure { e ->
                RLog.e(TAG, "头像URL加载失败: ${e.message}")
                _uiState.update { it.copy(avatarUrl = null, isLoadingAvatar = false) }
            }
        }
    }

    // ==================== 头像相关 ====================

    /**
     * 显示头像选择器
     */
    fun showAvatarPicker() {
        _uiState.update { it.copy(showAvatarPickerDialog = true) }
    }

    /**
     * 隐藏头像选择器
     */
    fun dismissAvatarPicker() {
        _uiState.update { it.copy(showAvatarPickerDialog = false) }
    }

    /**
     * 处理选择的图片 (旧方法，保留兼容性)
     */
    fun handleSelectedImage(uri: Uri) {
        dismissAvatarPicker()
        uploadCroppedAvatar(uri)
    }

    /**
     * 上传裁剪后的头像
     * 用于uCrop裁剪完成后调用
     */
    fun uploadCroppedAvatar(croppedUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingAvatar = true) }

            try {
                RLog.d(TAG, "开始上传裁剪后的头像: $croppedUri")

                // 压缩到目标大小 (300KB)
                val imageData = compressToTargetSize(croppedUri, MAX_IMAGE_SIZE)
                if (imageData == null) {
                    _uiState.update {
                        it.copy(
                            isUploadingAvatar = false,
                            errorMessage = "图片处理失败"
                        )
                    }
                    return@launch
                }

                RLog.d(TAG, "压缩后大小: ${imageData.size / 1024}KB")

                val fileName = "avatar_${System.currentTimeMillis()}.jpg"
                val result = userRepository.uploadAvatar(imageData, fileName)

                result.onSuccess { newUrl ->
                    RLog.d(TAG, "头像上传成功: $newUrl")
                    // 重新加载头像URL以获取带签名的临时URL
                    _uiState.value.userId.takeIf { it.isNotEmpty() }?.let { loadAvatarUrl(it) }
                    _uiState.update {
                        it.copy(
                            isUploadingAvatar = false,
                            successMessage = "头像更新成功"
                        )
                    }
                }.onFailure { e ->
                    RLog.e(TAG, "头像上传失败: ${e.message}")
                    _uiState.update {
                        it.copy(
                            isUploadingAvatar = false,
                            errorMessage = e.message ?: "头像上传失败"
                        )
                    }
                }
            } catch (e: Exception) {
                RLog.e(TAG, "头像上传异常: ${e.message}")
                _uiState.update {
                    it.copy(
                        isUploadingAvatar = false,
                        errorMessage = "头像上传失败"
                    )
                }
            }
        }
    }

    /**
     * 压缩图片到目标大小
     * @param uri 图片Uri
     * @param targetSize 目标大小(字节)，默认300KB
     * @return 压缩后的图片数据，如果失败返回null
     */
    private fun compressToTargetSize(uri: Uri, targetSize: Int): ByteArray? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            var bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (bitmap == null) return null

            // 1. 先缩放到合适尺寸
            bitmap = scaleBitmapIfNeeded(bitmap, MAX_DIMENSION)

            // 2. 逐步降低质量直到满足大小要求
            var quality = JPEG_QUALITY
            var outputStream: ByteArrayOutputStream

            do {
                outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                if (outputStream.size() <= targetSize) break
                quality -= 5
            } while (quality > 10)

            val result = outputStream.toByteArray()
            RLog.d(TAG, "图片压缩完成: ${result.size / 1024}KB, quality=$quality")

            // 清理资源
            bitmap.recycle()
            outputStream.close()

            result
        } catch (e: Exception) {
            RLog.e(TAG, "图片压缩失败: ${e.message}")
            null
        }
    }

    /**
     * 如果图片尺寸超过最大值，按比例缩小
     */
    private fun scaleBitmapIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxDimension && height <= maxDimension) {
            return bitmap
        }

        val scale = maxDimension.toFloat() / maxOf(width, height)
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()

        RLog.d(TAG, "缩放图片: ${width}x${height} -> ${newWidth}x${newHeight}")
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    // ==================== 昵称相关 ====================

    /**
     * 显示昵称编辑器
     */
    fun showNicknameEditor() {
        _uiState.update {
            it.copy(
                showNicknameEditor = true,
                editingNickname = it.userName,
                nicknameError = null
            )
        }
    }

    /**
     * 隐藏昵称编辑器
     */
    fun dismissNicknameEditor() {
        _uiState.update {
            it.copy(
                showNicknameEditor = false,
                editingNickname = "",
                nicknameError = null
            )
        }
    }

    /**
     * 更新编辑中的昵称
     */
    fun updateEditingNickname(nickname: String) {
        val error = ValidationUtils.getNicknameErrorStrict(nickname)
        _uiState.update {
            it.copy(
                editingNickname = nickname,
                nicknameError = error
            )
        }
    }

    /**
     * 保存昵称
     */
    fun saveNickname() {
        val nickname = _uiState.value.editingNickname
        if (!ValidationUtils.isValidNicknameStrict(nickname)) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingNickname = true) }

            RLog.d(TAG, "保存昵称: $nickname")

            val result = userRepository.updateNickname(nickname)

            result.onSuccess {
                RLog.d(TAG, "昵称更新成功")
                _uiState.update {
                    it.copy(
                        isUpdatingNickname = false,
                        showNicknameEditor = false,
                        userName = nickname,
                        editingNickname = "",
                        successMessage = "昵称更新成功"
                    )
                }
            }.onFailure { e ->
                RLog.e(TAG, "昵称更新失败: ${e.message}")
                _uiState.update {
                    it.copy(
                        isUpdatingNickname = false,
                        errorMessage = e.message ?: "昵称更新失败"
                    )
                }
            }
        }
    }

    // ==================== 退出登录 ====================

    /**
     * 显示退出登录确认对话框
     */
    fun showLogoutConfirm() {
        _uiState.update { it.copy(showLogoutConfirmDialog = true) }
    }

    /**
     * 隐藏退出登录确认对话框
     */
    fun dismissLogoutConfirm() {
        _uiState.update { it.copy(showLogoutConfirmDialog = false) }
    }

    /**
     * 执行退出登录
     */
    fun logout() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoggingOut = true,
                    showLogoutConfirmDialog = false
                )
            }

            RLog.i(TAG, "用户退出登录")

            // Clear userId from repository
            RunDataRepositoryImpl.getInstance(RunDatabase.getInstance(context)).setCurrentUserId(null)

            val result = userRepository.logoutFromServer()

            result.onSuccess {
                RLog.d(TAG, "退出登录成功")
                _uiState.update {
                    it.copy(
                        isLoggingOut = false,
                        navigateToLogin = true
                    )
                }
            }.onFailure { e ->
                // 即使失败也导航到登录页，因为本地数据已清除
                RLog.e(TAG, "退出登录失败: ${e.message}")
                _uiState.update {
                    it.copy(
                        isLoggingOut = false,
                        navigateToLogin = true
                    )
                }
            }
        }
    }

    // ==================== 注销账号 ====================

    /**
     * 显示注销账号确认对话框
     */
    fun showDeactivateConfirm() {
        _uiState.update { it.copy(showDeactivateConfirmDialog = true) }
    }

    /**
     * 隐藏注销账号确认对话框
     */
    fun dismissDeactivateConfirm() {
        _uiState.update { it.copy(showDeactivateConfirmDialog = false) }
    }

    /**
     * 继续注销流程（显示密码确认）
     */
    fun proceedToPasswordConfirm() {
        _uiState.update {
            it.copy(
                showDeactivateConfirmDialog = false,
                showPasswordConfirmDialog = true,
                confirmPassword = "",
                passwordError = null
            )
        }
    }

    /**
     * 隐藏密码确认对话框
     */
    fun dismissPasswordConfirm() {
        _uiState.update {
            it.copy(
                showPasswordConfirmDialog = false,
                confirmPassword = "",
                passwordError = null
            )
        }
    }

    /**
     * 更新确认密码
     */
    fun updateConfirmPassword(password: String) {
        val error = if (password.isEmpty()) null else ValidationUtils.getPasswordError(password)
        _uiState.update {
            it.copy(
                confirmPassword = password,
                passwordError = error
            )
        }
    }

    /**
     * 执行注销账号
     */
    fun deactivateAccount() {
        val password = _uiState.value.confirmPassword
        if (!ValidationUtils.isValidPassword(password)) {
            _uiState.update { it.copy(passwordError = "请输入正确的密码") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isDeactivating = true) }

            RLog.i(TAG, "用户注销账号")

            val result = userRepository.deactivateAccount(password)

            result.onSuccess {
                RLog.d(TAG, "账号注销成功")
                _uiState.update {
                    it.copy(
                        isDeactivating = false,
                        showPasswordConfirmDialog = false,
                        navigateToLogin = true
                    )
                }
            }.onFailure { e ->
                RLog.e(TAG, "账号注销失败: ${e.message}")
                _uiState.update {
                    it.copy(
                        isDeactivating = false,
                        passwordError = e.message ?: "密码错误或注销失败"
                    )
                }
            }
        }
    }

    // ==================== 其他 ====================

    /**
     * 重置导航状态
     */
    fun resetNavigateToLogin() {
        _uiState.update { it.copy(navigateToLogin = false) }
    }

    /**
     * 清除错误消息
     */
    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * 清除成功消息
     */
    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }
}
