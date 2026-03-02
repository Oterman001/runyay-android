package com.oterman.rundemo.data.local

import android.content.Context
import android.content.SharedPreferences
import com.oterman.rundemo.domain.model.DashboardCardId
import com.oterman.rundemo.domain.model.DashboardCardItem
import com.oterman.rundemo.domain.model.GoalSettings
import com.oterman.rundemo.domain.model.GoalType
import com.oterman.rundemo.presentation.components.trajectory.TrajectoryColorMode
import com.oterman.rundemo.ui.theme.ThemeMode
import com.oterman.rundemo.util.Constants
import java.util.Calendar

/**
 * SharedPreferences管理器
 * 负责用户信息的本地持久化存储
 * 对应iOS的AccountManager部分功能
 */
class PreferencesManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        Constants.PreferenceKeys.USER_PREFS,
        Context.MODE_PRIVATE
    )
    
    /**
     * 保存用户Token
     */
    fun saveUserToken(token: String) {
        prefs.edit().putString(Constants.PreferenceKeys.KEY_USER_TOKEN, token).apply()
    }
    
    /**
     * 获取用户Token
     */
    fun getUserToken(): String? {
        return prefs.getString(Constants.PreferenceKeys.KEY_USER_TOKEN, null)
    }
    
    /**
     * 保存用户ID
     */
    fun saveUserId(userId: String) {
        prefs.edit().putString(Constants.PreferenceKeys.KEY_USER_ID, userId).apply()
    }
    
    /**
     * 获取用户ID
     */
    fun getUserId(): String? {
        return prefs.getString(Constants.PreferenceKeys.KEY_USER_ID, null)
    }
    
    /**
     * 保存用户名
     */
    fun saveUserName(userName: String) {
        prefs.edit().putString(Constants.PreferenceKeys.KEY_USER_NAME, userName).apply()
    }
    
    /**
     * 获取用户名
     */
    fun getUserName(): String? {
        return prefs.getString(Constants.PreferenceKeys.KEY_USER_NAME, null)
    }
    
    /**
     * 保存手机号
     */
    fun savePhoneNumber(phoneNumber: String) {
        prefs.edit().putString(Constants.PreferenceKeys.KEY_PHONE_NUMBER, phoneNumber).apply()
    }
    
    /**
     * 获取手机号
     */
    fun getPhoneNumber(): String? {
        return prefs.getString(Constants.PreferenceKeys.KEY_PHONE_NUMBER, null)
    }
    
    /**
     * 保存邮箱
     */
    fun saveEmail(email: String) {
        prefs.edit().putString(Constants.PreferenceKeys.KEY_EMAIL, email).apply()
    }
    
    /**
     * 获取邮箱
     */
    fun getEmail(): String? {
        return prefs.getString(Constants.PreferenceKeys.KEY_EMAIL, null)
    }
    
    /**
     * 保存头像URL
     */
    fun saveImageUrl(imageUrl: String) {
        prefs.edit().putString(Constants.PreferenceKeys.KEY_IMAGE_URL, imageUrl).apply()
    }
    
    /**
     * 获取头像URL
     */
    fun getImageUrl(): String? {
        return prefs.getString(Constants.PreferenceKeys.KEY_IMAGE_URL, null)
    }
    
    /**
     * 保存Token过期时间
     * @param expireDay Token有效天数
     */
    fun saveTokenExpireDate(expireDay: Int) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, expireDay)
        val expireTime = calendar.timeInMillis
        prefs.edit().putLong(Constants.PreferenceKeys.KEY_TOKEN_EXPIRE_DATE, expireTime).apply()
    }
    
    /**
     * 获取Token过期时间
     */
    fun getTokenExpireDate(): Long {
        return prefs.getLong(Constants.PreferenceKeys.KEY_TOKEN_EXPIRE_DATE, 0L)
    }
    
    /**
     * 检查Token是否过期
     */
    fun isTokenExpired(): Boolean {
        val expireTime = getTokenExpireDate()
        if (expireTime == 0L) return true
        return System.currentTimeMillis() > expireTime
    }
    
    /**
     * 设置登录状态
     */
    fun setLoggedIn(isLoggedIn: Boolean) {
        prefs.edit().putBoolean(Constants.PreferenceKeys.KEY_IS_LOGGED_IN, isLoggedIn).apply()
    }
    
    /**
     * 检查用户是否已登录
     */
    fun isUserLoggedIn(): Boolean {
        val isLoggedIn = prefs.getBoolean(Constants.PreferenceKeys.KEY_IS_LOGGED_IN, false)
        val hasToken = getUserToken() != null
        val hasUserId = getUserId() != null
        val tokenNotExpired = !isTokenExpired()
        
        return isLoggedIn && hasToken && hasUserId && tokenNotExpired
    }
    
    /**
     * 保存完整的用户信息
     */
    fun saveUserInfo(
        userId: String,
        userName: String?,
        phoneNumber: String?,
        email: String?,
        token: String,
        imageUrl: String?,
        expireDay: Int?
    ) {
        prefs.edit().apply {
            putString(Constants.PreferenceKeys.KEY_USER_ID, userId)
            putString(Constants.PreferenceKeys.KEY_USER_TOKEN, token)
            userName?.let { putString(Constants.PreferenceKeys.KEY_USER_NAME, it) }
            phoneNumber?.let { putString(Constants.PreferenceKeys.KEY_PHONE_NUMBER, it) }
            email?.let { putString(Constants.PreferenceKeys.KEY_EMAIL, it) }
            imageUrl?.let { putString(Constants.PreferenceKeys.KEY_IMAGE_URL, it) }
            putBoolean(Constants.PreferenceKeys.KEY_IS_LOGGED_IN, true)
            apply()
        }
        
        // 保存Token过期时间
        expireDay?.let { saveTokenExpireDate(it) }
    }
    
    /**
     * 清除所有用户数据
     */
    fun clearUserData() {
        prefs.edit().apply {
            remove(Constants.PreferenceKeys.KEY_USER_ID)
            remove(Constants.PreferenceKeys.KEY_USER_TOKEN)
            remove(Constants.PreferenceKeys.KEY_USER_NAME)
            remove(Constants.PreferenceKeys.KEY_PHONE_NUMBER)
            remove(Constants.PreferenceKeys.KEY_EMAIL)
            remove(Constants.PreferenceKeys.KEY_IMAGE_URL)
            remove(Constants.PreferenceKeys.KEY_TOKEN_EXPIRE_DATE)
            remove(Constants.PreferenceKeys.KEY_CACHED_AVATAR_URL)
            remove(Constants.PreferenceKeys.KEY_CACHED_AVATAR_EXPIRATION)
            putBoolean(Constants.PreferenceKeys.KEY_IS_LOGGED_IN, false)
            apply()
        }
    }
    
    /**
     * 更新用户名（用于注册时设置昵称）
     */
    fun updateUserName(userName: String) {
        saveUserName(userName)
    }

    // ==================== Avatar Cache ====================

    fun saveCachedAvatarUrl(url: String, expirationTime: Long) {
        prefs.edit()
            .putString(Constants.PreferenceKeys.KEY_CACHED_AVATAR_URL, url)
            .putLong(Constants.PreferenceKeys.KEY_CACHED_AVATAR_EXPIRATION, expirationTime)
            .apply()
    }

    fun getCachedAvatarUrl(): String? =
        prefs.getString(Constants.PreferenceKeys.KEY_CACHED_AVATAR_URL, null)

    fun getCachedAvatarExpiration(): Long =
        prefs.getLong(Constants.PreferenceKeys.KEY_CACHED_AVATAR_EXPIRATION, 0L)

    fun clearCachedAvatar() {
        prefs.edit()
            .remove(Constants.PreferenceKeys.KEY_CACHED_AVATAR_URL)
            .remove(Constants.PreferenceKeys.KEY_CACHED_AVATAR_EXPIRATION)
            .apply()
    }

    // ==================== Goal Settings ====================

    /**
     * 获取目标设置
     */
    fun getGoalSettings(): GoalSettings {
        val goalEnabled = prefs.getBoolean(Constants.PreferenceKeys.KEY_GOAL_ENABLED, false)
        val goalTypeStr = prefs.getString(Constants.PreferenceKeys.KEY_GOAL_TYPE, GoalType.DISTANCE.name)
        val goalType = try {
            GoalType.valueOf(goalTypeStr ?: GoalType.DISTANCE.name)
        } catch (e: Exception) {
            GoalType.DISTANCE
        }
        val yearDistanceGoal = prefs.getFloat(Constants.PreferenceKeys.KEY_YEAR_DISTANCE_GOAL, 0f).toDouble()
        val monthDistanceGoal = prefs.getFloat(Constants.PreferenceKeys.KEY_MONTH_DISTANCE_GOAL, 0f).toDouble()
        val yearDurationGoal = prefs.getFloat(Constants.PreferenceKeys.KEY_YEAR_DURATION_GOAL, 0f).toDouble()
        val monthDurationGoal = prefs.getFloat(Constants.PreferenceKeys.KEY_MONTH_DURATION_GOAL, 0f).toDouble()

        return GoalSettings(
            goalEnabled = goalEnabled,
            goalType = goalType,
            yearDistanceGoal = yearDistanceGoal,
            monthDistanceGoal = monthDistanceGoal,
            yearDurationGoal = yearDurationGoal,
            monthDurationGoal = monthDurationGoal
        )
    }

    /**
     * 保存目标设置
     */
    fun saveGoalSettings(goalSettings: GoalSettings) {
        prefs.edit().apply {
            putBoolean(Constants.PreferenceKeys.KEY_GOAL_ENABLED, goalSettings.goalEnabled)
            putString(Constants.PreferenceKeys.KEY_GOAL_TYPE, goalSettings.goalType.name)
            putFloat(Constants.PreferenceKeys.KEY_YEAR_DISTANCE_GOAL, goalSettings.yearDistanceGoal.toFloat())
            putFloat(Constants.PreferenceKeys.KEY_MONTH_DISTANCE_GOAL, goalSettings.monthDistanceGoal.toFloat())
            putFloat(Constants.PreferenceKeys.KEY_YEAR_DURATION_GOAL, goalSettings.yearDurationGoal.toFloat())
            putFloat(Constants.PreferenceKeys.KEY_MONTH_DURATION_GOAL, goalSettings.monthDurationGoal.toFloat())
            apply()
        }
    }

    /**
     * 检查是否启用了目标
     */
    fun isGoalEnabled(): Boolean {
        return prefs.getBoolean(Constants.PreferenceKeys.KEY_GOAL_ENABLED, false)
    }

    /**
     * 获取目标类型
     */
    fun getGoalType(): GoalType {
        val goalTypeStr = prefs.getString(Constants.PreferenceKeys.KEY_GOAL_TYPE, GoalType.DISTANCE.name)
        return try {
            GoalType.valueOf(goalTypeStr ?: GoalType.DISTANCE.name)
        } catch (e: Exception) {
            GoalType.DISTANCE
        }
    }

    // ==================== DataTab Display Settings ====================

    /**
     * 保存 DataTab 显示模式
     * @param useHeatmap true=热力图模式, false=简单模式
     */
    fun saveDataTabDisplayMode(useHeatmap: Boolean) {
        prefs.edit().putBoolean(Constants.PreferenceKeys.KEY_DATATAB_USE_HEATMAP, useHeatmap).apply()
    }

    /**
     * 获取 DataTab 显示模式
     * @return true=热力图模式(默认), false=简单模式
     */
    fun getDataTabDisplayMode(): Boolean {
        return prefs.getBoolean(Constants.PreferenceKeys.KEY_DATATAB_USE_HEATMAP, true)
    }

    // ==================== Trajectory Wall Settings ====================

    /**
     * 保存轨迹墙每行个数
     */
    fun saveTrajectoryItemsPerRow(count: Int) {
        prefs.edit().putInt(Constants.PreferenceKeys.KEY_TRAJECTORY_ITEMS_PER_ROW, count).apply()
    }

    /**
     * 获取轨迹墙每行个数
     * @return 每行个数 (默认6)
     */
    fun getTrajectoryItemsPerRow(): Int {
        val saved = prefs.getInt(Constants.PreferenceKeys.KEY_TRAJECTORY_ITEMS_PER_ROW, 6)
        return if (saved in 3..10) saved else 6
    }

    // ==================== Trajectory Color Mode ====================

    fun saveTrajectoryColorMode(mode: TrajectoryColorMode) {
        prefs.edit().putString(Constants.PreferenceKeys.KEY_TRAJECTORY_COLOR_MODE, mode.name).apply()
    }

    fun getTrajectoryColorMode(): TrajectoryColorMode {
        val saved = prefs.getString(
            Constants.PreferenceKeys.KEY_TRAJECTORY_COLOR_MODE,
            TrajectoryColorMode.DISTANCE_BASED.name
        )
        return try {
            TrajectoryColorMode.valueOf(saved ?: TrajectoryColorMode.DISTANCE_BASED.name)
        } catch (e: Exception) {
            TrajectoryColorMode.DISTANCE_BASED
        }
    }

    // ==================== Dashboard Card Config ====================

    fun saveDashboardCardConfig(cards: List<DashboardCardItem>) {
        val value = cards.joinToString(",") { "${it.id.name}:${it.visible}" }
        prefs.edit().putString(Constants.PreferenceKeys.KEY_DASHBOARD_CARD_CONFIG, value).apply()
    }

    fun getDashboardCardConfig(): List<DashboardCardItem> {
        val saved = prefs.getString(Constants.PreferenceKeys.KEY_DASHBOARD_CARD_CONFIG, null)
            ?: return DashboardCardId.entries.map { DashboardCardItem(it) }
        val savedItems = saved.split(",").mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                try {
                    val id = DashboardCardId.valueOf(parts[0])
                    val visible = parts[1].toBoolean()
                    DashboardCardItem(id, visible)
                } catch (_: Exception) {
                    null
                }
            } else null
        }
        val savedIds = savedItems.map { it.id }.toSet()
        val newCards = DashboardCardId.entries
            .filter { it !in savedIds }
            .map { DashboardCardItem(it) }
        return savedItems + newCards
    }

    // ==================== Theme Mode ====================

    fun saveThemeMode(mode: ThemeMode) {
        prefs.edit().putString(Constants.PreferenceKeys.KEY_THEME_MODE, mode.name).apply()
    }

    fun getThemeMode(): ThemeMode {
        val saved = prefs.getString(
            Constants.PreferenceKeys.KEY_THEME_MODE,
            ThemeMode.AUTO.name
        )
        return try {
            ThemeMode.valueOf(saved ?: ThemeMode.AUTO.name)
        } catch (e: Exception) {
            ThemeMode.AUTO
        }
    }
}

