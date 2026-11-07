# AdministraPochi Copilot Instructions

## Project Context

AdministraPochi is a modular application for managing vehicles, users, payments, expenses, and related operational records.

The repository contains a Spring Boot backend under the `backend` directory. All backend code must follow the existing project conventions and the layered architecture defined in this document.

## Source of Truth

When implementing or modifying a module, use the following sources in this order:

1. The module specification under `docs/modules/` for functional requirements and business rules.
2. This file for global architecture, implementation, and testing rules.
3. Existing production code for established package structure, naming, and implementation conventions.
4. The Users module as the initial reference implementation for new modules.

Do not introduce a different architectural pattern unless the module specification explicitly requires it.

## Backend Technology Stack

Use the technologies already configured in `backend/pom.xml`, including:

- Java
- Spring Boot
- Spring Web
- Spring Data JPA
- Jakarta Bean Validation
- PostgreSQL
- MapStruct
- JUnit 5
- Mockito
- AssertJ
- JaCoCo

Do not add a new dependency when the current stack can solve the requirement adequately.

## Module Architecture

Every backend module must follow this flow:

```text
Entity
  -> DTOs
  -> MapStruct Mapper
  -> Repository
  -> Service
  -> Controller
  -> Validation
  -> Centralized exception handling
  -> Unit tests with JaCoCo coverage
```

Responsibilities must remain separated:

- **Entity:** Represents the persistence model and database relationships.
- **DTOs:** Define the API input and output contracts.
- **Mapper:** Converts between entities and DTOs using MapStruct.
- **Repository:** Performs persistence operations using Spring Data JPA.
- **Service:** Contains business rules and coordinates repositories and mappers.
- **Controller:** Exposes REST endpoints and delegates business operations to services.
- **Validation:** Validates request syntax through Jakarta Bean Validation and business rules in the service layer.
- **Exception handling:** Converts application exceptions into consistent HTTP error responses.
- **Tests:** Verify business behavior in isolation and protect the project's JaCoCo coverage requirements.

## General Implementation Rules

- Follow the package structure and naming conventions already present in the project.
- Keep classes focused on a single responsibility.
- Prefer constructor injection. Do not use field injection.
- Do not place business logic in controllers, repositories, entities, or mappers.
- Do not expose JPA entities directly through REST endpoints.
- Do not return request DTOs as response models.
- Avoid duplicated logic and unnecessary abstractions.
- Do not create interfaces or layers that provide no concrete value merely for architectural symmetry.
- Use English for class names, field names, database mappings, endpoint names, validation messages, comments, and documentation.
- Preserve the conventions of existing modules unless a module specification states otherwise.

## Entities and Persistence

- Map entities explicitly to their database tables and columns.
- Use `Integer` identifiers for small catalog entities when the database column uses `INTEGER`.
- Use `Long` only when the database schema or expected data volume requires `BIGINT`.
- Use timezone-aware Java types for `TIMESTAMP WITH TIME ZONE` columns.
- Respect database constraints in the JPA mapping, but do not rely exclusively on application validation for data integrity.
- Do not physically delete records when historical relationships must be preserved. Use logical activation or deactivation when the entity provides an `active` field.
- Do not silently add audit fields such as `createdBy` or `updatedBy`. Add them only when required by the module specification and supported by the database schema.
- Avoid changing the database schema from application code unless the requested task explicitly includes a migration.

## DTO Conventions

Use operation-specific DTOs when their responsibilities differ. Prefer names such as:

- `CreateXRequest`
- `UpdateXRequest`
- `XResponse`

Rules:

- Apply Jakarta Bean Validation annotations to request DTOs.
- Keep persistence annotations out of DTOs.
- Return only fields intended for API consumers.
- Do not accept generated or system-controlled fields from clients unless explicitly required.
- Keep validation constraints consistent with database limits and module business rules.

## MapStruct

