package com.oterman.rundemo.domain.model

import com.oterman.rundemo.data.network.dto.RunSummaryBasicInfoDto

/**
 * 统一文件信息领域模型
 * 扩展现有FileInfo概念，新增runSummary字段
 */
data class UnifiedFileInfo(
    val id: Int,
    val platformCode: String,
    val summaryId: String,
    val dataDate: String,
    val deviceName: String,
    val ossUrl: String? = null,
    val fitUrl: String? = null,
    val runSummary: RunSummaryBasicInfoDto? = null
) {
    /**
     * 向后兼容转换为FileInfo
     */
    fun toFileInfo(): FileInfo = FileInfo(
        id = id,
        platformCode = platformCode,
        summaryId = summaryId,
        dataDate = dataDate,
        deviceName = deviceName,
        ossUrl = ossUrl,
        fitUrl = fitUrl
    )

    val hasOssUrl: Boolean
        get() = !ossUrl.isNullOrEmpty()

    val hasRunSummary: Boolean
        get() = runSummary != null
}
