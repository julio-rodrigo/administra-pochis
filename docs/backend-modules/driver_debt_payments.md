# Driver Debts Module

## Overview

The Driver Debts module manages monetary amounts that a driver owes.

A driver debt represents any monetary amount that an administrator decides must remain outstanding for a driver.

A debt may optionally originate from a Settlement, but debts are not limited to Settlements. Administrators may also create debts manually for other business reasons.

The module also manages Debt Payments.

Debt payments represent individual monetary payments made by a driver toward an existing debt.

This module must preserve financial traceability. Once financial records have been created, they must not be deleted or freely modified.

---

# Database Tables

The module is based on two tables:

- `driver_debts`
- `driver_debt_payments`

Relationship:

Driver

1 → N Driver Debts

Driver Debt

1 → N Driver Debt Payments

Settlement

1 → 0..N Driver Debts

A Driver Debt may exist without a Settlement.

---

# Driver Debt

## Entity

Create:

`DriverDebt`

mapped to:

`driver_debts`

Main fields:

- `id`
- `driver`
- `settlement`
- `amount`
- `status`
- `description`
- `debtDate`
- `createdBy`
- `cancellationReason`
- `cancelledAt`
- `cancelledBy`
- `createdAt`
- `updatedAt`

Use the existing project conventions for JPA entities and Lombok.

---

# Driver Debt Status

Create the following Java enum:

`DriverDebtStatus`

Values:

- `PENDING`
- `PARTIALLY_PAID`
- `PAID`
- `CANCELLED`

Store the enum using:

`EnumType.STRING`

Do not use ordinal enum storage.

Meaning:

## PENDING

The debt exists but no payment has been registered.

## PARTIALLY_PAID

At least one payment has been registered, but the total amount paid is less than the original debt amount.

## PAID

The total amount paid equals the original debt amount.

## CANCELLED

The debt was cancelled before any payments were registered.

---

# Debt Amount

The `amount` field represents the original amount of the debt.

The debt must always satisfy:

`amount > 0`

Do NOT store:

- `paidAmount`
- `remainingAmount`

These values must be calculated from the associated Debt Payments.

Calculate:

`paidAmount = SUM(driver_debt_payments.amount)`

Calculate:

`remainingAmount = debt.amount - paidAmount`

Use `BigDecimal` for all monetary calculations.

Never use `double` or `float` for monetary values.

---

# Settlement Relationship

A Driver Debt may optionally reference a Settlement.

Therefore:

`settlement_id` is nullable.

A debt associated with a Settlement means that the debt originated from or was identified during that Settlement.

IMPORTANT:

A difference between the expected Settlement amount and the received Settlement amount MUST NOT automatically generate a debt.

Example:

Expected Settlement:

`$400`

Amount received:

`$300`

Difference:

`$100`

The system must NOT assume that the driver owes `$100`.

The administrator decides:

1. Whether a debt should be created.
2. The actual amount of the debt.

For example:

Expected:

`$400`

Received:

`$300`

The administrator may decide:

- No debt.
- Debt of `$50`.
- Debt of `$100`.
- Another valid amount according to the business situation.

Do not derive the Driver Debt amount automatically from the Settlement difference.

Do not validate that the Driver Debt amount equals the Settlement difference.

The administrator has the final decision regarding whether a debt exists and its amount.

---

# Manual Debts

A Driver Debt may be created without a Settlement.

Example:

`settlement = null`

This allows administrators to register other monetary obligations owed by a driver.

The module must therefore NOT require a Settlement when creating a Driver Debt.

A Driver is always required.

---

# Creating Driver Debts

Create a DTO following the existing project conventions:

`DriverDebtCreateRequest`

It should contain the information required to create a debt.

Suggested fields:

- `driverId`
- `settlementId`
- `amount`
- `description`
- `debtDate`

`settlementId` must be optional.

Validation:

- Driver must exist.
- Settlement must exist when `settlementId` is provided.
- `amount` must be greater than zero.
- Description must not be blank.
- Debt date must be valid.

New Driver Debts must always start with:

`PENDING`

The client must NOT decide the initial status.

The backend controls debt status.

---

# Updating Driver Debts

Driver Debts represent financial records.

Do NOT provide a general update operation that allows financial information to be freely modified.

After creation, do not allow modification of:

- `driver`
- `settlement`
- `amount`
- `debtDate`

The original financial information must remain immutable.

If there is currently no business requirement to modify debt metadata, do not create a generic PUT endpoint.

Prefer explicit business operations instead of unrestricted CRUD operations.

---

# Cancelling Driver Debts

Do NOT physically delete Driver Debts.

There must NOT be a normal DELETE operation for debts.

A debt may be cancelled only when no Debt Payments exist for that debt.

Cancellation changes:

`status = CANCELLED`

And records:

- `cancellationReason`
- `cancelledAt`
- `cancelledBy`

Cancellation reason is required.

The backend must verify that:

