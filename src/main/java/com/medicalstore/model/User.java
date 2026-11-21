package com.medicalstore.model;

public abstract class User {
    private long id;
    private String username;
    private String password;
    private String fullName;

    protected User(long id, String username, String password, String fullName) {
        setId(id);
        setUsername(username);
        setPassword(password);
        setFullName(fullName);
    }

    public long getId() { return id; }
    public void setId(long id) { if (id < 0) throw new IllegalArgumentException("ID cannot be negative"); this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { if (username == null || username.isBlank()) throw new IllegalArgumentException("Username is required"); this.username = username.trim(); }
    public String getPassword() { return password; }
    public void setPassword(String password) { if (password == null || password.isBlank()) throw new IllegalArgumentException("Password is required"); this.password = password; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { if (fullName == null || fullName.isBlank()) throw new IllegalArgumentException("Name is required"); this.fullName = fullName.trim(); }
    public abstract String getRole();
    public abstract String getAccessLevel();
}