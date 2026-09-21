# After

After is a REST API for managing private events in personal spaces: houses, apartments, villas, estates, or other places where a host can organize a party and allow other users to join.

The goal of the MVP is to validate the main flow of an "Airbnb for parties" style platform: a user registers, creates an event as the host, and other users can join, leave, be invited, or be kicked by the host.

## Project Status

Portfolio backend in active development. The core product flows are implemented, tested, documented, and validated in GitHub Actions.

The project currently includes:

- User registration and login.
- JWT authentication.
- User profile management.
- Event creation and retrieval.
- Filters by event type and music style.
- Users joining and leaving events.
- User invitation and removal by the host.
- Event capacity control.
- Role-based permissions with FREE, PREMIUM, and ADMIN roles.
- Global API error responses through `GlobalExceptionHandler`.
- DTO-based validation.
- Public responses through DTOs.
- OpenAPI/Swagger documentation.
- In-memory caching for user and event read queries.
- Continuous integration checks for pull requests.

## Tech Stack

- Java 21
- Spring Boot 4
- Spring Web MVC
- Spring Data JPA
- MySQL
- JWT with JJWT
- BCrypt for passwords
- Jakarta Validation
- Lombok
- Springdoc OpenAPI / Swagger UI
- Maven
- HSQLDB for in-memory tests

## Domain Model

The MVP is organized around three main concepts:

- `UserAccess`: access credentials, username, encrypted password, and relation with the user.
- `Users`: public user profile.
- `Events`: event created by a host, with party details, address, capacity, and attendees.
- `UserRole`: role assigned to a user with a list of allowed resources.

An event has:

- Name.
- Description.
- Date and time.
- Capacity.
- Event type.
- Music style.
- Host.
- List of attendee users.
- Associated address.

## Main Features

### Authentication

Users can register and log in.

After login, the API returns a JWT that must be sent in protected requests:

```http
Authorization: Bearer <token>
```

### Users

An authenticated user can:

- Retrieve a profile.
- Update their own profile.
- Update their display name.
- Delete their own profile.

The API prevents users from modifying another user's profile.

Admins can update user roles through a protected role-management endpoint.

### Events

An authenticated user can:

- List events.
- Filter events by type and music style.
- Retrieve an event by id.
- Create an event as host.
- Join an event.
- Leave an event.

The host of an event can:

- Invite users.
- Kick users.
- Update event details.
- Delete the event.

Main implemented rules:

- The host cannot join their own event as an attendee.
- The host cannot leave their own event.
- The host cannot be kicked.
- Event capacity cannot be exceeded.
- A user cannot join the same event twice.
- Only the host can invite, kick, or delete the event.
- Only users with `UPDATE_ROLE` permission can change another user's role.
- Invalid or expired tokens return a standardized `401 Unauthorized` response.

## DTOs

The project avoids exposing entities directly in the main API operations.

Current DTOs:

- `LoginDTO`
- `UpdateUserDTO`
- `UpdateDisplayNameDTO`
- `UpdateRoleDTO`
- `CreateEventDTO`
- `UpdateEventDTO`
- `AddressDTO`
- `UserResponseDTO`
- `EventResponseDTO`
- `ErrorResponseDTO`

This separates the public API contract from the internal persistence model.

## Error Responses

