# Kitchen Twenty2 Project TODO

This file tracks the remaining implementation work identified during the project review.

## Status Legend

- [x] Complete
- [ ] Pending

## Priority 1: Core Order Workflow

- [x] Wire order creation and editing to `OrderCreateEditViewModel` and Room.
  - Form fields now use the ViewModel-owned `OrderFormState`.
  - Create and edit navigation routes collect state from the ViewModel.
  - Menu items and customer suggestions are loaded from Room-backed repositories.
  - Save returns to the dashboard only after a successful repository save.
  - Full Gradle build validation remains pending because the repository has no Gradle wrapper and no system Gradle executable is currently available.
- [ ] Verify order creation manually on an Android device or emulator.
- [ ] Verify editing an existing order preserves all existing payment information.
- [ ] Add validation for required customer name, at least one item, valid prices, discounts, and advance payments.

## Priority 2: Payments and Accounting Safety

- [ ] Connect intermediate payment actions from `OrderDetailScreen` through navigation and `OrderDetailViewModel` to Room.
- [ ] Connect settlement discount input so the entered value is persisted correctly.
- [ ] Connect cancellation and refund actions with persisted refund records.
- [ ] Reject payments greater than the outstanding balance.
- [ ] Reject refunds greater than the amount collected.
- [ ] Reject discounts greater than the order total.
- [ ] Enforce the rule that future-dated orders cannot be settled.
- [ ] Make order and payment updates transactional so partial writes cannot leave inconsistent data.
- [ ] Preserve payment history and status correctly when an order is edited.

## Priority 3: Menu and Expense Persistence

- [ ] Replace local Compose state in menu setup with `MenuSetupViewModel` and Room-backed menu CRUD.
- [ ] Verify menu changes remain available after leaving and reopening the screen.
- [ ] Implement expense deletion in `ExpenseRepositoryImpl` if deletion remains part of the product scope.
- [ ] Replace hardcoded form dates with the device's current date.

## Priority 4: Dashboard and Reporting

- [ ] Connect dashboard previous-day and next-day arrows to `DashboardViewModel`.
- [ ] Implement Reports / History navigation and its first usable screen.
- [ ] Calculate daily revenue from payment and refund dates, not only order dates.
- [ ] Add expected revenue projections for future-selected dates.
- [ ] Confirm dashboard totals exclude cancelled and refunded amounts according to the accounting rules.

## Priority 5: Database and Production Readiness

- [ ] Remove fake customer, order, payment, and expense seed records from production database creation, or isolate them to previews/demo builds.
- [ ] Replace `fallbackToDestructiveMigration()` with explicit Room migrations before releasing to users.
- [ ] Enable Room schema export and keep migration history under version control.
- [ ] Add the Gradle wrapper so the project can be built consistently on another machine and in CI.
- [ ] Review release configuration, app icon, versioning, and signing setup.

## Testing

- [ ] Add repository tests for order creation, editing, payments, settlement, cancellation, refunds, and validation boundaries.
- [ ] Add Room DAO tests for date filtering and payment history.
- [ ] Add ViewModel tests for form updates and save success/failure behavior.
- [ ] Add Compose UI tests for create order, edit order, payment, and dashboard date navigation.
- [ ] Run a debug build and install it on an emulator or physical Android device.

## Current Project Notes

- The app is an offline-first Kotlin/Compose/Room/Hilt project.
- The main dashboard and supporting screen shells are present.
- The order wiring changes are currently uncommitted in the working tree.
- No backend or network service is required by the current product scope.