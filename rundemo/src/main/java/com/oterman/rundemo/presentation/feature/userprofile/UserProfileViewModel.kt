package com.oterman.rundemo.presentation.feature.userprofile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.local.database.RunDatabase
import com.oterman.rundemo.data.repository.AvatarManager
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 用户信息页面ViewModel
 * 对应iOS的UserProfileView功能
 */
class UserProfileViewModel(
    private val context: Context,
    private val preferencesManager: PreferencesManager = PreferencesManager(context),
    private val userRepository: UserRepository = UserRepository(context)
) : ViewModel() {

    private val avatarManager: AvatarManager = AvatarManager.getInstance(context)

    companion object {
        private const val TAG = "UserProfileViewModel"
        private const val MAX_IMAGE_SIZE = 200 * 1024 // 200KB (裁剪后压缩目标)
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

        val physio = preferencesManager.getHearRateZoneSettings()
        _uiState.update { state ->
            state.copy(
                userId = userId,
                userName = userName,
                phoneNumber = phoneNumber,
                isMale = physio.isMale,
                birthdayMillis = physio.birthdayMillis
            )
        }

        // 异步加载头像
        if (userId.isNotEmpty()) {
            loadAvatarUrl(userId)
        }
    }

    /**
     * 加载头像URL（复用缓存，不强制刷新）
     */
    private fun loadAvatarUrl(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAvatar = true) }

            RLog.d(TAG, "开始加载头像URL: userId=$userId")

            val result = avatarManager.getAvatarUrl(userId)

            result.onSuccess { url ->
                RLog.d(TAG, "头像URL加载成功: $url")
                _uiState.update { it.copy(avatarUrl = url, isLoadingAvatar = false) }
            }.onFailure { e ->
                RLog.e(TAG, "头像URL加载失败: ${e.message}")
                _uiState.update { it.copy(avatarUrl = null, isLoadingAvatar = false) }
            }
        }
    }

    /**
     * 下拉刷新头像（强制从API获取）
     */
    fun refreshAvatar() {
        val userId = _uiState.value.userId.takeIf { it.isNotEmpty() } ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAvatar = true) }
            val result = avatarManager.getAvatarUrl(userId, forceRefresh = true)
            result.onSuccess { url ->
                _uiState.update { it.copy(avatarUrl = url, isLoadingAvatar = false) }
            }.onFailure {
                _uiState.update { it.copy(isLoadingAvatar = false) }
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
                    // 清除缓存并强制获取最新头像URL
                    _uiState.value.userId.takeIf { it.isNotEmpty() }?.let { userId ->
                        avatarManager.clearCache()
                        val result2 = avatarManager.getAvatarUrl(userId, forceRefresh = true)
                        result2.onSuccess { freshUrl ->
                            _uiState.update { s -> s.copy(avatarUrl = freshUrl) }
                        }
                    }
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
     * @param targetSize 目标大小(字节)，目标200KB
     * @return 压缩后的图片数据，如果失败返回null
     */
    private fun compressToTargetSize(uri: Uri, targetSize: Int): ByteArray? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            var bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (bitmap == null) return null

            // 1. 先缩放到初始合适尺寸
            bitmap = scaleBitmapIfNeeded(bitmap, MAX_DIMENSION)

            var lastResult: ByteArray? = null

            // 2. 外层循环：尺寸缩减（当质量压缩无法满足时，按0.75比例缩小图片）
            while (minOf(bitmap.width, bitmap.height) >= 64) {
                var quality = JPEG_QUALITY
                var outputStream: ByteArrayOutputStream

                // 3. 内层循环：逐步降低质量直到满足大小要求
                do {
                    outputStream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                    if (outputStream.size() <= targetSize) {
                        val result = outputStream.toByteArray()
                        RLog.d(TAG, "图片压缩完成: ${result.size / 1024}KB, quality=$quality, size=${bitmap.width}x${bitmap.height}")
                        bitmap.recycle()
                        outputStream.close()
                        return result
                    }
                    lastResult = outputStream.toByteArray()
                    quality -= 5
                } while (quality > 10)

                // 内层质量降到最低仍超标，缩小尺寸后重试
                val newWidth = (bitmap.width * 0.75f).toInt()
                val newHeight = (bitmap.height * 0.75f).toInt()
                RLog.d(TAG, "质量压缩不足，缩小尺寸: ${bitmap.width}x${bitmap.height} -> ${newWidth}x${newHeight}")
                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
                bitmap.recycle()
                bitmap = scaledBitmap
            }

            // 兜底：返回最后一次压缩结果（已尽最大努力压缩）
            bitmap.recycle()
            RLog.w(TAG, "图片压缩已达极限，最终大小: ${lastResult?.size?.div(1024)}KB")
            lastResult
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

    // ==================== 性别 / 出生年月 ====================

    fun showGenderPicker() = _uiState.update { it.copy(showGenderPicker = true) }
    fun dismissGenderPicker() = _uiState.update { it.copy(showGenderPicker = false) }
    fun saveGender(isMale: Boolean) {
        val updated = preferencesManager.getHearRateZoneSettings().copy(isMale = isMale)
        preferencesManager.saveHearRateZoneSettings(updated)
        _uiState.update { it.copy(isMale = isMale, showGenderPicker = false) }
        viewModelScope.launch {
            userRepository.updateBasicInfo(gender = if (isMale) "M" else "F").onFailure {
                RLog.w(TAG, "update性别失败，尝试save: ${it.message}")
                userRepository.saveBasicInfo(updated).onFailure { e ->
                    RLog.e(TAG, "save性别也失败: ${e.message}")
                    _uiState.update { it.copy(errorMessage = "操作失败，请稍后重试") }
                }
            }
        }
    }

    fun showBirthdayPicker() = _uiState.update { it.copy(showBirthdayPicker = true) }
    fun dismissBirthdayPicker() = _uiState.update { it.copy(showBirthdayPicker = false) }
    fun saveBirthday(millis: Long) {
        val updated = preferencesManager.getHearRateZoneSettings().copy(birthdayMillis = millis)
        preferencesManager.saveHearRateZoneSettings(updated)
        _uiState.update { it.copy(birthdayMillis = millis, showBirthdayPicker = false) }
        viewModelScope.launch {
            val birthDate = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(millis))
            userRepository.updateBasicInfo(birthDate = birthDate).onFailure {
                RLog.w(TAG, "update生日失败，尝试save: ${it.message}")
                userRepository.saveBasicInfo(updated).onFailure { e ->
                    RLog.e(TAG, "save生日也失败: ${e.message}")
                    _uiState.update { it.copy(errorMessage = "操作失败，请稍后重试") }
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
