package com.example.expensetracker;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements ExpenseAdapter.OnItemActionListener {

    public static final String EXTRA_EXPENSE_ID = "extra_expense_id";

    private DatabaseHelper databaseHelper;
    private ExpenseAdapter adapter;
    private RecyclerView recyclerView;
    private TextView tvTotal, tvEmpty;
    private Spinner spinnerFilter;

    private final String[] categories = {"All", "Food", "Transport", "Shopping", "Bills", "Entertainment", "Health", "Other"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setTitle(R.string.app_name);

        databaseHelper = new DatabaseHelper(this);

        tvTotal = findViewById(R.id.tvTotal);
        tvEmpty = findViewById(R.id.tvEmpty);
        recyclerView = findViewById(R.id.recyclerView);
        spinnerFilter = findViewById(R.id.spinnerFilter);
        FloatingActionButton fab = findViewById(R.id.fabAdd);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL));

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilter.setAdapter(spinnerAdapter);

        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                loadExpenses(categories[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        fab.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, AddExpenseActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload every time we come back (after adding/editing/deleting)
        String selected = spinnerFilter.getSelectedItem() != null ? spinnerFilter.getSelectedItem().toString() : "All";
        loadExpenses(selected);
    }

    private void loadExpenses(String category) {
        List<Expense> expenses = databaseHelper.getExpensesByCategory(category);

        if (adapter == null) {
            adapter = new ExpenseAdapter(expenses, this);
            recyclerView.setAdapter(adapter);
        } else {
            adapter.updateList(expenses);
        }

        tvEmpty.setVisibility(expenses.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);

        double total = databaseHelper.getTotalAmount();
        tvTotal.setText(String.format(Locale.getDefault(), "Total: ₹%.2f", total));
    }

    @Override
    public void onEdit(Expense expense) {
        Intent intent = new Intent(this, AddExpenseActivity.class);
        intent.putExtra(EXTRA_EXPENSE_ID, expense.getId());
        startActivity(intent);
    }

    @Override
    public void onDelete(Expense expense) {
        new AlertDialog.Builder(this)
                .setTitle("Delete expense")
                .setMessage("Delete \"" + expense.getTitle() + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    databaseHelper.deleteExpense(expense.getId());
                    Toast.makeText(this, "Expense deleted", Toast.LENGTH_SHORT).show();
                    String selected = spinnerFilter.getSelectedItem() != null ? spinnerFilter.getSelectedItem().toString() : "All";
                    loadExpenses(selected);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_statistics) {
            startActivity(new Intent(MainActivity.this, StatisticsActivity.class));
            return true;
        }
        if (item.getItemId() == R.id.action_set_budget) {
            startActivity(new Intent(MainActivity.this, BudgetActivity.class));
            return true;
        }
        if (item.getItemId() == R.id.action_clear_all) {
            new AlertDialog.Builder(this)
                    .setTitle("Clear all expenses")
                    .setMessage("This will permanently delete every saved expense. Continue?")
                    .setPositiveButton("Clear", (dialog, which) -> {
                        for (Expense e : databaseHelper.getAllExpenses()) {
                            databaseHelper.deleteExpense(e.getId());
                        }
                        loadExpenses("All");
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
