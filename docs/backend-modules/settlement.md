# Settlements Module

## 1. Purpose

The Settlements module records the actual amount accepted by an administrator for work performed by a driver using a Pochi.

This module must distinguish between:

- The amount suggested by the applicable Settlement Rule.
- The amount actually accepted by the administrator.

Settlement Rules provide a recommendation based on the work performed, but the administrator always has the final decision.

Example:

Settlement Rule:

MORNING + EVENING = $400.00

The system calculates:

expectedAmount = 400.00

The administrator may accept:

acceptedAmount = 400.00

or may override it:

acceptedAmount = 300.00
adjustmentReason = "Partial settlement accepted. Remaining amount will be handled separately."

The Settlement must preserve both values historically.


---

## 2. Important Business Principles

### Settlement Rules are recommendations

`settlement_rules` defines how much a driver would normally be expected to settle.

However, Settlement Rules must never automatically determine the final accepted amount.

The administrator has the final decision.

Therefore:

expectedAmount = calculated/recommended amount
acceptedAmount = actual amount accepted by ADMIN


### Historical values must not be recalculated

Once a Settlement has been created, its historical monetary values must remain independent from future modifications to Settlement Rules.

Example:

At settlement time:

expectedAmount = 400.00
acceptedAmount = 400.00

If the corresponding Settlement Rule later changes from $400.00 to $450.00, the historical Settlement must remain:

expectedAmount = 400.00
acceptedAmount = 400.00


---

## 3. Settlement and Pochi Assignment Relationship

Settlements must be explicitly connected to the Pochi Assignments being settled.

Do NOT infer the relationship only from:

- driver
- pochi
- work date
- shift

Use an explicit relationship through:

`settlement_assignments`

Example:

Pochi Assignment #101

Driver 1
Pochi 16
2026-11-11
MORNING

Pochi Assignment #102

Driver 1
Pochi 16
2026-11-11
EVENING

Settlement #30:

expectedAmount = 400.00
acceptedAmount = 400.00

Relationships:

Settlement #30 -> Assignment #101
Settlement #30 -> Assignment #102


---

## 4. Critical Uniqueness Rule

A Pochi Assignment can belong to ONE Settlement only.

Once an assignment has been settled, it cannot be included in another Settlement.

Database enforcement:

UNIQUE (pochi_assignment_id)

This rule is critical.

Example:

Settlement #30 already contains:

Assignment #101
Assignment #102

Attempting:

Settlement #31 -> Assignment #102

must fail.

The application must validate this situation before attempting persistence, while the database unique constraint remains the final integrity protection.


---

## 5. Settlement Granularity

Normally, one Settlement represents:

Driver + Pochi + Work Date

and may contain one or more assignments for that combination.

Example:

Driver 1
Pochi 16
2026-11-11

Assignments:

MORNING
EVENING

Both assignments may belong to the same Settlement.

Do not persist `shiftCount` in the Settlement.

The number and types of shifts must be derived from the related Pochi Assignments.

This avoids duplicated state.


---

## 6. Expected Amount vs Accepted Amount

The Settlement must contain:

expectedAmount
acceptedAmount

### expectedAmount

Represents the amount recommended by the applicable Settlement Rule.

### acceptedAmount

Represents the amount the administrator actually accepts.

These values may differ.


---

## 7. Adjustment Reason

If:

acceptedAmount == expectedAmount

then:

adjustmentReason is optional.

If:

acceptedAmount != expectedAmount

then:

adjustmentReason is mandatory.

Example:

expectedAmount = 400.00
acceptedAmount = 300.00

requires something like:

adjustmentReason =
"Partial settlement accepted. Remaining amount will be handled separately."

Validation must exist at both application and database level.


---

## 8. Debts Are NOT Created Automatically

A difference between `expectedAmount` and `acceptedAmount` does NOT automatically represent a debt.

Example:

expectedAmount = 400.00
acceptedAmount = 300.00

This could mean:

### Case A - Adjustment

The administrator decided that $300.00 is the final accepted amount.

Debt:

$0.00

### Case B - Partial Payment

The administrator accepts $300.00 now, but the driver still owes $100.00.

Debt:

$100.00

The Settlements module must NOT decide which case applies.

Driver debts will be handled explicitly in the future Driver Debts module.

Creating a Settlement must therefore NEVER automatically create a debt.


---

