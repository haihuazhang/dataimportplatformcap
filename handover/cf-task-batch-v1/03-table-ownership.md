# Table Ownership

## 总原则
- 使用单一共享数据库。
- 现有 `db/` 目录名保持不变。
- 物理建模统一放在同一个 `db` 模块中。
- 不拆成 CAP 程序一套表、task 程序一套表。
- 表归属按“谁主写”定义，不按“谁建表”定义。

## CAP 控制面主拥有的表

继续保留：
- `zzdt.BatchImportConfig`
- `zzdt.BatchImportStructure`
- `zzdt.BatchImportField`
- `zzdt.BatchImportFile`

新增：
- `zzdt.ProcessorArtifact`
- `zzdt.BatchImportExecution`

主写职责：
- `BatchImportConfig / Structure / Field`
  - 只由 CAP 程序维护。
  - task 程序只读。
- `BatchImportFile`
  - CAP 程序创建上传文件记录和附件。
  - task 程序只更新执行状态字段。
- `ProcessorArtifact`
  - 由 CAP 程序维护 `ProcessKey -> processor artifact` 映射。
  - 由 CAP 程序维护 processor JAR 的 media 内容、启停状态与启用版本。
- `BatchImportExecution`
  - CAP 程序创建 `SUBMITTED` 记录。
  - 冻结本次执行绑定的 artifact 版本信息。
  - task 程序回写生命周期字段。

## task 执行面主写的表

继续复用：
- `zzdt.BatchImportData`
- `zzdt.BatchImportMessage`
- `zzdt.BatchImportFile`
- `zzdt.BatchImportExecution`

主写职责：
- `BatchImportData`
  - task 程序写原始根节点 JSON。
- `BatchImportMessage`
  - task 程序写处理消息。
- `BatchImportFile`
  - task 程序更新 `JobName`、`Status`、`StatusText`、`StatusCriticality`。
- `BatchImportExecution`
  - task 程序更新 `TaskState`、`FailureReason`、`JobInstanceId`、`StartedAt`、`FinishedAt`。

## Spring Batch 框架表

继续复用：
- `batch.job_instance`
- `batch.job_execution`
- `batch.job_execution_context`
- `batch.job_execution_params`
- `batch.step_execution`
- `batch.step_execution_context`

规则：
- 主写端只有 `batch-task`。
- `cap-api` 只读 projection 展示日志。
- `BatchImportFile.JobName` 继续保存 `JOB_INSTANCE_ID`。

## Quartz 表

v1 处理方式：
- `qrtz.*` 不进入新执行链路。
- 现有 Quartz 相关模型和代码视为冻结遗留。
- 如果未来恢复定时导入，需要单独再做调度设计。

## 新增表建议字段

`ProcessorArtifact`
- `ID`
- `ProcessKey`
- `Version`
- `Description`
- `ArtifactStorageType`
- `MediaFileName`
- `MediaMimeType`
- `MediaSize`
- `MediaContent`
- `MediaUrl`
- `ArtifactChecksum`
- `EntryPointClass`
- `SpiVersion`
- `Enabled`

说明：
- `ArtifactStorageType`
  - v1 实现值为 `DB_MEDIA`。
  - 预留 `S3` / `OBJECT_STORAGE` 作为后续兼容值。
- `MediaContent`
  - 在 `DB_MEDIA` 模式下直接存放 JAR 二进制内容。
- `MediaUrl`
  - v1 先落字段，不实现外部下载逻辑。
  - 后续可在不改 task 主接口的前提下兼容 `S3` / 对象存储。

`BatchImportExecution`
- `ID`
- `FileUUID`
- `ProcessKey`
- `ProcessorArtifactID`
- `ProcessorVersion`
- `ArtifactChecksum`
- `TaskHostApp`
- `TaskId`
- `TaskName`
- `TaskState`
- `FailureReason`
- `JobInstanceId`
- `StartedAt`
- `FinishedAt`

## CDS 字段草案

命名原则：
- 延续现有 `db/model-dt.cds` 的 PascalCase 字段风格。
- 继续使用 `cuid, managed`。
- media 字段沿用当前 `BatchImportConfig.Template` / `BatchImportFile.Attachment` 的 CAP 注解写法。

`ProcessorArtifact` 建议草案：

