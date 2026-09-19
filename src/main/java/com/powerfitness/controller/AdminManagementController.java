package com.powerfitness.controller;

import com.powerfitness.dto.AdminUserDto;
import com.powerfitness.entity.Role;
import com.powerfitness.entity.User;
import com.powerfitness.repository.UserRepository;
import com.powerfitness.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/accounts")
@CrossOrigin(origins = "*")
public class AdminManagementController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthService authService;

    // Helper to check admin authorization
    private User requireAdmin(String authHeader) {
        User user = authService.getAuthenticatedUser(authHeader);
        if (user == null || user.getRole() != Role.ADMIN || !user.isEnabled()) {
            return null;
        }
        return user;
    }

    // --- 1. GET ALL ADMIN ACCOUNTS ---
    @GetMapping
    public ResponseEntity<?> getAllAdmins(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        User currentAdmin = requireAdmin(authHeader);
        if (currentAdmin == null) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        List<User> admins = userRepository.findByRoleOrderByIdAsc(Role.ADMIN);
        List<AdminUserDto> dtos = admins.stream()
            .map(AdminUserDto::fromUser)
            .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // --- 2. CREATE NEW ADMIN ACCOUNT ---
    @PostMapping
    public ResponseEntity<?> createAdmin(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, String> body) {

        User currentAdmin = requireAdmin(authHeader);
        if (currentAdmin == null) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        String username = body != null ? body.get("username") : null;
        String fullName = body != null ? body.get("fullName") : null;
        String password = body != null ? body.get("password") : null;
        String confirmPassword = body != null ? body.get("confirmPassword") : null;

        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username or email is required"));
        }
        username = username.trim();

        if (username.length() < 3) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username must be at least 3 characters long"));
        }

        if (fullName == null || fullName.trim().isEmpty()) {
            fullName = "Administrator";
        } else {
            fullName = fullName.trim();
        }

        if (password == null || password.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password is required"));
        }

        if (password.length() < 8) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 8 characters long"));
        }

        if (confirmPassword == null || !password.equals(confirmPassword)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Passwords do not match"));
        }

        if (userRepository.existsByUsername(username)) {
            return ResponseEntity.badRequest().body(Map.of("error", "An account with this username or email already exists"));
        }

        User newAdmin = new User(username, authService.hashPassword(password), fullName, Role.ADMIN);
        newAdmin.setEnabled(true);
        newAdmin.setCreatedAt(LocalDateTime.now());
        newAdmin.setUpdatedAt(LocalDateTime.now());

        User saved = userRepository.save(newAdmin);

        return ResponseEntity.ok(Map.of(
            "message", "Administrator account created successfully",
            "admin", AdminUserDto.fromUser(saved)
        ));
    }

    // --- 3. TOGGLE ADMIN STATUS (ENABLE / DISABLE) ---
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateAdminStatus(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {

        User currentAdmin = requireAdmin(authHeader);
        if (currentAdmin == null) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        Optional<User> targetOpt = userRepository.findById(id);
        if (targetOpt.isEmpty() || targetOpt.get().getRole() != Role.ADMIN) {
            return ResponseEntity.status(404).body(Map.of("error", "Administrator account not found"));
        }

        User target = targetOpt.get();

        if (currentAdmin.getId().equals(target.getId())) {
            return ResponseEntity.badRequest().body(Map.of("error", "You cannot disable your own administrator account"));
        }

        Boolean enableReq = body != null && body.get("enabled") != null ? Boolean.valueOf(body.get("enabled").toString()) : false;

        // If disabling, ensure at least one other active admin remains
        if (!enableReq) {
            long activeCount = userRepository.countByRoleAndEnabled(Role.ADMIN, true);
            if (target.isEnabled() && activeCount <= 1) {
                return ResponseEntity.badRequest().body(Map.of("error", "Cannot disable the last active administrator account. At least one active admin must remain."));
            }
        }

        target.setEnabled(enableReq);
        target.setUpdatedAt(LocalDateTime.now());
        userRepository.save(target);

        return ResponseEntity.ok(Map.of(
            "message", enableReq ? "Administrator enabled successfully" : "Administrator disabled successfully",
            "admin", AdminUserDto.fromUser(target)
        ));
    }

    // --- 4. RESET ADMIN PASSWORD ---
    @PutMapping("/{id}/password")
    public ResponseEntity<?> resetAdminPassword(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        User currentAdmin = requireAdmin(authHeader);
        if (currentAdmin == null) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        Optional<User> targetOpt = userRepository.findById(id);
        if (targetOpt.isEmpty() || targetOpt.get().getRole() != Role.ADMIN) {
            return ResponseEntity.status(404).body(Map.of("error", "Administrator account not found"));
        }

        User target = targetOpt.get();

        String newPassword = body != null ? body.get("newPassword") : null;
        String confirmPassword = body != null ? body.get("confirmPassword") : null;

        if (newPassword == null || newPassword.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "New password is required"));
        }

        if (newPassword.length() < 8) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 8 characters long"));
        }

        if (confirmPassword == null || !newPassword.equals(confirmPassword)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Passwords do not match"));
        }

        target.setPassword(authService.hashPassword(newPassword));
        target.setUpdatedAt(LocalDateTime.now());
        userRepository.save(target);

        return ResponseEntity.ok(Map.of(
            "message", "Password reset successfully for " + target.getUsername()
        ));
    }

    // --- 5. DELETE ADMIN ACCOUNT ---
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAdmin(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id) {

        User currentAdmin = requireAdmin(authHeader);
        if (currentAdmin == null) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        Optional<User> targetOpt = userRepository.findById(id);
        if (targetOpt.isEmpty() || targetOpt.get().getRole() != Role.ADMIN) {
            return ResponseEntity.status(404).body(Map.of("error", "Administrator account not found"));
        }

        User target = targetOpt.get();

        if (currentAdmin.getId().equals(target.getId())) {
            return ResponseEntity.badRequest().body(Map.of("error", "You cannot delete your own administrator account"));
        }

        long totalAdmins = userRepository.countByRole(Role.ADMIN);
        if (totalAdmins <= 1) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cannot delete the last administrator account"));
        }

        long activeAdmins = userRepository.countByRoleAndEnabled(Role.ADMIN, true);
        if (target.isEnabled() && activeAdmins <= 1) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cannot delete the last active administrator account"));
        }

        userRepository.delete(target);

        return ResponseEntity.ok(Map.of(
            "message", "Administrator account deleted successfully"
        ));
    }
}
