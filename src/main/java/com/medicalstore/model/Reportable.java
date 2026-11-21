package com.medicalstore.model;

import java.util.Map;

public interface Reportable {
    Map<String, Object> generateReport();
}