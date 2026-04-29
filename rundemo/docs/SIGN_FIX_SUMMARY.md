# 签名算法修复总结

## 修复日期
2026-01-29

## 问题描述

Android登录请求一直失败，经过对比iOS成功日志和Android失败日志，发现签名生成算法不一致。

### 根本原因

Android使用了简单的SHA-256哈希，而iOS使用的是HMAC-SHA256（带密钥的消息认证码）。

## 修复内容

### 1. 添加SecretConstants对象

实现了与iOS完全一致的密钥解密算法：

```kotlin
object SecretConstants {
    private val KEY_ARR = byteArrayOf(
        128.toByte(), 123.toByte(), 129.toByte(), 107.toByte(),
        56.toByte(), 40.toByte(), 56.toByte(), 47.toByte(),
        130.toByte(), 56.toByte(), 66.toByte(), 47.toByte(),
        128.toByte(), 51.toByte(), 43.toByte(), 39.toByte()
    )
    
    private const val SALUT = "com.yaya.run.ios"
    private const val SALT: Byte = 37
    
    val mySpecialID: String by lazy { reveal() }
    
    private fun reveal(): String {
        // XOR + Salt 解密算法
        // (byte - salt) XOR keyByte → originalByte
    }
}
```

**解密算法：**
- 输入：混淆字节数组 (KEY_ARR)、密钥字符串 (SALUT)、盐值 (SALT)
- 步骤：
  1. 对每个混淆字节减去盐值：`saltRemovedByte = byte - salt`
  2. 使用密钥字节进行XOR操作：`originalByte = saltRemovedByte XOR keyByte`
- 输出：解密后的密钥字符串 `891hjbjksahdugiq`（16字符）

### 2. 实现HMAC-SHA256签名函数

```kotlin
fun hmacSha256(secret: String, content: String): String {
    // 1. 先对content进行SHA-256哈希
    val hashedContent = content.sha256()
    
    // 2. 使用secret作为密钥，对hashedContent进行HMAC-SHA256
    val secretKeySpec = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256")
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(secretKeySpec)
    val hmacBytes = mac.doFinal(hashedContent.toByteArray(Charsets.UTF_8))
    
    // 3. 转换为十六进制字符串
    return hmacBytes.joinToString("") { "%02x".format(it) }
}
```

**对应iOS代码：** `SignatureUtil.sign(secret:content:)`

**算法流程：**
1. SHA-256(content) → hashedContent (64位hex字符串)
2. HMAC-SHA256(hashedContent, secret) → signature (32字节)
3. 转换为64位hex字符串

### 3. 更新generateSign方法

```kotlin
fun generateSign(
    params: Map<String, String> = emptyMap(),
    timestamp: String,
    appKey: String
): String {
    val content = "$appKey$timestamp"
    return hmacSha256(secret = SecretConstants.mySpecialID, content = content)
}
```

**对应iOS代码：** `NetworkUtils.generateSign(params:timestamp:appKey:)`

## 算法对比

### 修复前（错误）

```
Android: SHA-256(appKey + timestamp) → sign
```

### 修复后（正确）

```
iOS & Android: 
  content = appKey + timestamp
  hashedContent = SHA-256(content)
  sign = HMAC-SHA256(hashedContent, secret)
```

## 关键差异

| 方面 | 修复前 | 修复后 |
|------|--------|--------|
| 算法 | SHA-256 | HMAC-SHA256 |
| 密钥 | 无 | SecretConstants.mySpecialID |
| 步骤 | 1步哈希 | 2步：SHA-256 + HMAC |
| 与iOS | 不一致 | 完全一致 |

## 验证方法

### 1. 密钥解密验证

在代码中添加日志：
```kotlin
Log.d("SignDebug", "Secret: ${SecretConstants.mySpecialID}")
```

预期输出：`891hjbjksahdugiq`

### 2. 签名对比验证

使用相同的测试数据：
- appKey: `1jns01o9lksa12`
- timestamp: `1761717897204`

**iOS日志中的签名：**
```
207351227b9076223a66d0f94b9bdbcfdd7f7087071bc090c322ece1acd42672
```

**Android应该生成相同的签名**

### 3. API请求验证

- 请求URL：`https://yayarun.cn/sys/api/user/login/passwordLogin`
- 请求体应包含正确的签名
- 预期响应：HTTP 200（而不是404）

## 技术要点

### HMAC vs SHA256

- **SHA256**：单向哈希函数，任何人用相同输入得到相同输出
- **HMAC-SHA256**：基于哈希的消息认证码，需要密钥才能生成/验证

### 混淆算法作用

iOS使用XOR + Salt混淆密钥字节，可以：
1. 防止静态分析工具从二进制文件提取密钥
2. 增加逆向工程难度
3. 保护敏感的API签名密钥

## 修改文件

- `rundemo/src/main/java/com/oterman/rundemo/util/SecurityUtils.kt`
  - 新增：`SecretConstants` 对象
  - 新增：`hmacSha256()` 函数
  - 修改：`generateSign()` 方法

## 构建状态

✅ 编译成功 - `./gradlew clean :rundemo:assembleDebug`

## 下一步测试

1. 运行应用
2. 查看日志确认密钥解密正确
3. 尝试登录
4. 对比网络请求日志中的签名
5. 验证登录是否成功

## 参考代码

### iOS
- `zrun/ZhiRun1/network/NetworkUtils.swift` - generateSign方法
- `zrun/ZhiRun1/utils/SignatureUtil.swift` - HMAC-SHA256实现
- `zrun/ZhiRun1/network/NetworkUtils.swift` - SecretConstants密钥解密

### Android
- `rundemo/src/main/java/com/oterman/rundemo/util/SecurityUtils.kt` - 完整实现

