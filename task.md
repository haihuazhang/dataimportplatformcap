# Task Plan

## Phase 1: 模型与服务
- 完成 CDS 模型扩展
- 暴露 `BatchImportStructure / BatchImportField / BatchImportData / BatchImportMessage`
- 补齐 UI annotation 和 value help
- 验收：服务 metadata 能看到新实体和导航

## Phase 2: 动态运行时
- 实现 `DynamicConfigurationBuilder`
- 实现 `DynamicHierarchyItemReader`
- 实现 `ProcessKeyDelegatingItemWriter`
- 实现 `BatchImportProcessorRegistry`
- 验收：batch job 可按动态配置解析并调用处理器

## Phase 3: 模板与状态
- 实现 `BatchImportTemplateService`
- 实现配置保存后模板重建
- 文件状态、原始数据、消息落库
- 验收：配置页模板可下载，文件页可追踪处理结果

## Phase 4: 前端
- 配置页支持结构和字段对象页导航
- 文件页展示状态、原始数据、消息
- 保持 Fiori Elements 主形态
- 验收：前端可完整维护并查看动态批导信息

## Phase 5: 业务处理迁移
- 按对象把旧 `ItemWriter` 迁移成 `BatchImportProcessor`
- 建立 `ProcessKey` 与处理器绑定
- 验收：至少一个真实业务对象和一个示例对象能跑通

## Phase 6: 验证
- 编译
- 关键链路联调
- 回归旧单结构配置兼容场景
- 验收：编译通过，核心用例可执行
