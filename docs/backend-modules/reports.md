# Reports Module

## 1. Purpose

The Reports module provides read-only, dynamically generated views of operational and financial data already stored by AdministraPochi.

The first and primary report is the **Weekly Owner Report**, which explains what happened with the Owner's Pochis from Monday through Sunday and how the final amount payable to the Owner was calculated.

Reports must aggregate existing historical records. Reports must not create, update, cancel, delete, lock, or duplicate business records.

---

## 2. Scope

The first version must support:

1. Admin preview of an `OPEN` week.
2. Full Weekly Owner Report for an `ADMIN_CLOSED` or `OWNER_CLOSED` week.
3. Lookup by `week_year` and `week_number`.
4. Lookup by an exact Monday-to-Sunday date range.
5. Weekly history with lightweight summary records.
6. Weeks pending Owner review/closure.
7. Data-quality warnings for the Admin preview.

The module must not generate PDF or spreadsheet exports in this first version unless requested in a future requirement.

---

## 3. Architectural Principles

### 3.1 Read-only module

Reports may:

- Read existing records.
- Aggregate information from multiple modules.
- Calculate derived totals.
- Build response DTOs.
- Detect and report missing or inconsistent data.
- Provide current and historical views.

Reports must not:

- Create business records.
- Update business records.
- Delete or cancel business records.
- Automatically fix missing data.
- Automatically create Settlements, Administration Charges, Expenses, Driver Debts, Debt Payments, Assignments, Daily Statuses, or Weekly Closings.
- Recalculate historical amounts from Settlement Rules.
- Recalculate historical amounts from Administration Fee Rules.
- Persist report snapshots.

### 3.2 No new persistence

Do not create any of the following:

- `reports`
- `weekly_reports`
- `report_items`
- `report_snapshots`
- Any other Reports-specific table

No SQL migration is required for this module.

New repository query methods, projections, specifications, or read-only database queries may be added to existing modules when necessary.

### 3.3 Historical records are the source of truth

Reports must display the amounts that were actually recorded:

- Use Settlement `expected_amount` and `accepted_amount`.
- Use the real Administration Charge amount.
- Use the real Expense amount.
- Use the real Driver Debt and Debt Payment data.

Settlement Rules and Administration Fee Rules are not normal report data sources. They are calculation rules used before historical records are created. Reports must not use those rules to replace or recalculate recorded history.

---

## 4. Data Sources

| Report information | Source module |
|---|---|
| Pochi identity and Owner association | Pochis |
| Driver identity | Drivers |
| `MORNING` and `AFTERNOON` work | Pochi Assignments |
| `WORKED`, `DAY_OFF`, `UNION_REST`, `NOT_WORKED` | Pochi Daily Status |
| Expected and accepted settlement amounts | Settlements |
| Settlement adjustment explanation | Settlements |
| Real administration amount | Administration Charges |
| Expense type | Expense Types |
| Pochi-related expenses and comments | Expenses |
| Debts generated for Drivers | Driver Debts |
| Money recovered from debts | Debt Payments |
| Week, period, status, and closing comment | Weekly Closing |

---

## 5. Weekly Report Identity

A weekly report represents one full business week:

- Start: Monday
- End: Sunday
- Duration: exactly 7 calendar days, inclusive

The report header must include:

- `weekYear`
- `weekNumber`
- `startDate`
- `endDate`
- Weekly Closing status
- `closingComment`
- `adminClosedAt`
- `ownerClosedAt`

`weekNumber` must never be presented without `weekYear`, because the week number alone is not unique.

The start and end dates are the clearest human-readable representation and must always be returned even when the report was requested by `weekYear` and `weekNumber`.

---

## 6. Access and Visibility Rules

### 6.1 Admin

An Admin may consult weeks with these statuses:

- `OPEN`
- `ADMIN_CLOSED`
- `OWNER_CLOSED`

When the week is `OPEN`, the response is a dynamic preview. The Admin preview may include warnings about missing or inconsistent information.

### 6.2 Owner

An Owner may consult only weeks with these statuses:

- `ADMIN_CLOSED`
- `OWNER_CLOSED`

An Owner must not consult an `OPEN` week.

The report must only include Pochis that belong to the requesting Owner. Data belonging exclusively to other Owners must not be exposed.

