# After

After is a REST API for managing private events in personal spaces: houses, apartments, villas, estates, or other places where a host can organize a party and allow other users to join.

The goal of the MVP is to validate the main flow of an "Airbnb for parties" style platform: a user registers, creates an event as the host, and other users can join, leave, be invited, or be kicked by the host.

## Project Status

Backend MVP in development.

The project currently includes:

- User registration and login.
- JWT authentication.
- User profile management.
- Event creation and retrieval.
- Filters by event type and music style.
- Users joining and leaving events.
- User invitation and removal by the host.
- Event capacity control.
- DTO-based validation.
- Public responses through DTOs.
- OpenAPI/Swagger documentation.

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

The API prevents users from modifying another user's profile.

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
- Delete the event.

Main implemented rules:

- The host cannot join their own event as an attendee.
- The host cannot leave their own event.
- The host cannot be kicked.
- Event capacity cannot be exceeded.
- A user cannot join the same event twice.
- Only the host can invite, kick, or delete the event.

## DTOs

The project avoids exposing entities directly in the main API operations.

Current DTOs:

- `RegisterDTO`
- `LoginDTO`
- `UpdateUserDTO`
- `UpdateDisplayNameDTO`
- `CreateEventDTO`
- `AddressDTO`
- `UserResponseDTO`
- `EventResponseDTO`

This separates the public API contract from the internal persistence model.

## Main Endpoints

### Auth

| Method | Endpoint | Description |
| --- | --- | --- |
| POST | `/token/auth/register` | Register user |
| POST | `/token/auth/login` | Login and JWT generation |

### Users

| Method | Endpoint | Description |
| --- | --- | --- |
| GET | `/users/{id}` | Get user by id |
| PUT | `/users/{id}` | Update own profile |
| PATCH | `/users/{id}/display-name` | Update display name |

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
| DELETE | `/events/{eventId}/kick/user/{userId}` | Kick user |
| DELETE | `/events/{id}` | Delete event |

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
POST /token/auth/register
Content-Type: application/json
```

```json
{
  "username": "ethanlo",
  "password": "password123"
}
```

### Login

```http
POST /token/auth/login
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
Tests run: 52, Failures: 0, Errors: 0, Skipped: 0
```

Note: tests that go through real BCrypt may take a little longer because the encoder uses a high cost.

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

- `controller`: exposes REST endpoints and performs basic token authorization validation.
- `service`: contains business logic.
- `repositories`: database access through Spring Data JPA.
- `entity`: persisted model.
- `dto`: input and output API contracts.
- `utils`: utilities such as JWT generation and validation.
- `exceptions`: domain/API exceptions.

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
- Enums stored as text to avoid issues if their order changes.
- DTO validations to control input data.
- The host is stored separately from the attendee list.
- Isolated test configuration with in-memory HSQLDB.
- Mockito is loaded as a `javaagent` in Surefire for compatibility with the current JDK.

## Current Limitations

This project is an MVP. Some parts are prepared to evolve:

- Security does not yet use full Spring Security with JWT filters.
- Errors are not yet centralized with a `ControllerAdvice`.
- There is no connected frontend yet.
- There are no advanced roles or event states yet.
- Attendee/address visibility can evolve depending on privacy rules.

## Near-Term Roadmap

- Refactor security toward Spring Security.
- Global error handling.
- Swagger documentation improvements.
- Possible web or mobile frontend to consume the API.
- Event states: open, full, cancelled, finished.
- Privacy policies for address and attendees.

## Author

Project developed as a learning and backend/full stack portfolio MVP.
