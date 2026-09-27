package com.ruralhealth.platform.controller;

import com.ruralhealth.platform.entity.AppUser;
import com.ruralhealth.platform.repository.AppUserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public record SignupRequest(String username, String password) {}

    @PostMapping("/signup")
    public ResponseEntity<Map<String, String>> signup(@RequestBody SignupRequest request) {
        String username = request.username() == null ? "" : request.username().trim().toLowerCase(Locale.ROOT);
        String password = request.password() == null ? "" : request.password();

        if (!username.matches("[a-z0-9._-]{3,30}")) {
            throw new IllegalArgumentException(
                    "Username must be 3-30 characters using letters, numbers, dots, underscores, or hyphens.");
        }
        if (password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters.");
        }
        if (userRepository.existsByUsername(username)) {
            throw new IllegalStateException("That username is already registered.");
        }

        userRepository.save(new AppUser(username, passwordEncoder.encode(password)));
        return ResponseEntity.status(201).body(Map.of("message", "Account created."));
    }
}