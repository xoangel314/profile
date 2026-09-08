# Architecture

## 请求流程

1. 前端通过 SSE提交问题，并携带登录身份、租户和当前页面上下文。
2. Agent Orchestrator 从数据库读取 Agent 配置和允许的工具集合。
3. 大模型选择只读工具；工具执行前由 `ProjectScopeGuard` 重新校验项目范围。
4. `ReadOnlyIotClient` 调用已有 IoT业务 API，而不是访问业务数据库。
5. `ResultSanitizer` 仅保留回答所需字段，并限制列表数量和长文本。
6. 模型根据结构化结果生成简洁中文回答；下游失败时返回稳定错误，不伪装成零数据。

## 工具划分

| 工具 | 用途 |
|---|---|
| `query_devices` | 分页查询设备 |
| `count_devices` | 查询总数、在线、离线及比例 |
| `query_offline_devices` | 返回离线设备摘要 |
| `query_device_catalog` | 将自然语言设备分类解析为真实 ID |
| `query_event_types` | 将事件名称解析为事件类型 ID |
| `query_monitoring_events` | 查询事件明细 |
| `aggregate_events` | 按事件类型聚合数量 |

## 安全边界

- 项目 ID取自已认证的会话/Agent配置，模型提供的值只能用于一致性检查。
- 只读 Agent只装配固定白名单工具，不能访问写工具。
- 查询范围最多 31天，明细最多 100条。
- 返回设备信息不包含密码、Token、连接字符串和原始文件 URL。
- 日志不记录 Authorization、Cookie、密码或完整请求体。
- 权限校验在工具执行层再次进行，不能只依赖 Prompt。

## Readiness

静态检查包括 Agent 是否绑定项目、只读类型和提示词，必需工具是否已注册。在线检查只调用无副作用的目录查询，验证 IoT服务发现和鉴权链路。
