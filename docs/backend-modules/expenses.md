# Expenses Module

## 1. Objective

Implement the **Expenses** module for AdministraPochi using the existing project architecture and conventions.

An Expense represents a real business expense. It is classified by an existing `ExpenseType`, may be associated with one `Pochi`, records who paid and who registered it, and keeps cancellation history instead of being physically deleted.

This module must remain simple. Do not add providers, beneficiaries, receipts, attachments, shared-expense allocations, or multi-Pochi relationships.

---

## 2. Terminology

Use the domain term **Pochi** everywhere:

- Java classes and properties: `Pochi`, `pochi`, `pochiId`
- Database relation: `pochis`
- API fields: `pochiId`
- Messages and documentation: `Pochi`

Do not use `Vehicle` or `vehicle`.

---

## 3. Existing database table

The table already exists. Do not create a new migration or modify its structure unless explicitly requested.

```sql
CREATE TABLE expenses (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    expense_type_id INTEGER NOT NULL,
    pochi_id INTEGER NULL,

    amount NUMERIC(12, 2) NOT NULL,
    expense_date DATE NOT NULL,

    paid_by_user_id INTEGER NOT NULL,
    registered_by_user_id INTEGER NOT NULL,

    description TEXT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    cancellation_reason TEXT NULL,
    cancelled_at TIMESTAMP NULL,
    cancelled_by_user_id INTEGER NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_expenses_expense_type
        FOREIGN KEY (expense_type_id) REFERENCES expense_types(id),

    CONSTRAINT fk_expenses_pochi
        FOREIGN KEY (pochi_id) REFERENCES pochis(id),

    CONSTRAINT fk_expenses_paid_by_user
        FOREIGN KEY (paid_by_user_id) REFERENCES users(id),

    CONSTRAINT fk_expenses_registered_by_user
        FOREIGN KEY (registered_by_user_id) REFERENCES users(id),

    CONSTRAINT fk_expenses_cancelled_by_user
        FOREIGN KEY (cancelled_by_user_id) REFERENCES users(id),

    CONSTRAINT chk_expenses_amount_positive
        CHECK (amount > 0),

    CONSTRAINT chk_expenses_status
        CHECK (status IN ('ACTIVE', 'CANCELLED')),

    CONSTRAINT chk_expenses_cancellation
        CHECK (
            (
                status = 'ACTIVE'
                AND cancellation_reason IS NULL
                AND cancelled_at IS NULL
                AND cancelled_by_user_id IS NULL
            )
            OR
            (
                status = 'CANCELLED'
                AND cancellation_reason IS NOT NULL
                AND cancelled_at IS NOT NULL
                AND cancelled_by_user_id IS NOT NULL
            )
        )
);
```

Existing indexes:

```sql
CREATE INDEX idx_expenses_expense_date ON expenses(expense_date);
CREATE INDEX idx_expenses_expense_type_id ON expenses(expense_type_id);
CREATE INDEX idx_expenses_pochi_id ON expenses(pochi_id);
CREATE INDEX idx_expenses_status ON expenses(status);
```

---

## 4. Domain model

Create an `Expense` entity mapped to `expenses`.

Recommended fields:

```java
Integer id;
ExpenseType expenseType;
Pochi pochi;
BigDecimal amount;
LocalDate expenseDate;
User paidByUser;
User registeredByUser;
String description;
ExpenseStatus status;
String cancellationReason;
LocalDateTime cancelledAt;
User cancelledByUser;
LocalDateTime createdAt;
LocalDateTime updatedAt;
```

### Mapping requirements

- Use `Integer` identifiers to match the current schema.
- Use `BigDecimal` for `amount`. Never use `double` or `float` for money.
- Map `expenseType` with `@ManyToOne(fetch = FetchType.LAZY)` and `optional = false`.
- Map `pochi` with `@ManyToOne(fetch = FetchType.LAZY)` and allow `null`.
- Map `paidByUser`, `registeredByUser`, and `cancelledByUser` as relationships to `User`.
- Map `status` with `@Enumerated(EnumType.STRING)`.
- Map table and column names explicitly when needed.
- Follow the same timestamp strategy already used by the other AdministraPochi modules.
- Do not expose JPA entities directly from controllers.

