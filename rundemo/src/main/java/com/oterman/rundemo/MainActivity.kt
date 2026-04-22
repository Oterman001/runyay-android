package com.oterman.rundemo

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 主Activity
 * 应用的入口，负责设置导航和主题
 */
class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"

        /** 通过系统分享到达时传递的待导入 URI 列表（供 Compose 侧订阅）。
         *  使用 StateFlow（replay=1 语义）而非 SharedFlow，确保晚订阅的 ManualImportScreen
         *  也能收到当前值，避免 onNewIntent 路径下 URI 丢失。
         */
        private val _pendingShareUris = MutableStateFlow<List<Uri>>(emptyList())
        val pendingShareUris: StateFlow<List<Uri>> = _pendingShareUris.asStateFlow()

        /** 消费完待导入 URI 后调用，防止重复触发导入 */
        fun clearPendingShareUris() {
            _pendingShareUris.value = emptyList()
        }
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
        RetrofitClient.setContext(this)

        // 初始化跑步数据仓库的userId
        initRunDataUserId()

        // 在 setContent 之前捕获分享 Intent 中的 URI，写入 StateFlow。
        // Compose 层的 LaunchedEffect 会统一观察并处理导航，覆盖 onCreate 和 onNewIntent 两条路径。
        val initialShareUris = extractShareUris(intent)
        if (initialShareUris.isNotEmpty()) {
            RLog.i(TAG, "onCreate 检测到分享启动，URI 数量: ${initialShareUris.size}")
            _pendingShareUris.value = initialShareUris
        }

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

                    // 根据登录状态决定起始页面（首次启动需先同意隐私政策）
                    val startDestination = when {
                        !preferencesManager.isPrivacyConsentAccepted() -> Screen.PrivacyConsent.route
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

                    // 统一监听待导入 URI（覆盖 onCreate 和 onNewIntent 两条路径）：
                    // 已登录时直接导航到 ManualImport；未登录时 URI 保留在 StateFlow，
                    // 由 NavGraph 的 Home composable 在用户登录后检查并补充导航。
                    LaunchedEffect(navController) {
                        pendingShareUris.collect { uris ->
                            if (uris.isNotEmpty() && preferencesManager.isUserLoggedIn()) {
                                RLog.i(TAG, "pendingShareUris 触发导航至 ManualImport，URI 数量: ${uris.size}")
                                navController.navigate(Screen.ManualImport.route) {
                                    launchSingleTop = true
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val uris = extractShareUris(intent)
        if (uris.isNotEmpty()) {
            RLog.i(TAG, "onNewIntent 收到分享，URI 数量: ${uris.size}")
            _pendingShareUris.value = uris
        }
    }

    /**
     * 从 Intent 中提取需要导入的文件 URI 列表。
     * 兼容 ACTION_VIEW（打开方式）、ACTION_SEND（单文件分享）、ACTION_SEND_MULTIPLE（多文件分享）。
     */
    @Suppress("DEPRECATION")
    private fun extractShareUris(intent: Intent): List<Uri> {
        return when (intent.action) {
            Intent.ACTION_VIEW -> listOfNotNull(intent.data)
            Intent.ACTION_SEND -> {
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
                }
                listOfNotNull(uri)
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("UNCHECKED_CAST")
                    intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                }.orEmpty()
            }
            else -> emptyList()
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
