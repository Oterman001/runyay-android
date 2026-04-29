
1 获取实时静息心率/最大摄氧量获取（佳明和高驰）

url：POST  /health/query
输入
JSON
{
"head": {
"appKey": "1jns01o9lksa12",
"sign": "42c6ffecfd21eae2960429fdc95d08b8da1aa81129c87f3eb020b867f02fd052",
"timestamp": "1764483770298",
"userId": "U17542070721940021",
"token": "eRVWidssAAQyKDgMWzr5Tw98C4ch9SXggkioRHN2R9GvY14LAyujQQrUu8N6yMCU2BW\/X+KpuWXAhLAbtD6P7ZIq3U9wDNbab4SwUZ+XODU="
},
"body": {
"GarminHealthQueryRequest": [
{
"userId": "U17542070721940021",
"platformCode": "GGB",
"startDate": "20260223",
"endDate": "20260223"
}
]
}
}

注意：
1.查询佳明的数据，开始和结束时间只能是同一天
2.支持平台：GCN（佳明中国）、GGB（佳明国际）、COROS（高驰）
3.当前只有佳明支持获取最大摄氧量，高驰的最大摄氧量为空
输出
JSON
{
"code": "0000",
"msg": "OK",
"data": {
"GarminHealthQueryResponse": [
{
"dailyData": [
{
"calendarDate": "2026-02-22",
"restingHeartRateInBeatsPerMinute": 45,
"vo2Max": null
},
{
"calendarDate": "2026-02-23",
"restingHeartRateInBeatsPerMinute": 47,
"vo2Max": 50.0
},
{
"calendarDate": "2026-02-24",
"restingHeartRateInBeatsPerMinute": null,
"vo2Max": null
}
]
}
]
}
}
