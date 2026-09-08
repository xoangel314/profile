# PROJECT_CONTEXT — 智慧管理系统只读 Agent

> 最后核对日期：2026-09-08（Asia/Shanghai）  
> 仓库：`D:\dreamdeck\dd-framework\dd-ai`，分支：`ddai`  
> 用途：供换电脑后、未读过历史对话的 Codex 继续开发。本文描述的是工作区当前状态，不代表所有改动已提交或已部署。

## 1. 项目目标与业务背景

目标是在 DreamDeck AI 对话系统中，为“智慧管理系统/温榆河项目”提供一个**项目级、严格只读**的智能体。用户用自然语言询问设备状态、设备数量、离线设备、事件类型、事件统计和事件明细；Agent 通过现有 IoT 微服务 API 获取页面本来就能看到的数据，不直接查业务数据库，不控制设备，不创建任务，也不调用写接口。

当前业务范围已经确认：

- 项目 `itemId=39`（用户已再次人工确认）。
- 测试 Agent ID：`2095058607509864448`，名称“温榆河”。
- 租户：`Tenant-Id=1`。
- 当前只读工具应为：`query_devices`、`count_devices`、`query_offline_devices`、`query_event_types`、`query_device_catalog`、`query_monitoring_events`、`aggregate_events`。
- Agent 正确数据库状态应为：`item_id=39`、`global=0`、`func_type='ecology'`，并绑定提示词 `2099000000000000001`。
- 不允许借助通用 HTTP、SQL、Shell、页面控制、任务调度或旧写工具绕过只读边界。

历史代码里还存在新增设备、修改设备、配置算法及“二次确认/审计”框架。这是早期更大范围方案的遗留实现，**不是当前只读 Agent 的授权范围**。除非用户明确改变目标，不要把这些写工具绑定给“温榆河”Agent，也不要在公共环境试执行。

## 2. 仓库与代码结构

这是 Maven 多模块工程，版本由父工程管理，当前 flattened 版本为 `5.1.4`。根 `pom.xml` 当前只启用：

- `dd-ai-base-api`：实体、DTO、聊天表单和跨模块契约。
- `dd-ai-base-biz`：主业务服务，启动类 `cn.dreamdeck.ai.base.biz.DdAiBaseApplication`，本地端口 `8501`。

根 POM 中 `dd-agent-api`、`dd-agent-biz`、`dd-expert-biz` 当前被注释，不属于本轮必须启动的链路。不要误启 `DdAgentBizApplication`；公共 Nacos 曾缺少它的专属配置，导致 `knowledge.algorithmic-path` 解析失败。

只读 Agent 的核心代码位于：

- `dd-ai-base-biz/.../integration/iot/`：IoT Feign 窄接口和查询参数对象。
- `dd-ai-base-biz/.../support/function/ecology/`：生态查询工具、参数验证、项目范围约束、结果脱敏；也包含当前不应绑定的旧写工具。
- `dd-ai-base-biz/.../support/provider/model/DdDynamicToolProvider.java`：根据 Agent 配置动态装配工具。
- `dd-ai-base-biz/.../support/readiness/DdEcologyAgentReadinessService.java`：配置和 readiness 检查。
- `dd-ai-base-biz/.../controller/agent/DdAgentController.java`：`ecology-configure`、`ecology-readiness` 入口。
- `docs/agent/`：接口、提示词、验收、部署和交接文档。
- `docs/migrations/`：数据库脚本；20260902 两个脚本是当前只读方案。
- `scripts/`：环境预检、冒烟、自然语言验收、集成门禁和发布完整性检查。

前端在同一上级工作区、但不在本仓库目录内：

```text
D:\dreamdeck\dd-framework\dd-frontend\dd-ai-frontent
```

前端关键文件：

- `apps/dd-ai-fontend/env/.env.development`
- `apps/dd-ai-fontend/src/api/core/auth.ts`

