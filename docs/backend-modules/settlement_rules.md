# Settlement Rules Module

## 1. Purpose

The Settlement Rules module defines the standard amount that a driver is expected to pay for a working day based on:

- The number of shifts worked.
- The Pochi involved, when a Pochi-specific settlement rule exists.
- The date on which the work occurred.

Settlement Rules represent expected/default settlement amounts only.

They MUST NOT represent:

- Actual payments.
- Settlements already received.
- Driver debts.
- Debt payments.
- Expenses.
- Temporary discounts.
- Weather-related adjustments.
- Operational incidents.
- Administrative settlement adjustments.

Those concepts belong to other modules.

---

# 2. Business Context

A driver may work one or two shifts during a day.

Current general business rules are:

- 1 shift = $250.00
- 2 shifts = $400.00

These amounts can change over time.

For example:

- A normal period may require $400.00 for two shifts.
- A later period may require $450.00 for two shifts.

There are no predefined "high season" or "low season" dates.

Settlement amounts change when the business receives notification of a new rate.

Therefore, settlement rules MUST be historical and date-based.

Historical rules MUST NOT be overwritten when a new rate becomes effective.

Example:

    Rule A
    shiftCount = 2
    amount = 400.00
    validFrom = 2026-01-01
    validTo = 2026-11-30

    Rule B
    shiftCount = 2
    amount = 450.00
    validFrom = 2026-12-01
    validTo = null

---

# 3. General and Pochi-Specific Rules

Settlement rules can exist at two levels.

## 3.1 General Rule

When `pochi_id` is NULL, the rule is a general rule.

Example:

    pochiId = null
    shiftCount = 1
    amount = 250.00

means:

    Any Pochi without a specific rule
    + 1 worked shift
    = expected settlement of $250.00

Another example:

    pochiId = null
    shiftCount = 2
    amount = 400.00

means:

    Any Pochi without a specific rule
    + 2 worked shifts
    = expected settlement of $400.00

---

## 3.2 Pochi-Specific Rule

Some Pochis may have a different settlement amount.

This can occur, for example, because of the condition or deterioration of a Pochi.

Example:

    pochiId = <Pochi 82 ID>
    shiftCount = 2
    amount = 350.00

This rule means that Pochi 82 should normally generate an expected settlement of $350.00 for two shifts instead of the general $400.00 rule.

A Pochi-specific rule has priority over a general rule when both could apply.

---

# 4. Rule Resolution

When resolving the expected settlement amount for:

- a Pochi,
- a shift count,
- and a date,

the application MUST use the following priority.

## Step 1 - Search for a Pochi-specific rule

Search for a rule matching:

    pochiId = requested Pochi
    shiftCount = requested shift count
    validFrom <= requested date
    AND
    (validTo IS NULL OR validTo >= requested date)

If a valid Pochi-specific rule exists, use it.

## Step 2 - Fall back to the general rule

If no Pochi-specific rule exists, search for:

    pochiId IS NULL
    shiftCount = requested shift count
    validFrom <= requested date
    AND
    (validTo IS NULL OR validTo >= requested date)

## Step 3 - No applicable rule

If neither a Pochi-specific nor a general rule exists:

DO NOT:

- Return zero.
- Invent an amount.
- Use the most recent rule regardless of dates.
- Silently continue.

Throw a business exception indicating that no applicable settlement rule exists.

---

# 5. Database Table

The existing PostgreSQL table is:

    settlement_rules

Fields:

    id          INTEGER PRIMARY KEY
    pochi_id    INTEGER NULL FK -> pochis(id)
    shift_count SMALLINT NOT NULL
    amount      NUMERIC(10,2) NOT NULL
    valid_from  DATE NOT NULL
    valid_to    DATE NULL
    created_at  TIMESTAMP NOT NULL
    updated_at  TIMESTAMP NOT NULL

Database constraints already guarantee:

    shift_count IN (1, 2)

    amount > 0

    valid_to IS NULL
    OR
    valid_to >= valid_from

The foreign key is:

    settlement_rules.pochi_id
        -> pochis.id

The existing lookup index is:

    idx_settlement_rules_lookup

