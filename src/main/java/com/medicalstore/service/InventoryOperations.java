package com.medicalstore.service;

import java.util.List;
import java.util.Map;

public interface InventoryOperations {
    List<Map<String, Object>> searchMedicines(String name);
    List<Map<String, Object>> searchMedicines(String category, boolean byCategory);
    List<Map<String, Object>> searchMedicines(long id);
}