## 3. 已完成的功能

代码层面已经实现：

- 7 个只读生态工具：设备列表、设备统计、离线设备、事件类型、设备目录、事件明细、事件聚合。
- 所有工具从当前消息/Agent 上下文取得并强制校验 `itemId`，拒绝模型跨项目覆盖。
- IoT 返回值白名单化和脱敏；不把设备连接配置、Token、原始文件 URL等敏感字段交给模型。
- 事件明细分页上限、统计时间范围和必要参数校验。
- `func_type='ecology'` 作为只读工具集合开关；不依赖公共库新增 `tool_names` 字段。
- 静态与在线 readiness：检查 Agent、提示词、必需工具、实现是否存在，以及 IoT 服务可达性。
- 配置接口会绑定只读提示词、写入 `func_type=ecology`、`item_id` 和 `global=0`。
- SSE 对话链路、Agent ID/项目上下文传递的相关修改。
- 自动化测试、环境预检、冒烟、集成验收和发布检查脚本。
- Maven 资源过滤已排除二进制字体，避免 `.ttc` 被当文本过滤而报 `MalformedInputException`。
- 已生成一份历史交接包：`ai-handoff-readonly-agent-20260902.zip`（它是快照，不应代替当前工作区）。

历史验证记录：曾用隔离本地仓库执行 `clean compile` 成功，并有一轮 18 个针对性测试通过。旧文档声称 89 个测试全部通过，但这是更早、更宽范围实现的记录；当前工作树之后仍有改动，不能把它当作最新验证结论，交接后应重新执行测试。

## 4. 重要技术决策及原因

### 4.1 复用 IoT API，不模拟网页、不直查业务库

页面已通过 `/device/statistics/info`、`/event/page`、`/event/count/event` 等接口取得真实数据。Agent 用 Feign 调用相同业务服务，可复用权限和业务语义，也避免页面结构变化与数据库耦合。

### 4.2 项目范围由服务端强制，不能信任模型参数

`itemId=39` 必须来自 Agent/会话绑定。模型即使传入其他 `itemId` 也要拒绝，否则会形成跨项目数据泄露。

### 4.3 当前用 `func_type=ecology` 绑定固定只读工具集

公共数据库不应为本地联调擅自升级，且此前运行旧 API JAR时出现过 `Unknown column 'tool_names'`。因此当前 readiness/provider 以已有 `func_type` 字段识别生态 Agent，并映射固定 `REQUIRED_TOOLS`。

### 4.4 只读提示词单独安装

提示词 ID `2099000000000000001`、code `ecology-monitoring-agent-v1`。它明确要求数据问题必须调用工具、无数据与接口失败分开、禁止写操作和绕过工具。安装脚本只更新提示词，不自动修改任意 Agent。

### 4.5 本地服务通过 Nacos metadata 灰度路由

前端请求仍经过公共 Gateway，Gateway 根据请求头 `version` 将 AI 请求转到本机注册实例。这样无需在本机启动整套微服务，但要求前端 version、后端 Nacos metadata version 和 Nacos 实例完全一致。

## 5. 修改过的重要文件/模块

当前工作树约有 130 条 porcelain 状态，包含已暂存、未暂存和未跟踪文件；**不要 reset、checkout 或覆盖用户改动**。先用 `git status --short` 和 `git diff` 重新确认。

重点文件：

- `dd-ai-base-api/src/main/java/.../entity/DdAgent.java`：Agent 字段/工具配置相关调整。
- `dd-ai-base-api/src/main/java/.../model/DdChatForm.java`：聊天上下文扩展。
- `dd-ai-base-biz/src/main/java/.../controller/agent/DdAgentController.java`：
  - `GET /agent/{id}/ecology-readiness?live=false|true`
  - `POST /agent/{id}/ecology-configure?itemId=39`
