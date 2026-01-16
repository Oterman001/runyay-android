package com.oterman.fitdemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.oterman.fitdemo.ui.screen.FitFileScreen
import com.oterman.fitdemo.ui.theme.ComopseDemoHubTheme
import com.oterman.fitdemo.viewmodel.FitViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            ComopseDemoHubTheme {
                // 创建ViewModel
                val viewModel = FitViewModel(applicationContext)
                val uiState by viewModel.uiState.collectAsState()
                
                // 文件选择器
                val filePickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri ->
                    uri?.let { viewModel.parseFitFile(it) }
                }
                
                FitFileScreen(
                    uiState = uiState,
                    onSelectFile = {
                        viewModel.resetState()
                        filePickerLauncher.launch("*/*")
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}