---

## 5. Expense status

Create:

```java
public enum ExpenseStatus {
    ACTIVE,
    CANCELLED
}
```

A newly created Expense is always `ACTIVE`.

Clients must not choose the initial status and must not change it through the normal update endpoint.

---

## 6. Temporary user rule

Authentication is not implemented yet. Use the same temporary-user strategy currently used by the other modules.

```java
private static final Integer TEMPORARY_USER_ID = 2;
```

For now:

- `paidByUser` is resolved with `TEMPORARY_USER_ID`.
- `registeredByUser` is resolved with `TEMPORARY_USER_ID`.
- `cancelledByUser` is resolved with `TEMPORARY_USER_ID` when cancelling.
- These user IDs must not be accepted from the request body.
- Load the `User` through `UserRepository` and fail clearly if user `1` does not exist.
- Keep this logic isolated and clearly marked as temporary so it can later be replaced by the authenticated user provider.

Do not implement authentication or recreate deleted authentication classes as part of this module.

---

## 7. Business rules

### 7.1 Expense Type

- `expenseTypeId` is required.
- The referenced `ExpenseType` must exist.
- The Expense Type must be active if the existing module supports active/inactive state.
- Read `requiresPochi` from the selected Expense Type.

### 7.2 Pochi requirement

Apply this exact rule:

```text
requiresPochi = true  -> pochiId is required
requiresPochi = false -> pochiId is optional
```

`requiresPochi = false` does not prohibit association with a Pochi.

When `pochiId` is present:

- The Pochi must exist.
- Apply the same active/inactive validation convention used by the existing Pochi module.

### 7.3 Amount

- Required.
- Must be greater than zero.
- Use two decimal places consistently.
- Do not use negative amounts to represent cancellations or corrections.

### 7.4 Expense date

- Required.
- Represents the real date on which the expense occurred.
- It is independent from `createdAt`, which represents when it was registered in the system.
- Allow past dates.
- Reject future dates unless the existing project has an explicit rule allowing them.

### 7.5 Description

- Optional.
- Trim surrounding whitespace.
- Convert blank input to `null`.
- Use it for operational details, including exceptional shared/global expenses.

Example:

```text
Compra de Pochi usado como donador de piezas para Pochi 16 y Pochi 25.
```

### 7.6 Global and exceptional shared expenses

The model supports at most one direct Pochi association.

- A normal Pochi-specific expense uses `pochiId`.
- A global expense uses `pochiId = null`.
- An exceptional expense benefiting multiple Pochis is registered as global with `pochiId = null`, and the involved Pochis are explained in `description`.

Do not create `expense_pochis`, `expense_allocations`, or another many-to-many structure.

### 7.7 Cancellation

- Do not physically delete Expenses.
- Only an `ACTIVE` Expense can be cancelled.
- Cancelling an already `CANCELLED` Expense must return a business validation error.
- A nonblank `cancellationReason` is required.
- On cancellation set:
  - `status = CANCELLED`
  - `cancellationReason`
  - `cancelledAt = current date-time`
  - `cancelledByUser = temporary user`
  - `updatedAt = current date-time`
- Cancellation data must not be accepted through create or regular update requests.
- Do not implement reactivation unless explicitly requested later.

### 7.8 Updating

- Allow updating only an `ACTIVE` Expense.
- Revalidate Expense Type, `requiresPochi`, Pochi, amount, date, and description.
- Do not change `registeredByUser`, `status`, or cancellation audit fields through update.
- Keep `createdAt` unchanged.
- Refresh `updatedAt`.

### 7.9 Closed periods

AdministraPochi has a domain rule that financial records within a closed period must not be modified or deleted.

If the project already has a reusable closed-period validation, use it before update or cancellation. If it does not exist yet, do not invent a new closure subsystem inside Expenses; leave a clear integration point or TODO consistent with the current architecture.

