package com.amit.collabdoc.document;


import com.amit.collabdoc.dto.CreateDocumentRequest;
import com.amit.collabdoc.dto.DocumentResponse;
import com.amit.collabdoc.exception.AccessDeniedException;
import com.amit.collabdoc.exception.ResourceNotFoundException;
import com.amit.collabdoc.model.Document;
import com.amit.collabdoc.model.User;
import com.amit.collabdoc.model.enums.PermissionType;
import com.amit.collabdoc.repository.DocumentPermissionRepository;
import com.amit.collabdoc.repository.DocumentRepository;
import com.amit.collabdoc.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final DocumentPermissionRepository documentPermissionRepository;

    // Constructor updated
    public DocumentService ( DocumentRepository documentRepository,
                             UserRepository userRepository,
                             DocumentPermissionRepository documentPermissionRepository){
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.documentPermissionRepository = documentPermissionRepository;
    }

    /**
     * Creates a new document for the currently authenticated user.
     * Updated to use CreateDocumentRequest DTO.
     */
    @Transactional
    public Document createDocument (CreateDocumentRequest request, String username){
        User owner = getUserByUsername(username);

        String docTitle = (request.getTitle() == null || request.getTitle().isBlank()) ? "Untitled Document": request.getTitle();

//         A valid empty Quill delta as a JSON string
        String defaultContent = "{\"ops\":[{\"insert\":\"\\n\"}]}";

        Document document = new Document(docTitle, owner, defaultContent);
        return documentRepository.save(document);
    }

    /**
     * Updates the title of a document.
     * Only the owner or an EDITOR can perform this action.
     */
    @Transactional
    public Document updateDocumentTitle (Long documentId, String newTitle, String username){
        User user = getUserByUsername(username);
        Document document = getDocumentById(documentId); // Helper that finds or throws 404

        // Check for permission (this is the new logic)
        if (!hasEditPermission(document, user)) {
            throw new AccessDeniedException("You do not have permission to edit this document.");
        }

        document.setTitle(newTitle);
        return documentRepository.save(document);
    }

    /**
     * Deletes a document.
     * Only the owner can perform this action.
     */
    @Transactional
    public void deleteDocument(Long documentId, String username){
        User user = getUserByUsername(username);
        Document document = getDocumentById(documentId);

        // Check if the user is the owner
        if (!document.getOwner().equals(user)) {
            // Use our custom 403 Forbidden exception
            throw new AccessDeniedException("Only the owner can delete this document.");
        }

        documentRepository.delete(document);
    }

    /**
     * NEW: Retrieves a single document by its ID, *if* the user has permission.
     * This is the method our CollaborationService and Controller will use.
     */
    @Transactional(readOnly = true)
    public Document getDocumentById(Long documentId, String username) {
        User user = getUserByUsername(username);
        Document document = getDocumentById(documentId); // Uses private helper

        // Check if user is owner OR has any permission (READER or EDITOR)
        if (document.getOwner().equals(user)) {
            return document; // Owner always has access
        }

        // Check for a specific permission entry
        boolean hasPermission = documentPermissionRepository.findByDocumentAndUser(document, user).isPresent();

        if (!hasPermission) {
            throw new AccessDeniedException("You do not have permission to view this document.");
        }

        return document;
    }

    /**
     * Gets all documents owned by the user.
     * (We can expand this later to include "shared with me" docs)
     * UPDATED: Now returns List<DocumentResponse> as the controller expects.
     */
    @Transactional(readOnly = true)
    public List<DocumentResponse> getAllDocuments(String username){
        User user = getUserByUsername(username);
        List<Document> documents = documentRepository.findByOwnerOrderByUpdatedAtDesc(user);

        return documents.stream()
                .map(this::mapToDocumentResponse)
                .collect(Collectors.toList());
    }

    // --- Private Helper Methods ---

    /**
     * Helper to check if a user is the owner OR has EDITOR permission.
     */
    private boolean hasEditPermission(Document document, User user) {
        // 1. Check if owner
        if (document.getOwner().equals(user)) {
            return true;
        }
        // 2. Check if they have an EDITOR permission
        return documentPermissionRepository.findByDocumentAndUser(document, user)
                .map(permission -> permission.getPermission() == PermissionType.EDITOR)
                .orElse(false); // If no permission entry, default to false
    }

    /**
     *  Helper to find a user or throw a standard error.
     */
    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }

    /**
     *  Helper to find a document or throw a standard error.
     */
    private Document getDocumentById(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", id));
    }

    /**
     *  Helper to map a Document Entity to a DTO.
     */
    private DocumentResponse mapToDocumentResponse(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getTitle(),
                document.getOwner().getUsername(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }
}

