package com.medicalstore.model;

public class Admin extends User {
    public Admin(long id, String username, String password, String fullName) { super(id, username, password, fullName); }
    @Override public String getRole() { return "ADMIN"; }
    @Override public String getAccessLevel() { return "FULL_ACCESS"; }
}