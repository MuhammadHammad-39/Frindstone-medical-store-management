package com.medicalstore.controller;

import com.medicalstore.model.User;
import com.medicalstore.service.AuthService;
import com.medicalstore.service.StoreService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class StoreController {
    private final StoreService store;
    private final AuthService auth;
    public StoreController(StoreService store, AuthService auth) { this.store = store; this.auth = auth; }
    @GetMapping("/dashboard") public Map<String, Object> dashboard(@RequestHeader(value = "Authorization", required = false) String token) { auth.require(token); return store.dashboard(); }
    @GetMapping("/medicines") public List<Map<String, Object>> medicines(@RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(required = false) String name, @RequestParam(required = false) String category, @RequestParam(required = false) Long id) {
        auth.require(token);
        if (id != null) return store.searchMedicines(id);
        if (category != null && !category.isBlank()) return store.searchMedicines(category, true);
        if (name != null && !name.isBlank()) return store.searchMedicines(name);
        return store.medicines();
    }
    @GetMapping("/reports/{type}") public Map<String, Object> report(@RequestHeader(value = "Authorization", required = false) String token,
            @org.springframework.web.bind.annotation.PathVariable String type) {
        if ("sales".equals(type)) auth.requireRole(token, "ADMIN");
        else auth.require(token);
        return store.report(type).get(0);
    }
    @PostMapping("/checkout") public Map<String, Object> checkout(@RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody Map<String, List<Map<String, Object>>> body) {
        User user = auth.requireRole(token, "PHARMACIST"); return store.checkout(user.getUsername(), body.get("items"));
    }
}