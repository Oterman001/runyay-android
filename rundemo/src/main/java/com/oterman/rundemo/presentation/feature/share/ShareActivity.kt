package com.oterman.rundemo.presentation.feature.share

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.ui.theme.ComopseDemoHubTheme
import com.oterman.rundemo.ui.theme.ThemeMode
import com.oterman.rundemo.util.RLog

/**
 * 分享页面独立Activity
 *
 * 与 RunDetailActivity 同级，使用独立 Activity 方式:
 * - 避免修改 RunDetailActivity 的 Compose 结构
 * - Activity 间数据通过 workoutId + ShareDataCache 传递
 */
class ShareActivity : ComponentActivity() {

    companion object {
        private const val TAG = "ShareActivity"
        private const val EXTRA_WORKOUT_ID = "workout_id"
        private const val EXTRA_SEGMENT_BAR_CHART_MODE = "segment_bar_chart_mode"
        private const val EXTRA_SEGMENT_METRIC_INDEX = "segment_metric_index"
        private const val EXTRA_SEGMENT_GROUP_SIZE = "segment_group_size"

        fun createIntent(
            context: Context,
            workoutId: String,
            segmentBarChartMode: Boolean = false,
            segmentMetricIndex: Int = 0,
            segmentGroupSize: Int = 1
        ): Intent {
            return Intent(context, ShareActivity::class.java).apply {
                putExtra(EXTRA_WORKOUT_ID, workoutId)
                putExtra(EXTRA_SEGMENT_BAR_CHART_MODE, segmentBarChartMode)
                putExtra(EXTRA_SEGMENT_METRIC_INDEX, segmentMetricIndex)
                putExtra(EXTRA_SEGMENT_GROUP_SIZE, segmentGroupSize)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        RLog.d(TAG, "onCreate START")
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val workoutId = intent.getStringExtra(EXTRA_WORKOUT_ID) ?: run {
            RLog.e(TAG, "workoutId is null, finishing activity")
            finish()
            return
        }
        val segmentBarChartMode = intent.getBooleanExtra(EXTRA_SEGMENT_BAR_CHART_MODE, false)
        val segmentMetricIndex = intent.getIntExtra(EXTRA_SEGMENT_METRIC_INDEX, 0)
        val segmentGroupSize = intent.getIntExtra(EXTRA_SEGMENT_GROUP_SIZE, 1)

        setContent {
            val preferencesManager = PreferencesManager(this)
            val darkTheme = when (preferencesManager.getThemeMode()) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.AUTO -> isSystemInDarkTheme()
            }

            LaunchedEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (darkTheme) SystemBarStyle.dark(Color.TRANSPARENT)
                    else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                    navigationBarStyle = if (darkTheme) SystemBarStyle.dark(Color.TRANSPARENT)
                    else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                )
            }

            ComopseDemoHubTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ShareScreen(
                        workoutId = workoutId,
                        segmentBarChartMode = segmentBarChartMode,
                        segmentMetricIndex = segmentMetricIndex,
                        segmentGroupSize = segmentGroupSize,
                        onNavigateBack = { finish() }
                    )
                }
            }
        }
        RLog.d(TAG, "onCreate END")
    }

    override fun onDestroy() {
        RLog.d(TAG, "onDestroy")
        ShareDataCache.clear()
        super.onDestroy()
    }
}
