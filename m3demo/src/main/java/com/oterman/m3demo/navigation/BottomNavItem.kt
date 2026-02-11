package com.oterman.m3demo.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 底部导航项密封类
 * 定义了三个导航目标
 */
sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Tab1 : BottomNavItem(
        route = "tab1",
        title = "首页",
        icon = Icons.Filled.Home
    )
    
    data object Tab2 : BottomNavItem(
        route = "tab2",
        title = "发现",
        icon = Icons.Filled.Search
    )
    
    data object Tab3 : BottomNavItem(
        route = "tab3",
        title = "我的",
        icon = Icons.Filled.Person
    )
    
    companion object {
        val items = listOf(Tab1, Tab2, Tab3)
    }
}

