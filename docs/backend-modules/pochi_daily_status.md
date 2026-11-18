# Pochi Daily Status Module

## 1. Purpose

The `Pochi Daily Status` module records exceptional daily conditions that explain why a Pochi did not operate as a normal working day.

This module complements `Pochi Assignments`; it does not replace it.

- `pochi_assignments` records which Driver was assigned to a Pochi on a date and shift.
- `pochi_daily_status` records special daily conditions such as a day off, union rest, or a day when the Pochi did not work.
- Reports will later combine both sources to determine the visible daily activity.

The domain term must always be `Pochi` / `pochi`. Do not use `Vehicle` / `vehicle` in entities, DTOs, services, repositories, endpoints, tests, documentation, or messages.

---

## 2. Existing database table

The database table already exists. Do not create another table and do not rename its columns.

```sql
CREATE TABLE pochi_daily_status (
    id SERIAL PRIMARY KEY,

    pochi_id INTEGER NOT NULL,
    status_date DATE NOT NULL,

    status VARCHAR(20) NOT NULL,
    comment VARCHAR(500),

    created_by INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_by INTEGER,
    updated_at TIMESTAMP,

    CONSTRAINT fk_pochi_daily_status_pochi
        FOREIGN KEY (pochi_id)
        REFERENCES pochis(id),

    CONSTRAINT fk_pochi_daily_status_created_by
        FOREIGN KEY (created_by)
        REFERENCES users(id),

    CONSTRAINT fk_pochi_daily_status_updated_by
        FOREIGN KEY (updated_by)
        REFERENCES users(id),

    CONSTRAINT uq_pochi_daily_status_pochi_date
        UNIQUE (pochi_id, status_date),

    CONSTRAINT chk_pochi_daily_status_status
        CHECK (
            status IN (
                'DAY_OFF',
                'UNION_REST',
                'NOT_WORKED'
            )
        ),

    CONSTRAINT chk_pochi_daily_status_not_worked_comment
        CHECK (
            status <> 'NOT_WORKED'
            OR NULLIF(TRIM(comment), '') IS NOT NULL
        )
);

CREATE INDEX idx_pochi_daily_status_date
    ON pochi_daily_status(status_date);

CREATE INDEX idx_pochi_daily_status_status
    ON pochi_daily_status(status);
```

---

## 3. Persisted statuses

Create an enum named similarly to:

```java
public enum PochiDailyStatusType {
    DAY_OFF,
    UNION_REST,
    NOT_WORKED
}
```

Only these three values are persisted.

### 3.1 `DAY_OFF`

A rest day granted to a Driver for working the full week.

Business behavior:

- The system may or may not have a `PochiAssignment` for that date.
- In the real business process, the Pochi may remain at the Driver's home.
- The administrator may not know whether the Driver actually used the Pochi.
- An assignment is therefore optional and does not prove that the day must be financially processed.
- `DAY_OFF` never generates a Settlement.
- `DAY_OFF` never generates an Administration Charge.
- `comment` is optional.

If both a `DAY_OFF` status and an assignment exist for the same Pochi and date, the daily status prevails for financial behavior: no Settlement and no Administration Charge must be generated.

### 3.2 `UNION_REST`

A date on which the Pochi does not operate because the union established the rest day.

Business behavior:

- A `PochiAssignment` is not allowed for that Pochi and date.
- It does not generate a Settlement.
- It does not generate an Administration Charge.
- `comment` is optional.

### 3.3 `NOT_WORKED`

The Pochi did not work for an operational reason, for example:

- no Driver was available;
- the Pochi was in the workshop;
- the Pochi was detained by traffic authorities;
- the Pochi did not start;
- a mechanical failure occurred;
- another operational reason prevented work.

Business behavior:

- A `PochiAssignment` is not allowed for that Pochi and date.
- It does not generate a Settlement.
- It does not generate an Administration Charge.
- `comment` is mandatory and must explain why the Pochi was stopped.
- Null, empty, or whitespace-only comments are invalid.

The backend may display this state as `Parada` in Spanish-facing reports or screens, but the technical enum remains `NOT_WORKED`.

---

## 4. Derived daily activities

Do not persist `WORKED` or `PENDING` in `pochi_daily_status`.

They are derived concepts used later by Reports and Weekly Closing.

### `WORKED`

A day is derived as `WORKED` when:

- no `PochiDailyStatus` exists for the Pochi and date; and
- at least one `PochiAssignment` exists for the Pochi and date.

Interpretation:

- one assignment means one worked shift;
- `MORNING` or `AFTERNOON` alone can be displayed as one shift or half day;
- `MORNING` plus `AFTERNOON` means two shifts or full day.

