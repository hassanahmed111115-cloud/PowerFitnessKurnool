package com.powerfitness.controller;

import com.powerfitness.entity.Role;
import com.powerfitness.entity.User;
import com.powerfitness.service.AuthService;
import com.powerfitness.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
@CrossOrigin(origins = "*")
public class FileUploadController {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private AuthService authService;

    @PostMapping(value = "/member-photo", consumes = {"multipart/form-data"})
    public ResponseEntity<?> uploadMemberPhotoMultipart(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam("file") MultipartFile file) {

        User user = authService.getAuthenticatedUser(authHeader);
        if (user == null || user.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        try {
            String fileUrl = fileStorageService.saveMemberPhoto(file);
            return ResponseEntity.ok(Map.of("url", fileUrl, "message", "Member photo saved successfully."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Unable to capture photo: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/member-photo", consumes = {"application/json"})
    public ResponseEntity<?> uploadMemberPhotoBase64(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, String> body) {

        User user = authService.getAuthenticatedUser(authHeader);
        if (user == null || user.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        if (body == null || !body.containsKey("base64Image")) {
            return ResponseEntity.badRequest().body(Map.of("error", "base64Image is required"));
        }

        try {
            String fileUrl = fileStorageService.saveMemberPhotoBase64(body.get("base64Image"));
            return ResponseEntity.ok(Map.of("url", fileUrl, "message", "Member photo saved successfully."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Unable to capture photo: " + e.getMessage()));
        }
    }

    @PostMapping("/supplement-photo")
    public ResponseEntity<?> uploadSupplementPhoto(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam("file") MultipartFile file) {

        User user = authService.getAuthenticatedUser(authHeader);
        if (user == null || user.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        try {
            String fileUrl = fileStorageService.saveSupplementPhoto(file);
            return ResponseEntity.ok(Map.of("url", fileUrl, "message", "Supplement image uploaded successfully."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Supplement image upload failed: " + e.getMessage()));
        }
    }
}