## 9. Administration Fees Are OUT OF SCOPE

Administration fees charged to Pochi owners do NOT belong to the Settlements module.

Administration fees will have their own business rules, similar in concept to Settlement Rules.

Future module:

administration_fee_rules

Examples of potential future rules:

MORNING only -> $50.00
EVENING only -> $50.00
MORNING + EVENING -> $70.00
No work -> $0.00

These rules must NOT be implemented as part of the Settlements module.

Settlements represent the driver-side financial operation.

Administration fees represent an owner-side financial operation.


---

## 10. Closing and Locking Are OUT OF SCOPE

Do NOT add fields such as:

locked
lockedAt
adminClosed
ownerClosed

to the Settlement entity at this stage.

Closing will be handled later by a dedicated period/owner closing mechanism.

The expected lifecycle will eventually resemble:

OPEN
->
ADMIN_CLOSED
->
OWNER_CLOSED

Once the administrative closing is completed, the related historical information may become immutable.

However, this logic does NOT belong to the current Settlements implementation.


---

# 11. Database Model

## settlements

Expected database structure:

- id
- driver_id
- pochi_id
- work_date
- expected_amount
- accepted_amount
- adjustment_reason
- notes
- created_by
- modified_by
- created_at
- updated_at

Relationships:

driver_id -> drivers.id

pochi_id -> pochis.id

created_by -> users.id

modified_by -> users.id


## settlement_assignments

Expected structure:

- settlement_id
- pochi_assignment_id

Relationships:

settlement_id -> settlements.id

pochi_assignment_id -> pochi_assignments.id

Constraints:

PRIMARY KEY (settlement_id, pochi_assignment_id)

UNIQUE (pochi_assignment_id)


---

# 12. Monetary Fields

Use BigDecimal in Java for monetary values.

Never use:

double
float

Expected entity fields:

BigDecimal expectedAmount;
BigDecimal acceptedAmount;

Database:

NUMERIC(10,2)


---

# 13. Entity Design

Create the necessary JPA entities following the existing project architecture and conventions.

Main entity:

Settlement

The relationship with Pochi Assignments must accurately represent the `settlement_assignments` table.

Avoid exposing JPA entities directly through the REST API.

Do not introduce unnecessary bidirectional relationships.

Prefer the simplest domain relationship that satisfies the required queries and persistence operations.


---

# 14. DTOs

Follow the existing project pattern.

Create separate DTOs for:

- creation
- update
- response

Suggested structure:

SettlementCreateRequest
SettlementUpdateRequest
SettlementResponse

Naming should remain consistent with the conventions already established in the project.


---

# 15. SettlementCreateRequest

The creation request should provide enough information to identify the Pochi Assignments being settled and the administrator decision.

Expected concept:

- List of Pochi Assignment IDs
- acceptedAmount
- adjustmentReason
- notes

The client should NOT be responsible for supplying trusted values such as:

- driverId
- pochiId
- workDate
- expectedAmount

when those values can be safely derived by the backend from the selected Pochi Assignments and Settlement Rules.

The backend must resolve and validate the selected assignments.

The backend must calculate the expected amount using the applicable Settlement Rule.

The administrator supplies the final accepted amount.


---

# 16. Settlement Creation Flow

The creation flow should conceptually be:

1. Receive selected Pochi Assignment IDs.

2. Fetch the Pochi Assignments from the database.

3. Verify that all requested assignments exist.

4. Verify that none of those assignments is already associated with another Settlement.

5. Verify that the assignments belong to the same driver.

6. Verify that the assignments belong to the same Pochi.

7. Verify that the assignments belong to the same work date.

8. Verify that the selected assignment combination is valid.

9. Determine the applicable Settlement Rule.

10. Calculate `expectedAmount`.

11. Receive `acceptedAmount` from the administrator.

12. If acceptedAmount != expectedAmount, require adjustmentReason.

13. Create the Settlement.

14. Associate the selected Pochi Assignments through `settlement_assignments`.

15. Persist the transaction atomically.

The Settlement and its assignment relationships must be created within the same transaction.


---

# 17. Important Validation Rules

The service layer must validate at least the following.

## Selected assignments must exist

Every supplied Pochi Assignment ID must exist.

If any requested assignment does not exist, creation must fail.


## Assignments cannot already be settled

Before creating a Settlement, validate that none of the selected Pochi Assignments belongs to an existing Settlement.


