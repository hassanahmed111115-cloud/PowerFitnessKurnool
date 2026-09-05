package com.powerfitness.controller;

import com.powerfitness.dto.AuthRequest;
import com.powerfitness.dto.AuthResponse;
import com.powerfitness.entity.Member;
import com.powerfitness.entity.Role;
import com.powerfitness.entity.User;
import com.powerfitness.repository.MemberRepository;
import com.powerfitness.repository.UserRepository;
import com.powerfitness.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        authService.logout(authHeader);
        Map<String, String> res = new HashMap<>();
        res.put("message", "Logged out successfully");
        return ResponseEntity.ok(res);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        User user = authService.getAuthenticatedUser(authHeader);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        Map<String, Object> details = new HashMap<>();
        details.put("userId", user.getId());
        details.put("username", user.getUsername());
        details.put("fullName", user.getFullName());
        details.put("role", user.getRole());

        if (user.getRole() == Role.USER) {
            Optional<Member> memberOpt = memberRepository.findByUserId(user.getId());
            memberOpt.ifPresent(m -> {
                details.put("memberId", m.getId());
                details.put("memberCode", m.getMemberCode());
                details.put("photoUrl", m.getPhotoUrl());
                details.put("status", m.getStatus());
                details.put("daysRemaining", m.getDaysRemaining());
            });
        }

        return ResponseEntity.ok(details);
    }

    // --- Admin: Change Username ---
    @PutMapping("/admin/username")
    public ResponseEntity<?> updateAdminUsername(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, String> body) {

        User user = authService.getAuthenticatedUser(authHeader);
        if (user == null || user.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        String newUsername = body != null ? body.get("newUsername") : null;
        if (newUsername == null || newUsername.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "New username is required"));
        }
        newUsername = newUsername.trim();

        if (newUsername.length() < 3) {
            return ResponseEntity.badRequest().body(Map.of("error", "New username must be at least 3 characters long"));
        }

        if (newUsername.equalsIgnoreCase(user.getUsername())) {
            return ResponseEntity.badRequest().body(Map.of("error", "New username must be different from current username"));
        }

        Optional<User> existing = userRepository.findByUsername(newUsername);
        if (existing.isPresent() && !existing.get().getId().equals(user.getId())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username is already taken by another account"));
        }

        user.setUsername(newUsername);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
            "message", "Username updated successfully",
            "username", newUsername
        ));
    }

    // --- Admin: Change Password ---
    @PutMapping("/admin/password")
    public ResponseEntity<?> updateAdminPassword(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, String> body) {

        User user = authService.getAuthenticatedUser(authHeader);
        if (user == null || user.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        String currentPassword = body != null ? body.get("currentPassword") : null;
        String newPassword = body != null ? body.get("newPassword") : null;
        String confirmPassword = body != null ? body.get("confirmPassword") : null;

        if (currentPassword == null || currentPassword.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Current password is required"));
        }
        if (newPassword == null || newPassword.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "New password is required"));
        }
        if (confirmPassword == null || confirmPassword.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Confirm password is required"));
        }

        if (!authService.verifyPassword(currentPassword, user.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Current password is incorrect"));
        }

        if (newPassword.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "New password must be at least 6 characters long"));
        }

        if (!newPassword.equals(confirmPassword)) {
            return ResponseEntity.badRequest().body(Map.of("error", "New passwords do not match"));
        }

        user.setPassword(authService.hashPassword(newPassword));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    // --- Member: Change Password ---
    @PutMapping("/member/password")
    public ResponseEntity<?> updateMemberPassword(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, String> body) {

        User user = authService.getAuthenticatedUser(authHeader);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        String currentPassword = body != null ? body.get("currentPassword") : null;
        String newPassword = body != null ? body.get("newPassword") : null;
        String confirmPassword = body != null ? body.get("confirmPassword") : null;

        if (currentPassword == null || currentPassword.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Current password is required"));
        }
        if (newPassword == null || newPassword.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "New password is required"));
        }
        if (confirmPassword == null || confirmPassword.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Confirm password is required"));
        }

        if (!authService.verifyPassword(currentPassword, user.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Current password is incorrect"));
        }

        if (newPassword.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "New password must be at least 6 characters long"));
        }

        if (!newPassword.equals(confirmPassword)) {
            return ResponseEntity.badRequest().body(Map.of("error", "New passwords do not match"));
        }

        user.setPassword(authService.hashPassword(newPassword));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }
}
