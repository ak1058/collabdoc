package com.amit.collabdoc.model;

import com.amit.collabdoc.model.enums.PermissionType;
import jakarta.persistence.*;

import java.util.Objects;

/**
 * This entity creates a "join table" that links a User to a Document
 * with a specific permission (e.g., User 'B' is an 'EDITOR' on Document '1').
 */
@Entity
@Table(name = "document_permissions")
public class DocumentPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PermissionType permission;

    // --- Constructors ---

    public DocumentPermission() {
    }

    public DocumentPermission(Document document, User user, PermissionType permission) {
        this.document = document;
        this.user = user;
        this.permission = permission;
    }

    // --- Getters and Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public PermissionType getPermission() {
        return permission;
    }

    public void setPermission(PermissionType permission) {
        this.permission = permission;
    }

    // --- equals, hashCode, toString ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DocumentPermission that = (DocumentPermission) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "DocumentPermission{" +
                "id=" + id +
                ", documentId=" + (document != null ? document.getId() : "null") +
                ", userId=" + (user != null ? user.getId() : "null") +
                ", permission=" + permission +
                '}';
    }
}