## Same Driver

All selected assignments must correspond to the same Driver.


## Same Pochi

All selected assignments must correspond to the same Pochi.


## Same Work Date

All selected assignments must correspond to the same work date.


## No duplicate assignments in request

Example:

[101, 101, 102]

must not be accepted as three assignments.


## Expected Amount

expectedAmount must be greater than or equal to zero.


## Accepted Amount

acceptedAmount must be greater than or equal to zero.


## Adjustment reason

If:

acceptedAmount != expectedAmount

then:

adjustmentReason must not be null, empty, or blank.


---

# 18. Settlement Rule Resolution

Settlement Rule resolution belongs to backend business logic.

The backend must inspect the selected assignments and determine the applicable Settlement Rule according to the Settlement Rules module already implemented.

Example:

Assignments:

MORNING
EVENING

The backend identifies the corresponding configured Settlement Rule.

If the rule indicates:

expectedAmount = 400.00

the Settlement stores:

expectedAmount = 400.00

The administrator may then accept:

acceptedAmount = 400.00

or another amount with a mandatory adjustment reason.


---

# 19. Mapper

Use MapStruct following the existing project conventions.

Create:

SettlementMapper

The mapper should be responsible only for mapping.

Business logic must NOT be placed inside MapStruct mappings.

Examples of logic that belong in the service instead of the mapper:

- resolving Pochi Assignments
- verifying existing settlements
- finding Settlement Rules
- calculating expectedAmount
- validating assignment compatibility
- validating adjustment reasons


---

# 20. Repository Layer

Create:

SettlementRepository

and any repository required for the `settlement_assignments` relationship according to the chosen JPA mapping.

Repository queries should support the business requirements without duplicating business logic.

Important capability:

Determine whether a Pochi Assignment has already been settled.

The database UNIQUE constraint remains the final protection against duplicate settlement.


---

# 21. Service Layer

Follow the same architecture used by the existing modules.

Suggested:

SettlementService

SettlementServiceImpl

The service layer owns the business logic.

Responsibilities include:

- resolving assignments
- validating assignments
- validating settlement eligibility
- resolving Settlement Rules
- calculating expectedAmount
- validating acceptedAmount
- validating adjustmentReason
- creating Settlement relationships
- updating allowed Settlement information
- coordinating repository calls
- throwing domain-appropriate exceptions

Use `@Transactional` where necessary.


---

# 22. Update Behavior

Settlement updates require special care because Settlements represent financial history.

Allow only explicitly supported modifications.

Do not casually replace associated Pochi Assignments during a standard update operation.

At minimum, updates may include:

- acceptedAmount
- adjustmentReason
- notes

If acceptedAmount changes, the following invariant must always remain valid:

acceptedAmount != expectedAmount
->
adjustmentReason required

Do not recalculate historical expectedAmount simply because the active Settlement Rule has changed.

Future closing/locking rules may make Settlements immutable.


---

# 23. Delete Behavior

Treat deletion carefully.

A Settlement is a financial record and should not be casually deleted.

If the existing project pattern requires DELETE support, ensure deletion does not leave inconsistent relationship data.

Do NOT implement cascading behavior that could delete:

- Driver
- Pochi
- User
- Pochi Assignment
- Settlement Rule

Deleting a Settlement, if currently allowed, may remove its rows from `settlement_assignments`, but must never delete the original Pochi Assignments.

Future administrative closing rules may prevent deletion completely once a period is closed.


---

# 24. Controller

Create a REST controller following the current project conventions.

Suggested base endpoint:

/api/settlements

Expected basic operations should follow existing module conventions.

Examples:

POST   /api/settlements
GET    /api/settlements/{id}
GET    /api/settlements
PUT    /api/settlements/{id}
DELETE /api/settlements/{id}

Do not add unnecessary custom endpoints until there is a real use case.


---

# 25. Filtering

The `findAll` endpoint should support useful optional filters following the project conventions.

Useful candidates include:

- driverId
- pochiId
- workDate
- workDateFrom
- workDateTo

Avoid implementing excessive filters unless already supported by established project patterns.


---

# 26. Response DTO

SettlementResponse should expose useful business information without exposing internal persistence implementation.

Useful information includes:

- id
- driver
- pochi
- workDate
- expectedAmount
- acceptedAmount
- adjustmentReason
- notes
- associated Pochi Assignments
- createdBy
- modifiedBy
- createdAt
- updatedAt

