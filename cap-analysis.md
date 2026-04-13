# dataimportplatformcap 解析

## 改造前现状
- CAP 原始方案是“半动态”：
  - 配置表只维护 `StructName / SheetName / StartLine / StartColumn`
  - Reader 通过 `StructName` 动态选择一个预先生成好的 `cds.gen.<Type>`
  - Writer 通过 `ImplementedByClass` 动态加载 `ItemWriter`
- 这只能做到“从若干预置结构里选一个”，做不到“结构和字段本身运行时配置”。

## 改造后模型
- 数据模型已扩成：
  - `BatchImportConfig`
  - `BatchImportStructure`
  - `BatchImportField`
  - `BatchImportFile`
  - `BatchImportData`
  - `BatchImportMessage`
- 仍然保持 CAP CDS 语法，不引入运行时生成 CDS type。
- 保留旧字段：
  - `StructName`
  - `SheetName`
  - `StartLine`
  - `StartColumn`
  - `ImplementedByClass`
- 新主运行字段为 `ProcessKey`。

## 当前运行链路
- 批处理入口在 [BatchImportJobConfiguration.java](/Users/haihuazhang/code/dataimportplatform/dataimportplatformcap/srv/src/main/java/customer/batchimportcat/batch/configurations/BatchImportJobConfiguration.java:1)。
- 配置读取与校验在 [GetBatchImportConfigTasklet.java](/Users/haihuazhang/code/dataimportplatform/dataimportplatformcap/srv/src/main/java/customer/batchimportcat/batch/tasklets/GetBatchImportConfigTasklet.java:1)。
- 动态配置构建在 [DynamicConfigurationBuilder.java](/Users/haihuazhang/code/dataimportplatform/dataimportplatformcap/srv/src/main/java/customer/batchimportcat/batch/dynamic/DynamicConfigurationBuilder.java:1)。
- 动态解析在 [DynamicHierarchyItemReader.java](/Users/haihuazhang/code/dataimportplatform/dataimportplatformcap/srv/src/main/java/customer/batchimportcat/batch/dynamic/DynamicHierarchyItemReader.java:1)。
- `ProcessKey` 分发写入在 [ProcessKeyDelegatingItemWriter.java](/Users/haihuazhang/code/dataimportplatform/dataimportplatformcap/srv/src/main/java/customer/batchimportcat/batch/dynamic/ProcessKeyDelegatingItemWriter.java:1)。
- 导入对象处理接口在 [BatchImportProcessor.java](/Users/haihuazhang/code/dataimportplatform/dataimportplatformcap/srv/src/main/java/customer/batchimportcat/batch/dynamic/BatchImportProcessor.java:1)。

## Reader 改造说明
- 旧模式：`StreamingXlsxItemReader<cds.gen.*>`
- 新模式：`ItemStreamReader<DynamicNode>`
- 新 reader 的职责：
  - 读取整个 workbook
  - 按结构配置读取多个 sheet
  - 按字段配置动态做类型转换
  - 按外键配置组装父子树
  - 每次 `read()` 返回一个根节点对象

## Writer/Processor 改造说明
- 旧模式：`ClassifierCompositeItemWriter + ImplementedByClass`
- 新模式：`ProcessKeyDelegatingItemWriter + BatchImportProcessorRegistry + BatchImportProcessor`
- 设计理由：
  - 一个文件只对应一个业务处理器
  - 无需按每条数据重复 classifier
  - Spring Batch 的 chunk / restart / transaction 语义仍然保留

## 模板机制
- 模板生成服务在 [BatchImportTemplateService.java](/Users/haihuazhang/code/dataimportplatform/dataimportplatformcap/srv/src/main/java/customer/batchimportcat/batch/dynamic/BatchImportTemplateService.java:1)。
- 配置保存后由 [BatchImportConfigHandler.java](/Users/haihuazhang/code/dataimportplatform/dataimportplatformcap/srv/src/main/java/customer/batchimportcat/handlers/BatchImportConfigHandler.java:1) 回写模板二进制、文件名和 MIME type。
- 规则：
  - 每个结构一个 sheet
  - 头行起点由 `StartLine` / `StartColumn` 决定
  - 读取时会跳过头/描述行，保证模板与导入逻辑一致

## 前端现状
- 配置 UI 在 [app/zzdtimpconf/webapp/manifest.json](/Users/haihuazhang/code/dataimportplatform/dataimportplatformcap/app/zzdtimpconf/webapp/manifest.json:1)。
- 文件 UI 在 [app/zzdtimpfile/webapp/manifest.json](/Users/haihuazhang/code/dataimportplatform/dataimportplatformcap/app/zzdtimpfile/webapp/manifest.json:1)。
- 已补齐：
  - 配置页 `Structures` facet
  - 结构页 `Fields` facet
  - 配置 -> 结构 -> 字段 的 FE 路由
  - 文件页状态、原始数据、消息 facet

## 已知限制
- 旧 Writer/Reader 相关类仍在仓库中，当前未删除，主要用于兼容和对照。
- 模板重建当前挂在配置根保存后；如果后续需要对子实体非根式 API 操作做即时刷新，可以再补结构/字段级刷新 handler。
- 首版层级解析采用内存组装，适合常规批导规模；极大文件量场景后续可再评估 staging 表方案。
