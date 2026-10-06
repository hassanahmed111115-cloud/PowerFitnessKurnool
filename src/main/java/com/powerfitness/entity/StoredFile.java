package com.powerfitness.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stored_files", indexes = {
    @Index(name = "idx_stored_files_key", columnList = "fileKey"),
    @Index(name = "idx_stored_files_category", columnList = "category")
})
public class StoredFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String fileKey;

    @Column(nullable = false, length = 100)
    private String category; // members, supplements, collections, upi

    @Column(nullable = false, length = 100)
    private String contentType; // image/jpeg, image/png, image/webp

    private long fileSize;

    @Column(name = "data", columnDefinition = "BYTEA")
    private byte[] data;

    @Column(length = 1000)
    private String publicUrl;

    private LocalDateTime createdAt = LocalDateTime.now();

    public StoredFile() {}

    public StoredFile(String fileKey, String category, String contentType, long fileSize, byte[] data, String publicUrl) {
        this.fileKey = fileKey;
        this.category = category;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.data = data;
        this.publicUrl = publicUrl;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFileKey() { return fileKey; }
    public void setFileKey(String fileKey) { this.fileKey = fileKey; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public byte[] getData() { return data; }
    public void setData(byte[] data) { this.data = data; }

    public String getPublicUrl() { return publicUrl; }
    public void setPublicUrl(String publicUrl) { this.publicUrl = publicUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
