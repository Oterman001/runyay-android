package com.oterman.fitdemo

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oterman.fitdemo.ui.screen.FitFileScreen
import com.oterman.fitdemo.ui.theme.ComopseDemoHubTheme
import com.oterman.fitdemo.viewmodel.FitViewModel

class MainActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        Log.d(TAG, "onCreate")
        
        setContent {
            ComopseDemoHubTheme {
                // 正确创建ViewModel
                val viewModel: FitViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsState()
                
                Log.d(TAG, "当前UI状态: $uiState")
                
                // 文件选择器
                val filePickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri ->
                    Log.d(TAG, "选择的文件URI: $uri")
                    uri?.let { 
                        viewModel.parseFitFile(it)
                    } ?: Log.w(TAG, "URI为空，用户取消选择")
                }
                
                FitFileScreen(
                    uiState = uiState,
                    onSelectFile = {
                        Log.d(TAG, "点击选择文件按钮")
                        viewModel.resetState()
                        filePickerLauncher.launch("*/*")
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}