### 6.3 Temporary authentication limitation

If real authentication and authorization are not yet implemented, follow the project's existing temporary-user strategy. Keep access-control logic separated and easy to replace later. Do not invent a new authentication subsystem inside Reports.

---

## 7. Weekly Report Structure

The main report is organized in this order:

1. Week and Weekly Closing information.
2. Pochi weekly details.
3. Seven daily entries for each Pochi, Monday through Sunday.
4. Pochi-related Expenses.
5. Driver Debts generated during the week.
6. Debt Payments received during the week.
7. Per-Pochi financial summary.
8. Overall weekly financial summary.
9. Admin warnings, when applicable.

The backend response must provide structured data. The backend must not attempt to reproduce a fixed PDF/table UI layout.

---

## 8. Pochi Daily Information

Each Pochi must contain exactly seven daily entries ordered from Monday to Sunday.

Each daily entry must provide:

- Date
- Day of week
- Daily status
- Daily status comment, when present
- Assignments
- Driver for each assignment
- Settlement associated with each assignment, when present
- Administration Charge related to that Pochi and date, when present

### 8.1 Daily statuses

Supported statuses:

- `WORKED`
- `DAY_OFF`
- `UNION_REST`
- `NOT_WORKED`

The report must use Pochi Daily Status as the source. Do not infer the full daily status only from the existence or absence of Assignments.

For `NOT_WORKED`, return the explanatory comment. The comment is necessary so the Owner understands why the Pochi did not produce income.

If comments exist for other statuses, return those comments as well.

### 8.2 Shifts and frontend interpretation

The backend returns only real assignments:

- `MORNING`
- `AFTERNOON`

Do not create or persist `FULL_DAY` or `HALF_DAY` values.

The frontend may present:

- `MORNING + AFTERNOON` as **Full Day / Día completo**.
- Only `MORNING` as **Half Day / Medio día**.
- Only `AFTERNOON` as **Half Day / Medio día**.

The Drivers for both shifts must remain visible in the daily detail.

The report summary does not need counters such as:

- Total Morning shifts
- Total Afternoon shifts
- Total Assignments

Those values are not required in the primary Weekly Owner Report.

---

## 9. Settlements

Settlements must be shown in the context of their Assignment and shift, not as an unrelated list.

For every Settlement return:

- `settlementId`
- `expectedAmount`
- `acceptedAmount`
- `difference`
- `adjustmentReason`

Calculate:

```text
difference = acceptedAmount - expectedAmount
```

Examples:

```text
expectedAmount = 200.00
acceptedAmount = 150.00
difference = -50.00
```

```text
expectedAmount = 200.00
acceptedAmount = 220.00
difference = 20.00
```

### 9.1 Adjustment reason

When:

```text
expectedAmount != acceptedAmount
```

`adjustmentReason` must be returned and displayed by the client.

The report must make the reason visible so the Owner can understand why the accepted amount differs from the expected amount.

The Reports module must not modify or invent the adjustment reason.

### 9.2 Settlement totals

Calculate both:

```text
expectedSettlements = sum(expectedAmount)
acceptedSettlements = sum(acceptedAmount)
settlementDifference = acceptedSettlements - expectedSettlements
```

All operational money calculations must use `BigDecimal`. Do not use `double` or `float`.

---

## 10. Administration Charges

Reports must use actual Administration Charge records.

Do not recalculate administration amounts from Administration Fee Rules.

Return enough information to identify:

- Charge ID
- Pochi
- Charge date
- Recorded amount
- Any existing relevant comment or adjustment information

Administration Charges reduce the amount payable to the Owner.

```text
totalToOwner = grossReceived - administrationCharges - pochiExpenses
```

---

## 11. Expenses

### 11.1 Inclusion rule

Only Expenses explicitly associated with a Pochi may appear in the Weekly Owner Report.

Include an Expense when all of the following are true:

1. The Expense date belongs to the requested week.
2. The Expense is associated with a Pochi.
3. The Pochi belongs to the Owner represented by the report.
4. The Expense must be included according to its existing active/cancellation business rules.

Expenses with no associated Pochi must not appear in the Weekly Owner Report.

