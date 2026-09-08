# Project-Scoped Read-Only IoT Agent

一个经过脱敏、面向技术面试的项目展示。它说明如何在微服务体系中使用 LLM Tool Calling，将自然语言问题安全地转换为项目范围内的设备与事件查询。

> 本目录是架构与核心机制演示，不包含原公司完整源码、真实环境配置、客户数据或凭证，也不是可直接部署的生产服务。

## 解决的问题

用户可以询问：

- 当前设备总数、在线率和离线数
- 哪些设备当前离线
- 指定时间段内各类事件数量
- 最近事件明细及连续追问

系统只允许查询当前会话绑定项目中的数据。模型不能自行指定其他项目，也不能使用写工具、SQL、Shell 或通用 HTTP 绕过权限。

## 核心设计

```text
Web Chat (SSE)
  -> API Gateway / Authentication
  -> Agent Orchestrator
  -> Tool Allowlist + Project Scope Guard
  -> Read-only IoT API Client
  -> Result Allowlist / Sanitizer
  -> LLM answer
```

关键点：

1. 服务端可信上下文决定项目范围，不信任模型参数。
2. 数量查询和明细查询使用不同工具，降低错误率与响应体积。
3. 所有下游结果经过字段白名单和长度限制。
4. “没有数据”和“接口失败”使用不同结果，防止误报。
5. readiness 同时检查 Agent 配置、工具实现、提示词和下游服务。

## 目录

```text
backend/                 核心机制的脱敏 Java 示例
tests/                   项目隔离与结果脱敏测试示例
docs/architecture.md     架构、请求流和安全边界
docs/api-contract.md     脱敏接口契约
docs/troubleshooting.md  实际联调问题与排查方法
docs/interview-notes.md  面试讲述提纲
examples/                脱敏请求与响应样例
```

## 面试演示建议

先用两分钟讲清业务目标和安全边界，再展示 `ProjectScopeGuard`、`ResultSanitizer` 和测试，最后讲一次服务发现/旧实例导致 SQL异常的排障过程。完整讲稿见 [docs/interview-notes.md](docs/interview-notes.md)。

## 技术栈

Java 17、Spring Boot、Spring Cloud、Nacos、OpenFeign、MyBatis-Plus、SSE、LLM Tool Calling、JUnit 5。