`SUM(payments) == 0`

before allowing cancellation.

If payments already exist, reject the cancellation.

A CANCELLED debt cannot receive payments.

---

# Driver Debt Payments

## Entity

Create:

`DriverDebtPayment`

mapped to:

`driver_debt_payments`

Fields:

- `id`
- `driverDebt`
- `amount`
- `paymentDate`
- `notes`
- `createdBy`
- `createdAt`

Follow the existing project conventions for JPA entities and Lombok.

---

# Creating Debt Payments

Create:

`DriverDebtPaymentCreateRequest`

Suggested fields:

- `amount`
- `paymentDate`
- `notes`

The debt ID should preferably be obtained from the endpoint path.

Example:

POST `/api/driver-debts/{debtId}/payments`

The authenticated/admin user responsible for the operation should determine `createdBy` according to the project's existing user/auditing pattern.

Payment validation:

- Debt must exist.
- Debt status must not be `PAID`.
- Debt status must not be `CANCELLED`.
- Payment amount must be greater than zero.
- Payment amount must not exceed the current remaining debt balance.

Calculate:

`remainingAmount = debt.amount - SUM(existing payments)`

Require:

`paymentAmount <= remainingAmount`

Overpayments are not allowed.

---

# Partial Payments

Partial payments are allowed.

Example:

Debt:

`$500`

First payment:

`$100`

Result:

Paid:

`$100`

Remaining:

`$400`

Status:

`PARTIALLY_PAID`

Second payment:

`$250`

Result:

Paid:

`$350`

Remaining:

`$150`

Status remains:

`PARTIALLY_PAID`

Final payment:

`$150`

Result:

Paid:

`$500`

Remaining:

`$0`

Status:

`PAID`

---

# Debt Status Transitions

The backend is responsible for controlling status transitions.

Valid normal lifecycle:

`PENDING -> PARTIALLY_PAID -> PAID`

A debt may also transition directly:

`PENDING -> PAID`

when the first payment covers the complete debt amount.

Cancellation:

`PENDING -> CANCELLED`

A debt with registered payments must not be cancelled.

The client must not directly change the debt status.

---

# Status Recalculation After Payment

After successfully creating a Debt Payment, recalculate the total paid amount.

Calculate:

`totalPaid = SUM(all payments for the debt)`

Then:

If:

`totalPaid == debt.amount`

set:

`status = PAID`

Otherwise, if:

`totalPaid > 0`

set:

`status = PARTIALLY_PAID`

The payment creation and debt status update must execute inside the same transactional operation.

Use:

`@Transactional`

at the appropriate service layer.

---

# Payment Immutability

Debt Payments represent historical financial movements.

Once created:

- Payments must NOT be updated.
- Payments must NOT be deleted.

Do not create:

- PUT payment endpoint
- PATCH payment endpoint
- DELETE payment endpoint

For the current version of the system, payment corrections or reversals are outside the scope of this module.

Do not implement reversal, void, refund, write-off, forgiveness, or adjustment functionality unless explicitly requested later.

---

# Response DTO

Create:

`DriverDebtResponse`

The response should expose useful calculated financial information.

Suggested fields:

- `id`
- `driverId`
- driver information according to existing DTO conventions
- `settlementId`
- `amount`
- `paidAmount`
- `remainingAmount`
- `status`
- `description`
- `debtDate`
- `createdBy`
- `cancellationReason`
- `cancelledAt`
- `cancelledBy`
- `createdAt`
- `updatedAt`

`paidAmount` and `remainingAmount` are calculated fields.

They are NOT database columns in `driver_debts`.

Also create an appropriate response DTO for Debt Payments.

Suggested:

`DriverDebtPaymentResponse`

Fields:

- `id`
- `driverDebtId`
- `amount`
- `paymentDate`
- `notes`
- `createdBy`
- `createdAt`

---

# Repository

Create:

`DriverDebtRepository`

and:

`DriverDebtPaymentRepository`

Use Spring Data JPA.

The repositories must support:

- Finding debts by driver.
- Finding debts by status.
- Finding debts by Settlement when needed.
- Retrieving payments belonging to a debt.
- Calculating the total amount paid for a debt.

An example conceptual aggregate operation is:

`SUM(payment.amount) WHERE payment.driverDebt.id = :debtId`

Handle debts without payments correctly.

When there are no payments:

`paidAmount = 0`

Do not return `null` as the effective paid amount.

---

# Service Layer

Follow the existing project architecture:

- Service interface.
- Service implementation.

Suggested services:

`DriverDebtService`

`DriverDebtServiceImpl`

The Service layer owns the business rules.

The Controller must not implement debt calculations or state transition logic.

Important Service operations include:

- Create debt.
- Find debt by ID.
- Search/list debts.
- Register payment.
- Retrieve debt payments.
- Cancel debt.

Keep business logic centralized in the Service layer.

---

# Payment Transaction Safety

