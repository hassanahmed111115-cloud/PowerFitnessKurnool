package com.powerfitness.service;

import com.powerfitness.entity.StoredFile;
import com.powerfitness.repository.StoredFileRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
public class FileStorageService {

    @Autowired
    private StoredFileRepository storedFileRepository;

    @Value("${supabase.url:${SUPABASE_URL:}}")
    private String supabaseUrl;

    @Value("${supabase.key:${SUPABASE_SERVICE_ROLE_KEY:${SUPABASE_KEY:}}}")
    private String supabaseKey;

    @Value("${supabase.storage.bucket:${SUPABASE_STORAGE_BUCKET:powerfitness-uploads}}")
    private String supabaseBucket;

    private final Path memberUploadPath = Paths.get("./uploads/members");
    private final Path supplementUploadPath = Paths.get("./uploads/supplements");
    private final Path collectionUploadPath = Paths.get("./uploads/collections");
    private final Path upiUploadPath = Paths.get("./uploads/upi");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public FileStorageService() {
        try {
            Files.createDirectories(memberUploadPath);
            Files.createDirectories(supplementUploadPath);
            Files.createDirectories(collectionUploadPath);
            Files.createDirectories(upiUploadPath);
        } catch (IOException e) {
            // On read-only or restricted filesystems, log and proceed with database-backed storage
            System.err.println("Notice: Could not initialize local upload directories: " + e.getMessage());
        }
    }

    @PostConstruct
    public void migrateExistingLocalFiles() {
        try {
            migrateDirectory(memberUploadPath, "members");
            migrateDirectory(supplementUploadPath, "supplements");
            migrateDirectory(collectionUploadPath, "collections");
            migrateDirectory(upiUploadPath, "upi");
        } catch (Exception e) {
            System.err.println("Notice: Initial local file migration skipped: " + e.getMessage());
        }
    }

    private void migrateDirectory(Path dir, String category) {
        if (!Files.exists(dir)) return;
        try (var stream = Files.list(dir)) {
            stream.filter(Files::isRegularFile).forEach(p -> {
                String filename = p.getFileName().toString();
                try {
                    if (!storedFileRepository.existsByFileKey(filename)) {
                        byte[] bytes = Files.readAllBytes(p);
                        String contentType = probeContentType(filename);
                        String publicUrl = "/api/files/" + category + "/" + filename;
                        StoredFile sf = new StoredFile(filename, category, contentType, bytes.length, bytes, publicUrl);
                        storedFileRepository.save(sf);
                        System.out.println("Migrated existing image to persistent storage: " + filename);
                    }
                } catch (Exception ex) {
                    System.err.println("Migration notice for " + filename + ": " + ex.getMessage());
                }
            });
        } catch (IOException ignored) {}
    }

    // --- Member Photos ---

    public String saveMemberPhoto(MultipartFile file) throws IOException {
        validateMultipartImage(file);
        byte[] bytes = file.getBytes();
        String contentType = file.getContentType() != null ? file.getContentType() : "image/jpeg";
        String ext = getFileExtension(file.getOriginalFilename());
        String filename = "member_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;

        return persistFile("members", filename, contentType, bytes, memberUploadPath);
    }

    public String saveMemberPhotoBase64(String base64Data) throws IOException {
        if (base64Data == null || base64Data.trim().isEmpty()) {
            throw new IllegalArgumentException("Photo data cannot be empty");
        }
        String cleanBase64 = base64Data;
        String ext = ".jpg";
        String contentType = "image/jpeg";
        if (base64Data.contains(",")) {
            String header = base64Data.split(",")[0];
            if (header.contains("png")) { ext = ".png"; contentType = "image/png"; }
            else if (header.contains("webp")) { ext = ".webp"; contentType = "image/webp"; }
            cleanBase64 = base64Data.split(",")[1];
        }
        byte[] decodedBytes = Base64.getDecoder().decode(cleanBase64.trim());
        validateImageBytes(decodedBytes, contentType);

        String filename = "member_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
        return persistFile("members", filename, contentType, decodedBytes, memberUploadPath);
    }

    // --- Supplement Photos ---

    public String saveSupplementPhoto(MultipartFile file) throws IOException {
        validateMultipartImage(file);
        byte[] bytes = file.getBytes();
        String contentType = file.getContentType() != null ? file.getContentType() : "image/jpeg";
        String ext = getFileExtension(file.getOriginalFilename());
        String filename = "supplement_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;

        return persistFile("supplements", filename, contentType, bytes, supplementUploadPath);
    }

    // --- Collection Photos ---

