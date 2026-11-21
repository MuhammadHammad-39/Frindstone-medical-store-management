package com.medicalstore.model;

import java.time.LocalDate;

public class GeneralMedicine extends Medicine {
    public GeneralMedicine(long id, String name, String category, String manufacturer, double price, int stock,
                          int threshold, LocalDate expiryDate) {
        super(id, name, category, manufacturer, price, stock, threshold, expiryDate);
    }
    @Override public boolean requiresPrescription() { return false; }
    @Override public void validateForSale(String prescriptionReference) { }
}