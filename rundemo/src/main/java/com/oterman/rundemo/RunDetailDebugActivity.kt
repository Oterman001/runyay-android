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
import com.oterman.rundemo.presentation.feature.rundetail.RunDetailDebugScreen
import com.oterman.rundemo.ui.theme.ComopseDemoHubTheme
import com.oterman.rundemo.ui.theme.ThemeMode

/**
 * 跑步详情调试页独立Activity
 */
class RunDetailDebugActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_WORKOUT_ID = "workout_id"

        fun createIntent(context: Context, workoutId: String): Intent {
            return Intent(context, RunDetailDebugActivity::class.java).apply {
                putExtra(EXTRA_WORKOUT_ID, workoutId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val workoutId = intent.getStringExtra(EXTRA_WORKOUT_ID) ?: run {
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
                    RunDetailDebugScreen(
                        workoutId = workoutId,
                        onNavigateBack = { finish() }
                    )
                }
            }
        }
    }
}
