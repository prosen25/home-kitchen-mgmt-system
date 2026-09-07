### Screen 1: Dashboard / Home Screen (Daily Overview)

**Purpose:** Main landing screen displaying daily financial summaries, quick navigation, and order listings for the selected date.

* **Top Bar Header:**
  * **Title:** Kitchen Twenty2
  * **Date Selector:** `[ < ]` **Today (DD/MM/YYYY)** `[ > ]` *(Tap opens Calendar Picker to switch dates)*

* **Financial Summary Cards (3-Box Layout):**
  * **Box 1 (Green):** Net Revenue (`Total Collected - Refunds`)
  * **Box 2 (Red):** Total Expenses
  * **Box 3 (Bold):** Net Profit / Loss (`Revenue - Expenses`)
  * *(Sub-label for future dates: "Expected Revenue")*

* **Main Content Area (Tabbed View):**
  * **Tab 1: Orders List (`Count`)**
    * Card for each order showing:
      * **Customer Name & Phone:** e.g., *Jane Doe* `[ 📞 Dial ]`
      * **Items Summary:** *2x Biryani, 1x Special Mix*
      * **Delivery Quick Actions:** `[ 📍 Maps ]` | `[ 🔗 Share Location ]`
      * **Total Amount | Outstanding Due**
      * **Status Badge:** `Unpaid` (Red) | `Partially Paid` (Yellow) | `Fully Paid` (Green) | `Cancelled` (Grey)
  * **Tab 2: Expenses List (`Count`)**
    * Line items showing Category, Amount, and Note.

* **Bottom Floating Action Button (FAB):**
  * `[ + Add New ]` *(Tapping opens modal: "New Order" or "New Expense")*

* **Bottom Navigation Bar:**
  * `[ Home / Dashboard ]` | `[ Master Menu ]` | `[ Reports / History ]`

---

### Screen 2: Order Creation & Edit Screen (Updated)

**Purpose:** Interface to build or edit an order, capture customer details with auto-suggestions, tag location links, and set initial payments.

* **Header:**
  * `[ ← Back ]` **Create Order / Edit Order #104**

* **Section 1: Basic Information & Order Date**
  * **Order Date Field:** `DD/MM/YYYY` *(Defaults to Today; editable for backdated or future orders)*

* **Section 2: Customer Contact & Location Details (NEW)**
  * **Customer Name:** Text Input with Auto-Complete Dropdown
    * *Typing displays matching past customers. Selecting auto-fills Phone, Address, and Location Link.*
  * **Mobile Number:** Phone Input Field + `[ 📞 Test Dial ]` button.
  * **Delivery Address:** Multi-line Text Area.
  * **Google Location Tag:**
    * Input Field: `[ Paste Google Maps Link / GPS Coordinates ]`
    * Quick Actions: `[ 📍 Open in Maps ]` | `[ 🔗 Share via WhatsApp/SMS ]`

* **Section 3: Order Items Selection**
  * **Master Menu Quick-Select Chips:** `[+ Biryani]` `[+ Fried Rice]` `[+ Paneer Curry]`
  * **Item Table List:**
    * `Item Name` | `Qty` | `Price` | `Subtotal` | `[ Trash Icon ]`
  * **Custom Item Button:** `[ + Add Custom/Typed Item ]`

* **Section 4: Financial Calculations & Payment**
  * Subtotal: **$XX.XX**
  * **Upfront Discount Field:** `[ $0.00 ]` *(Fixed amount)*
  * Net Total: **$XX.XX**
  * **Advance Payment Field:** `[ $0.00 ]`
  * Remaining Balance Due: **$XX.XX**

* **Footer Bar:**
  * `[ Save Order ]` (Primary Action Button)

---

### Screen 3: Order Details & Settlement Modal / Screen (Updated)

**Purpose:** View order progress, execute direct customer actions (Call / Navigate / Share), log intermediate payments, process final settlement, or cancel.

* **Header:**
  * `[ ← Back ]` **Order Details (#104)**
  * Status Badge: `Partially Paid`

* **Section 1: Customer & Delivery Action Card (NEW)**
  * **Customer Name:** Jane Doe
  * **Phone:** +1 555-0199
    * **Action Button:** `[ 📞 Call Customer ]` *(Opens Phone Dialer)*
  * **Address:** 123 Main Street, Apt 4B
  * **Location Link Actions:**
    * `[ 🗺️ Open Navigation ]` *(Launches Google Maps App)*
    * `[ 📤 Share to Delivery Partner ]` *(Triggers Android Share Sheet for WhatsApp, Messenger, SMS)*

* **Section 2: Order & Payment Progress**
  * Order Date: `DD/MM/YYYY`
  * Total Net Amount: **$50.00**
  * Total Collected So Far: **$20.00**
  * **Current Remaining Due:** **$30.00**

* **Section 3: Itemized Breakup & Payment Activity Log**
  * Item List & Upfront Discounts Applied.
  * Payment Log: *01/10/2026:* Advance Received — **$20.00**
  * `[ + Add Intermediate Payment ]` Button

* **Section 4: Settlement Actions**
  * **Settlement Discount Field:** `[ $0.00 ]`
  * **New Final Due:** **$30.00**
  * **"Mark as Fully Paid & Settle" Button (Green)**
    * *Rule:* Enabled only if `Order Date <= Today`. Grayed out if `Order Date > Today`.

* **Section 5: Danger Zone**
  * `[ Cancel Order ]` Button (Red Outline)
    * *Triggers Refund Prompt:* Asks for **Refund Amount** (`$0.00` to `Total Collected`).

---

### Screen 4: Master Menu Setup Screen

**Purpose:** Simple catalog manager to maintain pre-saved items and prices.

* **Header:** `[ ← Back ]` **Menu Setup**
* **Section 1: Add New Item Bar:** `[ Dish Name ]` | `[ Price ]` | `[ Add to Menu ]`
* **Section 2: Saved Menu Items List:** Dish Name, Default Price, `[ Edit ]` | `[ Delete ]`

---

### Screen 5: Expense Entry Modal / Screen

**Purpose:** Quick form to log business expenses.

* **Header:** **Add Expense**
* **Form Fields:** Date (`DD/MM/YYYY`), Category Dropdown (`Groceries`, `Packaging`, `Gas`, `Other`), Amount ($), Notes.
* **Footer:** `[ Cancel ]` | `[ Save Expense ]`