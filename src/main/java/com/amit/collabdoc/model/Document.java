package com.amit.collabdoc.model;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Lob
    @Column(columnDefinition = "TEXT")
    @Basic(fetch = FetchType.LAZY)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;


    // A document can have many permissions attached to it
    @OneToMany(
            mappedBy = "document", // "document" is the field in DocumentPermission
            cascade = CascadeType.ALL, // When a document is deleted, all its permissions are deleted
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private Set<DocumentPermission> permissions;

    // --- CONSTRUCTORS ---

    @PrePersist
    protected void onCreate(){
        this.createdAt = this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected  void onUpdate(){
        this.updatedAt = LocalDateTime.now();
    }

    public Document(){
        this.permissions = new HashSet<>();
    }

    public Document(String title, User owner, String content){
        this.title = title;
        this.owner = owner;
        this.content = content;
        this.permissions = new HashSet<>();
    }

    // --- GETTERS AND SETTERS ---

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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
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

    // --- NEW GETTER/SETTER ---
    public Set<DocumentPermission> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<DocumentPermission> permissions) {
        this.permissions = permissions;
    }


    // --- equals, hashCode, toString ---

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Document document = (Document) obj;
        // Use ID for equality check for JPA entities
        return id != null && Objects.equals(id, document.id);
    }

    @Override
    public String toString() {
        return "Document{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", ownerId=" + (owner != null ? owner.getId() : "null") +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}

