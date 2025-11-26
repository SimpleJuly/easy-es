# Changelog

All notable changes to this project will be documented in this file.

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
