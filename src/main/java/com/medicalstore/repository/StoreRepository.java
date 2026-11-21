package com.medicalstore.repository;

import com.medicalstore.model.GeneralMedicine;
import com.medicalstore.model.Medicine;
import com.medicalstore.model.PrescriptionMedicine;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class StoreRepository {
    private final JdbcTemplate jdbc;
    public StoreRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Map<String, Object> findUser(String username) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM users WHERE username = ?", username);
        return rows.isEmpty() ? null : rows.get(0);
    }
    public List<Map<String, Object>> medicines() { return jdbc.queryForList("SELECT * FROM medicines ORDER BY name"); }
    public Medicine medicine(long id) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM medicines WHERE id = ?", id);
        if (rows.isEmpty()) return null;
        Map<String, Object> row = rows.get(0);
        LocalDate expiry = LocalDate.parse(row.get("expiry_date").toString());
        if (((Number) row.get("requires_prescription")).intValue() == 1) {
            return new PrescriptionMedicine(((Number) row.get("id")).longValue(), row.get("name").toString(), row.get("category").toString(), row.get("manufacturer").toString(), ((Number) row.get("price")).doubleValue(), ((Number) row.get("stock")).intValue(), ((Number) row.get("low_stock_threshold")).intValue(), expiry);
        }
        return new GeneralMedicine(((Number) row.get("id")).longValue(), row.get("name").toString(), row.get("category").toString(), row.get("manufacturer").toString(), ((Number) row.get("price")).doubleValue(), ((Number) row.get("stock")).intValue(), ((Number) row.get("low_stock_threshold")).intValue(), expiry);
    }
    public List<Map<String, Object>> findMedicines(String field, String value) {
        if ("id".equals(field)) return jdbc.queryForList("SELECT * FROM medicines WHERE id = ?", Long.parseLong(value));
        if ("category".equals(field)) return jdbc.queryForList("SELECT * FROM medicines WHERE lower(category) LIKE lower(?) ORDER BY name", "%" + value + "%");
        return jdbc.queryForList("SELECT * FROM medicines WHERE lower(name) LIKE lower(?) ORDER BY name", "%" + value + "%");
    }
    public Map<String, Object> dashboard() {
        Map<String, Object> metrics = jdbc.queryForMap("SELECT COUNT(*) AS totalProducts, SUM(CASE WHEN stock <= low_stock_threshold THEN 1 ELSE 0 END) AS lowStockCount, SUM(CASE WHEN expiry_date <= date('now', 'localtime', '+30 day') THEN 1 ELSE 0 END) AS nearExpiryCount FROM medicines");
        metrics.put("todaySalesTotal", jdbc.queryForObject("SELECT COALESCE(SUM(total), 0) FROM bills WHERE date(created_at) = date('now', 'localtime')", Double.class));
        return metrics;
    }
    public List<Map<String, Object>> lowStock() { return jdbc.queryForList("SELECT * FROM medicines WHERE stock <= low_stock_threshold ORDER BY stock"); }
    public List<Map<String, Object>> nearExpiry() { return jdbc.queryForList("SELECT * FROM medicines WHERE expiry_date <= date('now', 'localtime', '+30 day') ORDER BY expiry_date"); }
    public List<Map<String, Object>> sales() { return jdbc.queryForList("SELECT id, cashier, created_at AS createdAt, total FROM bills ORDER BY created_at DESC LIMIT 100"); }
    public double salesTotal() { return jdbc.queryForObject("SELECT COALESCE(SUM(total), 0) FROM bills", Double.class); }
    public List<Map<String, Object>> staff() { return jdbc.queryForList("SELECT id, username, full_name AS fullName, role FROM users ORDER BY role, full_name"); }
    public List<Map<String, Object>> suppliers() { return jdbc.queryForList("SELECT * FROM suppliers ORDER BY name"); }
    public long saveSupplier(long id, Map<String, Object> body) {
        String name = required(body, "name"); String phone = required(body, "phone"); String email = required(body, "email");
        if (id == 0) return jdbc.queryForObject("INSERT INTO suppliers(name, phone, email) VALUES(?, ?, ?) RETURNING id", Long.class, name, phone, email);
        jdbc.update("UPDATE suppliers SET name=?, phone=?, email=? WHERE id=?", name, phone, email, id); return id;
    }
    public long saveMedicine(long id, Map<String, Object> body) {
        String name = required(body, "name"), category = required(body, "category"), manufacturer = required(body, "manufacturer");
        double price = Double.parseDouble(body.get("price").toString()); int stock = Integer.parseInt(body.get("stock").toString());
        int threshold = Integer.parseInt(body.getOrDefault("lowStockThreshold", 10).toString()); String expiry = required(body, "expiryDate");
        int prescription = Boolean.parseBoolean(String.valueOf(body.getOrDefault("requiresPrescription", false))) ? 1 : 0;
        Object supplierId = body.get("supplierId");
        if (id == 0) {
            return jdbc.queryForObject("INSERT INTO medicines(name, category, manufacturer, price, stock, low_stock_threshold, expiry_date, requires_prescription, supplier_id) VALUES(?,?,?,?,?,?,?,?,?) RETURNING id", Long.class, name, category, manufacturer, price, stock, threshold, expiry, prescription, supplierId);
        }
        jdbc.update("UPDATE medicines SET name=?, category=?, manufacturer=?, price=?, stock=?, low_stock_threshold=?, expiry_date=?, requires_prescription=?, supplier_id=? WHERE id=?", name, category, manufacturer, price, stock, threshold, expiry, prescription, supplierId, id);
        return id;
    }
    public void deleteMedicine(long id) { jdbc.update("DELETE FROM medicines WHERE id=?", id); }
    public void deleteSupplier(long id) { jdbc.update("DELETE FROM suppliers WHERE id=?", id); }
    public long insertBill(String cashier, double total, String createdAt) {
        return jdbc.queryForObject("INSERT INTO bills(cashier,total,created_at) VALUES(?,?,?) RETURNING id", Long.class, cashier, total, createdAt);
    }
    public void insertBillItem(long billId, long medicineId, String name, int quantity, double price, String prescription) {
        jdbc.update("INSERT INTO bill_items(bill_id,medicine_id,medicine_name,quantity,unit_price,prescription_reference) VALUES(?,?,?,?,?,?)", billId, medicineId, name, quantity, price, prescription);
    }
    public int deductStock(long medicineId, int quantity) { return jdbc.update("UPDATE medicines SET stock=stock-? WHERE id=? AND stock>=?", quantity, medicineId, quantity); }
    public List<Map<String, Object>> billItems(long billId) { return jdbc.queryForList("SELECT medicine_name AS name, quantity, unit_price AS unitPrice, quantity*unit_price AS lineTotal, prescription_reference AS prescriptionReference FROM bill_items WHERE bill_id=?", billId); }

    private String required(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (value == null || value.toString().isBlank()) throw new IllegalArgumentException(key + " is required");
        return value.toString().trim();
    }
}