Prefer useful lightweight nested DTOs or the existing response patterns rather than returning complete JPA entities.


---

# 27. Audit Fields

Audit information is important in this module because Settlements will become a critical source for future financial reports.

Persist:

createdBy
modifiedBy
createdAt
updatedAt

`createdBy` identifies the user who originally registered the Settlement.

`modifiedBy` identifies the user responsible for the latest modification.

Do not confuse:

WHO performed the operation

with:

WHEN the operation happened.


---

# 28. Exceptions

Use the existing global exception-handling architecture.

Handle business situations such as:

- Settlement not found
- Pochi Assignment not found
- Assignment already settled
- Assignments belong to different drivers
- Assignments belong to different Pochis
- Assignments belong to different work dates
- Invalid assignment combination
- Settlement Rule not found
- Adjustment reason required
- Invalid monetary amount

Reuse existing exception types where appropriate instead of creating unnecessary exception classes.


---

# 29. Tests

Implement unit tests following the existing project's testing conventions.

At minimum cover:

## Successful creation

MORNING + EVENING assignments.

Expected amount calculated correctly.

Accepted amount equal to expected amount.

Settlement successfully created.


## Single assignment

Valid MORNING-only or EVENING-only settlement.


## Assignment already settled

Attempting to settle an assignment that already belongs to another Settlement must fail.


## Different drivers

Assignments from different drivers must fail.


## Different Pochis

Assignments from different Pochis must fail.


## Different dates

Assignments from different work dates must fail.


## Duplicate IDs

A request containing duplicate Pochi Assignment IDs must fail.


## Adjustment without reason

expectedAmount = 400.00
acceptedAmount = 300.00
adjustmentReason = null

Must fail.


## Adjustment with reason

expectedAmount = 400.00
acceptedAmount = 300.00
adjustmentReason = "Partial amount accepted."

Must succeed.


## Settlement Rule resolution

Verify that the correct Settlement Rule is selected based on the assignments.


## Historical amount preservation

Updating a Settlement Rule must not retroactively modify existing Settlement expectedAmount.


---

# 30. Architecture

Follow the architecture already established in the project.

Typical structure:

controller
SettlementController

dto
SettlementCreateRequest
SettlementUpdateRequest
SettlementResponse

entity
Settlement

mapper
SettlementMapper

repository
SettlementRepository

service
SettlementService
SettlementServiceImpl

Use additional classes only when they provide clear value.

Do not overengineer the module.


---

# 31. General Coding Conventions

Follow the existing global Copilot instructions and project conventions.

Use:

- Spring Boot
- Spring Data JPA
- Jakarta Validation
- Lombok
- MapStruct
- PostgreSQL
- BigDecimal for monetary values
- LocalDate for work dates
- LocalDateTime or the project's established type for audit timestamps

Keep:

Controller -> Service -> Repository

separation.

Controllers must remain thin.

Business logic belongs in the service.

Mapping belongs in MapStruct mappers.

Persistence logic belongs in repositories.

Entities must not be exposed directly through REST endpoints.


---

# 32. Do Not Implement Yet

The Settlements module must NOT implement:

- automatic Driver Debt creation
- Driver Debt Payments
- Administration Fee Rules
- Administration Charges
- Expenses
- Owner Closing
- Administrative Closing
- Settlement locking
- Owner payment confirmation
- Reports

These concepts will be implemented in their corresponding modules.

Do not create speculative abstractions for future modules.


---

# 33. Final Business Flow

The expected flow is:

Pochi Assignments
|
v
Determine actual worked shifts
|
v
Settlement Rule
|
v
Calculate expectedAmount
|
v
Administrator reviews amount
|
+-------------------------+
|                         |
v                         v
Accept expected amount       Override amount
|
v
adjustmentReason required
|                         |
+------------+------------+
|
v
Create Settlement
|
v
Link Pochi Assignments
|
v
Assignments cannot be settled again


Future modules may continue:

Settlement
|
+--> Driver Debt
|
+--> Administration Fees
|
+--> Expenses
|
+--> Owner Closing
|
+--> Reports


---

# 34. Core Principle

The system calculates and recommends.

The administrator decides.

The database preserves exactly what happened.

A Settlement is therefore the historical record of the administrator's accepted financial decision for specific Pochi Assignments.