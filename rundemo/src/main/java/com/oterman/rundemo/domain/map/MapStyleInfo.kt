package com.oterman.rundemo.domain.map

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 地图样式描述（供应商无关）
 */
data class MapStyleInfo(
    val id: String,
    val name: String,
    val description: String,
    val styleUri: String,
    val icon: ImageVector
)
