22:05:33.019 28248-29229 okhttp.OkHttpClient              I  --> POST https://yayarun.cn/sys/api/user/login
22:05:33.019 28248-29229 okhttp.OkHttpClient              I  Content-Length: 191
22:05:33.019 28248-29229 okhttp.OkHttpClient              I  Content-Type: application/json
22:05:33.019 28248-29229 okhttp.OkHttpClient              I  {"data":[{"deviceId":"99acfb33712e0034","loginType":"PASSWORD","password":"6592e67b4b52a43826d5d889afa959f5","phoneNumber":"17512081100","sceneType":"LOGIN"}],"dtoName":"UserLoginRequestDto"}
22:05:33.019 28248-29229 okhttp.OkHttpClient              I  --> END POST (191-byte body)
22:05:33.145 28248-29229 okhttp.OkHttpClient              I  <-- 404 https://yayarun.cn/sys/api/user/login (125ms)
22:05:33.145 28248-29229 okhttp.OkHttpClient              I  Server: nginx
22:05:33.145 28248-29229 okhttp.OkHttpClient              I  Date: Wed, 28 Jan 2026 14:05:33 GMT
22:05:33.145 28248-29229 okhttp.OkHttpClient              I  Content-Type: application/json
22:05:33.145 28248-29229 okhttp.OkHttpClient              I  Transfer-Encoding: chunked
22:05:33.145 28248-29229 okhttp.OkHttpClient              I  Connection: keep-alive
22:05:33.145 28248-29229 okhttp.OkHttpClient              I  Vary: Accept-Encoding
22:05:33.145 28248-29229 okhttp.OkHttpClient              I  Vary: Origin
22:05:33.145 28248-29229 okhttp.OkHttpClient              I  Vary: Access-Control-Request-Method
22:05:33.145 28248-29229 okhttp.OkHttpClient              I  Vary: Access-Control-Request-Headers
22:05:33.145 28248-29229 okhttp.OkHttpClient              I  X-Content-Type-Options: nosniff
22:05:33.145 28248-29229 okhttp.OkHttpClient              I  X-XSS-Protection: 1; mode=block
22:05:33.145 28248-29229 okhttp.OkHttpClient              I  Cache-Control: no-cache, no-store, max-age=0, must-revalidate
22:05:33.145 28248-29229 okhttp.OkHttpClient              I  Pragma: no-cache
22:05:33.145 28248-29229 okhttp.OkHttpClient              I  Expires: 0
22:05:33.145 28248-29229 okhttp.OkHttpClient              I  X-Frame-Options: DENY
22:05:33.145 28248-29229 okhttp.OkHttpClient              I  Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
22:05:33.145 28248-29229 okhttp.OkHttpClient              I  X-Frame-Options: SAMEORIGIN
22:05:33.145 28248-29229 okhttp.OkHttpClient              I  X-Content-Type-Options: nosniff
22:05:33.145 28248-29229 okhttp.OkHttpClient              I  X-XSS-Protection: 1; mode=block
22:05:33.145 28248-29229 okhttp.OkHttpClient              I  Referrer-Policy: no-referrer-when-downgrade
22:05:33.145 28248-29229 okhttp.OkHttpClient              I  {"timestamp":"2026-01-28T14:05:33.590+00:00","status":404,"error":"Not Found","message":"","path":"/api/user/login"}
22:05:33.145 28248-29229 okhttp.OkHttpClient              I  <-- END HTTP (116-byte body)