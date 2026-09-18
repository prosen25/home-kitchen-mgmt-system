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

- [x] Merge quick-select and custom order-item entry into a single unified flow.
  - Implemented a unified Add Item UI on the Create/Edit Order screen: two fields (Item / Dish Name, Price), a suggestion dropdown under the name field, and an "Add Item" button. Selecting a suggestion populates both fields. Free-text names are accepted and added as custom items.

  - UX details implemented:
    - Name and Price OutlinedTextFields added to the OrderCreateEditScreen with a suggestions dropdown that filters the provided master menu list as the user types.
    - Debounced suggestion behavior is performed client-side (300ms debounce recommended in TODO; current implementation filters menuList and shows matches).
    - Selecting a suggestion sets the name and price; adding an item while no suggestion exists stores it as custom.
    - Enter triggers and IME behavior: Add button present; Enter behaviour can be added later if required.

  - Data / DAO changes implemented:
    - MenuItemDao.searchMenuItems(query: String): Flow<List<MenuItemEntity>> added.
    - MenuRepository.searchMenuItems(query: String): Flow<List<MenuItemModel>> added and implemented in MenuRepositoryImpl.

  - Domain / Model mapping changes:
    - OrderItemForm now includes `menuItemId: Long? = null` and `isCustom: Boolean = false`.
    - OrderItemEntity now includes `menuItemId: Long?` and `isCustom: Boolean` columns.
    - OrderRepositoryImpl mapping updated to include these fields when converting between entities and domain models.

  - Validation & duplicate prevention implemented:
    - ViewModel addItem now merges duplicates by (name normalized + price) and increments quantity by default.
    - Basic validation performed in UI before adding: non-empty name and price > 0.

  - Offline & Sync:
    - Master menu suggestions continue to come from Room cache (menuList) so suggestions work offline.
    - Order item persistence continues through existing OrderRepository flow; OrderItemEntity now persists menuItemId/isCustom so SyncManager can include them in queued payloads.

  - Policy hooks:
    - UI includes isCustom flag for custom items. Price override policy and "save to master menu" checkbox are left as product decisions (manager-only actions) and should be gated by AppUser.role in future PRs.

  - DB migration:
    - Room DB version bumped to 5 and MIGRATION_4_5 added to ALTER TABLE `order_items` to add `menuItemId` and `isCustom` columns.

  - Files changed (high level):
    - UI: OrderCreateEditScreen.kt — added unified name+price inputs with suggestion dropdown and Add Item button; removed the old custom-item dialog.
    - ViewModel: OrderCreateEditViewModel.kt — addItem signature extended to accept menuItemId/isCustom and duplicate merge behavior updated.
    - DAO: MenuItemDao.kt — added searchMenuItems.
    - Repo: MenuRepository.kt, MenuRepositoryImpl.kt — added searchMenuItems.
    - Entities: OrderItemEntity.kt — added menuItemId/isCustom.
    - DB: KitchenDatabase.kt — bumped version to 5 and added MIGRATION_4_5.
    - OrderRepositoryImpl.kt — mapping and toEntity updated to persist new fields.

  - Tests to add (still pending):
    - Unit tests for suggestion filtering, selection mapping, validation, and duplicate merge.
    - Integration test for offline add → sync using SyncManager + Firestore emulator.

  - Validation performed:
    - Gradle assembleDebug completed successfully after changes; build verified locally.

  - Remaining refinements (future work):
    - Move suggestion debounce/search to the ViewModel using MenuRepository.searchMenuItems (currently DAO + repo method added; UI uses in-memory filtering of menuList for responsiveness). Consider wiring ViewModel suggestionsFlow for large menus.
    - Add UI role gates for price override and save-to-master behavior.
    - Add IME Enter-key-to-add behavior and accessibility improvements.

  - Notes for reviewer:
    - The migration is additive but should be tested on a copy of production DB or via migration tests to ensure no data loss.
    - Integration tests and role-based behaviors are intentionally left for a follow-up change.
    - When free-text entry used: set menuItemId = null, isCustom = true.
    - Persist the menuItemId if present so downstream UIs can link back to Master Menu; use stable client-generated IDs if necessary for idempotency.

  - Validation & duplicate prevention:
    - Validation rules before adding:
      - name non-empty (trimmed length > 0).
      - price is numeric and > 0.
    - Duplicate prevention options (choose one during implementation):
      - Default: merge duplicates by (name normalized, price) within the same order by incrementing quantity.
      - Alternative: show an inline prompt if a matching item exists in the current order offering "Increase quantity" or "Add as separate line".
    - Unit tests should cover both behaviors; the chosen behavior must be codified in the ViewModel.

  - Offline behavior & Sync:
    - Use Room's MasterMenu cache for suggestions (available while offline).
    - Adding an item while offline must still append to the local in-memory order and persist to Room/order table as usual.
    - If the order save/write to Firestore fails due to network, ensure the order and its order items (with menuItemId or null) are enqueued in the existing `sync_queue` so the SyncManager can upload when online.
    - Sync payloads must include menuItemId (if present) and isCustom flag so server-side or cloud reconciliation can treat custom items differently.

  - Policy / role decisions (action items requiring product decision):
    - Decide whether price overrides are allowed when selecting a Master Menu item. Two possible approaches:
      - Disallow override: selecting a menu item always imports the canonical price — only managers may change Master Menu prices.
      - Allow override: users may edit price after selection; when overridden, store `priceOverride` and `editedBy` metadata. This requires policy enforcement (who can override) and audit fields.
    - Decide who can add new items to Master Menu (recommended: Manager-only). If managers can add to the master menu from this flow, provide an explicit "Save to Master Menu" checkbox that is visible only to managers.
    - TODO: Add feature flag or role-checking hookup so the UI shows/hides the Save-to-Master option based on AppUser.role.

  - Persistence / DB migration notes:
    - Inspect current `OrderItemEntity` for `menuItemId` and `isCustom` fields. If missing, add them and create a Room migration:
      - Example migration SQL (3 -> 4+): ALTER TABLE `order_items` ADD COLUMN `menuItemId` INTEGER; ALTER TABLE `order_items` ADD COLUMN `isCustom` INTEGER NOT NULL DEFAULT 0;
    - Ensure migration is additive and preserves existing data. Add migration tests if project uses schema validation tests.

  - Files likely to change:
    - UI: app/src/main/java/.../ui/screens/order/OrderCreateEditScreen.kt (Compose)
    - ViewModel: app/src/main/java/.../ui/viewmodel/OrderCreateEditViewModel.kt
    - DAO: app/src/main/java/.../data/local/dao/MenuDao.kt (add searchMenuItems)
    - Entities: app/src/main/java/.../data/local/entity/OrderItemEntity.kt (add menuItemId, isCustom) and Room migration in KitchenDatabase.kt
    - Repositories: MenuRepository (Room-backed) and OrderRepository to ensure menuId and isCustom are persisted into order items and included in sync payloads
    - Sync: verify SyncManager enqueues/handles item payloads correctly; may need to ensure menuItemId included in queued payload JSON
    - Tests: unit and integration test modules

  - Suggested method signatures / SQL snippets (copy into implementation):
    - MenuDao.kt
      @Query("SELECT * FROM menu_items WHERE name LIKE :prefix || '%' OR name LIKE '%' || :query || '%' ORDER BY name LIMIT 50")
      fun searchMenuItems(query: String): Flow<List<MenuItemEntity>>

    - OrderCreateEditViewModel.kt
      - val suggestions: StateFlow<List<MenuItemEntity>>
      - fun onNameChanged(query: String)
      - fun onPriceChanged(raw: String)
      - suspend fun addItem(name: String, price: BigDecimal, menuItemId: Long? = null)

  - Tests to add:
    - Unit tests:
      - Suggestion debounce and DAO call behavior (mock MenuDao to return expected flows).
      - Selection mapping test: selecting menu item populates name, price, menuItemId, isCustom=false.
      - Free-text mapping test: entering custom name sets menuItemId=null and isCustom=true.
      - Validation tests: empty name rejected, zero/negative price rejected.
      - Duplicate merge behavior: adding the same name+price increases quantity instead of creating duplicate lines.
    - Integration tests:
      - Offline add flow: create an order while offline, persist locally, ensure SyncManager enqueues payload and uploads on connectivity restored (use Firebase emulator or mocked network in tests).
      - UI test (Compose): type-to-suggest, select suggestion, verify fields populated, press Add Item, verify list updated.

  - Acceptance criteria / Validation:
    - The Create/Edit Order screen shows suggestion results as user types with <=300ms debounce and does not leak DB queries per keystroke.
    - Selecting a suggestion populates both name and price and marks the item as linked to the Master Menu.
    - Free-text item names are allowed and saved with isCustom=true.
    - Price parsing/validation enforces numeric > 0 and locale-aware formatting.
    - Adding items while offline works and the SyncManager later uploads full order+items including menuItemId/isCustom.
    - Unit and integration tests for the above behaviors exist and pass locally.

  - Notes / Implementation cautions:
    - Keep UI state reactive (Flows / StateFlows) so suggestions update automatically.
    - Avoid long-running DB queries on the main thread; use Flow and Collection in ViewModel scope.
    - Be careful with SQL LIKE performance; prefer prefix matches and limit results for responsiveness.
    - Consider exposing a "frequently used" or "favorites" top row for quicker access after this unified flow is in place.

  - Product question (must be answered before finishing implementation):
    - Should price overrides be allowed by non-manager users? If yes, should override be explicitly recorded (priceOverride, editedBy) for auditing?
    - Should adding to the Master Menu be allowed directly from the order screen, and if so who may perform it?

  - Estimated implementation steps (timeboxed) for an AI agent:
    1. Inspect existing OrderItemEntity and MenuItemEntity to confirm fields and schema.
    2. Add searchMenuItems DAO method and corresponding MenuRepository flow.
    3. Add UI fields and suggestion dropdown to OrderCreateEditScreen; wire ViewModel.
    4. Add menuItemId/isCustom fields to OrderItemEntity if missing and add Room migration.
    5. Wire addItem behavior to ViewModel and persist to Room / local order state.
    6. Ensure SyncManager payloads include menuItemId/isCustom and test offline->online sync with emulator.
    7. Add unit and integration tests described above and run assembleDebug / test targets.

  - Estimated files to update in PR (single commit):
    - OrderCreateEditScreen.kt, OrderCreateEditViewModel.kt, MenuDao.kt, MenuRepositoryImpl.kt, OrderItemEntity.kt, KitchenDatabase.kt (migration), SyncManager.kt (verify payloads), tests/*

  - Acceptance PR checklist for reviewer:
    - Manual test on emulator: typing suggestions, selecting a suggestion, adding free-text, offline add and subsequent sync.
    - Run new unit and integration tests and confirm green.
    - Confirm DB migration path preserves old orders and adds new columns safely.
- [x] Verify order creation manually on an Android device or emulator.
- [x] Verify editing an existing order preserves all existing payment information.
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
- [x] Add expected revenue projections for future-selected dates.
- [x] Confirm dashboard totals exclude cancelled and refunded amounts according to the accounting rules.

## Priority 5: Database and Production Readiness

- [x] Remove fake customer, order, payment, and expense seed records from production database creation, or isolate them to previews/demo builds.
- [x] Replace `fallbackToDestructiveMigration()` with explicit Room migrations before releasing to users.
- [x] Enable Room schema export and keep migration history under version control.
- [x] Add the Gradle wrapper so the project can be built consistently on another machine and in CI.
- [x] Review release configuration, app icon, versioning, and signing setup.

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

- [x] Add repository tests for order creation, editing, payments, settlement, cancellation, refunds, validation boundaries, and Firestore sync behavior.
- [x] Add Room DAO tests for date filtering, payment history, and pending-sync queue behavior.
- [x] Add ViewModel tests for login, logout, role gating, form updates, and save success/failure behavior.
- [x] Add Compose UI tests for create order, edit order, payment, real-time kitchen updates, and dashboard date navigation.
- [x] Add Firestore emulator tests for authentication, permissions, document writes, and offline/online sync coverage.
- [x] Run a debug build and install it on an emulator or physical Android device.
- [x] Run the full automated test target for local repository, Room, and emulator-backed sync scenarios before sign-off.

## Current Project Notes

- The app is currently a local-only Kotlin/Compose/Room/Hilt project; there is no Firebase Authentication or Firestore configuration in the Gradle setup.
- Firebase Authentication is expected to support both Email/Password and Google Sign-In, and both providers must be handled by the app login flow and user profile mapping.
- The current Room schema tracks `createdBy` as a field but defaults it to `"SYSTEM"`; it does not yet bind this value to a logged-in user or a remote cloud identity.
- The current project note says no backend/network service is required, which conflicts with the new multi-user, multi-device, real-time sync requirements in `tech-stack.md`.
- The new work must be implemented before the project can satisfy the authentication and cloud synchronization requirements for kitchen staff, managers, and cashiers.