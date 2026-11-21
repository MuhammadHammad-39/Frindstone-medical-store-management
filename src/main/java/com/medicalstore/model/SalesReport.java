package com.medicalstore.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SalesReport implements Reportable {
    private final double total;
    private final List<Map<String, Object>> sales;
    public SalesReport(double total, List<Map<String, Object>> sales) { this.total = total; this.sales = sales; }
    @Override public Map<String, Object> generateReport() {
        Map<String, Object> result = new LinkedHashMap<>(); result.put("type", "sales"); result.put("total", total); result.put("sales", sales); return result;
    }
}