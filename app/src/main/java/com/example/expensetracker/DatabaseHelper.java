package com.example.expensetracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/**
 * Handles creation of the SQLite database and all CRUD operations
 * for the "expenses" table.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "expense_tracker.db";
    private static final int DATABASE_VERSION = 2;

    // Table and column names - expenses
    public static final String TABLE_EXPENSES = "expenses";
    public static final String COL_ID = "id";
    public static final String COL_TITLE = "title";
    public static final String COL_AMOUNT = "amount";
    public static final String COL_CATEGORY = "category";
    public static final String COL_DATE = "date";
    public static final String COL_NOTE = "note";

    // Table and column names - budgets
    public static final String TABLE_BUDGETS = "budgets";
    public static final String COL_BUDGET_ID = "id";
    public static final String COL_PERIOD_TYPE = "period_type"; // "MONTHLY" or "YEARLY"
    public static final String COL_PERIOD_KEY = "period_key";   // "2026-07" or "2026"
    public static final String COL_BUDGET_AMOUNT = "amount";

    public static final String PERIOD_MONTHLY = "MONTHLY";
    public static final String PERIOD_YEARLY = "YEARLY";

    private static final String CREATE_TABLE_QUERY =
            "CREATE TABLE " + TABLE_EXPENSES + " (" +
                    COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_TITLE + " TEXT NOT NULL, " +
                    COL_AMOUNT + " REAL NOT NULL, " +
                    COL_CATEGORY + " TEXT, " +
                    COL_DATE + " TEXT, " +
                    COL_NOTE + " TEXT" +
                    ")";

    private static final String CREATE_BUDGETS_TABLE_QUERY =
            "CREATE TABLE " + TABLE_BUDGETS + " (" +
                    COL_BUDGET_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_PERIOD_TYPE + " TEXT NOT NULL, " +
                    COL_PERIOD_KEY + " TEXT NOT NULL, " +
                    COL_BUDGET_AMOUNT + " REAL NOT NULL, " +
                    "UNIQUE(" + COL_PERIOD_TYPE + ", " + COL_PERIOD_KEY + ")" +
                    ")";

    public DatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_QUERY);
        db.execSQL(CREATE_BUDGETS_TABLE_QUERY);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL(CREATE_BUDGETS_TABLE_QUERY);
        }
    }

    /** Insert a new expense. Returns the new row id, or -1 on failure. */
    public long addExpense(Expense expense) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TITLE, expense.getTitle());
        values.put(COL_AMOUNT, expense.getAmount());
        values.put(COL_CATEGORY, expense.getCategory());
        values.put(COL_DATE, expense.getDate());
        values.put(COL_NOTE, expense.getNote());

        long id = db.insert(TABLE_EXPENSES, null, values);
        db.close();
        return id;
    }

    /** Update an existing expense identified by its id. Returns number of rows affected. */
    public int updateExpense(Expense expense) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TITLE, expense.getTitle());
        values.put(COL_AMOUNT, expense.getAmount());
        values.put(COL_CATEGORY, expense.getCategory());
        values.put(COL_DATE, expense.getDate());
        values.put(COL_NOTE, expense.getNote());

        int rows = db.update(TABLE_EXPENSES, values, COL_ID + " = ?",
                new String[]{String.valueOf(expense.getId())});
        db.close();
        return rows;
    }

    /** Delete an expense by id. */
    public void deleteExpense(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_EXPENSES, COL_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    /** Return every expense, most recent date first. */
    public List<Expense> getAllExpenses() {
        List<Expense> expenseList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_EXPENSES + " ORDER BY " + COL_DATE + " DESC, " + COL_ID + " DESC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Expense expense = cursorToExpense(cursor);
                expenseList.add(expense);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return expenseList;
    }

    /** Return expenses filtered by category ("All" returns everything). */
    public List<Expense> getExpensesByCategory(String category) {
        if (category == null || category.equalsIgnoreCase("All")) {
            return getAllExpenses();
        }

        List<Expense> expenseList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_EXPENSES +
                " WHERE " + COL_CATEGORY + " = ? ORDER BY " + COL_DATE + " DESC, " + COL_ID + " DESC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{category});

        if (cursor.moveToFirst()) {
            do {
                expenseList.add(cursorToExpense(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return expenseList;
    }

    /** Sum of every expense amount currently stored. */
    public double getTotalAmount() {
        String query = "SELECT SUM(" + COL_AMOUNT + ") FROM " + TABLE_EXPENSES;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        double total = 0;
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0);
        }
        cursor.close();
        db.close();
        return total;
    }

    // ---------------------------------------------------------------
    // Aggregate queries used by StatisticsActivity (monthly / yearly /
    // category breakdowns for the charts).
    // Dates are stored as "yyyy-MM-dd" text, so substr() slices them.
    // ---------------------------------------------------------------

    /** Distinct "yyyy-MM" values that have at least one expense, most recent first. */
    public List<String> getAvailableMonths() {
        List<String> months = new ArrayList<>();
        String query = "SELECT DISTINCT substr(" + COL_DATE + ",1,7) AS ym FROM " + TABLE_EXPENSES +
                " ORDER BY ym DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            do {
                months.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return months;
    }

    /** Distinct "yyyy" values that have at least one expense, most recent first. */
    public List<String> getAvailableYears() {
        List<String> years = new ArrayList<>();
        String query = "SELECT DISTINCT substr(" + COL_DATE + ",1,4) AS yr FROM " + TABLE_EXPENSES +
                " ORDER BY yr DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            do {
                years.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return years;
    }

    /** Sum of all expenses whose date starts with the given prefix ("2026-07" or "2026"). */
    public double getTotalForPrefix(String prefix) {
        String query = "SELECT SUM(" + COL_AMOUNT + ") FROM " + TABLE_EXPENSES +
                " WHERE " + COL_DATE + " LIKE ?";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{prefix + "%"});
        double total = 0;
        if (cursor.moveToFirst()) total = cursor.getDouble(0);
        cursor.close();
        db.close();
        return total;
    }

    /** Category -> total spent, for all expenses whose date starts with the given prefix. */
    public LinkedHashMap<String, Double> getCategoryTotals(String prefix) {
        LinkedHashMap<String, Double> result = new LinkedHashMap<>();
        String query = "SELECT " + COL_CATEGORY + ", SUM(" + COL_AMOUNT + ") FROM " + TABLE_EXPENSES +
                " WHERE " + COL_DATE + " LIKE ? GROUP BY " + COL_CATEGORY + " ORDER BY SUM(" + COL_AMOUNT + ") DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{prefix + "%"});
        if (cursor.moveToFirst()) {
            do {
                result.put(cursor.getString(0), cursor.getDouble(1));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return result;
    }

    /** Month ("01".."12") -> total spent, for the given year. Every month is present, 0 if empty. */
    public LinkedHashMap<String, Double> getMonthlyTotalsForYear(String year) {
        LinkedHashMap<String, Double> result = new LinkedHashMap<>();
        for (int i = 1; i <= 12; i++) {
            result.put(String.format(Locale.US, "%02d", i), 0.0);
        }

        String query = "SELECT substr(" + COL_DATE + ",6,2) AS mo, SUM(" + COL_AMOUNT + ") FROM " + TABLE_EXPENSES +
                " WHERE substr(" + COL_DATE + ",1,4) = ? GROUP BY mo";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{year});
        if (cursor.moveToFirst()) {
            do {
                result.put(cursor.getString(0), cursor.getDouble(1));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return result;
    }

    /** Day ("01".."31") -> total spent, for the given "yyyy-MM" month. Every day present, 0 if empty. */
    public LinkedHashMap<String, Double> getDailyTotalsForMonth(String yearMonth) {
        LinkedHashMap<String, Double> result = new LinkedHashMap<>();
        for (int i = 1; i <= 31; i++) {
            result.put(String.format(Locale.US, "%02d", i), 0.0);
        }

        String query = "SELECT substr(" + COL_DATE + ",9,2) AS da, SUM(" + COL_AMOUNT + ") FROM " + TABLE_EXPENSES +
                " WHERE substr(" + COL_DATE + ",1,7) = ? GROUP BY da";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{yearMonth});
        if (cursor.moveToFirst()) {
            do {
                result.put(cursor.getString(0), cursor.getDouble(1));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return result;
    }

    // ---------------------------------------------------------------
    // Budget CRUD — one row per (period_type, period_key) pair, e.g.
    // ("MONTHLY", "2026-07") or ("YEARLY", "2026").
    // ---------------------------------------------------------------

    /** Create or overwrite the budget for a given period. */
    public void setBudget(String periodType, String periodKey, double amount) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_PERIOD_TYPE, periodType);
        values.put(COL_PERIOD_KEY, periodKey);
        values.put(COL_BUDGET_AMOUNT, amount);
        db.insertWithOnConflict(TABLE_BUDGETS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }

    /** Returns the budget amount for the period, or null if none has been set. */
    @Nullable
    public Double getBudget(String periodType, String periodKey) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_BUDGETS, new String[]{COL_BUDGET_AMOUNT},
                COL_PERIOD_TYPE + " = ? AND " + COL_PERIOD_KEY + " = ?",
                new String[]{periodType, periodKey}, null, null, null);

        Double amount = null;
        if (cursor.moveToFirst()) {
            amount = cursor.getDouble(0);
        }
        cursor.close();
        db.close();
        return amount;
    }

    /** Removes any budget set for the given period. */
    public void deleteBudget(String periodType, String periodKey) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_BUDGETS, COL_PERIOD_TYPE + " = ? AND " + COL_PERIOD_KEY + " = ?",
                new String[]{periodType, periodKey});
        db.close();
    }

    private Expense cursorToExpense(Cursor cursor) {
        Expense expense = new Expense();
        expense.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)));
        expense.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE)));
        expense.setAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(COL_AMOUNT)));
        expense.setCategory(cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY)));
        expense.setDate(cursor.getString(cursor.getColumnIndexOrThrow(COL_DATE)));
        expense.setNote(cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTE)));
        return expense;
    }
}
