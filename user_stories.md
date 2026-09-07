### Epic 1: Menu & Customer Catalog Management

#### User Story 1.1: Manage Master Menu Items
> **As the** Kitchen Owner,  
> **I want to** create, edit, and delete dishes in a master menu list,  
> **So that** I can quickly select items when taking new orders.

* **Acceptance Criteria:**
  1. Dedicated "Menu Setup" screen accessible from navigation.
  2. Add/edit/delete items with **Item Name** and **Default Price**.
  3. Deleting a menu item does not alter past historical orders.
  4. Changes are saved locally on the device.

#### User Story 1.2: Customer Auto-Suggestion & Records
> **As the** Kitchen Owner,  
> **I want the app to** auto-suggest customer details when I start typing a customer's name,  
> **So that** I don't have to re-enter their phone number, address, or location link every time they order.

* **Acceptance Criteria:**
  1. When entering a customer's name on an order, an autocomplete dropdown shows matching past customers stored in local database.
  2. Selecting a suggested customer automatically populates their saved **Mobile Number**, **Delivery Address**, and **Google Location Link/Coordinates**.
  3. Updating customer details on a new order updates their master profile for future suggestions.

---

### Epic 2: Order Logging, Customer Actions & Settlement

#### User Story 2.1: Create New Order with Customer Contact & Location Tagging
> **As the** Kitchen Owner,  
> **I want to** capture customer contact details and tag their Google Location on an order,  
> **So that** I have all the delivery and contact info tied directly to the order.

* **Acceptance Criteria:**
  1. **Customer Info Fields:** Includes fields for **Customer Name** (with auto-complete), **Mobile Number**, **Address**, and **Google Location Link / Coordinates**.
  2. **Location Tagging:** Dedicated button/field to paste or pick a Google Maps link/GPS point.
  3. **Order Date Selection:** Select past, current, or future dates.
  4. **Item Selection & Discounts:** Add pre-saved/custom items, apply upfront fixed discounts, and record advance payments.
  5. **Calculations & Status:**
     
     $$\text{Initial Due} = \text{Subtotal} - \text{Upfront Discount} - \text{Advance Payment}$$

#### User Story 2.2: Delivery Sharing & Phone Dialer Integration (NEW)
> **As the** Kitchen Owner,  
> **I want to** call the customer directly or share their location with delivery drivers via external apps,  
> **So that** orders can be delivered smoothly without copying numbers and links manually.

* **Acceptance Criteria:**
  1. **Direct Phone Call:** Tapping a Phone Icon next to the customer's mobile number opens the device's native Phone / Dialer app pre-filled with their number.
  2. **Open in Maps:** Tapping a Map Pin Icon opens the saved Google Location link directly inside the Google Maps app for turn-by-turn navigation.
  3. **Share Location (Delivery Partner):** Tapping a Share Button triggers the Android system share sheet, letting the owner send the customer's address and Google Location link via WhatsApp, Messenger, SMS, or other installed apps.

#### User Story 2.3: Edit & Update Active Orders
> **As the** Kitchen Owner,  
> **I want to** edit an existing order to correct mistakes, add/remove items, or update customer/date info,  
> **So that** the order details always stay accurate.

* **Acceptance Criteria:**
  1. Modify order items, quantities, custom prices, discounts, order dates, or customer contact/location details.
  2. Recalculate balances and statuses automatically upon saving edits.

#### User Story 2.4: Record Intermediate Payments & Settle Orders
> **As the** Kitchen Owner,  
> **I want to** log partial payments and settle orders on or after their delivery date,  
> **So that** I can track customer balances over time.

* **Acceptance Criteria:**
  1. Record step-by-step intermediate payments before settlement.
  2. **Settlement Restriction:** Settlement button is disabled if `Order Date > Today`.
  3. On or after the order date, apply settlement discounts and tap **"Mark as Fully Paid"**.

#### User Story 2.5: Cancel Order & Process Full/Partial Refunds
> **As the** Kitchen Owner,  
> **I want to** cancel orders and log full, partial, or zero refunds,  
> **So that** my financial reports accurately reflect retained revenues vs. refunded cash.

* **Acceptance Criteria:**
  1. Enter a custom refund amount (up to total collected amount).
  2. Order status updates to **Cancelled**, excluding uncollected balances from active accounting.

---

### Epic 3: Expense Logging

#### User Story 3.1: Record Daily Expenses
> **As the** Kitchen Owner,  
> **I want to** log operational costs with dates, categories, and amounts,  
> **So that** I can track business expenses accurately.

* **Acceptance Criteria:**
  1. Log expenses with **Amount**, **Category**, **Date** (defaults to Today, allows backdating/future dating), and notes.

---

### Epic 4: Dashboard & Financial Summaries

#### User Story 4.1: View Daily Profit & Loss Summary
> **As the** Kitchen Owner,  
> **I want to** see real-time net revenue, expense, and net profit calculations,  
> **So that** I know my financial health for any selected date.

* **Acceptance Criteria:**
  1. Displays **Total Net Revenue** (Actual cash collected minus refunds), **Total Expenses**, and **Net Profit/Loss**:
     
     $$\text{Net Profit/Loss} = \text{Total Net Revenue} - \text{Total Expenses}$$

  2. Select any date to inspect historical performance or future expected bookings.