---

## 8. DTOs

Use DTOs consistent with the existing modules. Suggested separation:

### CreateExpenseRequest

```java
@NotNull Integer expenseTypeId;
Integer pochiId;
@NotNull @DecimalMin(value = "0.01") BigDecimal amount;
@NotNull LocalDate expenseDate;
String description;
```

Do not include user IDs, status, cancellation fields, timestamps, or entity objects.

### UpdateExpenseRequest

```java
@NotNull Integer expenseTypeId;
Integer pochiId;
@NotNull @DecimalMin(value = "0.01") BigDecimal amount;
@NotNull LocalDate expenseDate;
String description;
```

### CancelExpenseRequest

```java
@NotBlank String cancellationReason;
```

### ExpenseResponse

Return enough information for later Reports and frontend use without exposing entities. Include at least:

```java
Integer id;
Integer expenseTypeId;
String expenseTypeCode;
String expenseTypeName;
Integer pochiId;
String pochiIdentifier; // Use the actual display field from the existing Pochi model
BigDecimal amount;
LocalDate expenseDate;
Integer paidByUserId;
String paidByUserName;
Integer registeredByUserId;
String registeredByUserName;
String description;
ExpenseStatus status;
String cancellationReason;
LocalDateTime cancelledAt;
Integer cancelledByUserId;
String cancelledByUserName;
LocalDateTime createdAt;
LocalDateTime updatedAt;
```

For nullable relations or cancellation data, return `null` safely.

Do not invent a Pochi property name. Inspect the existing `Pochi` entity/response and use its actual human-readable field.

---

## 9. Repository

Create `ExpenseRepository extends JpaRepository<Expense, Integer>` and support filtering without loading all rows into memory.

Use the filtering/pagination pattern already established by the project, preferably `JpaSpecificationExecutor<Expense>` if that is already used.

Supported optional filters should include:

- `expenseTypeId`
- `pochiId`
- `status`
- `expenseDateFrom`
- `expenseDateTo`

Date range behavior:

- `expenseDateFrom` is inclusive.
- `expenseDateTo` is inclusive.
- If both are present, `expenseDateFrom` cannot be after `expenseDateTo`.

Default sorting recommendation:

```text
expenseDate DESC, id DESC
```

Use `Page`/`Pageable` if pagination is the existing application convention.

---

## 10. Service

Create service interface and implementation following the existing architecture.

Expected operations:

```java
ExpenseResponse create(CreateExpenseRequest request);
ExpenseResponse findById(Integer id);
Page<ExpenseResponse> findAll(...filters..., Pageable pageable);
ExpenseResponse update(Integer id, UpdateExpenseRequest request);
ExpenseResponse cancel(Integer id, CancelExpenseRequest request);
```

The service owns all business validation. Controllers must remain thin.

Recommended internal helpers:

```java
private ExpenseType findExpenseTypeOrThrow(Integer id);
private Pochi findPochiOrThrow(Integer id);
private User findTemporaryUserOrThrow();
private void validatePochiRequirement(ExpenseType type, Integer pochiId);
private void validateDateRange(LocalDate from, LocalDate to);
private void validateExpenseIsActive(Expense expense);
private String normalizeNullableText(String value);
```

Use `@Transactional` for create, update, and cancel. Use read-only transactions for queries where consistent with the project.

---

## 11. Mapper

Use MapStruct according to the project conventions.

- Map entity to response DTO.
- Handle nested IDs and display fields explicitly.
- Ignore server-controlled fields when applying an update request.
- Do not let MapStruct overwrite relationships before they have been validated and resolved by the service.
- Avoid triggering unnecessary lazy-loading loops.

---

## 12. REST controller

Suggested base path:

```text
/api/expenses
```

Suggested endpoints:

```http
POST   /api/expenses
GET    /api/expenses/{id}
GET    /api/expenses
PUT    /api/expenses/{id}
PATCH  /api/expenses/{id}/cancel
```

Do not expose a physical `DELETE` endpoint.

Suggested responses:

- Create: `201 Created`
- Find/list/update/cancel: `200 OK`
- Validation failure: use the existing global exception-handler convention
- Missing Expense, Expense Type, Pochi, or temporary User: `404 Not Found` according to existing conventions
- Invalid state or business rule: use the project's existing business exception/status convention

Example create request:

```json
{
  "expenseTypeId": 1,
  "pochiId": 16,
  "amount": 850.00,
  "expenseDate": "2026-11-17",
  "description": "Cambio de balatas delanteras"
}
```

Example global/shared exceptional expense:

```json
{
  "expenseTypeId": 5,
  "pochiId": null,
  "amount": 20000.00,
  "expenseDate": "2026-11-17",
  "description": "Compra de Pochi usado como donador de piezas para Pochi 16 y Pochi 25"
}
```

Example cancellation request:

```json
{
  "cancellationReason": "Gasto registrado por duplicado"
}
```

---

## 13. Reports preparation

The module must remain queryable for future Reports.

Operational financial totals must normally include only:

```text
status = ACTIVE
```

Reports will use:

- `amount` for totals
- `expenseDate` for period grouping
- `expenseType` for category grouping
- `pochi` for Pochi-specific grouping
- `pochi = null` for global expenses
- `paidByUser` for who made the disbursement
- `status` to exclude cancelled records from normal totals

Do not create the Reports module or report-specific aggregate endpoints as part of this implementation.

---

## 14. Explicitly out of scope

Do not implement:

- Vendor or beneficiary fields
- Supplier catalog
- Receipts or file attachments
- File storage
- Multi-Pochi relationships
- Expense allocations or automatic distribution between Owners
- Physical deletion
- Reactivation of cancelled Expenses
- Authentication/security redesign
- Reports
- A generic financial-movements table

---

## 15. Tests

Create unit tests consistent with the existing test conventions. Cover at least:

1. Creates an `ACTIVE` Expense with valid data.
2. Assigns the temporary user as payer and registrar.
3. Rejects amount equal to or below zero.
4. Rejects an unknown Expense Type.
5. Requires `pochiId` when `requiresPochi = true`.
6. Allows `pochiId = null` when `requiresPochi = false`.
7. Allows a Pochi when `requiresPochi = false`.
8. Rejects an unknown Pochi.
9. Updates an active Expense and preserves server-controlled fields.
10. Cancels an active Expense and fills every cancellation field.
11. Requires a nonblank cancellation reason.
12. Rejects cancellation of an already cancelled Expense.
13. Rejects update of a cancelled Expense.
14. Filters by status, Expense Type, Pochi, and inclusive date range.
15. Rejects an inverted date range.
16. Safely maps nullable Pochi and cancellation data.

Do not let test expansion block the basic working implementation, but keep the service structured so these cases can be tested cleanly.

---

## 16. Implementation checklist

- [ ] `ExpenseStatus`
- [ ] `Expense` entity
- [ ] Request and response DTOs
- [ ] `ExpenseRepository`
- [ ] Filtering specification/query support
- [ ] MapStruct mapper
- [ ] Service interface
- [ ] Service implementation
- [ ] Temporary user resolution with ID `1`
- [ ] `requiresPochi` business validation
- [ ] Create operation
- [ ] Find-by-ID operation
- [ ] Paginated/filterable listing
- [ ] Update operation for active Expenses
- [ ] Cancellation operation
- [ ] No physical delete endpoint
- [ ] Global exception handling integration
- [ ] Unit tests
- [ ] Compile and run existing tests

---

## 17. Final verification

Before finishing:

1. Confirm the project compiles.
2. Confirm existing tests still pass.
3. Confirm an active Expense is inserted with all cancellation fields as `null`.
4. Confirm cancellation fills all cancellation fields atomically.
5. Confirm the API never accepts `paidByUserId`, `registeredByUserId`, or `cancelledByUserId` from the caller.
6. Confirm `requiresPochi` is enforced in the service.
7. Confirm there is no physical delete endpoint.
8. Summarize created and modified files and note any assumption made from existing code.
