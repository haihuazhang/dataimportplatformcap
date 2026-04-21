# Implementation Worktree Plan

## 默认 worktree 设置
- 基线分支：`feature/postgresql`
- 新分支：`feature/cf-task-batch-v1`
- worktree 路径：`/Users/haihuazhang/code/dataimportplatformcap-cf-task-batch-v1`

## 实施顺序
1. 从 `feature/postgresql` 建新 worktree 和新分支。
2. 保持现有物理目录不变：
   - `srv/` 名称保持不变，继续承载逻辑上的 `cap-api`
   - `db/` 名称保持不变
3. 在现有仓库中新增程序边界对应模块：
   - 新增 `batch-task` module
   - 新增 `processor-sdk` module
   - 凡是需要部署到 Cloud Foundry 的新增程序，都纳入 deployable module
4. 调整 `db` 模型：
   - 新增 `ProcessorArtifact`
   - 为 `ProcessorArtifact` 增加 media 字段、`ArtifactStorageType`、`MediaUrl`
   - 新增 `BatchImportExecution`
   - 为 `BatchImportExecution` 增加 artifact 版本冻结字段
5. 将现有 `srv` 中的 `BatchImportJobTriggerService` 改造成 `BatchImportTaskTriggerService`。
6. 在现有 `srv` 中新建 `TaskLaunchService` 及 CF/local 两个实现。
7. 新建 `batch-task` 程序入口：
   - `TaskApplication`
   - `BatchImportTaskMain`
8. 将 batch 运行链路迁移到 `batch-task` 程序，并移除静态 `BatchImportProcessorRegistry` 依赖。
9. 新建 processor JAR 动态装载能力：
   - `ProcessorArtifactResolver`
   - `ProcessorArtifactContentLoader`
   - `ProcessorClassLoaderFactory`
   - `DynamicProcessorExecutor`
10. 将内部表写入改为 `PersistenceService`，并引入 `ProcessorRuntime`。
11. 补齐项目模块编排：
   - 根 `pom.xml` 增加新 module
   - 后续若引入或维护 Cloud Foundry 部署描述，新增 deployable 程序同步加 module
12. 将 API 侧 `ProcessKey` value help 改为查启用中的 `ProcessorArtifact`。
13. 回归验证 UI、执行状态、消息、Spring Batch 日志与 processor 热替换发布流程。

## 建议目录结构草案

```text
dataimportplatformcap/
├─ app/
│  ├─ zzdtimpconf/
│  ├─ zzdtimpfile/
│  └─ zzdtimplog/
├─ db/
│  ├─ src/
│  └─ package.json
├─ handover/
├─ srv/
│  ├─ pom.xml
│  └─ src/
│     └─ main/java/customer/batchimportcat/
│        ├─ handlers/
│        ├─ service/
│        └─ tasklaunch/
├─ batch-task/
│  ├─ pom.xml
│  └─ src/
│     └─ main/java/customer/batchimportcat/batchtask/
│        ├─ config/
│        ├─ launcher/
│        ├─ runtime/
│        └─ artifact/
├─ processor-sdk/
│  ├─ pom.xml
│  └─ src/
│     └─ main/java/customer/batchimportcat/processorsdk/
│        ├─ spi/
│        ├─ runtime/
│        ├─ payload/
│        └─ dynamic/
├─ pom.xml
└─ package.json
```

约束：
- 不新增 `cap-api/` 目录。
- 不重命名现有 `srv/`、`db/`。
- `srv/` 继续作为 CAP OData/UI 控制面。
- `batch-task/` 是新增独立执行程序目录。
- `processor-sdk/` 是新增库目录，不直接部署到 Cloud Foundry。

## 根 `pom.xml` modules 草案

当前根 `pom.xml` 只有：

```xml
<modules>
    <module>srv</module>
</modules>
```

改造后建议变为：

```xml
<modules>
    <module>processor-sdk</module>
    <module>srv</module>
    <module>batch-task</module>
</modules>
```

说明：
- `processor-sdk`
  - 普通 jar module。
  - 放稳定 SPI、payload、runtime facade。
- `srv`
  - 继续是现有 CAP API Spring Boot module。
  - 不依赖 `batch-task`。
- `batch-task`
  - 新增 Spring Boot executable jar module。
  - 依赖 `processor-sdk`。
  - 自身不承载任何业务 processor 实现类。

依赖规则：
- `srv -> batch-task`
  - 禁止依赖。
- `batch-task -> srv`
  - 禁止依赖。
- `batch-task -> processor-sdk`
  - 允许。
- `processor-plugin -> processor-sdk`
  - 允许。
- `srv -> processor-sdk`
  - 仅当后续确认 CAP 控制面确实需要复用公共枚举或 DTO 时才允许；默认不依赖。

## `processor-sdk/pom.xml` 草案

定位：
- 普通 jar library。
- 不做 Spring Boot repackage。
- 不依赖 `srv`。

最小骨架建议：

```xml
<project>
    <parent>
        <groupId>customer</groupId>
        <artifactId>batchimportcat-parent</artifactId>
        <version>${revision}</version>
    </parent>

    <artifactId>batchimportcat-processor-sdk</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <dependency>
            <groupId>com.sap.cds</groupId>
            <artifactId>cds-services-api</artifactId>
        </dependency>
    </dependencies>
</project>
```

放置内容：
- `BatchImportProcessor`
- `ProcessorRuntime`
- `BatchImportProcessPayload`
- `BatchImportProcessResult`
- `BatchImportProcessMessage`
- `Dynamic*` 公共类型

