# Sanitized API Contract

以下均为脱敏的接口形式，路径用于说明设计，不对应任何真实公司环境。

| 能力 | 方法与路径 | 主要参数 |
|---|---|---|
| 设备分页 | `GET /iot/device/page` | page、size、projectId、parkId、productId |
| 设备统计 | `GET /iot/device/statistics` | projectId及可选过滤条件 |
| 事件明细 | `GET /iot/event/page` | page、size、projectId、类型、时间范围 |
| 事件聚合 | `GET /iot/event/count` | projectId、事件类型、开始/结束日期 |
| 事件类型 | `GET /iot/event-types` | projectId、parkId、productId |
| 设备目录 | `GET /iot/device-catalog` | projectId和父级分类 |

## 约束

- `projectId` 最终由服务端可信上下文覆盖或校验。
- 统计接口必须同时提供开始和结束时间，范围不超过31天。
- 明细默认20条，上限100条。
- HTTP成功但业务包装 `ok=false` 仍按失败处理。
- SSE 的 HTTP 200不代表业务成功，客户端必须解析事件内容。

## 示例

问题：`最近7天各类监测事件有多少？`

工具输入：

```json
{"startDate":"2026-01-01","endDate":"2026-01-07","eventTypeIds":["TYPE_A","TYPE_B"]}
```

工具输出：

```json
[
  {"name":"事件A","typeId":"TYPE_A","count":42},
  {"name":"事件B","typeId":"TYPE_B","count":3}
]
```
