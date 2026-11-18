# Weekly Closing Module

## 1. Purpose

The Weekly Closing module controls the weekly account-closing process between an Admin and an Owner.

A Weekly Closing represents the account status for one Owner during one ISO calendar week and records:

- The ISO week number and its Monday-to-Sunday period.
- The Pochis included in the closing.
- Whether the week is still open, closed by the Admin, or definitively accepted by the Owner.
- Who performed each closing action and when.
- The final closing comment agreed upon by Admin and Owner.
- Every reopening performed before the Owner's final acceptance.

The main business purpose is to know exactly up to which week Admin and Owner have completed and accepted their accounts, even when they review several accumulated weeks at once.

---

## 2. Existing Database Tables

The database already contains these tables for the module:

### `weekly_closings`

Main Weekly Closing record.

Expected columns:

- `id`
- `owner_id`
- `week_year`
- `week_number`
- `week_start`
- `week_end`
- `status`
- `closing_comment`
- `admin_closed_by`
- `admin_closed_at`
- `owner_closed_by`
- `owner_closed_at`
- `created_at`

### `weekly_closing_pochis`

Stores the Pochis included in the Weekly Closing.

Expected columns:

- `id`
- `weekly_closing_id`
- `pochi_id`

This table must be treated as a historical snapshot of the Pochis included when the Weekly Closing was created. Do not dynamically replace its contents later based only on the Owner's current Pochis.

### `weekly_closing_reopens`

Stores the complete reopening history.

Expected columns:

- `id`
- `weekly_closing_id`
- `reopened_by`
- `reopened_at`
- `reason`

Every reopening must create a new record. Previous reopening records must never be overwritten or deleted.

---

## 3. ID Convention

All project IDs use `Integer` in Java.

Database primary keys use:

```sql
INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY
```

Do not use `Long`, `BIGINT`, `SERIAL`, or `BIGSERIAL` for this module.

---

## 4. Domain Terminology

Always use the domain term `Pochi` / `pochi`.

Do not use `Vehicle` / `vehicle` in entities, DTOs, services, repositories, controllers, tests, validation messages, comments, or documentation.

Use English for code, database-facing names, classes, fields, methods, and API contracts.

---

## 5. Week Definition

A week always runs from Monday through Sunday and follows ISO week rules.

Example:

```text
week_year: 2026
week_number: 47
week_start: 2026-11-16
week_end: 2026-11-22
```

`week_year` identifies the ISO week-based year. It is required because the same week number exists every year and dates close to New Year's Day can belong to the ISO week-based year adjacent to their calendar year.

The backend must calculate and validate the four week values consistently. Do not trust arbitrary combinations supplied by a client.

Required rules:

- `week_number` must be between 1 and 53.
- `week_start` must be Monday.
- `week_end` must be Sunday.
- `week_end` must equal `week_start + 6 days`.
- `week_year` and `week_number` must match the ISO week represented by `week_start` and `week_end`.
- Only one Weekly Closing may exist for the same `owner_id`, `week_year`, and `week_number`.

Prefer accepting `ownerId`, `weekYear`, and `weekNumber` when creating a closing, then calculate `weekStart` and `weekEnd` in the backend using Java ISO week fields.

---

## 6. Closing Unit

The closing unit is:

```text
Owner + ISO Week
```

The closing is not created independently per Pochi.

Example:

```text
Owner: Juan
Week: 47 / 2026
Period: 2026-11-16 to 2026-11-22
Pochis: 16, 24, 82
Status: OPEN
```

When the Weekly Closing is created, the backend must find the Pochis that belong to the Owner and add them to `weekly_closing_pochis`.

Creation must fail if no Pochis can be included.

---

## 7. Statuses

Create a Java enum equivalent to:

```java
public enum WeeklyClosingStatus {
    OPEN,
    ADMIN_CLOSED,
    OWNER_CLOSED
}
```

Valid transitions:

```text
OPEN -> ADMIN_CLOSED
ADMIN_CLOSED -> OPEN
ADMIN_CLOSED -> OWNER_CLOSED
```

Invalid transitions include:

```text
OPEN -> OWNER_CLOSED
OWNER_CLOSED -> OPEN
OWNER_CLOSED -> ADMIN_CLOSED
OWNER_CLOSED -> OWNER_CLOSED
```

`OWNER_CLOSED` is a terminal and permanent state.

