package com.oterman.rundemo.domain.model

import com.oterman.rundemo.R

/**
 * 数据源平台枚举
 * 对应iOS的DataSourceType
 */
enum class DataSourcePlatform(
    val code: String,
    val displayName: String,
    val displayNameEn: String,
    val appBrandName: String,
    val iconResId: Int,
    val isEnabled: Boolean = true,
    val supportsSorting: Boolean = true,
    /** 是否需要 OAuth 授权绑定（false 表示始终可用，不需要主动绑定，如 Apple Health、手动导入） */
    val requiresOAuthBinding: Boolean = true
) {
    GARMIN_CHINA(
        code = "GCN",
        displayName = "佳明中国",
        displayNameEn = "Garmin CN",
        appBrandName = "GARMIN CONNECT™",
        iconResId = R.drawable.ic_garmin,
        isEnabled = true,
        supportsSorting = true
    ),
    GARMIN_GLOBAL(
        code = "GGB",
        displayName = "佳明国际",
        displayNameEn = "Garmin GB",
        appBrandName = "GARMIN CONNECT™",
        iconResId = R.drawable.ic_garmin,
        isEnabled = true,
        supportsSorting = true
    ),
    COROS(
        code = "COROS",
        displayName = "高驰",
        displayNameEn = "COROS",
        appBrandName = "COROS",
        iconResId = R.drawable.icon_coros,
        isEnabled = true,
        supportsSorting = true
    ),
    APPLE_HEALTH(
        code = "HK",
        displayName = "苹果健康",
        displayNameEn = "Apple HK",
        appBrandName = "Apple Health",
        iconResId = R.drawable.apple_hk,
        isEnabled = true,
        supportsSorting = true,
        requiresOAuthBinding = false
    ),
    HUAWEI(
        code = "HUAWEI",
        displayName = "华为运动健康",
        displayNameEn = "Hua Wei",
        appBrandName = "华为运动健康",
        iconResId = R.drawable.icon_huawei,
        isEnabled = false,
        supportsSorting = false
    ),
    MANUAL(
        code = "MANUAL",
        displayName = "手动导入",
        displayNameEn = "Manual",
        appBrandName = "手动导入",
        iconResId = R.drawable.ic_manual2,
        isEnabled = true,
        supportsSorting = true,
        requiresOAuthBinding = false
    );

    companion object {
        /**
         * 根据平台代码获取平台枚举
         */
        fun fromCode(code: String): DataSourcePlatform? {
            return entries.find { it.code == code }
        }

        /**
         * 获取所有已启用的平台
         */
        fun getEnabledPlatforms(): List<DataSourcePlatform> {
            return entries.filter { it.isEnabled }
        }

        /**
         * 获取所有支持排序的平台
         */
        fun getSortablePlatforms(): List<DataSourcePlatform> {
            return entries.filter { it.supportsSorting }
        }
    }
}

