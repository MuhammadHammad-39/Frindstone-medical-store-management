package com.medicalstore.controller;

import com.medicalstore.service.AuthService;
import com.medicalstore.service.StoreService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final StoreService store;
    private final AuthService auth;
    public AdminController(StoreService store, AuthService auth) { this.store = store; this.auth = auth; }
    @GetMapping("/staff") public List<Map<String, Object>> staff(@RequestHeader(value = "Authorization", required = false) String token) { auth.requireRole(token, "ADMIN"); return store.staff(); }
    @PostMapping("/medicines") public Map<String, Long> createMedicine(@RequestHeader(value = "Authorization", required = false) String token, @RequestBody Map<String, Object> body) {
        auth.requireRole(token, "ADMIN"); return Map.of("id", store.saveMedicine(0, body));
    }
    @PutMapping("/medicines/{id}") public Map<String, Long> updateMedicine(@RequestHeader(value = "Authorization", required = false) String token, @PathVariable long id, @RequestBody Map<String, Object> body) {
        auth.requireRole(token, "ADMIN"); return Map.of("id", store.saveMedicine(id, body));
    }
    @DeleteMapping("/medicines/{id}") public Map<String, String> deleteMedicine(@RequestHeader(value = "Authorization", required = false) String token, @PathVariable long id) {
        auth.requireRole(token, "ADMIN"); store.deleteMedicine(id); return Map.of("message", "Medicine deleted");
    }
    @GetMapping("/suppliers") public List<Map<String, Object>> suppliers(@RequestHeader(value = "Authorization", required = false) String token) { auth.requireRole(token, "ADMIN"); return store.suppliers(); }
    @PostMapping("/suppliers") public Map<String, Long> createSupplier(@RequestHeader(value = "Authorization", required = false) String token, @RequestBody Map<String, Object> body) {
        auth.requireRole(token, "ADMIN"); return Map.of("id", store.saveSupplier(0, body));
    }
    @PutMapping("/suppliers/{id}") public Map<String, Long> updateSupplier(@RequestHeader(value = "Authorization", required = false) String token, @PathVariable long id, @RequestBody Map<String, Object> body) {
        auth.requireRole(token, "ADMIN"); return Map.of("id", store.saveSupplier(id, body));
    }
    @DeleteMapping("/suppliers/{id}") public Map<String, String> deleteSupplier(@RequestHeader(value = "Authorization", required = false) String token, @PathVariable long id) {
        auth.requireRole(token, "ADMIN"); store.deleteSupplier(id); return Map.of("message", "Supplier deleted");
    }
}