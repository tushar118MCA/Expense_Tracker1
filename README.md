# Expense Tracker

A simple Android application to track daily income and expenses, helping users manage their personal finances with ease.

## Overview

Expense Tracker allows users to record transactions, categorize spending, set monthly/yearly budgets, and view summaries of their financial activity over time. It's designed to give a clear picture of where money is going and help with budgeting decisions.

## Features

* Add, edit, and delete expense entries
* Categorize transactions (Food, Rent, Transport, Entertainment, Bills, Health, Shopping, Other)
* View running total and filter transactions by category
* Set a monthly or yearly budget and track spending against it, with a progress bar that flags overspending
* Visual summary via pie charts (spend by category) and bar charts (daily/monthly trend)
* Data persistence via a local SQLite database — works fully offline


## Screenshots 

##  Tech Stack

* **Platform**: Android (Java)
* **Database**: SQLite, via `SQLiteOpenHelper`
* **UI**: AndroidX, Material Components, RecyclerView

##  Getting Started

### Prerequisites

* Android Studio (Giraffe or newer recommended)
* Android SDK with `minSdk 21` (Android 5.0+) available
* An emulator or physical Android device to run on

### Installation

```bash
# Clone the repository
git clone https://github.com/your-username/expense-tracker.git

# Open the project folder in Android Studio
# (File > Open, then select the ExpenseTracker folder containing settings.gradle)
```

Let Gradle sync — it will pull in AndroidX, Material, and MPAndroidChart (via JitPack) automatically. Then press **Run** to install the app on an emulator or connected device.

##  Usage

1. Launch the app.
2. Tap **+** to add a new expense — enter the title, amount, category, date, and an optional note.
3. View your running total and filter the list by category from the main screen.
4. Open **Statistics** from the menu to see monthly/yearly charts and category breakdowns.
5. Open **Set Budget** to set how much you plan to spend for a month or year, then track it on the Statistics screen.

##  Database

Two tables, managed by `DatabaseHelper`:

```sql
CREATE TABLE expenses (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL, amount REAL NOT NULL,
    category TEXT, date TEXT, note TEXT
);

CREATE TABLE budgets (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    period_type TEXT NOT NULL,   -- "MONTHLY" or "YEARLY"
    period_key  TEXT NOT NULL,   -- "2026-07" or "2026"
    amount REAL NOT NULL,
    UNIQUE(period_type, period_key)
);
```
## Contributing

Contributions are welcome! Feel free to open an issue or submit a pull request.

## License

This project is licensed under the MIT License.
