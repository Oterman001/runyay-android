package com.oterman.fitdemo.util

import android.content.Context
import com.mapbox.maps.Style

/**
 * 地图偏好设置管理器
 * 用于保存和读取用户的地图风格选择
 */
object MapPreferences {
    private const val PREFS_NAME = "mapbox_prefs"
    private const val KEY_SELECTED_STYLE = "selected_style"
    
    /**
     * 保存用户选择的地图风格
     */
    fun saveMapStyle(context: Context, styleUri: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SELECTED_STYLE, styleUri)
            .apply()
    }
    
    /**
     * 获取用户保存的地图风格
     * @return 返回保存的风格URI，如果没有保存则返回默认的STANDARD风格
     */
    fun getMapStyle(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SELECTED_STYLE, Style.STANDARD) ?: Style.STANDARD
    }
    
    /**
     * 清除保存的地图风格偏好
     */
    fun clearMapStyle(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_SELECTED_STYLE)
            .apply()
    }
}


