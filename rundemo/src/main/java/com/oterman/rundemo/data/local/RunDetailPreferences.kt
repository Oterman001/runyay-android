package com.oterman.rundemo.data.local

import android.content.Context
import android.content.SharedPreferences

/**
 * 跑步详情页 UI 偏好存储
 * 目前保存：公里分段视图模式（柱状图/表格）、第三列指标索引
 */
class RunDetailPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    fun saveSegmentBarChartMode(isBarChart: Boolean) {
        prefs.edit().putBoolean(KEY_SEGMENT_BAR_CHART_MODE, isBarChart).apply()
    }

    fun getSegmentBarChartMode(): Boolean {
        return prefs.getBoolean(KEY_SEGMENT_BAR_CHART_MODE, DEFAULT_BAR_CHART_MODE)
    }

    fun saveSegmentMetricIndex(index: Int) {
        prefs.edit().putInt(KEY_SEGMENT_METRIC_INDEX, index).apply()
    }

    fun getSegmentMetricIndex(): Int {
        return prefs.getInt(KEY_SEGMENT_METRIC_INDEX, 0)
    }

    companion object {
        private const val PREFS_NAME = "run_detail_prefs"
        private const val KEY_SEGMENT_BAR_CHART_MODE = "segment_bar_chart_mode"
        private const val KEY_SEGMENT_METRIC_INDEX = "segment_metric_index"
        private const val DEFAULT_BAR_CHART_MODE = true
    }
}
