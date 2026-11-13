# Administration Charges Module

## 1. Objective

Implement the **Administration Charges** module for AdministraPochi.

An Administration Charge is the immutable historical record of the administration amount actually charged for one **Pochi on one date**. The charge is calculated from the Pochi Assignments and Settlements registered for that date, using the applicable Administration Fee Rule as a suggested amount.

The Administration Fee Rule is a suggestion. The administrator may provide a different final amount, but must explain the adjustment in `note`.

Use the existing project architecture, conventions, package structure, exception handling, DTO patterns, MapStruct configuration, pagination/filtering style, auditing approach, and testing conventions already implemented in the other AdministraPochi modules.

Always use the domain term **Pochi**. Do not use `Vehicle` or `vehicle` in class names, fields, endpoints, messages, documentation, tests, or variables.

---

## 2. Database Model

The database tables already exist. Do not create or modify the schema from the application.

### 2.1 `administration_charges`

| Column | Type | Rules |
|---|---|---|
| `id` | `INTEGER` | Primary key |
| `pochi_id` | `INTEGER` | Required FK to `pochis(id)` |
| `charge_date` | `DATE` | Required |
| `administration_fee_rule_id` | `INTEGER` | Required FK to `administration_fee_rules(id)` |
| `shift_count` | `SMALLINT` | Required; only `1` or `2` |
| `suggested_amount` | `NUMERIC(10,2)` | Required; greater than zero |
| `amount` | `NUMERIC(10,2)` | Required; greater than zero |
| `note` | `TEXT` | Required when `amount` differs from `suggested_amount` |
| `created_by` | `INTEGER` | Required FK to `users(id)` |
| `created_at` | `TIMESTAMP` | Required; defaults to current timestamp |

Database constraints already enforce:

- Only one Administration Charge per `pochi_id + charge_date`.
- `shift_count` must be `1` or `2`.
- `suggested_amount` and `amount` must be greater than zero.
- A nonblank `note` is mandatory when `amount != suggested_amount`.

### 2.2 `administration_charge_settlements`

| Column | Type | Rules |
|---|---|---|
| `administration_charge_id` | Integer-compatible FK | FK to `administration_charges(id)` |
| `settlement_id` | `INTEGER` | FK to `settlements(id)` |

Database constraints already enforce:

- Composite primary key over `administration_charge_id + settlement_id`.
- A Settlement can participate in only one Administration Charge because `settlement_id` is unique.

Map this association according to the project's current JPA conventions. Prefer an explicit join entity if that is consistent with the codebase and makes existence checks easier. Do not expose the join entity as an independent CRUD resource.

---

## 3. Core Business Rules

### 3.1 Unique daily charge

Only one Administration Charge may exist for a given:

```text
Pochi + charge date
```

Before creating a charge, verify that one does not already exist. Also retain the database unique constraint as the final concurrency safeguard.

### 3.2 Charge date

`chargeDate` represents the date of the Pochi Assignments being charged.

The applicable Administration Fee Rule must be resolved using `chargeDate`, not `createdAt` and not the current date.

All Assignments used to create the charge must have:

- the requested Pochi;
- `assignmentDate == chargeDate`.

### 3.3 Eligible Assignments and Settlements

Load every Pochi Assignment for the requested `pochiId + chargeDate`.

Creation must fail when:

- no Assignment exists for that Pochi and date;
- more than two Assignments exist;
- the Assignments contain duplicate shifts;
- any Assignment does not have its corresponding Settlement;
- a related Settlement already participates in another Administration Charge.

Allowed scenarios:

#### One-shift charge

Exactly one Assignment exists, either `MORNING` or `AFTERNOON`, and it has a Settlement.

```text
shiftCount = 1
```

#### Two-shift charge

Exactly two Assignments exist, one `MORNING` and one `AFTERNOON`, and both have their corresponding Settlements.

```text
shiftCount = 2
```

If two Assignments exist but only one has been settled, the charge is not eligible and must not be created.

Never accept Settlement IDs from the client as the source of truth. The backend must derive the Settlements from the Assignments found for the requested Pochi and date.

