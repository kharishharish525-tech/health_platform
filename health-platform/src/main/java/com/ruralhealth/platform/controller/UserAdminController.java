package com.ruralhealth.platform.controller;

import com.ruralhealth.platform.entity.AppUser;
import com.ruralhealth.platform.repository.AppUserRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/users")
public class UserAdminController {
    private static final Set<String> ROLES = Set.of("USER", "ACCOUNTANT", "ADMIN");
    private final AppUserRepository userRepository;

    public UserAdminController(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public record UserSummary(Long userId, String username, String role) {}
    public record RoleUpdateRequest(String role) {}

    @GetMapping
    public List<UserSummary> getUsers() {
        return userRepository.findAll().stream()
                .map(user -> new UserSummary(user.getUserId(), user.getUsername(), user.getRole()))
                .toList();
    }

    @PatchMapping("/{id}/role")
    public UserSummary updateRole(@PathVariable Long id, @RequestBody RoleUpdateRequest request,
                                  @AuthenticationPrincipal UserDetails currentUser) {
        String role = request.role() == null ? "" : request.role().toUpperCase(Locale.ROOT);
        if (!ROLES.contains(role)) {
            throw new IllegalArgumentException("Role must be USER, ACCOUNTANT, or ADMIN");
        }
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        if ("ADMIN".equals(user.getRole()) && !"ADMIN".equals(role)
                && userRepository.countByRole("ADMIN") <= 1) {
            throw new IllegalStateException("The last administrator cannot be demoted");
        }
        if (user.getUsername().equals(currentUser.getUsername()) && !"ADMIN".equals(role)) {
            throw new IllegalStateException("You cannot remove your own administrator role");
        }
        user.setRole(role);
        userRepository.save(user);
        return new UserSummary(user.getUserId(), user.getUsername(), user.getRole());
    }
}