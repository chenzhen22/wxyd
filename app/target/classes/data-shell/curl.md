---
name: Curl HTTP 调试
category: 网络工具
order: 2
---

## 介绍

curl 是命令行 HTTP 客户端，发请求、看响应、测接口、传文件的瑞士军刀。调试 REST 接口、模拟回调、检查服务健康状态、下载资源全靠它，贴合网关与 API 服务的日常排查。

核心特点：
- **全协议支持**：HTTP/HTTPS/FTP，GET/POST/PUT/DELETE 全覆盖
- **可控性强**：自定义 Header、Cookie、超时、重定向行为
- **响应可观测**：-v 看全过程、-w 输出耗时状态码
- **脚本友好**：静默模式 + 退出码判断，适合健康检查

## 语法

### GET 请求

```bash
curl https://example.com/api/users
```
- 描述: 默认 GET，输出响应体内容

### POST JSON

```bash
curl -X POST -H "Content-Type: application/json" -d '{"name":"test"}' URL
```
- 描述: -X 指定方法，-H 加请求头，-d 带请求体

### 自定义 Header

```bash
curl -H "Authorization: Bearer token123" URL
```
- 描述: 多个 -H 可叠加，传 token / 自定义头

### 跟随重定向

```bash
curl -L http://example.com
```
- 描述: -L 跟随 3xx 重定向直到最终响应

### 静默与超时

```bash
curl -s --connect-timeout 3 --max-time 10 URL
```
- 描述: -s 静默不显示进度；连接超时 3 秒、总超时 10 秒

### 显示详细过程

```bash
curl -v URL
```
- 描述: -v 显示请求头/响应头/TLS 握手全过程，排障利器

### 输出耗时统计

```bash
curl -o /dev/null -s -w "%{http_code} %{time_total}s\n" URL
```
- 描述: -w 自定义输出：状态码、总耗时，性能摸底常用

## 示例

### 接口功能测试

- 描述: 完整模拟一次 API 调用

```bash
# POST JSON 并指定编码
curl -X POST "http://127.0.0.1:8090/api/login" \
  -H "Content-Type: application/json" \
  -d '{"body":{"username":"admin","password":"xxx"}}'

# PUT 更新资源
curl -X PUT -H "Content-Type: application/json" -d '{"status":1}' URL/api/user/1

# DELETE 删除
curl -X DELETE URL/api/robot/3
```

### 服务健康检查

- 描述: 脚本化探活与告警

```bash
# 只取状态码判断服务是否存活
code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 http://127.0.0.1:8090/api/actuator/health)
if [ "$code" != "200" ]; then echo "服务异常: $code"; fi

# 测接口响应耗时
curl -o /dev/null -s -w "DNS:%{time_namelookup} 连接:%{time_connect} 总耗时:%{time_total}\n" URL
```

### 文件上传下载

- 描述: -O 下载、-F 上传

```bash
# 下载文件（-O 用原文件名保存）
curl -O https://example.com/app.tar.gz

# 模拟表单上传文件
curl -F "file=@/opt/data/report.pdf" URL/api/upload
```