- `dd-ai-base-biz/src/main/java/.../integration/iot/DdIotAgentQueryClient.java`：IoT 只读 API 契约。
- `DdIotDeviceQuery.java`、`DdIotEventQuery.java`：真实接口参数映射。
- `support/function/ecology/*`：工具实现、验证和脱敏。
- `support/provider/model/DdDynamicToolProvider.java`：动态工具选择。
- `support/readiness/DdEcologyAgentReadinessService.java`：只读工具和提示词配置/自检。
- `dd-ai-base-biz/pom.xml`、根 `pom.xml`、`assembly.xml`：构建与打包修正。
- `docs/migrations/20260902_ecology_readonly_agent.sql`：安装只读提示词。
- `docs/migrations/20260902_ecology_readonly_verify.sql`：只读核验 Agent 状态。
- `docs/开发联调/生态Agent本地联调启动SOP.md`：当前最有用的本地启动手册。

工作树中还有完整的写操作框架（`support/action/`、`DdAgentAction*`、IoT/AI Box command clients 和三个写工具）以及 20260807 迁移。这些属于早期方案，保留但不要默认启用。`local-run/`、`local-run-safe/` 和交接目录也有重复快照，开发时以模块源码为准。

## 6. 运行环境、依赖与部署方式

### 后端

- Windows + PowerShell + IDEA。
- JDK：Eclipse Adoptium JDK 17（历史路径为 `C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot`）。
- Maven 多模块，项目版本 `5.1.4`。
- Spring Boot/Cloud、Nacos、OpenFeign、MyBatis-Plus、MySQL、Redis、RabbitMQ、MongoDB、MinIO、LangChain4j、SSE。
- 启动 `DdAiBaseApplication`，监听 `8501`。
- 公共环境主机曾为 `172.28.0.21`：Gateway `30092`、Nacos `30848`、MySQL `30006`、Redis `30379`、MongoDB `30117`、RabbitMQ `30672`。地址和端口是环境配置，换电脑后必须重新确认，不能盲信旧值。
- 后端 VM option 至少包含：

```text
-Dspring.cloud.nacos.discovery.metadata.version=ecology-jianglin
```

- 若 Nacos 自动注册到错误网卡，再加：

```text
-Dspring.cloud.nacos.discovery.ip=<当前电脑的真实内网 IPv4>
```

IP 会随电脑、DHCP、VPN变化。历史出现过 `.230`、`.245`、`.181`；这些都不是永久配置。每次用 `ipconfig`、路由/网卡信息和 Nacos 实例页核对，确保不是 VPN 地址。

### 前端

当前 `.env.development`（2026-09-08读取）为：

```properties
VITE_PORT=5777
VITE_BASE=/ai
VITE_PROXY_TARGET=http://172.28.0.21:30092
VITE_GLOB_API_URL=/api
VITE_TENANT_ID=1
VITE_MENU_PARENT_ID=130
VITE_VERSION=ecology-jianglin
VITE_ENCRYPT_ENABLE=false
```

启动：

```powershell
cd D:\dreamdeck\dd-framework\dd-frontend\dd-ai-frontent
pnpm dev:ai
```

访问地址必须以 Vite 控制台实际输出的 `Local:` 为准。虽然 env 写 `5777`，历史上 Vite 曾实际监听 `5173`；不要只测固定端口。

完整本地请求链路：

```text
本地前端 -> 公共 Gateway -> 按 version 灰度 -> 本地 dd-ai-base-biz:8501
             -> 公共 Auth/UPMS/IoT/数据库等服务
```

不要把 Nacos账号、数据库密码、登录密码、Bearer/access token 写入本文、代码、截图或提交。历史截图中曾泄露 token；换机后应重新登录获取新 token。

## 7. 已知问题与踩坑

### 7.1 Agent 仍可能未完成生态配置

用户最后提供的 Agent 数据是：

```json
{"id":"2095058607509864448","name":"温榆河","global":0,"itemId":39,"funcType":""}
```