```cds
entity ProcessorArtifact : cuid, managed {
    ProcessKey          : String(100) @Common.Label:'{i18n>zzdt_ProcessKey}';
    Version             : String(40)  @Common.Label:'{i18n>zzdt_ProcessorVersion}';
    Description         : String(255) @Common.Label:'{i18n>zzdt_Description}';
    ArtifactStorageType : String(30)  @Common.Label:'{i18n>zzdt_ArtifactStorageType}' default 'DB_MEDIA';
    MediaMimeType       : String      @Core.IsMediaType;
    MediaFileName       : String(255) @Common.Label:'{i18n>zzdt_MediaFileName}';
    MediaContent        : LargeBinary @Core.MediaType: MediaMimeType @Core.ContentDisposition.Filename: MediaFileName;
    MediaSize           : Int64       @Common.Label:'{i18n>zzdt_MediaSize}';
    MediaUrl            : String(500) @Common.Label:'{i18n>zzdt_MediaUrl}';
    ArtifactChecksum    : String(128) @Common.Label:'{i18n>zzdt_ArtifactChecksum}';
    EntryPointClass     : String(255) @Common.Label:'{i18n>zzdt_EntryPointClass}';
    SpiVersion          : String(40)  @Common.Label:'{i18n>zzdt_SpiVersion}';
    Enabled             : Boolean     @Common.Label:'{i18n>zzdt_Enabled}' default true;
}
```

约束建议：
- `ProcessKey + Version` 应唯一。
- 同一 `ProcessKey` 同时只允许一个 `Enabled = true` 的启用版本。
- `ArtifactStorageType = 'DB_MEDIA'` 时：
  - `MediaContent`
  - `MediaMimeType`
  - `MediaFileName`
  - `ArtifactChecksum`
  必填。
- `MediaMimeType` v1 固定要求 `application/java-archive`。
- `MediaUrl` v1 非必填，只作后续兼容。

`BatchImportExecution` 建议草案：

```cds
entity BatchImportExecution : cuid, managed {
    FileUUID             : UUID         @Common.Label:'{i18n>zzdt_FileUUID}';
    ConfigUUID           : UUID         @Common.Label:'{i18n>zzdt_ConfigUUID}';
    ProcessKey           : String(100)  @Common.Label:'{i18n>zzdt_ProcessKey}';
    ProcessorArtifactID  : UUID         @Common.Label:'{i18n>zzdt_ProcessorArtifactID}';
    ProcessorVersion     : String(40)   @Common.Label:'{i18n>zzdt_ProcessorVersion}';
    ArtifactChecksum     : String(128)  @Common.Label:'{i18n>zzdt_ArtifactChecksum}';
    TaskHostApp          : String(100)  @Common.Label:'{i18n>zzdt_TaskHostApp}';
    TaskId               : String(100)  @Common.Label:'{i18n>zzdt_TaskId}';
    TaskName             : String(255)  @Common.Label:'{i18n>zzdt_TaskName}';
    TaskState            : String(30)   @Common.Label:'{i18n>zzdt_TaskState}';
    FailureReason        : LargeString  @Common.Label:'{i18n>zzdt_FailureReason}';
    JobInstanceId        : String(100)  @Common.Label:'{i18n>zzdt_JobInstanceId}';
    StartedAt            : Timestamp    @Common.Label:'{i18n>zzdt_StartedAt}';
    FinishedAt           : Timestamp    @Common.Label:'{i18n>zzdt_FinishedAt}';
    to_File              : Association to one BatchImportFile
                               on FileUUID = to_File.ID;
    to_Config            : Association to one BatchImportConfig
                               on ConfigUUID = to_Config.ID;
    to_ProcessorArtifact : Association to one ProcessorArtifact
                               on ProcessorArtifactID = to_ProcessorArtifact.ID;
}
```

约束建议：
- `FileUUID`、`ConfigUUID`、`ProcessKey` 在创建 execution 时即冻结。
- `ProcessorArtifactID`、`ProcessorVersion`、`ArtifactChecksum` 在提交 task 前冻结，后续不再修改。
- `TaskState` 建议枚举值至少覆盖：
  - `SUBMITTED`
  - `RUNNING`
  - `SUCCEEDED`
  - `FAILED`
  - `CANCELLED`
- `FailureReason` 只在失败态填写。

## Service Projection 草案

如果后续需要在 `srv/service-dt.cds` 暴露控制面维护与日志查询，建议增加：

```cds
service DataImportService {
    entity ProcessorArtifact    as projection on zzdt.ProcessorArtifact;
    entity BatchImportExecution as projection on zzdt.BatchImportExecution;
}
```

控制面建议：
- `ProcessorArtifact`
  - 允许维护启用版本、查看 media 元信息。
  - v1 是否直接在 FE 中上传 JAR，可单独决定。
- `BatchImportExecution`
  - 主要用于查询，不建议作为草稿业务对象开放编辑。
