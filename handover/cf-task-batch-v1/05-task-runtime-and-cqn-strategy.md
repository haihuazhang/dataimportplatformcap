# Task Runtime And CQN Strategy

## task 程序主入口

task 程序由以下入口组成：
- `TaskApplication`
- `BatchImportTaskMain`
- `SingleFileBatchLaunchService`
- `TaskExecutionLifecycleService`

启动参数：
- `fileUUID`
- `executionUUID`

## task 内部执行链
1. `BatchImportTaskMain` 读取参数。
2. `TaskExecutionLifecycleService` 将 execution 记为 `RUNNING`。
3. `SingleFileBatchLaunchService` 调用 Spring Batch job。
4. Spring Batch 依次执行：
   - `GetBatchImportConfigTasklet`
   - `DynamicHierarchyItemReader`
   - `ProcessKeyDelegatingItemWriter`
   - 外置 `BatchImportProcessor`
5. `ProcessKeyDelegatingStepState` 按 execution 绑定的 `ProcessorArtifact` 装载 processor JAR。
6. `DynamicProcessorExecutor` 校验 checksum、创建隔离 classloader、实例化 `EntryPointClass`。
7. `ProcessKeyDelegatingItemWriter` 调用已装载的 processor。
8. `BatchImportJobExecutionListener` 更新 `BatchImportFile` 状态。
9. `TaskExecutionLifecycleService` 更新 execution 最终状态。
10. 进程以 `0/非0` 退出。

## processor artifact 装载策略

`batch-task` 不再持有静态 `BatchImportProcessorRegistry`，而是在每次 task 执行时按 execution 绑定的 artifact 动态装载。

读取规则：
- `ArtifactStorageType = DB_MEDIA`
  - 从 `ProcessorArtifact.MediaContent` 读取 JAR 二进制。
- `ArtifactStorageType = S3 / OBJECT_STORAGE`
  - 通过 `MediaUrl` 预留后续兼容。
  - v1 先不实现实际下载逻辑。

执行规则：
- task 在启动后校验 `ArtifactChecksum`。
- 通过隔离 classloader 加载 processor JAR，避免与 `batch-task` 主程序依赖混淆。
- 通过 `EntryPointClass` 创建 `BatchImportProcessor` 实例。
- 一个 task 进程只服务一次 execution，不做运行中热替换。

## 为什么不在 writer 里启动 task
- writer 是 chunk 语义的一部分。
- 在 writer 里启动 task 会打散 Spring Batch 的事务、重试和状态模型。
- `cf run-task` 适合“一次文件一次任务”，不适合“一个 chunk 一个任务”。

## CQN 策略

运行时内部表：
- `BatchImportFile`
- `BatchImportData`
- `BatchImportMessage`
- `BatchImportExecution`
- `ProcessorArtifact`

默认使用：
- `PersistenceService`

业务 processor 写业务对象：
- 通过 `ProcessorRuntime` 获取受控运行时能力。
- 允许通过 `ProcessorRuntime` 获取 `PersistenceService`。
- 允许通过 `ProcessorRuntime` 按白名单获取 `ApplicationService` / `CqnService`。

原因：
- task 程序不需要前端协议也能运行 CAP Java。
- `PersistenceService` 适合纯后台内部表写入。
- `ApplicationService` 适合仍需保留业务 handler、projection、校验语义的业务写入。
- `processor-plugin` 不应依赖 `batch-task` 内部 Spring bean 注入。

## 对现有持久化类的影响
- `CdsBatchImportPersistenceService` 不再作为 task 程序内部表写入实现。
- task 程序默认新增 `PersistenceBatchImportPersistenceService`。
- 具体业务 processor 不再以 `@Component` 形式加入 `batch-task` Spring 容器。
- 现有 `@Qualifier(...CDS_NAME) CqnService` 直接字段注入不作为主方案保留。
- 具体业务 processor 改为通过 `ProcessorRuntime` 获取受控服务访问能力。

## 参考资料
- CAP Java services:
  - https://cap.cloud.sap/docs/java/services
- CAP Java CQN services:
  - https://cap.cloud.sap/docs/java/cqn-services/index
- CAP Java application services:
  - https://cap.cloud.sap/docs/java/cqn-services/application-services
- CAP Java Spring Boot integration:
  - https://cap.cloud.sap/docs/java/spring-boot-integration
- Cloud Foundry tasks:
  - https://docs.cloudfoundry.org/devguide/using-tasks.html
