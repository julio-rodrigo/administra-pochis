# Pochi Assignments Module

## 1. Purpose

The **Pochi Assignments** module records the daily relationship between a Driver, a Pochi, a work shift, and a specific date.

Its purpose is to preserve operational history for future use by the **Settlements**, administrative closing, and reporting modules.

The module must answer:

- Which Driver worked a Pochi.
- On which date the Driver worked.
- Which shift the Driver worked.
- Whether the same Driver worked both shifts.
- Which assignment may later generate a settlement obligation.

Use the term **Pochi Assignment**, in class names, DTOs, services, endpoints, validation messages, and documentation.

---

## 2. Architectural Decision

Implement Pochi Assignments as an independent, lightweight module with its own endpoints and business rules.

Do not place assignment creation exclusively inside the Settlements module.

Responsibilities remain separated:

- **Pochi Assignments:** records who worked which Pochi, on which date, and during which shift.
- **Settlements:** records financial obligations, payments, balances, statuses, and settlement calculations.
- **Administrative Closures:** will later define date ranges whose operational and financial records are immutable.

This is not an unrestricted CRUD. It is a controlled historical-record workflow:

- Create assignments while the corresponding administrative period is open.
- Create a full-day assignment as two shift records in one transaction.
- Retrieve and filter assignment history.
- Correct assignments only while their administrative period is open.
- Delete erroneous assignments only while their administrative period is open and before another module references them.
- Never hard-delete an assignment referenced by a settlement or another business record.

---

## 3. Existing Project Structure

Use the current package organization under `com.administrapochi`:

```text
src/main/java/com/administrapochi/
├── configurations/
├── controllers/
├── exceptions/
├── models/
│   ├── dtos/
│   ├── entities/
│   ├── enums/
│   └── mappers/
├── repositories/
├── services/
└── AdministrapochiApplication.java
```

Create the module classes in the corresponding existing folders. Do not create a separate `pochiassignment` root package and do not create module-specific `controller`, `repository`, or `service` subfolders.

Suggested classes:

```text
controllers/
└── PochiAssignmentController.java

models/dtos/
├── PochiAssignmentCreateRequest.java
├── PochiAssignmentUpdateRequest.java
├── PochiAssignmentResponse.java
└── PochiAssignmentFilter.java          optional

models/entities/
└── PochiAssignment.java

models/enums/
├── Shift.java
└── AssignmentShiftOption.java

models/mappers/
└── PochiAssignmentMapper.java

repositories/
└── PochiAssignmentRepository.java

services/
├── PochiAssignmentService.java
└── PochiAssignmentServiceImpl.java
```

Reuse the shared classes and conventions already present in `configurations` and `exceptions`.

---

## 4. Database Table

The table is named `pochi_assignments` and references `pochis`.

Expected final schema:

```sql
CREATE TABLE pochi_assignments (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    assignment_date DATE NOT NULL,
    pochi_id INTEGER NOT NULL,
    driver_id INTEGER NOT NULL,
    shift VARCHAR(20) NOT NULL,
    notes VARCHAR(500),

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by INTEGER,
    updated_by INTEGER,

    CONSTRAINT fk_pochi_assignments_pochi
        FOREIGN KEY (pochi_id)
        REFERENCES pochis(id),

    CONSTRAINT fk_pochi_assignments_driver
        FOREIGN KEY (driver_id)
        REFERENCES drivers(id),

    CONSTRAINT fk_pochi_assignments_created_by
        FOREIGN KEY (created_by)
        REFERENCES users(id),

    CONSTRAINT fk_pochi_assignments_updated_by
        FOREIGN KEY (updated_by)
        REFERENCES users(id),

    CONSTRAINT chk_pochi_assignments_shift
        CHECK (shift IN ('MORNING', 'AFTERNOON')),

    CONSTRAINT uq_pochi_assignment_date_pochi_shift
        UNIQUE (assignment_date, pochi_id, shift),

    CONSTRAINT uq_pochi_assignment_date_driver_shift
        UNIQUE (assignment_date, driver_id, shift)
);
```

