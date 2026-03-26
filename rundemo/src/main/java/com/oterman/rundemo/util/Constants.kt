package com.oterman.rundemo.util

/**
 * 常量定义
 */
object Constants {
    
    /**
     * 网络相关常量
     */
    object Network {
        // API基础URL
        const val BASE_URL = "https://yayarun.cn/sys/"
        const val CONNECT_TIMEOUT = 30L
        const val READ_TIMEOUT = 30L
        const val WRITE_TIMEOUT = 30L
        
        // AppKey - 对应iOS的NetworkConfig
        const val APP_KEY = "1jns01o9lksa12"
    }
    
    /**
     * SharedPreferences键名
     */
    object PreferenceKeys {
        const val USER_PREFS = "user_prefs"
        const val KEY_USER_ID = "user_id"
        const val KEY_USER_TOKEN = "user_token"
        const val KEY_USER_NAME = "user_name"
        const val KEY_PHONE_NUMBER = "phone_number"
        const val KEY_EMAIL = "email"
        const val KEY_IMAGE_URL = "image_url"
        const val KEY_TOKEN_EXPIRE_DATE = "token_expire_date"
        const val KEY_IS_LOGGED_IN = "is_logged_in"

        // Goal settings keys
        const val KEY_GOAL_ENABLED = "goal_enabled"
        const val KEY_GOAL_TYPE = "goal_type"
        const val KEY_YEAR_DISTANCE_GOAL = "year_distance_goal"
        const val KEY_MONTH_DISTANCE_GOAL = "month_distance_goal"
        const val KEY_YEAR_DURATION_GOAL = "year_duration_goal"
        const val KEY_MONTH_DURATION_GOAL = "month_duration_goal"

        // DataTab display settings
        const val KEY_DATATAB_USE_HEATMAP = "datatab_use_heatmap"

        // Trajectory wall settings
        const val KEY_TRAJECTORY_ITEMS_PER_ROW = "trajectory_items_per_row"
        const val KEY_TRAJECTORY_COLOR_MODE = "trajectory_color_mode"

        // Theme mode
        const val KEY_THEME_MODE = "theme_mode"

        // Dashboard card config
        const val KEY_DASHBOARD_CARD_CONFIG = "dashboard_card_config"

        // Avatar cache
        const val KEY_CACHED_AVATAR_URL = "cached_avatar_url"
        const val KEY_CACHED_AVATAR_EXPIRATION = "cached_avatar_expiration"
        const val KEY_CACHED_AVATAR_LAST_FETCH = "cached_avatar_last_fetch"

        // Heart rate zone settings
        const val KEY_HR_MANUAL_ENABLED = "hr_manual_enabled"
        const val KEY_HR_MAX_HEART_RATE = "hr_max_heart_rate"
        const val KEY_HR_RESTING_HEART_RATE = "hr_resting_heart_rate"
        const val KEY_HR_BIRTHDAY_MILLIS = "hr_birthday_millis"
        const val KEY_HR_AUTO_SYNC_ENABLED = "hr_auto_sync_enabled"
        const val KEY_HR_PREFERRED_PLATFORM = "hr_preferred_platform"
        const val KEY_HR_GENDER = "hr_gender"
        const val KEY_PHYSIO_SETUP_COMPLETED = "physio_setup_completed"
        const val KEY_FIT_IMPORT_DISCLAIMER_DISMISSED = "fit_import_disclaimer_dismissed"

        // Notification permission dialog
        const val KEY_NOTIFICATION_DENIED_DATE = "notification_denied_date"

        // Token refresh
        const val KEY_LAST_DAILY_TOKEN_REFRESH_DATE = "last_daily_token_refresh_date"
        const val KEY_LAST_LOGIN_DATE = "last_login_date"

        // Downloaded APK cache
        const val KEY_DOWNLOADED_APK_VERSION_CODE = "downloaded_apk_version_code"
        const val KEY_DOWNLOADED_APK_PATH = "downloaded_apk_path"
    }
    
    /**
     * 登录类型
     */
    object LoginType {
        const val PASSWORD = "PASSWORD"
        const val VERIFICATION_CODE = "VERIFICATION_CODE"
    }
    
    /**
     * 场景类型
     */
    object SceneType {
        const val LOGIN = "LOGIN"
        const val REGISTER = "REGISTER"
    }
    
    /**
     * 响应成功标识
     */
    object ResponseFlags {
        const val SUCCESS = "Y"
        const val FAILURE = "N"
        const val SUCCESS_CODE = "0000"
    }
    
    /**
     * 验证规则
     */
    object Validation {
        const val MIN_PASSWORD_LENGTH = 6
        const val PHONE_NUMBER_LENGTH = 11
    }
}

