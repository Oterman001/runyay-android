package com.oterman.rundemo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.presentation.feature.runningshoes.detail.RunningShoeDetailScreen
import com.oterman.rundemo.ui.theme.ComopseDemoHubTheme
import com.oterman.rundemo.ui.theme.ThemeMode

/**
 * 跑鞋详情独立Activity
 *
 * 从跑步详情页点击关联跑鞋卡片时启动，
 * 因为 RunDetailScreen 在独立的 RunDetailActivity 中，无法直接使用 MainActivity 的 NavController。
 */
class RunningShoeDetailActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_SHOE_ID = "shoe_id"

        fun createIntent(context: Context, shoeId: String): Intent {
            return Intent(context, RunningShoeDetailActivity::class.java).apply {
                putExtra(EXTRA_SHOE_ID, shoeId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val shoeId = intent.getStringExtra(EXTRA_SHOE_ID) ?: run {
            finish()
            return
        }

        setContent {
            val preferencesManager = PreferencesManager(this)
            val darkTheme = when (preferencesManager.getThemeMode()) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.AUTO -> isSystemInDarkTheme()
            }

            ComopseDemoHubTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RunningShoeDetailScreen(
                        shoeId = shoeId,
                        onNavigateBack = { finish() },
                        showActions = false
                    )
                }
            }
        }
    }
}
