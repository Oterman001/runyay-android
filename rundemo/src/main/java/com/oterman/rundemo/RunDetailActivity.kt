package com.oterman.rundemo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.oterman.rundemo.presentation.feature.rundetail.RunDetailScreen
import com.oterman.rundemo.ui.theme.ComopseDemoHubTheme

/**
 * 跑步详情独立Activity
 *
 * 使用独立Activity而非Compose Navigation的原因：
 * - MapView.onDestroy() 是同步操作，会阻塞主线程50-300ms
 * - Compose Navigation返回时序：popBackStack() → 移除Composition → onDestroy() → 显示Home
 * - 独立Activity返回时序：finish() → 播放动画 → Home可见 → onDestroy()（后台执行）
 *
 * 这样MapView销毁在用户看到Home页面之后才执行，不会阻塞返回动画
 */
class RunDetailActivity : ComponentActivity() {

    companion object {
        private const val TAG = "RunDetailPerf"
        private const val EXTRA_WORKOUT_ID = "workout_id"

        fun createIntent(context: Context, workoutId: String): Intent {
            return Intent(context, RunDetailActivity::class.java).apply {
                putExtra(EXTRA_WORKOUT_ID, workoutId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "onCreate START")
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val workoutId = intent.getStringExtra(EXTRA_WORKOUT_ID) ?: run {
            Log.e(TAG, "workoutId is null, finishing activity")
            finish()
            return
        }

        setContent {
            ComopseDemoHubTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RunDetailScreen(
                        workoutId = workoutId,
                        onNavigateBack = { finish() }
                    )
                }
            }
        }
        Log.d(TAG, "onCreate END, cost=${System.currentTimeMillis() - startTime}ms")
    }

    override fun onStart() {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "onStart START")
        super.onStart()
        Log.d(TAG, "onStart END, cost=${System.currentTimeMillis() - startTime}ms")
    }

    override fun onStop() {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "onStop START")
        super.onStop()
        Log.d(TAG, "onStop END, cost=${System.currentTimeMillis() - startTime}ms")
    }

    override fun onDestroy() {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "onDestroy START")
        super.onDestroy()
        Log.d(TAG, "onDestroy END, cost=${System.currentTimeMillis() - startTime}ms")
    }

    override fun finish() {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "finish() called")
        super.finish()
        Log.d(TAG, "finish() returned, cost=${System.currentTimeMillis() - startTime}ms")
    }
}
