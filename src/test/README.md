# Tests

| Package | What it tests |
|---|---|
| `unit` | Services with Mockito mocks, mappers, role initialization and exception handling without starting Spring |
| `integration` | HTTP endpoints, security, JWT configuration, application startup, development admin, account deletion, production profile and MySQL |

`integration/HttpTestBase` is a helper for the HTTP tests. It starts Spring with MockMvc, cleans the test database and creates users, tokens and events. It is not a test class.

The regular suite uses an in-memory HSQLDB database (`application-test.properties`), so no local MySQL is needed:

```sh
./mvnw test
```

`MySqlContainerTest` starts a real MySQL with Testcontainers (Docker must be running). It checks persistence and that event capacity is never exceeded when several requests arrive at the same time. It runs in CI and locally with:

```sh
AFTER_RUN_MYSQL_TESTS=true ./mvnw -Dtest=MySqlContainerTest test
```

Environment variables override `application-test.properties`. Tests that need the JWT secret or the development admin read them with `@Value`, so they work with or without those variables exported.

`./mvnw verify` generates a JaCoCo report in `target/site/jacoco/index.html`.
