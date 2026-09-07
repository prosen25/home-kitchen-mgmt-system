# High-Level Requirements (v1.1 - Final Scope)

## 1. Business Objective
Provide a free, offline-first single-user mobile application for "Kitchen Twenty2" to streamline order logging, track partial payments and flexible discounts, record operational expenses, and calculate accurate daily profit and loss automatically.

---

## 2. Scope & Key Features

* **Offline Data & Storage (Single-User Model)**
  * All data (orders, expenses, menu items) is stored locally on a single Android device.
  * No user registration, login, cloud database, or ongoing server costs required.

* **Menu Catalog Management**
  * Dedicated screen to create, update, or delete pre-saved dishes and default prices.

* **Order & Flexible Payment Management**
  * **Order Creation:** Build orders using pre-saved menu items, custom typed items, or custom prices.
  * **Order-Level Discount:** Apply a fixed amount discount (e.g., $5 off) when creating the initial order.
  * **Advance Payment:** Record initial deposit/advance paid by the customer.
  * **Settlement & Settlement Discount:** During final payment upon delivery, allow adding an additional fixed amount discount before marking the balance as fully paid.
  * **Balance Calculation:** Automatically calculate remaining due amount:
    
    $$\text{Due Amount} = \text{Order Total} - \text{Discounts} - \text{Advance Payment}$$

  * **Payment Statuses:** Track status clearly (*Unpaid*, *Partially Paid*, *Fully Paid*).

* **Expense Tracking**
  * Log daily business costs (e.g., ingredients, packaging, fuel) with date, category, and amount.

* **Daily Profit & Loss Summaries**
  * Real-time dashboard reflecting:
    * **Total Net Revenue:** Sum of actual collected payments (or total order values minus discounts).
    * **Total Expenses:** Sum of all logged costs.
    * **Net Profit/Loss:** Revenue minus expenses.
  * Historical calendar view to inspect past daily performance.

---

## 3. Updated Operational Flow Example

1. **Order Creation:** Owner adds items totaling $50. She applies a $5 upfront discount. Total becomes $45.
2. **Advance Collected:** Customer pays $15 advance. App logs order as *Partially Paid* with $30 Due.
3. **Delivery & Settlement:** Upon delivery, customer requests a $2 courtesy discount. Owner inputs $2 settlement discount.
4. **Final Payment:** New remaining balance updates to $28. Customer pays $28, owner taps "Mark Fully Paid", and $43 net revenue is booked to the day's total.