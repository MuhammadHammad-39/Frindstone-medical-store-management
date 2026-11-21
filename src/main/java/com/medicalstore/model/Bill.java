package com.medicalstore.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Bill {
    private long id;
    private String cashier;
    private LocalDateTime createdAt;
    private List<BillItem> items = new ArrayList<>();

    public Bill(long id, String cashier, LocalDateTime createdAt, List<BillItem> items) {
        setId(id); setCashier(cashier); setCreatedAt(createdAt); setItems(items);
    }
    public long getId() { return id; }
    public void setId(long id) { if (id < 0) throw new IllegalArgumentException("ID cannot be negative"); this.id = id; }
    public String getCashier() { return cashier; }
    public void setCashier(String cashier) { if (cashier == null || cashier.isBlank()) throw new IllegalArgumentException("Cashier is required"); this.cashier = cashier.trim(); }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { if (createdAt == null) throw new IllegalArgumentException("Bill time is required"); this.createdAt = createdAt; }
    public List<BillItem> getItems() { return List.copyOf(items); }
    public void setItems(List<BillItem> items) { if (items == null || items.isEmpty()) throw new IllegalArgumentException("Bill requires at least one item"); this.items = new ArrayList<>(items); }
    public double getTotal() { return items.stream().mapToDouble(BillItem::getLineTotal).sum(); }
}