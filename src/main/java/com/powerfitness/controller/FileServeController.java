package com.powerfitness.controller;

import com.powerfitness.entity.StoredFile;
import com.powerfitness.repository.StoredFileRepository;
import com.powerfitness.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@RestController
@CrossOrigin(origins = "*")
public class FileServeController {

    @Autowired
    private StoredFileRepository storedFileRepository;

    @Autowired
    private FileStorageService fileStorageService;

    // Primary permanent endpoint
    @GetMapping("/api/files/{category}/{filename}")
    public ResponseEntity<?> serveFile(
            @PathVariable String category,
            @PathVariable String filename) {
        return fetchAndServe(category, filename);
    }

    // Backward compatibility endpoint for legacy paths (/uploads/members/..., /uploads/supplements/...)
    @GetMapping("/uploads/{category}/{filename}")
    public ResponseEntity<?> serveLegacyUpload(
            @PathVariable String category,
            @PathVariable String filename) {
        return fetchAndServe(category, filename);
    }

    private ResponseEntity<?> fetchAndServe(String category, String filename) {
        String cleanName = fileStorageService.extractFilename(filename);

        // 1. Try persistent Supabase PostgreSQL stored_files
        Optional<StoredFile> sfOpt = storedFileRepository.findByCategoryAndFileKey(category, cleanName);
        if (sfOpt.isEmpty()) {
            sfOpt = storedFileRepository.findByFileKey(cleanName);
        }

        if (sfOpt.isPresent()) {
            StoredFile sf = sfOpt.get();
            MediaType mediaType;
            try {
                mediaType = MediaType.parseMediaType(sf.getContentType());
            } catch (Exception e) {
                mediaType = MediaType.IMAGE_JPEG;
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, mediaType.toString())
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                    .body(sf.getData());
        }

        // 2. Fallback to local disk (and auto-migrate into persistent storage if found)
        Path localPath = Paths.get("./uploads", category, cleanName);
        if (Files.exists(localPath)) {
            try {
                byte[] bytes = Files.readAllBytes(localPath);
                String contentType = Files.probeContentType(localPath);
                if (contentType == null) contentType = "image/jpeg";

                StoredFile sf = new StoredFile(cleanName, category, contentType, bytes.length, bytes, "/api/files/" + category + "/" + cleanName);
                storedFileRepository.save(sf);

                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, contentType)
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                        .body(bytes);
            } catch (IOException ignored) {}
        }

        // 3. Not found
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
}
