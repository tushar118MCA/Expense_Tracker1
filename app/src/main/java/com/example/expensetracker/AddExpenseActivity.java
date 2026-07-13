package com.example.expensetracker;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;
import java.util.Locale;

public class AddExpenseActivity extends AppCompatActivity {

    private DatabaseHelper databaseHelper;
    private EditText etTitle, etAmount, etNote;
    private Spinner spinnerCategory;
    private TextView tvDate;
    private Button btnSave, btnDelete;

    private final String[] categories = {"Food", "Transport", "Shopping", "Bills", "Entertainment", "Health", "Other"};

    private int expenseId = -1; // -1 means "adding new"
    private String selectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        databaseHelper = new DatabaseHelper(this);

        etTitle = findViewById(R.id.etTitle);
        etAmount = findViewById(R.id.etAmount);
        etNote = findViewById(R.id.etNote);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        tvDate = findViewById(R.id.tvDate);
        btnSave = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.btnDelete);

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        // Default date = today
        Calendar today = Calendar.getInstance();
        selectedDate = formatDate(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH));
        tvDate.setText(selectedDate);

        tvDate.setOnClickListener(v -> showDatePicker());

        expenseId = getIntent().getIntExtra(MainActivity.EXTRA_EXPENSE_ID, -1);
        if (expenseId != -1) {
            setTitle(R.string.edit_expense);
            btnDelete.setVisibility(android.view.View.VISIBLE);
            populateFieldsForEdit(expenseId);
        } else {
            setTitle(R.string.add_expense);
            btnDelete.setVisibility(android.view.View.GONE);
        }

        btnSave.setOnClickListener(v -> saveExpense());
        btnDelete.setOnClickListener(v -> {
            databaseHelper.deleteExpense(expenseId);
            Toast.makeText(this, "Expense deleted", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void populateFieldsForEdit(int id) {
        for (Expense e : databaseHelper.getAllExpenses()) {
            if (e.getId() == id) {
                etTitle.setText(e.getTitle());
                etAmount.setText(String.valueOf(e.getAmount()));
                etNote.setText(e.getNote());
                selectedDate = e.getDate();
                tvDate.setText(selectedDate);

                int index = 0;
                for (int i = 0; i < categories.length; i++) {
                    if (categories[i].equalsIgnoreCase(e.getCategory())) {
                        index = i;
                        break;
                    }
                }
                spinnerCategory.setSelection(index);
                break;
            }
        }
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        try {
            String[] parts = selectedDate.split("-");
            calendar.set(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) - 1, Integer.parseInt(parts[2]));
        } catch (Exception ignored) {
        }

        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            selectedDate = formatDate(year, month, dayOfMonth);
            tvDate.setText(selectedDate);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private String formatDate(int year, int month, int day) {
        // month is 0-indexed from the picker
        return String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day);
    }

    private void saveExpense() {
        String title = etTitle.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim();
        String note = etNote.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();

        if (TextUtils.isEmpty(title)) {
            etTitle.setError("Title is required");
            etTitle.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(amountStr)) {
            etAmount.setError("Amount is required");
            etAmount.requestFocus();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                etAmount.setError("Enter an amount greater than 0");
                etAmount.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            etAmount.setError("Enter a valid number");
            etAmount.requestFocus();
            return;
        }

        Expense expense = new Expense(title, amount, category, selectedDate, note);

        if (expenseId == -1) {
            databaseHelper.addExpense(expense);
            Toast.makeText(this, "Expense added", Toast.LENGTH_SHORT).show();
        } else {
            expense.setId(expenseId);
            databaseHelper.updateExpense(expense);
            Toast.makeText(this, "Expense updated", Toast.LENGTH_SHORT).show();
        }

        finish();
    }
}
