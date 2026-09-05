package com.powerfitness.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path memberUploadPath = Paths.get("./uploads/members");
    private final Path supplementUploadPath = Paths.get("./uploads/supplements");
    private final Path collectionUploadPath = Paths.get("./uploads/collections");
    private final Path upiUploadPath = Paths.get("./uploads/upi");

    public FileStorageService() {
        try {
            Files.createDirectories(memberUploadPath);
            Files.createDirectories(supplementUploadPath);
            Files.createDirectories(collectionUploadPath);
            Files.createDirectories(upiUploadPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize upload directories", e);
        }
    }

    public String saveMemberPhoto(MultipartFile file) throws IOException {
        validateImageFile(file);
        String ext = getFileExtension(file.getOriginalFilename());
        String filename = "member_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
        Path target = memberUploadPath.resolve(filename);
        Files.copy(file.getInputStream(), target);
        return "/uploads/members/" + filename;
    }

    public String saveMemberPhotoBase64(String base64Data) throws IOException {
        if (base64Data == null || base64Data.trim().isEmpty()) {
            throw new IllegalArgumentException("Photo data cannot be empty");
        }
        String cleanBase64 = base64Data;
        String ext = ".jpg";
        if (base64Data.contains(",")) {
            String header = base64Data.split(",")[0];
            if (header.contains("png")) ext = ".png";
            else if (header.contains("webp")) ext = ".webp";
            cleanBase64 = base64Data.split(",")[1];
        }
        byte[] decodedBytes = Base64.getDecoder().decode(cleanBase64.trim());
        if (decodedBytes.length == 0) {
            throw new IllegalArgumentException("Decoded image is empty");
        }
        if (decodedBytes.length > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("Image size exceeds 10MB limit");
        }

        String filename = "member_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
        Path target = memberUploadPath.resolve(filename);
        try (FileOutputStream fos = new FileOutputStream(target.toFile())) {
            fos.write(decodedBytes);
        }
        return "/uploads/members/" + filename;
    }

    public String saveSupplementPhoto(MultipartFile file) throws IOException {
        validateImageFile(file);
        String ext = getFileExtension(file.getOriginalFilename());
        String filename = "supplement_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
        Path target = supplementUploadPath.resolve(filename);
        Files.copy(file.getInputStream(), target);
        return "/uploads/supplements/" + filename;
    }

    public String saveCollectionPhoto(MultipartFile file) throws IOException {
        validateImageFile(file);
        String ext = getFileExtension(file.getOriginalFilename());
        String filename = "collection_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
        Path target = collectionUploadPath.resolve(filename);
        Files.copy(file.getInputStream(), target);
        return "/uploads/collections/" + filename;
    }

    public String saveCollectionPhotoBase64(String base64Data) throws IOException {
        if (base64Data == null || base64Data.trim().isEmpty()) {
            throw new IllegalArgumentException("Collection photo data cannot be empty");
        }
        String cleanBase64 = base64Data;
        String ext = ".jpg";
        if (base64Data.contains(",")) {
            String header = base64Data.split(",")[0];
            if (header.contains("png")) ext = ".png";
            else if (header.contains("webp")) ext = ".webp";
            cleanBase64 = base64Data.split(",")[1];
        }
        byte[] decodedBytes = Base64.getDecoder().decode(cleanBase64.trim());
        if (decodedBytes.length == 0) {
            throw new IllegalArgumentException("Decoded image is empty");
        }
        if (decodedBytes.length > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("Image size exceeds 10MB limit");
        }

        String filename = "collection_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
        Path target = collectionUploadPath.resolve(filename);
        try (FileOutputStream fos = new FileOutputStream(target.toFile())) {
            fos.write(decodedBytes);
        }
        return "/uploads/collections/" + filename;
    }

    public String saveUpiQrCode(MultipartFile file) throws IOException {
        validateImageFile(file);
        String ext = getFileExtension(file.getOriginalFilename());
        String filename = "upi_qr_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
        Path target = upiUploadPath.resolve(filename);
        Files.copy(file.getInputStream(), target);
        return "/uploads/upi/" + filename;
    }

    public String saveUpiQrCodeBase64(String base64Data) throws IOException {
        if (base64Data == null || base64Data.trim().isEmpty()) {
            throw new IllegalArgumentException("QR Code image data cannot be empty");
        }
        String cleanBase64 = base64Data;
        String ext = ".jpg";
        if (base64Data.contains(",")) {
            String header = base64Data.split(",")[0];
            if (header.contains("png")) ext = ".png";
            else if (header.contains("webp")) ext = ".webp";
            cleanBase64 = base64Data.split(",")[1];
        }
        byte[] decodedBytes = Base64.getDecoder().decode(cleanBase64.trim());
        if (decodedBytes.length == 0) {
            throw new IllegalArgumentException("Decoded QR image is empty");
        }
        if (decodedBytes.length > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("QR image size exceeds 10MB limit");
        }

        String filename = "upi_qr_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
        Path target = upiUploadPath.resolve(filename);
        try (FileOutputStream fos = new FileOutputStream(target.toFile())) {
            fos.write(decodedBytes);
        }
        return "/uploads/upi/" + filename;
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("File size exceeds 10MB limit");
        }
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("image/") && !contentType.equalsIgnoreCase("application/octet-stream"))) {
            throw new IllegalArgumentException("Only image files (JPEG, PNG, WEBP) are allowed");
        }
    }

    private String getFileExtension(String originalFilename) {
        if (originalFilename != null && originalFilename.lastIndexOf('.') != -1) {
            return originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase();
        }
        return ".jpg";
    }
}