### `PENDING`

A day is derived as `PENDING` when:

- no `PochiDailyStatus` exists for the Pochi and date; and
- no `PochiAssignment` exists for the Pochi and date.

`PENDING` means the administrator has not yet explained what happened on that day. It must not be inserted into the database.

---

## 5. Domain compatibility matrix

| Condition | Assignment allowed | Settlement allowed | Administration Charge allowed | Comment |
|---|---:|---:|---:|---|
| Normal worked day | Required to derive `WORKED` | Yes | Yes | Not applicable |
| `DAY_OFF` | Optional | No | No | Optional |
| `UNION_REST` | No | No | No | Optional |
| `NOT_WORKED` | No | No | No | Required |
| Derived `PENDING` | No assignment currently exists | No | No | Not persisted |

---

## 6. Cross-module validations

The database protects the allowed status values, one record per Pochi/date, and the mandatory comment for `NOT_WORKED`.

Cross-module rules must be validated in the Spring service layer. Do not implement database triggers.

### 6.1 When creating or updating a daily status

For `UNION_REST` or `NOT_WORKED`:

1. Search for existing `PochiAssignments` using `pochiId` and `statusDate`.
2. If at least one assignment exists, reject the operation with a clear business exception.

For `DAY_OFF`:

- Existing assignments are allowed.

When changing an existing record from `DAY_OFF` to `UNION_REST` or `NOT_WORKED`, perform the same assignment validation.

### 6.2 When creating or updating a Pochi Assignment

Update the `Pochi Assignments` service so that it checks whether a `PochiDailyStatus` already exists for the same Pochi and date.

- `DAY_OFF`: allow the assignment.
- `UNION_REST`: reject the assignment.
- `NOT_WORKED`: reject the assignment.
- no daily status: allow the assignment.

This validation must also apply when an existing assignment changes its Pochi or assignment date.

### 6.3 Settlements

Update the Settlement creation flow to reject a Settlement when the associated assignment belongs to a Pochi/date marked as:

- `DAY_OFF`;
- `UNION_REST`;
- `NOT_WORKED`.

`DAY_OFF` may contain an assignment, but it must never produce a Settlement.

### 6.4 Administration Charges

Update the Administration Charge creation/calculation flow so that a Pochi/date marked as any persisted daily status does not generate an Administration Charge:

- `DAY_OFF`;
- `UNION_REST`;
- `NOT_WORKED`.

Do not create zero-value Administration Charge rows. The absence of a charge represents that no administration amount applies for that date.

---

## 7. Module architecture

Follow the existing AdministraPochi layered architecture and package conventions. Reuse existing base classes, exception types, response formats, validation patterns, mapper configuration, pagination conventions, and auditing approach.

Expected components:

```text
pochi daily status package
├── controller
│   └── PochiDailyStatusController
├── dto
│   ├── PochiDailyStatusCreateRequest
│   ├── PochiDailyStatusUpdateRequest
│   └── PochiDailyStatusResponse
├── entity
│   └── PochiDailyStatus
├── enums
│   └── PochiDailyStatusType
├── mapper
│   └── PochiDailyStatusMapper
├── repository
│   └── PochiDailyStatusRepository
└── service
    ├── PochiDailyStatusService
    └── implementation following the existing project convention
```

Adapt names and package placement to the real repository structure. Do not create a competing architecture.

---

## 8. Entity requirements

Map the existing table exactly:

```text
id          -> Integer
pochi       -> Pochi relationship through pochi_id
statusDate  -> LocalDate
status      -> PochiDailyStatusType persisted as STRING
comment     -> String, maximum 500 characters
createdBy   -> User relationship through created_by
createdAt   -> LocalDateTime
updatedBy   -> User relationship through updated_by
updatedAt   -> LocalDateTime
```

Requirements:

- Use `@Enumerated(EnumType.STRING)` for `status`.
- Preserve the unique rule for `pochi_id + status_date`.
- Do not expose JPA entities directly from the controller.
- Avoid Lombok-generated equality methods that recursively traverse JPA relationships.
- Follow the auditing pattern already used by the project.
- The current project may still use `TEMPORARY_USER_ID = 1` while authentication is pending. Reuse the current established mechanism; do not generate authentication/security classes.

---

## 9. DTO requirements

### Create request

Suggested fields:

```java
Integer pochiId;
LocalDate statusDate;
PochiDailyStatusType status;
String comment;
```

Validation:

- `pochiId`: required.
- `statusDate`: required.
- `status`: required.
- `comment`: maximum 500 characters.
- Conditional `NOT_WORKED` comment validation belongs in the service or a reusable class-level validator.