即项目范围已正确，但 `funcType` 仍为空。日志也曾显示 `[agent-tools] ... baseTools=[]`。这会让模型只会泛泛回答或尝试“页面控制”，不会调用生态只读工具。此前未拿到一次明确成功的 `ecology-configure` 响应。

### 7.2 `dd_memory_message.device_id` 数据库错误

页面多次返回：

```text
Unknown column 'device_id' in 'field list'
SELECT ... device_id ... FROM dd_memory_message ...
```

但当前源码 `DdMemoryMessage`、编译产物和当时检查的本机 Maven `dd-ai-base-api-5.1.4.jar` 均不含 `deviceId`。本地后端日志有时也显示正确 SQL（不含 `device_id`）。因此高度怀疑请求被网关路由到旧/他人实例、进程未真正重启，或运行 classpath 仍混入旧包。不要直接给公共表加 `device_id` 来掩盖路由/版本问题。

诊断必须同时确认：浏览器 Request Headers 的 `version`、Nacos 实例 metadata/IP、IDEA 在点击发送瞬间是否出现 `[nio-8501-exec-*]` 日志。只看到 `[dd-cron-1]` 的定时 SQL不代表请求到了本机。

### 7.3 前端登录加密开关没有真正控制登录逻辑

2026-09-08再次检查 `apps/dd-ai-fontend/src/api/core/auth.ts`：`loginApi()` 仍然无条件调用 `encryption()`，并硬编码 AES key `dddddddddddddddd`。因此即使 `.env.development` 是 `VITE_ENCRYPT_ENABLE=false`，登录密码仍可能被加密，后端在“全部都没有加密”的环境下会返回“用户名或密码错误”。此前只给过修改建议，没有在该外部前端目录落实修改。

应将登录密码逻辑改成由 `VITE_ENCRYPT_ENABLE === 'true'` 决定，false 时发送原密码；修改后重启 Vite。修改前先确认团队期望，且不要提交个人环境值。

### 7.4 端口与服务发现

- `503 No instance available for dd-upms-biz` 是公共注册中心无 UPMS 可用实例，不是 Vite 静态页面故障。
- `connection timed out ... /172.27.0.51:8501` 或旧本机 IP，说明 Nacos 中仍有错误/旧实例或请求命中错误实例。
- `null: /<ip>:8501`、SSE 200 但页面提示发送失败，也可能是流式响应内部返回业务错误；必须查看 EventStream/Response 和后端同一时间日志。
- `401/424 token已过期` 时重新登录，不要重复使用旧 access token。
- `406 Not Acceptable` 可能是直接在 Console `fetch` readiness 时 `Accept` 不匹配；显式使用 `Accept: application/json`。
- 从 DevTools “编辑并重新发送”时，Raw text 曾导致 `Content-Type: text/plain;charset=UTF-8`，后端报不支持。应使用 `application/json`，或在 Console 用 `fetch` 明确设置 headers 并 `JSON.stringify`。

### 7.5 构建和运行时类问题

- 曾出现 ECJ 生成的损坏 class，启动时报 `Unresolved compilation problems`/Spring ASM 错误。执行标准 Maven `clean compile` 后消失。
- 本机 Maven 仓库可能有旧 `dd-ai-base-api:5.1.4`。必要时先单独 `clean install` API 模块并让 IDEA reload Maven，再启动 biz。
- `.ttc` 二进制字体不能参与 Maven文本资源过滤。
- 每十秒一次 `dd_dispatch_task_watch` SQL来自定时任务，是正常日志，不等于聊天请求。
- `RefreshTokenTask` 曾因认证中心连接拒绝报错；这是公共 Auth 地址/服务可用性问题，可能影响固定 token，但不要将其误判成 Agent 工具逻辑错误。

## 8. 已验证可行与不可行的方案

### 已验证/合理可行

