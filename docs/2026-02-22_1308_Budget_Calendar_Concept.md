---
tags:
  - narrative
  - idea
  - app-concept
---

# Personal Budgeting Calendar App Concept

## The Core Concept
A personal finance and budgeting tool built around the conceptual model of a **Calendar**. Instead of abstract categories or dry ledgers, the user experiences their finances as a chronological map of the month. 

The primary innovation is the blend of **retroactive tracking** (what I have spent) and **proactive scheduling** (what I will spend), seamlessly contributing to a real-time budget model. 

## Key Behavioral Pillars
1.  **Chronological Finances**: Time is the primary axis for understanding money.
2.  **Account-Based Liquidity**: Explicitly separating the "Safe to Spend" pool from long-term savings or investments across multiple accounts.
3.  **Forward-Looking Simulation**: The app is not just a historian; it is a crystal ball.
4.  **The "Safe to Spend" Reality**: Every scheduled transaction instantly impacts the present reality of the active spending pool.

---

## Core Systems

### 1. The Multi-Account Architecture
The foundation of the budgeting logic relies on distinguishing *where* money lives and *what* it is meant for.
*   **Account Types**: Users can create discrete accounts (e.g., Cash-on-Hand, Checking, Savings, Investment Portfolios).
*   **The "Spending Pool" Designation**: Only specific accounts are tagged as contributing to the active, liquid spending pool used for daily transactions.
*   **Walled Gardens**: Savings and investment accounts are tracked for net worth visibility but do not falsely inflate the daily budget available for the calendar.

### 2. The Core Calendar View
This is the heart of the application.
*   **Visualizing History**: Users can see exactly what they spent on specific days in the past, visualized as events or totals on the calendar grid.
*   **The "Scheduled Future"**: The ability to plot out recurring bills (rent, utilities) or planned expenses (a weekend trip) directly onto future dates.
*   **Real-time Budget Deduction**: The moment a future transaction is scheduled, it is deducted from the *current* available budget pool, preventing accidental overspending before the bill actually hits.

### 3. The Isolated Sandbox Environment
A dedicated, separate mode for "What-If" scenario planning. It acts as an isolated scratchpad.
*   **Snapshot Loading**: The Sandbox initializes by loading a "snapshot" of the current live calendar (the present reality).
*   **Future Simulation**: Users can jump to future months (e.g., next month) and simulate potential schedules: *What if I get paid $X on the 10th and $Y on the 25th, and I schedule these specific bills?*
*   **Consequence-Free Execution**: Safely model large purchases (e.g., *What if I buy this new laptop next Tuesday? How does that impact my buffer for rent on the 1st?*) without altering the primary calendar logic.

### 4. Flexible Income Modeling
The system must support both predictable and volatile cash flows.
*   **Recurring Income**: Simple, automated deposit scheduling for users with a steady salary.
*   **Manual/Freelance Income**: Essential tools for manually plotting irregular payments directly onto the calendar on specific days as they are earned (or expected).

## Core Mechanics & Edge Cases

### 1. Transaction Lifecycles (The "Missed" Transaction Fix)
Because the app relies on the future deducting from the present, transactions must have states rather than just disappearing if ignored.
*   **The "Pending" state**: A scheduled future transaction. It reserves (deducts) money from the Spending Pool ahead of time.
*   **The "Overdue" state**: If the scheduled date passes without the user confirming it, the transaction turns red and becomes 'Overdue.' *Crucially, it remains deducted from the Spending Pool.* It acts as a stubborn placeholder until the user acts.
*   **Resolution Actions**: To clear an Overdue transaction, the user must:
    *   **Confirm & clear**: It happened exactly as planned.
    *   **Edit & clear**: The bill was higher or lower than expected. The system instantly refunds or deducts the difference to the Spending Pool.
    *   **Cancel/Skip**: The event didn't happen. The system releases the reserved funds back into the active Spending Pool.

### 2. The Credit Card Philosophy
To prevent debt while using credit cards, the app treats credit cards as liability accounts that instantly pull from liquid cash.
*   **Instant Realization**: When you buy a $50 dinner on a credit card, the calendar logs the expense. The $50 is instantly deducted from your "Safe to Spend" pool, but the money hasn't left your checking account yet.
*   **The "Reserved for CC Payment" Bucket (The Ledger)**: That $50 essentially moves from your "Safe to Spend" pool into a specific bucket dedicated to that credit card.
    *   *Purpose:* This bucket serves exactly as your running total of "How much have I spent on this card?"
    *   *Visibility:* You will always be able to view this bucket (e.g., "Amex Balance: $450") so you know exactly what your next credit card bill will be. The beauty is that you already know you have the cash to pay it, because it was already subtracted from your Safe to Spend pool.
*   **Paying the Bill**: When the credit card bill is due, you just log a transfer from Checking to the Credit Card. That action empties the "Reserved for CC Payment" bucket. It doesn't affect your Safe to Spend pool because that money was already accounted for on the day you bought the dinner.

### 3. Continuous Calendar (Rollover)
Month boundaries are visual, not mathematical walls.
*   **Automated Rollover**: Any unspent funds in the "Spending Pool" at the end of the month gracefully roll over to the 1st of the next month.
*   **Spending Equity**: This encourages a reward cycle where users can plainly see their baseline pool growing over time if they consistently spend less than their income, giving a true graphical representation of accumulating wealth or "buffer."

---

## Next Steps

- [ ] **Define the UI/UX Flow**: Map out the primary screens (Dashboard, Sandbox Overlay, Transaction Modals, Account Settings) and user journeys.
- [ ] **Define the Data Entities**: Draft the backend logic and data models for key objects (e.g., `Transaction`, `Account`, `SandboxSnapshot`).
- [ ] **Establish Categorization Rules**: Finalize how/if traditional categories are used versus simple chronological logging.
- [ ] **Technology Stack Selection**: Decide on the front-end and back-end tools (e.g., React Native/Flutter for mobile, or a web-first approach like Next.js) based on personal use goals.

## UI Implementation Details: Calendar View
- The grid uses a 7-column layout mapping explicitly to days of the week.
- Opacity shifts indicate trailing/leading month days (e.g. earlier days in the first row rendering at 40% opacity).
- Small typography under the main date number injects proactive and retroactive math:
	- A positive impact (e.g., Payday) is indicated with small `+$1k` subtext.
	- Negative events (bills, simulated drafts) display with `-$80` subtext.
	- Micro dots indicate that there is an event on that day without explicitly revealing the balance if it's too clustered.
- The active day `(e.g., Today · Feb 12)` presents in dark-mode utilizing `$bg-dark` and inverted text to anchor the structural math model to reality.
- Selecting a day surfaces its specific `Transaction Item` components in a flat list beneath the calendar to bridge math with action.