The system must not automatically distribute a global Expense among several Pochis.

If the Admin needs an Expense to affect a specific Owner report, the business data must explicitly associate the appropriate Expense with that Owner's Pochi.

### 11.2 Expense detail

Return:

- `expenseId`
- Date
- Expense Type ID
- Expense Type code
- Expense Type name
- Amount
- Description/comment, when present
- Pochi ID
- Pochi number or display identifier
- Cancellation state or relevant existing status, when applicable

If the Expense contains a description or comment, return it so the Owner understands why the money was deducted.

### 11.3 Expense totals

Only included Pochi Expenses participate in the Weekly Owner Report calculation:

```text
pochiExpenses = sum(included Pochi Expense amounts)
```

---

## 12. Driver Debts Generated

Driver Debts generated during the requested week must appear as explicit informational detail.

Return enough information to identify:

- Debt ID
- Driver ID and name
- Original debt amount
- Current status
- Debt creation/origin date
- Related Settlement, when present
- Origin Pochi, date, and shift, when derivable from the existing relationship
- Existing description/reason, when present

### 12.1 Financial treatment

New Driver Debts do not create an additional subtraction from `totalToOwner`.

Example:

```text
expected settlement = 200.00
accepted settlement = 150.00
new debt = 50.00
```

The missing `50.00` is already absent from `acceptedSettlements`. Subtracting the new debt again would double-count the same shortage.

Therefore:

- New debts are included in `newDriverDebts` for informational visibility.
- New debts are not subtracted again from `grossReceived` or `totalToOwner`.

---

## 13. Debt Payments Received

Debt Payments received during the requested week are real money recovered and must be delivered to the Owner.

Debt Payments must participate in the weekly financial result according to the Payment date, even if the original Driver Debt or Settlement belongs to an older week.

Example:

```text
Original debt date: 2026-11-05
Payment date:       2026-11-19
Payment amount:     100.00
```

The `100.00` belongs financially to the week containing `2026-11-19`.

### 13.1 Required traceability

Do not return only a total. Return detailed traceability for each payment:

- `paymentId`
- `paymentDate`
- `amount`
- Driver ID and name
- Driver Debt ID
- Original debt amount
- Debt balance before payment, if reliably available
- Debt balance after payment, if reliably available
- Debt status
- Related Settlement ID, when present
- Origin Pochi ID and identifier, when present
- Origin date, when present
- Origin shift, when present
- Existing payment comment, description, or registration note, when present

The response must allow the frontend to explain:

> Which Driver paid, how much was paid, which Driver Debt received the payment, and where that debt originated.

Do not invent unavailable historical balance values. If before/after balances cannot be reconstructed reliably from existing data, omit those fields or return them as `null` according to the project's DTO conventions.

### 13.2 Owner and Pochi association

A Debt Payment may be included only when its related Debt belongs to the Owner through its recorded Pochi/Settlement origin or another explicit existing relation.

Do not guess ownership. If ownership cannot be determined reliably from existing relationships, produce an Admin warning and do not expose the payment to an Owner until the data relationship is resolved.

### 13.3 Totals

```text
debtPaymentsReceived = sum(Debt Payments whose paymentDate belongs to the week)
```

Debt Payments increase `grossReceived` and `totalToOwner`.

---

## 14. Financial Calculations

### 14.1 Per-Pochi formulas

```text
expectedSettlements = sum(Pochi Settlement expected amounts)

acceptedSettlements = sum(Pochi Settlement accepted amounts)

settlementDifference = acceptedSettlements - expectedSettlements

debtPaymentsReceived = sum(included Debt Payments associated with the Pochi/Owner)

grossReceived = acceptedSettlements + debtPaymentsReceived

administrationCharges = sum(Pochi Administration Charges)

expenses = sum(included Pochi Expenses)

totalToOwner = grossReceived - administrationCharges - expenses
```

### 14.2 Overall weekly formulas

```text
expectedSettlements = sum(all Pochi expectedSettlements)

acceptedSettlements = sum(all Pochi acceptedSettlements)

settlementDifference = acceptedSettlements - expectedSettlements

debtPaymentsReceived = sum(all included Debt Payments)

grossReceived = acceptedSettlements + debtPaymentsReceived

administrationCharges = sum(all Pochi Administration Charges)

expenses = sum(all included Pochi Expenses)

newDriverDebts = sum(new Driver Debts generated during the week)

totalToOwner = grossReceived - administrationCharges - expenses
```

