package com.amit.collabdoc.repository;

import com.amit.collabdoc.model.Document;
import com.amit.collabdoc.model.DocumentPermission;
import com.amit.collabdoc.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentPermissionRepository extends JpaRepository<DocumentPermission, Long> {

    /**
     * Finds a specific permission entry for a given document and user.
     * This is how we will check if a user has access.
     */
    Optional<DocumentPermission> findByDocumentAndUser(Document document, User user);
}
