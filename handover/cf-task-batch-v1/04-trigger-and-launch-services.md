# Trigger And Launch Services

## 角色区别

`BatchImportTaskTriggerService`
- 业务编排服务。
- 放在逻辑上的 `cap-api`，物理上仍位于现有 `srv` 模块。
- 负责根据 `fileUUID` 发起一次导入执行。
- 负责查文件、查配置、查 `ProcessKey`、查 `ProcessorArtifact`、创建 `BatchImportExecution`、冻结 artifact 版本、更新业务状态。

`TaskLaunchService`
- 平台启动适配器。
- 放在逻辑上的 `cap-api`，物理上仍位于现有 `srv` 模块。
- 负责把一个已经编排好的任务真正拉起来。
- 不感知 `BatchImportFile`、`BatchImportConfig` 等业务实体。

## 调用关系
- `BatchImportTaskTriggerService -> TaskLaunchService`
- 这是单向调用。
- `TaskLaunchService` 不反向依赖业务服务。

## 建议接口

`BatchImportTaskTriggerService`

```java
public interface BatchImportTaskTriggerService {
    BatchImportExecutionRef trigger(String fileUUID);
    BatchImportExecutionRef retry(String executionUUID);
}
```

`BatchImportExecutionRef`

```java
public record BatchImportExecutionRef(
        String executionUUID,
        String fileUUID,
        String processKey,
        String taskState) {
}
```

`TaskLaunchService`

```java
public interface TaskLaunchService {
    TaskLaunchResult launch(TaskLaunchRequest request);
}
```

`TaskLaunchRequest`

```java
public record TaskLaunchRequest(
        String executionUUID,
        String fileUUID,
        String processKey,
        String processorArtifactId,
        String processorVersion,
        String artifactChecksum,
        String taskHostApp,
        String command,
        String launcherType,
        Integer memoryMb,
        Integer diskMb,
        Map<String, String> environment,
        Duration timeout) {
}
```

`TaskLaunchResult`

```java
public record TaskLaunchResult(
        boolean accepted,
        String launcherType,
        String platformTaskId,
        String platformTaskName,
        Instant acceptedAt,
        String rawState,
        String failureReason) {
}
```

## 具体用途

`BatchImportTaskTriggerService`
- 输入业务主键。
- 负责业务幂等和可触发性校验。
- 负责 execution 记录创建与失败处理。
- 负责把本次执行选中的 `ProcessorArtifactID / Version / Checksum` 固化到 execution。
- 负责文件状态从待执行到失败的业务状态更新。

`TaskLaunchService`
- 输入平台启动参数。
- BTP 实现通过 Cloud Foundry API / Java client 提交 task。
- 本地实现通过子进程拉起同一个 task 入口。
- `taskHostApp` 与 `command` 来自通用 `batch-task` 运行配置，不从 `ProcessorArtifact` 读取。
- `batch-task` 作为独立 deployable module 发布到 Cloud Foundry。

## 调用链路
1. `DataImportServiceHandler` 在文件创建完成后调用 `trigger(fileUUID)`。
2. `BatchImportTaskTriggerService` 查询 `BatchImportFile` 与配置。
3. 解析 `ProcessKey`。
4. 查询启用中的 `ProcessorArtifact`。
5. 创建 `BatchImportExecution`，初始状态 `SUBMITTED`，并冻结 `ProcessorArtifactID / Version / Checksum`。
6. 将 `BatchImportFile` 写成 `Q`。
7. 组装 `TaskLaunchRequest`，其中 task 宿主来自通用 `batch-task` 配置。
8. 调用 `TaskLaunchService.launch(request)`。
9. 成功时回写 `TaskId`、`TaskName`。
10. 失败时将 execution 记为 `FAILED`，并将 file 记为 `E`。

## retry 规则
- `retry(executionUUID)` 不复用旧 execution。
- 旧 execution 只保留历史记录。
- 新重试重新生成一条新的 `BatchImportExecution`。
