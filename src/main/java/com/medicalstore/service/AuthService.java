package com.medicalstore.service;

import com.medicalstore.model.Admin;
import com.medicalstore.model.Pharmacist;
import com.medicalstore.model.User;
import com.medicalstore.repository.StoreRepository;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final StoreRepository repository;
    private final Map<String, User> sessions = new ConcurrentHashMap<>();
    public AuthService(StoreRepository repository) { this.repository = repository; }

    public Map<String, Object> login(String username, String password, String requestedRole) {
        Map<String, Object> row = repository.findUser(username);
        if (row == null || !password.equals(row.get("password")) || !requestedRole.equalsIgnoreCase(row.get("role").toString())) {
            throw new IllegalArgumentException("Invalid username, password, or role");
        }
        String role = row.get("role").toString();
        User user = "ADMIN".equals(role)
            ? new Admin(((Number) row.get("id")).longValue(), username, password, row.get("full_name").toString())
            : new Pharmacist(((Number) row.get("id")).longValue(), username, password, row.get("full_name").toString());
        String token = UUID.randomUUID().toString(); sessions.put(token, user);
        return Map.of("token", token, "username", user.getUsername(), "fullName", user.getFullName(), "role", user.getRole(), "accessLevel", user.getAccessLevel());
    }
    public User require(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) throw new IllegalArgumentException("Please sign in");
        User user = sessions.get(authorization.substring(7));
        if (user == null) throw new IllegalArgumentException("Session expired; please sign in again");
        return user;
    }
    public User requireRole(String authorization, String role) {
        User user = require(authorization);
        if (!role.equals(user.getRole())) throw new IllegalArgumentException("This action requires " + role + " access");
        return user;
    }
    public void logout(String authorization) { if (authorization != null && authorization.startsWith("Bearer ")) sessions.remove(authorization.substring(7)); }
}