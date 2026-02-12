package com.oterman.rundemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.network.RetrofitClient
import com.oterman.rundemo.presentation.navigation.AppNavGraph
import com.oterman.rundemo.presentation.navigation.Screen
import com.oterman.rundemo.ui.theme.ComopseDemoHubTheme
import com.oterman.rundemo.util.RLog

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
        
        enableEdgeToEdge()
        
        setContent {
            ComopseDemoHubTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    // 根据登录状态决定起始页面
                    val startDestination = if (preferencesManager.isUserLoggedIn()) {
                        Screen.Home.route
                    } else {
                        Screen.Welcome.route
                    }
                    
                    // 设置应用导航图
                    AppNavGraph(
                        navController = navController,
                        startDestination = startDestination
                    )
                }
            }
        }
    }
}