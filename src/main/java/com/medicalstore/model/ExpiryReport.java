package com.medicalstore.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExpiryReport implements Reportable {
    private final List<Map<String, Object>> medicines;
    public ExpiryReport(List<Map<String, Object>> medicines) { this.medicines = medicines; }
    @Override public Map<String, Object> generateReport() {
        Map<String, Object> result = new LinkedHashMap<>(); result.put("type", "expiry"); result.put("count", medicines.size()); result.put("medicines", medicines); return result;
    }
}