不放内容：
- `@Configuration`
- Spring Batch job 配置
- CAP handler
- 任何具体 processor 实现

## `batch-task/pom.xml` 草案

定位：
- 独立 Spring Boot executable jar。
- 作为 Cloud Foundry deployable module。

最小骨架建议：

```xml
<project>
    <parent>
        <groupId>customer</groupId>
        <artifactId>batchimportcat-parent</artifactId>
        <version>${revision}</version>
    </parent>

    <artifactId>batchimportcat-batch-task</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <dependency>
            <groupId>customer</groupId>
            <artifactId>batchimportcat-processor-sdk</artifactId>
            <version>${revision}</version>
        </dependency>

        <dependency>
            <groupId>com.sap.cds</groupId>
            <artifactId>cds-starter-spring-boot</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.batch</groupId>
            <artifactId>spring-batch-core</artifactId>
        </dependency>

        <dependency>
            <groupId>com.sap.cds</groupId>
            <artifactId>cds-feature-postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
    </dependencies>
</project>
```

说明：
- `batch-task` 允许依赖 CAP Java runtime 和 Spring Batch。
- `batch-task` 不依赖 `cds-adapter-odata-v4`，除非后续出现明确需要。
- `batch-task` 不依赖 `srv` module。
- `batch-task` 不把业务 processor 作为编译期依赖。

## 根 `pom.xml` plugin 约束

根 `pom.xml` 继续维护统一版本：
- `cds-services-bom`
- `spring-boot-dependencies`
- `spring-batch-bom`

新增 module 后建议继续在父 POM 统一：
- `maven-compiler-plugin`
- `maven-surefire-plugin`
- `flatten-maven-plugin`

额外约束：
- 只有 deployable module 才开启 `spring-boot-maven-plugin` 的 `repackage`。
- `processor-sdk` 不应生成可执行 jar。
- `srv` 与 `batch-task` 的最终产物名称要能清晰区分，避免 CF 上传时混淆。

## 未来 `mta.yaml` modules 草案

当前仓库还没有根级 `mta.yaml`。如果后续补齐或恢复 Cloud Foundry MTA 部署描述，建议至少包含以下 deployable modules：

```yaml
modules:
  - name: batchimportcat-srv
    type: java
    path: srv
    parameters:
      memory: 1024M
    properties:
      SPRING_PROFILES_ACTIVE: cloud

  - name: batchimportcat-batch-task
    type: java
    path: batch-task
    parameters:
      memory: 1024M
      no-route: true
    properties:
      SPRING_PROFILES_ACTIVE: cloud
```

说明：
- `batchimportcat-srv`
  - 继续对外提供 OData/Fiori API。
- `batchimportcat-batch-task`
  - 作为 `cf run-task` 的宿主 app。
  - 不对外暴露业务路由。
  - `TaskLaunchService` 运行时应指向这个 app 名称。
- `processor-sdk`
  - 不进入 `mta.yaml` modules。
- `processor-plugin`
  - 不进入 `mta.yaml` modules。
  - 只作为上传到 `ProcessorArtifact` 的 JAR 工件。
- `db/`
  - 继续作为模型来源目录，不单独定义为 deployable module。
  - 除非后续明确引入专门的 db-deployer，这不属于 v1 范围。

## 未来 `mta.yaml` resources 草案

新增 `batch-task` 后，资源绑定原则应与 `srv` 分开描述，但尽量复用同一套后端资源：

```yaml
resources:
  - name: batchimportcat-postgres
    type: org.cloudfoundry.managed-service

  - name: batchimportcat-destination
    type: org.cloudfoundry.managed-service

  - name: batchimportcat-xsuaa
    type: org.cloudfoundry.managed-service
```

绑定原则：
- `batchimportcat-postgres`
  - `srv`、`batch-task` 都要绑定。
- `destination` / `xsuaa`
  - 只有在当前运行时已经依赖这些服务时才继续给 `batch-task` 绑定。
- `batch-task` 的绑定目标应以“能完成后台处理所需最小运行集”为准，不机械复制全部前台依赖。

## 验收清单
- `srv/`、`db/` 目录名保持不变。
- 逻辑上的 `cap-api` 仍由现有 `srv` 承载。
- 新增需要发布到 Cloud Foundry 的程序已作为 deployable module 纳入项目。
- 文件上传后 API 不再本地启动 Spring Batch。
- task 程序能够独立执行完整 batch。
- task 程序发布包中不包含具体业务 processor 实现类。
- 新增 processor JAR 上传并切换启用版本后，不要求 `batch-task` 重新部署。
- v1 能从 `ProcessorArtifact.MediaContent` 成功装载 processor JAR。
- `BatchImportFile.Status`、`BatchImportMessage`、`BatchImportData` 与现状保持一致。
- `BatchImportFile.JobName` 仍能关联 Spring Batch 日志页。
- 本地与 BTP 仅任务启动方式不同，业务执行链路一致。

## 风险与注意事项
- `ProcessorArtifact` 的内容、checksum 与启用版本必须一致。
- JAR 存表会带来数据库容量增长，需要设置版本保留策略。
- `processor-sdk` 的 SPI 兼容性必须受控。
- classloader 隔离和依赖冲突需要专项验证。
- `ArtifactStorageType` / `MediaUrl` 仅为后续兼容位，v1 不实现 `S3` / 对象存储下载。
- `BatchImportExecution` 状态更新必须幂等。
- `BatchImportFile` 和 `BatchImportExecution` 的双状态要保持一致。
- Quartz v1 不纳入此分支的主执行链改造。
