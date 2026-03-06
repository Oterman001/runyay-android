package com.oterman.rundemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.local.database.RunDatabase
import com.oterman.rundemo.data.network.RetrofitClient
import com.oterman.rundemo.data.repository.RunDataRepositoryImpl
import com.oterman.rundemo.presentation.navigation.AppNavGraph
import com.oterman.rundemo.presentation.navigation.Screen
import com.oterman.rundemo.ui.theme.ComopseDemoHubTheme
import com.oterman.rundemo.ui.theme.ThemeMode
import com.oterman.rundemo.util.RLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 主Activity
 * 应用的入口，负责设置导航和主题
 */
class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var preferencesManager: PreferencesManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化PreferencesManager
        preferencesManager = PreferencesManager(this)

        // 初始化Logger（默认启用文件日志，设置 tag 前缀为 XRUN）
        RLog.init(this, prefix = "XRUN")
        RLog.i(TAG, "应用启动")

        // 设置Retrofit的token提供者
        RetrofitClient.setTokenProvider {
            preferencesManager.getUserToken()
        }

        // 初始化跑步数据仓库的userId
        initRunDataUserId()

        enableEdgeToEdge()
        
        setContent {
            var themeMode by remember { mutableStateOf(preferencesManager.getThemeMode()) }
            val darkTheme = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.AUTO -> isSystemInDarkTheme()
            }

            ComopseDemoHubTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    // 根据登录状态决定起始页面
                    val startDestination = when {
                        !preferencesManager.isUserLoggedIn() -> Screen.Welcome.route
                        !preferencesManager.isPhysioSetupCompleted() -> Screen.PhysioSetup.createRoute("home")
                        else -> Screen.Home.route
                    }

                    // 设置应用导航图
                    AppNavGraph(
                        navController = navController,
                        startDestination = startDestination,
                        onThemeModeChanged = { themeMode = it }
                    )
                }
            }
        }
    }

    private fun initRunDataUserId() {
        if (!preferencesManager.isUserLoggedIn()) return
        val userId = preferencesManager.getUserId() ?: return

        val database = RunDatabase.getInstance(this)
        val repository = RunDataRepositoryImpl.getInstance(database)
        repository.setCurrentUserId(userId)

        // 一次性迁移旧数据（userId为空的记录归属到当前用户）
        val migrationKey = "user_data_migrated_$userId"
        val prefs = getSharedPreferences("run_data_migration", MODE_PRIVATE)
        if (!prefs.getBoolean(migrationKey, false)) {
            CoroutineScope(Dispatchers.IO).launch {
                repository.migrateOrphanedRecords(userId)
                prefs.edit().putBoolean(migrationKey, true).apply()
                RLog.i(TAG, "旧数据迁移完成: userId=$userId")
            }
        }
    }
}