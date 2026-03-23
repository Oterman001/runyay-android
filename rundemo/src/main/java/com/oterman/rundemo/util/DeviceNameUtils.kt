package com.oterman.rundemo.util

import com.oterman.rundemo.data.local.entity.RunRecordEntity
import com.oterman.rundemo.domain.model.DataSourcePlatform

/**
 * 设备名解析工具：统一详情页和分享页的设备名显示逻辑
 */
object DeviceNameUtils {
    fun resolveDisplayName(record: RunRecordEntity): String? = when {
        record.datasource == "HK" && record.deviceInfo?.contains("apple watch", ignoreCase = true) == true ->
            AppleWatchDeviceUtils.getModelName(record.deviceVersion)

        record.datasource == DataSourcePlatform.MANUAL.code ->
            if (record.deviceInfo.isNullOrBlank()) "Manual" else "Manual-${record.deviceInfo}"

        else -> record.deviceInfo
    }
}
