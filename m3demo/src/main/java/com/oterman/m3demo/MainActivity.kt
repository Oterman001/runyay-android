package com.oterman.m3demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.oterman.m3demo.screens.MainScreen
import com.oterman.m3demo.ui.theme.ComopseDemoHubTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ComopseDemoHubTheme {
                MainScreen()
            }
        }
    }
}