Registering a payment changes both:

1. Debt Payment history.
2. Driver Debt status.

These operations must be transactional.

Conceptually:

1. Retrieve debt.
2. Validate current status.
3. Calculate existing payments.
4. Calculate remaining amount.
5. Validate requested payment.
6. Save Debt Payment.
7. Recalculate total paid.
8. Update Driver Debt status.
9. Return updated debt/payment information.

The entire operation must succeed or fail as one transaction.

---

# Controllers

Follow the REST conventions already used by the project.

Suggested base endpoint:

`/api/driver-debts`

Possible endpoints:

GET `/api/driver-debts`

GET `/api/driver-debts/{id}`

POST `/api/driver-debts`

POST `/api/driver-debts/{id}/payments`

GET `/api/driver-debts/{id}/payments`

PATCH `/api/driver-debts/{id}/cancel`

Do NOT expose:

DELETE `/api/driver-debts/{id}`

Do NOT expose:

DELETE `/api/driver-debts/{debtId}/payments/{paymentId}`

Do NOT expose unrestricted payment update endpoints.

---

# Debt Search / Filtering

The debt listing should support useful administrative filters consistent with the existing project patterns.

Suggested optional filters:

- `driverId`
- `settlementId`
- `status`
- `debtDateFrom`
- `debtDateTo`

Do not overengineer the search implementation.

Use the same filtering/pagination approach already established by previous modules whenever applicable.

---

# Mapper

Use MapStruct following the existing project conventions.

Create appropriate mappers for:

- `DriverDebt`
- `DriverDebtPayment`

Do not place business calculations inside MapStruct when they require repository access.

Values such as:

- `paidAmount`
- `remainingAmount`

should be calculated by the Service or by an appropriate query/projection and then included in the response.

MapStruct should remain responsible primarily for object mapping.

---

# Exception Handling

Use the project's existing global exception handling conventions.

Business validation errors should produce appropriate HTTP responses.

Examples:

Trying to pay a PAID debt:

`Debt is already fully paid.`

Trying to pay a CANCELLED debt:

`Cancelled debts cannot receive payments.`

Trying to overpay:

`Payment amount cannot exceed the remaining debt balance.`

Trying to cancel a debt with payments:

`A debt with registered payments cannot be cancelled.`

Trying to create a debt with an invalid driver:

`Driver not found.`

Trying to use an invalid Settlement:

`Settlement not found.`

Do not create a completely separate exception architecture for this module if the project already provides reusable exception handling.

---

# Financial Precision

Use `BigDecimal` for all monetary values.

Compare monetary values using `BigDecimal.compareTo()`.

Do not use:

`==`

for BigDecimal value comparisons.

Do not use:

- `double`
- `float`

for financial calculations.

Database monetary values use:

`NUMERIC(10,2)`

according to the current database schema.

---

# Concurrency Consideration

Payment registration must execute transactionally.

The implementation should avoid a situation where multiple simultaneous payments can independently validate against the same remaining balance and produce an overpayment.

Use a simple approach consistent with the current project's scale.

If necessary, use a pessimistic write lock when retrieving a debt for payment processing.

Do not introduce unnecessary distributed locking or complex infrastructure.

---

# Tests

Create unit tests following the existing testing conventions of the project.

At minimum test:

## Debt Creation

- Create debt associated with Settlement.
- Create debt without Settlement.
- Reject zero amount.
- Reject negative amount.
- Reject nonexistent driver.
- Reject nonexistent Settlement.
- New debt starts as PENDING.

## Payments

- Register partial payment.
- Multiple partial payments.
- Full payment.
- Direct full payment of a PENDING debt.
- Reject zero payment.
- Reject negative payment.
- Reject overpayment.
- Reject payment on PAID debt.
- Reject payment on CANCELLED debt.

## Status

- PENDING becomes PARTIALLY_PAID after partial payment.
- PENDING becomes PAID after complete payment.
- PARTIALLY_PAID becomes PAID when remaining balance is paid.

## Cancellation

- Cancel PENDING debt with no payments.
- Require cancellation reason.
- Reject cancellation when payments exist.
- Reject payments after cancellation.

## Calculations

Verify:

`paidAmount = SUM(payments)`

Verify:

`remainingAmount = debt.amount - paidAmount`

A debt without payments must return:

`paidAmount = 0`

and:

`remainingAmount = debt.amount`

---

# Scope Restrictions

For this version, DO NOT implement:

- Automatic debt creation from Settlements.
- Automatic debt amount calculation from Settlement differences.
- Payment deletion.
- Payment editing.
- Debt deletion.
- Debt forgiveness.
- Debt write-offs.
- Payment reversals.
- Refunds.
- Interest.
- Late fees.
- Installment schedules.
- Automatic due dates.
- Collections workflows.

Implement only the business rules documented in this file.

Keep the module simple, explicit, auditable, and consistent with the existing AdministraPochi architecture.