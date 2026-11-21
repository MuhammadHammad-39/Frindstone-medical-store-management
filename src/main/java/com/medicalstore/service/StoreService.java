package com.medicalstore.service;

import com.medicalstore.model.ExpiryReport;
import com.medicalstore.model.LowStockReport;
import com.medicalstore.model.Medicine;
import com.medicalstore.model.Reportable;
import com.medicalstore.model.SalesReport;
import com.medicalstore.repository.StoreRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StoreService extends AbstractStoreService implements InventoryOperations {
    public StoreService(StoreRepository repository) { super(repository); }
    @Override public List<Map<String, Object>> searchMedicines(String name) { return repository.findMedicines("name", name); }
    @Override public List<Map<String, Object>> searchMedicines(String category, boolean byCategory) {
        return repository.findMedicines(byCategory ? "category" : "name", category);
    }
    @Override public List<Map<String, Object>> searchMedicines(long id) { return repository.findMedicines("id", Long.toString(id)); }
    public Map<String, Object> dashboard() { return repository.dashboard(); }
    public List<Map<String, Object>> medicines() { return repository.medicines(); }
    public List<Map<String, Object>> staff() { return repository.staff(); }
    public List<Map<String, Object>> suppliers() { return repository.suppliers(); }
    public List<Map<String, Object>> report(String type) {
        Reportable report = switch (type) {
            case "sales" -> new SalesReport(repository.salesTotal(), repository.sales());
            case "low-stock" -> new LowStockReport(repository.lowStock());
            case "expiry" -> new ExpiryReport(repository.nearExpiry());
            default -> throw new IllegalArgumentException("Unknown report type");
        };
        return List.of(report.generateReport());
    }
    public long saveMedicine(long id, Map<String, Object> body) { return repository.saveMedicine(id, body); }
    public void deleteMedicine(long id) { repository.deleteMedicine(id); }
    public long saveSupplier(long id, Map<String, Object> body) { return repository.saveSupplier(id, body); }
    public void deleteSupplier(long id) { repository.deleteSupplier(id); }

    @Transactional
    public Map<String, Object> checkout(String cashier, List<Map<String, Object>> requestedItems) {
        if (requestedItems == null || requestedItems.isEmpty()) throw new IllegalArgumentException("Add at least one medicine");
        List<Map<String, Object>> validated = new ArrayList<>();
        Map<Long, Integer> quantitiesByMedicine = new HashMap<>();
        for (Map<String, Object> item : requestedItems) {
            long medicineId = Long.parseLong(item.get("medicineId").toString());
            int quantity = Integer.parseInt(item.get("quantity").toString());
            if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
            quantitiesByMedicine.merge(medicineId, quantity, Integer::sum);
        }
        double total = 0;
        for (Map<String, Object> item : requestedItems) {
            long medicineId = Long.parseLong(item.get("medicineId").toString());
            int quantity = Integer.parseInt(item.get("quantity").toString());
            String reference = item.get("prescriptionReference") == null ? "" : item.get("prescriptionReference").toString();
            if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
            Medicine medicine = repository.medicine(medicineId);
            if (medicine == null) throw new IllegalArgumentException("Medicine not found: " + medicineId);
            medicine.validateForSale(reference);
            if (medicine.getExpiryDate().isBefore(LocalDate.now())) throw new IllegalArgumentException(medicine.getName() + " is expired and cannot be sold");
            if (medicine.getStock() < quantitiesByMedicine.get(medicineId)) throw new IllegalArgumentException("Insufficient stock for " + medicine.getName());
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("medicine", medicine); line.put("quantity", quantity); line.put("prescriptionReference", reference);
            validated.add(line); total += medicine.getPrice() * quantity;
        }
        String createdAt = LocalDateTime.now().withNano(0).toString();
        long billId = repository.insertBill(cashier, total, createdAt);
        for (Map<String, Object> line : validated) {
            Medicine medicine = (Medicine) line.get("medicine"); int quantity = (Integer) line.get("quantity");
            if (repository.deductStock(medicine.getId(), quantity) != 1) throw new IllegalArgumentException("Stock changed during checkout for " + medicine.getName());
            repository.insertBillItem(billId, medicine.getId(), medicine.getName(), quantity, medicine.getPrice(), (String) line.get("prescriptionReference"));
        }
        Map<String, Object> receipt = new LinkedHashMap<>();
        receipt.put("id", billId); receipt.put("cashier", cashier); receipt.put("createdAt", createdAt);
        receipt.put("items", repository.billItems(billId)); receipt.put("total", total);
        return receipt;
    }
}