---

## 8. Creating an OPEN Weekly Closing

A Weekly Closing is created initially with:

```text
status = OPEN
```

It may be created before the week has ended so the Admin can review its progress.

Creation rules:

1. Validate that the Owner exists.
2. Validate that another Weekly Closing does not already exist for the same Owner and ISO week.
3. Calculate the Monday and Sunday dates from `week_year` and `week_number`.
4. Create the Weekly Closing as `OPEN`.
5. Find the Owner's current Pochis.
6. Create one `weekly_closing_pochis` record for every included Pochi.
7. Fail the entire transaction if no Pochis are found or if any required step fails.

Creation of the closing and its Pochi snapshot must run in one transaction.

Do not require the week to be complete during creation. Completeness is required only for Admin closing.

---

## 9. Resolving Each Pochi Day

Before Admin closing, every date for every included Pochi must be resolved.

A Pochi-day is resolved when either:

1. At least one `pochi_assignment` exists for that Pochi and date. The derived result is `WORKED`.
2. A `pochi_daily_status` exists for that Pochi and date with an allowed non-work status such as `DAY_OFF`, `UNION_REST`, or `NOT_WORKED`.

A Pochi-day is unresolved when:

```text
no assignment + no Pochi Daily Status
```

`PENDING` must not be stored as another daily status. It is a derived condition.

One assignment is sufficient to resolve the day as `WORKED`, regardless of whether the assignment is `MORNING`, `AFTERNOON`, or both shifts.

The existing business rules of Pochi Assignments and Pochi Daily Status remain authoritative. Weekly Closing must not duplicate or replace them.

---

## 10. OPEN to ADMIN_CLOSED

Only an Admin action may perform this transition.

Required validations:

1. The current status must be `OPEN`.
2. The week must have ended. Admin closing is allowed starting on the Monday after `week_end`.
3. The Weekly Closing must contain at least one Pochi.
4. Every date from `week_start` through `week_end` must be resolved for every included Pochi.
5. Admin closing must follow the sequential-closing rule.
6. All validations must be repeated after a reopening. A previous successful close does not bypass them.

When successful:

```text
status = ADMIN_CLOSED
admin_closed_by = current user ID
admin_closed_at = current timestamp
```

Until real authentication is implemented, use the project's established temporary-user strategy in one centralized component or constant. Do not accept `admin_closed_by` directly from an untrusted API request.

### Unresolved-days response

If any days are unresolved, reject the transition and return all missing Pochi/date combinations.

Expected response shape:

```json
{
  "message": "Weekly closing cannot be completed",
  "unresolvedDays": [
    {
      "pochiId": 82,
      "date": "2026-11-19"
    }
  ]
}
```

Do not return only the first unresolved day. Return the complete list so the Admin can correct the week efficiently.

The transition and all validations must execute transactionally.

---

## 11. Sequential Admin Closing

Admin closings must not skip an earlier Weekly Closing for the same Owner.

Example:

```text
Week 45: OPEN
Week 46: OPEN
```

Week 46 cannot move to `ADMIN_CLOSED` while Week 45 remains `OPEN`.

This is valid:

```text
Week 45: ADMIN_CLOSED
Week 46: ADMIN_CLOSED
Week 47: ADMIN_CLOSED
```

The Owner does not need to accept Week 45 before the Admin closes Week 46. Admin sequencing and Owner sequencing are validated independently.

For the first closing available for an Owner, do not require a historical Weekly Closing for every prior week before this module existed. The implementation needs a clear operational starting point. Use the earliest Weekly Closing already created for that Owner as the beginning of sequence enforcement unless the existing project has an explicit migration/start-date rule.

---

## 12. ADMIN_CLOSED Behavior

When status is `ADMIN_CLOSED`:

- Included Pochi Assignments for the closing period are locked.
- Included Pochi Daily Status records for the closing period are locked.
- Included Settlements for the closing period are locked.
- Included Administration Charges for the closing period are locked.
- `closing_comment` remains editable by Admin.
- Reopening remains allowed.
- Owner closing remains allowed after its validations pass.

Expenses, Driver Debts, and Debt Payments are intentionally outside the Weekly Closing lock in this first version.

Do not add locking rules for them yet.

---

## 13. Closing Comment

Use one shared field:

```text
closing_comment
```

Do not create separate `admin_comment` and `owner_comment` fields.

