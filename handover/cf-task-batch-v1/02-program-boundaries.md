# Program Boundaries

## cap-api 程序

物理落点：
- 继续使用现有 `srv/` 目录与 `srv` Maven module。
- 不新增 `cap-api/` 目录，也不重命名现有 `srv/`。

职责：
- 提供 OData/Fiori API。
- 维护 `BatchImportConfig`、`BatchImportStructure`、`BatchImportField`。
- 创建 `BatchImportFile`。
- 维护 `ProcessorArtifact` 的上传内容、启停状态与启用版本。
- 创建 `BatchImportExecution`。
- 根据 `ProcessKey` 和 `ProcessorArtifact` 提交 task。
- 查询文件状态、消息、原始数据、Spring Batch 日志。

现有类留在 `cap-api`：
- `DataImportServiceHandler`
- `BatchImportConfigHandler`
- `BatchImportTemplateService`

现有类改造后留在 `cap-api`：
- `BatchImportJobTriggerService`
  - 改名为 `BatchImportTaskTriggerService`
  - 不再依赖 `JobLauncher`
  - 改为创建 execution 并启动 task

新增类放在 `cap-api`：
- `DefaultBatchImportTaskTriggerService`
- `TaskLaunchService`
- `CloudFoundryTaskLaunchService`
- `LocalProcessTaskLaunchService`
- `BatchImportExecutionService`
- `ProcessorArtifactService`
- `ProcessorArtifactContentService`
- `BatchImportFileQueryService`
- `TaskLaunchRequestFactory`
- `TaskLaunchRequest`
- `TaskLaunchResult`

## batch-task 程序

物理落点：
- 新增独立目录，例如 `batch-task/`。
- 作为根 `pom.xml` 的新增 Maven module。
- 作为 Cloud Foundry 的新增 deployable module 纳入统一部署描述。

职责：
- 作为 `cf run-task` 的宿主程序。
- 接收 `fileUUID` 和 `executionUUID`。
- 执行完整 Spring Batch job。
- 读取 execution 绑定的 `ProcessorArtifact`。
- 装载 processor JAR 并执行外置 `BatchImportProcessor`。
- 回写业务执行状态和 Spring Batch 执行状态。

现有类迁到 `batch-task`：
- `BatchImportJobConfiguration`
- `GetBatchImportConfigTasklet`
- `DynamicHierarchyItemReader`
- `ProcessKeyDelegatingItemWriter`
- `ProcessKeyDelegatingStepState`
- `BatchImportJobExecutionListener`

新增类放在 `batch-task`：
- `TaskApplication`
- `BatchImportTaskMain`
- `SingleFileBatchLaunchService`
- `TaskExecutionLifecycleService`
- `ProcessorArtifactResolver`
- `ProcessorArtifactContentLoader`
- `ProcessorClassLoaderFactory`
- `DynamicProcessorExecutor`

## processor-sdk / shared-core 模块

物理落点：
- 新增独立目录，例如 `processor-sdk/`。
- 作为根 `pom.xml` 的新增 Maven module。
- 这是库模块，不作为 Cloud Foundry deployable module。

职责：
- 承载 `batch-task` 与 `processor-plugin` 共用的稳定 SPI、payload、result、dynamic type、状态枚举、持久化接口。

放在 `shared-core`：
- `BatchImportProcessor`
- `ProcessorRuntime`
- `BatchImportProcessContext`
- `BatchImportProcessPayload`
- `BatchImportProcessResult`
- `BatchImportProcessMessage`
- `BatchImportPersistenceService`
- `BatchImportFileStatusService`
- `Dynamic*` 类型
- `ExecutionStatus` / `TaskState`

## processor-plugin 工件

职责：
- 由业务团队独立仓库或独立模块开发。
- 只依赖 `processor-sdk`，不依赖 `batch-task` 源码。
- 打包为 JAR 并作为 `ProcessorArtifact` 版本上传。
- 独立发布，不要求 `batch-task` 重新下线部署。
- 它是上传工件，不作为 Cloud Foundry deployable module。

## 边界规则
- 现有 `srv/`、`db/` 目录名不改。
- 文档中的 `cap-api` 仅表示职责边界，不表示新目录名。
- 凡是需要发布到 Cloud Foundry 的新增程序，都必须作为本仓库新增 deployable module 统一纳管。
- `cap-api` 不执行 Spring Batch job。
- `cap-api` 不持有具体 processor bean。
- `batch-task` 不内置具体 processor 源码或固定 processor bean。
- `batch-task` 不暴露前端 API。
- `batch-task` 不反向调用 `cap-api`。
- `processor-plugin` 不直接依赖 `batch-task` 内部实现。
- 各侧通过共享数据库和稳定 SDK 协作。
