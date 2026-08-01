# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 版本线(既定策略,勿擅自变更)

本仓库并行维护两条版本线,按使用方的 Spring Boot 版本区分:

| 版本 | 面向 | ES Java Client | 分支 |
|---|---|---|---|
| **3.2.0** | Spring Boot 3.x | 8.19.7 | `release/3.2.0` |
| **3.3.0** | Spring Boot 4.x | 9.4.2 | `release/3.3.0` = `main` |

- 两条线**功能完全等价**;3.3.0 相对 3.2.0 只改了 5 个 ES9 客户端适配文件
  (`EsClientUtils`、`BaseEsMapperImpl`、`WrapperProcessor`、`IndexUtils`、`EntityInfo`),没有新功能。
- 版本线之所以按 Spring Boot 划分,是因为 Spring Boot 的依赖管理会锁定
  `elasticsearch-client.version`(SB3 默认 8.18.8,SB4 默认 9.4.2),而 8.x 与 9.x 客户端
  二进制不兼容(底层分别是 Apache HttpClient 4 与 5)。**消费方 BOM 的 dependencyManagement
  永远优先于传递依赖,easy-es 无法从自己的 pom 覆盖**——已验证:即使在 starter 中显式写死
  版本也无效。交叉使用只能由使用方在自己的 `properties` 中固定该版本。
- **注意区分「ES 服务端版本」与「ES 客户端库版本」**:上表只关乎客户端库。
  Spring Boot 3.x + 3.2.0(8.x 客户端)连 **Elasticsearch 9.x 服务端是零配置可用的**,
  因为 ES 9.x 服务端声明 `minimum_wire_compatibility_version: 8.18.0`。已实测全链路 CRUD 通过。
- 新功能/修复若需同时进两条线,目前靠手动同步。当这种同步频繁到成为负担时,
  再考虑把上述 5 处差异抽成兼容层 + Maven profile 双构建(单源码出两个构件)。

## Build & Test Commands

```bash
# Build all modules (skip tests)
mvn clean install -DskipTests

# Build and run all tests
mvn clean install

# Run tests for a specific module
mvn test -pl easy-es-springboot-test

# Run a single test class
mvn test -pl easy-es-springboot-test -Dtest=AllTest

# Run a single test method
mvn test -pl easy-es-springboot-test -Dtest=AllTest#testInsert
```

Tests in `easy-es-springboot-test` and `easy-es-solon-test` require a running Elasticsearch instance configured in the respective `application.yml`.

## Module Structure

```
easy-es-parent/          # Parent POM with dependency management
easy-es-annotation/      # @IndexName, @IndexField, @IndexId, @HighLight, @Join, @Distance, etc.
easy-es-common/          # Constants, enums, exceptions, EasyEsProperties, shared utilities
easy-es-extension/       # Extension interfaces and base abstractions
easy-es-core/            # Core engine (see below)
easy-es-spring/          # Spring Framework integration (beans, AOP, context)
easy-es-boot-starter/    # Spring Boot auto-configuration entry point
easy-es-solon-plugin/    # Solon framework integration (alternative to Spring Boot)
easy-es-springboot-test/ # Integration tests with Spring Boot
easy-es-solon-test/      # Integration tests with Solon
easy-es-springboot-sample/ # Sample application
```

## Core Architecture

### Mapper Pattern
Users define mapper interfaces extending `BaseEsMapper<T>` (in `easy-es-core/kernel/`). At runtime, `EsMapperProxy` provides the implementation via JDK dynamic proxy. This mirrors the MyBatis-Plus pattern.

### Wrapper/Builder Pattern
All query/update/index conditions are built via fluent wrappers:
- `LambdaEsQueryWrapper<T>` — query conditions and field selection
- `LambdaEsUpdateWrapper<T>` — update conditions
- `LambdaEsIndexWrapper<T>` — index management
- Chain variants: `LambdaEsQueryChainWrapper`, `LambdaEsUpdateChainWrapper`, `LambdaEsIndexChainWrapper`
- Factory entry point: `EsWrappers` utility class

Wrappers use Java lambda method references (e.g., `Document::getTitle`) for type-safe field references, resolved to actual field names via `SFunction<T, R>`.

### Entity Metadata
`EntityInfo` (in `easy-es-core/biz/`) holds complete mapping metadata for an entity class — index name, routing, field mappings, join config, etc. `EntityInfoHelper` builds and caches these at startup by scanning annotations.

### Automatic Index Lifecycle Management
This is the standout feature. Two strategies in `easy-es-core/index/`:
- `AutoProcessIndexSmoothlyStrategy` — zero-downtime index updates: creates a new versioned index, migrates data, then switches the alias. Uses `S1`/`SO` index name suffixes.
- `AutoProcessIndexNotSmoothlyStrategy` — direct index operations without migration.

Strategy is selected via `easy-es.global-config.process-index-mode` in configuration.

### Query Processing Pipeline
`LambdaEsQueryWrapper` → `WrapperProcessor` → `SearchRequest.Builder` → Elasticsearch Java Client (`co.elastic.clients`). The processor handles keyword suffix inference, nested query wrapping, geo queries, aggregations, and highlighting.

### Framework Integration Points
- **Spring Boot**: `easy-es-boot-starter` provides `@EnableEasyEs` and auto-configuration via `EasyEsAutoConfiguration`. Mappers are registered as Spring beans.
- **Solon**: `easy-es-solon-plugin` provides equivalent integration for the Solon framework.
- Both integrations share the same `easy-es-core` and `easy-es-spring` logic.

## Key Annotations

| Annotation | Location | Purpose |
|---|---|---|
| `@IndexName` | Class | Maps entity to an ES index |
| `@IndexId` | Field | Marks the document ID field |
| `@IndexField` | Field | Configures field type, analyzer, etc. |
| `@HighLight` | Field | Marks fields for highlight queries |
| `@Join` | Class | Configures parent-child join relationships |
| `@Distance` | Field | Marks geo-distance fields |

## Configuration

Main config prefix: `easy-es` in `application.yml`. Key properties are in `EasyEsProperties` (`easy-es-common`). Global runtime state is held in `GlobalConfigCache`.
