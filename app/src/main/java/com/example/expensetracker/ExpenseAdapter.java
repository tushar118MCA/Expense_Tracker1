package com.example.expensetracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    public interface OnItemActionListener {
        void onEdit(Expense expense);
        void onDelete(Expense expense);
    }

    private List<Expense> expenseList;
    private final OnItemActionListener listener;

    public ExpenseAdapter(List<Expense> expenseList, OnItemActionListener listener) {
        this.expenseList = expenseList;
        this.listener = listener;
    }

    public void updateList(List<Expense> newList) {
        this.expenseList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        Expense expense = expenseList.get(position);

        holder.tvTitle.setText(expense.getTitle());
        holder.tvCategory.setText(expense.getCategory());
        holder.tvDate.setText(expense.getDate());
        holder.tvAmount.setText(String.format(Locale.getDefault(), "₹%.2f", expense.getAmount()));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(expense);
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onDelete(expense);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return expenseList == null ? 0 : expenseList.size();
    }

    static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvCategory, tvDate, tvAmount;

        ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvAmount = itemView.findViewById(R.id.tvAmount);
        }
    }
}