### 14.3 Important non-duplication rule

`newDriverDebts` is informational and must not be subtracted from `totalToOwner`.

### 14.4 Currency and rounding

- Use `BigDecimal` for all monetary values.
- Apply the same scale and rounding rules already used by the financial modules.
- Do not introduce module-specific floating-point calculations.
- Totals must be calculated by the backend, not trusted from the frontend.

---

## 15. Proposed DTO Contract

Class names may be adjusted to fit existing project conventions, but the functional contract and fields must be preserved.

### 15.1 `WeeklyReportResponse`

```text
WeeklyReportResponse
- WeekReportResponse week
- WeeklyClosingReportResponse closing
- List<PochiWeeklyReportResponse> pochis
- WeeklyFinancialSummaryResponse summary
- List<ReportWarningResponse> warnings
```

### 15.2 `WeekReportResponse`

```text
WeekReportResponse
- Integer weekYear
- Integer weekNumber
- LocalDate startDate
- LocalDate endDate
```

### 15.3 `WeeklyClosingReportResponse`

```text
WeeklyClosingReportResponse
- Integer weeklyClosingId
- WeeklyClosingStatus status
- String closingComment
- LocalDateTime adminClosedAt
- LocalDateTime ownerClosedAt
- boolean preview
```

`preview = true` when the Admin consults an `OPEN` week.

### 15.4 `PochiWeeklyReportResponse`

```text
PochiWeeklyReportResponse
- Integer pochiId
- String pochiIdentifier
- List<PochiDailyReportResponse> days
- List<ExpenseReportResponse> expenses
- List<DriverDebtReportResponse> debtsGenerated
- List<DebtPaymentReportResponse> debtPayments
- PochiFinancialSummaryResponse summary
```

Use the real Pochi display field already used by the project. Do not rename the domain to Vehicle.

### 15.5 `PochiDailyReportResponse`

```text
PochiDailyReportResponse
- LocalDate date
- DayOfWeek dayOfWeek
- PochiDailyStatusType dailyStatus
- String statusComment
- List<AssignmentReportResponse> assignments
- AdministrationChargeReportResponse administrationCharge
```

Return all seven dates, even when some nested collections are empty.

### 15.6 `AssignmentReportResponse`

```text
AssignmentReportResponse
- Integer assignmentId
- Shift shift
- DriverReportResponse driver
- SettlementReportResponse settlement
```

### 15.7 `DriverReportResponse`

```text
DriverReportResponse
- Integer driverId
- String driverName
```

Use the project's actual Driver name fields and mapping conventions.

### 15.8 `SettlementReportResponse`

```text
SettlementReportResponse
- Integer settlementId
- BigDecimal expectedAmount
- BigDecimal acceptedAmount
- BigDecimal difference
- String adjustmentReason
```

### 15.9 `AdministrationChargeReportResponse`

```text
AdministrationChargeReportResponse
- Integer administrationChargeId
- LocalDate chargeDate
- BigDecimal amount
- String comment
```

Keep only fields supported by the existing Administration Charge model.

### 15.10 `ExpenseReportResponse`

```text
ExpenseReportResponse
- Integer expenseId
- LocalDate expenseDate
- Integer expenseTypeId
- String expenseTypeCode
- String expenseTypeName
- BigDecimal amount
- String description
- Integer pochiId
- String pochiIdentifier
- String status
```

Adapt `description`, `comment`, and `status` to the exact field names already present in Expenses. Do not create redundant fields.

### 15.11 `DriverDebtReportResponse`

```text
DriverDebtReportResponse
- Integer debtId
- DriverReportResponse driver
- BigDecimal originalAmount
- BigDecimal currentBalance
- String status
- LocalDate debtDate
- String description
- DebtOriginReportResponse origin
```

### 15.12 `DebtPaymentReportResponse`

```text
DebtPaymentReportResponse
- Integer paymentId
- LocalDate paymentDate
- BigDecimal amount
- String comment
- DriverReportResponse driver
- Integer debtId
- BigDecimal originalDebtAmount
- BigDecimal balanceBeforePayment
- BigDecimal balanceAfterPayment
- String debtStatus
- DebtOriginReportResponse origin
```

