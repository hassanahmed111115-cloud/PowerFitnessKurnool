package com.powerfitness.controller;

import com.powerfitness.entity.Notification;
import com.powerfitness.entity.Role;
import com.powerfitness.entity.User;
import com.powerfitness.repository.NotificationRepository;
import com.powerfitness.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private AuthService authService;

    @GetMapping
    public ResponseEntity<?> getNotifications(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        User user = authService.getAuthenticatedUser(authHeader);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        List<Notification> list;
        if (user.getRole() == Role.ADMIN) {
            list = notificationRepository.findTop20ByOrderByCreatedAtDesc();
        } else {
            list = notificationRepository.findByUserIdOrUserIdIsNullOrderByCreatedAtDesc(user.getId());
        }

        return ResponseEntity.ok(list);
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id) {

        User user = authService.getAuthenticatedUser(authHeader);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        Optional<Notification> nOpt = notificationRepository.findById(id);
        if (nOpt.isPresent()) {
            Notification n = nOpt.get();
            n.setRead(true);
            notificationRepository.save(n);
            return ResponseEntity.ok(n);
        }

        return ResponseEntity.notFound().build();
    }
}
