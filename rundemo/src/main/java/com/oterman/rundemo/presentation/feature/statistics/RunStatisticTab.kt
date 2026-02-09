package com.oterman.rundemo.presentation.feature.statistics

/**
 * 跑步统计页面的Tab枚举
 * 定义周/月/年/总四个统计维度
 */
enum class RunStatisticTab(val label: String) {
    WEEK("周"),
    MONTH("月"),
    YEAR("年"),
    TOTAL("总");

    companion object {
        /**
         * 根据名称获取对应的Tab
         * @param name tab名称（不区分大小写）
         * @return 对应的Tab，默认返回WEEK
         */
        fun fromName(name: String): RunStatisticTab =
            entries.find { it.name.equals(name, ignoreCase = true) } ?: WEEK
    }
}
