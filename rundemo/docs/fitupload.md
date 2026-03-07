

手动上传FIT文件接口

接口路径： POST /api/fit/upload
接口描述： 用户手动上传本地 FIT 文件到服务端，服务端解析并存储至 OSS，关联到指定的运动记录。接口会校验用户身份，防止水平越权。
⚠️ 注意：本接口为 Multipart 上传，与其他 JSON 接口请求格式不同。

request Part 内容（DtoClassName：FitFileUploadRequestDto）：

{
"head": {
"token": "用户Token（必填）"
},
"body": {
"FitFileUploadRequestDto": [{
"workoutId": "运动记录ID",
"platformCode": "HK",
"userId": "用户ID",
"deviceName": "Apple Watch Series 9",
"activityType": "or",
"activityStartTime": "20240101080000",
"activityName": "早晨跑步"
}]
}
}

参数：
workoutId
string
必须
运动记录ID，用于关联运动数据

platformCode
string
必须
平台代码：GCN-佳明中国，GGB-佳明国际，COROS-高驰，HK-HealthKit，MANUAL-手动导入

userId
string
是
用户ID，必须与 Token 中的用户ID一致（防越权）

deviceName
string
是
设备名称，如 "Apple Watch Series 9"

activityType
string
否
活动类型：ir-室内跑，or-户外跑，cr-越野跑

activityStartTime
string
否
活动开始时间，格式 yyyyMMddHHmmss

activityName
string
否
活动名称


响应参数
Data DtoClassName：FitFileUploadResponseDto

{
"code": "0000",
"msg": "OK",
"data": {
"FitFileUploadResponseDto": [{
"workoutId": "abc123",
"ossUrl": "https://oss.example.com/fit/abc123.fit",
"status": "S"
}]
}
}

workoutId
string
运动记录ID

ossUrl
string
上传后的FIT文件OSS存储地址

status
string
上传状态：S-成功