Most application errors are centralized through `GlobalExceptionHandler` and returned as `ErrorResponseDTO`:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Event capacity is full",
  "path": "/events/6/join",
  "timestamp": "2026-08-11T13:25:00"
}
```

Current global mappings:

| Exception | HTTP status |
| --- | --- |
| `InvalidTokenException` | `401 Unauthorized` |
| `UnauthorizedException` | `403 Forbidden` |
| `NotFoundException` | `404 Not Found` |
| `BadRequestException` | `400 Bad Request` |
| `FormatRequestException` | `400 Bad Request` |
| `AlreadyExistsException` | `409 Conflict` |
| `RuntimeException` | `500 Internal Server Error` |

Validation errors from Jakarta Validation return a field-error map, for example:

```json
{
  "username": "Username required",
  "password": "Password must contain at least 6 characters"
}
```

## Main Endpoints

### Auth

| Method | Endpoint | Description |
| --- | --- | --- |
| POST | `/v1/auth/register` | Register user |
| POST | `/v1/auth/login` | Login and JWT generation |
| POST | `/v1/auth/logout` | Invalidate current token |

### Users

| Method | Endpoint | Description |
| --- | --- | --- |
| GET | `/users/{id}` | Get user by id |
| PUT | `/users/{id}` | Update own profile |
| PATCH | `/users/{id}/display-name` | Update display name |
| PATCH | `/users/{id}/role` | Update user role, requires `UPDATE_ROLE` |
| DELETE | `/users/{id}` | Delete own profile |

### Events

| Method | Endpoint | Description |
| --- | --- | --- |
| GET | `/events` | List events |
| GET | `/events?type=AFTER` | Filter by type |
| GET | `/events?style=TECHNO` | Filter by music style |
| GET | `/events?type=AFTER&style=TECHNO` | Filter by type and style |
| GET | `/events/{id}` | Get event by id |
| POST | `/events` | Create event |
| PATCH | `/events/{id}/join` | Join event |
| PATCH | `/events/{id}/leave` | Leave event |
| PATCH | `/events/{eventId}/invite/user/{userId}` | Invite user |
| PATCH | `/events/{eventId}` | Update event, host only |
| DELETE | `/events/{eventId}/kick/user/{userId}` | Kick user |
| DELETE | `/events/{id}` | Delete event |

## Roles and Permissions

The application initializes three roles on startup:

| Role | Includes |
| --- | --- |
| `FREE` | `VIEW_EVENT`, `CREATE_EVENT`, `ACCESS_EVENT`, `UPDATE_USER`, `VIEW_USER` |
| `PREMIUM` | All FREE permissions plus `INVITE_USER` |
| `ADMIN` | All PREMIUM permissions plus `KICK_USER`, `DELETE_EVENT`, `UPDATE_ROLE` |

An admin user is also created on startup if it does not already exist. Its values are configured with:

```properties
after.admin.username=
after.admin.password=
after.admin.name=
after.admin.lastname=
after.admin.email=
after.admin.phone-number=
```

## Allowed Values

### EventType

```text
AFTER
PREVIA
HOUSE_PARTY
CHILL
CLUB
POOL_PARTY
```

### MusicStyle

```text
REGGAETON
TECHNO
TEKNO
HOUSE
POP
MIXED
```

## Usage Examples

### Register

```http
POST /v1/auth/register
Content-Type: application/json
```

```json
{
  "username": "ethanlo",
  "password": "password123",
  "name": "Ethan",
  "lastname": "Lorente",
  "email": "ethanlo@example.com",
  "phoneNumber": "+34612345678"
}
```

### Login

```http
POST /v1/auth/login
Content-Type: application/json
```

```json
{
  "username": "ethanlo",
  "password": "password123"
}
```

Response:

```text
eyJhbGciOiJIUzI1NiJ9...
```

### Create Event

```http
POST /events
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "name": "Techno Underground Barcelona",
  "eventType": "AFTER",
  "musicStyle": "TECHNO",
  "description": "Underground techno party in Barcelona",
  "dateTime": "2026-08-15T23:00:00",
  "capacity": 150,
  "address": {
    "street": "Carrer Marina",
    "streetNum": "25",
    "postalCode": "08005",
    "additionalInfo": "Industrial warehouse near the beach",
    "city": "Barcelona",
    "province": "Catalonia"
  }
}
```

Response:

```json
{
  "id": 6,
  "name": "Techno Underground Barcelona",
  "description": "Underground techno party in Barcelona",
  "dateTime": "2026-08-15T23:00:00",
  "capacity": 150,
  "eventType": "AFTER",
  "musicStyle": "TECHNO",
  "hostDisplayName": "ethanlo",
  "usersCount": 0
}
```

### Update Event

```http
PATCH /events/6
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "name": "Techno Underground Updated",
  "capacity": 80,
  "description": "Updated private party details"
}
```

### Update User Role

```http
PATCH /users/7/role
Authorization: Bearer <admin-token>
Content-Type: application/json
```

```json
{
  "roleName": "PREMIUM"
}
```

## Installation and Execution

### Requirements

- Java 21
- Maven Wrapper included in the project
- Local MySQL
- `after` database

### Create Database

```sql
CREATE DATABASE after;
```

### Configuration

The main configuration is located at:

```text
src/main/resources/application.properties
```

Current default values:

```properties
server.port=8081
spring.datasource.url=jdbc:mysql://localhost:3306/after
spring.datasource.username=root
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=update
```

The JWT secret is read from the `JWT_SECRET` environment variable.

If it does not exist, a Base64 development value is used:

```properties
jwt.secret=${JWT_SECRET:ZGV2LXNlY3JldC1kZXYtc2VjcmV0LWRldi1zZWNyZXQ=}
```

For a real environment, a custom secret should be defined:

```bash
export JWT_SECRET=<base64-secret>
```

### Run the Application

```bash
./mvnw spring-boot:run
```

The API will be available at:

```text
http://localhost:8081
```

### Run Tests

```bash
./mvnw test
```

The current suite includes service unit tests and HTTP integration tests with `MockMvc`.

The integration tests start the Spring Boot context and use an in-memory HSQL database configured in:

```text
src/test/resources/application.properties
```

This makes it possible to test HTTP flows without depending on local MySQL or real data.

Current coverage:

- Spring Boot context startup.
- Service logic for users, access, and events.
- Registration and login through HTTP.
- JWT generation and validation in protected flows.
- User retrieval and update through HTTP.
- Event creation, retrieval, listing, and filtering through HTTP.
- Event join, leave, invite, kick, and delete through HTTP.
- DTO validations.
- Main errors: invalid token, not found, bad request, conflict, and forbidden.

Latest test execution result:

```text
Tests run: 71, Failures: 0, Errors: 0, Skipped: 0
```

Note: tests that go through real BCrypt may take a little longer because the encoder uses a high cost.

## Continuous Integration

Every pull request targeting `main` runs the GitHub Actions workflow in `.github/workflows/ci.yml`.

The workflow uses Java 21 and executes:

```bash
./mvnw clean verify
```

This prevents changes from being merged without compiling and passing the test suite.

## Swagger

With the application running, Swagger UI is available at:

```text
http://localhost:8081/swagger-ui/index.html
```

## Current Architecture

The project follows a classic Spring Boot structure:

```text
controller/
service/
repositories/
entity/
dto/
exceptions/
enums/
utils/
```

Responsibilities:

- `controller`: exposes REST endpoints and delegates authentication to Spring Security.
- `service`: contains business logic.
- `repositories`: database access through Spring Data JPA.
- `entity`: persisted model.
- `dto`: input and output API contracts.
- `utils`: utilities such as JWT generation and validation.
- `exceptions`: domain/API exceptions.
- `loader`: startup data initialization for roles and the admin account.
- `mappers`: repeated entity-to-DTO conversion logic.
- `cache`: in-memory cache configuration for frequently read users and events.

## Tests

The test folder is split by responsibility:

```text
src/test/java/com/afterApp/after/services/
src/test/java/com/afterApp/after/controller/
```

- `services`: business logic unit tests with Mockito.
- `controller`: HTTP flow integration tests with `@SpringBootTest`, `@AutoConfigureMockMvc`, real JWT, and real repositories over HSQLDB.

Controller tests do not depend on an external API or a manually started server. `MockMvc` executes requests against the Spring context inside the test itself.

## Technical Decisions

- Passwords encrypted with BCrypt.
- JWT with a one-hour expiration.
- DTOs to separate the public API from JPA entities.
- Centralized error responses through `GlobalExceptionHandler`.
- Role permissions initialized from composable FREE, PREMIUM, and ADMIN definitions.
- Enums stored as text to avoid issues if their order changes.
- DTO validations to control input data.
- The host is stored separately from the attendee list.
- Isolated test configuration with in-memory HSQLDB.
- Mockito is loaded as a `javaagent` in Surefire for compatibility with the current JDK.
- `ConcurrentMapCacheManager` caches event and user reads. Event updates/deletes and user profile updates/deletes evict cached entries to avoid serving stale data.

## Current Limitations

The API is suitable as a portfolio backend, but these areas must evolve before a production launch:

- Security does not yet use a full Spring Security filter chain or persistent token revocation.
- Production configuration still needs environment-specific profiles and externally managed secrets; the development admin credentials must be overridden outside local development.
- Database schema changes use Hibernate `ddl-auto=update`; versioned migrations are still needed.
- The in-memory cache is appropriate for local development and a single application instance. A shared cache and complete invalidation strategy are needed before horizontal scaling.
- There is no Docker-based deployment or connected frontend yet.

## Near-Term Roadmap

- Adopt Spring Security with JWT filters, refresh-token handling, and persistent token revocation.
- Add Docker, database migrations, environment profiles, and a deployable production configuration.
- Add pagination, sorting, concurrency protection for capacity, and a shared cache before scaling event discovery.
- Add application health checks, structured logging, metrics, and error monitoring.
- Build a web or mobile frontend, event states, and privacy policies for addresses and attendees.

## Author

Project developed as a learning and backend/full stack portfolio MVP.
