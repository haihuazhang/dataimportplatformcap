# Technical Plan

## 目标
- 让 `dataimportplatformcap` 支持多层次批导结构配置
- 支持动态字段配置
- 支持配置驱动模板生成
- 支持配置驱动多 sheet / 多层级解析
- 保持 CAP CDS + CAP Java + Spring Batch + Fiori Elements 主技术栈

## 目标架构

### 1. 配置层
- `BatchImportConfig` 保存业务对象和处理入口 `ProcessKey`
- `BatchImportStructure` 保存 sheet 级配置和层级关系
- `BatchImportField` 保存字段级配置、顺序、主键和父子映射信息

### 2. 运行层
- `BatchImportFile` 保存上传文件和作业状态
- `BatchImportData` 保存根节点原始 JSON
- `BatchImportMessage` 保存处理消息

### 3. 批处理层
- `GetBatchImportConfigTasklet`
  - 回读文件和动态配置
  - 做基础校验
- `DynamicHierarchyItemReader`
  - 把 Excel 解析成根节点列表
- `ProcessKeyDelegatingItemWriter`
  - 找到唯一 `BatchImportProcessor`
  - 保存原始数据
  - 执行业务导入
  - 保存消息与状态

### 4. 业务处理 SPI
- `BatchImportProcessor`
  - 用 `ProcessKey` 识别业务处理器
  - 输入为根节点 `DynamicNode`
  - 输出为 `BatchImportProcessResult`

## 关键设计决策

### 数据模型
- 不做运行时 CDS 类型生成
- 仅做运行时数据结构生成
- 这样可以保持 CAP 模型稳定，避免破坏 codegen 和 service contract

### Reader 粒度
- item 从“单 Excel 行”改成“单个根业务对象”
- 这样才能自然承载一主多子、多层子表

### ProcessKey
- `ProcessKey` 替代 `ImplementedByClass` 成为主分发键
- 注册表负责把 `ProcessKey` 解析到 Spring Bean
- 这样可以保留动态处理能力，同时降低反射和配置耦合

### 模板机制
- 模板不依赖静态文件
- 每次从结构/字段配置直接生成

## 兼容策略
- 旧配置只维护 `StructName` 时：
  - 动态构建一个 synthetic root structure
  - 从预定义 CDS type 自动初始化字段
  - 若没有 key 字段，默认把第一个字段提升为 key
- 旧 Writer 类配置保留但不再作为主运行链路

## 风险点
- Draft 场景下模板刷新时机需要持续验证
- 大文件下内存树组装可能需要后续分页或 staging 优化
- 业务处理器迁移到 `ProcessKey` 后，旧自定义 writer 需要逐步改造成 processor

## 验收标准
- 可以在 FE 页面维护结构和字段
- 配置保存后可下载动态模板
- 文件上传后能按动态结构解析
- 多层结构能正确组装父子关系
- 同一平台可通过 `ProcessKey` 处理不同批导对象
- 文件页能查看状态、原始数据、处理消息
