package com.oterman.rundemo.presentation.feature.share

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.oterman.rundemo.ui.theme.ComopseDemoHubTheme
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

        fun createIntent(context: Context, workoutId: String): Intent {
            return Intent(context, ShareActivity::class.java).apply {
                putExtra(EXTRA_WORKOUT_ID, workoutId)
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

        setContent {
            ComopseDemoHubTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ShareScreen(
                        workoutId = workoutId,
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
