# AGENTS.md

Guidelines for AI agents and coding assistants working on the After backend.

## Project Context

After is a Java/Spring Boot backend MVP for private events hosted in personal spaces such as houses, flats, chalets or estates. The core product flow is:

- A user registers and logs in.
- A user creates an event as host.
- Other users can join, leave, be invited, or be kicked from an event.
- Hosts control privileged event actions.

This repository is currently a backend API. Do not assume there is a frontend unless one is explicitly added.

## Tech Stack

- Java 21
- Spring Boot 4
- Spring Web MVC
- Spring Data JPA
- MySQL
- JWT with JJWT
- BCrypt for password hashing
- Jakarta Validation
- Lombok
- Springdoc OpenAPI / Swagger
- Maven
- JUnit 5 and Mockito for tests

## Current Package Responsibilities

Follow the existing package boundaries:

- `controller`: REST endpoints, request validation entrypoint, HTTP status mapping.
- `service`: business rules, authorization checks, entity/DTO mapping.
- `repositories`: Spring Data JPA persistence access only.
- `entity`: JPA persistence model.
- `dto`: API request and response contracts.
- `exceptions`: domain/API exception types.
- `enums`: stable domain enumerations.
- `utils`: cross-cutting helpers such as JWT utilities.

Do not introduce a new architectural layer unless it removes real duplication or supports a clearly repeated pattern.

## General Coding Rules

- Preserve the existing Spring Boot style unless a change is explicitly requested.
- Keep changes small and local to the requested feature.
- Do not duplicate code. If the same logic appears in multiple places, extract it into a private helper, mapper method, shared service method, or utility depending on ownership.
- Do not add broad abstractions for one-off cases.
- Prefer clear business-rule methods over clever code.
- Keep method names descriptive and aligned with the current naming style.
- Do not move files or rename public API paths unless the user explicitly asks.
- Remove unused imports when editing a file.
- Do not add comments that simply restate the code. Add comments only for non-obvious domain or security decisions.

## API and DTO Rules

- Controllers must not accept JPA entities as request bodies.
- Public API responses should use response DTOs, not entities.
- Request DTOs should contain only client-provided fields.
- Response DTOs should contain only fields that are safe and useful for the client.
- Do not expose password hashes, `UserAccess`, internal entity relationships, or full user lists unless the endpoint explicitly needs them.
- Keep entity-to-DTO mapping in the service layer unless a dedicated mapper is introduced for repeated mapping logic.
- Existing mapping pattern:
  - `EventServices.toDto(Events e)`
  - `UserServices.toDto(Users u)`
- If mapping logic grows or is duplicated across services, extract a mapper class instead of copying mapping code.

## Validation Rules

- Validate input through DTO annotations using Jakarta Validation.
- Use `@Valid` on controller request bodies that contain validated DTOs.
- Put format and required-field constraints on DTOs, not only on entities.
- Validate important business rules in services, not only through annotations.
- Examples of service-level rules that must remain protected:
  - Host cannot join their own event.
  - Host cannot leave their own event.
  - Host cannot be kicked.
  - Event capacity cannot be exceeded.
  - User cannot join the same event twice.
  - Only the host can invite, kick or delete.

## Security Rules

- Treat all controller inputs as untrusted.
- Never return password hashes or credential entities.
- Passwords must remain hashed with BCrypt.
- JWT secrets must not be hardcoded for production. Use configuration/environment variables.
- Protected endpoints must validate the `Authorization: Bearer <token>` header before calling business logic.
- Authorization checks belong in services when they depend on domain state.
- Do not trust user ids from path parameters for ownership. Always compare against the user extracted from the token.
- When comparing users in event attendee sets, compare by `id`, not by object instance identity.
- Return 401 for invalid/missing authentication and 403 for authenticated users without permission.
- Avoid logging tokens, passwords, or raw credentials.

## Performance Rules

- Avoid unnecessary database calls inside loops.
- Prefer repository queries for filtering instead of loading everything and filtering in memory.
- Keep DTO mapping linear and simple.
- Avoid repeated stream traversals over the same collection when one pass is enough.
- For current MVP-sized collections, simple streams are fine. If attendee lists grow, consider repository-level queries or pagination.
- Do not introduce inefficient nested loops for membership checks when ids can be compared with sets or repository queries.
- Do not perform expensive work in controllers.

