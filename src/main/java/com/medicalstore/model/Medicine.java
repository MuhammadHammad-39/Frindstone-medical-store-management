package com.medicalstore.model;

import java.time.LocalDate;

public abstract class Medicine {
    private long id;
    private String name;
    private String category;
    private String manufacturer;
    private double price;
    private int stock;
    private int lowStockThreshold;
    private LocalDate expiryDate;

    protected Medicine(long id, String name, String category, String manufacturer, double price, int stock,
                       int lowStockThreshold, LocalDate expiryDate) {
        setId(id); setName(name); setCategory(category); setManufacturer(manufacturer); setPrice(price);
        setStock(stock); setLowStockThreshold(lowStockThreshold); setExpiryDate(expiryDate);
    }

    public long getId() { return id; }
    public void setId(long id) { if (id < 0) throw new IllegalArgumentException("ID cannot be negative"); this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { if (name == null || name.isBlank()) throw new IllegalArgumentException("Medicine name is required"); this.name = name.trim(); }
    public String getCategory() { return category; }
    public void setCategory(String category) { if (category == null || category.isBlank()) throw new IllegalArgumentException("Category is required"); this.category = category.trim(); }
    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { if (manufacturer == null || manufacturer.isBlank()) throw new IllegalArgumentException("Manufacturer is required"); this.manufacturer = manufacturer.trim(); }
    public double getPrice() { return price; }
    public void setPrice(double price) { if (!Double.isFinite(price) || price < 0) throw new IllegalArgumentException("Price must be zero or more"); this.price = price; }
    public int getStock() { return stock; }
    public void setStock(int stock) { if (stock < 0) throw new IllegalArgumentException("Stock cannot be negative"); this.stock = stock; }
    public int getLowStockThreshold() { return lowStockThreshold; }
    public void setLowStockThreshold(int threshold) { if (threshold < 0) throw new IllegalArgumentException("Threshold cannot be negative"); this.lowStockThreshold = threshold; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { if (expiryDate == null) throw new IllegalArgumentException("Expiry date is required"); this.expiryDate = expiryDate; }
    public abstract boolean requiresPrescription();
    public abstract void validateForSale(String prescriptionReference);
}