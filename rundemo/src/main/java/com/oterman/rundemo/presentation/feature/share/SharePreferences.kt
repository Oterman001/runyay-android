package com.oterman.rundemo.presentation.feature.share

import android.content.Context
import android.content.SharedPreferences

/**
 * 分享设置持久化
 * 保存用户选择的指标、卡片开关、日期显示等偏好
 */
class SharePreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    /**
     * 保存短图选中的指标列表
     */
    fun saveSelectedMetrics(metrics: List<ShareMetricType>) {
        val names = metrics.joinToString(",") { it.name }
        prefs.edit().putString(KEY_SELECTED_METRICS, names).apply()
    }

    /**
     * 获取短图选中的指标列表
     */
    fun getSelectedMetrics(): List<ShareMetricType>? {
        val names = prefs.getString(KEY_SELECTED_METRICS, null) ?: return null
        return names.split(",").mapNotNull { name ->
            try { ShareMetricType.valueOf(name) } catch (_: Exception) { null }
        }.ifEmpty { null }
    }

    /**
     * 保存长图卡片开关
     */
    fun saveEnabledCards(cards: Map<ShareCardType, Boolean>) {
        val editor = prefs.edit()
        cards.forEach { (type, enabled) ->
            editor.putBoolean("${KEY_CARD_PREFIX}${type.name}", enabled)
        }
        editor.apply()
    }

    /**
     * 获取长图卡片开关
     */
    fun getEnabledCards(): Map<ShareCardType, Boolean> {
        val result = mutableMapOf<ShareCardType, Boolean>()
        ShareCardType.entries.forEach { type ->
            val key = "${KEY_CARD_PREFIX}${type.name}"
            if (prefs.contains(key)) {
                result[type] = prefs.getBoolean(key, true)
            }
        }
        return result.ifEmpty { ShareCardType.entries.associateWith { true } }
    }

    /**
     * 保存是否显示日期
     */
    fun saveShowDate(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_DATE, show).apply()
    }

    /**
     * 获取是否显示日期
     */
    fun getShowDate(): Boolean {
        return prefs.getBoolean(KEY_SHOW_DATE, true)
    }

    /**
     * 保存自定义品牌文案
     */
    fun saveBrandText(text: String) {
        prefs.edit().putString(KEY_BRAND_TEXT, text).apply()
    }

    /**
     * 获取自定义品牌文案
     */
    fun getBrandText(): String {
        return prefs.getString(KEY_BRAND_TEXT, "") ?: ""
    }

    /**
     * 保存自定义设备名
     */
    fun saveCustomDeviceName(name: String?) {
        if (name != null) {
            prefs.edit().putString(KEY_CUSTOM_DEVICE_NAME, name).apply()
        } else {
            prefs.edit().remove(KEY_CUSTOM_DEVICE_NAME).apply()
        }
    }

    /**
     * 获取自定义设备名
     */
    fun getCustomDeviceName(): String? {
        return prefs.getString(KEY_CUSTOM_DEVICE_NAME, null)
    }

    companion object {
        private const val PREFS_NAME = "share_prefs"
        private const val KEY_SELECTED_METRICS = "selected_metrics"
        private const val KEY_CARD_PREFIX = "card_enabled_"
        private const val KEY_SHOW_DATE = "show_date"
        private const val KEY_BRAND_TEXT = "brand_text"
        private const val KEY_CUSTOM_DEVICE_NAME = "custom_device_name"
    }
}
