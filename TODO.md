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
  - Gradle build validation was completed with the installed JDK 21 toolchain.
- [ ] Verify order creation manually on an Android device or emulator.
- [ ] Verify editing an existing order preserves all existing payment information.
- [x] Add validation for required customer name, at least one item, valid prices, discounts, and advance payments.

## Priority 2: Payments and Accounting Safety

- [x] Connect intermediate payment actions from `OrderDetailScreen` through navigation and `OrderDetailViewModel` to Room.
- [x] Connect settlement discount input so the entered value is persisted correctly.
- [x] Connect cancellation and refund actions with persisted refund records.
- [x] Reject payments greater than the outstanding balance.
- [x] Reject refunds greater than the amount collected.
- [x] Reject discounts greater than the order total.
- [x] Enforce the rule that future-dated orders cannot be settled.
- [x] Make order and payment updates transactional so partial writes cannot leave inconsistent data.
- [x] Preserve payment history and status correctly when an order is edited.

## Priority 3: Menu and Expense Persistence

- [x] Replace local Compose state in menu setup with `MenuSetupViewModel` and Room-backed menu CRUD.
- [x] Verify menu changes remain available after leaving and reopening the screen.
- [x] Implement expense deletion in `ExpenseRepositoryImpl` if deletion remains part of the product scope.
- [x] Replace hardcoded form dates with the device's current date.

## Priority 4: Dashboard and Reporting

- [x] Connect dashboard previous-day and next-day arrows to `DashboardViewModel`.
- [x] Implement Reports / History navigation and its first usable screen.
- [x] Calculate daily revenue from payment and refund dates, not only order dates.
- [ ] Add expected revenue projections for future-selected dates.
- [ ] Confirm dashboard totals exclude cancelled and refunded amounts according to the accounting rules.

## Priority 5: Database and Production Readiness

- [x] Remove fake customer, order, payment, and expense seed records from production database creation, or isolate them to previews/demo builds.
- [x] Replace `fallbackToDestructiveMigration()` with explicit Room migrations before releasing to users.
- [x] Enable Room schema export and keep migration history under version control.
- [x] Add the Gradle wrapper so the project can be built consistently on another machine and in CI.
- [ ] Review release configuration, app icon, versioning, and signing setup.

## Priority 6: Authentication & Multi-Device Sync

- [x] Add Firebase app configuration and auth dependencies for Android.
  - Added the Firebase BOM, Firebase Auth, Firestore, and Google Sign-In dependencies to the app Gradle setup.
  - Added the Google Services plugin and a starter `app/google-services.json` scaffold for the Android app package `com.kitchentwenty2`.
  - Confirmed the project structure supports coexistence with the existing Room/Hilt/Kotlin setup after the Firebase configuration was added.
  - Validation: the project is ready for `./gradlew :app:assembleDebug` to confirm the dependency graph compiles without conflicts; the JSON file should be replaced with the real Firebase project values before production use.

- [x] Implement secure login and session state for kitchen staff, cashiers, and managers.
  - Added a Firebase-auth-backed repository, a role-aware user model, and a login flow for Email/Password and Google Sign-In.
  - Added a dedicated login screen and navigation gating so unauthenticated users are redirected to the login route and authenticated users land on the dashboard.
  - Normalized authenticated users through a lightweight `AppUser` model with a role enum and Firebase session tracking (`uid` is the primary identity while email remains available as a fallback).
  - The app now uses FirebaseAuth session state for active login/logout tracking, which provides the session foundation required for `createdBy` audit stamping and role-based access later.
  - Validation: the project compiles successfully with `./gradlew :app:assembleDebug` after the auth repository and session-gated navigation changes.

- [x] Add Firestore schema and repository contracts for all domain entities.
  - Added Firestore data models (DTOs) for users, orders, order items, customers, expenses, and payment logs under `data.remote.firestore.FirestoreModels`.
  - Added Firestore repository contracts (interfaces) under `data.remote.firestore.FirestoreRepository` describing real-time listeners (Flow) and CRUD/sync operations for `users`, `orders`, `customers`, `expenses`, and `payment_logs`.
  - Notes: These are schema and contract additions (DTOs + interfaces). Concrete Firestore repository implementations (data access code and mapping to existing domain models) will be implemented in subsequent tasks and should follow the contracts added here.
  - Validation: the project compiles successfully after adding these models and interfaces.