    public String saveCollectionPhoto(MultipartFile file) throws IOException {
        validateMultipartImage(file);
        byte[] bytes = file.getBytes();
        String contentType = file.getContentType() != null ? file.getContentType() : "image/jpeg";
        String ext = getFileExtension(file.getOriginalFilename());
        String filename = "collection_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;

        return persistFile("collections", filename, contentType, bytes, collectionUploadPath);
    }

    public String saveCollectionPhotoBase64(String base64Data) throws IOException {
        if (base64Data == null || base64Data.trim().isEmpty()) {
            throw new IllegalArgumentException("Collection photo data cannot be empty");
        }
        String cleanBase64 = base64Data;
        String ext = ".jpg";
        String contentType = "image/jpeg";
        if (base64Data.contains(",")) {
            String header = base64Data.split(",")[0];
            if (header.contains("png")) { ext = ".png"; contentType = "image/png"; }
            else if (header.contains("webp")) { ext = ".webp"; contentType = "image/webp"; }
            cleanBase64 = base64Data.split(",")[1];
        }
        byte[] decodedBytes = Base64.getDecoder().decode(cleanBase64.trim());
        validateImageBytes(decodedBytes, contentType);

        String filename = "collection_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
        return persistFile("collections", filename, contentType, decodedBytes, collectionUploadPath);
    }

    // --- UPI QR Code ---

    public String saveUpiQrCode(MultipartFile file) throws IOException {
        validateMultipartImage(file);
        byte[] bytes = file.getBytes();
        String contentType = file.getContentType() != null ? file.getContentType() : "image/png";
        String ext = getFileExtension(file.getOriginalFilename());
        String filename = "upi_qr_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;

        return persistFile("upi", filename, contentType, bytes, upiUploadPath);
    }

    public String saveUpiQrCodeBase64(String base64Data) throws IOException {
        if (base64Data == null || base64Data.trim().isEmpty()) {
            throw new IllegalArgumentException("QR Code image data cannot be empty");
        }
        String cleanBase64 = base64Data;
        String ext = ".jpg";
        String contentType = "image/jpeg";
        if (base64Data.contains(",")) {
            String header = base64Data.split(",")[0];
            if (header.contains("png")) { ext = ".png"; contentType = "image/png"; }
            else if (header.contains("webp")) { ext = ".webp"; contentType = "image/webp"; }
            cleanBase64 = base64Data.split(",")[1];
        }
        byte[] decodedBytes = Base64.getDecoder().decode(cleanBase64.trim());
        validateImageBytes(decodedBytes, contentType);

        String filename = "upi_qr_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
        return persistFile("upi", filename, contentType, decodedBytes, upiUploadPath);
    }

    // --- Core Persistence Engine ---

    @Transactional
    public String persistFile(String category, String filename, String contentType, byte[] data, Path localDirPath) {
        String publicUrl = "/api/files/" + category + "/" + filename;

        // 1. Permanent Storage in Supabase PostgreSQL
        StoredFile storedFile = new StoredFile(filename, category, contentType, data.length, data, publicUrl);
        storedFileRepository.save(storedFile);

        // 2. Local Disk Cache (if available)
        try {
            if (localDirPath != null && Files.exists(localDirPath)) {
                Path target = localDirPath.resolve(filename);
                Files.write(target, data);
            }
        } catch (Exception e) {
            System.err.println("Local disk cache skipped for " + filename + ": " + e.getMessage());
        }

        // 3. Optional Supabase Storage REST API Upload (if configured via env vars)
        uploadToSupabaseStorageBucket(category, filename, contentType, data);

        return publicUrl;
    }

    @Transactional
    public void deleteFile(String fileUrlOrKey) {
        if (fileUrlOrKey == null || fileUrlOrKey.trim().isEmpty()) return;
        String trimmed = fileUrlOrKey.trim();
        if (trimmed.contains("unsplash.com") || trimmed.contains("images.unsplash")) {
            return; // Never delete shared static presets
        }

        String filename = extractFilename(trimmed);
        if (filename == null || filename.isEmpty()) return;

        // 1. Delete from persistent database
        try {
            storedFileRepository.findByFileKey(filename).ifPresent(sf -> storedFileRepository.delete(sf));
        } catch (Exception ex) {
            System.err.println("Error deleting stored file record " + filename + ": " + ex.getMessage());
        }

        // 2. Delete from local cache
        deleteLocalDiskFile(filename);

        // 3. Delete from Supabase Storage REST API if configured
        deleteFromSupabaseStorageBucket(filename);
    }

