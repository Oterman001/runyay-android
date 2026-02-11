# Material3 三Tab折叠标题框架 - 实现总结

## 实现概述

已成功在 `m3demo` 模块中实现了一个基于 Material Design 3 的三 Tab 页面框架，包含以下特性：

- ✅ 底部 3 个 Tab 导航（首页、发现、我的）
- ✅ 每个 Tab 都有 Large → Small 折叠标题效果
- ✅ 标题文字在滚动时渐变透明和缩放
- ✅ 沉浸式状态栏（内容延伸到状态栏下方）
- ✅ 右上角固定操作按钮
- ✅ 每个页面填充了 40 条可滚动的假数据

## 文件结构

```
m3demo/src/main/java/com/oterman/m3demo/
├── MainActivity.kt (已修改)
├── navigation/
│   ├── BottomNavItem.kt (新建)
│   └── NavGraph.kt (新建)
├── components/
│   └── CollapsingTopBarScaffold.kt (新建)
├── screens/
│   ├── MainScreen.kt (新建)
│   ├── Tab1Screen.kt (新建)
│   ├── Tab2Screen.kt (新建)
│   └── Tab3Screen.kt (新建)
└── ui/theme/
    └── Theme.kt (已修改)
```

## 技术实现细节

### 1. 依赖配置

**gradle/libs.versions.toml:**
- 添加了 `navigationCompose = "2.8.5"` 版本
- 添加了 `androidx-navigation-compose` 库定义

**m3demo/build.gradle.kts:**
- 添加了 `implementation(libs.androidx.navigation.compose)` 依赖

### 2. 底部导航系统

**BottomNavItem.kt:**
- 使用密封类定义三个导航目标
- 每个导航项包含：route（路由）、title（标题）、icon（图标）
- 使用 Material Icons：Home、Search、Person

**NavGraph.kt:**
- 使用 `NavHost` 管理三个页面的路由
- 起始页面设置为 Tab1（首页）

### 3. 折叠标题组件

**CollapsingTopBarScaffold.kt:**
- 使用 `LargeTopAppBar` 实现大标题效果
- 集成 `TopAppBarDefaults.exitUntilCollapsedScrollBehavior()` 滚动行为
- 标题透明度计算：`alpha = 1f - (collapsedFraction * 0.3f)`
- 标题缩放计算：`scale = 1f - (collapsedFraction * 0.1f)`
- 右上角固定 `IconButton`，点击显示 Toast 提示
- 内容区域通过 `nestedScroll` 与滚动行为连接

### 4. 沉浸式状态栏

**Theme.kt:**
- 导入了 `WindowCompat` 和相关 API
- 使用 `SideEffect` 配置窗口系统栏：
  - 状态栏颜色：透明
  - 导航栏颜色：透明
  - 根据主题自动调整状态栏图标颜色（深色/浅色）

**MainActivity.kt:**
- 调用 `enableEdgeToEdge()` 启用边到边显示

### 5. Tab 页面

**Tab1Screen.kt / Tab2Screen.kt / Tab3Screen.kt:**
- 都使用 `CollapsingTopBarScaffold` 作为容器
- 使用 `LazyColumn` 渲染 40 条可滚动数据
- 每个页面使用不同的配色方案：
  - Tab1：`primaryContainer`（首页 - 蓝色系）
  - Tab2：`secondaryContainer`（发现 - 绿色系）
  - Tab3：`tertiaryContainer`（我的 - 紫色系）
- 每条数据使用 `Card` + `ListItem` 组合展示

### 6. 主屏幕

**MainScreen.kt:**
- 使用 `Scaffold` + `NavigationBar` 组合
- 集成 `NavController` 管理导航状态
- 底部导航栏自动高亮当前选中的 Tab
- 实现了导航优化：
  - `popUpTo` + `saveState` 避免重复堆栈
  - `launchSingleTop` 避免重复创建
  - `restoreState` 恢复之前的状态

## 功能特性

### 折叠标题效果
- 标题初始高度约 152dp（Large），折叠后变为 64dp（Small）
- 滚动过程中标题文字透明度从 100% 降低到 70%
- 滚动过程中标题文字缩放从 100% 缩小到 90%
- 效果流畅自然，符合 Material Design 规范

### 沉浸式体验
- 状态栏完全透明，内容延伸到屏幕顶部
- 导航栏也透明，与底部导航栏融为一体
- 状态栏图标颜色自动适配深色/浅色主题

### 交互体验
- 底部导航支持点击切换，有平滑过渡动画
- 每个 Tab 页面独立滚动，互不干扰
- 右上角按钮固定不动，始终可点击
- 点击按钮显示对应页面的 Toast 提示

## 测试建议

1. **滚动测试**：在每个 Tab 页面向上滚动，观察标题折叠效果
2. **导航测试**：点击底部 Tab 切换页面，验证状态保存和恢复
3. **按钮测试**：点击右上角操作按钮，确认 Toast 显示正确
4. **主题测试**：切换系统深色/浅色模式，验证状态栏图标适配
5. **边缘测试**：观察内容是否正确延伸到状态栏下方

## 使用方法

直接运行 `m3demo` 模块即可看到效果。应用启动后会显示首页，底部有三个 Tab 可以切换，每个页面都可以滚动查看折叠标题效果。

## 后续扩展建议

1. **添加下拉刷新**：在每个页面的 `LazyColumn` 中集成下拉刷新功能
2. **添加浮动按钮**：在某些页面添加 FAB（Floating Action Button）
3. **优化动画**：添加 Tab 切换时的过渡动画
4. **添加搜索功能**：在发现页面添加搜索框
5. **个性化设置**：在我的页面添加用户信息和设置选项

