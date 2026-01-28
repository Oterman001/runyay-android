package com.oterman.rundemo.util

/**
 * 常量定义
 */
object Constants {
    
    /**
     * 网络相关常量
     */
    object Network {
        // TODO: 替换为实际的API基础URL
        const val BASE_URL = "https://your-api-base-url.com/"
        const val CONNECT_TIMEOUT = 30L
        const val READ_TIMEOUT = 30L
        const val WRITE_TIMEOUT = 30L
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

