package com.oterman.m3demo.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.oterman.m3demo.screens.Tab1Screen
import com.oterman.m3demo.screens.Tab2Screen
import com.oterman.m3demo.screens.Tab3Screen

/**
 * 导航图配置
 * 管理三个 Tab 页面之间的导航
 */
@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = BottomNavItem.Tab1.route
    ) {
        composable(BottomNavItem.Tab1.route) {
            Tab1Screen()
        }
        
        composable(BottomNavItem.Tab2.route) {
            Tab2Screen()
        }
        
        composable(BottomNavItem.Tab3.route) {
            Tab3Screen()
        }
    }
}

