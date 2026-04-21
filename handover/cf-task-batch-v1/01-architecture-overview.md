# Architecture Overview

## 当前现状
- `BatchImportProcessor` 在启动期由 `BatchImportProcessorRegistry` 一次性收集。
- 上传文件后由 CAP 常驻进程内直接触发 Spring Batch。
- `Spring Batch`、`BatchImportProcessor`、CAP OData/UI 运行在同一个 JVM 中。
- 具体 processor 的发布与批处理运行程序绑在同一次部署中。
- 当前返回结构并不是 HTTP 同步返回，而是通过 `BatchImportFile`、`BatchImportMessage`、`BatchImportData` 落库后由 UI 查询。

## 改造目标
- 保留 `Spring Batch` 主执行链路。
- CAP 常驻程序只保留控制面能力。
- 批处理执行程序改为由 `cf run-task` 启动的一次性通用 task 程序。
- 保持现有 `srv/`、`db/` 目录名不变。
- 凡是需要部署到 Cloud Foundry 的新增程序，都以新增 module 的方式并入当前项目。
- `BatchImportProcessor` 开发人员不需要进入 `batch-task` 代码仓或发布流程。
- 新增或升级 processor 时，不要求 `batch-task` 重新下线部署。
- 本地开发与 BTP 保持相同的程序边界。
- 不在 `writer` 粒度启动 task。

## 推荐架构
- `cap-api`
  - 逻辑职责名
  - 物理目录继续使用现有 `srv/`
  - OData/Fiori API
  - 配置维护
  - 文件上传
  - `ProcessorArtifact` 上传、启停、版本切换
  - 创建执行记录
  - 提交 task
  - 查询状态与日志
- `batch-task`
  - 新增 deployable module
  - 接收 `fileUUID` / `executionUUID`
  - 执行完整 Spring Batch job
  - 从 `ProcessorArtifact` 读取 processor JAR
  - 以隔离 classloader 加载外置 `BatchImportProcessor`
  - 回写执行状态、消息、原始数据
- `processor-sdk`
  - `BatchImportProcessor`
  - `ProcessorRuntime`
  - `BatchImportProcessContext`
  - `BatchImportProcessPayload`
  - `BatchImportProcessResult`
  - `Dynamic*` 类型
- `processor-plugin`
  - 由业务团队独立仓库或独立模块开发
  - 打包为 JAR
  - 作为 `ProcessorArtifact` 版本上传
- `db`
  - 物理目录继续使用现有 `db/`
  - 统一维护数据库模型
  - 单库共享

## 非目标
- 不做 CAP 常驻 JVM 内热替换 processor bean。
- processor 只在 task 启动后按 execution 绑定的 artifact 加载。
- 不做 `writer` 中调用 `cf run-task`。
- v1 不实现 `S3` / 对象存储下载，只预留 `ArtifactStorageType` / `MediaUrl`。
- v1 不将 Quartz 纳入新执行链路。
- v1 不扩展多平台部署设计，主线只覆盖 BTP CF 与本地。
