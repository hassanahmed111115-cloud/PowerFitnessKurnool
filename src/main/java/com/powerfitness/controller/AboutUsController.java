package com.powerfitness.controller;

import com.powerfitness.entity.AboutUs;
import com.powerfitness.entity.Role;
import com.powerfitness.entity.User;
import com.powerfitness.repository.AboutUsRepository;
import com.powerfitness.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/about")
@CrossOrigin(origins = "*")
public class AboutUsController {

    @Autowired
    private AboutUsRepository aboutUsRepository;

    @Autowired
    private AuthService authService;

    // --- Get About Us Content (Public / All Roles) ---
    @GetMapping
    public ResponseEntity<?> getAboutUs() {
        AboutUs about = aboutUsRepository.findById(1L).orElseGet(() -> {
            AboutUs def = new AboutUs();
            def.setId(1L);
            return aboutUsRepository.save(def);
        });
        return ResponseEntity.ok(about);
    }

    // --- Update About Us Content (Admin Only) ---
    @PutMapping
    public ResponseEntity<?> updateAboutUs(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody AboutUs updated) {

        User user = authService.getAuthenticatedUser(authHeader);
        if (user == null || user.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required to modify About Us content"));
        }

        AboutUs about = aboutUsRepository.findById(1L).orElseGet(() -> {
            AboutUs def = new AboutUs();
            def.setId(1L);
            return def;
        });

        if (updated.getGymName() != null && !updated.getGymName().trim().isEmpty()) {
            about.setGymName(updated.getGymName().trim());
        }
        if (updated.getTagline() != null) about.setTagline(updated.getTagline().trim());
        if (updated.getIntroduction() != null) about.setIntroduction(updated.getIntroduction().trim());
        if (updated.getMission() != null) about.setMission(updated.getMission().trim());
        if (updated.getVision() != null) about.setVision(updated.getVision().trim());
        if (updated.getFacilities() != null) about.setFacilities(updated.getFacilities().trim());
        if (updated.getTrainers() != null) about.setTrainers(updated.getTrainers().trim());
        if (updated.getOwnerInfo() != null) about.setOwnerInfo(updated.getOwnerInfo().trim());
        if (updated.getPhone() != null) about.setPhone(updated.getPhone().trim());
        if (updated.getEmail() != null) about.setEmail(updated.getEmail().trim());
        if (updated.getAddress() != null) about.setAddress(updated.getAddress().trim());
        if (updated.getTimings() != null) about.setTimings(updated.getTimings().trim());
        if (updated.getGalleryNote() != null) about.setGalleryNote(updated.getGalleryNote().trim());

        about.setUpdatedAt(LocalDateTime.now());
        AboutUs saved = aboutUsRepository.save(about);

        return ResponseEntity.ok(Map.of(
            "message", "About Us content updated successfully",
            "about", saved
        ));
    }
}