# Changelog

All notable changes to this project will be documented in this file.

## [3.2.0] - 2026-07-11

### Upgraded
- **Spring Boot**: Upgraded from `3.5.7` to `4.0.7` (Spring Framework `7.0.8`).
  - Compatible with Spring Boot 4.x's Jakarta EE 11 baseline.
  - Auto-configuration continues to register via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
  - The library pins Jackson 2.x internally and is unaffected by Spring Boot 4's Jackson 3 default.

### Merged
- Merged upstream `dromara/easy-es` v3.0.1 and v3.0.2:
  - Customizable `ObjectMapper` via `EasyEsObjectMapperCustomizer` / `ObjectMapperBean`.
  - Custom request headers via `EasyEsHeadersCustomizer`.
  - Enhanced date serialization (`Date`/`LocalDate`/`LocalDateTime` with configurable formats).
  - KNN/ANN vector query support and or-nested query enhancements.
  - Unified `IndexSettings` construction with `refreshInterval` support.
  - `EasyEsConfiguredCondition` conditional assembly.

### Fixed
- dromara/easy-es#164: `orderBy(OrderByParam)` string-based sorts now preserve invocation order with lambda sorts; `_score` uses the score sorter explicitly.
- dromara/easy-es#167: pipeline aggregations with more than two levels no longer lose deeper aggregation fields (aggregation tree is now built bottom-up).
- dromara/easy-es#151: `updateIndex` can now update index settings alone via `wrapper.settings(...)` without requiring mapping args.
- dromara/easy-es#126: `filter()` chaining now correctly generates `filter` clauses instead of always `must`.
- dromara/easy-es#157: `createIndex(String indexName)` is now reusable for the same entity (settings stored as immutable `IndexSettings`).

## [3.1.0] - 2025-11-26

### Upgraded
- **Spring Boot**: Upgraded from `2.x` to `3.5.7`.
  - Migrated from `javax.*` to `jakarta.*` namespace for Jakarta EE 9+ compatibility.
  - Updated configuration properties to align with Spring Boot 3.x standards.
- **Elasticsearch Java Client**: Upgraded to `8.19.7`.
  - Replaced deprecated `RestHighLevelClient` with the new official `ElasticsearchClient`.
  - Adapted core logic to use the new fluent API of the Elasticsearch Java Client.
- **Java Version**: Minimum required Java version is now `17`.

### Added
- Implemented connection pool management and retry mechanisms in `EsClientUtils`.
- Added support for Elasticsearch 8.x features.
- Encapsulated new 8.x APIs while maintaining backward compatibility where possible.

### Fixed
- Resolved compilation errors due to API changes in Elasticsearch 8.x (e.g., `size()` -> `maxDocs()`, `inline` script -> `source`).
- Fixed `bucket.key()` type handling (changed from String check to Long check).

### Known Issues
- Legacy code using `RestHighLevelClient` directly needs to be migrated to `ElasticsearchClient`.
- Some custom configurations might need adjustment for Spring Boot 3.x.
