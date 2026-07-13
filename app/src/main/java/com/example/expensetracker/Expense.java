package com.example.expensetracker;

public class Expense {

    private int id;
    private String title;
    private double amount;
    private String category;
    private String date; // stored as yyyy-MM-dd
    private String note;

    public Expense() {
    }

    public Expense(int id, String title, double amount, String category, String date, String note) {
        this.id = id;
        this.title = title;
        this.amount = amount;
        this.category = category;
        this.date = date;
        this.note = note;
    }

    public Expense(String title, double amount, String category, String date, String note) {
        this.title = title;
        this.amount = amount;
        this.category = category;
        this.date = date;
        this.note = note;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
