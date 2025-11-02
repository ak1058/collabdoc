package com.amit.collabdoc.dto;

import java.time.LocalDateTime; // CHANGED from Instant

/**
 * DTO for sending document data.
 * This has been updated to match the changes in our Document model
 * and to include the 'content' field for the editor.
 */
public class DocumentResponse {

    private Long id;
    private String title;
    private String ownerUsername; // CHANGED from ownerId
    private LocalDateTime createdAt; // CHANGED from Instant
    private LocalDateTime updatedAt; // CHANGED from Instant
    private String content; // <-- ADDED THIS FIELD

    // No-arg constructor
    public DocumentResponse() {
    }

    // Constructor for lists (no content)
    public DocumentResponse(Long id, String title, String ownerUsername, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.title = title;
        this.ownerUsername = ownerUsername;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.content = null; // Default to null
    }

    // --- Getters and Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // --- GETTER AND SETTER FOR NEW FIELD ---
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}

