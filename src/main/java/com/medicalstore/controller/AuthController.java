package com.medicalstore.controller;

import com.medicalstore.service.AuthService;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    public AuthController(AuthService auth) { this.auth = auth; }
    @PostMapping("/login") public Map<String, Object> login(@RequestBody Map<String, String> body) {
        return auth.login(body.getOrDefault("username", ""), body.getOrDefault("password", ""), body.getOrDefault("role", ""));
    }
    @PostMapping("/logout") public Map<String, String> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        auth.logout(authorization); return Map.of("message", "Signed out");
    }
}