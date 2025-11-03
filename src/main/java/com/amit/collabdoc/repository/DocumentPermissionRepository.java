package com.amit.collabdoc.repository;

import com.amit.collabdoc.model.Document;
import com.amit.collabdoc.model.DocumentPermission;
import com.amit.collabdoc.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentPermissionRepository extends JpaRepository<DocumentPermission, Long> {

    /**
     * Finds a specific permission entry for a given document and user.
     * This is how we will check if a user has access.
     */
    Optional<DocumentPermission> findByDocumentAndUser(Document document, User user);

    /**
     * Finds all permissions granted to a specific user.
     * Spring Data JPA will create the query for us, ordering by the associated
     * document's 'updatedAt' field in descending order.
     */
    List<DocumentPermission> findByUserOrderByDocumentUpdatedAtDesc(User user);
}