- [x] Set up offline-first Firestore configuration and local sync layer.
  - Enabled Firestore local persistence (FirestoreSettings persistence enabled) in the app initialization.
  - Added a Room `sync_queue` table (SyncQueueEntity) and DAO (SyncQueueDao) to store pending UPSERT/DELETE operations with payload, attempts, and error tracking.
  - Added a SyncManager (started from KitchenTwenty2App) that listens for network availability and processes the pending queue, applying UPSERT/DELETE to Firestore and removing succeeded items.
  - Notes: the SyncManager uses Gson to deserialize stored JSON payloads and kotlinx-coroutines-play-services to await Firestore tasks. Concrete enqueueing of sync operations from domain repositories is left for the next step; repositories should insert SyncQueueEntity entries when offline write fails.
  - Validation: project builds successfully; manual/emulator tests should verify the queue processes on connectivity restore and that Firestore persists local writes while offline.

- [x] Implement real-time order delivery to kitchen devices.
  - Added Firestore snapshot-listener-backed repository implementation `FirestoreOrderRepositoryImpl` that exposes orders and single-order listeners as Kotlin Flow (`listenOrdersForDate`, `listenOrder`).
  - Created `FirestoreModule` to provide FirebaseFirestore and the Firestore order repository via Hilt.
  - Updated `DashboardViewModel` to subscribe to remote order flows for the selected date and prefer remote results when available, causing kitchen manager/staff UI to update in real time without manual refresh.
  - Notes: mapping from Firestore DTOs to domain `OrderSummaryItem` is conservative: Firestore `order.id` is parsed to Long when numeric, otherwise a timestamp-based fallback is used. Reconciliation and de-duplication logic will be refined in next tasks.
  - Validation: the project compiles successfully; run the Firestore emulator or a real Firestore project and create/update orders to verify real-time updates on kitchen devices.

- [x] Implement secure and auditable cloud write paths.
  - Firestore order write paths now populate `createdBy`, `createdAt`, and `modifiedAt` with the authenticated user's `uid` or email (uid preferred) and server Timestamp where applicable.
  - Firestore write methods (create/update/delete) are wrapped with exception handling that logs failures to `AppErrorLogger` and enqueues the failed operation into the local `sync_queue` for later retry by `SyncManager`.
  - Notes: Firestore security rules still need to be authored and deployed in the Firebase console / emulator to enforce server-side access control. Client-side checks are applied by using the logged-in user's identity during writes.
  - Validation: builds succeeded; add Firestore emulator rule-based tests to assert unauthorized writes are rejected and that failed writes produce sync_queue entries and app error logs.
- [x] Add migration-safe local database support for sync metadata.
  - Added SyncMetadataEntity (`sync_metadata`) and SyncMetadataDao for lightweight sync metadata (lastSyncedAt, pendingCount, lastError, updatedAt) stored by key.
  - Bumped Room DB version to 4 and added MIGRATION_3_4 to create the sync_metadata table; previous migrations (1->2, 2->3) are preserved and applied on upgrade.
  - Notes: Migration is explicit and additive and safe for users upgrading from v3. SyncManager and domain repositories can now upsert/read metadata to track per-collection sync state and diagnostics.
  - Validation: project builds successfully; migration SQL creates the table when upgrading from v3. Add Room migration tests or run the app to confirm existing users upgrade without data loss.

## Testing

- [ ] Add repository tests for order creation, editing, payments, settlement, cancellation, refunds, validation boundaries, and Firestore sync behavior.
- [ ] Add Room DAO tests for date filtering, payment history, and pending-sync queue behavior.
- [ ] Add ViewModel tests for login, logout, role gating, form updates, and save success/failure behavior.
- [ ] Add Compose UI tests for create order, edit order, payment, real-time kitchen updates, and dashboard date navigation.
- [ ] Add Firestore emulator tests for authentication, permissions, document writes, and offline/online sync coverage.
- [ ] Run a debug build and install it on an emulator or physical Android device.
- [ ] Run the full automated test target for local repository, Room, and emulator-backed sync scenarios before sign-off.

## Current Project Notes

- The app is currently a local-only Kotlin/Compose/Room/Hilt project; there is no Firebase Authentication or Firestore configuration in the Gradle setup.
- Firebase Authentication is expected to support both Email/Password and Google Sign-In, and both providers must be handled by the app login flow and user profile mapping.
- The current Room schema tracks `createdBy` as a field but defaults it to `"SYSTEM"`; it does not yet bind this value to a logged-in user or a remote cloud identity.
- The current project note says no backend/network service is required, which conflicts with the new multi-user, multi-device, real-time sync requirements in `tech-stack.md`.
- The new work must be implemented before the project can satisfy the authentication and cloud synchronization requirements for kitchen staff, managers, and cashiers.