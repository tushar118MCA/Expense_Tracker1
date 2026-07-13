package com.example.expensetracker;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Shows monthly or yearly spending: a summary total, a pie chart broken
 * down by category, and a bar chart trend (day-by-day within a month,
 * or month-by-month within a year).
 */
public class StatisticsActivity extends AppCompatActivity {

    private static final String[] MONTH_NAMES = {
            "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };

    private DatabaseHelper databaseHelper;
    private Button btnMonthly, btnYearly;
    private Spinner spinnerPeriod;
    private TextView tvPeriodLabel, tvPeriodTotal, tvNoData, tvTrendLabel, tvBudgetSummary;
    private PieChart pieChart;
    private BarChart barChart;
    private Button btnSetBudget;
    private ProgressBar progressBudget;

    private boolean isMonthlyMode = true;
    private List<String> periods = new ArrayList<>();
    private String currentPeriodKey;

    private final ActivityResultLauncher<Intent> budgetLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> renderForPeriod(currentPeriodKey)
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);
        setTitle("Statistics");

        databaseHelper = new DatabaseHelper(this);

        btnMonthly = findViewById(R.id.btnMonthly);
        btnYearly = findViewById(R.id.btnYearly);
        spinnerPeriod = findViewById(R.id.spinnerPeriod);
        tvPeriodLabel = findViewById(R.id.tvPeriodLabel);
        tvPeriodTotal = findViewById(R.id.tvPeriodTotal);
        tvNoData = findViewById(R.id.tvNoData);
        tvTrendLabel = findViewById(R.id.tvTrendLabel);
        pieChart = findViewById(R.id.pieChart);
        barChart = findViewById(R.id.barChart);
        btnSetBudget = findViewById(R.id.btnSetBudget);
        progressBudget = findViewById(R.id.progressBudget);
        tvBudgetSummary = findViewById(R.id.tvBudgetSummary);

        btnSetBudget.setOnClickListener(v -> {
            Intent intent = new Intent(this, BudgetActivity.class);
            intent.putExtra(BudgetActivity.EXTRA_PERIOD_TYPE,
                    isMonthlyMode ? DatabaseHelper.PERIOD_MONTHLY : DatabaseHelper.PERIOD_YEARLY);
            intent.putExtra(BudgetActivity.EXTRA_PERIOD_KEY, currentPeriodKey);
            budgetLauncher.launch(intent);
        });

        btnMonthly.setOnClickListener(v -> switchMode(true));
        btnYearly.setOnClickListener(v -> switchMode(false));

        spinnerPeriod.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                if (position >= 0 && position < periods.size()) {
                    renderForPeriod(periods.get(position));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        switchMode(true);
    }

