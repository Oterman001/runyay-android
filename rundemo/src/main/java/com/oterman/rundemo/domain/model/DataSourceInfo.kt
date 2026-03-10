package com.oterman.rundemo.domain.model

/**
 * 数据源信息
 * 包含平台类型和授权说明
 * 对应iOS的DataSourceInfo
 */
data class DataSourceInfo(
    val platform: DataSourcePlatform,
    val descriptions: List<String>,
    val isAuthorized: Boolean = false,
    val priority: Int = 999
) {
    companion object {
        /**
         * 获取所有数据源信息
         */
        fun getAllDataSources(): List<DataSourceInfo> {
            return listOf(
                // 佳明中国
                DataSourceInfo(
                    platform = DataSourcePlatform.GARMIN_CHINA,
                    descriptions = listOf(
                        "1、授权时，记得打开历史数据同步开关，该开关默认为关闭，需要手动打开，否则无法获取历史数据",
                        "2、账号授权后，会自动同步之后的新增数据",
                        "3、手动同步支持同步近一个月数据，同步更久远数据待后续支持",
                        "4、数据拉取的速度依赖佳明服务器推送，触发数据同步后，尽量保持应用位于前台，避免同步异常",
                    )
                ),
                // 佳明国际
                DataSourceInfo(
                    platform = DataSourcePlatform.GARMIN_GLOBAL,
                    descriptions = listOf(
                        "1、授权时，记得打开历史数据同步开关，该开关默认为关闭，需要手动打开，否则无法获取历史数据",
                        "2、账号授权后，会自动同步之后的新增数据",
                        "3、手动同步支持同步近一个月数据，同步更久远数据待后续支持",
                        "4、数据拉取的速度依赖佳明服务器推送，触发数据同步后，尽量保持应用位于前台，避免同步异常",
                    )
                ),
                // COROS 高驰
                DataSourceInfo(
                    platform = DataSourcePlatform.COROS,
                    descriptions = listOf(
                        "1、注意！授权时，记得勾选同意第三方在24小时内获取历史数据，否则可能出现获取数据不全",
                        "2、账号授权后，会自动同步之后的新增数据",
                        "3、手动同步支持同步近一年数据，若发现未能同步一年内数据，尝试取消授权后重新授权；若要支持同步更久远的数据，联系客服",
                        "4、数据拉取的速度依赖高驰服务器推送，触发数据同步后，尽量保持应用位于前台，避免同步异常",
                    )
                ),
                // 手动导入
                DataSourceInfo(
                    platform = DataSourcePlatform.MANUAL,
                    descriptions = listOf(
                        "1、支持导入单个或批量FIT文件",
                        "2、导入后数据会自动解析并存储到本地",
                        "3、支持的文件格式：.fit（Garmin、COROS等设备导出的运动数据文件）",
                    )
                ),
                // 苹果健康
                DataSourceInfo(
                    platform = DataSourcePlatform.APPLE_HEALTH,
                    descriptions = listOf(
                        "1、数据来自其他苹果设备同步到服务器，无需授权",
                        "2、支持手动同步，点击手动同步按钮即可拉取最新数据",
                        "3、同步的数据包括跑步、步行等运动记录",
                    )
                ),
            )
        }

        /**
         * 根据平台获取数据源信息
         */
        fun getDataSource(platform: DataSourcePlatform): DataSourceInfo? {
            return getAllDataSources().find { it.platform == platform }
        }
    }
}

