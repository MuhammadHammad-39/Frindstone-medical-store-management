package com.medicalstore.model;

public class Supplier {
    private long id;
    private String name;
    private String phone;
    private String email;

    public Supplier(long id, String name, String phone, String email) { setId(id); setName(name); setPhone(phone); setEmail(email); }
    public long getId() { return id; }
    public void setId(long id) { if (id < 0) throw new IllegalArgumentException("ID cannot be negative"); this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { if (name == null || name.isBlank()) throw new IllegalArgumentException("Supplier name is required"); this.name = name.trim(); }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { if (phone == null || phone.isBlank()) throw new IllegalArgumentException("Phone is required"); this.phone = phone.trim(); }
    public String getEmail() { return email; }
    public void setEmail(String email) { if (email == null || !email.contains("@")) throw new IllegalArgumentException("A valid email is required"); this.email = email.trim(); }
}