### Update request

Allow updating only business-editable information:

```java
LocalDate statusDate;
PochiDailyStatusType status;
String comment;
```

Do not allow clients to update:

- `id`;
- `createdBy`;
- `createdAt`;
- `updatedBy`;
- `updatedAt`.

If the project convention treats the Pochi as editable, `pochiId` may be included, but all uniqueness and cross-module validations must run again. Prefer keeping the Pochi immutable unless existing modules establish another convention.

### Response

Return at least:

```java
Integer id;
Integer pochiId;
String pochiIdentifierOrDisplayName; // use an existing meaningful field, if available
LocalDate statusDate;
PochiDailyStatusType status;
String comment;
Integer createdBy;
LocalDateTime createdAt;
Integer updatedBy;
LocalDateTime updatedAt;
```

Match actual DTO naming conventions in the codebase.

---

## 10. Repository requirements

Include repository operations required for validation and filtering, such as:

```java
boolean existsByPochiIdAndStatusDate(Integer pochiId, LocalDate statusDate);

boolean existsByPochiIdAndStatusDateAndIdNot(
    Integer pochiId,
    LocalDate statusDate,
    Integer id
);

Optional<PochiDailyStatus> findByPochiIdAndStatusDate(
    Integer pochiId,
    LocalDate statusDate
);
```

Add filtered queries or specifications consistent with the repository architecture for:

- `pochiId`;
- `status`;
- exact `statusDate`;
- `dateFrom` and `dateTo`.

Do not load all rows into memory to filter them in Java.

---

## 11. Service operations

Implement operations consistent with the current modules:

- create;
- find by ID;
- find all with filters and pagination if pagination is the project convention;
- update;
- delete only if deletion is consistent with the current project approach.

Recommended filters:

```text
pochiId
status
dateFrom
dateTo
```

### Create validation sequence

1. Validate that the Pochi exists.
2. Normalize `comment` by trimming it; convert empty optional comments to `null` if consistent with project conventions.
3. Validate the `NOT_WORKED` comment.
4. Validate uniqueness of Pochi + date.
5. For `UNION_REST` or `NOT_WORKED`, verify that no assignments exist.
6. Set audit values using the existing current-user approach.
7. Save and map the response.

### Update validation sequence

1. Load the existing record or throw the project not-found exception.
2. Normalize the comment.
3. Validate the `NOT_WORKED` comment.
4. Revalidate uniqueness when the date or Pochi can change.
5. Revalidate assignment compatibility for the resulting status.
6. Preserve creation audit fields.
7. Set update audit fields.
8. Save and return the mapped response.

---

## 12. Suggested REST API

Follow the API naming conventions already present in AdministraPochi. A reasonable default is:

```text
POST   /api/pochi-daily-statuses
GET    /api/pochi-daily-statuses/{id}
GET    /api/pochi-daily-statuses
PUT    /api/pochi-daily-statuses/{id}
DELETE /api/pochi-daily-statuses/{id}
```

Suggested query examples:

```text
GET /api/pochi-daily-statuses?pochiId=82
GET /api/pochi-daily-statuses?status=NOT_WORKED
GET /api/pochi-daily-statuses?dateFrom=2026-10-19&dateTo=2026-10-25
GET /api/pochi-daily-statuses?pochiId=82&dateFrom=2026-10-19&dateTo=2026-10-25
```

If the existing project uses another pluralization or base path convention, follow the existing convention.

---

## 13. Error behavior

Use clear domain messages consistent with the global exception handler.

Examples:

```text
Pochi daily status not found.
A daily status already exists for this Pochi and date.
A comment is required when the status is NOT_WORKED.
UNION_REST cannot be registered because assignments already exist for this Pochi and date.
NOT_WORKED cannot be registered because assignments already exist for this Pochi and date.
An assignment cannot be created because the Pochi is marked as UNION_REST for this date.
An assignment cannot be created because the Pochi is marked as NOT_WORKED for this date.
A Settlement cannot be created for a date marked as DAY_OFF.
An Administration Charge cannot be created for a date marked as DAY_OFF.
```

Do not leak database constraint names as the primary API error message.

---

## 14. Required tests

Create unit tests using the existing project testing stack and conventions.

### Service tests

At minimum, cover:

1. create `DAY_OFF` without comment;
2. create `DAY_OFF` when an assignment exists;
3. create `UNION_REST` without assignments;
4. reject `UNION_REST` when an assignment exists;
5. create `NOT_WORKED` with a valid comment;
6. reject `NOT_WORKED` with null comment;
7. reject `NOT_WORKED` with empty comment;
8. reject `NOT_WORKED` with whitespace-only comment;
9. reject `NOT_WORKED` when an assignment exists;
10. reject duplicate Pochi + date;
11. update `DAY_OFF` to `NOT_WORKED` with a valid comment;
12. reject update to `NOT_WORKED` without a comment;
13. reject update to `UNION_REST` when assignments exist;
14. find by ID;
15. return not found for an unknown ID;
16. filter by Pochi;
17. filter by status;
18. filter by date range.

### Pochi Assignment integration/service tests

Cover:

1. assignment allowed with no daily status;
2. assignment allowed with `DAY_OFF`;
3. assignment rejected with `UNION_REST`;
4. assignment rejected with `NOT_WORKED`;
5. update rejected when moving an assignment to a conflicting Pochi/date.

### Settlement tests

Cover rejection for assignments whose Pochi/date is marked as:

- `DAY_OFF`;
- `UNION_REST`;
- `NOT_WORKED`.

### Administration Charge tests

Cover that no charge is created or calculated for:

- `DAY_OFF`;
- `UNION_REST`;
- `NOT_WORKED`.

---

## 15. Out of scope

Do not implement the following as part of this module:

- Reports;
- weekly report calculations;
- weekly closures;
- `OPEN`, `ADMIN_CLOSED`, or `OWNER_CLOSED`;
- a reason catalog for `NOT_WORKED`;
- automatic Expense creation;
- automatic zero-value Settlements;
- automatic zero-value Administration Charges;
- authentication or authorization infrastructure;
- database triggers;
- a persisted `WORKED` or `PENDING` status.

These concerns will be handled separately.

---

## 16. Acceptance criteria

The module is complete when:

1. the existing `pochi_daily_status` table is mapped correctly;
2. only `DAY_OFF`, `UNION_REST`, and `NOT_WORKED` are persisted;
3. `NOT_WORKED` always requires a meaningful comment;
4. only one status can exist per Pochi/date;
5. `DAY_OFF` permits an optional assignment but never permits financial generation;
6. `UNION_REST` and `NOT_WORKED` reject existing or new assignments;
7. Settlements are blocked for all three persisted statuses;
8. Administration Charges are blocked for all three persisted statuses;
9. no zero-value financial rows are generated;
10. filters operate at repository/database level;
11. DTOs, mapper, service, controller, validations, exception handling, and tests follow the current AdministraPochi architecture;
12. no unintended authentication/security classes are introduced;
13. all existing tests continue passing.

---

## 17. Implementation order for Copilot

Implement in this order:

1. inspect the current project architecture and analogous completed modules;
2. create the enum;
3. map the entity to the existing table;
4. create DTOs and mapper;
5. create repository methods;
6. create service validations and CRUD/query operations;
7. create controller endpoints;
8. add validation to `Pochi Assignments`;
9. add validation to Settlements;
10. add validation to Administration Charges;
11. add or update unit tests;
12. run the complete test suite;
13. report the files created/modified, validations implemented, and test results.

Do not change unrelated modules or database structures.

---

# Prompt to give GitHub Copilot

Implement the **Pochi Daily Status** module using the requirements in this document.

Before writing code, inspect the existing AdministraPochi project and identify the completed module that best represents the current architecture for entities, DTOs, MapStruct mappers, repositories, services, controllers, validations, exception handling, filters, auditing, and unit tests. Follow that architecture instead of introducing new patterns.

Important rules:

- The database table already exists. Do not create or modify SQL unless an actual mapping mismatch is found and reported first.
- Use only the persisted statuses `DAY_OFF`, `UNION_REST`, and `NOT_WORKED`.
- Do not persist `WORKED` or `PENDING`.
- `NOT_WORKED` requires a non-empty comment.
- `DAY_OFF` may coexist with an assignment, but it must never generate a Settlement or Administration Charge.
- `UNION_REST` and `NOT_WORKED` cannot coexist with assignments.
- Add validation in both directions: daily-status creation/update and assignment creation/update.
- Block Settlements and Administration Charges for all three persisted statuses.
- Do not create zero-value financial records.
- Reuse the current user/auditing approach, including the existing temporary user mechanism if authentication is not implemented.
- Do not generate authentication or security classes.
- Always use `Pochi` / `pochi`; never use `Vehicle` / `vehicle`.
- Add comprehensive tests and run the complete test suite.

Work autonomously through the complete implementation. When finished, provide:

1. a summary of the implementation;
2. the list of files created and modified;
3. the business validations added to each affected module;
4. the test cases added;
5. the final test results;
6. any conflict found between this document and the current codebase.