over:

    pochi_id
    shift_count
    valid_from
    valid_to

---

# 6. Date Validity

A Settlement Rule applies to a date when:

    validFrom <= date

and:

    validTo == null
    OR
    validTo >= date

Both `validFrom` and `validTo` are inclusive.

Example:

    validFrom = 2026-11-01
    validTo   = 2026-11-30

The rule applies from November 1 through November 30 inclusive.

A NULL `validTo` represents an open-ended rule.

Example:

    validFrom = 2026-12-01
    validTo = null

means the rule applies from December 1 onward until another business operation closes its validity.

---

# 7. Overlapping Rules

The application MUST NOT allow overlapping rules for the same scope.

Scope is defined by:

    pochiId + shiftCount

For general rules:

    pochiId = null + shiftCount

For Pochi-specific rules:

    pochiId + shiftCount

Example of an invalid overlap:

    General
    shiftCount = 2
    amount = 400
    2026-01-01 -> 2026-12-31

and:

    General
    shiftCount = 2
    amount = 450
    2026-11-01 -> null

These rules overlap and MUST NOT be allowed.

The service layer MUST validate overlapping periods before creating or updating a rule.

The validation must also work correctly when `validTo` is NULL.

When updating a rule, exclude the rule being updated from the overlap search.

Throw a business exception when an overlap is detected.

---

# 8. Historical Integrity

Existing historical rules MUST NOT be overwritten just because the settlement amount changes.

Example:

Current rule:

    2 shifts
    $400
    validFrom = 2026-01-01
    validTo = null

New rate effective 2026-12-01:

The previous rule should become:

    $400
    validFrom = 2026-01-01
    validTo = 2026-11-30

And a new rule should be created:

    $450
    validFrom = 2026-12-01
    validTo = null

DO NOT simply update:

    amount = 400

to:

    amount = 450

because doing so would destroy historical information.

---

# 9. Settlement Rules vs Settlements

Settlement Rules define EXPECTED settlement amounts.

They do not represent the final amount accepted by an administrator.

Example:

General rule:

    1 shift = $250

A driver works one shift, but heavy rain prevents completion of the normal working day.

An administrator may later accept:

    $150

This DOES NOT change the Settlement Rule.

The rule remains:

    expected amount = $250

The exceptional amount belongs to the future Settlements module.

Conceptually:

    expectedAmount = 250
    finalAmount = 150
    adjustmentReason = WEATHER

Do not implement this settlement adjustment behavior inside Settlement Rules.

---

# 10. Operational Pochi Changes

A driver can operate different Pochis during the same day due to operational circumstances.

Example:

    Driver 3
    MORNING -> Pochi 16
    EVENING -> Pochi 82

The Settlement Rules module MUST NOT decide which Pochi receives the final settlement.

That decision belongs to the administrator during the future settlement process.

Settlement Rules should only provide possible expected amounts.

For example:

    General two-shift rule -> $400

    Pochi 82 two-shift rule -> $350

The administrator will later determine how the settlement is assigned.

DO NOT implement automatic settlement allocation in this module.

---

# 11. Driver Debts

Driver debts are NOT Settlement Rules.

Example:

A driver owes money because the Owner initially paid an insurance-related expense after an accident.

The driver may repay that debt gradually.

Example:

    Original debt: $3,000

    Payment 1: $200
    Payment 2: $300
    Payment 3: $100

Debt history and debt payments will belong to a future module.

DO NOT add debt fields or debt logic to Settlement Rules.

---

# 12. Entity

Create:

    SettlementRule

Map it to:

    settlement_rules

Suggested fields:

    Integer id
    Pochi pochi
    Integer or Short shiftCount
    BigDecimal amount
    LocalDate validFrom
    LocalDate validTo
    LocalDateTime createdAt
    LocalDateTime updatedAt

Use the existing project's Lombok and JPA conventions.

Use:

    BigDecimal

for monetary amounts.

Do NOT use:

    double
    float

for money.

`pochi` MUST be nullable because NULL represents a general rule.

---

# 13. DTOs

Follow the architecture already established by the project.

Create separate DTOs for create, update, and response operations.

