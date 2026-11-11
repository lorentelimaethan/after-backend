# Tests

| Package | What it tests | Type |
|---|---|---|
| `service` | Business rules of the services, with Mockito mocks | Unit |
| `mappers` | Entity ↔ DTO mapping | Unit |
| `loader` | Role and development admin initialization | Unit / Spring |
| `utils` | JWT generation and validation | Spring |
| `controller` | HTTP endpoints, status codes, validation and cache | HTTP (MockMvc + Spring) |
| `security` | Authentication and Actuator protection | HTTP |
| `integration` | Account deletion, prod profile and real MySQL | Integration |
| `support` | `HttpTestBase`: shared setup for the HTTP tests | — |

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