Existing indexes:

```sql
CREATE INDEX idx_pochi_assignments_date
    ON pochi_assignments (assignment_date);

CREATE INDEX idx_pochi_assignments_pochi
    ON pochi_assignments (pochi_id);

CREATE INDEX idx_pochi_assignments_driver
    ON pochi_assignments (driver_id);

CREATE INDEX idx_pochi_assignments_driver_date
    ON pochi_assignments (driver_id, assignment_date);
```

If the table was already created without user audit columns, apply this alteration before implementing the entity fields:

```sql
ALTER TABLE pochi_assignments
    ADD COLUMN created_by INTEGER,
    ADD COLUMN updated_by INTEGER;

ALTER TABLE pochi_assignments
    ADD CONSTRAINT fk_pochi_assignments_created_by
        FOREIGN KEY (created_by) REFERENCES users(id),
    ADD CONSTRAINT fk_pochi_assignments_updated_by
        FOREIGN KEY (updated_by) REFERENCES users(id);
```

Keep `created_by` and `updated_by` nullable until authentication provides the current application user reliably. Never hardcode a user ID.

Do not generate a migration that recreates the table unless the project already uses a migration tool and the migration is required to represent the existing schema.

---

## 5. Domain Rules

### 5.1 Persisted shifts

Only these values may be stored:

```text
MORNING
AFTERNOON
```

Do not persist `FULL_DAY`.

A full-day assignment is stored as two records for the same date, Pochi, and Driver:

```text
2026-11-10 | Driver 3 | Pochi 82 | MORNING
2026-11-10 | Driver 3 | Pochi 82 | AFTERNOON
```

The API may accept `FULL_DAY` as a request option, but the service must create both persisted records within one transaction.

### 5.2 One Driver per Pochi, date, and shift

A Pochi cannot have two Drivers assigned to the same shift on the same date.

Protected by:

```text
UNIQUE (assignment_date, pochi_id, shift)
```

### 5.3 One Pochi per Driver, date, and shift

A Driver cannot work two Pochis during the same shift on the same date.

Protected by:

```text
UNIQUE (assignment_date, driver_id, shift)
```

### 5.4 Different Pochis in different shifts are allowed

Valid example:

```text
2026-11-10 | Driver 1 | MORNING   | Pochi 16
2026-11-10 | Driver 1 | AFTERNOON | Pochi 82
```

### 5.5 Two Drivers on one Pochi are allowed when shifts differ

Valid example:

```text
2026-11-10 | Pochi 16 | MORNING   | Driver 1
2026-11-10 | Pochi 16 | AFTERNOON | Driver 2
```

### 5.6 Driver and Pochi validation

Before creating or updating an assignment:

- Verify that the Driver exists.
- Verify that the Pochi exists in `pochis`.
- Reject inactive Drivers or Pochis if those modules expose active status.
- Do not silently replace an existing assignment.
- Return a business conflict for a date, shift, Driver, or Pochi collision.

### 5.7 Historical responsibility

Assignments are historical records. Do not automatically modify previous assignments when Driver or Pochi information changes.

Do not add settlement amounts, payment statuses, rest days, accidents, repairs, expenses, or maintenance information to this entity.

---

## 6. Administrative Period Locking

Assignments are editable only while the administrative period containing `assignmentDate` remains open.

Example:

```text
Closed period: 2026-10-01 through 2026-10-31
```

After that period is closed, assignments dated within the range must be queryable but immutable.

The future rule applies to all write operations:

```text
CREATE + date inside CLOSED period -> reject
UPDATE + date inside CLOSED period -> reject
DELETE + date inside CLOSED period -> reject
READ or REPORT + date inside CLOSED period -> allow
```

A change of date during an update must validate both sides:

- Reject if the original assignment date is inside a closed period.
- Reject if the requested new assignment date is inside a closed period.