- Vite 前端通过公共 Gateway + version 灰度访问本地 `8501`。
- 本地 `DdAiBaseApplication` 正确注册到 Nacos 后可正常登录和普通聊天。
- 通过 `Test-NetConnection`、Nacos实例页、浏览器 Network 和 IDEA 请求线程日志四方交叉检查路由。
- 使用 IoT 官方接口完成设备统计、事件统计和事件明细：
  - `GET /device/statistics/info`
  - `GET /event/count/event`
  - `GET /event/page`
- 用户提供的真实页面样例中，设备统计返回 `total=555,onLine=551,offLine=4`；事件统计在 2026-08-22 至 2026-08-28 返回骑行监测 6882 等。这些仅是当时样例，不是当前实时值。
- `itemId=39` 已由用户重新确认。
- 清理错误 Nacos 实例、用当日真实内网 IP注册后，连接超时问题曾恢复正常。

### 已证明不可行或不应使用

- 把历史 IP（`.230/.245/.181`）永久写死；换网络或开关 VPN 后会失效。
- 仅凭 `localhost` 页面能打开就认定后端链路正常。
- 只看 HTTP 200；SSE 会用 200承载内部错误文本。
- 通过公共 Gateway 联调却不核对 `version` 请求头和 Nacos metadata。
- 给公共数据库盲目加字段来修复疑似旧实例/classpath 造成的 SQL错误。
- 用 Raw text 发 JSON却不设置 `Content-Type: application/json`。
- 在 `VITE_ENCRYPT_ENABLE=false` 的情况下假设登录一定不加密；当前 `loginApi` 实现并非如此。
- 未经用户确认执行公共数据库迁移、修改公共 Nacos 或测试真实设备写操作。

## 9. 当前正在做的事情

在创建本文前，最新工作焦点已从后端 SQL问题切换到**换日/重启后的前端登录与端口检查**：

- 后端启动被用户描述为正常。
- 前端有时打不开；Vite 一次显示实际端口 `5173`，随后用户又测试固定 `5777` 得到 False。
- 页面后来能加载，但登录返回“用户名或密码错误”。代码核对显示登录函数无视 `VITE_ENCRYPT_ENABLE=false`，仍强制 AES 加密，这是当前最直接的可疑原因。
- 只读 Agent 本身尚未完成最终端到端验收；`funcType` 最后仍为空，`device_id` 错误的请求归属也未最终闭环。

## 10. 尚未完成的任务

按优先级排序：

1. 修复/确认前端登录请求是否应明文：让 `loginApi` 真正遵循 `VITE_ENCRYPT_ENABLE`，重启并验证登录。
2. 每日重新获取当前内网 IP，启动本地后端，以 `version=ecology-jianglin` 注册；在 Nacos 下线同 version 的旧实例。
3. 在获得用户对公共测试 Agent 数据修改的明确同意后，确保只读提示词已安装，再调用：

   ```http
   POST /api/agent/agent/2095058607509864448/ecology-configure?itemId=39
   ```

4. 调用静态 readiness（`live=false`），确认 `ready=true`、7 个工具齐全、提示词已绑定；再调用 `live=true` 检查 IoT依赖。
5. 在页面发送问题，同时确认请求确实落到本机 `8501`，彻底闭环 `dd_memory_message.device_id` 错误来源。
6. 重新执行当前工作树的 targeted tests/compile；不要沿用旧的“18/89 tests passed”作为现状。
7. 按顺序做只读验收：设备总数/在线率、离线设备、事件类型、指定日期事件统计、事件明细、连续追问、无数据、越权和接口失败。
8. 核对真实 IoT 字段和参数。尤其当前代码已使用 `/event/count/event`，而早期契约文档仍写 `/event/feignCountEvent`，以真实 IoT 源码和 Network 为准并更新文档。
9. 整理或拆分巨大未提交工作树，和用户确认哪些早期写操作代码保留、哪些应排除；未经确认不要删除。

## 11. 建议的下一步操作