Suggested structure:

    SettlementRuleCreateRequest
    SettlementRuleUpdateRequest
    SettlementRuleResponse

Do not expose the JPA entity directly through the REST API.

Validate input DTOs using Jakarta Validation following the existing project conventions.

Expected validations include:

    shiftCount required
    shiftCount between 1 and 2

    amount required
    amount > 0

    validFrom required

    validTo optional
    validTo >= validFrom

`pochiId` is optional.

If `pochiId` is provided, verify that the Pochi exists.

If `pochiId` is NULL, treat the rule as general.

---

# 14. Mapper

Create a MapStruct mapper following the same pattern used by the existing modules.

Suggested name:

    SettlementRuleMapper

Responsibilities:

    Entity -> Response DTO
    Create Request -> Entity
    Apply Update Request -> Entity

Be careful with the Pochi relationship.

The service layer should resolve the Pochi entity by ID.

Do not create a fake/detached Pochi entity in the mapper simply to populate the relationship.

---

# 15. Repository

Create:

    SettlementRuleRepository

extending the repository type already used by the project.

The repository must support:

- Standard CRUD operations.
- Finding rules by Pochi.
- Finding general rules.
- Finding rules by shift count.
- Finding an applicable rule for a date.
- Detecting overlapping validity periods.
- Excluding the current rule during overlap validation on update.

Prefer clear Spring Data JPA methods when practical.

Use explicit JPQL queries when they make temporal or NULL logic easier to understand.

Do not attempt to reproduce business logic inside controllers.

---

# 16. Service Layer

Follow the existing service pattern:

    SettlementRuleService
    SettlementRuleServiceImpl

The service layer owns the business logic.

Responsibilities include:

- Create settlement rule.
- Update settlement rule.
- Find rule by ID.
- List rules.
- Validate Pochi when provided.
- Validate date ranges.
- Prevent overlapping rules.
- Resolve the applicable settlement rule.
- Preserve historical rules.

The controller MUST NOT implement these business rules.

---

# 17. Rule Resolution Service Method

Provide a service operation conceptually equivalent to:

    resolveApplicableRule(
        Integer pochiId,
        Integer shiftCount,
        LocalDate date
    )

The implementation should:

    1. Validate the requested Pochi.
    2. Look for a Pochi-specific applicable rule.
    3. If found, return it.
    4. Otherwise look for the general applicable rule.
    5. If found, return it.
    6. Otherwise throw a business exception.

This operation will be important when the Settlements module is implemented.

Keep this logic reusable and outside the controller.

---

# 18. Controller

Create a REST controller following the conventions of the existing modules.

Suggested base path:

    /api/settlement-rules

Provide operations appropriate for the module, such as:

    POST   /api/settlement-rules
    GET    /api/settlement-rules/{id}
    GET    /api/settlement-rules
    PUT    /api/settlement-rules/{id}
    PATCH  /api/settlement-rules/{id}

The PATCH endpoint updates only `validTo`. It accepts a required date in the request body:

    {
      "validTo": "2026-11-30"
    }

`validTo` is inclusive. The date must be on or after the rule's `validFrom`, and the change must not create an overlap with another rule in the same scope. The endpoint returns the updated Settlement Rule. Use the existing PUT endpoint when changing other rule fields.

A DELETE endpoint should NOT automatically hard-delete historical settlement rules.

Prefer preserving historical records.

If deletion is implemented, it must not allow historical integrity to be destroyed.

Do not invent soft-delete behavior unless the existing project has an established convention for it.

---

# 19. Filtering

The list endpoint should support useful optional filters consistent with the existing project style.

Candidates include:

    pochiId
    shiftCount
    validOn

For example:

    GET /api/settlement-rules?pochiId=82

    GET /api/settlement-rules?shiftCount=2

    GET /api/settlement-rules?validOn=2026-11-11

Do not over-engineer filtering.

Only implement filters that fit naturally with the current repository/service conventions.

---

# 20. Exceptions

Reuse the project's existing exception-handling architecture.

Business error cases include:

- Settlement Rule not found.
- Pochi not found.
- Invalid validity period.
- Overlapping Settlement Rule.
- No applicable Settlement Rule configured.