### 3.4 Applicable Administration Fee Rule

Resolve the rule already defined by the Administration Fee Rules module.

Use this precedence:

1. Applicable Pochi-specific rule for `pochiId` and `chargeDate`.
2. Applicable GLOBAL rule for `chargeDate`, only when no applicable Pochi-specific rule exists.
3. Reject charge creation when no applicable rule exists.

Reuse the Administration Fee Rules service or domain logic. Do not duplicate its rule-resolution algorithm inside this module.

The number of shifts determines which amount from the rule is suggested:

- one eligible shift: use the rule's one-shift amount;
- two eligible shifts: use the rule's two-shift amount.

Use the exact field names already implemented in `AdministrationFeeRule`; do not invent duplicate rule fields.

### 3.5 Suggested and actual amount

`suggestedAmount` is always calculated by the backend from the applicable rule.

The creation request may optionally include `amount`:

- If `amount` is absent, save `amount = suggestedAmount`.
- If `amount` is present, save the supplied value as the actual amount.
- The supplied `amount` must be greater than zero.
- The client must never send `suggestedAmount`.

When the final `amount` differs numerically from `suggestedAmount`, `note` is mandatory and must contain nonblank text.

Use `BigDecimal.compareTo`, not `BigDecimal.equals`, when comparing monetary values:

```java
amount.compareTo(suggestedAmount) != 0
```

This avoids treating `70.0` and `70.00` as different amounts.

When `amount` equals `suggestedAmount`, `note` is optional. If a note is supplied, trim it before persistence.

Example:

```text
Pochi: 35
Charge date: 2026-11-13
Eligible shifts: 2
Suggested amount: 70.00
Actual amount: 60.00
Note: Pochi did not work the complete afternoon shift.
```

### 3.6 Audit fields

`createdBy` identifies the authenticated user who created the charge. Do not accept `createdBy` from the request body.

Resolve it using the same authenticated-user mechanism used by the existing modules. Until the final security integration is enabled, reuse the project's current temporary authenticated-user provider or auditing strategy. Do not hardcode a new user ID inside this module.

`createdAt` is generated automatically and is read-only.

### 3.7 Immutability

An Administration Charge is an immutable historical record.

Implement:

- create;
- find by ID;
- paginated/filterable listing.

Do not implement:

- update;
- partial update;
- delete.

Do not expose `PUT`, `PATCH`, or `DELETE` endpoints for Administration Charges.

### 3.8 Lock related records

After a Settlement participates in an Administration Charge:

- the Settlement cannot be updated;
- the Settlement cannot be deleted;
- its Pochi Assignment cannot be updated;
- its Pochi Assignment cannot be deleted.

Additionally, after an Administration Charge exists for a Pochi and date, no new Pochi Assignment may be created for that same Pochi and date.

Integrate these checks into the existing Settlement and Pochi Assignment services. Use repository existence queries rather than loading entire object graphs only to check whether a charge exists.

Suggested repository operations include equivalents of:

```java
boolean existsBySettlementId(Integer settlementId);

boolean existsBySettlementPochiAssignmentId(Integer assignmentId);

boolean existsByPochiIdAndChargeDate(Integer pochiId, LocalDate chargeDate);
```

Adapt method names to the actual entity relationships in the project.

---

## 4. Transaction and Concurrency Requirements

Charge creation must run inside a single `@Transactional` service operation.

The transaction must include:

1. duplicate-charge validation;
2. Pochi lookup;
3. Assignment lookup;
4. Settlement eligibility validation;
5. applicable-rule resolution;
6. suggested-amount calculation;
7. actual-amount and note validation;
8. Administration Charge persistence;
9. association of all eligible Settlements.

If any step fails, neither the charge nor its Settlement associations may remain persisted.

Handle the database unique constraints as concurrency safeguards. Translate relevant `DataIntegrityViolationException` cases into the project's standard conflict/business exception instead of returning an unhandled HTTP 500.

---

## 5. Suggested Package Structure

Follow the existing project's package names. The module should contain equivalents of:

```text
administrationcharge/
├── controller/
│   └── AdministrationChargeController.java
├── dto/
│   ├── AdministrationChargeCreateRequest.java
│   ├── AdministrationChargeResponse.java
│   └── AdministrationChargeSummaryResponse.java
├── entity/
│   ├── AdministrationCharge.java
│   └── AdministrationChargeSettlement.java
├── mapper/
│   └── AdministrationChargeMapper.java
├── repository/
│   ├── AdministrationChargeRepository.java
│   └── AdministrationChargeSettlementRepository.java
├── service/
│   ├── AdministrationChargeService.java
│   └── AdministrationChargeServiceImpl.java
└── specification/
    └── AdministrationChargeSpecification.java
```

Do not create unnecessary abstractions if the existing codebase organizes modules differently. Consistency with the current project takes priority.

---

## 6. Entity Requirements

### 6.1 `AdministrationCharge`

Use the project's existing ID-generation convention and Java ID type. Because this schema uses `INTEGER`, the Java ID type should be `Integer`, not `Long`.

Expected relationships:

- `ManyToOne` to `Pochi`.
- `ManyToOne` to `AdministrationFeeRule`.
- `ManyToOne` to `User` for `createdBy`.
- One-to-many association to the charge-settlement join records, or an equivalent mapping consistent with the project.

Use:

- `LocalDate` for `chargeDate`;
- `Short` or the project's preferred small numeric type for `shiftCount`;
- `BigDecimal` for monetary amounts;
- `LocalDateTime` for `createdAt`.

Do not use `double` or `float` for money.

The entity must not contain business logic that queries repositories. Keep orchestration in the service layer.

### 6.2 `AdministrationChargeSettlement`

Model the composite key correctly using the same JPA pattern used elsewhere in the project, such as `@EmbeddedId` with `@MapsId`, if appropriate.

The association is immutable after creation.

A Settlement must not be associated with more than one charge.

Avoid cascade settings that could delete a Settlement when a charge association changes. A charge must never own the lifecycle of a Settlement.

---

## 7. DTOs

### 7.1 Create request

The request should expose only:

```java
Integer pochiId;
LocalDate chargeDate;
BigDecimal amount; // optional
String note;       // conditionally required
```

Do not accept:

- `shiftCount`;
- `suggestedAmount`;
- `administrationFeeRuleId`;
- Settlement IDs;
- Assignment IDs;
- `createdBy`;
- `createdAt`.

All of those values are derived or audited by the backend.

Apply Bean Validation for basic structural rules:

- `pochiId`: required and positive;
- `chargeDate`: required;
- `amount`: optional, but positive when supplied;
- `note`: apply a sensible maximum length consistent with the project.

The conditional note rule belongs in the service or in a reusable class-level validator because it depends on the calculated `suggestedAmount`.

### 7.2 Response

Return enough information for the frontend to explain the charge:

```text
id
pochiId
pochi identifier/display data already used by the project
chargeDate
administrationFeeRuleId
rule scope or useful rule summary, when available
shiftCount
suggestedAmount
amount
adjusted (derived boolean)
note
createdById
createdBy display data
createdAt
settlements
```

Each Settlement summary should contain only useful read-only information, such as:

```text
settlementId
pochiAssignmentId
shift
expectedAmount
acceptedAmount
settledAt / createdAt, using the actual existing field
```

Do not expose complete nested entities or bidirectional object graphs.

`adjusted` should be derived as:

```java
amount.compareTo(suggestedAmount) != 0
```

It does not require a database column.

---

## 8. MapStruct

Use MapStruct consistently with the other modules.

MapStruct may map entity data to response DTOs, but charge creation cannot be a blind DTO-to-entity conversion because the backend must resolve:

- Pochi;
- Assignments;
- Settlements;
- Administration Fee Rule;
- suggested amount;
- actual amount;
- authenticated creator.

Ignore derived and relational fields during initial mapping and set them explicitly in the service.

Prevent recursive mappings between:

- Administration Charge;
- join records;
- Settlements;
- Assignments.

---

## 9. Repository Requirements