Do not add an `is_locked` column to `pochi_assignments`. Locking belongs to a future administrative closure entity that represents a date range and can protect assignments, settlements, expenses, and related records consistently.

The exact closure scope is not implemented in this module yet because the later Settlements design must determine whether a closure is global, per Owner, per Pochi, or another business scope.

For the current implementation:

- Centralize write authorization in a reusable service-level method or policy, such as `validatePeriodIsOpen(LocalDate date)`.
- Do not duplicate closure checks in controllers.
- Provide a temporary implementation that allows writes while no closure module exists.
- Mark the temporary implementation clearly for replacement when Administrative Closures is implemented.
- Do not create a speculative `administrative_closures` table as part of this module.
- Add tests that verify the service calls the period-lock policy before create, update, and delete operations.

---

## 7. Entity

Create `PochiAssignment` mapped to `pochi_assignments`.

Recommended fields:

```java
Integer id;
LocalDate assignmentDate;
Pochi pochi;
Driver driver;
Shift shift;
String notes;
LocalDateTime createdAt;
LocalDateTime updatedAt;
User createdBy;
User updatedBy;
```

Requirements:

- Use `@ManyToOne(fetch = FetchType.LAZY)` for Pochi, Driver, `createdBy`, and `updatedBy`.
- Map the Pochi relationship through `pochi_id`.
- Map the Driver relationship through `driver_id`.
- Map audit users through `created_by` and `updated_by`.
- Do not cascade persistence from assignments to Pochis, Drivers, or Users.
- Use `@Enumerated(EnumType.STRING)` for `Shift`.
- Use `LocalDate` for `assignmentDate`.
- Use `LocalDateTime` for audit timestamps.
- Never expose the entity directly from controllers.
- Do not add `FULL_DAY` to the persisted `Shift` enum.

Persisted enum:

```java
public enum Shift {
    MORNING,
    AFTERNOON
}
```

Request-level enum:

```java
public enum AssignmentShiftOption {
    MORNING,
    AFTERNOON,
    FULL_DAY
}
```

---

## 8. DTOs

### 8.1 PochiAssignmentCreateRequest

```java
@NotNull LocalDate assignmentDate;
@NotNull Integer pochiId;
@NotNull Integer driverId;
@NotNull AssignmentShiftOption shift;
@Size(max = 500) String notes;
```

Do not accept `createdAt`, `updatedAt`, `createdBy`, or `updatedBy` from the client.

### 8.2 PochiAssignmentUpdateRequest

```java
@NotNull LocalDate assignmentDate;
@NotNull Integer pochiId;
@NotNull Integer driverId;
@NotNull Shift shift;
@Size(max = 500) String notes;
```

A single-record update accepts only `MORNING` or `AFTERNOON`. Do not transform a single assignment into `FULL_DAY` through this endpoint.

Do not accept audit fields from the client.

### 8.3 PochiAssignmentResponse

Recommended fields:

```java
Integer id;
LocalDate assignmentDate;
String shift;
Integer pochiId;
String pochiIdentifier;
Integer driverId;
String driverName;
String notes;
LocalDateTime createdAt;
LocalDateTime updatedAt;
Integer createdById;
String createdByName;
Integer updatedById;
String updatedByName;
```

Use actual display properties already available in the Pochi, Driver, and User entities. Do not invent nonexistent fields.

---

## 9. Mapper

Create `PochiAssignmentMapper` using MapStruct and the project conventions.

Responsibilities:

- Map `PochiAssignment` to `PochiAssignmentResponse`.
- Flatten nested Pochi, Driver, and audit-user identifiers into the response.
- Do not load or create entities in the mapper.
- Resolve Pochi, Driver, and authenticated User references in the service or auditing infrastructure.
- Ignore ID and audit fields when mapping requests.

---

## 10. Repository

Create:

```java
PochiAssignmentRepository extends
    JpaRepository<PochiAssignment, Integer>,
    JpaSpecificationExecutor<PochiAssignment>
```

Recommended methods:

```java
boolean existsByAssignmentDateAndPochiIdAndShift(
    LocalDate assignmentDate, Integer pochiId, Shift shift);

boolean existsByAssignmentDateAndDriverIdAndShift(
    LocalDate assignmentDate, Integer driverId, Shift shift);

boolean existsByAssignmentDateAndPochiIdAndShiftAndIdNot(
    LocalDate assignmentDate, Integer pochiId, Shift shift, Integer id);

boolean existsByAssignmentDateAndDriverIdAndShiftAndIdNot(
    LocalDate assignmentDate, Integer driverId, Shift shift, Integer id);
```

Use pagination for general history queries. Support optional filters for:

- Exact date.
- Start date.
- End date.
- Driver ID.
- Pochi ID.
- Shift.

Weekly and multi-week reporting must be supported through inclusive `startDate` and `endDate` filters. Do not hardcode seven-day or four-week ranges in the repository.

---

## 11. Service

Create:

```java
PochiAssignmentService
PochiAssignmentServiceImpl
```

Recommended operations:

```java
List<PochiAssignmentResponse> create(PochiAssignmentCreateRequest request);

PochiAssignmentResponse findById(Integer id);

Page<PochiAssignmentResponse> findAll(
    LocalDate startDate,
    LocalDate endDate,
    Integer driverId,
    Integer pochiId,
    Shift shift,
    Pageable pageable
);

PochiAssignmentResponse update(
    Integer id,
    PochiAssignmentUpdateRequest request
);

void delete(Integer id);
```

The `create` operation may return one record for a single shift or two records for `FULL_DAY`.

### 11.1 Create

The service must:

1. Validate that the requested date is in an open period.
2. Validate Driver and Pochi existence and status.
3. Validate Driver and Pochi availability for each requested shift.
4. Resolve the authenticated User for audit fields when authentication is available.
5. Save one assignment or both full-day assignments.
6. Convert database uniqueness violations into a clear `409 Conflict`.

Use `@Transactional` so a full-day request creates both records atomically.

### 11.2 Update

Updates exist only to correct captured historical information.

Before saving:

1. Load the assignment.
2. Validate that the original assignment date is in an open period.
3. Validate that the requested new date is in an open period.
4. Validate Driver and Pochi.
5. Check collisions while excluding the current ID.
6. Set `updatedBy` from the authenticated User when available.
7. Update `updatedAt` through auditing or lifecycle callbacks.
8. Reject if a settlement or other immutable business record references the assignment.

### 11.3 Delete

Before deleting:

1. Load the assignment.
2. Validate that its date is in an open period.
3. Verify that no settlement or other business record references it.
4. Delete only if it was an erroneous capture.

Never cascade deletion to Pochi, Driver, or User.

---

## 12. Audit Fields

Use:

```text
created_at
updated_at
created_by
updated_by
```

These fields provide basic accountability for records that influence settlements.

Rules:

- `createdAt` is assigned once and never changed.
- `updatedAt` changes on every valid update.
- `createdBy` is assigned once and never replaced.
- `updatedBy` identifies the latest User who changed the record.
- API clients must never supply audit fields.
- Never hardcode a User ID such as `1`.
- Prefer Spring Data JPA auditing with `AuditorAware<User>` when authentication is available.
- Until authentication is available, nullable `created_by` and `updated_by` are acceptable.

Do not introduce Hibernate Envers or a full revision table in this module unless the project later requires field-by-field change history.

---

## 13. REST API

Base path:

```text
/api/pochi-assignments
```

Endpoints:

```http
POST   /api/pochi-assignments
GET    /api/pochi-assignments/{id}
GET    /api/pochi-assignments
PUT    /api/pochi-assignments/{id}
DELETE /api/pochi-assignments/{id}
```

Create example:

```json
{
  "assignmentDate": "2026-11-10",
  "pochiId": 82,
  "driverId": 3,
  "shift": "FULL_DAY",
  "notes": null
}
```

History examples:

