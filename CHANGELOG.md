# Changelog

All notable changes to this project will be documented in this file.

## [3.2.0] - 2026-07-11

### Upgraded
- **Spring Boot**: Upgraded from `3.5.7` to `4.1.0` (Spring Framework `7.0.8`).
  - Compatible with Spring Boot 4.x's Jakarta EE 11 baseline.
  - Auto-configuration continues to register via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
  - The library pins Jackson 2.x internally and is unaffected by Spring Boot 4's Jackson 3 default.
- **JUnit Jupiter**: `5.11.0` → `6.0.3`, aligned with Spring Boot 4.1's managed version (mixing 5.x engine with Spring Test 7 causes `NoSuchMethodError`).

### Fixed (dependency hygiene)
- **Spring dependencies are now `provided` scope** in `easy-es-spring` and `easy-es-boot-starter`.
  Previously they leaked at `compile` scope, so a consumer project that did not import the Spring Boot
  BOM (or declared easy-es before `spring-boot-starter`) would pull Spring Framework `7.0.8` and
  `spring-boot-autoconfigure` `4.1.0` into a Spring Boot 3.x application, failing at startup with
  `Could not find class [org.springframework.boot.thread.Threading]`.
  The same artifact now runs unmodified on both Spring Boot 3.x and 4.x.

### Known limitation
- Spring Boot 4.x manages `elasticsearch-client.version` to `9.4.2`, which switches the transport to
  `elasticsearch-rest5-client` (Apache HttpClient 5) and is incompatible with this framework's
  HttpClient 4 based client construction (`NoClassDefFoundError: org/apache/http/auth/Credentials`).
  Spring Boot 4.x users must pin `<elasticsearch-client.version>8.19.7</elasticsearch-client.version>`
  in their own `properties`. Native Elasticsearch Java Client 9.x support is not yet implemented.

### Verified
- Integration test suite `AllTest` (81 tests) passes against a live Elasticsearch 9.0.3 with the 8.19.7 Java client under Spring Boot 4.1.0.
- End-to-end consumer smoke tests: Spring Boot 3.5.7 with BOM, Spring Boot 3.5.7 without BOM (easy-es declared first), and Spring Boot 4.1.0 — all start the context, inject mappers, and reach a live Elasticsearch.
- `IndexFalseTest` updated for ES 8.1+ behavior: querying an `index: false` keyword field with doc values no longer throws; it searches via doc values.

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
