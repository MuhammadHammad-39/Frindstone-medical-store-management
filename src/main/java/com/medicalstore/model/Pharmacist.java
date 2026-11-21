package com.medicalstore.model;

public class Pharmacist extends User {
    public Pharmacist(long id, String username, String password, String fullName) { super(id, username, password, fullName); }
    @Override public String getRole() { return "PHARMACIST"; }
    @Override public String getAccessLevel() { return "SALES_AND_INVENTORY_READ"; }
}