```http
GET /api/pochi-assignments?startDate=2026-10-05&endDate=2026-10-11
GET /api/pochi-assignments?startDate=2026-10-01&endDate=2026-10-31
GET /api/pochi-assignments?driverId=3&startDate=2026-10-01&endDate=2026-10-31
GET /api/pochi-assignments?pochiId=82&startDate=2026-10-01&endDate=2026-10-31
```

Closed periods remain available through GET endpoints and reports.

Recommended statuses:

- `201 Created`: assignment or full-day pair created.
- `200 OK`: retrieved or updated.
- `204 No Content`: erroneous and unreferenced assignment deleted.
- `400 Bad Request`: invalid request.
- `404 Not Found`: assignment, Driver, or Pochi not found.
- `409 Conflict`: scheduling conflict, referenced assignment, or closed administrative period.

---

## 14. Exception Handling

Reuse shared exception types from `com.administrapochi.exceptions`.

Expected errors:

```text
Pochi assignment not found.
Driver not found.
Pochi not found.
The Pochi already has a Driver assigned for the selected date and shift.
The Driver is already assigned to another Pochi for the selected date and shift.
A full-day assignment cannot be created because one or both shifts are unavailable.
The assignment cannot be modified because its administrative period is closed.
The assignment cannot be deleted because its administrative period is closed.
The assignment cannot be deleted because it is referenced by a settlement.
```

Do not expose PostgreSQL constraint names or stack traces.

---

## 15. Tests

At minimum, cover:

1. Create MORNING successfully.
2. Create AFTERNOON successfully.
3. Create FULL_DAY as two records in one transaction.
4. Roll back FULL_DAY if either shift is unavailable.
5. Reject two Drivers for the same Pochi, date, and shift.
6. Reject one Driver in two Pochis for the same date and shift.
7. Allow one Driver in different Pochis when shifts differ.
8. Allow two Drivers on one Pochi when shifts differ.
9. Reject unknown or inactive Driver.
10. Reject unknown or inactive Pochi.
11. Query by weekly date range.
12. Query by multi-week or monthly date range.
13. Update while excluding the current ID from collision checks.
14. Reject create inside a closed period through the period-lock policy.
15. Reject update when the original or requested date is closed.
16. Reject delete inside a closed period.
17. Allow read/report operations for closed periods.
18. Verify a full-day operation uses one transaction.
19. Verify audit fields are not accepted from request JSON.
20. Verify uniqueness constraints in repository integration tests.

Mocks may be used for the future period-lock policy until Administrative Closures is implemented.

---

## 16. Out of Scope

Do not implement these concepts here:

- Settlement amounts or payment tracking.
- Current balance or settlement status.
- Weekly settlement calculations.
- Union rest or compensation days.
- Accidents, repairs, or lack of operator.
- Expenses or maintenance payments.
- Report generation.
- The final Administrative Closure entity or table.

The module must only support date-range queries needed later by reports.

---

## 17. Future Integration

### 17.1 Settlements

Settlements should reference the assignment ID where practical instead of duplicating Driver, Pochi, date, and shift without a relationship.

Once referenced, the assignment becomes immutable even if its period is otherwise open.

### 17.2 Administrative Closures

A future closure module will represent an administrative reconciliation between an Owner and an Admin over a date range.

The closure will protect assignments and related financial records against retroactive changes while keeping them available for weekly, multi-week, monthly, and historical reports.

Do not use an `is_locked` flag on individual assignments.

---

## 18. Implementation Order

1. Apply the optional audit-column SQL if not already applied.
2. Create persisted and request-level shift enums.
3. Create the JPA entity.
4. Create DTOs and validation.
5. Create the repository and specifications.
6. Create the MapStruct mapper.
7. Create or prepare the period-lock policy abstraction.
8. Create the service interface.
9. Create the transactional service implementation.
10. Create the controller.
11. Integrate shared exception handling.
12. Add audit handling without hardcoded users.
13. Create unit, repository integration, and controller tests.
14. Run the complete test suite.

Preserve all conventions already established by the Users, Pochis, and Drivers modules.