### 15.13 `DebtOriginReportResponse`

```text
DebtOriginReportResponse
- Integer settlementId
- Integer pochiId
- String pochiIdentifier
- LocalDate originDate
- Shift shift
```

All origin fields may be nullable when the existing Driver Debt is not related to a Settlement. Never invent an origin.

### 15.14 `PochiFinancialSummaryResponse`

```text
PochiFinancialSummaryResponse
- BigDecimal expectedSettlements
- BigDecimal acceptedSettlements
- BigDecimal settlementDifference
- BigDecimal debtPaymentsReceived
- BigDecimal grossReceived
- BigDecimal administrationCharges
- BigDecimal expenses
- BigDecimal newDriverDebts
- BigDecimal totalToOwner
```

### 15.15 `WeeklyFinancialSummaryResponse`

```text
WeeklyFinancialSummaryResponse
- BigDecimal expectedSettlements
- BigDecimal acceptedSettlements
- BigDecimal settlementDifference
- BigDecimal debtPaymentsReceived
- BigDecimal grossReceived
- BigDecimal administrationCharges
- BigDecimal expenses
- BigDecimal newDriverDebts
- BigDecimal totalToOwner
```

### 15.16 `ReportWarningResponse`

```text
ReportWarningResponse
- String code
- String message
- Integer pochiId
- LocalDate date
- Shift shift
- String sourceType
- Integer sourceId
```

Warnings are informational. Reports must not modify the source record.

### 15.17 Weekly history DTO

The history endpoint must use a lightweight DTO and must not return seven daily entries for every listed week.

```text
WeeklyReportHistoryResponse
- Integer weeklyClosingId
- Integer weekYear
- Integer weekNumber
- LocalDate startDate
- LocalDate endDate
- WeeklyClosingStatus status
- String closingComment
- LocalDateTime adminClosedAt
- LocalDateTime ownerClosedAt
- BigDecimal acceptedSettlements
- BigDecimal debtPaymentsReceived
- BigDecimal administrationCharges
- BigDecimal expenses
- BigDecimal totalToOwner
```

Use the project's normal pagination response when history may contain multiple records.

---

## 16. Endpoints

Use the project's existing base path and controller conventions. Recommended endpoints follow.

### 16.1 Weekly report by ISO week

```http
GET /api/reports/weekly/{weekYear}/{weekNumber}
```

Purpose:

- Return one complete Weekly Report.
- Admin may preview `OPEN`.
- Owner may consult `ADMIN_CLOSED` or `OWNER_CLOSED`.

Validation:

- `weekYear` must be valid.
- `weekNumber` must be valid for the specified ISO week-based year.
- Resolve and return the corresponding Monday and Sunday.
- A matching Weekly Closing must exist according to current business rules.

### 16.2 Weekly report by date range

```http
GET /api/reports/weekly?startDate=2026-11-16&endDate=2026-11-22
```

Purpose:

- Return the same full Weekly Report using dates the user understands.

Validation:

- Both dates are required.
- `startDate` must be Monday.
- `endDate` must be Sunday.
- The inclusive range must contain exactly 7 calendar days.
- `endDate` must not be before `startDate`.
- The dates must resolve to one Weekly Closing.

The frontend should validate these conditions before sending the request, but the backend must repeat defensive validation because the API may be consumed by other clients.

If the project does not allow both endpoint forms under the same controller mapping cleanly, use an explicit path such as:

```http
GET /api/reports/weekly/by-range?startDate=...&endDate=...
```

Prefer consistency and unambiguous Spring mappings over forcing a specific URL shape.

### 16.3 Weekly history

```http
GET /api/reports/weekly/history
```

Recommended optional filters:

```text
weekYear
status
startDate
endDate
page
size
sort
```

Owner scope must be obtained from the authenticated/temporary user context, not trusted from an arbitrary Owner ID supplied by the caller, unless the existing Admin workflow explicitly supports selecting an Owner.

The endpoint returns lightweight history entries, not full weekly details.

### 16.4 Weeks pending Owner close

