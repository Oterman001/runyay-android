# FIT文件解析调试指南

## 已修复的问题

### 1. ViewModel 创建方式错误 ✅
**问题**: 在 MainActivity 中直接使用 `FitViewModel(applicationContext)` 会导致每次重组时创建新实例，状态丢失。

**修复**:
- 将 `FitViewModel` 改为继承 `AndroidViewModel`
- 在 MainActivity 中使用 `viewModel()` 函数正确创建实例
- 这样可以确保 ViewModel 在配置更改时保持状态

### 2. 添加完整的日志系统 ✅
在以下位置添加了详细日志：
- **MainActivity**: 文件选择、URI获取
- **FitViewModel**: 状态变化、解析流程
- **FitFileRepository**: 文件打开、解析过程、消息监听

## 查看日志的方法

### 方法1: 使用Android Studio Logcat
1. 打开 Android Studio
2. 点击底部的 "Logcat" 标签
3. 在过滤器中输入: `package:com.oterman.fitdemo`
4. 或者按标签过滤: `MainActivity` 或 `FitViewModel` 或 `FitFileRepository`

### 方法2: 使用 adb 命令行
```bash
# 查看所有应用日志
adb logcat | findstr /i "fitdemo"

# 或者按标签过滤
adb logcat | findstr /i "MainActivity FitViewModel FitFileRepository"

# 清除日志后重新开始
adb logcat -c
adb logcat -s MainActivity:D FitViewModel:D FitFileRepository:D
```

## 预期的日志流程

### 正常流程的日志顺序

1. **应用启动**
```
MainActivity: onCreate
MainActivity: 当前UI状态: Idle
```

2. **点击选择文件按钮**
```
MainActivity: 点击选择文件按钮
FitViewModel: 重置状态
```

3. **选择文件后**
```
MainActivity: 选择的文件URI: content://...
FitViewModel: 开始解析FIT文件: content://...
FitViewModel: 状态更新为: Loading
MainActivity: 当前UI状态: Loading
FitFileRepository: 开始解析FIT文件: content://...
FitFileRepository: 文件输入流打开成功，开始解析...
FitFileRepository: 创建Decode和MesgBroadcaster
FitFileRepository: 注册消息监听器
FitFileRepository: 开始读取FIT文件...
```

4. **解析过程中**
```
FitFileRepository: 收到FileIdMesg
FitFileRepository: 收到SessionMesg
FitFileRepository: 收到LapMesg (可能多个)
FitFileRepository: 收到RecordMesg (很多个，不会打印)
```

5. **解析成功**
```
FitFileRepository: FIT文件读取完成, 记录数: xxx, 区间数: xxx
FitFileRepository: 构建FitSummaryData对象
FitFileRepository: 文件解析成功
FitViewModel: 解析成功: FitSummaryData(...)
MainActivity: 当前UI状态: Success(...)
```

6. **解析失败**
```
FitFileRepository: 解析FIT文件失败
FitViewModel: 解析失败
MainActivity: 当前UI状态: Error(...)
```

## 常见问题排查

### 问题1: 选择文件后无任何反应
**检查**:
- 查看 Logcat 中是否有 "选择的文件URI"
- 如果没有，可能是文件选择器被取消

**日志**:
```
MainActivity: URI为空，用户取消选择
```

### 问题2: 显示 "无法打开文件"
**原因**: 
- 文件权限问题
- URI 无效
- 文件已被删除

**日志**:
```
FitFileRepository: 无法打开文件输入流
```

**解决**: 确保选择的是本地文件，不是云端文件

### 问题3: 解析失败
**可能原因**:
- 文件不是有效的 FIT 文件
- FIT 文件损坏
- FIT SDK 版本不兼容

**日志**:
```
FitFileRepository: Decode.read返回false
```

**解决**: 
- 使用 Garmin 设备导出的原始 FIT 文件
- 检查文件扩展名是否为 .fit

### 问题4: UI 不更新
**可能原因**:
- ViewModel 未正确创建
- StateFlow 未正确订阅

**检查**:
- 查看是否有 "当前UI状态" 的日志
- 确认状态是否从 Idle -> Loading -> Success/Error

## 测试建议

1. **准备测试文件**
   - 从 Garmin 设备导出真实的 FIT 文件
   - 文件应该在手机的本地存储中

2. **测试步骤**
   ```
   1. 打开 Logcat
   2. 清除所有日志 (可选)
   3. 打开 FIT 文件解析应用
   4. 点击"选择FIT文件"按钮
   5. 选择一个 .fit 文件
   6. 观察 UI 变化和日志输出
   ```

3. **验证点**
   - [ ] 点击按钮后文件选择器正常打开
   - [ ] 选择文件后显示 Loading 状态
   - [ ] Logcat 中有完整的解析流程日志
   - [ ] 最终显示 Success 或 Error 状态
   - [ ] Success 状态下能看到所有解析的数据

## 性能监控

在日志中可以观察：
- 记录点数量（应该是几千到几万）
- 区间数量（通常是跑步的圈数）
- 是否包含 GPS/心率/步频数据

## 下一步调试建议

如果问题仍然存在，请：
1. 复制完整的 Logcat 输出
2. 说明具体的现象（停在哪个状态）
3. 提供使用的 FIT 文件信息（来源、大小等）