### 第一步：恢复可重复的本地链路

```powershell
ipconfig | findstr /i "IPv4"
Test-NetConnection 172.28.0.21 -Port 30848
Test-NetConnection 172.28.0.21 -Port 30092
```

用真实内网 IP更新本次 IDEA VM option（仅当自动选择错误），启动 `DdAiBaseApplication`，确认：

```powershell
Test-NetConnection 127.0.0.1 -Port 8501
```

然后启动前端，并使用控制台实际 `Local` URL，不预设一定是 5777。

### 第二步：解决登录加密

先在 Network 检查 `/auth/oauth2/token` 的 Request Payload。再修改前端 `loginApi`，伪代码：

```ts
const password = import.meta.env.VITE_ENCRYPT_ENABLE === 'true'
  ? encryption({ data: { password: data.password }, ... }).password
  : data.password;
```

使用项目已有 env 读取风格和 AES 配置，不再新增第二套硬编码；重启 Vite验证。

### 第三步：配置并检查 Agent

仅在登录 token 有效且用户授权修改该测试 Agent 后执行 configure。用 JSON `Accept` 检查 readiness。成功标准：

- `itemId=39`
- `global=0`
- `funcType=ecology`
- `ecologyPromptBound=true`
- `missingRequiredTools=[]`
- `configuredButUnavailableTools=[]`
- `ready=true`

### 第四步：定位 SQL来源

若仍出现 `device_id`：记录精确时间，在浏览器复制完整 Request URL、request `version`、SSE response；同时截取 IDEA同秒的 `[nio-8501-exec-*]` 日志。若 IDEA没有请求，则问题在 Gateway/Nacos路由；若本机确实打印包含 `device_id` 的 SQL，再检查运行 PID classpath、实际加载的 `DdMemoryMessage.class` 来源和 Maven依赖树。

## 12. 用户工作方式、偏好与项目约束

- 用户偏好简洁、口语化、一步一步的操作说明；一次给一个可执行检查通常更有效。
- 用户经常直接贴 PowerShell输出、浏览器 Network截图和 IDEA日志，应基于具体证据判断，不让用户重复无关步骤。
- IP会变化且用户可能开 VPN；每次涉及 Nacos discovery IP，都要解释该 IP来自哪块网卡并要求现场确认，不能沿用历史值。
- 用户希望 Agent 与智慧管理页面数据一致，最关心真实查询是否可用，而不是只看单测或 Mock。
- 用户已明确当前目标是“只读 Agent”。写操作、任务调度、设备控制不是默认范围。
- 公共 Nacos、Gateway、数据库及其他公共微服务由团队共享：未经明确授权，不修改配置、不执行迁移、不下线他人实例、不写真实设备。
- 工作树包含用户已有改动。严禁 `git reset --hard`、`git checkout --` 或批量覆盖；修改前查看 diff，尽量做小范围补丁。
- 不在输出中复述 access token、密码或密钥。截图和交接材料必须脱敏。

## 13. 新 Codex 接手时的最小检查清单

1. 阅读本文，以及：
   - `docs/开发联调/生态Agent本地联调启动SOP.md`
   - `docs/agent/ecology-query-tools.md`
   - `docs/agent/iot-contract-matrix.md`
   - `docs/migrations/20260902_ecology_readonly_agent.sql`
2. 执行 `git status --short`、`git diff --stat`，保护现有改动。
3. 核对当前电脑 IP、公共 Nacos/Gateway 连通性和 IDEA VM options。
4. 核对前端实际监听端口、`VITE_VERSION` 与登录加密实现。
5. 不假设 Agent 已配置；读取实际 Agent 数据和 readiness 响应。
6. 不假设旧 SQL错误来自当前代码；用同一请求的浏览器、Nacos和本机日志建立证据链。
7. 任何公共数据或配置写入前先向用户说明目标、影响和回滚方式并取得明确同意。

