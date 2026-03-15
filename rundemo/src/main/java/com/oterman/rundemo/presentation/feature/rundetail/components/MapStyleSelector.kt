package com.oterman.rundemo.presentation.feature.rundetail.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.oterman.rundemo.domain.map.MapProvider
import com.oterman.rundemo.domain.map.MapStyleInfo
import com.oterman.rundemo.domain.map.TrackMapRenderer
import com.oterman.rundemo.ui.theme.RunTheme

/**
 * 地图风格偏好设置管理器
 */
object RunMapPreferences {
    private const val PREFS_NAME = "run_map_prefs"
    private const val KEY_MAP_STYLE_LIGHT = "map_style_light"
    private const val KEY_MAP_STYLE_DARK  = "map_style_dark"
    private const val KEY_SHOW_KM_MARKERS = "show_km_markers"
    private const val KEY_KM_MARKER_INTERVAL = "km_marker_interval"
    private const val KEY_MAP_PROVIDER = "map_provider"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getMapProvider(context: Context): MapProvider {
        val saved = prefs(context).getString(KEY_MAP_PROVIDER, null)
        return try {
            if (saved != null) MapProvider.valueOf(saved) else MapProvider.MAPBOX
        } catch (e: Exception) {
            MapProvider.MAPBOX
        }
    }

    fun saveMapProvider(context: Context, provider: MapProvider) {
        prefs(context).edit().putString(KEY_MAP_PROVIDER, provider.name).apply()
    }

    fun getMapStyle(context: Context, isDarkTheme: Boolean, renderer: TrackMapRenderer): String {
        val key = if (isDarkTheme) KEY_MAP_STYLE_DARK else KEY_MAP_STYLE_LIGHT
        val saved = prefs(context).getString(key, null)
            ?: return renderer.getDefaultStyle(isDarkTheme).styleUri
        val availableUris = renderer.getAvailableStyles(isDarkTheme).map { it.styleUri }
        return if (saved in availableUris) saved else renderer.getDefaultStyle(isDarkTheme).styleUri
    }

    fun saveMapStyle(context: Context, isDarkTheme: Boolean, styleUri: String) {
        val key = if (isDarkTheme) KEY_MAP_STYLE_DARK else KEY_MAP_STYLE_LIGHT
        prefs(context).edit().putString(key, styleUri).apply()
    }

    fun getShowKmMarkers(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_SHOW_KM_MARKERS, true)
    }

    fun saveShowKmMarkers(context: Context, show: Boolean) {
        prefs(context).edit().putBoolean(KEY_SHOW_KM_MARKERS, show).apply()
    }

    fun getKmMarkerInterval(context: Context): Int {
        return prefs(context).getInt(KEY_KM_MARKER_INTERVAL, 1)
    }

    fun saveKmMarkerInterval(context: Context, interval: Int) {
        prefs(context).edit().putInt(KEY_KM_MARKER_INTERVAL, interval.coerceIn(1, 10)).apply()
    }

    fun clearAll(context: Context) {
        prefs(context).edit().clear().apply()
    }
}

/**
 * 地图设置底部弹窗
 * 包含：1) 地图供应商切换  2) 地图风格横向选择  3) 公里点开关 + 间隔控制
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunMapSettingBottomSheet(
    currentProvider: MapProvider,
    currentStyleUri: String,
    showKmMarkers: Boolean,
    kmMarkerInterval: Int,
    renderer: TrackMapRenderer,
    onProviderChanged: (MapProvider) -> Unit,
    onStyleSelected: (String) -> Unit,
    onKmMarkersToggled: (Boolean) -> Unit,
    onKmIntervalChanged: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isDarkTheme = RunTheme.isDark
    val styles = renderer.getAvailableStyles(isDarkTheme)

    var tempShowKm by remember { mutableStateOf(showKmMarkers) }
    var tempInterval by remember { mutableFloatStateOf(kmMarkerInterval.toFloat()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 32.dp)
        ) {
            // ========== 第零部分：地图供应商切换 ==========
            Text(
                text = "地图供应商",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MapProvider.entries.forEach { provider ->
                    FilterChip(
                        selected = provider == currentProvider,
                        onClick = {
                            if (provider != currentProvider) {
                                onProviderChanged(provider)
                            }
                        },
                        label = { Text(provider.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RunTheme.colorScheme.blue.copy(alpha = 0.15f),
                            selectedLabelColor = RunTheme.colorScheme.blue
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))

            // ========== 第一部分：地图风格 ==========
            Text(
                text = "地图风格",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(styles) { style ->
                    MapStyleCard(
                        style = style,
                        isSelected = style.styleUri == currentStyleUri,
                        onClick = { onStyleSelected(style.styleUri) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            // ========== 第二部分：公里点设置 ==========
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "展示公里点",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = tempShowKm,
                    onCheckedChange = { newValue ->
                        tempShowKm = newValue
                        onKmMarkersToggled(newValue)
                    },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = RunTheme.colorScheme.blue
                    )
                )
            }

            AnimatedVisibility(
                visible = tempShowKm,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                KmIntervalSetting(
                    intervalValue = tempInterval,
                    onIntervalChange = { newVal ->
                        tempInterval = newVal
                        onKmIntervalChanged(newVal.toInt())
                    }
                )
            }
        }
    }
}

@Composable
private fun KmIntervalSetting(
    intervalValue: Float,
    onIntervalChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        Text(
            text = "公里点间隔",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "每",
                style = MaterialTheme.typography.bodyMedium
            )

            Slider(
                value = intervalValue,
                onValueChange = onIntervalChange,
                valueRange = 1f..10f,
                steps = 8,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )

            Text(
                text = "${intervalValue.toInt()} 公里",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.width(52.dp)
            )
        }

        Text(
            text = getIntervalPreviewText(intervalValue.toInt()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

private fun getIntervalPreviewText(interval: Int): String {
    return if (interval == 1) {
        "将在每公里处显示公里点"
    } else {
        val examples = (1..3).map { it * interval }
        val exampleStr = examples.joinToString(", ") { "第${it}公里" }
        "将在${exampleStr}...处显示公里点"
    }
}

/**
 * 横向风格卡片（图标 + 名称纵向排列）
 */
@Composable
private fun MapStyleCard(
    style: MapStyleInfo,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val blueColor = RunTheme.colorScheme.blue
    val borderStroke = if (isSelected) {
        BorderStroke(2.dp, blueColor)
    } else {
        null
    }

    Card(
        modifier = modifier
            .width(76.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        border = borderStroke,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                blueColor.copy(alpha = 0.12f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = style.icon,
                contentDescription = style.name,
                modifier = Modifier.size(28.dp),
                tint = if (isSelected) {
                    blueColor
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = style.name,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) {
                    blueColor
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}
