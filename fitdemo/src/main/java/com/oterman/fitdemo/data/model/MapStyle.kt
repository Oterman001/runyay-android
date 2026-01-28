package com.oterman.fitdemo.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.ui.graphics.vector.ImageVector
import com.mapbox.maps.Style

/**
 * 地图风格数据模型
 */
data class MapStyle(
    val id: String,
    val name: String,
    val description: String,
    val styleUri: String,
    val icon: ImageVector
)

/**
 * 所有可用的Mapbox地图风格
 */
object MapStyles {
    val STANDARD = MapStyle(
        id = "standard",
        name = "标准",
        description = "通用地图风格",
        styleUri = Style.STANDARD,
        icon = Icons.Default.Place
    )
    
    val OUTDOORS = MapStyle(
        id = "outdoors",
        name = "户外",
        description = "适合徒步和户外活动",
        styleUri = Style.OUTDOORS,
        icon = Icons.Default.Place
    )
    
    val LIGHT = MapStyle(
        id = "light",
        name = "亮色",
        description = "简洁的浅色主题",
        styleUri = Style.MAPBOX_STREETS,
        icon = Icons.Default.Place
    )
    
    val DARK = MapStyle(
        id = "dark",
        name = "深色",
        description = "夜间模式深色主题",
        styleUri = Style.DARK,
        icon = Icons.Default.Place
    )
    
    val SATELLITE = MapStyle(
        id = "satellite",
        name = "卫星",
        description = "真实卫星影像",
        styleUri = Style.SATELLITE,
        icon = Icons.Default.Place
    )
    
    val SATELLITE_STREETS = MapStyle(
        id = "satellite_streets",
        name = "卫星街道",
        description = "卫星影像叠加街道信息",
        styleUri = Style.SATELLITE_STREETS,
        icon = Icons.Default.Place
    )
    
    /**
     * 所有可用风格列表
     */
    val ALL_STYLES = listOf(
        STANDARD,
        OUTDOORS,
        LIGHT,
        DARK,
        SATELLITE,
        SATELLITE_STREETS
    )
    
    /**
     * 根据URI获取风格
     */
    fun getStyleByUri(uri: String): MapStyle {
        return ALL_STYLES.find { it.styleUri == uri } ?: STANDARD
    }
}

