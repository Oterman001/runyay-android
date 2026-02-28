package com.oterman.rundemo.domain.model

import com.oterman.rundemo.R

/**
 * 数据源平台枚举
 * 对应iOS的DataSourceType
 */
enum class DataSourcePlatform(
    val code: String,
    val displayName: String,
    val appBrandName: String,
    val iconResId: Int,
    val isEnabled: Boolean = true,
    val supportsSorting: Boolean = true
) {
    GARMIN_CHINA(
        code = "GCN",
        displayName = "佳明(中国)",
        appBrandName = "GARMIN CONNECT™",
        iconResId = R.drawable.icon_garmin,
        isEnabled = true,
        supportsSorting = true
    ),
    GARMIN_GLOBAL(
        code = "GGB",
        displayName = "佳明(国际)",
        appBrandName = "GARMIN CONNECT™",
        iconResId = R.drawable.icon_garmin,
        isEnabled = true,
        supportsSorting = true
    ),
    COROS(
        code = "COROS",
        displayName = "COROS 高驰",
        appBrandName = "COROS",
        iconResId = R.drawable.icon_coros,
        isEnabled = true,
        supportsSorting = true
    ),
    APPLE_HEALTH(
        code = "HK",
        displayName = "苹果健康",
        appBrandName = "Apple Health",
        iconResId = R.drawable.icon_huawei,
        isEnabled = true,
        supportsSorting = false
    ),
    HUAWEI(
        code = "HUAWEI",
        displayName = "华为运动健康",
        appBrandName = "华为运动健康",
        iconResId = R.drawable.icon_huawei,
        isEnabled = false,
        supportsSorting = false
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

