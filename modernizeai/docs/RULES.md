# Rule catalog

| Rule | Severity | Certainty | Detects |
|---|---|---|---|
| MIG-POM-001 | HIGH | VERIFIED | `pom.xml` is not well-formed XML (analysis continues without it) |
| MIG-BOOT-001 | HIGH | VERIFIED | Spring Boot < 3 (parent, BOM import, plugin or property) |
| MIG-BOOT-002 | LOW | VERIFIED | Spring Boot 3.0/3.1 (older 3.x line) |
| MIG-BOOT-003 | INFO | POTENTIAL | Spring Boot dependencies present but version not determinable (likely from an un-uploaded parent) |
| MIG-JAVA-001 | HIGH | VERIFIED (POTENTIAL if inferred from parent default) | Java level below the target (17 or 21) |
| MIG-JAVA-003 | MEDIUM | POTENTIAL | No Java level declared |
| MIG-JAVA-004 | LOW | VERIFIED | Compiler `source` ≠ `target` |
| MIG-JAKARTA-001 | HIGH | VERIFIED | Imports from Java EE packages that moved to `jakarta.*` (servlet, persistence, validation, transaction, ws.rs, xml.bind, …). JDK-owned `javax.*` (sql, crypto, naming, swing, xml.parsers, …) is ignored |
| MIG-JAKARTA-002 | MEDIUM | POTENTIAL | `javax.annotation.*` / `javax.inject.*` (only some classes moved; JSR-305 excluded) |
| MIG-SPRING-001 | HIGH | VERIFIED / POTENTIAL (wildcard import) | Extends `WebSecurityConfigurerAdapter` |
| MIG-SPRING-002 | MEDIUM | VERIFIED | `@EnableGlobalMethodSecurity` |
| MIG-SPRING-003 | HIGH | POTENTIAL | `antMatchers` / `mvcMatchers` / `regexMatchers` calls in files importing Spring Security |
| MIG-SPRING-004 | MEDIUM | POTENTIAL | `authorizeRequests()` |
| MIG-SPRING-005 | HIGH | VERIFIED | Extends `WebMvcConfigurerAdapter` |
| MIG-SPRING-006 | HIGH | VERIFIED | Extends `HandlerInterceptorAdapter` |
| MIG-SPRING-007 | MEDIUM | VERIFIED | Old `org.springframework.boot.web.server.LocalServerPort` import |
| MIG-SPRING-008 | MEDIUM | VERIFIED | Old `ConstructorBinding` import |
| MIG-SPRING-009 | HIGH | VERIFIED | Spring Cloud Sleuth API imports |
| MIG-CONFIG-001 | MEDIUM | VERIFIED | Renamed/removed properties in `application*.properties|yml` (`spring.redis.*`, `server.max-http-header-size`, `management.metrics.export.*`, …) |
| MIG-CONFIG-002 | HIGH | VERIFIED | Auto-configuration registered in `spring.factories` |
| MIG-DEP-001 | HIGH (conflict) / LOW (same version) | VERIFIED | Same dependency declared twice in one POM |
| MIG-DEP-002 | HIGH / LOW / INFO | VERIFIED only when no parent/BOM exists | Dependency without a version and no visible management |
| MIG-DEP-003 | MEDIUM | POTENTIAL | Explicit version overriding a Spring Boot-managed library |
| MIG-DEP-004 | varies | VERIFIED / POTENTIAL | Artifacts without Boot 3 support: Springfox, `javax.*` APIs, `mysql:mysql-connector-java`, Sleuth, Ehcache 2, HttpClient 4, old Hibernate Validator, JUnit 4 |
| MIG-DEP-005 | HIGH / INFO | VERIFIED only without parent | `${property}` in a version that this POM doesn't define |
| MIG-DEP-006 | HIGH | VERIFIED | `org.springframework.boot` artifact pinned to a different version than the Boot parent/BOM |
| MIG-JDK-001 | HIGH (`sun.misc.BASE64*`) / MEDIUM | VERIFIED / POTENTIAL | Imports of JDK-internal `sun.*` / `com.sun.*.internal.*` |