- Use MapStruct for entity-to-DTO and DTO-to-entity conversions.
- Configure mappers as Spring components.
- Do not write repetitive manual mapping code when MapStruct can perform the mapping safely.
- Explicitly ignore generated, immutable, or system-controlled fields when mapping create requests.
- For updates, preserve fields that clients are not allowed to modify.
- Follow the MapStruct conventions already used by the Users module.

## Repositories

- Use Spring Data JPA repositories.
- Keep repositories limited to persistence concerns.
- Add derived queries or explicit queries only when required by a business use case.
- Use existence checks for uniqueness rules when appropriate.
- Do not implement business decisions inside repositories.

## Services

- Services contain the module's business rules.
- Validate uniqueness, existence, allowed state transitions, and other business constraints in the service layer.
- Use meaningful application exceptions for expected failures.
- Keep transactions at the service layer when an operation requires transactional consistency.
- Avoid swallowing exceptions or converting all failures into generic runtime exceptions.
- For logical deletion, update the entity state instead of calling a physical delete operation.

## Controllers

- Controllers must remain thin.
- Controllers must validate incoming DTOs and delegate to services.
- Use appropriate HTTP verbs and status codes.
- Do not catch business exceptions directly in controllers.
- Do not access repositories or mappers directly from controllers.
- Keep endpoint paths and response structures consistent across modules.

## Validation

Validation has two levels:

1. **Request validation:** Use Jakarta Bean Validation for required values, size limits, formats, and structurally invalid input.
2. **Business validation:** Use the service layer for uniqueness, resource existence, state transitions, and rules requiring persistence access.

Do not duplicate the same validation unnecessarily across multiple layers.

## Exception Handling

- Use the project's centralized exception handling strategy.
- Reuse existing exception classes when their meaning matches the failure.
- Prefer meaningful exceptions such as `ResourceNotFoundException` and `DuplicateResourceException` when those classes exist in the project.
- Return consistent error responses.
- Do not expose stack traces, SQL messages, or internal exception details through the API.
- Do not introduce module-specific exception handlers when the global handler can represent the error correctly.

## Testing Requirements

Every new module or behavior must include unit tests.

Use:

- JUnit 5
- Mockito
- AssertJ
- JaCoCo for coverage verification

At minimum, service tests must cover the applicable scenarios:

- Successful creation
- Successful lookup by identifier
- Successful listing
- Successful update
- Resource not found
- Duplicate or conflict conditions
- Business rule violations
- Activation and deactivation behavior
- Optional values and boundary cases

Testing rules:

- Mock repositories and mappers in service unit tests.
- Verify returned values and relevant dependency interactions.
- Do not test private methods directly.
- Do not create tests solely to execute lines without asserting behavior.
- Keep tests deterministic and independent.
- Follow the naming and structure established by the Users module tests.
- Run the full test suite after implementing a module.
- The implementation must satisfy the JaCoCo rules configured in `backend/pom.xml`.
- Do not weaken exclusions or coverage thresholds merely to make the build pass.

Controller tests should be added when they provide meaningful coverage of HTTP contracts, validation, serialization, status codes, or exception translation. Do not replace service unit tests with controller tests.

## Module Specifications

Module documents belong under:

```text
docs/modules/
```

Each module specification should describe only module-specific requirements, including:

- Purpose
- Database table and fields
- DTO requirements
- Business rules
- API endpoints
- Validation rules
- Expected error scenarios
- Module-specific test cases

Do not duplicate all global architectural rules inside each module document. Reference this file instead.

Examples:

```text
docs/modules/users.md
docs/modules/vehicles.md
docs/modules/payments.md
docs/modules/expenses.md
```

## Definition of Done

A module is complete only when:

- The implementation follows the project architecture.
- Entities are not exposed through the API.
- DTO mappings use MapStruct.
- Business rules are enforced in the service layer.
- Validation and errors are handled consistently.
- Unit tests cover successful and failure scenarios.
- All project tests pass.
- The JaCoCo verification configured in `backend/pom.xml` passes.
- No secrets, credentials, generated reports, build output, or local configuration are committed.
- The implementation matches its module specification.
