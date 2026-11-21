package com.medicalstore.model;

import java.time.LocalDate;

public class PrescriptionMedicine extends Medicine {
    public PrescriptionMedicine(long id, String name, String category, String manufacturer, double price, int stock,
                                int threshold, LocalDate expiryDate) {
        super(id, name, category, manufacturer, price, stock, threshold, expiryDate);
    }
    @Override public boolean requiresPrescription() { return true; }
    @Override public void validateForSale(String prescriptionReference) {
        if (prescriptionReference == null || prescriptionReference.isBlank()) {
            throw new IllegalArgumentException("A prescription reference is required for " + getName());
        }
    }
}