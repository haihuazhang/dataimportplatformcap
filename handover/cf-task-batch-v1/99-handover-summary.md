# Handover Summary

## 背景
- 当前 `dataimportplatformcap` 的 `BatchImportProcessor` 与 CAP 常驻进程绑定。
- 文件上传后由 CAP 进程内直接触发 Spring Batch。
- 这导致 processor 发布、CAP API、批处理执行都绑在同一个程序中。

## 关键结论
- 保留 `Spring Batch`。
- 不在 `writer` 中运行 `cf run-task`。
- 整个 Spring Batch job 放到 `cf run-task` 启动的通用 task 程序中执行。
- `CAP API` 与 `batch-task` 拆成两个程序边界，但现有 `srv/` 目录名保持不变。
- 现有 `db/` 目录名保持不变。
- 凡是需要发布到 Cloud Foundry 的新增程序，都作为新增 deployable module 并入当前项目。
- `BatchImportProcessor` 实现类改为独立 `processor-plugin` JAR，不进入 `batch-task`。
- v1 先把 processor JAR 存在 `ProcessorArtifact` 表的 media 字段中。
- `ProcessorArtifact` 预留 `ArtifactStorageType` / `MediaUrl`，后续兼容 `S3` / 对象存储，但 v1 不实现。
- 使用单一共享数据库，不拆双库。
- `Quartz` 在 v1 中冻结，不进入新链路。

## 程序划分
- `cap-api`
  - 逻辑职责名
  - 物理上继续使用现有 `srv` module
  - 提供 OData/Fiori
  - 维护配置和上传文件
  - 维护 `ProcessorArtifact`
  - 创建 execution
  - 提交 task
  - 查询状态和日志
- `batch-task`
  - 新增独立 deployable module
  - 接收 `fileUUID`
  - 执行完整 Spring Batch
  - 动态装载外置 processor JAR
  - 调用 processor
  - 回写状态、消息、原始数据
- `processor-sdk`
  - 新增库 module
  - 复用 SPI、payload、result、dynamic type、持久化接口
- `processor-plugin`
  - 由业务团队独立开发和发布
  - 作为上传工件存在，不作为 CF deployable module

## 表归属
- CAP 主拥有：
  - `BatchImportConfig`
  - `BatchImportStructure`
  - `BatchImportField`
  - `BatchImportFile`
  - `ProcessorArtifact`
  - `BatchImportExecution`
- task 主写：
  - `BatchImportData`
  - `BatchImportMessage`
  - `BatchImportFile` 执行状态字段
  - `BatchImportExecution` 生命周期字段
- Spring Batch 框架表：
  - `batch.job_*`
  - `batch.step_*`
  - 仅 task 主写，CAP 只读展示

## 服务设计
- `BatchImportTaskTriggerService`
  - 业务编排服务
  - 位于 `cap-api`
  - 负责根据 `fileUUID` 创建 execution 并发起任务
- `TaskLaunchService`
  - 平台启动适配器
  - 位于 `cap-api`
  - 负责真正启动 CF task 或本地子进程
- 调用关系：
  - `BatchImportTaskTriggerService -> TaskLaunchService`

## 执行链路
1. 文件上传完成。
2. `DataImportServiceHandler` 触发 `BatchImportTaskTriggerService.trigger(fileUUID)`。
3. 创建 `BatchImportExecution`。
4. `BatchImportFile` 状态写成 `Q`。
5. 解析 `ProcessKey`，查询 `ProcessorArtifact`。
6. 将 `ProcessorArtifactID / Version / Checksum` 冻结到 execution。
7. `TaskLaunchService.launch(...)` 提交任务。
8. task 程序启动。
9. task 程序从 `ProcessorArtifact` 读取 JAR，校验 checksum，装载 processor。
10. task 程序执行完整 Spring Batch。
11. task 程序回写 `BatchImportFile`、`BatchImportMessage`、`BatchImportData`、`BatchImportExecution`。
12. UI 继续通过查表展示结果。

## CQN 策略
- task 程序不需要前端协议。
- 内部运行时表默认通过 `PersistenceService` 访问。
- 具体业务 processor 通过 `ProcessorRuntime` 获取受控的 `PersistenceService` / `ApplicationService` / `CqnService`。

## 本地与 BTP
- BTP:
  - `cap-api` 通过 Cloud Foundry API / Java client 提交 `run-task`
- 本地:
  - 使用相同的 task 主入口
  - 只把任务启动方式换成本地子进程

## worktree 默认值
- 基线分支：`feature/postgresql`
- 新分支：`feature/cf-task-batch-v1`
- worktree 路径：`/Users/haihuazhang/code/dataimportplatformcap-cf-task-batch-v1`

## 相关文档
- `README.md`
- `01-architecture-overview.md`
- `02-program-boundaries.md`
- `03-table-ownership.md`
- `04-trigger-and-launch-services.md`
- `05-task-runtime-and-cqn-strategy.md`
- `06-implementation-worktree-plan.md`

## 历史参考
- `cap-analysis.md`
- `technical-plan.md`
- `task.md`
- `AGENTS.md`