    private void deleteLocalDiskFile(String filename) {
        Path[] paths = new Path[] {
            memberUploadPath.resolve(filename),
            supplementUploadPath.resolve(filename),
            collectionUploadPath.resolve(filename),
            upiUploadPath.resolve(filename)
        };
        for (Path p : paths) {
            try {
                if (Files.exists(p)) {
                    Files.delete(p);
                }
            } catch (Exception ignored) {}
        }
    }

    // --- Supabase Storage REST API Integration (Zero-dependency HttpClient) ---

    private void uploadToSupabaseStorageBucket(String category, String filename, String contentType, byte[] data) {
        if (supabaseUrl == null || supabaseUrl.trim().isEmpty() || supabaseKey == null || supabaseKey.trim().isEmpty()) {
            return; // Supabase Storage REST credentials not provided; persistent PostgreSQL storage active
        }

        try {
            String cleanUrl = supabaseUrl.replaceAll("/+$", "");
            String objectPath = category + "/" + filename;
            URI uploadUri = URI.create(cleanUrl + "/storage/v1/object/" + supabaseBucket + "/" + objectPath);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uploadUri)
                    .header("Authorization", "Bearer " + supabaseKey.trim())
                    .header("apikey", supabaseKey.trim())
                    .header("Content-Type", contentType)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(data))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                System.out.println("Uploaded image to Supabase Storage Bucket: " + objectPath);
            } else {
                System.err.println("Supabase Storage upload status " + response.statusCode() + ": " + response.body());
            }
        } catch (Exception e) {
            System.err.println("Notice: Supabase Storage REST upload skipped: " + e.getMessage());
        }
    }

    private void deleteFromSupabaseStorageBucket(String filename) {
        if (supabaseUrl == null || supabaseUrl.trim().isEmpty() || supabaseKey == null || supabaseKey.trim().isEmpty()) {
            return;
        }

        try {
            String cleanUrl = supabaseUrl.replaceAll("/+$", "");
            // Check all potential category prefixes
            String[] categories = new String[] { "members", "supplements", "collections", "upi" };
            for (String cat : categories) {
                String objectPath = cat + "/" + filename;
                URI deleteUri = URI.create(cleanUrl + "/storage/v1/object/" + supabaseBucket + "/" + objectPath);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(deleteUri)
                        .header("Authorization", "Bearer " + supabaseKey.trim())
                        .header("apikey", supabaseKey.trim())
                        .DELETE()
                        .timeout(Duration.ofSeconds(10))
                        .build();

                httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            }
        } catch (Exception ignored) {}
    }

    // --- Validation & Helpers ---

    private void validateMultipartImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("File size exceeds 10MB limit");
        }
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("image/") && !contentType.equalsIgnoreCase("application/octet-stream"))) {
            throw new IllegalArgumentException("Only image files (JPEG, PNG, WEBP, GIF) are allowed");
        }

        try {
            validateImageBytes(file.getBytes(), contentType);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read image bytes: " + e.getMessage());
        }
    }

    private void validateImageBytes(byte[] bytes, String declaredContentType) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Image data cannot be empty");
        }
        if (bytes.length > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("Image size exceeds 10MB limit");
        }
        if (bytes.length < 4) {
            throw new IllegalArgumentException("Invalid image file: file too small");
        }

        // Magic bytes verification
        boolean isJpeg = (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF);
        boolean isPng = (bytes[0] == (byte) 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47);
        boolean isGif = (bytes[0] == 0x47 && bytes[1] == 0x49 && bytes[2] == 0x46);
        boolean isWebp = bytes.length >= 12 &&
                bytes[0] == 0x52 && bytes[1] == 0x49 && bytes[2] == 0x46 && bytes[3] == 0x46 &&
                bytes[8] == 0x57 && bytes[9] == 0x45 && bytes[10] == 0x42 && bytes[11] == 0x50;

        if (!isJpeg && !isPng && !isGif && !isWebp) {
            throw new IllegalArgumentException("Invalid image file format. Only JPEG, PNG, WEBP, and GIF images are permitted.");
        }
    }

    public String extractFilename(String urlOrPath) {
        if (urlOrPath == null) return null;
        int lastSlash = urlOrPath.lastIndexOf('/');
        if (lastSlash != -1 && lastSlash < urlOrPath.length() - 1) {
            String name = urlOrPath.substring(lastSlash + 1);
            if (name.contains("?")) {
                name = name.substring(0, name.indexOf('?'));
            }
            return name;
        }
        return urlOrPath;
    }

    private String getFileExtension(String originalFilename) {
        if (originalFilename != null && originalFilename.lastIndexOf('.') != -1) {
            return originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase();
        }
        return ".jpg";
    }

    private String probeContentType(String filename) {
        if (filename == null) return "image/jpeg";
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".gif")) return "image/gif";
        return "image/jpeg";
    }
}
