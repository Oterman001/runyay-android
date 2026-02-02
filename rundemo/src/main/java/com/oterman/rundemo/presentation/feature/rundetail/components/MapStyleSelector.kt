package com.oterman.rundemo.presentation.feature.rundetail.components

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Satellite
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.mapbox.maps.Style

/**
 * Mapbox地图风格数据模型
 */
data class RunMapStyle(
    val id: String,
    val name: String,
    val description: String,
    val styleUri: String,
    val icon: ImageVector
)

/**
 * 所有可用的Mapbox地图风格
 */
object RunMapStyles {
    val STANDARD = RunMapStyle(
        id = "standard",
        name = "标准",
        description = "通用地图风格",
        styleUri = Style.STANDARD,
        icon = Icons.Default.Map
    )

    val OUTDOORS = RunMapStyle(
        id = "outdoors",
        name = "户外",
        description = "适合户外跑步和越野",
        styleUri = Style.OUTDOORS,
        icon = Icons.Default.Terrain
    )

    val LIGHT = RunMapStyle(
        id = "light",
        name = "浅色",
        description = "简洁的浅色主题",
        styleUri = Style.LIGHT,
        icon = Icons.Default.LightMode
    )

    val DARK = RunMapStyle(
        id = "dark",
        name = "深色",
        description = "夜间模式深色主题",
        styleUri = Style.DARK,
        icon = Icons.Default.DarkMode
    )

    val SATELLITE = RunMapStyle(
        id = "satellite",
        name = "卫星",
        description = "真实卫星影像",
        styleUri = Style.SATELLITE,
        icon = Icons.Default.Satellite
    )

    val SATELLITE_STREETS = RunMapStyle(
        id = "satellite_streets",
        name = "卫星街道",
        description = "卫星影像叠加街道信息",
        styleUri = Style.SATELLITE_STREETS,
        icon = Icons.Default.SatelliteAlt
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
    fun getStyleByUri(uri: String): RunMapStyle {
        return ALL_STYLES.find { it.styleUri == uri } ?: OUTDOORS
    }
}

/**
 * 地图风格偏好设置管理器
 */
object RunMapPreferences {
    private const val PREFS_NAME = "run_map_prefs"
    private const val KEY_MAP_STYLE = "map_style"

    /**
     * 获取用户保存的地图风格
     * @return 返回保存的风格URI，如果没有保存则返回默认的OUTDOORS风格
     */
    fun getMapStyle(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_MAP_STYLE, Style.OUTDOORS) ?: Style.OUTDOORS
    }

    /**
     * 保存用户选择的地图风格
     */
    fun saveMapStyle(context: Context, styleUri: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MAP_STYLE, styleUri)
            .apply()
    }
}

/**
 * 地图风格选择器底部弹窗
 *
 * @param currentStyleUri 当前选中的风格URI
 * @param onStyleSelected 风格被选中的回调
 * @param onDismiss 关闭弹窗的回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunMapStyleBottomSheet(
    currentStyleUri: String,
    onStyleSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "选择地图风格",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn {
                items(RunMapStyles.ALL_STYLES) { style ->
                    MapStyleItem(
                        style = style,
                        isSelected = style.styleUri == currentStyleUri,
                        onClick = {
                            onStyleSelected(style.styleUri)
                        }
                    )

                    Spacer(modifier = Modifier.padding(vertical = 4.dp))
                }

                // 底部额外空间
                item {
                    Spacer(modifier = Modifier.padding(bottom = 16.dp))
                }
            }
        }
    }
}

/**
 * 单个地图风格选项卡片
 */
@Composable
private fun MapStyleItem(
    style: RunMapStyle,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = style.icon,
                contentDescription = style.name,
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = style.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                Text(
                    text = style.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}
