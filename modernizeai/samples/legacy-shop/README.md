# legacy-shop (demo input for ModernizeAI)

A deliberately outdated Spring Boot 2.5 / Java 8 project. It is **not** meant to be built; it exists
so ModernizeAI has known migration issues to find:

- Spring Boot 2.5.14 parent, Java 1.8
- `javax.persistence`, `javax.validation`, `javax.servlet`, `javax.annotation` imports
- `WebSecurityConfigurerAdapter`, `@EnableGlobalMethodSecurity`, `antMatchers`, `authorizeRequests`
- `WebMvcConfigurerAdapter`, `HandlerInterceptorAdapter`, old `LocalServerPort` import
- Springfox, Spring Cloud Sleuth, `mysql:mysql-connector-java`, JUnit 4
- Duplicate `commons-lang3` with different versions, pinned `jackson-databind`, undefined `${guava.version}`
- Renamed properties (`spring.redis.*`, `server.max-http-header-size`, ...) and `spring.factories` auto-config
- `sun.misc.BASE64Encoder`
- A fake password (shows snippet redaction) and a prompt-injection comment (shows that Claude is told to
  treat uploaded code as data)

Create the upload with `../make-sample-zips.sh` (or use the committed `legacy-shop.zip`).