Create focused repository methods for:

- finding a charge by ID;
- checking whether a charge exists by Pochi and date;
- retrieving charges with pagination and filters;
- checking whether a Settlement is already linked to a charge;
- checking whether an Assignment's Settlement is linked to a charge;
- obtaining Assignments for a Pochi and date in deterministic shift order;
- retrieving the Settlement for each Assignment without N+1 queries.

Use fetch joins, entity graphs, projections, or a focused query where appropriate. Avoid issuing repeated repository calls inside loops.

The duplicate check in Java improves the error message, but it does not replace the database unique constraint.

---

## 10. Service Creation Algorithm

Implement the equivalent of:

```text
create(request):
    verify no charge exists for request.pochiId + request.chargeDate

    pochi = find Pochi or fail

    assignments = find all Assignments for pochi + chargeDate
    validate assignment count is 1 or 2
    validate shifts are not duplicated

    settlements = resolve one Settlement for every Assignment
    reject if any Assignment is not settled
    reject if any Settlement already belongs to a charge

    shiftCount = assignments.size

    rule = resolve applicable Pochi-specific or GLOBAL rule
           using chargeDate

    suggestedAmount = select rule amount for shiftCount

    actualAmount = request.amount != null
        ? request.amount
        : suggestedAmount

    validate actualAmount > 0

    if actualAmount differs from suggestedAmount:
        require nonblank request.note

    creator = resolve current authenticated user

    create immutable AdministrationCharge
    save charge

    associate every resolved Settlement with the charge
    save associations

    return response
```

Do not silently generate a charge when eligibility is incomplete. Return a clear business error describing which Assignment or shift has not been settled.

---

## 11. REST API

Use the project's base API path and naming conventions. Suggested endpoints:

### Create

```http
POST /api/administration-charges
```

Default-rule amount request:

```json
{
  "pochiId": 35,
  "chargeDate": "2026-11-13"
}
```

Adjusted amount request:

```json
{
  "pochiId": 35,
  "chargeDate": "2026-11-13",
  "amount": 60.00,
  "note": "Pochi did not work the complete afternoon shift."
}
```

Return `201 Created` with the response DTO.

### Find by ID

```http
GET /api/administration-charges/{id}
```

Return `200 OK`, or `404 Not Found` when absent.

### List and filter

```http
GET /api/administration-charges
```

Support pagination and sorting using the project's existing pattern.

Useful optional filters:

```text
pochiId
chargeDate
chargeDateFrom
chargeDateTo
createdById
adjusted
```

If both exact date and date range are supported, define and document their precedence or reject contradictory combinations consistently.

No update or delete endpoints are allowed.

---

## 12. Error Handling

Use the project's global exception handler and error-response format.

Create or reuse specific business exceptions/messages for cases such as:

- Pochi not found.
- Administration Charge not found.
- Charge already exists for the Pochi and date.
- No Pochi Assignment exists for the date.
- Invalid number or combination of shifts.
- MORNING Assignment has no Settlement.
- AFTERNOON Assignment has no Settlement.
- Settlement already belongs to another Administration Charge.
- No applicable Administration Fee Rule exists for the date.
- Adjusted amount requires a nonblank note.
- Related Settlement is locked by an Administration Charge.
- Related Pochi Assignment is locked by an Administration Charge.
- A new Assignment cannot be created because the daily Administration Charge already exists.

Use appropriate status codes consistent with the project, generally:

- `400 Bad Request` for invalid request values or invalid shift combinations;
- `404 Not Found` for missing referenced resources;
- `409 Conflict` for duplicate charges, already-used Settlements, or attempts to change locked records.

Do not expose PostgreSQL constraint names or stack traces to API clients.

---

## 13. Integration Changes to Existing Modules

### 13.1 Pochi Assignments

Before creating a new Assignment, verify that no Administration Charge already exists for the same Pochi and assignment date.

Before updating or deleting an Assignment, verify that its Settlement is not linked to an Administration Charge.

Preserve all existing closed-period validations. The Administration Charge lock is an additional restriction, not a replacement.

