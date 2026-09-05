package com.powerfitness.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "about_us")
public class AboutUs {

    @Id
    private Long id = 1L;

    @Column(nullable = false)
    private String gymName = "Power Fitness Unisex GYM Kurnool";

    private String tagline = "Build Your Body. Build Your Discipline.";

    @Column(length = 4000)
    private String introduction = "[Gym Introduction Placeholder - Enter details about Power Fitness Unisex GYM Kurnool, establishment history, and training philosophy here]";

    @Column(length = 2000)
    private String mission = "[Our Mission Placeholder - Enter your gym's mission statement, fitness commitment, and athlete transformation goals here]";

    @Column(length = 2000)
    private String vision = "[Our Vision Placeholder - Enter your gym's long-term vision, health awareness goals, and Kurnool fitness community impact here]";

    @Column(length = 3000)
    private String facilities = "[Facilities & Equipment Placeholder - List available facilities such as Heavy Strength Training Zone, Advanced Cardio Section, Cross-Training Area, Locker Rooms, Steam/Shower, and Front Desk Supplement Store]";

    @Column(length = 3000)
    private String trainers = "[Certified Trainers Placeholder - Add information about certified fitness coaches, personal trainers, bodybuilding mentors, and nutrition counseling staff]";

    @Column(length = 3000)
    private String ownerInfo = "[Gym Owner Information Placeholder - Enter gym owner name, credentials, bodybuilding experience, and personal welcome message to athletes]";

    private String phone = "+91 9876543210";
    private String email = "contact@powerfitnesskurnool.com";
    private String address = "Opposite Power Station, Near Main Road, Kurnool, Andhra Pradesh - 518001";
    private String timings = "Morning: 5:30 AM - 11:30 AM | Evening: 4:30 PM - 10:00 PM (Monday to Saturday)";

    @Column(length = 2000)
    private String galleryNote = "[Future Gym Photos & Gallery Placeholder - Visual photos of workout areas, equipment, and member achievements will appear here]";

    private LocalDateTime updatedAt = LocalDateTime.now();

    public AboutUs() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getGymName() { return gymName; }
    public void setGymName(String gymName) { this.gymName = gymName; }

    public String getTagline() { return tagline; }
    public void setTagline(String tagline) { this.tagline = tagline; }

    public String getIntroduction() { return introduction; }
    public void setIntroduction(String introduction) { this.introduction = introduction; }

    public String getMission() { return mission; }
    public void setMission(String mission) { this.mission = mission; }

    public String getVision() { return vision; }
    public void setVision(String vision) { this.vision = vision; }

    public String getFacilities() { return facilities; }
    public void setFacilities(String facilities) { this.facilities = facilities; }

    public String getTrainers() { return trainers; }
    public void setTrainers(String trainers) { this.trainers = trainers; }

    public String getOwnerInfo() { return ownerInfo; }
    public void setOwnerInfo(String ownerInfo) { this.ownerInfo = ownerInfo; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getTimings() { return timings; }
    public void setTimings(String timings) { this.timings = timings; }

    public String getGalleryNote() { return galleryNote; }
    public void setGalleryNote(String galleryNote) { this.galleryNote = galleryNote; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}