The comment represents the final agreement reviewed by both parties. Examples include:

```text
$2,500 is reserved for tires.
$5,000 is reserved for painting Pochi 82.
$20,000 was delivered in cash to the Owner.
```

Rules:

- The comment is optional.
- Admin may create or edit it while status is `OPEN`.
- Admin may continue editing it while status is `ADMIN_CLOSED`.
- The Owner reviews the comment before final closing.
- The Owner's `OWNER_CLOSED` action means the Owner accepts both the report and the comment exactly as they exist at that moment.
- Once status becomes `OWNER_CLOSED`, the comment is permanently immutable.

Provide a focused endpoint or service operation for updating the closing comment. Do not expose unrestricted entity updates.

---

## 14. Reopening

Only this reopening transition is valid:

```text
ADMIN_CLOSED -> OPEN
```

Reopening is not allowed from `OWNER_CLOSED`.

Required reopening data:

- `weekly_closing_id`
- `reopened_by`
- `reopened_at`
- non-blank `reason`

Behavior:

1. Validate that the current status is `ADMIN_CLOSED`.
2. Validate that the reason is not blank.
3. Create a new `weekly_closing_reopens` history record.
4. Change the Weekly Closing status to `OPEN`.
5. Unlock the affected v1 modules again, unless a record is protected by another permanent closing rule.
6. Keep all previous reopening records.

The history insert and status transition must occur in one transaction.

Do not erase the previous `admin_closed_by` or `admin_closed_at` merely to hide the earlier closing. When the Admin closes the week again, update those fields to represent the latest effective Admin closing. The reopening history explains the intervening event.

Until authentication exists, `reopened_by` must use the same centralized temporary-user strategy. Do not trust a client-supplied actor ID.

---

## 15. ADMIN_CLOSED to OWNER_CLOSED

Only a closing currently in `ADMIN_CLOSED` may be accepted by the Owner.

Required validations:

1. The current status must be `ADMIN_CLOSED`.
2. The acting user must correspond to the closing's Owner once authentication and authorization are implemented.
3. The closing must be the earliest `ADMIN_CLOSED` week pending Owner acceptance for that Owner.
4. No earlier created week for that Owner may remain `OPEN` or `ADMIN_CLOSED` when a later week is being accepted.
5. The current closing comment and report are accepted exactly as presented.

When successful:

```text
status = OWNER_CLOSED
owner_closed_by = current user ID
owner_closed_at = current timestamp
```

`owner_closed_by` and `owner_closed_at` must exist in the implementation now, even before real authentication is available. Resolve the actor through the established temporary-user mechanism until authentication replaces it.

Do not accept `owner_closed_by` or `owner_closed_at` from the request body.

---

## 16. Sequential Owner Closing

Owner closing must be chronological and must not leave gaps.

Invalid example:

```text
Week 45: ADMIN_CLOSED
Week 46: ADMIN_CLOSED
Week 47: ADMIN_CLOSED

Attempt: close Week 47 as Owner
Result: rejected
```

Required order:

```text
Week 45 -> OWNER_CLOSED
Week 46 -> OWNER_CLOSED
Week 47 -> OWNER_CLOSED
```

This rule guarantees that the latest `OWNER_CLOSED` week indicates exactly how far Admin and Owner have definitively completed their accounts.

The API should return a meaningful business error identifying the earlier pending week.

---

## 17. OWNER_CLOSED Behavior

`OWNER_CLOSED` is terminal.

After this transition:

- The Weekly Closing cannot be reopened.
- Its status cannot be changed.
- Its `closing_comment` cannot be edited.
- Its Pochi snapshot cannot be changed.
- Its `admin_closed_by`, `admin_closed_at`, `owner_closed_by`, and `owner_closed_at` cannot be changed.
- The locked v1 movements remain permanently locked.

Any later correction must be represented by a new business movement in a later period, not by rewriting this historical closing.

No generic update or delete endpoint may bypass this rule.

---

## 18. V1 Movement Lock

Weekly Closing v1 protects these modules:

- `pochi_assignments`
- `pochi_daily_status`
- `settlements`
- `administration_charges`

A protected record is locked when:

1. Its Pochi belongs to `weekly_closing_pochis`.
2. Its relevant business date belongs to the closing period.
3. The closing is `ADMIN_CLOSED` or `OWNER_CLOSED`.

