# AGENTS

## Scope
- Repository: `dataimportplatformcap`
- Current working branch: `feature/dynamic_data_factory`
- Primary implementation branch: `feature/dynamic-structure-implementation`
- Git user: `haihuazhang <haihuazhang941106@gmail.com>`
- `dataimportplatformrap` is read-only reference input, not a change target.

## Current Direction
- Goal: support configurable multi-level import structures, dynamic fields, template generation, and dynamic parsing in CAP.
- Frontend stays Fiori Elements first. Extend manifests/annotations before introducing custom UI.
- Data model stays in CAP CDS. Runtime does not generate CDS types dynamically.
- CAP baseline: `cds-services 4.8.0`, `Spring Boot 3.5.11`, `Spring Batch 5.2.5`, `cds-dk 9.8.3`.
- Local database baseline: PostgreSQL 16 via Homebrew, database `batchimportcat`.

## Runtime Architecture
- Config root: `BatchImportConfig`
- Dynamic config nodes: `BatchImportStructure`, `BatchImportField`
- Runtime artifacts: `BatchImportFile`, `BatchImportData`, `BatchImportMessage`
- Processor dispatch mode: `ProcessKey`
- Reader mode: parse workbook into in-memory hierarchical root nodes, then stream one root node per Spring Batch item
- Writer mode: `ProcessKeyDelegatingItemWriter` resolves a single `BatchImportProcessor` per file/job
- Dynamic data factory mode: processors consume `BatchImportProcessPayload`, not raw `DynamicNode`
- `DynamicNode` stays internal to reader/writer plumbing
- Processors primarily read `DynamicTable` / `DynamicRow` from payload

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
- Main method: `process(BatchImportProcessContext context, BatchImportProcessPayload payload)`
- `BatchImportProcessPayload` exposes root tables by structure UUID and structure name
- `DynamicRow` provides typed child-table access for nested processing
- Processors return `BatchImportProcessResult` with row/object messages.

## Local PostgreSQL
- Default CAP database kind is `postgres`.
- `.cdsrc-private.json` contains `[pg]` credentials for local PostgreSQL.
- `srv/pom.xml` uses `cds-feature-postgresql` and `org.postgresql:postgresql`.
- `db/package.json` uses `@cap-js/postgres`.
- Local profile in `application.yaml` is `pg`.
- Batch auto-run is disabled with `spring.batch.job.enabled=false` because the app defines multiple jobs.
- `@EnableBatchProcessing` is intentionally commented out in job configuration classes; do not re-enable unless framework behavior is re-evaluated.

## SQL Init Order
- Local startup uses repeatable PostgreSQL initialization in this order:
- `schema-pg-reset.sql`
- `org/springframework/batch/core/schema-drop-postgresql.sql`
- `org/springframework/batch/core/schema-postgresql.sql`
- `org/quartz/impl/jdbcjobstore/tables_postgres.sql`
- `schema-pg-compat.sql`
- generated `schema.sql`
- `schema-pg-reset.sql` drops CAP views that depend on Spring Batch / Quartz tables before those tables are recreated.
- `schema-pg-compat.sql` adds PostgreSQL compatibility column `qrtz_calendars.description`.
- Repeat startup against the same PostgreSQL database has been validated.

## Fiori Local Serving
- Static resource locations are configured as `file:./app/,file:../app/` to support starting from repo root or `srv`/IDE working directories.
- `/fiori.html` has been validated in browser and opens the FLP sandbox shell.
- FE apps behind tiles such as `#zzdtimpfile-manage` load from the FLP shell.
- Known non-blocking browser console noise remains for local dev:
- FLP search endpoints under `/sap/opu/odata/sap/ESH_SEARCH_SRV/*` and `/sap/es/ina/*`
- Flex endpoints under `/sap/bc/lrep/flex/*`
- Optional `Component-preload.js` and locale-specific `i18n_*` files when not built/generated

## Baseline Commands
- Compile: `mvn -q -f pom.xml -DskipTests compile`
- Run local app: `mvn -q -f pom.xml -DskipTests spring-boot:run`
- Repo status: `git status --short`
- Search: `rg`

## Delivery Files
- `rap-analysis.md`
- `cap-analysis.md`
- `technical-plan.md`
- `task.md`
