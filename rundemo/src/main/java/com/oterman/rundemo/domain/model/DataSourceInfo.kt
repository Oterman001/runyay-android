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
                        "1、如果当前安装了佳明Connect，请确保佳明Connect登录的账号为中国区域账号，否则可能无法正常授权",
                        "2、拉起佳明Connect应用后，不要做其他操作，请耐心等待授权，避免授权失败",
                        "3、账号授权后，会自动同步之后的新增数据",
                        "4、手动同步支持同步近一年数据，若发现一年内数据有遗漏，尝试取消授权后重新授权",
                        "5、数据拉取的速度依赖佳明服务器推送，触发数据同步后，尽量保持应用位于前台，避免同步异常"
                    )
                ),
                // 佳明国际
                DataSourceInfo(
                    platform = DataSourcePlatform.GARMIN_GLOBAL,
                    descriptions = listOf(
                        "1、如果当前安装了佳明Connect，请确保佳明Connect登录的账号为国际区域账号，否则可能无法正常授权",
                        "2、拉起佳明Connect应用后，不要做其他操作，国际区授权耗时较久，请耐心等待授权，避免授权失败",
                        "3、账号授权后，会自动同步之后的新增数据",
                        "4、手动同步支持同步近一个月数据，同步更久远数据待后续支持",
                        "5、数据拉取的速度依赖佳明服务器推送，触发数据同步后，尽量保持应用位于前台，避免同步异常"
                    )
                ),
                // COROS 高驰
                DataSourceInfo(
                    platform = DataSourcePlatform.COROS,
                    descriptions = listOf(
                        "1、账号授权后，会自动同步之后的新增数据",
                        "2、手动同步支持同步近一年数据，若发现未能同步一年内数据，尝试取消授权后重新授权",
                        "3、数据拉取的速度依赖高驰服务器推送，触发数据同步后，尽量保持应用位于前台，避免同步异常"
                    )
                ),
//                // 华为运动健康 (暂未支持)
//                DataSourceInfo(
//                    platform = DataSourcePlatform.HUAWEI,
//                    descriptions = listOf(
//                        "1、功能即将开放，敬请期待"
//                    )
//                )
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

