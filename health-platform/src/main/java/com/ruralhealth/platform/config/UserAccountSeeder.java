package com.ruralhealth.platform.config;

import com.ruralhealth.platform.entity.AppUser;
import com.ruralhealth.platform.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class UserAccountSeeder implements CommandLineRunner {
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;
    private final String accountantUsername;
    private final String accountantPassword;

    public UserAccountSeeder(AppUserRepository userRepository, PasswordEncoder passwordEncoder,
            @Value("${app.security.admin.username:admin}") String adminUsername,
            @Value("${app.security.admin.password:admin123}") String adminPassword,
            @Value("${app.security.accountant.username:accountant}") String accountantUsername,
            @Value("${app.security.accountant.password:accountant123}") String accountantPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.accountantUsername = accountantUsername;
        this.accountantPassword = accountantPassword;
    }

    @Override
    public void run(String... args) {
        addAccount(adminUsername, adminPassword, "ADMIN");
        addAccount(accountantUsername, accountantPassword, "ACCOUNTANT");
    }

    private void addAccount(String username, String password, String role) {
        String normalizedUsername = username.toLowerCase(Locale.ROOT);
        if (!userRepository.existsByUsername(normalizedUsername)) {
            userRepository.save(new AppUser(normalizedUsername, passwordEncoder.encode(password), role));
        }
    }
}