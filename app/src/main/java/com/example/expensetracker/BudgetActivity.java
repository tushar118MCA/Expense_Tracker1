package com.example.expensetracker;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Lets the user set (or update) how much they plan to spend for a chosen
 * month or year. Saved into the "budgets" table and compared against
 * actual spending on the Statistics screen.
 */
public class BudgetActivity extends AppCompatActivity {

    public static final String EXTRA_PERIOD_TYPE = "extra_period_type";
    public static final String EXTRA_PERIOD_KEY = "extra_period_key";

    private static final String[] MONTH_NAMES = {
            "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };

    private DatabaseHelper databaseHelper;
    private Button btnMonthly, btnYearly, btnSave;
    private Spinner spinnerPeriod;
    private EditText etAmount;

    private boolean isMonthly = true;
    private List<String> periodKeys = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget);
        setTitle(R.string.set_budget);

        databaseHelper = new DatabaseHelper(this);

        btnMonthly = findViewById(R.id.btnBudgetMonthly);
        btnYearly = findViewById(R.id.btnBudgetYearly);
        spinnerPeriod = findViewById(R.id.spinnerBudgetPeriod);
        etAmount = findViewById(R.id.etBudgetAmount);
        btnSave = findViewById(R.id.btnSaveBudget);

        btnMonthly.setOnClickListener(v -> switchMode(true));
        btnYearly.setOnClickListener(v -> switchMode(false));
        btnSave.setOnClickListener(v -> saveBudget());

        spinnerPeriod.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                if (position >= 0 && position < periodKeys.size()) {
                    prefillAmount(periodKeys.get(position));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // If launched from Statistics for a specific period, honor it.
        String requestedType = getIntent().getStringExtra(EXTRA_PERIOD_TYPE);
        boolean startMonthly = requestedType == null || requestedType.equals(DatabaseHelper.PERIOD_MONTHLY);

        switchMode(startMonthly);

        String requestedKey = getIntent().getStringExtra(EXTRA_PERIOD_KEY);
        if (requestedKey != null) {
            int index = periodKeys.indexOf(requestedKey);
            if (index >= 0) {
                spinnerPeriod.setSelection(index);
            }
        }
    }

    private void switchMode(boolean monthly) {
        isMonthly = monthly;

        btnMonthly.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                monthly ? android.graphics.Color.parseColor("#6A1B9A") : android.graphics.Color.parseColor("#E0E0E0")));
        btnMonthly.setTextColor(monthly ? android.graphics.Color.WHITE : android.graphics.Color.parseColor("#212121"));

        btnYearly.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                !monthly ? android.graphics.Color.parseColor("#6A1B9A") : android.graphics.Color.parseColor("#E0E0E0")));
        btnYearly.setTextColor(!monthly ? android.graphics.Color.WHITE : android.graphics.Color.parseColor("#212121"));

        periodKeys = monthly ? buildRecentMonths() : buildRecentYears();

        List<String> displayLabels = new ArrayList<>();
        for (String key : periodKeys) {
            displayLabels.add(monthly ? formatMonthLabel(key) : key);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, displayLabels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPeriod.setAdapter(adapter);
        spinnerPeriod.setSelection(0);

        prefillAmount(periodKeys.get(0));
    }

    /** Current month plus the previous 11 months, most recent first, as "yyyy-MM". */
    private List<String> buildRecentMonths() {
        List<String> months = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        for (int i = 0; i < 12; i++) {
            months.add(String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1));
            cal.add(Calendar.MONTH, -1);
        }
        return months;
    }

    /** Current year plus the previous 4 years, most recent first, as "yyyy". */
    private List<String> buildRecentYears() {
        List<String> years = new ArrayList<>();
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        for (int i = 0; i < 5; i++) {
            years.add(String.valueOf(currentYear - i));
        }
        return years;
    }

    private String formatMonthLabel(String yearMonth) {
        try {
            String[] parts = yearMonth.split("-");
            int month = Integer.parseInt(parts[1]);
            return MONTH_NAMES[month - 1] + " " + parts[0];
        } catch (Exception e) {
            return yearMonth;
        }
    }

    private void prefillAmount(String periodKey) {
        String periodType = isMonthly ? DatabaseHelper.PERIOD_MONTHLY : DatabaseHelper.PERIOD_YEARLY;
        Double existing = databaseHelper.getBudget(periodType, periodKey);
        etAmount.setText(existing != null ? String.valueOf(existing) : "");
    }

    private void saveBudget() {
        String amountStr = etAmount.getText().toString().trim();
        if (TextUtils.isEmpty(amountStr)) {
            etAmount.setError("Enter a budget amount");
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

        int selected = spinnerPeriod.getSelectedItemPosition();
        String periodKey = periodKeys.get(selected);
        String periodType = isMonthly ? DatabaseHelper.PERIOD_MONTHLY : DatabaseHelper.PERIOD_YEARLY;

        databaseHelper.setBudget(periodType, periodKey, amount);
        Toast.makeText(this, "Budget saved", Toast.LENGTH_SHORT).show();

        Intent result = new Intent();
        result.putExtra(EXTRA_PERIOD_TYPE, periodType);
        result.putExtra(EXTRA_PERIOD_KEY, periodKey);
        setResult(RESULT_OK, result);
        finish();
    }
}
