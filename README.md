# Budget Calendar

A personal finance and budgeting tool built around the conceptual model of a **Calendar**. Instead of abstract categories or dry ledgers, experience your finances as a chronological map of the month. 

The primary innovation is the blend of **retroactive tracking** (what I have spent) and **proactive scheduling** (what I will spend), seamlessly contributing to a real-time budget model. 

## Key Behavioral Pillars

1.  **Chronological Finances**: Time is the primary axis for understanding money.
2.  **Account-Based Liquidity**: Explicitly separating the "Safe to Spend" pool from long-term savings or investments across multiple accounts.
3.  **Forward-Looking Simulation**: The app is not just a historian; it is a crystal ball.
4.  **The "Safe to Spend" Reality**: Every scheduled transaction instantly impacts the present reality of the active spending pool.

## Core Systems

- **The Multi-Account Architecture**: Distinguishes *where* money lives and *what* it is meant for, keeping "Safe to Spend" separate from savings and investments.
- **The Core Calendar View**: Visualize history, schedule the future, and see your budget deducting in real-time.
- **The Isolated Sandbox Environment**: Safely model large purchases and "What-If" scenarios without altering your primary calendar logic.
- **Flexible Income Modeling**: Tools for scheduling recurring salary deposits alongside volatile or freelance income.

## Core Mechanics

- **Transaction Lifecycles ("Missed" Fix)**: Transactions have states like *Pending* or *Overdue*, remaining deducted from your Spending Pool until you confirm, edit, or cancel them.
- **The Credit Card Philosophy**: Treat credit cards as liability accounts that instantly pull from liquid cash, keeping you out of debt.
- **Continuous Calendar (Rollover)**: Any unspent funds automatically roll over to the next month, encouraging a reward cycle and visualizing growing wealth.

## UI Implementation Details

- 7-column grid layout mapping explicitly to days of the week.
- Opacity shifts to indicate trailing/leading month days.
- Small typography under main dates showing proactive and retroactive math (`+$1k` for payday, `-$80` for bills).
- Active day high-contrast visualization using dark-mode styles to anchor the model to reality.
- Dedicated transaction lists beneath the calendar upon selecting specific days.