    private void switchMode(boolean monthly) {
        isMonthlyMode = monthly;

        btnMonthly.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                monthly ? Color.parseColor("#6A1B9A") : Color.parseColor("#E0E0E0")));
        btnMonthly.setTextColor(monthly ? Color.WHITE : Color.parseColor("#212121"));

        btnYearly.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                !monthly ? Color.parseColor("#6A1B9A") : Color.parseColor("#E0E0E0")));
        btnYearly.setTextColor(!monthly ? Color.WHITE : Color.parseColor("#212121"));

        tvTrendLabel.setText(monthly ? "Daily trend" : "Monthly trend");

        periods = monthly ? databaseHelper.getAvailableMonths() : databaseHelper.getAvailableYears();

        if (periods.isEmpty()) {
            // Fall back to the current period so the screen still renders something.
            java.util.Calendar cal = java.util.Calendar.getInstance();
            String current = monthly
                    ? String.format(Locale.US, "%04d-%02d", cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1)
                    : String.valueOf(cal.get(java.util.Calendar.YEAR));
            periods.add(current);
        }

        List<String> displayLabels = new ArrayList<>();
        for (String p : periods) {
            displayLabels.add(monthly ? formatMonthLabel(p) : p);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, displayLabels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPeriod.setAdapter(adapter);
        spinnerPeriod.setSelection(0);

        renderForPeriod(periods.get(0));
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

    private void renderForPeriod(String periodKey) {
        currentPeriodKey = periodKey;
        tvPeriodLabel.setText(isMonthlyMode ? formatMonthLabel(periodKey) : ("Year " + periodKey));

        double total = databaseHelper.getTotalForPrefix(periodKey);
        tvPeriodTotal.setText(String.format(Locale.getDefault(), "₹%.2f", total));

        renderBudgetCard(periodKey, total);
        renderPieChart(periodKey);
        renderBarChart(periodKey);
    }

    private void renderBudgetCard(String periodKey, double spent) {
        String periodType = isMonthlyMode ? DatabaseHelper.PERIOD_MONTHLY : DatabaseHelper.PERIOD_YEARLY;
        Double budget = databaseHelper.getBudget(periodType, periodKey);

        if (budget == null) {
            tvBudgetSummary.setText("No budget set for this period.");
            tvBudgetSummary.setTextColor(Color.parseColor("#757575"));
            progressBudget.setVisibility(android.view.View.GONE);
            return;
        }

        double remaining = budget - spent;
        int percent = budget > 0 ? (int) Math.min(100, Math.round((spent / budget) * 100)) : 0;

        progressBudget.setVisibility(android.view.View.VISIBLE);
        progressBudget.setProgress(percent);

        if (remaining >= 0) {
            tvBudgetSummary.setText(String.format(Locale.getDefault(),
                    "Budget ₹%.2f · Spent ₹%.2f · Remaining ₹%.2f", budget, spent, remaining));
            tvBudgetSummary.setTextColor(Color.parseColor("#424242"));
            progressBudget.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#6A1B9A")));
        } else {
            tvBudgetSummary.setText(String.format(Locale.getDefault(),
                    "Budget ₹%.2f · Spent ₹%.2f · Over by ₹%.2f", budget, spent, Math.abs(remaining)));
            tvBudgetSummary.setTextColor(Color.parseColor("#D32F2F"));
            progressBudget.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#D32F2F")));
        }
    }

    private void renderPieChart(String periodKey) {
        Map<String, Double> categoryTotals = databaseHelper.getCategoryTotals(periodKey);

        if (categoryTotals.isEmpty()) {
            pieChart.setVisibility(android.view.View.GONE);
            tvNoData.setVisibility(android.view.View.VISIBLE);
            return;
        }
        pieChart.setVisibility(android.view.View.VISIBLE);
        tvNoData.setVisibility(android.view.View.GONE);

        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(chartColors());
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setSliceSpace(2f);

        PieData pieData = new PieData(dataSet);
        pieData.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "₹%.0f", value);
            }
        });

        pieChart.setData(pieData);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawEntryLabels(false);
        pieChart.setHoleRadius(45f);
        pieChart.setTransparentCircleRadius(50f);
        pieChart.setEntryLabelColor(Color.BLACK);

        Legend legend = pieChart.getLegend();
        legend.setTextSize(12f);
        legend.setWordWrapEnabled(true);

        pieChart.animateY(600);
        pieChart.invalidate();
    }

    private void renderBarChart(String periodKey) {
        LinkedHashMap<String, Double> data = isMonthlyMode
                ? databaseHelper.getDailyTotalsForMonth(periodKey)
                : databaseHelper.getMonthlyTotalsForYear(periodKey);

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        int index = 0;
        for (Map.Entry<String, Double> entry : data.entrySet()) {
            entries.add(new BarEntry(index, entry.getValue().floatValue()));
            labels.add(isMonthlyMode ? entry.getKey() : MONTH_NAMES[Integer.parseInt(entry.getKey()) - 1]);
            index++;
        }

        BarDataSet dataSet = new BarDataSet(entries, isMonthlyMode ? "Spend per day" : "Spend per month");
        dataSet.setColor(Color.parseColor("#6A1B9A"));
        dataSet.setValueTextSize(9f);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.7f);

        barChart.setData(barData);
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.setFitBars(true);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(isMonthlyMode ? -60f : 0f);
        xAxis.setTextSize(9f);
        xAxis.setDrawGridLines(false);

        barChart.getAxisRight().setEnabled(false);
        barChart.animateY(600);
        barChart.invalidate();
    }

    private int[] chartColors() {
        return new int[]{
                Color.parseColor("#6A1B9A"),
                Color.parseColor("#1D9E75"),
                Color.parseColor("#D85A30"),
                Color.parseColor("#378ADD"),
                Color.parseColor("#BA7517"),
                Color.parseColor("#D4537E"),
                Color.parseColor("#639922"),
                Color.parseColor("#5F5E5A")
        };
    }
}