### 13.2 Settlements

Before updating or deleting a Settlement, verify that it is not linked to an Administration Charge.

Preserve the existing rule that one Pochi Assignment cannot be liquidated twice.

Do not automatically alter or delete an Administration Charge if an operation is rejected.

### 13.3 Administration Fee Rules

Reuse the existing rule-resolution behavior.

An existing Administration Charge must never be recalculated when:

- a Rule is modified;
- a new Rule is created;
- a Rule expires or is deactivated.

Historical charges preserve their own `suggestedAmount`, `amount`, Rule reference, and shift count.

---

## 14. Unit Tests

Follow the project's existing JUnit 5 and Mockito conventions.

At minimum, cover:

### Successful creation

1. Creates a one-shift charge from a MORNING Assignment and Settlement.
2. Creates a one-shift charge from an AFTERNOON Assignment and Settlement.
3. Creates a two-shift charge only when MORNING and AFTERNOON are both settled.
4. Uses the Pochi-specific Rule before the GLOBAL Rule.
5. Falls back to the GLOBAL Rule when no applicable Pochi-specific Rule exists.
6. Resolves the Rule using `chargeDate`, not the current date.
7. Uses `suggestedAmount` as `amount` when the request omits amount.
8. Accepts an overridden amount when a nonblank note is supplied.
9. Associates exactly the eligible Settlement or Settlements.
10. Sets `createdBy` from the current-user provider.

### Validation failures

11. Rejects a duplicate charge for Pochi + date.
12. Rejects creation when no Assignment exists.
13. Rejects creation when one of two Assignments has no Settlement.
14. Rejects creation when no applicable Rule exists.
15. Rejects an overridden amount without a note.
16. Rejects an overridden amount with a blank note.
17. Rejects zero or negative amounts.
18. Rejects a Settlement already linked to another charge.
19. Treats `70.0` and `70.00` as equal by using `BigDecimal.compareTo`.
20. Rolls back the full operation if association persistence fails.

### Immutability and locking

21. Confirms no update endpoint exists.
22. Confirms no delete endpoint exists.
23. Rejects Settlement update when linked to a charge.
24. Rejects Settlement deletion when linked to a charge.
25. Rejects Assignment update when its Settlement is linked to a charge.
26. Rejects Assignment deletion when its Settlement is linked to a charge.
27. Rejects creating a new Assignment for Pochi + date when a charge already exists.

Also add repository tests for the important existence queries and database uniqueness behavior if that matches the project's current test strategy.

---

## 15. Implementation Quality Requirements

- Use constructor injection.
- Keep controllers thin.
- Put business orchestration in the service layer.
- Use `@Transactional` for creation and appropriate read-only transactions for queries.
- Use `BigDecimal` for all money.
- Reuse existing entities, repositories, services, exceptions, security helpers, and response wrappers.
- Avoid N+1 queries.
- Do not return JPA entities directly.
- Do not add update or delete behavior.
- Do not add a status column unless the database and business requirements are intentionally redesigned later.
- Do not recalculate historical charges during reads.
- Do not trust client-supplied derived values.
- Keep code and identifiers in English.
- Keep user-facing validation messages consistent with the existing application.
- Ensure compilation and all existing tests continue to pass.

---

## 16. Definition of Done

The module is complete when:

- The project compiles.
- All existing tests still pass.
- New unit tests pass.
- A one-shift eligible charge can be created.
- A two-shift charge cannot be created until both Settlements exist.
- Pochi-specific and GLOBAL Rule precedence works correctly.
- The Rule is selected using the Assignment/charge date.
- Suggested and overridden amounts are persisted correctly.
- An overridden amount requires a nonblank note.
- Each Settlement can belong to only one Administration Charge.
- Duplicate Pochi/date charges are rejected.
- Charges cannot be updated or deleted through the API.
- Related Settlements and Assignments are protected from update/delete.
- A new Assignment cannot be added after the daily charge exists.
- The API supports read-by-ID and paginated/filterable listing.
- Swagger/OpenAPI reflects the available endpoints and DTO validations.
