# dataimportplatformrap 解析

## 目标范围
- 参考仓库：`dataimportplatformrap`
- 目标主题：动态结构/字段配置、模板生成、后台作业导入、多层结构解析、前端呈现

## 核心结论
- RAP 版本已经实现了“配置头 -> 结构 -> 字段 -> 文件 -> 作业 -> 原始数据/消息”的完整闭环。
- 动态的关键不在静态 DDIC 结构，而在结构表、字段表和后台类 `zzcl_dtimp_process_generic` 按配置创建运行时表结构并解析 Excel。
- 模板生成与结构/字段维护强绑定，字段或结构保存时会重建模板。

## 配置模型
- `Configuration` 为配置头，存放对象编码、对象名称、模板等基础信息。
- `Structure` 为结构层，至少包含：
  - `RootNode`
  - `SheetName`
  - `SheetNameUp`
  - `StartLine`
  - `StartColumn`
  - `HasFieldnameLine`
  - `HasDescLine`
- `Field` 为字段层，至少包含：
  - `FieldName`
  - `FieldDescription`
  - `Sequence`
  - `IsKeyField`
  - `IsForeignField`
  - `ForeignField`
  - `FieldType1`
  - `FieldLength`
  - `FieldDecimal`

## 模板生成
- 主要实现在 [zbp_r_zt_dtimp_conf.clas.locals_imp.abap](/Users/haihuazhang/code/dataimportplatform/dataimportplatformrap/src/zbp_r_zt_dtimp_conf.clas.locals_imp.abap:1)。
- `determineTemplatebyField` 和 `determineTemplatebyStructure` 会在结构/字段保存时回读配置、结构、字段并重建 Excel 模板。
- 模板生成策略：
  - 每个 `Structure` 一个 sheet
  - 第一行写字段名
  - 第二行写字段描述
  - 模板内容直接回写到配置头二进制字段
- 使用 XCO XLSX API 生成 workbook，而不是静态模板文件。

## 后台作业触发
- 文件保存触发逻辑在 [zbp_r_zt_dtimp_files.clas.locals_imp.abap](/Users/haihuazhang/code/dataimportplatform/dataimportplatformrap/src/zbp_r_zt_dtimp_files.clas.locals_imp.abap:1)。
- `save_modified` 在文件新增后调用 `cl_apj_rt_api=>schedule_job`。
- 作业参数只传文件 UUID，后台类再回读文件、配置和结构字段信息。

## 动态解析与层级组装
- 核心实现在 [zzcl_dtimp_process_generic.clas.abap](/Users/haihuazhang/code/dataimportplatform/dataimportplatformrap/src/zzcl_dtimp_process_generic.clas.abap:1)。
- 关键方法：
  - `get_batch_import_configuration`
  - `get_sheets_from_xlsx`
  - `build_hierarchical_data_model`
  - `append_child_model_as_node`
  - `append_child_data_as_node`
  - `save_data`
  - `save_messages`
- 实现方式：
  - 先读取配置头、结构表、字段表
  - 按字段配置动态创建 ABAP table/structure handle
  - 每个 sheet 按 `StartLine` / `StartColumn` 读取为内表
  - 按 `SheetNameUp + IsForeignField + ForeignField` 递归挂接父子节点
  - 最终把整棵层级数据作为一条根业务数据处理

## 原始数据与消息
- RAP 版本不仅执行导入，还保留：
  - 原始导入数据
  - 处理消息
  - 作业与日志信息
- 这使得前端可追踪每次导入的结构化原始内容和处理结果。

## 作业状态与日志
- [zzcl_get_job_status.clas.abap](/Users/haihuazhang/code/dataimportplatform/dataimportplatformrap/src/zzcl_get_job_status.clas.abap:1) 提供虚拟字段计算。
- 它根据 `JobName / JobCount / LogHandle` 实时取 APJ 作业状态、文本、criticality，并拼装 Application Log 跳转 URL。
- 这部分是“文件页可观测性”的关键。

## 前端实现
- 配置端 UI 在 [app/zzdtimpconf/webapp/manifest.json](/Users/haihuazhang/code/dataimportplatform/dataimportplatformrap/app/zzdtimpconf/webapp/manifest.json:1)。
- 文件端 UI 在 [app/zzdtimpfile/webapp/manifest.json](/Users/haihuazhang/code/dataimportplatform/dataimportplatformrap/app/zzdtimpfile/webapp/manifest.json:1)。
- 结论：
  - 主体仍然是 Fiori Elements
  - 文件页额外扩展了 Application Log 片段和 controller extension
  - 结构/字段配置依赖对象页导航和 facet，不是整页自定义应用

## 对 CAP 的可迁移点
- 数据模型必须从“配置头”扩展为“配置头 + 结构 + 字段 + 文件 + 数据 + 消息”。
- Reader 不能再绑定预生成类型，必须按字段配置动态解析。
- 处理器分发必须在“文件/配置级别”完成，而不是依赖固定结构类。
- 模板必须改为配置驱动生成。
- 文件页必须展示运行状态、原始数据、处理消息。
