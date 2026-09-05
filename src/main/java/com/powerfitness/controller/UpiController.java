package com.powerfitness.controller;

import com.powerfitness.entity.Role;
import com.powerfitness.entity.UpiSetting;
import com.powerfitness.entity.User;
import com.powerfitness.repository.UpiSettingRepository;
import com.powerfitness.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/upi")
@CrossOrigin(origins = "*")
public class UpiController {

    @Autowired
    private UpiSettingRepository upiSettingRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private com.powerfitness.service.FileStorageService fileStorageService;

    @GetMapping
    public ResponseEntity<?> getUpiSetting() {
        Optional<UpiSetting> settingOpt = upiSettingRepository.findTopByOrderByIdDesc();
        UpiSetting setting = settingOpt.orElseGet(() ->
            new UpiSetting("powerfitnesskurnool@okaxis", "PowerFitnessKurnool Gym", "/api/upi/qr", "Scan QR to pay")
        );
        if (setting.getQrCodeUrl() == null || setting.getQrCodeUrl().trim().isEmpty() || setting.getQrCodeUrl().contains("\\") || setting.getQrCodeUrl().startsWith("data:")) {
            setting.setQrCodeUrl("/api/upi/qr");
        }
        return ResponseEntity.ok(setting);
    }

    @GetMapping("/qr")
    public ResponseEntity<byte[]> getUpiQrImage() {
        try {
            Optional<UpiSetting> settingOpt = upiSettingRepository.findTopByOrderByIdDesc();
            UpiSetting setting = settingOpt.orElse(null);
            
            byte[] imageBytes = null;
            org.springframework.http.MediaType mediaType = org.springframework.http.MediaType.IMAGE_PNG;

            if (setting != null && setting.getQrCodeUrl() != null && !setting.getQrCodeUrl().trim().isEmpty()) {
                String qrUrl = setting.getQrCodeUrl().trim();

                // 1. Data URI (Base64)
                if (qrUrl.startsWith("data:image/")) {
                    String[] parts = qrUrl.split(",");
                    String header = parts[0].toLowerCase();
                    String base64Data = parts.length > 1 ? parts[1] : "";
                    if (header.contains("jpeg") || header.contains("jpg")) {
                        mediaType = org.springframework.http.MediaType.IMAGE_JPEG;
                    } else if (header.contains("webp")) {
                        mediaType = org.springframework.http.MediaType.parseMediaType("image/webp");
                    }
                    imageBytes = java.util.Base64.getDecoder().decode(base64Data.trim());
                } 
                // 2. Local uploaded file
                else {
                    java.nio.file.Path filePath = null;
                    if (qrUrl.startsWith("/uploads/")) {
                        filePath = java.nio.file.Paths.get("." + qrUrl);
                    } else if (qrUrl.startsWith("uploads/")) {
                        filePath = java.nio.file.Paths.get("./" + qrUrl);
                    } else if (!qrUrl.startsWith("http://") && !qrUrl.startsWith("https://") && !qrUrl.startsWith("/api/")) {
                        filePath = java.nio.file.Paths.get(qrUrl);
                    }

                    if (filePath != null && java.nio.file.Files.exists(filePath) && java.nio.file.Files.size(filePath) > 200) {
                        imageBytes = java.nio.file.Files.readAllBytes(filePath);
                        String fn = filePath.getFileName().toString().toLowerCase();
                        if (fn.endsWith(".jpg") || fn.endsWith(".jpeg")) {
                            mediaType = org.springframework.http.MediaType.IMAGE_JPEG;
                        } else if (fn.endsWith(".webp")) {
                            mediaType = org.springframework.http.MediaType.parseMediaType("image/webp");
                        }
                    }
                }
            }

            // 3. Fallback to default persistent QR code image
            if (imageBytes == null || imageBytes.length < 200) {
                java.nio.file.Path defPath = java.nio.file.Paths.get("./uploads/upi/default_upi_qr.png");
                if (java.nio.file.Files.exists(defPath) && java.nio.file.Files.size(defPath) > 200) {
                    imageBytes = java.nio.file.Files.readAllBytes(defPath);
                    mediaType = org.springframework.http.MediaType.IMAGE_PNG;
                }
            }

            if (imageBytes != null && imageBytes.length > 0) {
                return ResponseEntity.ok()
                        .contentType(mediaType)
                        .header(org.springframework.http.HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                        .header(org.springframework.http.HttpHeaders.PRAGMA, "no-cache")
                        .header(org.springframework.http.HttpHeaders.EXPIRES, "0")
                        .body(imageBytes);
            }

            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/admin")
    public ResponseEntity<?> updateUpiSetting(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody UpiSetting newSetting) {

        User user = authService.getAuthenticatedUser(authHeader);
        if (user == null || user.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        if (newSetting.getUpiId() == null || newSetting.getUpiId().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "UPI ID is required"));
        }

        Optional<UpiSetting> settingOpt = upiSettingRepository.findTopByOrderByIdDesc();
        UpiSetting setting = settingOpt.orElse(new UpiSetting());
        setting.setUpiId(newSetting.getUpiId().trim());
        setting.setMerchantName(newSetting.getMerchantName() != null ? newSetting.getMerchantName().trim() : "PowerFitnessKurnool Gym");
        
        if (newSetting.getQrCodeUrl() != null && newSetting.getQrCodeUrl().startsWith("data:image/")) {
            try {
                String savedPath = fileStorageService.saveUpiQrCodeBase64(newSetting.getQrCodeUrl());
                setting.setQrCodeUrl(savedPath);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Failed to save QR code image: " + e.getMessage()));
            }
        } else if (newSetting.getQrCodeUrl() != null && !newSetting.getQrCodeUrl().trim().isEmpty()) {
            setting.setQrCodeUrl(newSetting.getQrCodeUrl().trim());
        }

        setting.setNotes(newSetting.getNotes());

        UpiSetting saved = upiSettingRepository.save(setting);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/admin/upload-qr")
    public ResponseEntity<?> uploadUpiQr(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {

        User user = authService.getAuthenticatedUser(authHeader);
        if (user == null || user.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        try {
            String qrUrl = fileStorageService.saveUpiQrCode(file);
            Optional<UpiSetting> settingOpt = upiSettingRepository.findTopByOrderByIdDesc();
            UpiSetting setting = settingOpt.orElse(new UpiSetting("powerfitnesskurnool@okaxis", "PowerFitnessKurnool Gym", "", "Scan QR to pay"));
            setting.setQrCodeUrl(qrUrl);
            UpiSetting saved = upiSettingRepository.save(setting);
            return ResponseEntity.ok(Map.of("qrCodeUrl", qrUrl, "setting", saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to upload QR code: " + e.getMessage()));
        }
    }
}