```http
GET /api/reports/weekly/pending-owner-close
```

Return weeks visible to the Owner whose Weekly Closing status is:

```text
ADMIN_CLOSED
```

Use a lightweight response suitable for a pending-review list. The Owner retrieves the full report using one of the weekly detail endpoints.

---

## 17. Admin Preview Warnings

Warnings are especially useful for an `OPEN` week.

Suggested warning codes include:

```text
WORKED_WITHOUT_ASSIGNMENTS
WORKED_WITHOUT_MORNING_ASSIGNMENT
WORKED_WITHOUT_AFTERNOON_ASSIGNMENT
ASSIGNMENT_WITHOUT_SETTLEMENT
MISSING_DAILY_STATUS
MISSING_ADMINISTRATION_CHARGE
SETTLEMENT_ADJUSTMENT_WITHOUT_REASON
EXPENSE_WITHOUT_POCHI_EXCLUDED
DEBT_PAYMENT_OWNER_UNRESOLVED
DUPLICATE_SOURCE_DATA
```

Warnings must be based on real business expectations already established by the modules. Do not flag a valid half day merely because one shift is absent unless existing business rules require both shifts for that specific record.

Warnings must:

- Identify the Pochi, date, shift, and source record when available.
- Be returned as structured data.
- Never cause Reports to create or modify records.

For closed weeks, warnings may still be returned if inconsistent historical data exists, but the report must remain read-only.

---

## 18. Ordering Rules

Return data in deterministic order:

1. Pochis by the project's normal Pochi identifier/display order.
2. Days from Monday through Sunday.
3. Assignments by shift: `MORNING`, then `AFTERNOON`.
4. Expenses by date, then ID.
5. Driver Debts by debt date, then ID.
6. Debt Payments by payment date, then ID.
7. History from newest week to oldest by default.

Do not depend on incidental database ordering.

---

## 19. Validation and Error Handling

Use the existing global exception-handling architecture.

Recommended error cases:

### Invalid weekly range

Reject when:

- Start date is not Monday.
- End date is not Sunday.
- Inclusive duration is not 7 days.
- End date is before start date.

### Invalid week number

Reject a week number that does not exist in the specified ISO week-based year.

### Weekly Closing not found

Return the project's standard not-found response when the requested week cannot be resolved.

### Owner attempts to read `OPEN`

Return the project's authorization/business-rule error. Do not return partial Owner data.

### Owner relation cannot be proven

Exclude data that cannot be reliably associated with the Owner and return an Admin warning where appropriate. Never guess ownership.

### Missing optional related data

Use empty collections or `null` according to the established DTO conventions. Do not fail the entire report for an optional comment or nullable origin.

### Missing required closed-week data

Return the report with an explicit warning when safe. Do not silently fabricate a value.

---

## 20. Service Responsibilities

Suggested organization:

```text
ReportsController
    -> ReportsService
        -> WeeklyReportAssembler / mapper helpers
        -> Existing repositories and read queries
```

The exact class structure may follow the current project architecture, but responsibilities should remain separated.

### `ReportsController`

- Parse and validate request inputs.
- Delegate to the service.
- Return response DTOs.
- Contain no financial aggregation logic.

### `ReportsService`

- Resolve the week and Weekly Closing.
- Verify report visibility.
- Resolve the Owner scope.
- Load all relevant source records.
- Group data by Pochi, date, and shift.
- Calculate totals.
- Build warnings.
- Return the final DTO.

### Assemblers/mappers

- Convert source records to report DTOs.
- Must not query the database unexpectedly from mapping methods.
- Must not recalculate financial business records.

### Repository/read-query layer

- Retrieve records efficiently for one Owner and one date range.
- Avoid one query per Pochi/day/assignment.
- Prefer bounded queries for the requested week.

---

## 21. Performance Requirements

A complete report contains three main dimensions:

```text
Owner -> Pochis -> 7 days -> related financial records
```

Avoid N+1 query behavior.

Prefer queries that load, for the complete Owner/week scope:

- Pochis
- Daily Statuses
- Assignments with Drivers
- Settlements
- Administration Charges
- Pochi Expenses with Expense Types
- Driver Debts
- Debt Payments with related Debt and origin
- Weekly Closing

