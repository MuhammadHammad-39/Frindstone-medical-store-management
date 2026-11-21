package com.medicalstore.service;

import java.time.LocalDate;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    public DatabaseInitializer(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override public void run(ApplicationArguments args) {
        jdbc.execute("CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT NOT NULL UNIQUE, password TEXT NOT NULL, full_name TEXT NOT NULL, role TEXT NOT NULL)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS suppliers (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, phone TEXT NOT NULL, email TEXT NOT NULL)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS medicines (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, category TEXT NOT NULL, manufacturer TEXT NOT NULL, price REAL NOT NULL CHECK(price >= 0), stock INTEGER NOT NULL CHECK(stock >= 0), low_stock_threshold INTEGER NOT NULL DEFAULT 10, expiry_date TEXT NOT NULL, requires_prescription INTEGER NOT NULL DEFAULT 0, supplier_id INTEGER REFERENCES suppliers(id))");
        jdbc.execute("CREATE TABLE IF NOT EXISTS bills (id INTEGER PRIMARY KEY AUTOINCREMENT, cashier TEXT NOT NULL, total REAL NOT NULL, created_at TEXT NOT NULL)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS bill_items (id INTEGER PRIMARY KEY AUTOINCREMENT, bill_id INTEGER NOT NULL REFERENCES bills(id), medicine_id INTEGER NOT NULL, medicine_name TEXT NOT NULL, quantity INTEGER NOT NULL, unit_price REAL NOT NULL, prescription_reference TEXT)");
        seedUsers(); seedInventory();
    }

    private void seedUsers() {
        jdbc.update("INSERT OR IGNORE INTO users(username,password,full_name,role) VALUES('admin','admin123','Store Administrator','ADMIN')");
        jdbc.update("INSERT OR IGNORE INTO users(username,password,full_name,role) VALUES('pharmacist','pharm123','Alex Pharmacist','PHARMACIST')");
    }
    private void seedInventory() {
        if (jdbc.queryForObject("SELECT COUNT(*) FROM medicines", Integer.class) > 0) return;
        long supplier = jdbc.queryForObject("INSERT INTO suppliers(name,phone,email) VALUES('Northstar Pharma','555-0142','orders@northstar.example') RETURNING id", Long.class);
        LocalDate today = LocalDate.now();
        jdbc.update("INSERT INTO medicines(name,category,manufacturer,price,stock,low_stock_threshold,expiry_date,requires_prescription,supplier_id) VALUES(?,?,?,?,?,?,?,?,?)", "Paracetamol 500mg", "Pain relief", "WellSpring Labs", 4.50, 86, 20, today.plusMonths(16).toString(), 0, supplier);
        jdbc.update("INSERT INTO medicines(name,category,manufacturer,price,stock,low_stock_threshold,expiry_date,requires_prescription,supplier_id) VALUES(?,?,?,?,?,?,?,?,?)", "Amoxicillin 250mg", "Antibiotic", "Cedar Health", 12.25, 8, 12, today.plusMonths(11).toString(), 1, supplier);
        jdbc.update("INSERT INTO medicines(name,category,manufacturer,price,stock,low_stock_threshold,expiry_date,requires_prescription,supplier_id) VALUES(?,?,?,?,?,?,?,?,?)", "Vitamin C 1000mg", "Vitamins", "WellSpring Labs", 8.00, 42, 10, today.plusDays(22).toString(), 0, supplier);
        jdbc.update("INSERT INTO medicines(name,category,manufacturer,price,stock,low_stock_threshold,expiry_date,requires_prescription,supplier_id) VALUES(?,?,?,?,?,?,?,?,?)", "Cetirizine 10mg", "Allergy", "Meridian Care", 6.75, 4, 10, today.minusDays(2).toString(), 0, supplier);
    }
}