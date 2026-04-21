# dataimportplatformcap cf run-task batch handover

本目录用于交接 `dataimportplatformcap` 的 `CAP API + cf run-task + Spring Batch` 改造设计。

目标摘要：
- 当前 `BatchImportProcessor` 与 CAP 常驻进程绑定。
- 改造目标是保留 `Spring Batch`，将 CAP 程序改成控制面，将批处理放入 `cf run-task` 启动的通用 task 程序中执行。
- 现有 `srv/`、`db/` 目录名保持不变，不做重命名迁移。
- 文档中的 `cap-api` 是逻辑职责名，物理上继续对应现有 `srv` 模块。
- `BatchImportProcessor` 实现类改为独立 `processor-plugin` JAR，不进入 `batch-task` 源码和发布包。
- v1 先将 processor JAR 作为 `ProcessorArtifact` 表中的 media 内容落库，并预留 `ArtifactStorageType` / `MediaUrl` 以兼容后续 `S3` / 对象存储。
- 后续新增凡是需要发布到 Cloud Foundry 的程序单元，都作为本仓库新增 deployable module 加入现有项目结构。
- 本地开发保留与 BTP 相同的任务边界，只替换任务启动方式。

阅读顺序：
1. `99-handover-summary.md`
2. `01-architecture-overview.md`
3. `02-program-boundaries.md`
4. `03-table-ownership.md`
5. `04-trigger-and-launch-services.md`
6. `05-task-runtime-and-cqn-strategy.md`
7. `06-implementation-worktree-plan.md`

历史参考文件：
- `cap-analysis.md`
- `technical-plan.md`
- `task.md`
- `AGENTS.md`

本目录中的文件保持原先设计拆分结构和文件数，不再二次加工重组。