Possible implementation strategies include:

- Explicit repository queries
- Entity graphs
- DTO projections
- Batched IDs
- A small number of bounded queries followed by in-memory grouping

Do not create one massive Cartesian `JOIN` that duplicates rows uncontrollably. A small set of focused week-scoped queries is acceptable and may be clearer.

---

## 22. Time and Date Rules

- Weekly range uses `LocalDate`.
- Closing timestamps use the existing timestamp type and timezone conventions.
- Week calculation must use ISO week-based-year semantics unless the existing Weekly Closing module defines another explicit rule.
- Use the Weekly Closing period as the authoritative week boundaries once resolved.
- Debt Payment inclusion uses `paymentDate`.
- Driver Debt generation uses its debt/origin creation date as defined by the existing model.
- Expenses use their business expense date, not creation timestamp, unless the current Expenses module explicitly defines otherwise.

---

## 23. Cancellation and Record Status Rules

Respect the existing cancellation/status semantics of every source module.

Do not assume cancellation means physical deletion.

The report should normally exclude canceled financial movements from active totals. If existing module rules require canceled records to remain visible for audit, expose them separately or include an explicit status without affecting active totals.

Before implementation, inspect the exact existing fields and service rules for:

- Settlements
- Administration Charges
- Expenses
- Driver Debts
- Debt Payments

Do not introduce a second cancellation interpretation in Reports.

---

## 24. Worked Financial Example

For one week:

```text
Expected Settlements          5,750.00
Accepted Settlements          5,700.00
Settlement Difference           -50.00

Debt Payments Received          100.00
Gross Received                5,800.00

Administration Charges       -1,030.00
Pochi Expenses                 -650.00

Total To Owner                4,120.00
```

Additional information:

```text
New Driver Debts                50.00
```

The new Driver Debts are not subtracted again.

A debt payment detail may explain:

```text
Driver: Roberto
Payment: 100.00
Payment date: 2026-11-19
Debt ID: 35
Original debt: 300.00
Origin Pochi: 82
Origin date: 2026-11-05
Origin shift: AFTERNOON
```

This makes it explicit that the recovered money is from an older debt rather than current-week Pochi production.

---

## 25. Testing Requirements

Follow the project's existing testing style. At minimum, cover the following service scenarios.

### Week and access

1. Admin obtains preview for `OPEN`.
2. Admin obtains report for `ADMIN_CLOSED`.
3. Admin obtains report for `OWNER_CLOSED`.
4. Owner cannot obtain `OPEN` report.
5. Owner obtains `ADMIN_CLOSED` report.
6. Owner obtains `OWNER_CLOSED` report.
7. Owner sees only owned Pochis.

### Range validation

8. Valid Monday-to-Sunday range succeeds.
9. Start date not Monday fails.
10. End date not Sunday fails.
11. Range longer than 7 days fails.
12. Range shorter than 7 days fails.
13. Missing range parameter fails.
14. Invalid ISO week fails.

### Daily report

15. Every Pochi returns seven ordered days.
16. `WORKED` returns real assignments and Drivers.
17. `NOT_WORKED` returns its comment.
18. `DAY_OFF` and `UNION_REST` are preserved.
19. Missing optional Assignment returns an empty collection, not invented data.

### Settlements

20. Equal expected and accepted amounts produce zero difference.
21. Different amounts return the adjustment reason.
22. Expected, accepted, and difference totals are correct.

### Administration Charges

23. Actual recorded Charge is used.
24. Fee Rules are not recalculated.
25. Administration Charges reduce `totalToOwner`.

### Expenses

26. Expense associated with owned Pochi is included.
27. Expense comment/description is returned.
28. Expense with `pochi = null` is excluded.
29. Expense linked to another Owner's Pochi is excluded.
30. Included Expenses reduce `totalToOwner`.

### Driver Debts

31. New debt is shown.
32. New debt contains available origin detail.
33. New debt is not subtracted again from `totalToOwner`.

### Debt Payments

34. Payment is included by `paymentDate`.
35. Payment from an older Debt increases current-week `grossReceived`.
36. Payment identifies Driver and Debt.
37. Payment returns available Pochi/Settlement/shift origin.
38. Payment with unresolved ownership is not guessed and produces a warning.