## Business Rules for Events

When changing event logic, preserve these invariants:

- `host` is separate from `users`.
- `users` represents attendees, not the host.
- Capacity applies to attendees in `users`.
- Join should fail if:
  - requester is host,
  - event is full,
  - requester is already attending.
- Leave should fail if:
  - requester is host,
  - requester is not attending.
- Invite should fail if:
  - requester is not host,
  - invited user is host,
  - invited user is already attending,
  - event is full.
- Kick should fail if:
  - requester is not host,
  - target user is host,
  - target user is not attending.
- Delete should be host-only.

## Error Handling Rules

- Use existing custom exceptions where appropriate:
  - `BadRequestException`
  - `NotFoundException`
  - `UnauthorizedException`
  - `AlreadyExistsException`
  - `FormatRequestException`
- Do not throw generic `RuntimeException` for expected business failures.
- Keep HTTP status mapping consistent:
  - 400 for invalid request or invalid business state.
  - 401 for invalid/missing token.
  - 403 for authenticated user without permission.
  - 404 for missing resource.
  - 409 for conflicts such as duplicate display names when applicable.
  - 500 only for unexpected failures.
- If a global exception handler is introduced later, remove duplicated controller catch blocks instead of adding more duplicated catch logic.

## Repository Rules

- Repositories should stay thin.
- Use Spring Data JPA method names for simple queries.
- Add explicit queries only when method names become unclear or performance requires it.
- Do not put business logic in repositories.
- Keep enum persistence as strings in entities.

## Testing Rules

- Existing tests use JUnit 5 and Mockito.
- Service tests should focus on business rules and repository interactions.
- Controller/flow tests should verify HTTP status codes, request/response DTOs, validation, auth behavior and important end-to-end flows.
- Add tests for every new business rule.
- For regressions, add a test that fails before the fix and passes after.
- Do not rely on test order.
- Avoid tests that require manual database state unless explicitly designed as integration tests.
- Prefer clear test names such as `shouldJoinEventSuccessfully` or `shouldThrowWhenHostJoinsOwnEvent`.

Minimum flow coverage to protect:

- Register user.
- Login and receive token.
- Create event as authenticated host.
- Join event as another user.
- Reject duplicate join.
- Reject join when event is full.
- Leave event.
- Reject host leaving own event.
- Invite user as host.
- Reject invite by non-host.
- Kick user as host.
- Reject kick by non-host.
- Delete event as host.
- Reject delete by non-host.

## Swagger and Documentation Rules

- Keep Swagger examples aligned with actual DTO responses.
- If a response DTO changes, update OpenAPI examples and README examples.
- Do not document fields that are not returned.
- Do not expose internal entity structure in documentation examples.

## Configuration Rules

- Keep local defaults developer-friendly, but mark production-sensitive values clearly.
- Do not commit real secrets.
- `JWT_SECRET` must be provided through environment/configuration for real environments.
- MySQL local config currently expects database `after`.
- If test-specific database config is introduced, isolate it under test resources or profiles.

## What Not to Do

- Do not accept `Users`, `Events`, `Address`, or `UserAccess` directly as public request bodies.
- Do not return `UserAccess`.
- Do not expose password fields.
- Do not copy-paste authorization logic into many places if a helper can safely centralize it.
- Do not add new dependencies without a clear reason.
- Do not make broad refactors while implementing a small feature.
- Do not silently change endpoint paths.
- Do not skip validation for new DTOs.
- Do not hide expected domain failures behind 500 responses.

## Before Finishing a Change

Run:

```bash
./mvnw test
```

If tests cannot run because MySQL or sandbox restrictions block local connections, report that clearly and explain what was verified instead.

Also check:

- No unused imports in edited files.
- No new entity exposure in controllers.
- No duplicated mapping or authorization code.
- New business behavior has tests or a clear reason why tests were not added.
- README and Swagger examples still match the API if public behavior changed.