When the closing is reopened from `ADMIN_CLOSED` to `OPEN`, the provisional lock is released.

When the closing is `OWNER_CLOSED`, the lock is permanent.

The exact relevant date must follow each module's existing business model. Reuse its true business date rather than `created_at`.

The lock must be enforced in the service layer of every affected module before update, cancellation, or deletion. Do not rely only on the Weekly Closing controller or on frontend behavior.

### Modules intentionally outside v1 lock

Do not block these modules yet:

- `expenses`
- `driver_debts`
- `debt_payments`

This is a deliberate business decision. Their behavior will be observed before defining future closing rules.

---

## 19. Recommended API Operations

Adapt paths to the project's existing controller conventions, but provide operations equivalent to:

```text
POST   /weekly-closings
GET    /weekly-closings/{id}
GET    /weekly-closings
POST   /weekly-closings/{id}/admin-close
POST   /weekly-closings/{id}/reopen
POST   /weekly-closings/{id}/owner-close
PATCH  /weekly-closings/{id}/comment
```

Useful filters for listing:

```text
ownerId
weekYear
weekNumber
status
```

Useful read models should include:

- Week label and period.
- Owner summary.
- Included Pochis.
- Current status.
- Closing comment.
- Admin closing audit fields.
- Owner closing audit fields.
- Reopening history when detailed output is requested.
- Resolved/unresolved day summary while `OPEN`.
- Whether Admin closing is currently allowed.
- Whether Owner closing is currently allowed.

Do not expose JPA entities directly through controllers.

---

## 20. Suggested DTOs

Use names aligned with the project's actual conventions. A reasonable starting set is:

```text
WeeklyClosingCreateRequest
WeeklyClosingResponse
WeeklyClosingDetailResponse
WeeklyClosingSummaryResponse
WeeklyClosingCommentUpdateRequest
WeeklyClosingReopenRequest
WeeklyClosingPochiResponse
WeeklyClosingReopenResponse
UnresolvedPochiDayResponse
```

Do not add actor IDs or generated timestamps to action request DTOs.

Example reopening request:

```json
{
  "reason": "Incorrect settlement amount for Pochi 16 on 2026-11-18."
}
```

Example comment update request:

```json
{
  "closingComment": "The Owner received $20,000 in cash and $2,500 remains reserved for tires."
}
```

---

## 21. Architecture and Implementation

Follow the existing AdministraPochi layered architecture and package structure.

Use the project's established patterns for:

- Entities.
- Enums.
- DTOs.
- MapStruct mappers.
- Spring Data JPA repositories.
- Services and service implementations, if that is the current pattern.
- REST controllers.
- Jakarta Bean Validation.
- Business exceptions.
- Global exception handling.
- Unit tests.

Before creating files, inspect similar completed modules such as Pochi Assignments, Settlements, Administration Charges, and Pochi Daily Status. Match the real naming, annotations, response style, pagination style, and exception conventions already present in the repository.

Do not invent a parallel architecture.

Use `@Transactional` for operations that change status, create Pochi snapshots, or write reopening history.

Avoid generic CRUD updates for a state-machine aggregate. Use explicit business methods such as:

```java
createOpenClosing(...)
closeByAdmin(...)
reopen(...)
closeByOwner(...)
updateClosingComment(...)
```

---

## 22. Repository Queries

Add focused queries only when needed. Expected capabilities include:

- Detect an existing closing for `owner_id + week_year + week_number`.
- Find closings by Owner ordered chronologically.
- Find earlier closings for the same Owner that violate Admin sequencing.
- Find earlier closings for the same Owner that violate Owner sequencing.
- Find included Pochis for a closing.
- Find whether a Pochi/date is protected by an `ADMIN_CLOSED` or `OWNER_CLOSED` closing.
- Find all reopening history ordered by `reopened_at` and `id`.

Keep sequential validation explicit and readable. Do not use a fragile `MAX(week_number)` comparison without `week_year`, because week numbers repeat annually.

Prefer comparisons based on `week_start` when validating chronology across years.

---

## 23. Validation and Error Handling

Create clear business exceptions/messages for at least:

- Weekly Closing not found.
- Duplicate Weekly Closing for Owner and week.
- Owner has no Pochis.
- Invalid ISO week.
- Invalid status transition.
- Week has not ended.
- Week contains unresolved Pochi-days.
- Earlier week must be Admin-closed first.
- Earlier week must be Owner-closed first.
- Reopening reason is blank.
- OWNER_CLOSED cannot be reopened.
- Closing comment cannot be changed after OWNER_CLOSED.
- Movement belongs to a closed Weekly Closing.

For unresolved days, use a structured error response containing the complete `unresolvedDays` list rather than embedding every item in a single text message.

---

## 24. Required Tests

Add unit tests following the project's existing test style.

At minimum, cover:

### Creation

- Creates an `OPEN` Weekly Closing with calculated Monday/Sunday dates.
- Creates the Pochi snapshot.
- Rejects a duplicate Owner/week.
- Rejects creation when the Owner has no Pochis.
- Validates ISO week 53 correctly.
- Handles ISO year boundaries correctly.

### Admin close

- Closes a completed past week.
- Rejects closing before the week ends.
- Rejects when at least one Pochi-day is unresolved.
- Returns all unresolved Pochi/date combinations.
- Treats one assignment as `WORKED`.
- Accepts `DAY_OFF`, `UNION_REST`, and `NOT_WORKED` as resolved.
- Rejects an invalid current status.
- Rejects skipping an earlier open week.
- Sets `admin_closed_by` and `admin_closed_at` internally.

### Comment

- Allows comment update while `OPEN`.
- Allows comment update while `ADMIN_CLOSED`.
- Rejects comment update while `OWNER_CLOSED`.

### Reopen

- Reopens `ADMIN_CLOSED` to `OPEN`.
- Creates a reopening-history record.
- Rejects blank reasons.
- Rejects reopening from `OPEN`.
- Rejects reopening from `OWNER_CLOSED`.
- Preserves multiple reopening-history records.
- Requires all validations again on the next Admin close.

### Owner close

- Closes `ADMIN_CLOSED` as Owner.
- Sets `owner_closed_by` and `owner_closed_at` internally.
- Rejects closing from `OPEN`.
- Rejects a later week while an earlier week is pending.
- Permanently rejects any reopening or mutation after `OWNER_CLOSED`.

### Movement locking

For each v1 protected module:

- Allows changes when the Weekly Closing is `OPEN`.
- Rejects changes when it is `ADMIN_CLOSED`.
- Allows changes again after reopening.
- Rejects changes permanently after `OWNER_CLOSED`.
- Does not lock another Owner's Pochis for the same dates.
- Does not add new locks to Expenses, Driver Debts, or Debt Payments.

---

## 25. Out of Scope

Do not implement the following unless separately requested:

- Authentication or complete authorization infrastructure.
- Locking Expenses.
- Locking Driver Debts.
- Locking Debt Payments.
- Automatically converting `closing_comment` text into financial movements.
- Storing report totals in `weekly_closings`.
- Duplicating assignment, settlement, charge, or status details inside the closing tables.
- Reopening an `OWNER_CLOSED` week.
- A separate closing per Pochi.
- New workflow statuses beyond `OPEN`, `ADMIN_CLOSED`, and `OWNER_CLOSED`.
- Hard deletion of Weekly Closings or reopening history.

---

## 26. Completion Checklist

Before considering the module complete, verify:

- [ ] Existing database schema is mapped exactly.
- [ ] All IDs use `Integer`.
- [ ] All domain naming uses `Pochi`, never `Vehicle`.
- [ ] ISO week calculation works, including year boundaries.
- [ ] Closing creation stores the Pochi snapshot transactionally.
- [ ] Admin close validates all Pochi-days and returns every unresolved day.
- [ ] Admin close is sequential per Owner.
- [ ] Admin actor and timestamp are set internally.
- [ ] Closing comment is editable only before `OWNER_CLOSED`.
- [ ] Reopening requires and preserves who, when, and reason.
- [ ] Reopened weeks are fully revalidated before closing again.
- [ ] Owner close is sequential per Owner.
- [ ] Owner actor and timestamp are set internally.
- [ ] `OWNER_CLOSED` is terminal and immutable.
- [ ] V1 movement locks are enforced inside affected services.
- [ ] Expenses, Driver Debts, and Debt Payments remain outside the lock.
- [ ] Controllers expose DTOs, not entities.
- [ ] Business operations are transactional.
- [ ] Unit tests cover success, failure, sequencing, reopening, ISO dates, and locks.
- [ ] The project compiles and all tests pass.