### Totals

39. `grossReceived = acceptedSettlements + debtPaymentsReceived`.
40. `totalToOwner = grossReceived - administrationCharges - expenses`.
41. Per-Pochi totals add up to the overall total without duplicate counting.
42. All decimal calculations use `BigDecimal` accurately.

### Warnings

43. Assignment without Settlement produces a warning.
44. Missing Administration Charge produces a warning when expected.
45. Report generation does not modify any source record.

### Repository/performance

46. Test or inspect that report generation does not execute one query per day/Pochi.
47. History endpoint returns lightweight records rather than full nested weekly DTOs.

---

## 26. Implementation Checklist

Before completing the module, verify:

- [ ] No new Report table or SQL migration was created.
- [ ] All domain terminology uses `Pochi`/`pochi`, never `Vehicle`/`vehicle`.
- [ ] Reports is read-only.
- [ ] Admin can preview `OPEN`.
- [ ] Owner cannot access `OPEN`.
- [ ] Owner data is scoped to owned Pochis.
- [ ] Report contains Monday through Sunday for every Pochi.
- [ ] Backend returns `MORNING` and `AFTERNOON` only.
- [ ] No `FULL_DAY`/`HALF_DAY` persistence was added.
- [ ] Adjustment reason is exposed when expected and accepted differ.
- [ ] Daily Status comment is returned.
- [ ] Expense description/comment is returned.
- [ ] Global Expenses without Pochi are excluded.
- [ ] New Driver Debts are visible but not subtracted twice.
- [ ] Debt Payments increase Gross Received.
- [ ] Debt Payment traceability identifies Driver, Debt, and available origin.
- [ ] Actual Administration Charges are used.
- [ ] Settlement Rules are not recalculated.
- [ ] Administration Fee Rules are not recalculated.
- [ ] Totals use `BigDecimal`.
- [ ] Date-range endpoint validates exact Monday-to-Sunday range.
- [ ] History endpoint is lightweight and paginated when appropriate.
- [ ] Pending endpoint returns `ADMIN_CLOSED` weeks awaiting Owner closure.
- [ ] Warnings are structured and do not modify records.
- [ ] Queries are bounded and avoid N+1 behavior.
- [ ] Unit/service tests cover formulas, permissions, inclusion rules, and traceability.

---

## 27. Definition of Done

The Reports module is complete when:

1. The Admin can preview a complete `OPEN` week.
2. The Owner can retrieve a complete `ADMIN_CLOSED` or `OWNER_CLOSED` week.
3. Each owned Pochi contains seven ordered days with status, assignments, Drivers, Settlements, and Administration Charges when available.
4. Settlement differences are explicit and explainable.
5. Only Pochi-associated Expenses are deducted and their comments are visible.
6. New Driver Debts are visible without double deduction.
7. Debt Payments are added to Owner money and contain complete available traceability.
8. The weekly financial formula produces consistent per-Pochi and overall totals.
9. Weekly history and pending Owner-close lists are available.
10. No Reports persistence or SQL migration exists.
11. All required tests pass.
12. No existing module business rule is duplicated or contradicted.

## 28. Backend Contract Notes

- Reports resolves the existing temporary user and reads its stored role. An Admin may select `ownerId`; a detail request must specify it if more than one Owner has a closing for the week. Owner requests are always scoped to the current Owner.
- Operational rows use the Weekly Closing Pochi snapshot. Debt ownership uses the origin week's snapshot when available, otherwise the recorded origin Pochi's Owner. Ambiguous or unlinked payments are excluded and flagged for Admin review. Collections for a verified historical origin outside the current snapshot contain debt activity only.
- An explicit Pochi Daily Status takes precedence. Without one, the existing daily-status rules resolve a day with assignments to `WORKED`; a day with neither source has a null status. A single assignment is valid.
- Closing timestamps retain the existing `Instant` type. The report uses the recorded Administration Charge `note` and Debt Payment `notes`. Historical payment balances remain null because they are not stored at registration. A debt origin with two shifts returns both in `originShifts` without inventing a single shift.
- A Settlement linked to both shifts appears in both assignment details; totals use the recorded Settlement once. History date filters select weeks fully contained within the specified bounds and return paginated summaries.