Do not create duplicate global exception infrastructure if the project already has it.

---

# 21. Unit Tests

Add unit tests following the same testing conventions already established in the project.

At minimum, test:

## Creation

- Create a valid general one-shift rule.
- Create a valid general two-shift rule.
- Create a valid Pochi-specific rule.
- Reject invalid shift count.
- Reject zero or negative amount.
- Reject validTo before validFrom.
- Reject unknown Pochi.
- Reject overlapping general rule.
- Reject overlapping Pochi-specific rule.

## Update

- Update a valid rule.
- PATCH only `validTo` and verify all other rule fields remain unchanged.
- Reject a PATCH date before `validFrom`.
- Reject a PATCH that causes an overlap.
- Reject update causing overlap.
- Ensure overlap query excludes the current rule.
- Reject unknown Pochi during update.

## Resolution

Test the most important business behavior:

### Specific rule wins

Given:

    General:
    2 shifts -> $400

    Pochi 82:
    2 shifts -> $350

When resolving:

    pochiId = 82
    shiftCount = 2

Then:

    $350

must be returned.

### General fallback

Given:

    General:
    2 shifts -> $400

and no specific Pochi rule,

When resolving:

    Pochi 16
    2 shifts

Then:

    $400

must be returned.

### No applicable rule

When neither a specific nor general applicable rule exists:

The service must throw the appropriate business exception.

### Date boundaries

Verify:

    date == validFrom -> applies

    date == validTo -> applies

    date < validFrom -> does not apply

    date > validTo -> does not apply

Test open-ended `validTo = null` rules as well.

---

# 22. Architecture

Follow the same project architecture used by the completed modules.

Expected flow:

    Controller
        |
        v
    Service Interface
        |
        v
    Service Implementation
        |
        +---- Repository
        |
        +---- Mapper
        |
        +---- Pochi validation
        |
        v
    PostgreSQL

Keep responsibilities separated.

Controller:
HTTP concerns only.

Service:
Business rules.

Repository:
Persistence queries.

Mapper:
Entity/DTO mapping.

Entity:
Persistence model.

DTO:
API contract.

---

# 23. Existing Project Conventions

IMPORTANT:

Before generating code, inspect and reuse the conventions already implemented in:

- Users
- Pochis
- Drivers
- Pochi Assignments

The Settlement Rules module MUST feel like another module of the same application, not an independently designed sample project.

Reuse existing:

- Package structure.
- Naming conventions.
- DTO conventions.
- Response models.
- Exception handling.
- Validation style.
- Mapper configuration.
- Lombok conventions.
- JPA conventions.
- Timestamp handling.
- Controller style.
- Pagination/filtering conventions, when applicable.
- Testing conventions.

Do not introduce a new architecture, generic framework, base repository, abstraction layer, or dependency unless strictly necessary.

---

# 24. Important Boundaries

DO NOT implement:

- Settlements.
- Settlement details.
- Driver debts.
- Debt payments.
- Expenses.
- Reports.
- Rain/weather adjustments.
- Administrator-authorized discounts.
- Settlement locking.
- Payment processing.
- Automatic assignment of settlements to Pochis.

Those concepts will be implemented in their corresponding modules.

This module has one clear responsibility:

> Define and resolve the expected settlement amount according to the applicable historical business rule.

---

# 25. Definition of Done

The module is complete when:

1. SettlementRule entity maps correctly to `settlement_rules`.
2. General rules support `pochi_id = NULL`.
3. Pochi-specific rules reference existing Pochis.
4. Create/update/response DTOs exist.
5. MapStruct mapping follows the project pattern.
6. Repository queries correctly handle NULL Pochi rules.
7. Overlapping validity periods are rejected.
8. Historical rules are preserved.
9. Pochi-specific rules take priority over general rules.
10. General rules are used as fallback.
11. Missing applicable rules produce a business error.
12. Controllers contain no business logic.
13. Monetary values use BigDecimal.
14. Validation follows existing project conventions.
15. Unit tests cover rule creation, overlap detection, dates, and rule resolution.
16. Existing project tests continue to pass.
17. No unrelated module or architecture is modified.