# AGENTS

## Scope
- Repository: `dataimportplatformcap`
- Feature branch: `feature/dynamic-structure-implementation`
- Git user: `haihuazhang <haihuazhang941106@gmail.com>`
- `dataimportplatformrap` is read-only reference input, not a change target.

## Current Direction
- Goal: support configurable multi-level import structures, dynamic fields, template generation, and dynamic parsing in CAP.
- Frontend stays Fiori Elements first. Extend manifests/annotations before introducing custom UI.
- Data model stays in CAP CDS. Runtime does not generate CDS types dynamically.

## Runtime Architecture
- Config root: `BatchImportConfig`
- Dynamic config nodes: `BatchImportStructure`, `BatchImportField`
- Runtime artifacts: `BatchImportFile`, `BatchImportData`, `BatchImportMessage`
- Processor dispatch mode: `ProcessKey`
- Reader mode: parse workbook into in-memory hierarchical root nodes, then stream one root node per Spring Batch item
- Writer mode: `ProcessKeyDelegatingItemWriter` resolves a single `BatchImportProcessor` per file/job

## Compatibility Rules
- Keep legacy fields `StructName`, `SheetName`, `StartLine`, `StartColumn`, `ImplementedByClass` for migration/bootstrap compatibility.
- If no dynamic structure exists but legacy `StructName` exists, build one synthetic root structure from the CDS type.
- Legacy fallback auto-promotes the first field to key when no CDS key is available.

## Template Rules
- Template is regenerated from configuration.
- One sheet per structure.
- `StartLine` and `StartColumn` define where header rows begin.
- Reader starts data rows after configured header lines.

## Processor Contract
- Interface: `BatchImportProcessor`
- Required method: `getProcessKey()`
- Main method: `process(BatchImportProcessContext context, List<DynamicNode> items)`
- Processors return `BatchImportProcessResult` with row/object messages.

## Baseline Commands
- Compile: `mvn -q -f pom.xml -DskipTests compile`
- Repo status: `git status --short`
- Search: `rg`

## Delivery Files
- `rap-analysis.md`
- `cap-analysis.md`
- `technical-plan.md`
- `task.md`
